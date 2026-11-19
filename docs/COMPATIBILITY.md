# Compatibility

## Current development APK

**0.5.30-dev.1/code47** updates the original Snap Companion package in place. The historical v0.5.24 public release and artifacts remain unchanged. No new GitHub release has been published.

| Area | Checked scope |
| --- | --- |
| Device | AYN Thor, Android13; the firmware's PServerBinder service is required |
| Emulator | Original `dev.eden.eden_emulator`, observed Eden0.2.1 |
| Game | Exact owned New Pokémon Snap2.0.1 fingerprint/layout |
| Direct readings | Active course/variant, selected level, measured rail geometry/distance/world position, travel direction and validated render-camera direction/lens |
| Hardware course examples | Park Day/Night Lv.2, Park's Flower Field continuation, Sands Night section/course progress, Undersea camera turn/zoom and Forest Lv.2 scene transitions; see the hardware record for exact scope |
| Illustrated maps | All30 course views,143 terrain/route assets and1,403 authored reference sightings; integrated approximate player/trail and camera cone |
| Calibration | Portions of30 illustrations/121 of123 catalog rails; two short rails, rejected intervals and other Forest variants remain unplaced/unverified |
| Map interaction | Pan/pinch/wheel/button zoom, Overview, default Follow, exclusive Pokémon filters and location/request cards; original map tool buttons removed at user request |
| Saved records | Existing supported Photodex, images, received/completed LenTalk and research providers retained |
| Screen observations | Supported English phase/photo-selection/FPS observations remain; not every field has a native provider |
| Identity/data | Original package and baseline signer verified; in-place upgrades retained saved114 Diamond categories and available pictures on the checked device |
| Other environments | Other devices, firmware, emulator packages, game versions and languages remain unverified |

## Limits

- Illustration coordinates are schematic calibrations, not surveyed native geographic coordinates. Invalid/stale/wrong-course/transition readings cannot drive a live marker or cone.
- Pokémon pins describe authored references across research levels. They are not live actors, current request poses or minimum-level guarantees. Saved-category filters use the four current official categories.
- Camera direction and lens are direct validated readings, separate from the vehicle's travel arrow. The projected cone is approximate on the schematic art and hides where projection is invalid.
- Forest/Camp whole-course totals remain neutral until scene continuation/branch histories are established. Valid current-section readings remain available.
- Native remaining-photo ownership across retries is not proven; the shipped app retains its existing screen-reading provider for that information.
- Live Pokémon positions, current-target/pose recognition and ungraded-photo ranking are not implemented. The existing Photo Selection helper compares recognized subject/category to saved benchmarks.
- All-course/branch/Illumina/Forest acceptance, process restart and long thermal/performance comparisons remain incomplete. Local browser/model checks do not prove every gameplay path. No sustained60FPS guarantee is made.
- Already-black saved originals cannot be reconstructed without intact source image data.

The [current checklist](AFK_MAP_WORK.md), [map scope](ROUTE_MAP.md) and [hardware record](THOR_READER_VALIDATION.md) separate implemented behavior from pending evidence. Historical sections in the hardware record describe the version explicitly named there; later dated sections supersede earlier limitations. The [v0.5.24 notes](releases/v0.5.24.md) document the preserved public baseline.
