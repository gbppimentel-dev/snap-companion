# Live and reading refinements — code59

Requested follow-up to code58: reduce recent-image delay, contain fallback
portraits, improve suggestion artwork/Photo Book routing, remove the sync popup,
compact the entry header, center close artwork, clarify example locations,
redesign Photo Tips and offer more specific LenTalk instructions when available.

## Recent photos

The image queue previously grouped its last twelve changed categories in an
unordered set and then sorted by category key. A just-registered snapshot could
wait behind older changes. It now orders that group newest-first, matching the
history order used by Recent Snapshots. Existing copy rate, backoff, profile/save
validation, quiet-scene requirement and guards against returning to an expedition
during an image read remain unchanged.

This is a queue-priority correction, not a claim that a photograph exists before
the game saves it or that the app can safely copy images during active gameplay.
The existing gallery-completion generation invalidates the UI image index; a
ready image replaces the placeholder on the next regular UI update.

The fallback is an eager, contained cutout inside a bounded native-paper print.
Tall/wide species stay inside it. Actual photographs retain their landscape
frame. Suggestions use native paper, a faint research seal and an inset Photo
Book tab with a separate arrow. Synced Now is a plain status label; its popup
action and dialog function were removed.

## Pokémon entries and Photo Tips

- The entry header is 45 px instead of 58 px. Name, native typing and close
  control have distinct columns. Completion artwork sits on the portrait, and
  live completion updates use that same position.
- Native close artwork is exactly centered at a smaller 11 px inside the 29 px
  native circle. The entry, LenTalk and Photo Tips readers use this treatment.
- My Photos uses the dark native camera emblem rather than the white film icon.
- Photo Spots is now Category Examples, with an explicit reference caption and
  Example level labels. These are example shots for the selected star category,
  not a claim about the user's saved photograph or a LenTalk match. The data is
  neither promoted to a guaranteed minimum level nor treated as live sightings.
- Photo Tips is an illustrated field notebook: native cover portrait/seal,
  category stars, numbered capture notes, tools and illustrated course examples.
  Its entry action is an inset native-paper tab, with clear text and an arrow
  separate from the label. Long notes remain available within its scroll area.

## LenTalk

The popup keeps the original full general solution. Native paper, a clipped
postcard, purple cover texture, route ticket and numbered notes refine its
presentation. A Step-by-Step expander adds reviewed route/setup/timing details
where additional facts were checked. It starts closed and does not add a new
setting or hide the existing solution.

Thirteen requests have additional notes: 3,000-Degree Flames, A Bite for the
Bully of the Sea, A Cheeky Pose, A Figure of Beauty, A Finicky Flame, A Goofy Pose,
An Eevee Party, An Unusual Spot, A New Way to Dance, A Calming Air, Don't Be
Scared, Hide-and-Seek in the Flowers and Adorable Water Spray. Other requests
retain their original solutions without a fictitious detailed version.

The independently phrased factual notes and source provenance are maintained in
`tools/data/request-walkthroughs.json`. `tools/prepare_request_walkthroughs.py`
checks IDs/course ownership and generates the link-free `request-details.js`.
The publication audit checks that it matches its source and contains no URLs.
Source articles/images are not bundled. These are reference procedures, not
new gameplay acceptance or changes to game-reading data.

## Validation scope

Browser checks cover 540×470, 540×405 and 412×640 for the new cases, alongside
existing Live/Photo Selection/controller/map/Photodex/Guide checks. Added cases
exercise bounded Onix/Wailord/Vivillon fallback prints, immediate replacement
when an image arrives, score updates, noninteractive sync, shorter header/type
clearance, centered visible close glyphs, example wording, all thirteen optional
expanders, every detailed step, original general solutions and absent expanders
for unreviewed requests. The image-order regression uses synthetic JPEGs and
metadata and proves that a new snapshot is copied before older pending changes,
while no copy occurs outside a quiet scene.

No native sampler, bridge, rail/calibration/model, save decoder, emulator setting,
application identity or saved collection format is changed. No personal photos
or device captures are added to repository documentation.

Final local checks passed: 12 real-browser cases, 182 Android/JVM tests, 73
Node/model checks, 6 diagnostics, native regressions, release lint, publication
audit and original-package/baseline-signer upgrade verification. Source and
packaged UI files match. Native libraries, the bridge, guidebook, main reference
dataset and map/model/calibration/rail assets are byte-for-byte identical to
code58. Previews of Live, entry, Photo Tips and both LenTalk states were reviewed
at Thor and narrower dimensions with synthetic progress.

Final APK: `SnapCompanion-0.5.33-dev.2.apk`, versionCode59. SHA-256:
`71fc483bbf966144f904ce5f5233140bf39346a507155fd52e6a916f35a69585`.

## Device delivery

Installed in place under the standing direct-push authorization. Android reports
0.5.33-dev.2 / code59. Thor was asleep before installation and remained asleep;
the same Eden process survived. No wake, game input, app launch, emulator restart,
settings adjustment, data clearing or save operation was sent. Consequently the
new UI and actual next-photo latency have not been checked in this running game
session. The next Companion opening uses the new build. Browser/JVM evidence
above does not substitute for a new live gameplay/performance acceptance run.
