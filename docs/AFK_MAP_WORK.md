# Implementation checklist — 0.5.27-dev.2

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
- [x] Stale, future, other-course and transition samples cannot drive the player marker or touch buttons.
- [x] Four fixed touch tools: Fluffruit, Illumina Orb, Melody and Scan. All four produced their visible game effects on Thor.
- [x] Tool requests check fresh native gameplay, the upper Eden window, visible drawer/paused overlays, exact built-in controller and held-key state; each press has a fixed80ms duration and always attempts release.
- [x] Original passive controller feedback and saved-data/photo-selection features retained.
- [x] GPL/NDK notices bundled, native symbol/map packaging checks and original signer/upgrade checks.
- [x] Consolidated code42 APK installed in place. Sands Night movement/section progress, Park Day automatic detection and required Flower Field transition checked on Thor; original114 Diamond categories retained.

## Still open

- [ ] Complete the remaining illustration alignments: Jungle main rails, disconnected Forest seasonal scenes, Outaway Cave and Research Camp. Portions of27 course illustrations/92 rails have bounded schematic alignments; rejected intervals remain unknown.
- [ ] Verify the camera viewing direction and view cone. Current heading is vehicle travel direction.
- [ ] Resolve active HUD ownership across retries before using its remaining-photo counter. A typed counter candidate was found, but the experiment was removed from the shipped reader because retry ownership is not proven. Existing screen-reading features retain their prior provider.
- [ ] Validate all30 courses, every branch/Illumina/Forest variation, process restart and long/thermal sessions on hardware. Thirty local browser map checks are not thirty gameplay acceptance runs.
- [ ] Any further touch controls such as aiming/turbo/shutter need separate state/input validation; there is no arbitrary-key or guest-memory write surface.

## Evidence and scope

EcologicalBookData has1403 location records across30 course IDs. The fields encode model/feature ID, sighting serial and artist coordinates in a3840×2160 reference space. The sighting serial is **not** a research level. Model IDs resolve through the existing saved-species index+1 mapping. Feature IDs1001/1003/1005/1007/1009/1011 are displayed as Crystablooms; other special records retain the neutral Landmark label. Sightings describe authored map locations across levels, not current Pokémon actors or request-specific poses.

The illustrated map is schematic. Reviewed per-course alignments plus per-interval rejection produce approximate route positions; this is not a surveyed world projection. The fixed vehicle symbols remain labelled Start. See [ROUTE_MAP.md](ROUTE_MAP.md) for the120-rail catalog, branch/progress handling and exact coverage limits.

The first Android synthetic event route could open Eden's menu because those events did not retain the required physical-controller identity. It was replaced with a narrowly scoped controller-port output. The memory descriptor remains read-only. Freshness/window/phase guards remain in front of that output.

No private packets were exported. Automatic review had previously rejected exporting a packet containing session/process metadata; that export was not retried or bypassed. Hardware checks use visible app/game behavior and fixed-purpose released inputs. See [the hardware record](THOR_READER_VALIDATION.md).
