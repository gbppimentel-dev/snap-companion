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

# Map polish and render-camera validation — 2026-11-19

This section supersedes the camera-pending and map-tool presentation statements in the historical records above. Final update: **0.5.28-dev.2/code44**, original package and baseline signer, installed successfully in place. APK SHA-256: `ac6b33672aed5adb8f1e89445245e4fed50f185725912655910708b4eda49645`.

- Thor was already on an Undersea result screen. Empty runs were retried at the existing Lv.2 for bounded camera checks; no photos were taken/submitted, no Photodex entry was replaced, and emulator configuration was not changed. The dashboard showed114/936 Diamond categories and25.2h on Thor before and after updating, with the saved Corsola suggestion, images and live Undersea Lv.2 connection intact.
- An independent fixed-purpose diagnostic validated the owned game fingerprint internally and sampled only game-domain camera fields. It did not open/export the companion's private packet. The production reader returned a validated camera on100/100 frames while turning and zooming: vertical lens angle ranged from36° to14.4°. A prior look test established31.1° separation from vehicle heading. The camera is not inferred from the rail tangent.
- The installed map displayed the fading camera cone independently of the vehicle arrow at Undersea Main Trail59% /course59%. A brief resumed camera turn changed the view cone, advanced the measured position slightly and returned immediately to the Pause Menu with72 shots remaining. All injected keys/axes were released.
- The final exclusive All Pokémon/Missing Diamonds/LenTalk/Hidden panel fits Thor's actual display scale without scrolling. Landmarks and Routes remain independent, side-by-side switches. The pre-existing Hidden preference migrated correctly; All Pokémon was subsequently selected for marker validation.
- Pokémon portraits appeared on the installed map and stayed the same size while the artwork enlarged to150%; zoom deliberately expands deterministic map-coordinate groups. The compact Filters/Overview/Follow/zoom toolbar and full-width section/course bars were visually checked. Photography-tool buttons are absent.
- Local checks passed: **176 Android/JVM tests,68 UI/model tests,6 diagnostics**, release lint, publication boundary and original identity/signer checks. Isolated browser checks covered all30 reference-map assets, received LenTalk/card return, exclusive filters, fixed35CSS-pixel pins across zoom, stable membership/centres during panning, dragging from a pin, touch events, camera zoom/turn presentation and stale/transition hiding. The filter panel was also checked at540×470CSS pixels rather than relying only on a larger desktop viewport.
-49 measured shared branch/rejoin junctions now meet after bounded local artwork corrections. Model regressions cover unique missed intermediate rails, ambiguous transition ranges, unrelated cycles, self-loop history and unknown geometry clearing old trails. These are model/calibration checks, not hardware acceptance of every optional branch.

Alignment coverage remains portions of27 illustrations/92 rails. Jungle main rails, disconnected Forest areas, Outaway Cave and Research Camp remain withheld where alignment is unreliable. Broad all-course/branch and sustained thermal/FPS acceptance remain open. The cone is a schematic projection of a verified camera reading, not a surveyed geographic field of view. See [ROUTE_MAP.md](ROUTE_MAP.md).

Final handoff: code44 installed, original data retained, Snap paused in Undersea Lv.2 and Companion's map open below. Temporary diagnostic JARs/library/scripts and the attempted UI accessibility dump were removed from Thor. Private local captures remain ignored. No GitHub publication occurred.

# Compact map UI, smoother trails and Forest scene discovery — 2026-11-19

Final update: **0.5.29-dev.2/code46**, installed successfully in place with the original application ID and baseline signer. APK SHA-256: `0ab8fd7ed6e5d51c35f8b4e9d5bba6ca4d65ec9eb10cfb97ce04248a003a5c42`. The intermediate code45 was also installed during this work; code46 supersedes it.

