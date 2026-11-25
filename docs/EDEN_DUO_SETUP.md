# Eden Duo host migration

Release 0.5.36-duo.1/code64 targets Eden Duo
(`dev.igawa6.edenduo`) instead of stock Eden. The APK keeps the original
Snap Companion identity, signer, interface and private-data paths. Its save,
gallery, research, visibility, performance and native-reader guards all target
Duo. It does not require a `.dsmod.zip`; the companion remains a separate APK.
The current release compatibility metadata describes Duo. Historical v0.5.35 remains the stock-Eden release; its APK, metadata and tag are preserved.

The owner explicitly authorized the Thor migration, save backup, launcher
replacement, companion update and graphics changes on 2026-11-24. Preserve
stock Eden and its original saves as a rollback baseline. The migrated Snap
save belongs to Duo's existing player profile; only the companion's corresponding
bound profile ID changes, retaining its cached records and history.

Configuration: carry over Snap's existing per-game renderer settings, enable
2x LSFG for Snap, and set global Duo anti-aliasing to SMAA. Keep existing
frame-generation defaults for other games. Set Snap's per-title No Companion option
to Off (`no_companion@0100F4300BF2C000=2` in Duo's second-screen
preferences), so its idle Presentation cannot cover the separate APK.
The original Cocoon tile uses a Duo-generated pinned shortcut; a direct ROM
entry would require Cocoon to obtain a separate document grant. The owner's existing Lossless.dll
and shader cache are used; do not distribute them. One-shot flow diagnostics
are temporary and must be disabled after checking actual generation.

Local verification uses `scripts/build-local.sh` with NDK r28c. Preserve the
baseline signing certificate and increment versionCode before in-place install.
The existing protocol and concurrent-heartbeat tests exposed two lifecycle
issues: a dead process could retain a visible-window flag, and backup-style
AtomicFile publication could briefly remove the directly read heartbeat path.
Visibility now requires a live PID; heartbeat publication uses one atomic rename.

Device evidence and private migration backups stay outside published source.
Hardware observations do not establish all-course, thermal or long-session
acceptance for the new host.

## Observed on Thor, 2026-11-24

- Original-signer code64 installed in place. 200 JVM tests, 79 model/map
  tests, six diagnostic tests, native reader checks, release lint and identity
  verification passed. Publication audit and diff whitespace checks passed.
- Existing Cocoon Snap tile retained its ID, artwork, folder placement and
  play history; a Duo-generated pinned shortcut replaced its stock-Eden target.
  Snap Companion opens automatically on physical display4; Duo runs on display0.
- All15 migrated save files and the local backup matched the stock originals by
  SHA-256 before launch. The populated save loaded normally. Companion showed
  212/936 Photodex categories and29 completed requests after reconnecting.
- After a second cold Duo launch, the per-title No Companion Off preference
  prevented the idle cover from obscuring the APK. The native guard recognized
  Snap2.0.1 and the sampler reported ready. In Park Day the APK showed level3,
  72 photos remaining, the illustrated map, current NEO-ONE marker and trail.
- Logs confirmed LSFG enabled, multiplier2 and SMAA active. A one-shot flow dump
  was produced only after the generation path became active. SurfaceFlinger
  measured median presentation intervals of16.635ms at the title screen and
  16.637ms in-course while the game/companion reported approximately30 game FPS.
  These are short samples, not a long-session performance guarantee.
- One-shot flow diagnostics are disabled in the final configuration. The game
  was left in its own Pause Menu with the live map open on the lower display.

## Publication verification — 2026-11-25

Thor reports v0.5.36-duo.1/code64 and Duo 1.4.0. The installed companion APK's
SHA-256 exactly matches the preserved signed release binary:
`7dd53c6e0b035b9b11780ccc2d3006b94df7412be5b573b755580ed1592cb0de`.
This publication preserves that exact binary, rather than silently replacing it
with a newly rebuilt APK. Current UI assets match source. Public release files do
not include user saves, personal photos, migration backups, an emulator APK,
Lossless.dll or shader caches. The comparison/publication task does not change
Thor's game session, firmware configuration or stored progress.
