# Compatibility

## Current local development build

0.5.26-dev.2/code40 updates the original Snap Companion APK in place. Historical v0.5.24 release files remain unchanged. No new GitHub release has been published.

| Area | Checked scope |
| --- | --- |
| Device | Connected AYN Thor, Android13; firmware's PServerBinder is required |
| Emulator | Original `dev.eden.eden_emulator`, observed Eden0.2.1 |
| Game | New Pokémon Snap2.0.1 exact fingerprint/layout |
| Direct readings | Active course, selected level, measured rail distance/world position and vehicle travel direction |
| Course hardware checks | Park Day/Night level2; pause, movement, transitions and mid-course Companion reopening |
| Illustrated maps | All30 course views,143 assets and1403 authored records; local browser/asset checks |
| Touch tools | Fluffruit, Illumina Orb, Melody and Scan visibly verified in Park Night; native/window/controller guards |
| Saved records | Existing Photodex, images, received/completed LenTalk and saved research providers retained |
| Screen reading | Existing supported English phase/photo-selection/FPS recognition remains; not every feature has a memory provider |
| Other devices/emulators/game updates | Unverified; matching package names alone do not establish compatibility |

## Limits

- The live player is on the measured Route Tracker. Its alignment with the separate illustrated map remains unverified. Baked illustrated vehicle symbols mean Start.
- Marker locations describe authored references across research levels, not live Pokémon actor positions, exact current request poses or minimum-level guarantees.
- Heading is vehicle travel direction; camera-view orientation/FOV has not been verified.
- The native remaining-photo HUD experiment is withheld until active ownership across retries is proven. Existing recognized photo information remains unchanged.
- All-course/branch/Illumina/Forest runtime acceptance, process restart, thermal comparisons and long sessions remain incomplete. No sustained60FPS guarantee is made.
- Missing images can be retried; already-black original saved photos cannot be recreated without intact source data.
- General story/tool/title/cosmetic/form ownership, current-run photo ranking and automatic visual/audio Pokémon recognition remain outside this verified memory feed.

The [implementation checklist](AFK_MAP_WORK.md) and [hardware validation](THOR_READER_VALIDATION.md) separate implemented behavior from pending acceptance. The [v0.5.24 release notes](releases/v0.5.24.md) describe the historical public baseline.
