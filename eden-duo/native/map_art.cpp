// SPDX-License-Identifier: GPL-3.0-or-later
#include "map_art.h"
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <limits>
#define BCDEC_IMPLEMENTATION
#include "bcdec.h"

// GPL-3.0-or-later, Powzix / rarten / zao. Caller supplies the documented
// 64-byte overrun padding on BOTH buffers. Only verified game-ROMFS input is used.
int Kraken_Decompress(const uint8_t*, size_t, uint8_t*, size_t);

namespace scn {
namespace {
constexpr size_t kLimit = 16 * 1024 * 1024;
uint32_t U32(const uint8_t* p) {
    return uint32_t(p[0]) | (uint32_t(p[1])<<8) | (uint32_t(p[2])<<16) | (uint32_t(p[3])<<24);
}
uint32_t Crc(const uint8_t* p, size_t n) {
    uint32_t c=~0u;
    while(n--) { c^=*p++; for(int j=0;j<8;j++) c=(c>>1)^((0u-(c&1))&0xedb88320u); }
    return ~c;
}
bool Read(const EdenDsmodHostApi& h, const char* p, uint64_t off, uint8_t* out, size_t n) {
    return h.read_romfs && n<=kLimit && h.read_romfs(h.userdata,p,off,out,n)==n;
}
}

bool ExtractMap(const EdenDsmodHostApi& h, unsigned archive, const std::string& member,
                std::vector<uint8_t>& result) {
    result.clear();
    // Fixed archive directory and simple names: no user-supplied path can escape it.
    if(archive<1 || archive>30 || member.size()>100 ||
       member.find_first_not_of("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.")!=std::string::npos ||
       (member.rfind("course_map_l_",0)!=0 && member.rfind("course_arrow_l_",0)!=0)) return false;
    char path[128]; std::snprintf(path,sizeof(path),"romfs:common/UI/master/Textures/course_l_%02u.drp",archive);
    if(!h.read_romfs) return false;
    const size_t file_size=h.read_romfs(h.userdata,path,0,nullptr,0);
    if(file_size<0x94 || file_size>128*1024*1024) return false;
    uint8_t hd[0x94];
    if(!Read(h,path,0,hd,sizeof(hd)) || U32(hd)!=0x7f0e5359 || U32(hd+8)!=0xc65753e8 ||
       Crc(hd+0x10,0x84)!=U32(hd+4)) return false;
    const unsigned count=U32(hd+0x10); if(count>4096) return false;
    uint64_t pos=0x94;
    for(unsigned i=0;i<count;i++) {
        if(pos>file_size || file_size-pos<0x20) return false;
        uint8_t entry[160]{};
        const size_t n=std::min<size_t>(sizeof(entry),file_size-pos);
        if(!Read(h,path,pos,entry,n) || U32(entry+4)!=0xe0a331b4) return false;
        const size_t total=U32(entry+8), compressed=U32(entry+16), decoded=U32(entry+20);
        if(total<0x20 || total>file_size-pos) return false;
        const auto* end=static_cast<const uint8_t*>(std::memchr(entry+24,0,std::min(n,total)-24));
        if(!end) return false;
        std::string name(reinterpret_cast<char*>(entry+24),end-(entry+24));
        if(name.size()<7 || name.compare(name.size()-7,7,".nutexb")!=0) name+=".nutexb";
        std::string wanted=member;
        if(wanted.size()<7 || wanted.compare(wanted.size()-7,7,".nutexb")!=0) wanted+=".nutexb";
        if(name==wanted && U32(entry+12)==0x5c156dbc) {
            const size_t offset=(size_t(end-entry)+4)&~size_t(3);
            if(decoded<0xb0 || decoded>kLimit || compressed<4 || compressed>kLimit ||
               offset>total || compressed>total-offset) return false;
            std::vector<uint8_t> input(compressed+64,0), output(decoded+64,0);
            if(!Read(h,path,pos+offset,input.data(),compressed)) return false;
            if(U32(input.data())==0xe42d98ba) {
                if(Kraken_Decompress(input.data()+4,compressed-4,output.data(),decoded)!=int(decoded)) return false;
            } else {
                if(compressed<decoded) return false;
                std::copy_n(input.data(),decoded,output.data());
            }
            if(Crc(output.data(),decoded)!=U32(entry)) return false;
            output.resize(decoded); result=std::move(output); return true;
        }
        pos+=total;
    }
    return false;
}

bool DeswizzleBlock(const std::vector<uint8_t>& src, unsigned wb, unsigned hb,
                    unsigned bpb, unsigned bx, unsigned by, uint8_t* out) {
    if(!wb || !hb || wb>1024 || hb>1024 || (bpb!=8 && bpb!=16) || bx>=wb || by>=hb || !out) return false;
    const unsigned hh=hb+hb/2;
    const unsigned bh=hh>=128?16:hh>=64?8:hh>=32?4:hh>=16?2:1;
    const size_t width_gobs=(size_t(wb)*bpb+63)/64;
    for(unsigned b=0;b<bpb;b++) {
        const size_t x=size_t(bx)*bpb+b, y=by;
        const size_t off=(y/(8*bh))*width_gobs*512*bh+(x/64)*512*bh+
            ((y%(8*bh))/8)*512+((x%64)/32)*256+((y%8)/2)*64+
            ((x%32)/16)*32+(y%2)*16+x%16;
        if(off>=src.size()) return false;
        out[b]=src[off];
    }
    return true;
}

bool DecodeMap(const std::vector<uint8_t>& bytes, Image& result) {
    result={}; if(bytes.size()<0xb0 || bytes.size()>kLimit) return false;
    const auto* t=bytes.data()+bytes.size()-0xb0;
    const unsigned w=U32(t+0x84), h=U32(t+0x88), fmt=U32(t+0x90);
    // These restrictions are verified against all 143 local map/route textures.
    if(w!=3840 || h!=2160 || U32(t+0x8c)!=1 || U32(t+0x94)!=2 || U32(t+0x98)!=1 ||
       U32(t+0x9c)!=0x1000 || U32(t+0xa0)!=1 || U32(t+0xa8)!=0x54455820 ||
       U32(t+0xac)!=2 || (fmt!=0x480 && fmt!=0x4a0 && fmt!=0x4e0)) return false;
    const size_t data_size=U32(t+0xa4);
    if(data_size!=bytes.size()-0xb0 || U32(t)!=data_size) return false;
    const unsigned bpb=fmt==0x480?8:16;
    const unsigned wb=w/4,hb=h/4;
    // Decode a block at a time and downsample directly: no 33 MiB RGBA intermediary.
    Image image; image.width=w/2; image.height=h/2;
    image.rgba.resize(size_t(image.width)*image.height*4);
    // Exclude the footer from all texture addresses.
    std::vector<uint8_t> tiled(bytes.begin(),bytes.begin()+data_size);
    for(unsigned y=0;y<hb;y++) for(unsigned x=0;x<wb;x++) {
        alignas(16) uint8_t block[16]{}, rgba[64]{};
        if(!DeswizzleBlock(tiled,wb,hb,bpb,x,y,block)) return false;
        if(fmt==0x480) bcdec_bc1(block,rgba,16);
        else if(fmt==0x4a0) bcdec_bc3(block,rgba,16);
        else bcdec_bc7(block,rgba,16);
        for(unsigned oy=0;oy<2;oy++) for(unsigned ox=0;ox<2;ox++) {
            unsigned sum[4]{},a=0;
            for(unsigned iy=0;iy<2;iy++) for(unsigned ix=0;ix<2;ix++) {
                const auto* p=rgba+((oy*2+iy)*4+ox*2+ix)*4;
                a+=p[3]; for(unsigned c=0;c<3;c++) sum[c]+=p[c]*p[3];
            }
            auto* p=image.rgba.data()+((size_t(y)*2+oy)*image.width+x*2+ox)*4;
            for(unsigned c=0;c<3;c++) p[c]=a?uint8_t((sum[c]+a/2)/a):0;
            p[3]=uint8_t((a+2)/4);
        }
    }
    result=std::move(image); return true;
}
}
