# Integrated route map — 0.5.28-dev.2

The route tracker is part of the illustrated course map. The separate tracker tab and diagnostic grid are gone. Existing terrain, Pokémon/landmark/request layers, location cards and fit/follow/pinch gestures remain available. Photography-tool buttons were removed at the user’s request; both progress bars use the full footer.

## Readings and presentation

- The read-only native sampler supplies course, selected level, rail distance/length, directed sampled rail geometry, pose, phase, run identity and validated render-camera orientation/lens readings. No elapsed-time position is substituted.
- Directed geometry and length match a factual catalog of **120 NEO-ONE rails across30 courses**. Same-length foreign geometry and ambiguous matches are rejected.
- `route-data.js` contains bounded rail points and rail-change events from the owned level tables. It contains no process addresses, tokens, saves, executable code or runtime packet captures.
- Section names are **companion labels**, not claimed native/localized section-name readings. Sands Night has Opening Dunes, Oasis and Sand Pits presentation sections; checked Park rails have Park Trail, Dam Crossing and Flower Field labels. Other matched rails retain neutral Main Trail/Alternate Trail names until their landmarks are reviewed.
- The section bar measures the current named section. The course bar measures the current route through required continuation rails. Selecting a different path can change the remaining route length and percentage.
- The gold line shows measured travelled portions. An observed scripted rail transition can confirm travel up to its authored event. Nothing is drawn through a rejected alignment interval.
- The small arrow follows the aligned route tangent. A separate fading cone follows the validated render-camera direction and narrows with the lens. It uses the same approximate illustration projection, hides when stale/unmapped or nearly vertical, and never falls back to vehicle heading.
- All Pokémon, Missing Diamonds, LenTalk and Hidden are exclusive Pokémon modes. Landmarks and route artwork are independent switches. LenTalk is not accidentally intersected with collection status.
- Map-coordinate clustering is deterministic and independent of panning. Pins counter-scale to35CSS pixels; zoom buckets expand groups without repeated regrouping during gestures/live samples. Dragging can begin on a pin without opening it. Compact controls provide Overview, Follow and zoom.

## Split paths and progress

A level's initial NEO-ONE path and path-action payloads identify the rail catalog. Ordinary Pokémon path actions share the payload type, so the export checks both the target-actor field and the NEO-ONE selector. Point-event action groups identify conditional and unconditional rail changes at measured distances. Required transitions trim unused rail tails before continuing on the next rail; optional branches are followed after the matching destination rail is observed.

The current default continuation supplies the remaining distance. Once a fork is taken, progress uses the prefix actually travelled and that destination's continuation. Both alternatives are never added to the travelled distance. After a rejoin, a cold opening can recover a **range** from the possible prefixes. Unknown/cyclic histories or ambiguous required continuations remain neutral. Observing a branch choice narrows the range.

Some rail changes are triggered outside the rail's point-event list, including collision/scan conditions. When one is observed, the app bounds the missing departure distance between the last measured point and the previous rail end and marks overall progress **≈**; it does not extrapolate a missing distance from time or assume a fabricated trigger location. A short missed rail can be inferred only from a unique graph-confirmed transition chain; ambiguous chains retain prefix ranges. Unrelated cycles do not erase progress, known self-loops preserve travelled history, and unmatched geometry clears old trails. A cold opening on such a disconnected branch can leave course progress unknown while section progress and a valid map position still work.

Current metadata uses the course's map stem to select its level. In particular, Park and Secret Side Path share a course archive; archive variants are not all sequential legs of Park. Illumina naming exceptions are accepted only when the extracted course pack has exactly one candidate map level.

## Illustration alignment and limits

The art is a schematic, with exaggerated features, and does not share a single exact world projection. Reviewed per-course alignments project measured rail distances onto the authored route strokes. The UI explicitly says **Approximate Route Position**. Individual high-error, out-of-bounds or discontinuous intervals are rejected, and the player disappears in those intervals instead of jumping to an invented location.

There are alignments for portions of **27 course illustrations /92 rails**. Explicit event anchors and bounded local corrections make49 measured, shared branch/rejoin junctions meet on the illustration. Only world junctions within0.15 units and artist gaps within8pixels are stitched; unrelated scenes and rejected intervals remain separate. The main Jungle Day/Night rails still fail the alignment acceptance threshold. Elsewhere Forest's disconnected seasonal areas, Outaway Cave's distorted layout, and Research Camp's multiple starts are withheld entirely. Their reference maps and valid section readings remain usable. This coverage is not all-course/all-branch hardware acceptance.

The alignment source is `tools/data/route-map-alignments.json`. `tools/prepare_route_calibrations.py` regenerates bounded schematic anchors from bundled art and factual rail data using Pillow/NumPy/SciPy. `tools/prepare_owned_routes.py --level-root <private-extracted-level-folder>` regenerates rail facts from the owner's local metadata; raw levels and archives remain private. Neither tool contacts Thor or reads a save.

## Validation

Model checks cover all catalog identities and destinations, calibration bounds/gaps, unequal forks, required transitions, rejoins, cold-opening ranges, missing interactive triggers, stale/future/wrong-course samples, rewinds, run changes and cyclic/ambiguous continuations. Isolated browser checks cover all30 illustrated views, prepared images, terrain selection, map-card navigation/back, fixed-size map-anchored pins, exclusive filters, drag gestures, the integrated marker/cone and transition guards. Hardware camera evidence includes100/100 valid production-reader frames while turning and zooming, with vertical FOV changing from36° to14.4°. These checks do not establish all-course runtime ownership, exact real-world map geometry or sustained60FPS.

The original application identity, signer and saved-photo provider are unchanged. The native camera addition follows an owned gameplay-world/primary-render-camera chain, checks the camera type/update function, shared ownership, consistent double reads, pose plausibility and revalidates ownership after sampling. Only normalized forward/up vectors, vertical FOV and aspect reach presentation; no camera addresses or process metadata do. Historical v0.5.24 artifacts remain immutable. Hardware checks are recorded in [THOR_READER_VALIDATION.md](THOR_READER_VALIDATION.md).
