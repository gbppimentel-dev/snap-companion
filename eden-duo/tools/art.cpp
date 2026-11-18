// SPDX-License-Identifier: GPL-3.0-or-later
// Offline verifier only. Never bundled inside a mod or linked into its native module.
#include "map_art.h"
#include <cassert>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <string>

namespace {
size_t Romfs(void* raw,const char* key,uint64_t off,void* out,size_t size) {
    const std::string prefix="romfs:";
    if(std::strncmp(key,prefix.c_str(),prefix.size()))return 0;
    std::ifstream f(*static_cast<std::string*>(raw)+"/"+(key+prefix.size()),std::ios::binary|std::ios::ate);
    if(!f)return 0;
    const auto n=f.tellg();if(n<0)return 0;
    if(!out)return size_t(n);
    if(off>uint64_t(n)||size>uint64_t(n)-off)return 0;
    f.seekg(off);f.read(static_cast<char*>(out),size);return size_t(f.gcount());
}
}
int main(int argc,char** argv) {
    if(argc==2&&std::string(argv[1])=="--self-test") {
        scn::Image im; assert(!scn::DecodeMap({},im));
        std::vector<uint8_t> b(512);uint8_t out[16]{};
        for(unsigned i=0;i<16;i++)b[i]=uint8_t(i);
        assert(scn::DeswizzleBlock(b,4,4,16,0,0,out));for(int i=0;i<16;i++)assert(out[i]==i);
        assert(!scn::DeswizzleBlock(b,4,4,16,4,0,out));assert(!scn::DeswizzleBlock({},4,4,16,0,0,out));
        std::cout<<"Map decoder bounds checks passed.\n";return 0;
    }
    if(argc!=5) { std::cerr<<"Usage: scn-art <own-romfs-root> <archive 1..30> <member> <output.pam>\n";return 2; }
    std::string root=argv[1]; EdenDsmodHostApi host{};host.userdata=&root;host.read_romfs=Romfs;
    std::vector<uint8_t> packed;scn::Image image;
    if(!scn::ExtractMap(host,unsigned(std::stoul(argv[2])),argv[3],packed)||!scn::DecodeMap(packed,image)) {
        std::cerr<<"Map extraction/decoding failed.\n";return 1;
    }
    std::ofstream f(argv[4],std::ios::binary);
    f<<"P7\nWIDTH "<<image.width<<"\nHEIGHT "<<image.height<<"\nDEPTH 4\nMAXVAL 255\nTUPLTYPE RGB_ALPHA\nENDHDR\n";
    f.write(reinterpret_cast<const char*>(image.rgba.data()),image.rgba.size());
    if(!f)return 1;
    std::cout<<image.width<<"x"<<image.height<<" RGBA from own ROMFS\n";
}
