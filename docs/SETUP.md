# Setup and maintenance

## Install and pair

1. Run your own compatible New Pokémon Snap 2.0.1 copy in the original Eden package (`dev.eden.eden_emulator`) on the supported AYN Thor firmware. Create a normal game save.
2. Download the signed APK from [Releases](https://github.com/gbppimentel-dev/snap-companion/releases/latest). Install it over the existing companion if present.
3. In Cocoon's Snap shortcut options, choose **Use Companion → Snap Companion → bottom screen**.
4. Launch normally. Saved-data, supported screen-reading and bundled native-reader connections start automatically.

Respond to Android permission prompts if presented. Screen capture uses Android MediaProjection; the supported firmware helper supplies its existing local access. There are no manual course/progress fields and no separate Duo mod to install. Unsupported or unknown readings remain neutral.

## Updates

Install the new signed APK over the existing app. Do not uninstall or clear data. Package `dev.gbppimentel.snapcompanion`, original signing certificate and private-data paths are preserved. A source build signed with a different key cannot replace the distributed APK.

## Read the screens

- **Live:** current supported course/variant and selected level, reference photo suggestion, recent snapshots and course requests. Tap **Explore Course Map** to expand. Photo Selection keeps this ordinary dashboard; no automatic comparison or grading page opens.
- **Map:** default Follow tracks a mapped NEO-ONE reading. Drag/pinch or use the Follow toggle to explore, and Overview to fit the illustration. Zoom is retained through short reading gaps. Use one Pokémon mode at a time: All, Missing Diamonds, LenTalk or Hidden. Map gestures do not steer the game.
- **Photodex:** saved stars, scores, medals and current official images. My Photos contains at most four current registered slots. Photo Tips separates alternate opportunities with Option/OR cards and offers specific reviewed methods where available.
- **LenTalk:** saved request statuses and full offline reference solutions. Optional Step-by-Step notes appear for supported reviewed requests.
- **Guide:** island/course encounters and category coverage. Its course/level filters browse reference information; they do not change the game.
- **Header:** available FPS, Cocoon-recorded Thor playtime, clock, sync status, battery icon/percentage and the credits button. The battery percentage is current charge, not battery health or remaining capacity.

## When something stays unknown

Fresh verified game-memory context takes priority for supported current course and selected-level readings. Saved maximum/unlocked level is never substituted for the level of the current run. Exact build/ownership checks, firmware access and the original Eden package are required. English text anchors still supply supported screen-only observations.

The illustrated map is partly aligned and approximate. A reference pin is not a detected Pokémon. Unaligned intervals stay unmapped. A brief handoff may show **Last Position · Reconnecting** with a stationary dashed marker; it expires rather than guessing movement. Full-course totals remain neutral where continuations are incomplete.

Photo metadata can arrive before the copied image. Copies refresh during supported quiet scenes; a missing image may show a developing placeholder. An image already black in the game save cannot be reconstructed from missing pixels.

Missing firmware access, multiple ambiguous saves, another emulator package or a different game build can prevent the link. A green connection dot does not prove every live field is available. See [compatibility](COMPATIBILITY.md) and include app/game/emulator versions when reporting a problem. Do not attach personal saves, photographs or raw memory packets publicly.
