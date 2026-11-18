# Evidence required before live tracking is called complete

No Thor inspection or Eden Duo installation has been performed for this mod. The current environment is regular Eden. A later authorized Eden Duo session is needed to identify and validate actual run objects. Read-only research must be distinguished from installing an emulator, copying saves, modifying settings or sending inputs.

A prebuilt **SCN Read-only Validation** package can be prepared locally for that session. The Reader Check page and `SCN_DIAGNOSTIC` log lines expose candidate course, selected level and measured-distance curve position. They are explicitly unverified; the normal companion's trusted fields remain unknown. This package does not draw a player pin on an uncalibrated illustration, implement Pokémon locations, port the existing interface or establish original-APK compatibility. Installation is a separate action and has not occurred.

For each supported full NSO build ID, record the source function or object layout, field type, offset, ownership/lifetime, phase checks, bounded read size, NCE relocation behavior and confidence. Keep raw dumps, executable bytes and saves private. A readable string, pattern hit or a plausible number alone is insufficient.

1. Find a live run owner and distinguish in-course state from menus, loading, photo selection, grading, laboratory and retry transitions.
2. Compare course/variant IDs to the game's own course display, including Florio day/night, alternate courses, Illumina courses and update content. Check at least two variants sharing a loaded LC archive.
3. Compare selected research level to the level being played, using a lower unlocked level as a counterexample to saved maximum-level shortcuts.
4. Verify actual NEO-ONE pose, ownership and heading independently of camera rotation. Check stopped, moving and accelerated travel, branches, exits and multi-segment maps.
5. Establish world-to-map projection from actual game metadata where possible. Otherwise record independent calibration and held-out positions, including edges and branches. RouteData coordinate strings are not yet proof of this transform.
6. Restart the game and emulator, then verify after ASLR/heap changes. An invalid or changed owner must clear the marker, image and previous course rather than hold stale state.
7. Verify original map/route/variant associations. Pokémon locations must honor level, day/night, branch and trigger conditions. A reference marker must never be represented as a currently active actor.
8. Verify bottom-screen layout and touch behavior in Eden Duo, including map expansion, pinch/pan/reset, toggles, pending images and leaving/reentering courses. Companion taps initially affect only the companion.
9. Measure actual module sampling time, image-worker work, memory use, frame pacing and game FPS on Thor under NCE, including long runs and course transitions. Compare with the companion hidden. There is no promise that a companion change fixes an emulator/game slowdown.

The release gate requires a matching `research/device-validation.json` receipt with result `passed`, the full build ID, and passed checks for `course_variants`, `selected_level`, `routes`, `second_boot_aslr`, `map_projection`, `transitions`, `performance`, `nce`, `touch_map` and `stale_clear`. No receipt or production offsets are fabricated from the synthetic tests.

Host verification covers build gates, reader bounds, invalid data, transition rejection, NCE relocation arithmetic and projection math using explicitly synthetic objects. The private map verifier uses the owner's files read-only. Neither establishes a live runtime mapping.
