// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dsmod_module_abi.h"
#include <array>
#include <optional>
namespace scn {
using Vec3=std::array<float,3>;
struct SnapObservation {
    uint64_t scene{},rail{},curve{};
    unsigned course{},selected_level{};
    float distance{},curve_length{};
    std::optional<Vec3> world;
    Vec3 forward{};
};
enum class SnapScopeKind { Course, Camp };
struct SnapScope {
    SnapScopeKind kind{};
    uint64_t owner{},scene{},scene_control{},render{},render_control{},world{},world_control{};
};
// Exact-build layouts. APK hardware evidence and the optional Duo-module's separate
// qualification are recorded in docs/THOR_READER_VALIDATION.md.
std::optional<Vec3> ReadRailPosition(const EdenDsmodHostApi&,uint64_t curve,float distance);
std::optional<SnapObservation> ReadSnapCandidate(const EdenDsmodHostApi&,uint64_t scene);
std::optional<SnapScope> ReadSnapScope(const EdenDsmodHostApi&,const SnapObservation&);
bool SameSnapScope(const EdenDsmodHostApi&,const SnapScope&,const SnapObservation&);
std::optional<unsigned> ReadRemainingPhotos(const EdenDsmodHostApi&,const SnapScope&,const SnapObservation&);
class SnapDiscovery {
    uint64_t cursor{},end{},candidate{},last_tick{};
    unsigned matches{};
    bool complete{};
public:
    std::optional<SnapObservation> Step(const EdenDsmodHostApi&);
};
}
