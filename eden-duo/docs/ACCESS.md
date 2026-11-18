# What Eden Duo gives SCN

The cited examples are genuine in-emulator integrations. They read each game's own state and assets. Super Mario Odyssey resolves its game/scene/player objects and projects the player's pose through map metadata; Skyward Sword HD combines build-specific points with a native art decoder. Their addresses and projection logic cannot be reused for Snap.

| Source | Available capability | Snap status | Intended feature |
| --- | --- | --- | --- |
| Guest memory | Checked reads, pointers, mapped ranges, exact build gates and optional NCE data relocation | ABI implemented; Snap current-run roots unknown | Automatic course, time/variant, selected level, run/route state |
| Guest pose | Typed bounded reads at module sample time | NEO-ONE and camera objects unknown | Real position and heading; no timer-based movement |
| Owned ROMFS | Runtime reads of patched game files, optional base/DLC/user sources | Original map/route textures found and decoded offline | Native course map and route overlays without scraping screenshots |
| Course data | DRP/FlatBuffer tables, level/rail information and map identity recipes | Static evidence exists; semantics and projection not fully established | Match map segment and possible branches to the actual run |
| Pokémon data/art | Runtime extraction is supported by the host | Locations/icons/conditions not implemented in SCN | Toggle verified Pokémon locations, filtered to current context |
| Host telemetry | `@perf.fps`, frame time, speed, battery; local clock and runtime game time | Available directly from Eden Duo | Compact status header without screen capture |
| Read-only save access | Title-scoped save extension | API available; existing Android export code not ported | Potential Photodex/LenTalk port later; not current-run detection |
| Companion touch | Native map pan/zoom, pages, flags and actions | Map manifest authored, runtime interaction untested | Expand/back, pinch/drag, route and Pokémon layers |
| Guest control | Emulated button presses and guarded writes; game-specific helper hooks where justified | No Snap writes, patches or gameplay actions implemented | Possible later actions after verifying semantics and game-state guards |

Touching an Eden Duo companion can control its own UI, send a normal controller button or invoke game-specific logic. These are different mechanisms. Echoes of Wisdom's item interaction does not establish that arbitrary Snap objects can safely be written. Guest function calls are unavailable under Android NCE; switching CPU backends is not a required SCN design step.

## Map asset findings

- Thirty original `common/UI/master/Textures/course_l_NN.drp` archives contain 143 `course_map_l_*` / `course_arrow_l_*` entries.
- All checked entries are unencrypted, Oodle-compressed NUTEXB textures, 3840×2160: 38 BC1, 98 BC3 and 7 BC7.
- The portable open-source ooz decoder reproduced all 143 previously decoded owned textures exactly. The current C++ path validates DRP metadata, output length and CRC before decoding the texture.
- Full extraction/rendering of all 143 layers also passed comparison with an independent numpy deswizzler and texture2ddecoder: alpha matched exactly; BC1/BC3 colour rounding differed by at most 1/255; BC7 matched exactly.
- A full map is about 31.6 MiB in RGBA8, above the host's 16 MiB image-sink limit. SCN currently decodes block-by-block directly to a 1920×1080 overview, about 7.9 MiB. Transparent pixels are downsampled with alpha weighting. This is an overview-quality path; native-detail tiling at high zoom remains a possible refinement.
- Texture work runs only on the host's asset worker, not the sampling thread. No source art is copied into a redistributable package.
- The Imgur album could not be inspected: web access failed and a direct fetch returned HTTP 429. No claim has been made about its precise annotations. The verified original game map is the visual base.

The current reader design samples bounded fields at 20 Hz and republishes cached output on the host's 60 Hz snapshots. It does not scan the heap, poll screenshots, run OCR, read saves every frame or redraw a rotated map for every camera movement. These are design choices, **not a measured FPS gain or a guarantee of steady 60 FPS on Thor**. Initial maps stay north-up.

## Sources

- [Contribution guide](https://github.com/igawa6/eden-duo-companions/blob/main/docs/CONTRIBUTE.md)
- [Package and map format](https://github.com/igawa6/eden-duo-companions/blob/main/docs/PACKAGE_FORMAT.md)
- [Native module guide](https://github.com/igawa6/eden-duo-companions/blob/main/docs/MODULE_GUIDE.md)
- [Porting a game](https://github.com/igawa6/eden-duo-companions/blob/main/docs/PORTING_A_GAME.md)
- [Odyssey reader](https://github.com/igawa6/eden-duo-companions/blob/main/native/modules/smo_reader.cpp) and [map projection](https://github.com/igawa6/eden-duo-companions/blob/main/native/modules/smo_map.cpp)
- [Skyward Sword HD module](https://github.com/igawa6/eden-duo-companions/blob/main/native/modules/01002DA013484000.cpp)
- [Echoes of Wisdom module](https://github.com/igawa6/eden-duo-companions/blob/main/native/modules/echoes_module.cpp)

The contribution guide's version note lags the package reference. This work pins module ABI 1 and targets runtime 25 for stale-image suppression, rather than assuming every feature exists in an older Eden Duo release.
