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
    uint64_t trigger{},change{},replacement{};unsigned reads{},on_read{1};
    template<class T>void Put(uint64_t p,const T& v){assert(p>=base&&p-base+sizeof(v)<=bytes.size());std::memcpy(bytes.data()+p-base,&v,sizeof(v));}
    void Q(uint64_t p,uint64_t v){Put(p,v);}void W(uint64_t p,uint32_t v){Put(p,v);}void F(uint64_t p,float v){Put(p,v);}
};
EdenDsmodBool Mapped(void* p,uint64_t a,uint64_t n){auto& m=*static_cast<Memory*>(p);return a>=m.base&&a-m.base<=m.bytes.size()&&n<=m.bytes.size()-(a-m.base);}
EdenDsmodBool Read(void* p,uint64_t a,void* b,size_t n){if(!Mapped(p,a,n))return false;auto& m=*static_cast<Memory*>(p);std::memcpy(b,m.bytes.data()+a-m.base,n);if(a==m.trigger&&++m.reads==m.on_read)m.Q(m.change,m.replacement);return true;}
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
    // Scope qualification rejects a retained scene, wrong controller and ambiguous owners.
    const uint64_t owner=m.base+0x6000,camp=m.base+0x6500,render=m.base+0x7000,render_control=m.base+0x7800,
        world=m.base+0x8000,world_control=world-32,hud=m.base+0xa020,records=m.base+0xb000,rail_control=m.base+0xc000;
    auto observation=*scn::ReadSnapCandidate(h,scene);
    m.Q(owner,m.main+0xc800b0);m.Q(owner+0x170,scene);m.Q(owner+0x178,scene-32);m.Q(scene+0x238,owner);
    m.Q(scene+0x1a8,render);m.Q(scene+0x1b0,render_control);m.Q(render_control+8,0);
    auto course_scope=scn::ReadSnapScope(h,observation);assert(course_scope&&course_scope->render==render&&!course_scope->world);
    m.Q(owner+0x178,scene);assert(!scn::ReadSnapScope(h,observation));m.Q(owner+0x178,scene-32);
    m.Q(camp,m.main+0xc89f88);m.Q(camp+0x90,scene);m.Q(camp+0x98,scene-32);m.W(camp+0x80,30);m.Q(scene+0x228,camp);
    m.W(scene+0x25c,30);m.W(rail+0x1e0,30);observation=*scn::ReadSnapCandidate(h,scene);
    assert(!scn::ReadSnapScope(h,observation));m.Q(scene+0x238,0);
    m.Q(camp+0xa0,world);m.Q(camp+0xa8,world_control);m.Q(world,m.main+0xc7f5e0);m.Q(world_control,m.main+0xc7ac38);m.Q(world_control+8,0);
    auto scope=scn::ReadSnapScope(h,observation);assert(scope&&scope->kind==scn::SnapScopeKind::Camp&&scope->world==world);
    assert(!scope->expedition);assert(!scn::ReadRemainingPhotos(h,*scope,observation));
    m.Q(camp+0x38,m.main+0x61e8d0);assert(!scn::SameSnapScope(h,*scope,observation));scope=scn::ReadSnapScope(h,observation);assert(scope&&scope->expedition);
    m.Q(world+0xa8,records);m.Q(world+0xb0,records+16);m.Q(records,hud);m.Q(records+8,hud-32);
    m.Q(hud,m.main+0xc7df90);m.Q(hud-32,m.main+0xc7e150);m.Q(hud-24,0);m.Q(hud+0x68,rail);m.Q(hud+0x70,rail_control);
    m.Q(scene+0x88,rail_control);m.Q(rail_control+8,0);m.Q(scene+0x38,m.main+0x630240);m.W(hud+0x2b8,72);
    assert(scn::ReadRemainingPhotos(h,*scope,observation)==72);
    m.W(hud+0x2b8,71);assert(scn::ReadRemainingPhotos(h,*scope,observation)==71);
    m.W(hud+0x2b8,0);assert(scn::ReadRemainingPhotos(h,*scope,observation)==0);
    m.W(hud+0x2b8,73);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.W(hud+0x2b8,72);
    m.Q(hud+0x70,rail_control+16);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(hud+0x70,rail_control);
    m.Q(hud-24,UINT64_MAX);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(hud-24,0);
    m.Q(records+8,hud);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(records+8,hud-32);
    m.Q(world+0xb0,records+17);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(world+0xb0,records+16);
    m.Q(scene+0x80,rail+16);assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(scene+0x80,rail);
    m.Q(camp+0x90,scene+0x3000);assert(!scn::SameSnapScope(h,*scope,observation));assert(!scn::ReadRemainingPhotos(h,*scope,observation));m.Q(camp+0x90,scene);
    m.Q(scene+0x38,m.main+0x62e100);assert(!scn::ReadRemainingPhotos(h,*scope,observation));
    m.Q(world+0xb0,records+32);m.Q(records+16,hud);m.Q(records+24,hud-32);m.Q(scene+0x38,m.main+0x62e180);assert(!scn::ReadRemainingPhotos(h,*scope,observation));
    m.Q(camp+0x38,m.main+0x454ee0);assert(!scn::SameSnapScope(h,*scope,observation));assert(!scn::ReadRemainingPhotos(h,*scope,observation));
    // Standard courses find the exact named HUD alias, then independently prove
    // its weak world is alive and that world's shared-owned list contains it.
    m.Q(scene+0x228,0);m.Q(scene+0x238,owner);m.W(scene+0x25c,1);m.W(rail+0x1e0,1);
    observation=*scn::ReadSnapCandidate(h,scene);course_scope=scn::ReadSnapScope(h,observation);assert(course_scope);
    const uint64_t registry=m.base+0xd000,registry_control=m.base+0xd100,root=m.base+0xe000,node=m.base+0xe100;
    m.Q(render+0x30,registry);m.Q(render+0x38,registry_control);m.Q(registry_control+8,0);m.Q(registry+8,root);m.Q(registry+0x10,2);
    m.W(root+0x20,0xc0000000);m.Q(root+8,node);m.W(node+0x20,0xcfc2a4c4);m.Q(node+0x28,hud+0x40);m.Q(node+0x30,hud-32);
    m.Q(hud+0x40,m.main+0xc7e038);m.Q(hud+0x20,world);m.Q(hud+0x28,world_control);m.Q(world+0xb0,records+16);
    assert(scn::ReadRemainingPhotos(h,*course_scope,observation)==72);
    m.W(hud+0x2b8,71);assert(scn::ReadRemainingPhotos(h,*course_scope,observation)==71);m.W(hud+0x2b8,72);
    m.W(node+0x38,1);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.W(node+0x38,0);
    m.W(node+0x20,0xcfc2a4c5);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.W(node+0x20,0xcfc2a4c4);
    m.Q(root+8,root);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(root+8,node);
    m.Q(registry+0x10,513);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(registry+0x10,2);
    m.Q(node+0x30,hud);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(node+0x30,hud-32);
    m.Q(hud+0x40,0);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(hud+0x40,m.main+0xc7e038);
    m.Q(world_control+8,UINT64_MAX);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(world_control+8,0);
    m.Q(world_control,0);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(world_control,m.main+0xc7ac38);
    m.Q(registry_control+8,UINT64_MAX);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(registry_control+8,0);
    m.Q(records+8,hud);assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(records+8,hud-32);
    // Simulate concurrent retry/replacement after reading the counter. A valid
    // first observation cannot authorize the value from a replaced registry,
    // reallocated owned list, changed counter or scene transition.
    m.trigger=hud+0x2b8;m.change=node+0x28;m.replacement=hud+0x48;m.reads=0;
    assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(node+0x28,hud+0x40);
    m.change=world+0xb0;m.replacement=records+32;m.reads=0;
    assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(world+0xb0,records+16);
    m.change=hud+0x2b8;m.replacement=70;m.reads=0;
    assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.W(hud+0x2b8,72);
    m.change=scene+0x38;m.replacement=m.main+0x62e100;m.reads=0;
    assert(!scn::ReadRemainingPhotos(h,*course_scope,observation));m.Q(scene+0x38,m.main+0x62e180);m.trigger=0;
    assert(scn::ReadRemainingPhotos(h,*course_scope,observation)==72);
    uint64_t second=m.base+0x18020;std::memcpy(m.bytes.data()+second-m.base-32,m.bytes.data()+scene-m.base-32,0x2f0);
    scn::SnapDiscovery ambiguous;for(unsigned i=0;i<128;i++){++m.tick;assert(!ambiguous.Step(h));}
    std::cout<<"Snap candidate checks passed: bounded discovery, controller ownership/ambiguity, named HUD registry, shared world/HUD lifetime, concurrent replacement/retry guards, counter bounds and curve identity. No live-game validation claimed.\n";
}