- Live now has a full-width **Explore Course Map** action with an expansion glyph. FPS/Thor-hours readings are smaller, and credits use a compact centered author line and symmetric columns. Thor's installed dashboard retained114/936 Diamond categories,28.1h from Cocoon, saved pictures and its normal navigation.
- Zoom percentage is removed. A newly opened map starts with Follow enabled; button zoom and Overview retain that choice. Follow toggling, dragging/pinching or scrolling the map can disable it. Local touch/browser checks covered these policies at Thor's540×470CSS-pixel viewport.
- The long measured trail is cached in16-anchor chunks while a short head reaches the current measured marker. Paused replay produces zero long-trail rewrites. Reviewed monotone artist traces and bounded smoothing reduce stroke/arrowhead jitter without predicting movement or connecting rejected gaps.
- Calibration now covers portions of30 illustrations/121 of123 catalog rails, including Jungle main/alternate routes, Cave, Camp circuits/laboratory and additional Forest areas.86 bounded junction anchors across65 rails were adjusted; these counts are calibration/model evidence, not all-branch hardware acceptance.
- A complete, photo-free **Elsewhere Forest Lv.2** run traversed37.651257,46.043808,54.266792 and54.679588-unit rails. Independent read-only game observations uniquely matched the owned geometry for the first misty area, Autumn Forest, deeper misty area and final verdant clearing. The latter three rails were absent from the prior catalog because scene scripts select them without ordinary PathAction references. Autumn's rail belongs to the course's second scene level006_2_A;006_3_C is the separate Illumina course. No arbitrary Pokemon rail was promoted to a player rail.
- The three supplemental nodes are checked against owned entity identity and length during regeneration. Replaying the actual four-rail sequence against the final model yields unique matches, valid schematic positions, separate historical subpaths and a neutral whole-course total. No unproven continuation edges were invented. The first misty marker/progress were observed on the installed code45; the installed code46 retry then transitioned correctly into Autumn Forest. The final paused map held Autumn Forest93% with Lv.2, a stable marker, its yellow measured trail and independent camera cone;72 shots remained. The transient pause transition briefly hid the reading and fresh paused samples recovered it.
- The completed diagnostic run visibly reported **0 photos /0 Pokemon /0 expedition points**. No submitted Photodex entry was replaced. Forest Lv.3 is not unlocked in this save; the retry menu offers only Lv.1 and Lv.2. Other seasonal/branch ownership, two short rails, Forest/Camp overall continuation totals, direct HUD remaining-photo ownership, broad all-course/branch acceptance and sustained thermal/FPS comparisons remain open.
- Final local checks passed:176 Android/JVM tests,73 Node UI/model tests,6 diagnostics,4 offline scientific-Python geometry regressions, release lint, publication audit and original identity/signer checks. Isolated browser acceptance covered all30 map assets, real camera turn/zoom replay, stale/transition hiding, received LenTalk/card return and the compact header/credits/Follow policies. One asset-check harness race was corrected to await decoded images after layout; all30 views then passed.

## Session interruption during local work

Thor initially had an unsaved two-photo Tentacruel review. Only its result introduction was advanced; the photos were not submitted/evaluated/replaced. During local work Thor timed out and slept, and Eden's game activity disappeared. Its cached background process was subsequently ended by Android. Reopening the existing game/save returned to camp; the unsaved review could not be recovered. The user was informed and an apology given. Saved Photodex categories remained114/936. The exact cause of the game activity disappearing was not established.

Thor reports its charger as AC while the original keep-awake preference covered USB only. Keep-awake was temporarily changed from2 to7 for subsequent checks, then restored to2 and verified before handoff. Emulator graphics/game configuration was not changed. The original Turnip Vulkan1.4.335 driver was observed after normal cold launch. Independent diagnostics emit whitelisted game fields only, do not read/export the companion's private packet, and remain excluded from source publication. Temporary device diagnostic JARs/library/scripts were removed after the checks. No GitHub publication occurred.

# Map view persistence and portrait rendering — 2026-11-19

Installed **0.5.30-dev.1/code47** in place with the original signer and data. APK SHA-256: `e2d7fc1094f8605fc4ab8167cb0cf0bbbda2bff713d208be01a58c56745abb6a`. The native game reader is unchanged in this checkpoint.

