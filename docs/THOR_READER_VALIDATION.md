# Android reader validation — 2026-11-18

The user authorized control, diagnostics and in-place APK installation. Current installed development APK: **0.5.25-dev.2 / versionCode38**, original package and baseline signer. The historical v0.5.24 artifacts remain unchanged. No GitHub publication occurred.

## Implemented and observed

- The APK automatically launches a separate persistent Thor helper. A v2.0.1 fingerprint check precedes native sampling. Reads are restricted to this game's readable HostMemory mappings in the fixed Eden process; the native adapter opens process memory read-only.
- Native sampling runs at 20 Hz. The visible dashboard consumes fresh active-course and selected-level packets. Its packet boundary rejects wrong tokens/PIDs, old/future samples, invalid dimensions and discovery-only states.
- Park Day: deliberately selected **level2 below saved maximum3**. Runtime field returned course1/selected-level2. Both fixed and advancing rail/world coordinates were observed. Paused prototype:45 ready samples, median native read276 microseconds; moving prototype:34 ready samples, median267 microseconds. These figures exclude Java publication/UI/capture overhead and are not sustained-FPS measurements.
- Returning that empty test run to camp produced no ready course/position samples in an8-second prototype check. The reader returned discovery state; this is not a claim that every non-course screen is classified.
- Installed APK dashboard displayed **Florio Nature Park / Night / Lv.2** during the Night test run. Restarting only Companion while Snap was paused mid-course recovered the same course/variant/level with no course/level label visible in the game. This checks cold recovery rather than inherited intro-screen context.
- Existing interface and displayed progress were retained across in-place updates. The emulator process was not restarted and no emulator settings or game/save files were edited.
- A controlled, immediately released built-in controller input route worked for menu navigation. That was development tooling, not a shipped product shortcut. The native memory adapter has no game-write or input API.

## Limits still open

- All-course/level/branch/Illumina and scene-transition acceptance, process restart recovery on hardware, long sessions and performance comparisons.
- Illustrated-map calibration and expansion, live map marker presentation, branch identity and Pokémon overlays.
- Finished product touch controls and their state/input handling. Existing controller feedback remains passive.
- Course/level readings now have a memory provider. Other existing saved-data/OCR features retain their previous providers; screen recognition has not been eliminated wholesale.
- Position and direction reach the private snapshot only. No illustrated-map player position is claimed until alignment is verified. Unsupported curve modes remain unknown.

The two-stage discovery reader rescans the small-allocation area independently of its slower fallback sweep; entering a new course must not wait for the fallback sweep to finish. Once a typed loader is found, it follows the owned scene and revalidates its control block, parent, course, selected level and rail references. Expired values cannot be substituted with a timer.

169 JVM/Robolectric tests,38 UI model tests,6 diagnostic tests and release lint passed for the APK;6 native packaging/contract tests also passed. Earlier native curve math checks and owned-map decode comparisons remain research evidence, not map alignment or Eden Duo runtime acceptance. Device screenshots, memory diagnostics and preference backups are private and excluded from source publication.

Automatic approval review declined exporting the app's private packet through shared device temp storage because it included session/process metadata. That export was not executed; the end-to-end Night/cold-recovery result was checked on screen instead.
