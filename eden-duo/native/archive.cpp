// SPDX-License-Identifier: GPL-3.0-or-later
#include "archive.h"
#include <algorithm>
#include <array>
#include <cstring>

int Kraken_Decompress(const uint8_t*,size_t,uint8_t*,size_t);
namespace scn {
namespace {
uint32_t U32(const uint8_t* p) { return uint32_t(p[0])|(uint32_t(p[1])<<8)|(uint32_t(p[2])<<16)|(uint32_t(p[3])<<24); }
void Put(uint8_t* p,uint32_t n) { for(int i=0;i<4;i++)p[i]=uint8_t(n>>(8*i)); }
uint32_t Crc(const uint8_t* p,size_t n) {
    uint32_t c=~0u;while(n--){c^=*p++;for(int i=0;i<8;i++)c=(c>>1)^((0u-(c&1))&0xedb88320u);}return ~c;
}
uint32_t Sar(uint32_t n,unsigned b) { return (n>>b)|((0u-(n>>31))<<(32-b)); }
struct Random {
    uint32_t x,y,z,w;
    explicit Random(uint32_t seed) {
        w=0x41c64e6du*seed+12345;z=0x41c64e6du*w+12345;y=0x41c64e6du*z+12345;x=0x41c64e6du*y+12345;
    }
    uint32_t Next() { uint32_t t=x^(x<<11);x=y;y=z;z=w;w=w^Sar(w,19)^(t^Sar(t,8));return w&0x7fffffff; }
    uint32_t Word() { const uint32_t a=Next(),b=Next()>>30;return a|(b<<31); }
};
Random Rng(uint32_t seed,const uint8_t* table) {
    uint32_t key=0;for(int i=0;i<4;i++)key|=uint32_t(table[(seed>>(8*i))&127])<<(8*i);return Random(key);
}
void Decode(uint8_t* p,size_t n,const uint8_t* table) {
    auto rng=Rng(U32(p),table);for(size_t i=4;i+4<=n;i+=4)Put(p+i,U32(p+i)^rng.Word());
}
bool Safe(const std::string& p) {
    if(p.empty()||p[0]=='/'||p.find("..")!=std::string::npos||p.size()>256)return false;
    return p.find_first_not_of("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_./-")==std::string::npos;
}
}
bool ReadArchiveMember(const EdenDsmodHostApi& host,const std::string& archive,
                       const std::string& member,const std::string& ext,std::vector<uint8_t>& out) {
    out.clear();
    constexpr size_t max_entry=32*1024*1024,max_output=16*1024*1024;
    if(!host.read_romfs||!Safe(archive)||!Safe(member)||!Safe(ext)||member.find('/')!=std::string::npos||ext.find('/')!=std::string::npos)return false;
    if(archive.rfind("common/",0)!=0&&archive.rfind("nx/Pack/",0)!=0)return false;
    const std::string path="romfs:"+archive;
    const size_t file_size=host.read_romfs(host.userdata,path.c_str(),0,nullptr,0);
    if(file_size<0x94||file_size>512*1024*1024)return false;
    auto read=[&](uint64_t off,uint8_t* dst,size_t n){return n<=max_entry&&off<=file_size&&n<=file_size-off&&host.read_romfs(host.userdata,path.c_str(),off,dst,n)==n;};
    std::array<uint8_t,0x94> header{};
    if(!read(0,header.data(),header.size())||U32(header.data())!=0x7f0e5359)return false;
    const auto* table=header.data()+0x14;const bool encoded=U32(header.data()+8)!=0xc65753e8;
    if(encoded){auto rng=Rng(U32(header.data()+4),table);for(size_t i=8;i<=16;i+=4)Put(header.data()+i,U32(header.data()+i)^rng.Word());}
    if(U32(header.data()+8)!=0xc65753e8||Crc(header.data()+0x10,0x84)!=U32(header.data()+4))return false;
    const unsigned count=U32(header.data()+16);if(count>16384)return false;
    const uint32_t ext_crc=Crc(reinterpret_cast<const uint8_t*>(ext.data()),ext.size());
    uint64_t pos=0x94;
    for(unsigned i=0;i<count;i++) {
        if(pos>file_size||file_size-pos<32)return false;
        std::array<uint8_t,256> peek{};const size_t len=std::min<size_t>(peek.size(),file_size-pos);
        if(!read(pos,peek.data(),len))return false;
        if(encoded)Decode(peek.data(),len,table);
        const size_t total=U32(peek.data()+8);
        if(U32(peek.data()+4)!=0xe0a331b4||total<32||total>file_size-pos)return false;
        const auto* end=static_cast<const uint8_t*>(std::memchr(peek.data()+24,0,std::min(len,total)-24));
        if(!end)return false;
        std::string name(reinterpret_cast<const char*>(peek.data()+24),end-(peek.data()+24));
        if((name==member||name==member+"."+ext)&&U32(peek.data()+12)==ext_crc) {
            const size_t csize=U32(peek.data()+16),dsize=U32(peek.data()+20),offset=(size_t(end-peek.data())+4)&~size_t(3);
            if(!dsize||dsize>max_output||total>max_entry||offset>total||csize>total-offset||csize<4)return false;
            std::vector<uint8_t> chunk(total+64,0),decoded(dsize+64,0);
            if(!read(pos,chunk.data(),total))return false;
            if(encoded)Decode(chunk.data(),total,table);
            auto* payload=chunk.data()+offset;
            if(U32(payload)==0xe42d98ba) {
                // Isolate the compressed payload and zero-pad its input too.
                std::vector<uint8_t> input(csize+64,0);std::copy_n(payload+4,csize-4,input.data());
                if(Kraken_Decompress(input.data(),csize-4,decoded.data(),dsize)!=int(dsize))return false;
            } else {if(csize<dsize)return false;std::copy_n(payload,dsize,decoded.data());}
            if(Crc(decoded.data(),dsize)!=U32(chunk.data()))return false;
            decoded.resize(dsize);out=std::move(decoded);return true;
        }
        pos+=total;
    }
    return false;
}
}
