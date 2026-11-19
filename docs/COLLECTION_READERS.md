# Collection readers — code58

The user requested a visible redesign inside each Pokémon entry, clear
Previous/Next buttons, a more compact LenTalk tab and popup, and an illustrated
Guide. The approved compact Photodex grid is retained.

## Photodex entries

- Original red Photodex fabric, book trim, native type icons, paper textures,
  labels and photo selection artwork replace the generic entry presentation.
- The photograph is the focus: a fixed 3:2 area, slim medal/score caption and
  four category buttons previewing the current official saved photographs.
  Missing images retain a silhouette while their existing photo is developing.
- The target note contains tools, illustrated course examples and an inset
  Photo Tips action. Course examples remain reference locations, explicitly
  labelled Examples when appropriate; they are not new live sightings.
- Previous/Next have separate native gold arrow buttons, portraits and name
  areas. Text does not sit on a stretched ribbon point.
- About includes the complete short Pokédex entry, measures and grouped
  illustrated course tickets without dropping the existing form/level notes.
  Appearances retain free transparent cutouts. My Photos still shows only the
  four official registered categories, with fixed contact sheets and expansion.

## LenTalk

- Four landscape postcards across the Thor layout, three on narrower screens.
  Course artwork fills the image; transparent Pokémon cutouts have a subtle
  outline. Day, Night, Evening and research badges use distinct native art.
- Completion ribbons occupy the corner; levels sit in the caption row. Pending,
  received and in-progress states remain distinct from completed requests.
  Small native tool icons share the image area without another text row.
- The popup uses a native illustrated identity, compact course/time/level/tools
  ticket, reward previews and numbered full-solution notes. Short requests shrink
  to their content; long solutions scroll within the reader. Solutions remain
  visible by default. Existing live status changes update the open request.
- The same fixed search and shared location/filter sheet are retained.

## Guide

- Native island badges group two-column illustrated course tiles. Course names
  include their time variant; badges distinguish time/research variants.
- Each tile retains medal coverage, Diamond counts and complete Pokémon counts.
  The course reader has a native island/course header, compact saved research,
  Diamond-first coverage, level buttons and larger encounter portraits.
- Every encounter retains all four saved star categories and the completion
  sash. Internal Photodex and course-map routes remain available.
- Browsing levels changes only the reference view. Saved research and current
  game course/level remain separate; no guessed live values are introduced.

## Assets and scope

Twenty-four additional lossless WebP assets (about 80 KiB total) are prepared
from the owner's original game resources. Their exact source IDs are recorded
in `tools/data/snap-skin-assets.json`, through the existing preparation pipeline:
eighteen native type icons plus book paper, note paper, label, trim, photo
selection frame and gold arrow-button art. Existing image rights notices apply.
No external fonts, remote images or generated imitation game assets are added.

`collection-pages.css` owns these readers and the LenTalk/Guide layouts. Older
book-specific rules were removed from `snap-skin.css`; shared Photo Tips styles
remain. Live, compact Photodex, map presentation and all game readers retain
their existing behavior. No native reader, bridge, save sync, model, input
observer, gameplay setting or application identity is changed.

## Validation

Browser checks cover Thor, short landscape and narrow portrait layouts with
invented progress and prepared course thumbnails as photo stand-ins. They check
all 234 entries' separate neighbor controls, loaded native type art/category
previews, fixed photos/albums, full notes, postcard artwork and ribbon clearance,
live request-status changes, complete long solutions, short popup sizing, reward
art, pinned search, all 30 courses/six islands, level filtering, encounter stars
and internal map/Photodex routing. Existing Live, controller artwork, Photo
Selection, filters and map regressions remain included.

Personal photographs and device captures are not repository previews. These UI
checks do not claim new course alignment, moving-game FPS or long-session
stability coverage.

Final validation passed: 73 Node/model checks, 6 diagnostics, native regressions,
181 Android/JVM tests, release lint, publication audit, ten browser acceptance
cases and the original-package/baseline-signer upgrade check. After the device
check exposed collapsed missing-category tracks and a shrunken photo pane, the
grids received explicit full-width columns. Focused browser checks then verified
painted missing stars and full-width placeholders/photos at all three sizes.
The LenTalk completion artwork also retains a request-specific accessible label.

The final APK's native libraries, bridge, guidebook, map UI/model, rail data and
calibrations are byte-for-byte identical to code57. Packaged UI files match the
verified source. APK: `SnapCompanion-0.5.33-dev.1.apk`, versionCode58. SHA-256:
`cd9894c4177b463c43d5eb095338f2450eddc829ce1857c1aa5ab44c2a541c8b`.

## Thor deployment

Installed in place and reopened on the lower display under the user's standing
direct-push authorization. Android confirms 0.5.33-dev.1 / code58. The same Eden
process remained running throughout; no game input, emulator restart, settings
change, data clearing or save operation was sent.

Private device checks confirmed the preserved five-column Photodex grid, native
entry header/types, larger 3:2 registered photograph, all four category previews
or silhouettes/stars, readable score, separate neighbor controls, About course
tickets, four-column LenTalk, native tools/reward assets, compact solution popup,
two-column island course tiles and five-column encounters with four-category
stars. The existing 114 Diamond categories and 29.7 playtime hours remained
visible. Companion is left on Vivillon's photo entry for review. These are
visual/startup checks, not a new gameplay/performance acceptance run.
