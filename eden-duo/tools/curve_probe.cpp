// SPDX-License-Identifier: GPL-3.0-or-later
// Host-only test adapter. Never included in an installable module/package.
#include "snap_runtime.h"
#include <cstring>
namespace {
struct Memory {const uint8_t* data;size_t size;uint64_t base;};
EdenDsmodBool Mapped(void* p,uint64_t a,uint64_t n){auto& m=*static_cast<Memory*>(p);return a>=m.base&&a-m.base<=m.size&&n<=m.size-(a-m.base);}
EdenDsmodBool Read(void* p,uint64_t a,void* out,size_t n){if(!Mapped(p,a,n))return false;auto& m=*static_cast<Memory*>(p);std::memcpy(out,m.data+a-m.base,n);return true;}
}
extern "C" __attribute__((visibility("default"))) int scn_probe_curve(const uint8_t* memory,size_t size,uint64_t base,uint64_t curve,float distance,float* xyz){
    if(!memory||!xyz)return 0;Memory m{memory,size,base};EdenDsmodHostApi h{};h.userdata=&m;h.is_mapped=Mapped;h.read_memory=Read;
    auto result=scn::ReadRailPosition(h,curve,distance);if(!result)return 0;std::memcpy(xyz,result->data(),12);return 1;
}
