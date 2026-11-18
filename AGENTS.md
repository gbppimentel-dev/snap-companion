# Snap Companion Next working rules

- Current direction (2026-11-18): SCN is an Eden Duo `.dsmod.zip` companion, developed in `eden-duo/`. This supersedes the previous Next Android APK direction. The Android foundation remains reference material; do not build/deploy an APK as the requested SCN deliverable.
- Follow Eden Duo's package/ABI format. Distribute source, authored UI and asset recipes, never the user's extracted game assets, executable, saves, keys or original decoder code. Read game art from the player's files at runtime.
- Course, day/night, selected research level, route and player pose require verified current-run sources for the exact game build. Loaded archive names and saved maximum levels do not establish current context. Never animate an estimated location or show a fixture as live. Release packaging must refuse unverified mappings.
- Initial map work is read-only. Touch for expanding, panning, zooming and layer toggles controls the companion. Guest writes or touch gameplay shortcuts need separate implementation evidence; do not borrow another game's offsets or hooks.
- The user reports v0.5.24 works well and requires it to stay that way. This worktree/branch is the only target for future improvements.
- Base: v0.5.24, commit b87fbbfe3e3d33dbdd09a55c9b190895a70a569a. Never change that tag, stable main, stable APK/release assets or the original stable app/storage.
- Fork app/package: Snap Companion Next / dev.gbppimentel.snapcompanion.next. Keep its namespace, firmware projection target, controller paths, diagnostics and APK output isolated. No automatic imports or migration from stable private data.
- Build with scripts/build-next.sh. Never use a stable application ID, signing/package workaround or overwrite a stable APK. The original stable release packager is intentionally disabled here.
- No Thor access, installation, app launching, inputs, settings changes or gameplay diagnostics without a new explicit user request. Creating this fork does not authorize the former Pass4 backlog.
- Existing UI contract remains: Live, Photodex, LenTalk, Guide; no manual bookkeeping or fabricated live fields; preserve records and the established Snap styling. Future features must be requested.
- Changes to this branch are optional. Do not merge or publish over stable main/release without explicit user direction; do not automatically deploy or publish Next builds.
- Any separately authorized Next publication must be a clearly named prerelease that does not become GitHub's latest stable release. Preserve v0.5.24 and its existing download links/assets.
- Android data isolation does not give independent game/device resources. Use one active live companion at a time when future on-device testing is authorized. Never stop the stable app automatically to make Next run.
