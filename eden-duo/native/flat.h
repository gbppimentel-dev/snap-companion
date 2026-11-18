// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <cstdint>
#include <cstring>
#include <optional>
#include <string>
#include <vector>
namespace scn {
class Flat {
    const std::vector<uint8_t>& b;
public:
    explicit Flat(const std::vector<uint8_t>& bytes):b(bytes){}
    bool Range(size_t p,size_t n)const{return p<=b.size()&&n<=b.size()-p;}
    std::optional<uint32_t> U32(size_t p)const {
        if(!Range(p,4))return {};return uint32_t(b[p])|(uint32_t(b[p+1])<<8)|(uint32_t(b[p+2])<<16)|(uint32_t(b[p+3])<<24);
    }
    std::optional<uint16_t> U16(size_t p)const {
        if(!Range(p,2))return {};return uint16_t(b[p])|(uint16_t(b[p+1])<<8);
    }
    std::optional<uint8_t> U8(size_t p)const{if(!Range(p,1))return {};return b[p];}
    std::optional<size_t> Indirect(size_t p)const{
        auto off=U32(p);if(!off||*off>b.size()-p)return {};size_t q=p+*off;if(!Range(q,4))return {};return q;
    }
    std::optional<size_t> Root()const{return Indirect(0);}
    std::optional<size_t> Field(size_t t,unsigned slot)const {
        auto distance=U32(t);if(!distance||slot>1024)return {};
        const int64_t vt=int64_t(t)-int32_t(*distance);if(vt<0)return {};
        auto len=U16(size_t(vt)),object=U16(size_t(vt)+2);
        if(!len||!object||*len<4||*len%2||!Range(size_t(vt),*len)||!Range(t,*object)||4+2*slot+2>*len)return {};
        auto off=U16(size_t(vt)+4+2*slot);if(!off||!*off||*off>=*object)return {};return t+*off;
    }
    std::optional<uint8_t> Byte(size_t t,unsigned s)const{auto p=Field(t,s);return p?U8(*p):std::nullopt;}
    std::optional<uint32_t> Word(size_t t,unsigned s)const{auto p=Field(t,s);return p?U32(*p):std::nullopt;}
    std::optional<std::string> String(size_t t,unsigned s)const{
        auto p=Field(t,s);auto q=p?Indirect(*p):std::nullopt;if(!q)return {};
        auto n=U32(*q);if(!n||*n>65536||!Range(*q+4,size_t(*n)+1)||b[*q+4+*n])return {};
        return std::string(reinterpret_cast<const char*>(b.data()+*q+4),*n);
    }
    std::vector<size_t> Tables(size_t t,unsigned s)const {
        std::vector<size_t> out;auto p=Field(t,s);auto v=p?Indirect(*p):std::nullopt;if(!v)return out;
        auto n=U32(*v);if(!n||*n>16384||!Range(*v+4,size_t(*n)*4))return out;
        for(unsigned i=0;i<*n;i++){auto child=Indirect(*v+4+4*i);if(!child)return {};out.push_back(*child);}return out;
    }
};
}
