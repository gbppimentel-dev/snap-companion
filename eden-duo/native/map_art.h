// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dsmod_module_abi.h"
#include <cstdint>
#include <string>
#include <vector>

namespace scn {
struct Image { uint32_t width{}, height{}; std::vector<uint8_t> rgba; };
// Asset worker only. The module never invokes the game's original decoder.
bool ExtractMap(const EdenDsmodHostApi&, unsigned archive, const std::string& member,
                std::vector<uint8_t>& nutexb);
bool DecodeMap(const std::vector<uint8_t>& nutexb, Image& image);
// Public test seam: performs bounds checking and never dereferences an invalid offset.
bool DeswizzleBlock(const std::vector<uint8_t>& bytes, unsigned width_blocks,
                    unsigned height_blocks, unsigned bytes_per_block,
                    unsigned bx, unsigned by, uint8_t* output);
}
