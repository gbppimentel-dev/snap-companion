# SCN continues as an APK update

The user clarified on 2026-11-18 that the freeze existed only because a Duo-specific mod was thought necessary. That restriction is lifted. Future builds may update the original Snap Companion APK in place.

- Application ID, namespace and launcher: `dev.gbppimentel.snapcompanion` / Snap Companion.
- Signing certificate: match historical v0.5.24. An update must never require uninstalling or replacing the key.
- Current development identity: 0.5.30-dev.2, versionCode48, updating the original package.
- Private preferences, records, saved images, helper paths and the established UI remain in the original app sandbox. No separate-package import is needed.
- Preserve the historical v0.5.24 tag, APK, release assets and existing backups; they are reference/rollback artifacts, not a prohibition on new app builds.
- The worktree/branch and `fork.json` filenames are historical. They no longer describe a second installed app. Continue development here until repository consolidation is requested.
- Build using `scripts/build-local.sh`; `scripts/build-next.sh` forwards to it for compatibility. Output: `dist/development/SnapCompanion-0.5.30-dev.2.apk`.
- Native build dependency: Android NDK28.2.13676358 (`sdkmanager 'ndk;28.2.13676358'`), or point `ANDROID_NDK_HOME` at an existing r28c installation. No archive decoder or game executable is linked into the APK reader.
- Keep the original appearance and feature flow. Replace screen-reading inputs with verified direct memory inputs where supported; do not redesign or pretend unmapped fields are live.

The APK bundles a persistent Thor adapter that validates the v2.0.1 fingerprint and reads course/selected level/rail/world fields at20Hz. The established dashboard receives course/level readings; an expandable illustrated map with integrated schematic route position and section/current-path course progress are implemented. The game-only WebView feed excludes session tokens, process IDs and raw addresses. The map no longer presents touch photography tools. The existing guarded four-action backend remains for compatibility, with no map dispatcher. No game-memory write API exists.

All30 illustrated course views,1403 authored locations, saved-category coverage, received LenTalk links, map layers and pan/zoom are included. The map matches directed sampled geometry against the owned course rail catalog. Known rail-change events distinguish conditional forks from required transitions; observed route choices narrow overall progress and do not double count branches. Schematic illustration alignments have separate per-interval validity guards. The native Camp HUD counter is qualified through its current controller, shared-owned world/HUD and exact rail. A normal shot and retry verified72→71→72. Other courses retain screen recognition for this counter until their separate world ownership paths are verified. The render-camera direction, up vector and lens angle now feed a guarded schematic view cone; live turning and zoom were checked on Thor. Locally reviewed traces now cover portions of30 illustrations/121 rails; later Forest scenes, two short catalog rails and broad all-course hardware/performance acceptance remain open. Forest/Camp overall totals remain neutral until their continuation histories are proven. See [ROUTE_MAP.md](ROUTE_MAP.md).
The user subsequently explicitly authorized Thor control, gameplay diagnostics and APK installation on 2026-11-18. Development APKs are being validated in place without clearing data or changing emulator settings. That device authorization does not authorize GitHub publication or a game/save migration. New releases may proceed normally when separately requested and validated; no permanent prerelease/separate-package restriction remains.

Current hardware checks and remaining acceptance work are recorded in [THOR_READER_VALIDATION.md](THOR_READER_VALIDATION.md).