- A temporary empty course followed by the same course no longer recreates the map or resets zoom. Real course changes still select their own overview. A browser regression exercises the missing/recovered-reading sequence at zoom greater than2x.
- High-resolution Pokémon portraits are now in a screen-space overlay beside the zoomed terrain. The overlay translates with panning; individual anchor coordinates update with map scale/size, without scaling the portrait pixels up and down through parent/inverse transforms. Fixed35CSS-pixel bounds, centered anchors, native source resolution at DPR2, drag-from-pin and stable DOM behavior passed browser checks.
- On Thor, Park Day's reference map was opened from Guide and visually checked at overview and1.5x zoom. Pin sizes stayed stable and portraits remained clear; this was a UI check, not a Park gameplay acceptance run. Saved114 Diamond categories, pictures and28.1h Cocoon time remained available.
-176 JVM tests,73 Node UI/model tests,6 diagnostic tests, release lint, publication-boundary and original identity/signer checks passed. The added optional real-browser regression and existing marker/gesture browser checks also passed.
- Current/historical README, compatibility, architecture, privacy and Duo research notes were reconciled. Standalone Duo-package gates are not duplicate unfinished APK tasks.
- Research Camp was entered from the observed camp menu for photo-free diagnostics; its completed run reported0 photos/0 species/0 expedition points. Cold reader discovery remains under investigation. Higher-budget temporary scans on the empty result screen produced intrusive workload and were not shipped; resident-page-only/typed-scene alternatives remain private experiments pending proof.
- A validation-control mistake was identified: `am task focus` did not change Android's focused display from4 to0 after lower-screen taps. Subsequent physical-input checks explicitly focus the upper display and verify it; a task-selection acknowledgement alone does not prove that a pause/input reached the game.

Broader game-reading and all-course/performance work continues in the current checklist. This checkpoint does not mark live actors, target/pose recognition, photo ranking or cold/restart acceptance complete. No game save, emulator graphics setting or GitHub release was changed.

## Camp reader and counter — code48

Installed **0.5.30-dev.2/code48**, original package/signer/data. APK SHA-256: `101856c14071904ad64c88d32dfe9e22d779464ea94ef61e382581581e8d92eb`.

- Research Camp uses a different current-controller link from standard courses. The exact-build typed scene, shared allocation, reverse owner capture and controller's current-scene pair are all required. The scanner is still read-only and bounded; no high-budget/private graph traversal or resident-page experiment was shipped.
- The shared scene render context provides the verified camera in Camp as well. The actual APK native library returned course30/Lv.1, measured rail distance/geometry, a valid camera and72 photos remaining while paused after retry.
- Camp's current world contains the HUD in a16-byte shared-owner vector. The separate80-byte callback vector is not misidentified as an ownership list. HUD and allocation types, live ownership, matching current rail/control, bounds, uniqueness and repeated values are checked.
- One temporary normal test photo changed both the visible game count and native reading from72 to71. Normal Retry discarded that shot and a new paused run returned72. No test photo was submitted or saved over a Photodex entry. No save edits or forced unlocks. Standard-course native counters still require their own world-path qualification.
- Local checks:177 Android/JVM tests,73 Node tests,6 diagnostics, native scope/counter regression executable, release lint and original signer/identity checks passed.
- During post-install acceptance Android's watchdog killed and restarted system_server. Logs show blocked ActivityManager, display and WindowManager locks; Eden and Companion subsequently received DeadSystemException. Cause is not established. The temporary test shot had already been discarded. Companion was reopened and the visible saved count remained114 Diamond categories and28.1 Thor hours. Eden was relaunched through its normal library; recovery acceptance continues. This is not a clean long-session/performance pass.

## Closing reports and readiness safeguards — code49

The subsequent targeted exit-record check found explicit force-stops (including controlled diagnostic stops), rather than a new Companion application exception. Two Eden ANR main-thread stacks were blocked in `NativeLibrary.pauseEmulation()` from `EmulationFragment.onPause()`. The earlier system-server watchdog remains a separate recorded interruption; the logs do not establish a single cause for every reported closure.

A controlled launch directly into Eden's game activity, with Companion stopped, reached the title and camp menu. Reopening Companion on the lower display after that preserved the same Eden process and the visibly running game. A released controller-B check did not close Companion. Repeatedly bringing Eden's library activity forward is not used as an emulation-resume method.

During this check the hub exposed another context error: Camp owns a typed scene/rail/HUD even behind its animated menu. Its controller remained in its no-op state; the active expedition, including its in-game Pause Menu, used the statically verified expedition state. Code49 requires that expedition state before publishing Camp gameplay, resets route history when returning to the hub, and leaves the ownership anchor cached for fast re-entry. The new native library still returned the paused real expedition and72 remaining photos. Hub rejection and installed-app acceptance are checked separately.

The startup-only pause-overlay helper now waits for a current recognized rendered game frame or verified native gameplay before connecting its window inspector. It no longer inspects an unknown/loading emulator solely because its activity is visible. This is a defensive startup change; sustained system/firmware stability still needs acceptance. All180 Android/JVM tests,73 Node checks,6 diagnostics, native regressions, release lint and original identity/signer checks passed.

