# SCN • Eden Duo companion mod

SCN is the optional successor to the frozen Snap Companion v0.5.24. Its new delivery target is an Eden Duo `.dsmod.zip`, not an Android APK. Stable main, tag, APK, private data and release remain unchanged. The old app source is retained; its Photodex, LenTalk, Guide and photo-selection features have **not yet been ported into this mod**.

**Presentation requirement:** preserve the original v0.5.24 interface and existing features, adding direct game readings and map expansion. The current native layout is an engineering placeholder, not a replacement design. Whether the original interface runs inside Duo or remains an Android interface paired with its reader mod is awaiting clarification. See [UI and compatibility contract](docs/UI-COMPATIBILITY.md).

**Current status: Android ARM64 reader implementation and a separate read-only validation package. The requested companion is not finished.** Static analysis identified candidate current-course and chosen-level fields; the rail-position calculation matches the original getter in 240 isolated synthetic cases. Live ownership/lifetime, illustrated-map calibration, the original interface integration and device behavior remain unverified. The validation package is for checking those findings, not a completed-feature release. The user is currently using regular Eden; there has been no device access, emulator migration or installation for this work.

The requested experience is automatic: the main screen shows the actual course, variant and selected research level. Tapping its map expands it, with pan, pinch zoom, NEO-ONE position and optional Pokémon locations. No route selector, timing estimate or OCR fallback will pretend to supply a missing live value.

## Checklist

- [x] Preserve stable v0.5.24 and isolate development on `next`.
- [x] Study Eden Duo's contribution, package, native module, map and touch APIs.
- [x] Inspect Skyward Sword HD, Super Mario Odyssey and Echoes of Wisdom implementations.
- [x] Pin the ABI headers and decoder dependencies; preserve their licenses.
- [x] Confirm the owned executable's full NSO build ID for New Pokémon Snap 2.0.1.
- [x] Native module builds on this Mac; exact title/build and ABI gates are implemented.
- [x] Native module also compiles as a stripped Android ARM64 ELF using Google's checksum-verified NDK 28.2.13676358.
- [x] Read encrypted DRP metadata containers and original English MSBT course text at runtime; all 30 owned course records decoded and checked locally.
- [x] Trace candidate scene, course, chosen-level and rail fields against the owned 2.0.1 executable; keep them isolated from trusted production readings.
- [x] Implement measured-distance curve sampling; compare cubic Hermite and linear positions against the unmodified original getter in 240 synthetic VM cases.
- [x] Guarded memory-reader foundation with synthetic transition, pointer, relocation and projection checks.
- [x] Worker-side DRP extraction, CRC validation, Oodle decompression and BC1/BC3/BC7 texture decoding.
- [x] Original Florio map rendered locally from the owned game files, with no game artwork in this source tree.
- [x] Byte-for-byte decompression checked against all 143 owned map/route textures across 30 archives.
- [x] Full extraction/rendering compared against an independent texture renderer for all 143 maps/layers; alpha exact, colour difference at most 1/255.
- [x] Author main/expanded map manifest, pan/zoom/reset and gated layer toggles. **Not visually validated in Eden Duo.**
- [x] Block installable release packaging while live mapping or device validation is incomplete.
- [x] Read the course catalog, map identity and labels from ROMFS at runtime.
- [ ] Verify a current-run root, phase/lifetime and course/variant identity for this build.
- [ ] Verify selected research level independently of saved/unlocked maximum level.
- [ ] Verify active route, branch changes and course segments.
- [ ] Verify NEO-ONE world position; establish camera/vehicle heading semantics.
- [ ] Establish each course/segment's world-to-map projection using game metadata or independently checked calibration.
- [ ] Connect those verified values to the main course card and moving map marker.
- [ ] Clear course, images, route and marker on loading, exit, invalid reads and changed runs.
- [ ] Provide map-layer and route recipes for every supported course/variant/segment, including update content.
- [ ] Source Pokémon icons from the player's files and verify location/level/variant conditions.
- [ ] Enable the Pokémon layer with clear location semantics; do not represent reference locations as live actors.
- [ ] Verify player's marker and optional heading at map edges, route branches and multi-part courses.
- [x] Build the Android ARM64 native library using the NDK.
- [x] Prepare separately named, read-only diagnostic packaging for the later device-validation step; normal release packaging remains blocked.
- [ ] Verify real Eden Duo rendering, touch hit targets, pan/zoom, reset and course changes.
- [ ] Verify a second boot/ASLR, day/night, research levels, retries, exits and invalidated pointers.
- [ ] Measure actual Thor sampling cost, decode stalls, CPU/memory usage and frame pacing under NCE.
- [ ] Package and validate the final `.dsmod.zip`; installation/publication require a separate request.

## Local work

```sh
cd eden-duo
python3 tools/generate_manifest.py
python3 tools/build.py --fetch-deps
python3 -m unittest discover -s tests -p 'test_*.py'
```

The decoder fetch is pinned open-source code only. Set `SCN_OOZ_ROOT` to an existing checkout to avoid another download. The host build emits a native test library and an offline art verifier under ignored `build/host/`; it does not produce an Android-compatible `.so`.

To privately verify a map from your own extracted ROMFS:

```sh
build/host/scn-art /path/to/own/romfs 1 course_map_l_001_1_A_1.nutexb /tmp/florio.pam
```

Normal release packaging through `tools/package.py` refuses unverified builds until the build evidence and device-validation receipt pass. An Android build uses `ANDROID_NDK_HOME=/path/to/ndk python3 tools/build.py --android`; it is never an APK and never targets the stable app's storage. Packaging only admits its manifest, module and licenses.

For development validation only, build with `--android --diagnostic-reader` and run `python3 tools/package.py --validation-build`. This creates `build/validation/0100F4300BF2C000.dsmod.zip`, labelled **SCN Read-only Validation**, with a Reader Check page. It exposes unverified candidate values separately, sends no controls, performs no guest writes and does not imply compatibility with the frozen Android app. Its native code can perform bounded object-discovery reads; its cost still needs measurement on Thor. See [static field/curve evidence](research/snap-static-profile.json). These are explicit development diagnostics, not a way to publish an unverified release.

See [access findings](docs/ACCESS.md), [live-validation requirements](docs/VALIDATION.md), [current evidence](research/builds.json) and [dependency provenance](research/upstream.lock.json). This subproject is GPL-3.0-or-later; the inherited app's license is unchanged.
