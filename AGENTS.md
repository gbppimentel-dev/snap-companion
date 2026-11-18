# Snap Companion Next working rules

- The user reports v0.5.24 works well and requires it to stay that way. This worktree/branch is the only target for future improvements.
- Base: v0.5.24, commit b87fbbfe3e3d33dbdd09a55c9b190895a70a569a. Never change that tag, stable main, stable APK/release assets or the original stable app/storage.
- Fork app/package: Snap Companion Next / dev.gbppimentel.snapcompanion.next. Keep its namespace, firmware projection target, controller paths, diagnostics and APK output isolated. No automatic imports or migration from stable private data.
- Build with scripts/build-next.sh. Never use a stable application ID, signing/package workaround or overwrite a stable APK. The original stable release packager is intentionally disabled here.
- No Thor access, installation, app launching, inputs, settings changes or gameplay diagnostics without a new explicit user request. Creating this fork does not authorize the former Pass4 backlog.
- Existing UI contract remains: Live, Photodex, LenTalk, Guide; no manual bookkeeping or fabricated live fields; preserve records and the established Snap styling. Future features must be requested.
- Changes to this branch are optional. Do not merge or publish over stable main/release without explicit user direction; do not automatically deploy or publish Next builds.
- Any separately authorized Next publication must be a clearly named prerelease that does not become GitHub's latest stable release. Preserve v0.5.24 and its existing download links/assets.
- Android data isolation does not give independent game/device resources. Use one active live companion at a time when future on-device testing is authorized. Never stop the stable app automatically to make Next run.
