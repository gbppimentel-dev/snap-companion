// SPDX-License-Identifier: GPL-3.0-or-later
#include "snap_runtime.h"
#include <cassert>
#include <cstring>
#include <iostream>
#include <vector>
namespace {
struct Memory {
    uint64_t base=0x40000000,main=0x10000000,tick=0;
    std::vector<uint8_t> bytes=std::vector<uint8_t>(0x40000);
    template<class T>void Put(uint64_t p,const T& v){assert(p>=base&&p-base+sizeof(v)<=bytes.size());std::memcpy(bytes.data()+p-base,&v,sizeof(v));}
    void Q(uint64_t p,uint64_t v){Put(p,v);}void W(uint64_t p,uint32_t v){Put(p,v);}void F(uint64_t p,float v){Put(p,v);}
};
EdenDsmodBool Mapped(void* p,uint64_t a,uint64_t n){auto& m=*static_cast<Memory*>(p);return a>=m.base&&a-m.base<=m.bytes.size()&&n<=m.bytes.size()-(a-m.base);}
EdenDsmodBool Read(void* p,uint64_t a,void* b,size_t n){if(!Mapped(p,a,n))return false;auto& m=*static_cast<Memory*>(p);std::memcpy(b,m.bytes.data()+a-m.base,n);return true;}
const uint8_t* Pointer(void* p,uint64_t a,size_t n){auto& m=*static_cast<Memory*>(p);return Mapped(p,a,n)?m.bytes.data()+a-m.base:nullptr;}
uint64_t Begin(void* p){return static_cast<Memory*>(p)->base;}uint64_t End(void* p){auto& m=*static_cast<Memory*>(p);return m.base+m.bytes.size();}uint64_t Tick(void* p){return static_cast<Memory*>(p)->tick;}
}
int main(){
    Memory m;EdenDsmodHostApi h{};h.userdata=&m;h.main_base=m.main;h.is_mapped=Mapped;h.read_memory=Read;h.get_read_pointer=Pointer;h.get_heap_begin=Begin;h.get_heap_end=End;h.get_tick=Tick;
    uint64_t scene=m.base+0x10020,rail=m.base+0x20000,curve=m.base+0x21000,part=m.base+0x22000,points=m.base+0x23000;
    m.Q(scene,m.main+0xc8a050);m.Q(scene-32,m.main+0xc80138);m.Q(scene-24,0);m.Q(scene+0x38,m.main+0x62e100);
    m.W(scene+0x25c,2);m.W(scene+0x26c,3);m.Q(scene+0x80,rail);m.Q(rail,m.main+0xc81e28);m.W(rail+0x1e0,2);m.Q(rail+0x30,curve);m.F(rail+0x70,5);
    m.Put(rail+0x1a0,scn::Vec3{1,0,0});m.F(curve+0x3c,10);m.Q(curve+0x40,part);m.Q(curve+0x48,part+56);m.W(curve+0x70,0);
    m.F(part+4,10);m.F(part+20,10);m.Q(part+24,points);m.Q(part+32,points+96);m.W(part+48,1);
    m.Put(points,scn::Vec3{0,0,0});m.Put(points+16,scn::Vec3{1,0,0});m.F(points+32,10);m.F(points+36,10);
    m.Put(points+48,scn::Vec3{10,0,0});m.Put(points+64,scn::Vec3{1,0,0});m.F(points+80,10);m.F(points+84,0);
    auto found=scn::ReadSnapCandidate(h,scene);assert(found&&found->course==2&&found->selected_level==3&&found->world&&(*found->world)[0]==5);
    scn::SnapDiscovery discovery;std::optional<scn::SnapObservation> discovered;
    for(unsigned i=0;i<128&&!discovered;i++){++m.tick;discovered=discovery.Step(h);}assert(discovered);
    m.Q(scene,0);assert(!discovery.Step(h));m.Q(scene,m.main+0xc8a050);
    m.Q(scene-24,UINT64_MAX);assert(!scn::ReadSnapCandidate(h,scene));m.Q(scene-24,0);
    m.W(rail+0x1e0,1);assert(!scn::ReadSnapCandidate(h,scene));m.W(rail+0x1e0,2);
    m.W(scene+0x26c,0);assert(!scn::ReadSnapCandidate(h,scene));m.W(scene+0x26c,3);
    m.W(curve+0x70,1);assert(!scn::ReadRailPosition(h,curve,5));m.W(curve+0x70,0);
    m.Q(points+8,0);assert(scn::ReadRailPosition(h,curve,5));
    m.Q(curve+0x48,part+57);assert(!scn::ReadRailPosition(h,curve,5));m.Q(curve+0x48,part+56);
    uint64_t second=m.base+0x18020;std::memcpy(m.bytes.data()+second-m.base-32,m.bytes.data()+scene-m.base-32,0x2f0);
    scn::SnapDiscovery ambiguous;for(unsigned i=0;i<128;i++){++m.tick;assert(!ambiguous.Step(h));}
    std::cout<<"Snap candidate checks passed: bounded discovery, ambiguity, expired ownership, identity, selected-level and curve guards. No live-game validation claimed.\n";
}
