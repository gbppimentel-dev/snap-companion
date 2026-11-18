// SPDX-License-Identifier: GPL-3.0-or-later
#include "live_reader.h"
#include <cassert>
#include <cmath>
#include <cstring>
#include <iostream>
#include <map>
#include <string>
#include <vector>

namespace {
struct Memory {
    std::vector<uint8_t> b=std::vector<uint8_t>(0x10000);
    uint64_t base=0x100000;
    bool tear=false;
    int epochs=0,reads=0;
    template<class T> void Put(uint64_t p,const T& x) { std::memcpy(b.data()+p-base,&x,sizeof(x)); }
};
EdenDsmodBool Mapped(void* raw,uint64_t p,uint64_t n) {
    auto& m=*static_cast<Memory*>(raw); return p>=m.base&&p-m.base<=m.b.size()&&n<=m.b.size()-(p-m.base);
}
EdenDsmodBool Read(void* raw,uint64_t p,void* out,size_t n) {
    auto& m=*static_cast<Memory*>(raw); ++m.reads;
    if(!Mapped(raw,p,n))return false;
    std::memcpy(out,m.b.data()+p-m.base,n);
    if(m.tear&&p==m.base+0x110) { ++m.epochs; if(m.epochs==2) ++*static_cast<uint32_t*>(out); }
    return true;
}
int64_t Delta(void*,const char*,int64_t) { return 0x40; }
}
int main() {
    assert(std::strlen(scn::kBuild)==64);
    assert(scn::SupportsBuild(scn::kBuild));
    assert(!scn::SupportsBuild(nullptr)); assert(!scn::SupportsBuild("DDB900FF04A912ED"));
    std::string bad(scn::kBuild); bad.back()='1'; assert(!scn::SupportsBuild(bad.c_str()));
    assert(!scn::ProductionLayout());
    Memory m; const uint64_t root=m.base+0x100,player=m.base+0x200;
    m.Put(m.base+8,root);
    const uint32_t phase=7,course=2,level=1,route=3,epoch=99;
    m.Put(root,phase);m.Put(root+4,course);m.Put(root+8,level);m.Put(root+12,route);m.Put(root+16,epoch);m.Put(root+24,player);
    std::array<float,3> xyz{10,20,30},dir{1,0,0};m.Put(player,xyz);m.Put(player+12,dir);
    EdenDsmodHostApi h{};h.userdata=&m;h.main_base=m.base;h.main_size=m.b.size();h.is_mapped=Mapped;h.read_memory=Read;h.get_i64=Delta;
    scn::Layout l{8,false,0,4,8,12,16,24,0,12,7,true};
    auto run=scn::ReadRun(h,l); assert(run&&run->course==2&&run->level==1&&run->route==3&&run->position==xyz);
    l.verified=false;m.reads=0;assert(!scn::ReadRun(h,l)&&m.reads==0);l.verified=true;
    m.tear=true;assert(!scn::ReadRun(h,l));m.tear=false;
    const uint32_t menu=0;m.Put(root,menu);assert(!scn::ReadRun(h,l));m.Put(root,phase);
    const uint64_t null=0;m.Put(root+24,null);assert(!scn::ReadRun(h,l));m.Put(root+24,player);
    xyz[1]=NAN;m.Put(player,xyz);assert(!scn::ReadRun(h,l));xyz[1]=20;m.Put(player,xyz);
    l.root_rva=m.b.size()-4;assert(!scn::ReadRun(h,l));l.root_rva=8;
    m.Put(m.base+8+0x40,root);l.root_in_relocated_data=true;assert(scn::ReadRun(h,l));l.root_in_relocated_data=false;
    scn::Projection p{{2,0,0,100},{0,0,3,200},3840,2160,true};
    auto dot=scn::Project(*run,p);assert(dot&&dot->x==120&&dot->y==-290&&dot->heading_valid&&dot->heading==90);
    p.verified=false;assert(!scn::Project(*run,p));p.verified=true;
    p.x[3]=-10000;assert(!scn::Project(*run,p));
    std::cout<<"Live reader checks passed: exact build, no unverified reads, pointer/phase/epoch guards, relocation, projection.\n";
}
