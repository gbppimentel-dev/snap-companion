// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dsmod_module_abi.h"
#include <map>
#include <optional>
#include <string>
#include <vector>
namespace scn {
struct CourseInfo {
    unsigned id{};std::string pack,map_stem,name,region,description;
};
struct CourseCatalog {
    std::map<unsigned,CourseInfo> courses;
    static std::optional<CourseCatalog> Load(const EdenDsmodHostApi&);
};
std::optional<std::map<std::string,std::string>> ReadMsbt(const std::vector<uint8_t>&);
}
