# Compact Photodex — code56

The user requested a more compact Photodex, better filters and expanded entries
using the game’s assets, with a direct Thor push after completion.

## Interface

- Five columns at the 540 px Thor layout (previously three), with compact native
  photo mounts on the original red Photodex fabric. Larger layouts adapt to six
  columns and the narrower layout to four.
- Original photo-number tabs, native corner completion ribbons, clear four-tier
  star clusters and transparent Pokémon cutouts. Cutouts are separate from the
  name area; no card text labels a completed entry unnecessarily.
- Compact collection counts and Diamond-first progress retain all five medal/
  missing totals and the 936-Diamond goal.
- A fixed, single-line search/filter bar and a one-tap Missing toggle. Complete
  four-Diamond Pokémon are hidden by that toggle; location/search are retained.
- The shared Photodex/LenTalk filter sheet uses native island badges in two
  columns, compact course/time choices, visible level buttons, a stable result
  action, Reset and cancelable drafts. All six islands fit at the Thor and short
  landscape test sizes. Location selections still carry between the two tabs.
- A Current Course shortcut uses the existing connected, visible game context
  and selected research level. It appears when a course is acquired and is
  removed when that context is unavailable; it never chooses a substitute.
- The expanded entry has a shorter illustrated header, native close artwork,
  original ribbon navigation, compact typing and next/previous name/portraits.
  The existing four category buttons and saved-photo data remain unchanged.
- Registered photographs retain a 3:2 area, now fitted inside the available
  padded space. Four official My Photos prints remain fixed and expandable.
- Native book trim, landscape course thumbnails, compact score/opportunity
  notes and an inset Photo Tips action organize the photo page. Full Photo Tips
  remain in their existing internal reader.
- About groups repeated course headings while retaining each former course/form
  note, its description and research level. The brief entry, measures, internal
  Course Guide and LenTalk routes remain. No external hyperlinks are added.
- Appearances keep loose transparent cutouts with readable pattern/gender
  captions and balanced single/pair layouts, with denser multiple-form rows.

No new artwork generator, raster rescaling or image download is used. The assets
are existing prepared owned-game resources with their existing rights notices.
Shared toolbar/filter improvements also apply to LenTalk and Guide where used;
their content layouts are otherwise outside this Photodex pass.

## Preserved behavior

No native reader, bridge, course/tracker model, helper cadence, save/photo sync,
input observer or emulator setting is changed. Code55’s course-reader recovery
is retained. The original identity, signer, records and historical v0.5.24
artifacts remain protected. The current-course shortcut is a local filter action,
not a gameplay control or a new source of live game data.

## Verification

Browser acceptance covers 540×470, 540×405, 620×540 and 412×640 where relevant:
compact grid density, star/name clearance, pinned search, search/clear, missing
completion filtering, the six-island sheet, current-course selection, draft
cancel/reset, shared LenTalk locations, live-context loss/recovery, fixed 3:2
photo/album layouts for one/three/four photos, Photo Tips, grouped notes and
appearance/next-entry navigation. Existing Live, Photo Selection, controller
artwork/expiry and map regressions remain included.

These checks use invented offline progress and prepared course thumbnails as
photo stand-ins, never personal save photographs. They do not claim moving-game
FPS, new course alignment coverage or long-session stability.

Final local validation passed: 181 Android/JVM tests, 73 Node/model checks,
6 diagnostics, native regressions, 8 real-browser tests, release lint, publication
audit and original-package/baseline-signer upgrade checks. Native helper, bundled
bridge, guidebook, map/tracker model, map UI, rail data and calibrations inside
the APK were byte-for-byte identical to code55. Packaged UI source matched the
verified local files. Local final previews were visually inspected.

APK: `SnapCompanion-0.5.32-dev.1.apk`, code56. SHA-256:
`849f62734a57fcfe6b5b878fadcbc6522704fe678d911b7d0d3ced393fbd0eda`.

## Direct Thor deployment

Installed code56 in place after the user’s explicit direct-push request. Android
reports 0.5.32-dev.1 / versionCode56. The same Eden process survived the update;
Snap was visibly at Professor Mirror’s existing photo-results/selection dialogue.
No game input, emulator restart or setting change was sent.

The dashboard retained 114 Diamond categories and 29.7 hours and reacquired Park
Day Lv.3 context. On-device checks showed five columns and ten complete cards,
the compact five-part collection totals, native island badges with all six
choices visible, the current-course/level shortcut, and an expanded Vivillon
entry with its existing registered photograph in the landscape area. Native
close and controller artwork were visible. Only Companion navigation was used;
no draft filter or saved record was changed. Display focus stayed at its original
lower-display value. The Photodex grid is left open below the running game.

Device captures remain private and are not repository previews. This deployment
is a visual/startup check, not extended gameplay or performance acceptance.
