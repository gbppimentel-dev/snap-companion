# Native reader research and diagnostics

**Current delivery is the Android APK**, updating the original Snap Companion app. A separate Eden Duo mod is not required. This folder contains reused native-reader/map-decoder sources, pinned ABI/dependency research and historical diagnostic packaging. Its placeholder Duo interface is not the product UI.

The installed APK development line is **0.5.29-dev.2/code46**. It bundles the verified20Hz Thor reader, active course/selected level, integrated approximate map position/trail, known branch/progress model and validated render-camera cone while preserving the original Android interface and saved data. See the authoritative [current checklist](../docs/AFK_MAP_WORK.md), [compatibility](../docs/COMPATIBILITY.md), [map scope](../docs/ROUTE_MAP.md) and [hardware evidence](../docs/THOR_READER_VALIDATION.md).

All30 illustrated maps and1,403 authored reference locations are included in the APK; reviewed alignment covers portions of121 of123 known rails. This is not all-course hardware acceptance. Two short rails, additional Forest variants, Forest/Camp overall continuations, native remaining-photo ownership and broad restart/performance checks remain open. Pokémon sightings are authored references, not live actors. Map photography-tool buttons were removed at the user's request; historical four-tool hardware checks do not imply those buttons remain in the UI.

## Research tools

The native sources include the exact-build memory reader, measured-distance Hermite/linear rail sampling, DRP parsing, Oodle decompression and BC texture decoding. Decoder output was independently compared for143 owned map/route textures; that establishes decoding, not live world-to-map alignment. Source/dependency license notices must remain intact.

```sh
cd eden-duo
python3 tools/generate_manifest.py
python3 tools/build.py --fetch-deps
python3 -m unittest discover -s tests -p 'test_*.py'
```

Dependencies are pinned open-source inputs. Set `SCN_OOZ_ROOT` for an existing checkout. The host build produces ignored native test/art-verifier tools. `ANDROID_NDK_HOME=/path/to/ndk python3 tools/build.py --android` builds an Android native library, not the product APK. Build the product with the repository-root `scripts/build-local.sh`.

An owner can privately verify extracted map data with:

```sh
build/host/scn-art /path/to/own/romfs 1 course_map_l_001_1_A_1.nutexb /tmp/florio.pam
```

## Historical Duo package

`--diagnostic-reader` and `tools/package.py --validation-build` retain a separately labelled read-only validation package for development. Its placeholder interface, offsets/evidence files and release gates must not be presented as a completed or device-qualified Duo mod. No Duo migration, save copying or standalone mod acceptance has been performed. APK validation does not automatically qualify the optional module's rendering, ABI, touch or lifecycle.

The earlier native-only plan is superseded; it is not an outstanding user-facing `.dsmod.zip` task. See [APK delivery rules](../docs/NEXT.md), [access research](docs/ACCESS.md), [validation boundaries](docs/VALIDATION.md), [static evidence](research/snap-static-profile.json) and [pinned provenance](research/upstream.lock.json). Do not fabricate a standalone module validation receipt from APK/model tests.

This subproject is GPL-3.0-or-later. The inherited companion's license and third-party asset ownership are documented separately at the repository root. Raw game archives, keys, process packets, device captures and saves remain private and excluded from publication.
