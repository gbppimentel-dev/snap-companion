// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dsmod_module_abi.h"
#include <string>
#include <vector>
namespace scn {
// Bounded DRP reader, including the title's own entry/header XOR container format.
// Asset/metadata access only; never reads keys, host files or guest executable bytes.
bool ReadArchiveMember(const EdenDsmodHostApi&,const std::string& archive,
                       const std::string& member,const std::string& extension,
                       std::vector<uint8_t>& output);
}
