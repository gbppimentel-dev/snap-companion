# SCN continues as an APK update

The user clarified on 2026-11-18 that the freeze existed only because a Duo-specific mod was thought necessary. That restriction is lifted. Future builds may update the original Snap Companion APK in place.

- Application ID, namespace and launcher: `dev.gbppimentel.snapcompanion` / Snap Companion.
- Signing certificate: match historical v0.5.24. An update must never require uninstalling or replacing the key.
- Current development identity: 0.5.25-dev.2, versionCode38, updating the original package.
- Private preferences, records, saved images, helper paths and the established UI remain in the original app sandbox. No separate-package import is needed.
- Preserve the historical v0.5.24 tag, APK, release assets and existing backups; they are reference/rollback artifacts, not a prohibition on new app builds.
- The worktree/branch and `fork.json` filenames are historical. They no longer describe a second installed app. Continue development here until repository consolidation is requested.
- Build using `scripts/build-local.sh`; `scripts/build-next.sh` forwards to it for compatibility. Output: `dist/development/SnapCompanion-0.5.25-dev.2.apk`.
- Native build dependency: Android NDK28.2.13676358 (`sdkmanager 'ndk;28.2.13676358'`), or point `ANDROID_NDK_HOME` at an existing r28c installation. No archive decoder or game executable is linked into the APK reader.
- Keep the original appearance and feature flow. Replace screen-reading inputs with verified direct memory inputs where supported; do not redesign or pretend unmapped fields are live.

The existing Thor-specific privileged helper demonstrated game-memory access. The APK now bundles a persistent native adapter, started automatically through the existing Thor helper. It verifies the v2.0.1 fingerprint and samples read-only course/selected-level/rail/world fields at 20 Hz. The original dashboard consumes fresh course/level values; unknown, wrong-process, stale and prior-session packets are rejected. Position remains internal until illustrated-map alignment is validated. All-course acceptance, map expansion, Pokémon markers and product touch gameplay actions remain unfinished. The `eden-duo/` module is development/research code, not a required installed component of the intended APK.

The user subsequently explicitly authorized Thor control, gameplay diagnostics and APK installation on 2026-11-18. Development APKs are being validated in place without clearing data or changing emulator settings. That device authorization does not authorize GitHub publication or a game/save migration. New releases may proceed normally when separately requested and validated; no permanent prerelease/separate-package restriction remains.

Current hardware checks and remaining acceptance work are recorded in [THOR_READER_VALIDATION.md](THOR_READER_VALIDATION.md).
