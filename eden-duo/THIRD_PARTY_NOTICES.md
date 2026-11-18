# SCN mod source notices

This `eden-duo/` subproject is GPL-3.0-or-later. It does not change the inherited Android app's license.

- Eden Duo ABI/extension headers: Eden DSMod fork, copyright 2026, GPL-3.0-or-later. Vendored from `igawa6/eden-duo`, commit in `research/upstream.lock.json`. Source: https://github.com/igawa6/eden-duo
- Ooz decompressor: Powzix (copyright 2016) and later rarten/zao contributors, GPL-3.0-or-later. Fetched from a pinned commit, not copied from Snap's executable. Source: https://github.com/zao/ooz . All upstream file notices must be preserved in a source distribution. No proprietary Oodle DLL is used.
- SIMDe: MIT; fetched at the ooz submodule's pinned commit. Preserve its full MIT license when redistributing the dependency's source. Source: https://github.com/simd-everywhere/simde
- bcdec: Sergii Kudlai / iOrange, MIT option, full notice in `vendor/bcdec/LICENSE` and header. Source: https://github.com/iOrange/bcdec
- DRP archive layout research: public ISC-licensed kwsch/NewSnap reader and the MIT-licensed `romfs_census.py` from the local pinned New Pokémon Snap recompilation research. SCN's bounded C++ reader is newly written; no game executable code is embedded. Source: https://github.com/kwsch/NewSnap
- NUTEXB/Tegra block-linear layout: public format research and local MIT reference tests. SCN implements a bounded block reader and uses bcdec for BC texture formats.

New Pokémon Snap, Pokémon, game artwork, fonts and game data belong to their respective rights holders. They are not distributed in this mod source or its package. Runtime extraction uses the player's own game files. The Imgur reference album is not downloaded, copied or bundled.

Before any public binary release, include matching source/build instructions, these notices, the GPL text and dependency license notices. This local work has not been published.
