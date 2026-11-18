# SCN continues as an APK update

The user clarified on 2026-11-18 that the freeze existed only because a Duo-specific mod was thought necessary. That restriction is lifted. Future builds may update the original Snap Companion APK in place.

- Application ID, namespace and launcher: `dev.gbppimentel.snapcompanion` / Snap Companion.
- Signing certificate: match historical v0.5.24. An update must never require uninstalling or replacing the key.
- Current development identity: 0.5.26-dev.2, versionCode40, updating the original package.
- Private preferences, records, saved images, helper paths and the established UI remain in the original app sandbox. No separate-package import is needed.
- Preserve the historical v0.5.24 tag, APK, release assets and existing backups; they are reference/rollback artifacts, not a prohibition on new app builds.
- The worktree/branch and `fork.json` filenames are historical. They no longer describe a second installed app. Continue development here until repository consolidation is requested.
- Build using `scripts/build-local.sh`; `scripts/build-next.sh` forwards to it for compatibility. Output: `dist/development/SnapCompanion-0.5.26-dev.2.apk`.
- Native build dependency: Android NDK28.2.13676358 (`sdkmanager 'ndk;28.2.13676358'`), or point `ANDROID_NDK_HOME` at an existing r28c installation. No archive decoder or game executable is linked into the APK reader.
- Keep the original appearance and feature flow. Replace screen-reading inputs with verified direct memory inputs where supported; do not redesign or pretend unmapped fields are live.

The APK bundles a persistent Thor adapter that validates the v2.0.1 fingerprint and reads course/selected level/rail/world fields at20Hz. The established dashboard receives course/level readings; an expandable reference map and a separate measured Route Tracker are now implemented. The game-only WebView feed excludes session tokens, process IDs and raw addresses. Only the four named photography tools can request controller input, after native gameplay and visible-window checks. No game-memory write API exists.

All30 illustrated course views,1403 authored locations, saved-category coverage, received LenTalk links, map layers and pan/zoom are included. The live route view uses actual sampled rail geometry; route changes are identified by a geometry fingerprint, and only observed portions of previous sections enter its trail. The native HUD photo-counter candidate was withheld because active HUD ownership across retries is not proven. Illustrated-map player calibration, camera-view tracking and broad all-course hardware/performance acceptance remain open.
The user subsequently explicitly authorized Thor control, gameplay diagnostics and APK installation on 2026-11-18. Development APKs are being validated in place without clearing data or changing emulator settings. That device authorization does not authorize GitHub publication or a game/save migration. New releases may proceed normally when separately requested and validated; no permanent prerelease/separate-package restriction remains.

Current hardware checks and remaining acceptance work are recorded in [THOR_READER_VALIDATION.md](THOR_READER_VALIDATION.md).
