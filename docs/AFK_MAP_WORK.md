# Implementation checklist — 0.5.30-dev.3

The user authorized extended unattended development, connected Thor control and in-place APK updates. The original app ID, signer, private data, appearance and historical v0.5.24 artifacts are preserved. This work is local; no GitHub release is published.

## Implemented

- [x] All30 course reference maps and143 prepared terrain/route layers in the APK.
- [x] Fully revealed reference terrain, including the complete Forest illustration.
- [x] All1403 authored location records; Pokémon model IDs resolve without unmapped species.
- [x] Pokémon portraits, Crystabloom/landmark markers, clustering and location cards.
- [x] Current four-category collection coverage and missing-Diamond filtering; no duplicate historical photos counted.
- [x] Received, unfinished LenTalk filtering and relevant request links from a Pokémon map card.
- [x] Map expansion from Live and Guide, pinch/wheel/button zoom, pan, fit and follow.
- [x] Remembered map-layer preferences; live readings still start automatically.
- [x] Integrated illustrated route position/travelled trail; separate tracker tab removed. Named section and current-path course progress.
- [x] Geometry-based rail identity, required/conditional rail-change graph, unequal-branch/rejoin progress and honest cold-opening ranges. Broad branch hardware acceptance is still pending.
- [x] Fast game-only map feed; no reader token, PID, raw pointer or timing metadata reaches WebView.
- [x] Stale, future, other-course and transition samples cannot drive the player marker or camera cone.
- [x] Historical code40 tool validation (map buttons subsequently removed at user request): Fluffruit, Illumina Orb, Melody and Scan. All four produced their visible game effects on Thor.
- [x] Tool requests check fresh native gameplay, the upper Eden window, visible drawer/paused overlays, exact built-in controller and held-key state; each press has a fixed80ms duration and always attempts release.
- [x] Original passive controller feedback and saved-data/photo-selection features retained.
- [x] GPL/NDK notices bundled, native symbol/map packaging checks and original signer/upgrade checks.
- [x] Consolidated code42 APK installed in place. Sands Night movement/section progress, Park Day automatic detection and required Flower Field transition checked on Thor; original114 Diamond categories retained.

- [x] Exclusive All/Missing Diamonds/LenTalk/Hidden modes, fixed-size map-anchored clusters and a compact horizontal control bar.
- [x] Map photography-tool buttons removed; full-width section/course progress retained.
- [x]49 shared branch/rejoin seams aligned with bounded corrections; skipped short rails, ambiguous transitions, unrelated cycles and loop history covered by model regressions.

- [x] Remove zoom percentage; Follow defaults on and only yields to its toggle or map gestures.
- [x] Prominent Live map-expansion entry, smaller FPS/Thor-hour readings and symmetric compact credits.
- [x] Continuous measured trail tip, cached longer body and bounded gap-preserving alignment smoothing.

## Still open

- [x] Expand locally reviewed alignment to portions of30 illustrations/121 rails, including Jungle main routes, Cave, Camp circuits/laboratory and three additional live-verified Forest rails.
- [ ] Place the two withheld short rails and unverified Forest variants using reliable ownership/scene evidence. Verify new calibrations in gameplay; local artist review is not hardware acceptance.
- [x] Verify render-camera direction and zoom on Thor; display the guarded view cone independently of the vehicle travel arrow.
- [x] Qualify Camp controller/scene ownership, universal scene render context and native Camp remaining-photo count across a normal shot and retry (72→71→72).
- [ ] Qualify remaining-photo world/HUD ownership in standard courses; they retain their original screen-reading provider.
- [ ] Validate all30 courses, every branch/Illumina/Forest variation, process restart and long/thermal sessions on hardware. Thirty local browser map checks are not thirty gameplay acceptance runs.
- Removed from the requested scope: additional touch tools; the user requested removal of the map tool buttons. The passive controller strip remains.

## Evidence and scope

EcologicalBookData has1403 location records across30 course IDs. The fields encode model/feature ID, sighting serial and artist coordinates in a3840×2160 reference space. The sighting serial is **not** a research level. Model IDs resolve through the existing saved-species index+1 mapping. Feature IDs1001/1003/1005/1007/1009/1011 are displayed as Crystablooms; other special records retain the neutral Landmark label. Sightings describe authored map locations across levels, not current Pokémon actors or request-specific poses.

The illustrated map is schematic. Reviewed per-course alignments plus per-interval rejection produce approximate route positions; this is not a surveyed world projection. The fixed vehicle symbols remain labelled Start. See [ROUTE_MAP.md](ROUTE_MAP.md) for the123-rail catalog, branch/progress handling and exact coverage limits.

The first Android synthetic event route could open Eden's menu because those events did not retain the required physical-controller identity. It was replaced with a narrowly scoped controller-port output. The memory descriptor remains read-only. Freshness/window/phase guards remain in front of that output.

No private packets were exported. Automatic review had previously rejected exporting a packet containing session/process metadata; that export was not retried or bypassed. Hardware checks use visible app/game behavior and fixed-purpose released inputs. See [the hardware record](THOR_READER_VALIDATION.md).

## Follow-up implementation and acceptance

- [x] Reconcile current README/compatibility/architecture/privacy and historical Duo-module notes; the APK is the delivery target.
- [x] Preserve map zoom across temporary missing/recovered course readings; real-browser regression added.
- [x] Render high-resolution portrait pins in an unscaled screen overlay to avoid parent/inverse-transform blur; verify fixed size, native source resolution and gesture alignment.
- [x] Replace the tiny standard-only wrapper scan with bounded typed-scene discovery and controller-specific current-scene checks. The actual bundled reader reacquired paused Camp after a retry.
- [ ] Complete full app/game restart acceptance across Camp and standard courses, including long-session performance.
- [ ] Identify and validate live Pokémon ownership, identities and positions before offering actor-based map markers.
- [ ] Identify the current camera target and verified request-pose/behavior facts; reference suggestions must not claim these readings meanwhile.
- [ ] Read and validate current-run ungraded photo records before implementing automatic photo ranking. Preserve the saved-category comparison helper and never submit/replace photos automatically.

## Startup and hub safeguards — code49

- [x] Require the verified Camp expedition controller state; its animated hub's background scene cannot publish a player location or photos-left counter. Ownership remains cached so a real expedition can reconnect without a new broad sweep.
- [x] Defer the automatic native pause-overlay inspector until a fresh recognized game frame or verified native gameplay exists. Loading/unknown/stale/hidden contexts do not trigger it; live startup remains automatic.
- [x] One controlled paired-shortcut cold launch reached the title with both apps alive and no new crash in the checked interval.
- [ ] Complete repeated foreground/background and long-session stability acceptance. Android's system-server watchdog and Eden's native pause deadlock were observed during recovery; they are not counted as passing long-session tests.
