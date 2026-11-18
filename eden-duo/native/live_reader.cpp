// SPDX-License-Identifier: GPL-3.0-or-later
#include "live_reader.h"
#include <cmath>
#include <cstring>
#include <limits>
#include <string>

namespace scn {
namespace {
bool Address(uint64_t p,size_t n) { return p>=0x10000 && p<(uint64_t(1)<<48) && n<=(uint64_t(1)<<48)-p; }
template<class T> bool Read(const EdenDsmodHostApi& h,uint64_t p,T& out) {
    out={};
    return Address(p,sizeof(T)) && h.is_mapped && h.read_memory &&
           h.is_mapped(h.userdata,p,sizeof(T)) && h.read_memory(h.userdata,p,&out,sizeof(T));
}
bool Ptr(const EdenDsmodHostApi& h,uint64_t p,uint64_t& out) {
    return Read(h,p,out) && Address(out,1) && !(out&7);
}
struct Identity { uint32_t phase{},course{},level{},route{},epoch{}; };
bool Id(const EdenDsmodHostApi& h,uint64_t p,const Layout& l,Identity& i) {
    return Read(h,p+l.phase,i.phase) && Read(h,p+l.course,i.course) && Read(h,p+l.level,i.level) &&
           Read(h,p+l.route,i.route) && Read(h,p+l.run_epoch,i.epoch);
}
bool Same(const Identity& a,const Identity& b) {
    return a.phase==b.phase && a.course==b.course && a.level==b.level && a.route==b.route && a.epoch==b.epoch;
}
}
bool SupportsBuild(const char* raw) {
    // Exact 32-byte NSO build. No prefix match or acceptance of other game patches.
    if(!raw || std::strlen(raw)!=64) return false;
    std::string id(raw);
    for(char& c:id) { if(c>='a'&&c<='f') c-=32; }
    return id==kBuild;
}
std::optional<Layout> ProductionLayout() {
    // Intentionally absent: prior OCR/saved-progress work did not verify a live root.
    // Add a profile only with the evidence in research/builds.json and docs/VALIDATION.md.
    return std::nullopt;
}
std::optional<Run> ReadRun(const EdenDsmodHostApi& h,const Layout& l) {
    if(!l.verified || !l.root_rva || !Address(h.main_base,l.root_rva+8) || l.root_rva>h.main_size || h.main_size-l.root_rva<8) return {};
    uint64_t root_address=h.main_base+l.root_rva;
    if(l.root_in_relocated_data) {
        const int64_t delta=h.get_i64?h.get_i64(h.userdata,"__relocation_delta",0):0;
        if(delta>=0) { if(uint64_t(delta)>UINT64_MAX-root_address) return {}; root_address+=uint64_t(delta); }
        else { const uint64_t magnitude=uint64_t(-(delta+1))+1; if(magnitude>root_address) return {}; root_address-=magnitude; }
    }
    uint64_t owner=0,player=0; Identity a{},b{};
    if(!Ptr(h,root_address,owner) || !Id(h,owner,l,a) || a.phase!=l.running_phase ||
       !a.course || !a.level || a.level>10 || a.route>1024 || !Ptr(h,owner+l.player_pointer,player)) return {};
    Run run; run.owner=owner; run.player=player;
    if(!Read(h,player+l.position,run.position) || !Read(h,player+l.forward,run.forward)) return {};
    // Reject a half-read course transition or released player object.
    uint64_t owner_after=0,player_after=0;
    if(!Id(h,owner,l,b) || !Same(a,b) || !Ptr(h,root_address,owner_after) || owner!=owner_after ||
       !Ptr(h,owner+l.player_pointer,player_after) || player!=player_after) return {};
    for(float v:run.position) if(!std::isfinite(v) || std::abs(v)>1e7f) return {};
    for(float v:run.forward) if(!std::isfinite(v) || std::abs(v)>2.f) return {};
    run.course=a.course; run.level=a.level; run.route=a.route; run.epoch=a.epoch;
    return run;
}
std::optional<Point> Project(const Run& r,const Projection& p) {
    if(!p.verified || !std::isfinite(p.width) || !std::isfinite(p.height) || p.width<=0 || p.height<=0) return {};
    double x=p.x[3],y=p.y[3],dx=0,dy=0;
    for(int i=0;i<3;i++) {
        if(!std::isfinite(r.position[i]) || !std::isfinite(r.forward[i])) return {};
        x+=p.x[i]*r.position[i]; y+=p.y[i]*r.position[i];
        dx+=p.x[i]*r.forward[i]; dy+=p.y[i]*r.forward[i];
    }
    if(!std::isfinite(x)||!std::isfinite(y)||x<0||y<0||x>p.width||y>p.height) return {};
    Point result{x,-y,0,false}; // Eden map coordinates are right/up.
    if(std::isfinite(dx)&&std::isfinite(dy)&&std::hypot(dx,dy)>1e-6) {
        result.heading=std::atan2(dx,-dy)*180.0/3.14159265358979323846;
        if(result.heading<0) result.heading+=360;
        result.heading_valid=true;
    }
    return result;
}
}
