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

## Local corrections — 0.5.31-dev.2 / code53

The user reported clipping/alignment issues after code52 and requested local work
while using Thor. This pass does not access, control or update the device.

- Tighter main tabs; icon diamond backplates removed.
- Content-sized playtime ticket, corrected number/unit baselines and a separate
  readable Playtime caption. The approved FPS treatment is retained.
- Inset Current Course label clears viewfinder strokes; the map action has a
  rectangular paper frame with explicit, padded icon/text columns.
- Single open refresh arrow, clearer star-category mounts and an inset Photo Book
  emblem/arrow; slightly straighter print mount. Recent Snapshots and Course
  Request designs are retained.
- Controller row has upper clearance; native L/R stick caps move inside separate
  wells instead of receiving a dot over the letter. Passive feedback still
  expires as before and sends no gameplay input.
- Close/+/- symbols are original font outlines in ink-centered SVG view boxes,
  removing font-bearing offsets. Zoom's shared divider keeps both cells equal.
- Map filters use explicit icon/text/checkmark columns and padded option frames;
  decorative ribbon notches no longer intersect their contents.
- NEO-ONE has a larger fixed-size coral badge, white/teal outline, legible label
  and outlined heading arrow. It remains subordinate to controls/filter sheets,
  follows the same verified position, and hides with stale/transition readings.
  No continuous animation or new location estimate is introduced.

Browser checks now include long playtime values, inner icon clearance, centered
map symbols, analogue travel bounds, filter content containment and marker
zoom/stale behavior in addition to the earlier navigation/map regressions.
These checks use offline fixtures and do not count as Thor gameplay acceptance.

Final code53 local checks passed: 180 Android/JVM tests, 73 Node/model checks, 6 diagnostics, native regressions, 4 real-browser tests, release lint, publication boundary and original package/signer checks. APK SHA-256: `43a1ba750f5b9e98176eecd6384bcf649d2c405765213a315c0dd75be9ebd993`. Code53 was ready locally at that checkpoint; its subsequent deployment is recorded below.


## Code53 deployment and code54 control correction

Following “push and open snap”, code53 was installed in place with the baseline
signer. Snap was visible on the upper screen at its existing Retry Menu, with
Companion below. Android had been asleep; a wake action and a tap on Eden’s visible
resume overlay restored rendering. No save clearing or emulator configuration
change was performed.

The user then reported that L/R sticks, map +/− and close X artwork was missing,
and that the Playtime ribbon still looked wrong. All missing symbols used external
SVG masks. Earlier desktop checks allowed file access and checked their boxes,
which did not establish that the symbols painted under the APK’s restricted file
origin. The WebView security settings remain unchanged.

Code54 / 0.5.31-dev.3 renders the same six original, ink-centered font outlines as
inline SVG paths with inherited color. The generated control-art.js replaces the
six external SVG-mask files. All shared modal/map close buttons, zoom controls and
passive analog caps/wells use these paths. Existing controller state, expiry,
movement and button glyphs are retained. No gameplay input is added.

Playtime now groups a readable title directly above the number and `hrs` unit,
with no ribbon, stretched asset, floating caption or surrounding box. The FPS
readout is retained.

Local acceptance now runs without the permissive file-access launch flag. A new
pixel-difference check verifies that left/right stick caps, controller +/−, map
+/−, map/filter close and Photodex/credits close symbols actually paint visible
pixels. The existing layout, routing, controller expiry and map zoom regressions
remain in place. Local Live and map/filter previews were visually inspected.
Thor was not accessed during this correction; hardware visual acceptance remains
pending installation.

Final code54 local validation passed: 180 Android/JVM tests, 73 Node/model checks,
6 diagnostics, native reader regressions, 5 real-browser tests, release lint,
publication audit and original package/baseline-signer upgrade checks. Packaged
inline artwork was checked against source. APK SHA-256:
`b0e097412850164a1d238ae36bbdd6a2acda76423a038b6499d754c7ea7b0f28`.


## Code54 deployment

Following the user’s “push”, the exact verified APK above was installed with
`adb install -r`. Android reports versionCode54 / 0.5.31-dev.3. Original package,
signer and APK hash were checked before installation. No app data was cleared,
no save migration performed and no emulator settings changed. Companion was
launched on display4; this installation does not count as gameplay acceptance.
Cocoon remained visible after launch attempts while its library selection was
changing; Companion’s final on-device artwork was not visually verified in this
push. Further foreground changes were left alone. Installation/version
verification succeeded independently of that visibility check.
