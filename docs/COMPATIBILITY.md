# Compatibility — v0.5.36-duo.1

The APK updates the original `dev.gbppimentel.snapcompanion` package in place with the baseline signing certificate. No separate Next app or Eden Duo mod is required. Historical v0.5.24 and v0.5.35 artifacts remain unchanged. This build targets Duo; it is not a stock-Eden/Duo auto-detecting build.

| Area | Checked scope |
| --- | --- |
| Device | AYN Thor, Android 13; compatible AYN PServerBinder service required |
| Emulator | Eden Duo `dev.igawa6.edenduo`, observed version 1.4.0 |
| Game | Exact owned New Pokémon Snap 2.0.1 fingerprint/layout |
| Direct readings | Current course/variant, selected level, bounded rail geometry/distance, travel direction and validated render-camera direction/lens |
| Earlier stock-host evidence | Park Day/Night Lv.2, Park Flower Field continuation, Sands Night progress, Undersea camera turn/zoom, Forest Lv.2 scene transitions, Research Camp scope/camera; see the dated hardware record for scope |
| Photos left | Supported typed HUD ownership checked in Camp through shot/retry and in Park Day; not qualified for every course/retry. Screen-reading fallback remains |
| Maps | 30 course views, 143 terrain/route assets, 1,403 authored sightings, approximate player/trail/cone |
| Calibration | Portions of 30 illustrations / 121 of 123 known rails; two short rails, rejected intervals and some seasonal/branch variants remain unplaced or unverified |
| Saved records | Supported automatic Photodex metadata/images, LenTalk received/completed statuses and research providers |
| Screen observations | Supported English phase/FPS and other screen-only fields; not every field has a native provider |
| Header | Available FPS, Cocoon-recorded Thor hours, clock/sync and current battery icon/percentage; no battery-health or capacity estimate |
| Duo checks | Exact installed code64 APK verified; paired cold launches, save synchronization and Park Day Lv.3 native map/marker/trail were checked on Thor on 2026-11-24 |
| Optional LSFG | Duo 2× generation and SMAA checked in short samples; user-supplied DLL/cache are not distributed. Display presentation cadence is separate from reported game FPS |
| Validation limits | Full split-path/event, all-course, long-session and thermal acceptance remain incomplete |
| Other environments | Other devices, firmware, emulator packages, builds and languages are unverified |

## Limits

- Map coordinates are schematic, not surveyed geographic coordinates. Invalid readings cannot drive a live marker or cone. A brief gap may show a stationary, labelled last position for at most 2.5 seconds; it is never advanced using time.
- Authored Pokémon sightings span reference research levels. They are not live actor positions, current request poses or minimum-level guarantees.
- Camera/lens readings are separate from vehicle travel. Their schematic projection is approximate and withheld where invalid or during event/last-position presentation.
- Forest/Camp course totals remain neutral where continuation data is incomplete; supported section readings remain available.
- There is no live target/pose recognition or ungraded-photo ranking. The automatic Photo Selection comparison page and grading helper are removed; the normal dashboard stays visible.
- Saved photo metadata may precede an image. Copies refresh in supported quiet scenes. Pixels already black in a game-save image cannot be reconstructed.
- All-course/branch/Illumina/season, new-save, restart, long-session, gyro and thermal/performance acceptance are incomplete. No sustained 60 FPS guarantee is made.

See [setup](SETUP.md), [map details](ROUTE_MAP.md), [hardware evidence](THOR_READER_VALIDATION.md) and [release notes](releases/v0.5.36-duo.1.md).

See [Duo migration/setup](EDEN_DUO_SETUP.md). The APK does not install an emulator, migrate saves automatically, enable LSFG automatically or include its DLL. Keep backups and stock saves as rollback sources.
