// SPDX-License-Identifier: GPL-3.0-or-later
#include "catalog.h"
#include "archive.h"
#include "flat.h"
#include <cstring>

namespace scn {
namespace {
void Utf8(std::string& out,uint32_t cp) {
    if(cp<0x80)out+=char(cp);
    else if(cp<0x800){out+=char(0xc0|(cp>>6));out+=char(0x80|(cp&63));}
    else if(cp<0x10000){out+=char(0xe0|(cp>>12));out+=char(0x80|((cp>>6)&63));out+=char(0x80|(cp&63));}
    else {out+=char(0xf0|(cp>>18));out+=char(0x80|((cp>>12)&63));out+=char(0x80|((cp>>6)&63));out+=char(0x80|(cp&63));}
}
std::string Key(const std::string& s){auto i=s.find('.');return i==std::string::npos?s:s.substr(i+1);}
}
std::optional<std::map<std::string,std::string>> ReadMsbt(const std::vector<uint8_t>& bytes) {
    Flat f(bytes);if(bytes.size()<32||bytes.size()>2*1024*1024||std::memcmp(bytes.data(),"MsgStdBn",8)||f.U16(8)!=0xfeff||bytes[12]!=1)return {};
    auto count=f.U16(14);if(!count||*count>32)return {};
    size_t label=0,text=0,label_end=0,text_end=0,pos=32;
    for(unsigned i=0;i<*count;i++){
        auto n=f.U32(pos+4);if(!n||!f.Range(pos,16)||!f.Range(pos+16,*n))return {};
        if(!std::memcmp(bytes.data()+pos,"LBL1",4)){label=pos+16;label_end=label+*n;}
        if(!std::memcmp(bytes.data()+pos,"TXT2",4)){text=pos+16;text_end=text+*n;}
        pos=(pos+16+*n+15)&~size_t(15);
    }
    if(!label||!text)return {};
    auto groups=f.U32(label),ntext=f.U32(text);if(!groups||!ntext||*groups>4096||*ntext>16384||label+4+8*size_t(*groups)>label_end||text+4+4*size_t(*ntext)>text_end)return {};
    std::map<unsigned,std::string> labels;
    for(unsigned i=0;i<*groups;i++){
        auto n=f.U32(label+4+8*i),off=f.U32(label+8+8*i);if(!n||!off||*n>16384||*off>label_end-label)return {};
        size_t p=label+*off;
        for(unsigned j=0;j<*n;j++){
            if(p>=label_end)return {};const size_t len=bytes[p++];
            if(p>label_end||len+4>label_end-p)return {};auto index=f.U32(p+len);if(!index||*index>=*ntext)return {};
            std::string key(reinterpret_cast<const char*>(bytes.data()+p),len);if(!labels.emplace(*index,key).second)return {};p+=len+4;
        }
    }
    std::map<std::string,std::string> out;
    for(auto& [i,key]:labels){
        auto off=f.U32(text+4+4*i);if(!off||*off<4+4*size_t(*ntext)||*off>=text_end-text)return {};
        size_t p=text+*off;std::string value;bool ended=false;
        while(p+2<=text_end){
            const uint16_t cp=*f.U16(p);p+=2;
            if(!cp){ended=true;break;}
            if(cp==14){if(p+6>text_end)return {};size_t skip=*f.U16(p+4);p+=6;if(skip>text_end-p)return {};p+=skip;continue;}
            if(cp==15){if(p+4>text_end)return {};p+=4;continue;}
            if(cp>=0xd800&&cp<=0xdbff){if(p+2>text_end)return {};uint16_t low=*f.U16(p);p+=2;if(low<0xdc00||low>0xdfff)return {};Utf8(value,0x10000+((cp-0xd800)<<10)+(low-0xdc00));}
            else if(cp>=0xdc00&&cp<=0xdfff)return {};
            else Utf8(value,cp);
            if(value.size()>65536)return {};
        }
        if(!ended)return {};out.emplace(key,value);
    }
    return out;
}
std::optional<CourseCatalog> CourseCatalog::Load(const EdenDsmodHostApi& host) {
    std::vector<uint8_t> data,names,info;
    if(!ReadArchiveMember(host,"nx/Pack/Resident/master/Resident.drp","RouteData","rtd",data)||
       !ReadArchiveMember(host,"common/Text/master/TextResource_en.drp","Place_Name","msbt",names)||
       !ReadArchiveMember(host,"common/Text/master/TextResource_en.drp","Place_Info","msbt",info))return {};
    auto labels=ReadMsbt(names),descriptions=ReadMsbt(info);if(!labels||!descriptions)return {};
    Flat f(data);auto root=f.Root();if(!root)return {};auto rows=f.Tables(*root,0);if(rows.size()!=30)return {};
    CourseCatalog catalog;
    for(size_t t:rows){
        auto id=f.Byte(t,0);auto pack=f.String(t,1),name=f.String(t,3),region=f.String(t,13),map=f.String(t,16),description=f.String(t,9);
        if(!id||!*id||*id>30||!pack||pack->rfind("LC_",0)||!name||!region||!map||!description)return {};
        auto title=labels->find(Key(*name)),island=labels->find(Key(*region)),detail=descriptions->find(Key(*description));
        if(title==labels->end()||island==labels->end()||detail==descriptions->end())return {};
        if(!catalog.courses.emplace(*id,CourseInfo{*id,*pack,*map,title->second,island->second,detail->second}).second)return {};
    }
    return catalog;
}
}
