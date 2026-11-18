// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dsmod_module_abi.h"
#include <array>
#include <cstdint>
#include <optional>

namespace scn {
constexpr uint64_t kTitleId=0x0100F4300BF2C000;
constexpr const char* kBuild="DDB900FF04A912ED42430508389853CFAF1ACCCE000000000000000000000000";
// No numerical address is considered valid without an evidence-reviewed build profile.
// Test layouts are synthetic and live only in tests/.
struct Layout {
    uint64_t root_rva{};
    bool root_in_relocated_data{};
    uint32_t phase{}, course{}, level{}, route{}, run_epoch{}, player_pointer{};
    uint32_t position{}, forward{};
    uint32_t running_phase{};
    bool verified{};
};
struct Run {
    uint64_t owner{}, player{};
    uint32_t course{}, level{}, route{}, epoch{};
    std::array<float,3> position{}, forward{};
};
struct Point { double x{},y{},heading{}; bool heading_valid{}; };
struct Projection {
    std::array<double,4> x{}, y{}; // world (x,y,z,1) -> image pixels (right, down)
    double width{},height{};
    bool verified{};
};
std::optional<Layout> ProductionLayout();
std::optional<Run> ReadRun(const EdenDsmodHostApi&, const Layout&);
std::optional<Point> Project(const Run&, const Projection&);
bool SupportsBuild(const char*);
}
