// SPDX-License-Identifier: GPL-3.0-or-later
#include "dsmod_module_extensions.h"
#include "live_reader.h"
#include <cassert>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <map>
#include <string>

namespace {
struct Host { uint64_t tick{}; unsigned reads{},files{}; std::map<std::string,int64_t> ints; std::map<std::string,std::string> text; };
uint64_t Tick(void* p) { return static_cast<Host*>(p)->tick; }
void Int(void* p,const char* k,int64_t v) { static_cast<Host*>(p)->ints[k]=v; }
void Text(void* p,const char* k,const char* v) { static_cast<Host*>(p)->text[k]=v; }
EdenDsmodBool Read(void* p,uint64_t,void*,size_t) { ++static_cast<Host*>(p)->reads;return false; }
size_t File(void* p,const char*,uint64_t,void*,size_t) { ++static_cast<Host*>(p)->files;return 0; }
void Image(void*,uint32_t,uint32_t,const uint8_t*,size_t) { assert(false); }
void Pin(void* p,uint32_t w,uint32_t h,const uint8_t* rgba,size_t size) {
    assert(w==64&&h==64&&size==64*64*4);assert(rgba[3]==0&&rgba[(32*64+32)*4+3]==255);
    *static_cast<bool*>(p)=true;
}
}
int main() {
    assert(!eden_dsmod_get_module(2,EDEN_DSMOD_MODULE_ABI_HASH));
    assert(!eden_dsmod_get_module(1,0));
    auto* api=eden_dsmod_get_module(1,EDEN_DSMOD_MODULE_ABI_HASH);assert(api&&api->title_id==scn::kTitleId);
    assert(!(api->capabilities&EDEN_DSMOD_CAP_WRITE_MEMORY));assert(!(api->capabilities&EDEN_DSMOD_CAP_GUEST_CALL));
    auto* ext=eden_dsmod_get_extensions(EDEN_DSMOD_EXT_VERSION,EDEN_DSMOD_EXT_HASH);assert(ext);
    assert(!eden_dsmod_get_extensions(EDEN_DSMOD_EXT_VERSION,0));
    EdenDsmodHostApi h{};Host state;h.abi_version=1;h.struct_size=sizeof(h);h.abi_hash=EDEN_DSMOD_MODULE_ABI_HASH;
    h.userdata=&state;h.title_id=scn::kTitleId;h.get_tick=Tick;h.publish_i64=Int;h.publish_text=Text;h.read_memory=Read;h.read_romfs=File;
    assert(!api->create(&h,"{}"));
    for(unsigned i=0;i<32;i++) { unsigned x=0;std::sscanf(scn::kBuild+2*i,"%2x",&x);h.build_id[i]=uint8_t(x); }
    h.title_id=0;assert(!api->create(&h,"{}"));h.title_id=scn::kTitleId;
    void* instance=api->create(&h,"{}");assert(instance);
    assert(state.files==1);state.files=0; // One bounded metadata attempt at creation, never a per-frame file poll.
    for(unsigned tick=0;tick<60;tick++) {
        state.tick=tick;state.ints.clear();state.text.clear();api->sample(instance,&h);api->tick(instance,&h);
        assert(state.ints.at("scn.connected")==1&&state.ints.at("scn.course_valid")==0&&state.ints.at("scn.player_valid")==0);
        assert(state.text.at("scn.map_src").empty());assert(state.text.at("scn.course_name")=="Waiting for course");
    }
    assert(state.reads==0&&state.files==0); // Unverified profiles do not scan the game.
    assert(!ext->on_action(instance,"write",1));
    assert(!ext->load_image(instance,&h,"module:scn:art/01/../../save",nullptr,Image));
    assert(!ext->load_image(instance,&h,"module:scn:art/99/course_map_l_any",nullptr,Image));
    assert(state.files==0);
    bool pin=false;assert(ext->load_image(instance,&h,"module:scn:player_pin",&pin,Pin));assert(pin&&state.files==0);
    api->destroy(instance);
    std::cout<<"Module ABI checks passed: title/build negotiation, no writes, per-tick republication, unknown-state output, asset path guards.\n";
}
