# Photo Selection detection — code61

The user reported that Photo Selection sometimes fails to show the Pokémon
currently being chosen. Thor was unavailable through ADB during diagnosis, so
this change uses source analysis, owned UI labels, synthetic replay and local
checks. It does not claim live acceptance of the user's failing screen.

## Findings

- The parser previously required exactly one species name across the whole
  screen. The folder overview has several labelled Pokémon at once. Its
  highlighted label was not distinguished from unselected labels.
- Scene detection relied on help sentences. The inner picker has independent
  Photodex/Selected/Compare/Photos Taken/All-or-Total labels and can remain on
  screen while its help text changes or is not read.
- Subject changes required three matching frames. Missing or changing subjects
  invalidate the earlier category correctly, but repeated confirmation could
  leave the helper blank during faster browsing.
- A brief unknown scene immediately returned the Live tab to its field layout.
  The waiting state also said Choose a Pokémon even after the player had done
  so in the game.

## Correction

`PhotoSelectionDetector` runs only on non-camera-HUD OCR results with the owned
overview controls (Evaluate Photos and Auto/Remove All). Sixteen bounded samples
around each plausible label check for the gold folder paper on both sides and
above/below the text. Tags retain the original word coordinates and confidence.
Only exact OCR species names on tagged labels can resolve a highlighted overview
when several names are visible. No Pokémon is inferred from a sprite or photo.

There is no second OCR pass, frame queue or capture-rate increase. The active
camera-HUD path is unchanged. The color check is a UI heuristic, not a measured
recognition probability or universally validated theme/layout model.

`PhotoSelectionText` recognizes the inner picker from its independent label
combination. Broad help sentences remain supported. Strong scene exits such as
Professor's Evaluation, pause and registration still take priority. No-subject
prompts suppress subject output even if other Pokémon names remain visible.

In selection, weak incidental species text no longer makes a strong unique label
ambiguous. Two distinct readings at confidence >=0.9 confirm subject/category;
medium-confidence fields retain the existing three-reading requirement. A new,
missing or weak selected subject still clears both subject and category before
confirmation. Original thresholds/confidence are not raised by pixel evidence.
Repeated timestamps do not count as new observations.

Only the desk presentation can survive up to 1.5 seconds of unknown frames after
a confirmed selection scene. Its subject is zero and its photo-phase payload
remains empty until a current selection reading agrees with stabilized fields.
Known scene exits, visibility loss, source/profile changes and expiry end this
continuity. The neutral UI says Reading Selection; it does not reuse the old
Pokémon, category or registered references.

## Evidence and limits

Owned `PhotoSubmit_UI` strings and `photo_submit` layout bindings distinguish the
folder selection and inner photo picker. A public native-game overview screenshot
was reviewed privately for its highlight treatment; its margins agreed with the
color rule and an unselected label did not. That picture is not bundled, does not
test English OCR and is not the user's current screen.

Synthetic bitmap/parser/stabilizer/output tests exercise multiple visible names,
focus moving between cards, weak/unreadable highlighted text, ambiguous/two/no
highlights, missing controls, footer/partial-edge rejection, multiword names,
inner-picker anchors, no-subject/evaluation priority, weak incidental names,
distinct-frame confirmation, category invalidation and medium-confidence waits.
State tests cover bounded neutral continuity and immediate known exits. Browser
checks cover subject changes, cleared unknown references, notice updates, category
changes and evaluation exit at Thor, short landscape and portrait dimensions.

Live English label recognition, actual switching latency and the user's exact
failing scene still need read-only checks on a connected Thor. No game inputs,
photo choices/submissions, saved data or emulator settings were changed. The
native sampler, bridge, map/calibration/rail data, saved-photo queue and guide
content remain unchanged.

## Local build and delivery

- Built `0.5.34-dev.1` / code61 through `scripts/build-local.sh`: model/map,
  diagnostic, native-reader, Android unit, release lint and upgrade-identity checks
  passed. The final Android test rerun passed all 197 cases.
- The focused Photo Selection browser regression passed at 540×470, 540×405
  and 412×640, including immediate subject changes and an empty waiting state.
- The original package and baseline signing certificate are preserved.
- APK: `dist/development/SnapCompanion-0.5.34-dev.1.apk`
- SHA-256: `c536dd96005c710e3489b4a7b8d19ac9a2cd409a9f75d64ed62b63813faa6e6c`
- Thor was absent from the final ADB inventory. This build is local only;
  installation and validation against the user's actual selection screen remain
  pending. These local checks do not establish live OCR accuracy or latency.
