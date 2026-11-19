# Game-asset UI refresh — 0.5.31-dev.1 / code52

This is a local APK update for the original package and signer. The last installed
Thor build at the start of this UI work was code51. No device installation or
interaction was needed for this visual pass.

The UI uses 25 selected original game textures plus the game's native Nintendo
controller-symbol font. These assets come from the owner's already decoded game
files, independently of live memory reading. The source member selection lives in
`tools/data/snap-skin-assets.json`; `tools/prepare_snap_skin.py --owned-root …`
reproduces their preparation. Rights remain with the original owners; the bundled
`snap-ui-art.txt` records this distinction.

## Requested changes

- [x] FPS lens/dial with a native photo-label tab; a patterned hours ticket and
  research stamp. Values still use reported Eden FPS and Cocoon Thor totals.
- [x] Four native ribbon tabs with separate accents, illustrated icon mounts and
  a gold selected state.
- [x] Current-course scene with native viewfinder trim, island sticker, time-of-day
  emblem, research-level label and an angled map-opening ribbon.
- [x] A framed photos-left ticket, keeping unknown values absent.
- [x] Suggested Pokémon on a clipped-on photographic print, clearer four-category
  star groups, separated refresh arrows and an illustrated Photo Book route.
- [x] Compact recent-snapshot prints with a landscape saved-photo area, score and
  native stars; a clearer Live LenTalk request note and full-silhouette cutout.
- [x] A single compact row of actual native A/B/X/Y/L/R/ZL/ZR, D-pad, plus/minus
  and stick glyphs, with native filled pressed states and analogue movement.
  Thor key identities and passive feedback freshness are unchanged. The row
  sends no game input.
- [x] Expanded map island header, original course-band trim, orange close button,
  paper-ribbon controls, diamond zoom keys, compact filter note and revised
  section/current-path course progress presentation.
- [x] Map-sighting print with an unclipped cutout, native star groups, an explicit
  Photo Book ribbon, clearer request links and compact grouped-location choices.
- [x] Shared close buttons and sheet-header palette match the revised interface.

## Behavior preserved

The single-task launch fix, startup readiness guard, game-memory reader, save/photo
sync, request status decoding, map geometry/branch calculations, stale-reading
rejection, camera cone and portrait-overlay rendering are unchanged. Suggestions
remain references based on known course/level and saved progress; this UI pass does
not add live Pokémon actors, target/pose detection or photo ranking.

Map zoom/follow policies, marker dimensions and portrait source resolution remain
intact. New decorative assets do not add a timer, continuous animation, extra
screen capture, another reader or another controller subscription.

## Local verification

- Real-browser checks at 540×470 (Thor layout), 540×405, 620×540 and 412×640:
  Live fits one screen, native feedback expires correctly, suggestions refresh,
  Photo Book routing works, Photo Selection retains all four reference cards,
  long Pokémon cutouts fit and saved snapshots keep a landscape photo area.
- All 30 map views load their images and fit header/controls. Checked live marker,
  camera cone/progress with an explicit offline fixture, exclusive filters,
  retained zoom across filter changes, sighting-to-Photo-Book return and close.
- Existing real-browser regression still covers zoom persistence across temporary
  course-reading gaps and fixed-size, unscaled high-resolution portrait overlays.

These browser fixtures contain invented example progress, not personal saved
photographs. They do not count as Thor gameplay or long-session crash acceptance.

Final local build passed: 180 Android/JVM tests, 73 Node model checks, 6 diagnostics, native reader regressions, 3 browser acceptance tests, release lint, publication audit, and original-package/signer upgrade checks. APK SHA-256: `eff5c265ee3b8e905ab4dfd9a2b8b8e0d7e70ca271268b1dd5d031f5180844e9`. No Thor deployment or GitHub publication was performed in this UI pass.

## Deployment after the user's “push”

Code52 was installed in place on Thor and Companion reopened on display4. The original package and baseline signer were verified before installation. The same Eden process survived, with Snap still in its existing in-game Pause Menu; no gameplay inputs or settings changes were sent. The installed UI reacquired Park Day Lv.3 and 72 photos left, retained 114/936 Diamond categories and 28.2 Thor hours, and loaded the native controller glyphs and refreshed artwork. One Companion activity was present and the checked post-install crash buffer had no new entry. This is a deployment/startup check, not long-session or all-course gameplay acceptance.
