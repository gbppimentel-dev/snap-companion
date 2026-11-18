// SPDX-License-Identifier: GPL-3.0-or-later
#include "dsmod_module_extensions.h"
#include "live_reader.h"
#include "map_art.h"
#include <cstdio>
#include <cstring>
#include <memory>
#include <string>

namespace {
struct Module {
    std::optional<scn::Layout> layout=scn::ProductionLayout();
    std::optional<scn::Run> run;
    uint64_t last_tick{};
    bool sampled{};
};
EdenDsmodBool Supports(const char* b) { return scn::SupportsBuild(b); }
void* Create(const EdenDsmodHostApi* h,const char*) {
    if(!h || h->struct_size<sizeof(EdenDsmodHostApi) || h->title_id!=scn::kTitleId || h->abi_version!=1 || h->abi_hash!=EDEN_DSMOD_MODULE_ABI_HASH) return nullptr;
    char id[65]{}; for(unsigned i=0;i<32;i++) std::snprintf(id+i*2,3,"%02X",h->build_id[i]);
    if(!scn::SupportsBuild(id)) return nullptr;
    return new Module;
}
void Destroy(void* p) { delete static_cast<Module*>(p); }
void Sample(void* raw,const EdenDsmodHostApi* h) {
    if(!raw || !h) return;
    auto& m=*static_cast<Module*>(raw);
    const uint64_t tick=h->get_tick?h->get_tick(h->userdata):0;
    // Fixed small reads at 20 Hz, no heap scan, no screenshot/OCR service.
    if(!m.sampled || tick<m.last_tick || tick-m.last_tick>=3) {
        m.sampled=true; m.last_tick=tick;
        m.run=m.layout?scn::ReadRun(*h,*m.layout):std::nullopt;
    }
    // The host clears its output each tick. Republish even when the reader is throttled.
    auto integer=[&](const char* n,int64_t v){ if(h->publish_i64)h->publish_i64(h->userdata,n,v); };
    auto text=[&](const char* n,const char* v){ if(h->publish_text)h->publish_text(h->userdata,n,v); };
    integer("scn.connected",1);
    text("scn.connection_label","Connected");
    integer("scn.course_valid",0); integer("scn.map_valid",0); integer("scn.player_valid",0);
    integer("scn.pokemon_available",0); integer("scn.route_available",0);
    text("scn.course_name","Waiting for course");
    text("scn.course_detail","");
    text("scn.status",m.layout?"Waiting for course":"Live tracking is being prepared");
    text("scn.map_src",""); text("scn.route_src",""); text("scn.pokemon_src","");
    text("scn.player_pin_src","module:scn:player_pin");
    // Do not turn raw IDs into a claimed detected course. A reviewed ROMFS course catalog,
    // variant mapping and projection must be connected together before enabling this UI.
}
void Tick(void*,const EdenDsmodHostApi*) {}
void Configure(void*,const EdenDsmodHostExtensions*) {}
EdenDsmodBool Action(void*,const char*,int64_t) { return false; }
EdenDsmodBool LoadImage(void*,const EdenDsmodHostApi* h,const char* key,void* receiver,EdenDsmodImageSink sink) {
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
const EdenDsmodModuleApi kApi={1,sizeof(EdenDsmodModuleApi),0,EDEN_DSMOD_MODULE_ABI_HASH,scn::kTitleId,
    "Snap Companion Next (development)",EDEN_DSMOD_CAP_ROMFS_READ|EDEN_DSMOD_CAP_EXTENSIONS|EDEN_DSMOD_CAP_NO_TICK_WHEN_HIDDEN,
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