Code49 was installed in place successfully: **0.5.30-dev.3**, SHA-256 `f35929260db39ad24415e1ba3f4d744c14fd1ce0c8c554706d877ddca69aeb7c`. The same Eden process survived that update. The hub's corrected native check returned seeking→idle with no expedition/counter output; the visible false72 counter disappeared. After a controlled photo-free stop, Cocoon's actual selected Snap shortcut launched both apps and reached the title with both activities alive and no new Companion/Eden crash in the checked interval. This is one successful paired cold launch, not sustained acceptance.

A later scheduling comparison found all68 Eden threads in top-app for both displays. Scene changes confounded the apparent focus/FPS comparison; it does not establish a new priority fix or sustained60FPS. No graphics or clock setting was changed.


## Standard HUD ownership and repeated-launch repair — codes50–51

The closing report remained the priority. The observed earlier Android watchdog and Eden native pause ANRs are not declared universally fixed. Code49's readiness gate remains in this build. A subsequent targeted Companion exit-history check showed controlled/install force-stops rather than a new application exception; the new-build crash-buffer interval was empty during the checked gameplay.

An independent read-only check in Park Day Lv.3 found the game's named HUD registration. The exact HUD alias, shared header, current rail, live weak-world owner and that world's shared-owned HUD entry all agreed. The verified remaining count was 72, matching the visible game. Standard courses now use this bounded lookup and repeat the registry/world/HUD/vector/phase/counter checks; invalid or changing ownership withholds the native count and preserves the original screen-reading fallback. This is a Park Day hardware result, not all-course coverage. Camp's separate 72→71→72 shot/retry evidence remains in the earlier record.

The private diagnostic, which includes additional registry reporting beyond production output, collected 100 warm paused samples: median 1,614 microseconds, p95 15,582 and maximum 29,809. All 101 counter-bearing samples, including the first, read 72. These are helper-read latencies including its periodic mapping refresh; they exclude Java/UI/OCR overhead and do not establish sustained gameplay FPS.

Code50 (0.5.30-dev.4) installed in place while preserving the same Eden process. An empty Park Day run reached its zero-photo/zero-Pokémon/zero-point results. Map zoom, camera cone, movement and a physical B press were observed without a new crash. The Home test revealed that Cocoon handles Home across both displays; restoring the existing emulation task resumed the game. This launcher transition is not counted as an unexplained crash or uninterrupted background acceptance.

That test also reproduced two dashboard activities in one task after mixed explicit/launcher intents. Each activity owns its own WebView and reader/controller lifecycle. Code51 changes the sole dashboard activity to Android's single-task launch policy so reopening reuses the existing screen. Its application ID, launcher component, signer, data paths and normal Back navigation remain unchanged.

Final APK: **0.5.30-dev.5 / code51**, SHA-256 `20c93bb640e937853b0ef04874b291339d3573b868e7d4c0dc7847486a78d446`. Installation succeeded with the same Eden process. All 180 Android/JVM tests, 73 Node checks, 6 diagnostics, expanded native lifetime/concurrent-replacement regressions, release lint, publication boundary and baseline upgrade/signer checks passed. Saved progress remained 114/936 Diamond categories. No game graphics or clock setting was changed, and no Photodex photo was submitted/replaced.

Three mixed explicit/launcher reopen intents on installed code51 all reused the same single activity and Eden process. The zoomed map remained visually unchanged; screenshot comparison found only negligible pixel variation, not a perfectly identical bitmap. After the zero-photo retry, the installed dashboard reacquired Park Day Lv.3 and displayed 72 photos left. The standard capture health diagnostic reported active/current input, 242 successful readings, no inference failures and a fresh 59.9 reported FPS sample at its checkpoint. This used the supported service dump and whitelisted counters; it did not open/export the private native packet.

The final crash-buffer check from code51's launch time contained no new entries. Snap is left in its own Pause Menu in Park Day Lv.3 with 72 photos remaining, Companion's zoomed map open below and Follow enabled (Park Trail32% / current-path course24%). Temporary device diagnostic files were removed. The temporary plugged-in keep-awake preference was restored to its exact original value2 and verified. These checks are a passing short-session checkpoint, not proof that the earlier Android watchdog or Eden native pause deadlock cannot recur. No GitHub publication occurred.
