// SPDX-License-Identifier: GPL-3.0-or-later
#include "snap_runtime.h"
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstring>
#include <limits>

namespace scn {
namespace {
constexpr uint64_t scene_vtable=0xc8a050,rail_vtable=0xc81e28,scene_shared_vtable=0xc80138;
bool Address(uint64_t p,size_t n) { return p>=0x10000&&p<(uint64_t(1)<<48)&&n<=(uint64_t(1)<<48)-p; }
template<class T> bool Read(const EdenDsmodHostApi& h,uint64_t p,T& v) {
    v={};return Address(p,sizeof(v))&&h.is_mapped&&h.read_memory&&h.is_mapped(h.userdata,p,sizeof(v))&&h.read_memory(h.userdata,p,&v,sizeof(v));
}
bool Ptr(const EdenDsmodHostApi& h,uint64_t p,uint64_t& v){return Read(h,p,v)&&Address(v,1)&&!(v&7);}
bool Finite(const Vec3& v){for(float x:v)if(!std::isfinite(x)||std::abs(x)>1e6f)return false;return true;}
uint64_t DataAddress(const EdenDsmodHostApi& h,uint64_t rva) {
    int64_t d=h.get_i64?h.get_i64(h.userdata,"__relocation_delta",0):0;
    const uint64_t base=h.main_base+rva;
    if(d>=0)return uint64_t(d)>UINT64_MAX-base?0:base+uint64_t(d);
    uint64_t n=uint64_t(-(d+1))+1;return n>base?0:base-n;
}
bool Count(uint64_t begin,uint64_t end,size_t stride,unsigned max,unsigned& out) {
    if(end<begin||(end-begin)%stride||(end-begin)/stride>max)return false;
    out=unsigned((end-begin)/stride);return out>0&&Address(begin,size_t(end-begin));
}
struct Node { Vec3 point{},tangent{};float end{},length{}; };
bool GetNode(const EdenDsmodHostApi& h,uint64_t p,Node& n) {
    return Read(h,p,n.point)&&Read(h,p+16,n.tangent)&&Read(h,p+32,n.end)&&Read(h,p+36,n.length)&&
        Finite(n.point)&&Finite(n.tangent)&&std::isfinite(n.end)&&std::isfinite(n.length)&&n.end>=0&&n.length>=0;
}
}
std::optional<Vec3> ReadRailPosition(const EdenDsmodHostApi& h,uint64_t curve,float distance) {
    // main+3e5a70 -> segment table -> main+4f8660 (Hermite) / 4f90e0 (linear).
    // The measured rail distance is the input. No elapsed-time/ride-speed estimate is used.
    uint32_t mode=0;float length=0;uint64_t begin=0,end=0;
    if(!Address(curve,0x74)||!Read(h,curve+0x70,mode)||mode!=0||!Read(h,curve+0x3c,length)||
       !std::isfinite(length)||length<=0||length>1e6f||!std::isfinite(distance)||
       distance<0||distance>length+0.1f||!Ptr(h,curve+0x40,begin)||!Read(h,curve+0x48,end))return {};
    unsigned segments=0;if(!Count(begin,end,56,1024,segments))return {};
    unsigned lo=0,hi=segments;
    while(lo<hi){unsigned mid=lo+(hi-lo)/2;float bound=0;if(!Read(h,begin+56*mid+4,bound)||!std::isfinite(bound))return {};if(bound>distance)hi=mid;else lo=mid+1;}
    unsigned index=std::min(lo,segments-1);uint64_t part=begin+56*index,points=0,points_end=0;
    float bound=0,part_length=0;uint32_t interpolation=0;
    if(!Read(h,part+4,bound)||!Read(h,part+20,part_length)||!Read(h,part+48,interpolation)||
       !std::isfinite(bound)||!std::isfinite(part_length)||part_length<=0||bound>length+0.1f||
       interpolation>1||!Ptr(h,part+24,points)||!Read(h,part+32,points_end))return {};
    unsigned count=0;if(!Count(points,points_end,48,8192,count))return {};
    const float t=std::clamp(1.f-(bound-distance)/part_length,0.f,1.f);
    Node a,b;
    if(count==1||t<=0){if(!GetNode(h,points,a))return {};return a.point;}
    if(t>=1){if(!GetNode(h,points+48*(count-1),a))return {};return a.point;}
    float d=t*part_length;lo=0;hi=count;
    while(lo<hi){unsigned mid=lo+(hi-lo)/2;float v=0;if(!Read(h,points+48*mid+32,v)||!std::isfinite(v))return {};if(v>d)hi=mid;else lo=mid+1;}
    if(lo>=count)return {};
    if(!GetNode(h,points+48*lo,a))return {};
    if(a.length<1e-6f)return a.point;
    const float u=std::clamp(1.f-(a.end-d)/a.length,0.f,1.f);
    Vec3 result{};
    if(interpolation==1){for(unsigned c=0;c<3;c++)result[c]=a.point[c]+u*a.length*a.tangent[c];}
    else {
        if(lo+1>=count||!GetNode(h,points+48*(lo+1),b))return {};
        const float u2=u*u,u3=u2*u,h00=2*u3-3*u2+1,h10=u3-2*u2+u,h01=-2*u3+3*u2,h11=u3-u2;
        for(unsigned c=0;c<3;c++)result[c]=h00*a.point[c]+h10*a.length*a.tangent[c]+h01*b.point[c]+h11*a.length*b.tangent[c];
    }
    if(!Finite(result))return {};
    // Reject replacement/reallocation of the curve's segment vector during this read.
    uint64_t after=0;if(!Read(h,curve+0x40,after)||after!=begin||!Read(h,curve+0x48,after)||after!=end)return {};
    return result;
}
std::optional<SnapObservation> ReadSnapCandidate(const EdenDsmodHostApi& h,uint64_t scene) {
    uint64_t vt=0,rail=0,curve=0,phase=0,control_vt=0;int64_t strong=-1;
    uint32_t course=0,level=0,rail_course=0;float distance=0,length=0;
    if(!Address(scene,0x2d0)||scene<32||!Read(h,scene,vt)||vt!=DataAddress(h,scene_vtable)||
       !Read(h,scene-32,control_vt)||control_vt!=DataAddress(h,scene_shared_vtable)||
       !Read(h,scene-24,strong)||strong<0||strong>10000||
       !Read(h,scene+0x38,phase)||phase<h.main_base+0x624420||phase>=h.main_base+0x633000||
       !Read(h,scene+0x25c,course)||course<1||course>30||!Read(h,scene+0x26c,level)||level<1||level>4||
       !Ptr(h,scene+0x80,rail)||!Read(h,rail,vt)||vt!=DataAddress(h,rail_vtable)||
       !Read(h,rail+0x1e0,rail_course)||rail_course!=course||!Ptr(h,rail+0x30,curve)||
       !Read(h,rail+0x70,distance)||!Read(h,curve+0x3c,length)||!std::isfinite(distance)||!std::isfinite(length)||
       distance<0||length<=0||length>1e6f||distance>length+0.1f)return {};
    SnapObservation out{scene,rail,curve,course,level,distance,length,{}, {}};
    out.world=ReadRailPosition(h,curve,distance);
    if(!Read(h,rail+0x1a0,out.forward)||!Finite(out.forward))out.forward={};
    uint32_t value=0;uint64_t pointer=0;
    if(!Read(h,scene+0x25c,value)||value!=course||!Read(h,scene+0x26c,value)||value!=level||
       !Read(h,scene+0x80,pointer)||pointer!=rail||!Read(h,rail+0x30,pointer)||pointer!=curve||
       !Read(h,scene,vt)||vt!=DataAddress(h,scene_vtable))return {};
    return out;
}
std::optional<SnapScope> ReadSnapScope(const EdenDsmodHostApi& h,const SnapObservation& observation) {
    const uint64_t scene=observation.scene;uint64_t vt=0;int64_t strong=-1;
    if(!Read(h,scene,vt)||vt!=DataAddress(h,scene_vtable)||scene<32||
       !Read(h,scene-32,vt)||vt!=DataAddress(h,scene_shared_vtable)||
       !Read(h,scene-24,strong)||strong<0||strong>10000)return {};
    struct Profile {SnapScopeKind kind;uint64_t type;unsigned capture,current;};
    const Profile profiles[]={{SnapScopeKind::Course,0xc800b0,0x238,0x170},{SnapScopeKind::Camp,0xc89f88,0x228,0x90}};
    std::optional<SnapScope> result;
    for(const auto& p:profiles){
        uint64_t owner=0,current=0,control=0,after=0;unsigned course=0;
        if(p.kind==SnapScopeKind::Camp&&observation.course!=30)continue;
        if(!Ptr(h,scene+p.capture,owner)||!Read(h,owner,vt)||vt!=DataAddress(h,p.type)||
           !Read(h,owner+p.current,current)||current!=scene||!Read(h,owner+p.current+8,control)||control!=scene-32)continue;
        if(p.kind==SnapScopeKind::Camp&&(!Read(h,owner+0x80,course)||course!=30))continue;
        SnapScope scope{p.kind,owner,scene,control};
        uint64_t render=0,render_control=0;
        if(Ptr(h,scene+0x1a8,render)&&Ptr(h,scene+0x1b0,render_control)&&
           Read(h,render_control+8,strong)&&strong>=0&&strong<10000){
            if(!Read(h,scene+0x1a8,after)||after!=render||!Read(h,scene+0x1b0,after)||after!=render_control)return {};
            scope.render=render;scope.render_control=render_control;
        }
        // Camp owns its HUD world separately from the shared render context.
        // The standard course's owner+0x98 is that render context, not this world.
        uint64_t world=0,world_control=0;
        if(p.kind==SnapScopeKind::Camp&&Ptr(h,owner+0xa0,world)&&Ptr(h,owner+0xa8,world_control)&&
           Read(h,world,vt)&&vt==DataAddress(h,0xc7f5e0)&&Read(h,world_control+8,strong)&&strong>=0&&strong<10000){
            if(!Read(h,owner+0xa0,after)||after!=world||!Read(h,owner+0xa8,after)||after!=world_control)return {};
            scope.world=world;scope.world_control=world_control;
        }
        if(!Read(h,owner,vt)||vt!=DataAddress(h,p.type)||!Read(h,scene+p.capture,after)||after!=owner||
           !Read(h,owner+p.current,after)||after!=scene||!Read(h,owner+p.current+8,after)||after!=control)return {};
        if(result)return {}; // Two different current owners are ambiguous, not a priority list.
        result=scope;
    }
    return result;
}
bool SameSnapScope(const EdenDsmodHostApi& h,const SnapScope& scope,const SnapObservation& observation) {
    auto current=ReadSnapScope(h,observation);
    return current&&current->kind==scope.kind&&current->owner==scope.owner&&current->scene==scope.scene&&
        current->scene_control==scope.scene_control&&current->render==scope.render&&current->render_control==scope.render_control&&current->world==scope.world&&current->world_control==scope.world_control;
}
std::optional<unsigned> ReadRemainingPhotos(const EdenDsmodHostApi& h,const SnapScope& scope,const SnapObservation& observation) {
    uint64_t phase=0,begin=0,end=0,after=0,rail_control=0;int64_t strong=-1;
    if(!scope.world||!SameSnapScope(h,scope,observation)||!Read(h,observation.scene+0x38,phase)||
       (phase!=h.main_base+0x62e180&&phase!=h.main_base+0x630240)||
       !Ptr(h,observation.scene+0x88,rail_control)||!Read(h,rail_control+8,strong)||strong<0||strong>10000||
       !Ptr(h,scope.world+0xa8,begin)||!Read(h,scope.world+0xb0,end))return {};
    // main+3f0edc destroys this vector of shared-owned objects (16-byte entries).
    // The separate +0x60 callback list holds raw captures and is not an ownership list.
    unsigned count=0;if(!Count(begin,end,16,128,count))return {};
    unsigned matches=0,result=0;uint64_t selected=0,selected_entry=0;
    for(unsigned i=0;i<count;i++){
        const uint64_t entry=begin+16*i;uint64_t hud=0,control=0,vt=0,rail=0,weak=0;int remaining=-1;
        if(!Ptr(h,entry,hud)||hud<32||!Read(h,entry+8,control)||control!=hud-32||
           !Read(h,hud,vt)||vt!=DataAddress(h,0xc7df90)||!Read(h,control,vt)||vt!=DataAddress(h,0xc7e150)||
           !Read(h,control+8,strong)||strong<0||strong>10000||!Read(h,hud+0x68,rail)||rail!=observation.rail||
           !Read(h,hud+0x70,weak)||weak!=rail_control||!Read(h,hud+0x2b8,remaining)||remaining<0||remaining>72)continue;
        ++matches;result=unsigned(remaining);selected=hud;selected_entry=entry;
    }
    int remaining=-1;
    if(matches!=1||!Read(h,selected_entry,after)||after!=selected||!Read(h,selected_entry+8,after)||after!=selected-32||
       !Read(h,selected,after)||after!=DataAddress(h,0xc7df90)||!Read(h,selected-32,after)||after!=DataAddress(h,0xc7e150)||
       !Read(h,selected-24,strong)||strong<0||strong>10000||!Read(h,selected+0x2b8,remaining)||remaining!=int(result)||
       !Read(h,selected+0x68,after)||after!=observation.rail||!Read(h,selected+0x70,after)||after!=rail_control||
       !Read(h,scope.world+0xa8,after)||after!=begin||!Read(h,scope.world+0xb0,after)||after!=end||
       !Read(h,observation.scene+0x80,after)||after!=observation.rail||!Read(h,observation.scene+0x88,after)||after!=rail_control||
       !Read(h,observation.scene+0x38,after)||after!=phase||!SameSnapScope(h,scope,observation))return {};
    return result;
}
std::optional<SnapObservation> SnapDiscovery::Step(const EdenDsmodHostApi& h) {
    // Diagnostic-only. One bounded sweep, with a 0.4 ms soft time slice checked every
    // 16 pages and an 8 MiB hard byte cap. Actual host/read costs still need measurement.
    const uint64_t tick=h.get_tick?h.get_tick(h.userdata):0;
    if(candidate){auto value=ReadSnapCandidate(h,candidate);if(value&&complete)return value;if(!value){candidate=0;complete=false;cursor=0;matches=0;}}
    if(complete&&tick>=last_tick&&tick-last_tick<120)return {};
    if(complete){complete=false;cursor=0;matches=0;candidate=0;}
    if(!h.get_heap_begin||!h.get_heap_end||!h.get_read_pointer)return {};
    const uint64_t start=h.get_heap_begin(h.userdata),limit=h.get_heap_end(h.userdata);
    if(!Address(start,1)||limit<=start||limit-start>uint64_t(8)*1024*1024*1024)return {};
    if(!cursor||cursor<start||end!=limit){cursor=(start+4095)&~uint64_t(4095);end=limit;candidate=0;matches=0;}
    const uint64_t vt=DataAddress(h,scene_vtable);
    const auto deadline=std::chrono::steady_clock::now()+std::chrono::microseconds(400);
    for(unsigned page=0;page<2048&&cursor<end;page++,cursor+=4096) {
        if(page&&!(page&15)&&std::chrono::steady_clock::now()>=deadline)break;
        const size_t n=std::min<uint64_t>(4096,end-cursor);const uint8_t* bytes=h.get_read_pointer(h.userdata,cursor,n);
        if(!bytes)continue;
        // memchr uses the platform's vectorized byte search. Only aligned hits can be pointers.
        for(size_t off=0;off+8<=n;){
            const auto* hit=static_cast<const uint8_t*>(std::memchr(bytes+off,int(vt&255),n-off-7));
            if(!hit)break;const size_t i=size_t(hit-bytes);off=i+1;
            if((cursor+i)&7)continue;uint64_t value=0;std::memcpy(&value,hit,8);
            if(value==vt&&ReadSnapCandidate(h,cursor+i)){++matches;candidate=cursor+i;}
        }
    }
    if(cursor>=end){complete=true;last_tick=tick;if(matches!=1)candidate=0;}
    return complete&&candidate?ReadSnapCandidate(h,candidate):std::nullopt;
}
}
