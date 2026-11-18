// SPDX-License-Identifier: GPL-3.0-or-later
#include "dsmod_module_extensions.h"
#include "live_reader.h"
#include "map_art.h"
#include "catalog.h"
#include "snap_runtime.h"
#include <cstdio>
#include <cstring>
#include <memory>
#include <string>

namespace {
struct Module {
    std::optional<scn::CourseCatalog> catalog;
    std::optional<scn::Layout> layout=scn::ProductionLayout();
    std::optional<scn::Run> run;
#ifdef SCN_DIAGNOSTIC_READER
    scn::SnapDiscovery discovery;
    std::optional<scn::SnapObservation> candidate;
    uint64_t last_log{};
#endif
    uint64_t last_tick{};
    bool sampled{};
};
EdenDsmodBool Supports(const char* b) { try{return scn::SupportsBuild(b);}catch(...){return false;} }
void* CreateImpl(const EdenDsmodHostApi* h,const char*) {
    if(!h || h->struct_size<sizeof(EdenDsmodHostApi) || h->title_id!=scn::kTitleId || h->abi_version!=1 || h->abi_hash!=EDEN_DSMOD_MODULE_ABI_HASH) return nullptr;
    char id[65]{}; for(unsigned i=0;i<32;i++) std::snprintf(id+i*2,3,"%02X",h->build_id[i]);
    if(!scn::SupportsBuild(id)) return nullptr;
    auto module=std::make_unique<Module>();
    module->catalog=scn::CourseCatalog::Load(*h);
    return module.release();
}
void* Create(const EdenDsmodHostApi* h,const char* config) { try{return CreateImpl(h,config);}catch(...){return nullptr;} }
void Destroy(void* p) { delete static_cast<Module*>(p); }
void SampleImpl(void* raw,const EdenDsmodHostApi* h) {
    if(!raw || !h) return;
    auto& m=*static_cast<Module*>(raw);
    const uint64_t tick=h->get_tick?h->get_tick(h->userdata):0;
    // Fixed small reads at 20 Hz, no heap scan, no screenshot/OCR service.
    if(!m.sampled || tick<m.last_tick || tick-m.last_tick>=3) {
        m.sampled=true; m.last_tick=tick;
        m.run=m.layout?scn::ReadRun(*h,*m.layout):std::nullopt;
#ifdef SCN_DIAGNOSTIC_READER
        m.candidate=m.discovery.Step(*h);
#endif
    }
    // The host clears its output each tick. Republish even when the reader is throttled.
    auto integer=[&](const char* n,int64_t v){ if(h->publish_i64)h->publish_i64(h->userdata,n,v); };
    auto text=[&](const char* n,const char* v){ if(h->publish_text)h->publish_text(h->userdata,n,v); };
    integer("scn.connected",1);
    integer("scn.catalog_ready",bool(m.catalog));
    integer("scn.course_count",m.catalog?m.catalog->courses.size():0);
    text("scn.connection_label","Connected");
    integer("scn.course_valid",0); integer("scn.map_valid",0); integer("scn.player_valid",0);
    integer("scn.pokemon_available",0); integer("scn.route_available",0);
    text("scn.course_name","Waiting for course");
    text("scn.course_detail","");
    text("scn.status",m.layout?"Waiting for course":"Live tracking is being prepared");
    text("scn.map_src",""); text("scn.route_src",""); text("scn.pokemon_src","");
    text("scn.player_pin_src","module:scn:player_pin");
#ifdef SCN_DIAGNOSTIC_READER
    // Candidate readings remain separate from trusted UI values. Static analysis does not
    // prove the active object's lifetime or the illustrated map's coordinate projection.
    integer("scn.candidate.present",bool(m.candidate));
    if(m.candidate){
        const auto& c=*m.candidate;integer("scn.candidate.course",c.course);integer("scn.candidate.level",c.selected_level);
        auto course=m.catalog?m.catalog->courses.find(c.course):std::map<unsigned,scn::CourseInfo>::const_iterator{};
        const std::string title=m.catalog&&course!=m.catalog->courses.end()?course->second.region+" · "+course->second.name:"Unknown course";
        text("scn.candidate.course_name",title.c_str());
        char position[120];
        if(c.world)std::snprintf(position,sizeof(position),"X %.3f    Y %.3f    Z %.3f",(*c.world)[0],(*c.world)[1],(*c.world)[2]);
        else std::snprintf(position,sizeof(position),"Position unavailable for this curve");
        text("scn.candidate.position",position);
        integer("scn.candidate.pose_valid",bool(c.world));
        if(h->publish_f64){h->publish_f64(h->userdata,"scn.candidate.distance",c.distance);h->publish_f64(h->userdata,"scn.candidate.length",c.curve_length);
            if(c.world)for(unsigned i=0;i<3;i++){char key[48];std::snprintf(key,sizeof(key),"scn.candidate.world_%c","xyz"[i]);h->publish_f64(h->userdata,key,(*c.world)[i]);}}
        if(h->log&&tick-m.last_log>=60){
            m.last_log=tick;char log[320];std::snprintf(log,sizeof(log),"SCN_DIAGNOSTIC unverified course=%u selected_level=%u distance=%.4f length=%.4f scene=0x%llx rail=0x%llx %s",c.course,c.selected_level,c.distance,c.curve_length,(unsigned long long)c.scene,(unsigned long long)c.rail,position);
            h->log(h->userdata,EDEN_DSMOD_LOG_INFO,log);
        }
    }
#endif
    // Do not turn raw IDs into a claimed detected course. A reviewed ROMFS course catalog,
    // variant mapping and projection must be connected together before enabling this UI.
}
void Sample(void* raw,const EdenDsmodHostApi* h) {
    try{SampleImpl(raw,h);}catch(...){
        if(raw){auto& m=*static_cast<Module*>(raw);m.run.reset();}
        if(h&&h->publish_i64){h->publish_i64(h->userdata,"scn.course_valid",0);h->publish_i64(h->userdata,"scn.map_valid",0);h->publish_i64(h->userdata,"scn.player_valid",0);h->publish_i64(h->userdata,"scn.candidate.present",0);}
    }
}
void Tick(void*,const EdenDsmodHostApi*) {}
void Configure(void*,const EdenDsmodHostExtensions*) {}
EdenDsmodBool Action(void*,const char*,int64_t) { return false; }
EdenDsmodBool LoadImageImpl(void*,const EdenDsmodHostApi* h,const char* key,void* receiver,EdenDsmodImageSink sink) {
    if(!h||!key||!sink) return false;
    if(std::strcmp(key,"module:scn:player_pin")==0) {
        // Authored locator, not a copied game sprite. The pin stays hidden without valid pose.
        std::vector<uint8_t> pixels(64*64*4,0);
        for(int y=0;y<64;y++) for(int x=0;x<64;x++) {
            const int dx=2*x-63,dy=2*y-63,rr=dx*dx+dy*dy;
            if(rr>58*58) continue;
            auto* p=pixels.data()+(y*64+x)*4;
            if(rr>46*46) { p[0]=255;p[1]=255;p[2]=247; }
            else { p[0]=36;p[1]=110;p[2]=114; }
            p[3]=255;
        }
        sink(receiver,64,64,pixels.data(),pixels.size());return true;
    }
    constexpr const char* prefix="module:scn:art/";
    if(std::strncmp(key,prefix,std::strlen(prefix))) return false;
    const char* number=key+std::strlen(prefix);
    // Canonical key: module:scn:art/01/course_map_l_001_1_A_1.nutexb
    if(number[0]<'0'||number[0]>'3'||number[1]<'0'||number[1]>'9'||number[2]!='/') return false;
    const unsigned archive=(number[0]-'0')*10+(number[1]-'0');
    std::vector<uint8_t> packed; scn::Image image;
    if(!scn::ExtractMap(*h,archive,number+3,packed)||!scn::DecodeMap(packed,image)) return false;
    sink(receiver,image.width,image.height,image.rgba.data(),image.rgba.size());
    return true;
}
EdenDsmodBool LoadImage(void* p,const EdenDsmodHostApi* h,const char* key,void* receiver,EdenDsmodImageSink sink) {
    try{return LoadImageImpl(p,h,key,receiver,sink);}catch(...){return false;}
}
#ifdef SCN_DIAGNOSTIC_READER
constexpr const char* kModuleName="SCN Read-only Validation reader 1";
#else
constexpr const char* kModuleName="Snap Companion Next (development)";
#endif
const EdenDsmodModuleApi kApi={1,sizeof(EdenDsmodModuleApi),0,EDEN_DSMOD_MODULE_ABI_HASH,scn::kTitleId,
    kModuleName,EDEN_DSMOD_CAP_ROMFS_READ|EDEN_DSMOD_CAP_EXTENSIONS|EDEN_DSMOD_CAP_NO_TICK_WHEN_HIDDEN,
    Supports,Create,Destroy,Sample,Tick};
const EdenDsmodModuleExtensions kExtensions={EDEN_DSMOD_EXT_VERSION,sizeof(EdenDsmodModuleExtensions),EDEN_DSMOD_EXT_HASH,
    Configure,Action,LoadImage};
}
extern "C" __attribute__((visibility("default"))) const EdenDsmodModuleApi* eden_dsmod_get_module(uint32_t v,uint64_t hash) {
    return v==1&&hash==EDEN_DSMOD_MODULE_ABI_HASH?&kApi:nullptr;
}
extern "C" __attribute__((visibility("default"))) const EdenDsmodModuleExtensions* eden_dsmod_get_extensions(uint32_t v,uint64_t hash) {
    return v==EDEN_DSMOD_EXT_VERSION&&hash==EDEN_DSMOD_EXT_HASH?&kExtensions:nullptr;
}
