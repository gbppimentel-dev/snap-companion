# Optional Next fork

**Current direction — 2026-11-18:** SCN is now an Eden Duo companion mod, not a new Android app. Its implementation and checklist are in [eden-duo/README.md](../eden-duo/README.md). The APK information below describes the previous isolated foundation, retained for reference only. It is not the new deliverable.

Stable v0.5.24 is the preserved working baseline. Its source tag, main branch, published release, APK and private app data remain unchanged.

| | Stable | Optional fork |
| --- | --- | --- |
| Name | Snap Companion | Snap Companion Next |
| Android package | dev.gbppimentel.snapcompanion | dev.gbppimentel.snapcompanion.next |
| Base/current version | 0.5.24 | 0.5.24-next.1 |
| Source | main / v0.5.24 | next worktree |
| Private data | Original Android sandbox | Separate Android sandbox; no migration |
| APK output | Existing frozen release | dist/next/SnapCompanionNext-0.5.24-next.1.apk |

The first Next build only establishes separation and an identifying badge. It adds no gameplay features and does not resume the old feature backlog. Choose the optional app in Cocoon only when you want to test a requested improvement. Creating/building the fork does not install it or change the current shortcut.

Capture and controller helpers target only the fork package and its private paths. Baseline release packaging is disabled here. Build with `scripts/build-next.sh`; it verifies the built identity, helper paths and pinned base tag. Publishing/installing still requires a separate request.

Both apps share the same physical Thor, game and Android capture resources. They are alternatives, not simultaneous live sessions. Future testing must not silently stop stable, replace its APK, import/change its private records or claim experimental performance is independent of other running work.

Do not merge Next into stable, retag v0.5.24 or replace its release assets without explicit new user instructions. Future Next versions keep a distinct app identity and artifact name.

If a Next publication is requested later, publish it as a clearly named prerelease without making it the latest stable release. The main/default download remains stable v0.5.24. This local foundation has not been pushed or installed.
