# Thor validation — 2026-11-18

The user authorized extended AFK development, device control and in-place APK updates. Consolidated target: **0.5.26-dev.2/code40**, original `dev.gbppimentel.snapcompanion` identity and baseline signer. Historical v0.5.24 artifacts remain unchanged. No GitHub publication occurred.

## Observed on hardware

- The persistent helper automatically starts through the supported Thor firmware service. It validates New Pokémon Snap2.0.1 before opening the fixed Eden process's HostMemory descriptor **read-only**. Eden was observed as0.2.1; the emulator process remained the same throughout these checks.
- Direct course/selected-level readings were checked in Park Day level2 below saved maximum3, Park Night level2, paused/moving states, leaving a course and cold Companion reopening mid-course. Saved maximum level is not substituted for selected level.
- The expanded Night map rendered on the lower display with course/level, authored routes, Pokémon clusters, location cards and current saved-star coverage. Its baked NEO-ONE symbols are explicitly labelled Start.
- The separate Route Tracker showed sampled rail geometry and the changing measured NEO-ONE position. Pausing held position; a fresh retry reset progress. Native packets are sampled at20Hz and only game fields reach the WebView.
- All four final controller-port tools produced their visible game effects in Park Night: airborne Fluffruit, launched blue Illumina Orb, active Melody indicator, and full Scan pulse. Each was requested from the lower map while Snap ran above. No emulator settings were changed.
- Tool buttons were disabled during the game's Pause Menu and post-run/photo-selection phases. During an earlier input experiment, the visible Eden drawer caused subsequent requests to be rejected. The drawer/paused-overlay guard remains in the final implementation.
- Saved Photodex count remained114 Diamond categories during the diagnostic runs; no submitted Photodex entry was selected or replaced. Existing app records and images remained in the original package sandbox. The empty test runs were finished without evaluation.

## Important corrections made during validation

The first synthetic Android KeyEvent route could open Eden's drawer instead of acting as a physical controller. An Android dispatch acknowledgement was therefore insufficient evidence. That route was removed. Final output uses only four whitelisted built-in controller event codes, after a fresh native gameplay check, visible-window/overlay check, verified upper game task, physical-held-key guard and another native state check. The native function fixes the press at80ms and always attempts release, including after a failed/short down write. The process-memory descriptor remains read-only; normal controller events are a separate output.

A native HUD remaining-photo candidate could be found and displayed72, but active HUD ownership after retries was not proven. Its discovery/output was removed from the final build. It is not a completed native photo-counter feature, and the final reader does not keep scanning for it. Existing recognized photo-selection information retains its original provider.

## Local validation

- All30 course-map views passed isolated browser checks: prepared assets loaded, fully revealed terrain selected, received LenTalk links opened and returned to the map, tool callbacks used fixed names, a fresh measured marker appeared and transition samples hid it.
- 174 Android/JVM tests,49 UI model tests,6 diagnostics and release lint passed for the consolidated APK.
- Node model checks cover all1403 bounded records, current four-category coverage, clustering/zoom/letterboxing, sampled route projection, stale/future/wrong-course/transition rejection, private-field exclusion, observed-only section trails and run resets.
- Android/JVM checks cover packet identity/freshness, geometry sanitization, tool whitelist/phase/time guards and rejection of unknown feedback actions.
- Release checks include lint, original application identity/signer, increasing versionCode, unchanged v0.5.24 tag, required JNI symbols, all143 map textures and packaged native license notices.
- Earlier prototype warm native reads had median267–276 microseconds on Thor. Those measurements predate the map UI and exclude Java, capture and display overhead. They are not a sustained-FPS result. Gameplay screenshots during tool checks varied in FPS; broad thermal/performance comparisons remain open.

## Still pending

- Illustrated-map live-player calibration. The live marker is on the separate measured Route Tracker; no guessed/timer position is drawn over the illustration.
- Camera viewing direction/FOV; current heading represents vehicle travel.
- Active HUD ownership and direct remaining-photo reading across retries.
- All30 courses, every branch/Illumina/Forest variation, emulator process restart and long-session acceptance. Local map coverage does not establish all-course runtime support.
- Live Pokémon actor positions, exact current request poses, target recognition and ungraded-photo ranking.

Private runtime captures, packets and backups are excluded from source publication. Automatic review previously rejected exporting a private packet containing session/process metadata; that export was not retried or bypassed. Final validation uses visible UI/game responses and fixed-purpose released inputs.

## Consolidated installation

Installed in place successfully: **0.5.26-dev.2/code40**. Package manager confirmed the version and original application ID; baseline signer was checked before installation. The Eden process remained unchanged and Snap remained in its in-game Pause Menu. APK SHA-256: `69d916834132e1dedf4a7de5a14e10224e6f84823e89b965ef7d73e039ec8bc9`. Final lower-display check recovered **Florio Nature Park / Night / Lv.2**, retained114 Diamond categories, and opened the measured Route Tracker at the held27% position. Tool buttons were disabled in the game Pause Menu. The companion remains open below Snap.
