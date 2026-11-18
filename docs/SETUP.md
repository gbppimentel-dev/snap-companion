# setup & maintenance

## install and pair

1. Install and run your own compatible New Pokémon Snap copy in the original Eden package (`dev.eden.eden_emulator`). Create a normal game save.
2. Download the APK from [releases](https://github.com/gbppimentel-dev/snap-companion/releases/latest). Install it normally.
3. In Cocoon, open the game's shortcut options → **Use Companion** → **Snap Companion** → bottom screen.
4. Launch the game normally. The companion opens its automatic saved-data and supported screen-reading connection on the target Thor environment.

If Android presents a permission prompt, respond to that system prompt. There are no manual course/progress fields to fill in. Unknown context remains neutral.

## updates

Install the new release APK over the existing app. Keep the package installed so Android preserves private records and cached photographs. The package name is `dev.gbppimentel.snapcompanion`; official release builds use the same signing certificate. A source build made with another key cannot replace an existing release without a signing conflict.

## reading the screens

- **Live:** course context where recognized, a reference suggestion and automatic Photo Selection assistance.
- **Photodex:** saved categories, medals and current official images. Each Pokémon's My Photos contains at most four current slots.
- **LenTalk:** saved request statuses and full reference solutions.
- **Guide:** offline course/species references. Research-level controls browse the guide and do not change the game.
- **Credits:** the GitHub-mark button beside the header clock.

## when something stays unknown

Supported English text anchors are needed for screen context. Opening mid-course may leave the course/selected level unknown until a supported transition or pause label appears. Saved maximum/unlocked level is deliberately not used as the selected level.

Saved-photo images are copied during supported quiet scenes. A new record can precede its locally available image. The sync summary distinguishes registered photos from images ready to display. An image already black in the game save cannot be reconstructed from missing pixels.

Only the original Eden package is targeted. Other emulator forks, multiple ambiguous saves, missing firmware access and different game data layouts can prevent the link. Do not treat a green connection dot as proof that every live field is recognized. See [compatibility](COMPATIBILITY.md).
