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

# Integrated map validation — 2026-11-19

This section supersedes the separate-tracker UI described in the code40 historical record above. Final target: **0.5.27-dev.2/code42**, installed in place with the original identity/signer and no app-data clearing or emulator setting changes. APK SHA-256: `50194bf85ce8525f36a3cecb5ef8db42f9c979707870c5959de51e77038ded9d`.

- Sweltering Sands Night Lv.2 was already open. The updated map placed the NEO-ONE directly on the illustrated route, retained its measured trail and showed Oasis63% with course41% while paused. The position held and tools were disabled. After resuming, the marker advanced, Oasis progress rose, the label changed to Sand Pits, and a later view showed Sand Pits88% /course94%.
- A lower-map Scan request passed the fixed tool dispatcher. The four tool implementations/native reader are unchanged from the prior hardware-validated build; the dispatch log alone is not a new visual proof of every tool effect.
- On leaving research, the player/trail, course/section percentages, selected-level reading and tools became neutral/disabled instead of retaining a stale live position.
- The Sands diagnostic run ended with **zero photos taken** and zero expedition points. No Photodex entry was submitted or replaced.
- After selecting Park Day at the game's existing Lv.3, the already-open map automatically changed course, time variant, level, terrain, saved-category coverage and island Orb asset. Park Trail13% /course9% correctly included the later required continuation; a later view showed Park Trail63% /course46%.
- The required transition into the **Flower Field** appeared on hardware with Flower Field66% /course91%, without resetting overall progress to zero. An independent fixed-purpose game-only read matched the actual43.052486-unit rail to catalog node1553600842. This establishes that checked required transition, not every optional fork on hardware.
- The independent diagnostic validates the owned v2.0.1 fingerprint internally and emits only whitelisted game fields. It never opens or exports the companion's private memory packet, token or process metadata. Temporary diagnostic files are removed from Thor after validation; private captures remain excluded from publication.
- The original dashboard retained **114/936 Diamond categories**, saved suggestions, images and Cocoon Thor playtime after installation. Empty diagnostic runs did not alter the submitted Photodex.
- The final redraw optimization keeps fast position/phase updates while redrawing the longer SVG trail only after measured advancement. Isolated browser observation confirmed zero paused-trail rewrites across continued20Hz samples. Marker clusters are not rebuilt on every position sample.
- Final local checks passed: **174 Android/JVM tests,57 UI/model tests,6 diagnostics**, release lint and original-package/signing checks. Thirty browser reference-map views passed asset/navigation checks; integrated marker/progress and transition guards were checked separately. These are not thirty gameplay acceptance runs.

Schematic alignment is approximate and currently covers portions of27 illustrations/92 rails. Jungle's two main rails, disconnected Forest areas, Outaway Cave and Research Camp remain incomplete. Course progress follows the current default continuation and re-evaluates after an observed alternate path; cold rejoins can show a range, and missing interactive-trigger history is explicitly approximate/unknown. See [ROUTE_MAP.md](ROUTE_MAP.md).

Observed gameplay FPS varied by course/scene (Sands screenshots around57 and Park screenshots around47–48, with result/menu screens near60). No sustained60FPS claim or attribution to this UI change is established by those screenshots; emulator configuration was not changed.

Final handoff: code42 is installed and open below Snap. Park Day was retried at **Lv.2**, below its saved maximum3; the map recovered the selected2 correctly. Snap is left in its in-game Pause Menu with **Park Trail27% /course20%**, the integrated marker visible and touch tools disabled. The user's established map-layer preferences are retained. Temporary Thor validation JARs/library/script were removed. No GitHub publication occurred.
