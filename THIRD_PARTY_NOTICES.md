# third-party notices

**code & documentation:** the [MIT license](LICENSE) applies to original companion code and project documentation. It does not grant rights to the third-party material below.

## New Pokémon Snap and Pokémon content

New Pokémon Snap, Pokémon characters, game art, branding, text, game-derived factual/catalog content and original game fonts belong to their respective rights holders, including Nintendo, Creatures Inc., GAME FREAK inc., The Pokémon Company and Bandai Namco Studios. The project is an unofficial fan companion and does not assert endorsement or authorization by those companies.

**included presentation material:** selected UI artwork, portraits, course thumbnails, 143 prepared map/route layers and fonts in `app/src/main/assets/game-art/`, selected launcher/font resources, the prepared reading dataset, and UI documentation images. These are not relicensed under MIT. Supplemental Pokémon HOME appearance art is distributed through [PokeAPI/sprites](https://github.com/PokeAPI/sprites); that repository is a reference source, not an assertion of a free-art license.

**excluded originals and private data:** no original game archives, executable images, console keysets, user saves, personal photographs, runtime captures, private device logs, SDK/JDK installations or signing keys are included. Documentation screenshots use invented example progress.

## reference facts

The prepared reference data draws on [Serebii](https://www.serebii.net/newpokemonsnap/), [PowerPyx](https://www.powerpyx.com/new-pokemon-snap-all-pokemon-locations-stars-photos/), [Bisafans](https://www.bisafans.de/spiele/spin-offs/new-snap/fotodex/), [Bulbapedia](https://bulbapedia.bulbagarden.net/wiki/LenTalk), [PokéAPI](https://pokeapi.co/docs/v2), and independently checked game tables. Sources retain their rights. Photo-course example levels are examples, not guaranteed minimum levels; conflicting labels are withheld. These references do not establish live route timing or recognition accuracy.

## save-format work

[kwsch/NewSnap](https://github.com/kwsch/NewSnap): ISC License, copyright 2021 SciresM and Kaphotics. The save-envelope/checksum implementation and small public test vectors are acknowledged in [`third_party/newsnap/LICENSE`](third_party/newsnap/LICENSE) and [`app/src/main/assets/licenses/newsnap.txt`](app/src/main/assets/licenses/newsnap.txt). Save-format constants are part of that independent implementation; no private console keyset is included.

[finalpantasy/new-poke-snap-switch-recomp](https://github.com/finalpantasy/new-poke-snap-switch-recomp): MIT License, copyright 2026 finalpantasy. Factual model/archive research informed mapping and independent read-only investigation. The notice is preserved in [`app/src/main/assets/licenses/snap-recomp.txt`](app/src/main/assets/licenses/snap-recomp.txt). No game executable or upstream save-writing tool is bundled.

## recognition and libraries

- **Google ML Kit / Google Play services text-recognition dependencies:** bundled offline Latin OCR through the declared Android dependencies; Google's licenses and third-party dependency notices apply. No recognition frames are uploaded by this app.
- **Hugo Peixoto / Cyberscore Auto-proofer:** only the CC0 digit-training PNG is reused. The CC0 notice is bundled in [`app/src/main/assets/licenses/cyberscore-digit-images-CC0.txt`](app/src/main/assets/licenses/cyberscore-digit-images-CC0.txt); provenance is in [`recognition/NOTICE.txt`](app/src/main/assets/recognition/NOTICE.txt). No AGPL scanner code is imported or ported.
- **Android / AndroidX / Robolectric / Gradle:** development/runtime libraries retain their respective licenses. SDKs and build toolchains are installed separately.

## other marks

GitHub's mark identifies the creator credit and remains GitHub's. AYN, Eden and Cocoon names identify the supported environment and remain their owners' marks. The credit panel follows the author's Underworld Companion presentation with Snap's own styling.

## Native game-memory helper

The separately executed game-memory/controller helper (`bridge/src/dev/gbppimentel/snapbridge/GameMemoryReader.java`, `app/src/main/cpp/snap_memory.cpp` and `eden-duo/native/snap_runtime.*` and the fixed `GameToolInput.java` controller output) is GPL-3.0-or-later. It reuses the Eden DSMod ABI header under GPL-3.0-or-later; copyright and complete license are retained in `eden-duo/vendor/eden/` and `eden-duo/LICENSE`. The APK shell communicates with that process through private JSON snapshots. Other original companion files retain their existing notices. Native reader source and build scripts must accompany any future distribution of this development reader.

The Android NDK C++ runtime includes LLVM/Apache-2.0 with LLVM exceptions and third-party notices retained in `eden-duo/vendor/ndk/LLVM-NOTICE`. The decoder research dependencies are documented under `eden-duo/`; the APK memory reader does not link the archive decoder.

The complete GPL text and NDK runtime notice are also bundled inside the APK under `assets/licenses/`. Process memory is opened read-only; the separate tool output sends only four fixed, short built-in controller presses. Authored map-location facts were prepared from the player-owned game tables using `tools/prepare_owned_maps.py`; original archives/executables and the Imgur album are not included.
