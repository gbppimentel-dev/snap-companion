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
// Exact-build static evidence, not an on-device validation claim. Output is diagnostic-only
// until the separately documented phase/lifetime and projection checks pass on Eden Duo.
std::optional<Vec3> ReadRailPosition(const EdenDsmodHostApi&,uint64_t curve,float distance);
std::optional<SnapObservation> ReadSnapCandidate(const EdenDsmodHostApi&,uint64_t scene);
class SnapDiscovery {
    uint64_t cursor{},end{},candidate{},last_tick{};
    unsigned matches{};
    bool complete{};
public:
    std::optional<SnapObservation> Step(const EdenDsmodHostApi&);
};
}
