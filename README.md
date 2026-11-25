# Snap Companion

[![release](https://img.shields.io/github/v/release/gbppimentel-dev/snap-companion)](https://github.com/gbppimentel-dev/snap-companion/releases/latest)
[![checks](https://github.com/gbppimentel-dev/snap-companion/actions/workflows/ci.yml/badge.svg)](https://github.com/gbppimentel-dev/snap-companion/actions/workflows/ci.yml)
[![license: MIT](https://img.shields.io/badge/companion-MIT-blue.svg)](LICENSE)

An offline second-screen companion for **New Pokémon Snap on AYN Thor**, running alongside Eden Duo. Keep your Photodex, LenTalk requests, photo tips and illustrated course map on the bottom screen while playing above.

**[Download v0.5.36-duo.1](https://github.com/gbppimentel-dev/snap-companion/releases/tag/v0.5.36-duo.1)** · [Duo setup](docs/EDEN_DUO_SETUP.md) · [Release notes](docs/releases/v0.5.36-duo.1.md) · [Compatibility](docs/COMPATIBILITY.md)

**Current host: Eden Duo 1.4.0 (`dev.igawa6.edenduo`).** This is the exact code64 build verified on Thor. The older [v0.5.35](https://github.com/gbppimentel-dev/snap-companion/releases/tag/v0.5.35) remains available for users still on stock Eden. The new APK does not automatically migrate emulator saves, install Duo or supply an LSFG DLL.

The APK updates the original Snap Companion app in place. It includes its own supported read-only game-memory reader; **no separate Next app or Eden Duo mod is required**. Keep the existing app installed to preserve its data.

## What it does

| Area | Available now |
| --- | --- |
| **Live** | Current course, day/night variant and selected research level from supported game memory; photos left when available; photo suggestions, recent snapshots and course requests. |
| **Header & controls** | Available Eden-reported FPS, Cocoon-recorded Thor playtime, clock, last sync, a quiet battery icon/percentage and passive controller/stick feedback. |
| **Expandable course map** | All 30 course illustrations, 143 terrain/route assets and 1,403 reference sightings. Pan, pinch/zoom, Overview and default Follow; Pokémon, Missing Diamonds, LenTalk and Hidden modes. |
| **NEO-ONE tracking** | Matched rail position, travelled trail, named section progress, branch/rejoin-aware current-path progress and a guarded render-camera view cone where aligned. Brief handoffs show a labelled, stationary last position. |
| **Photodex** | 234 Pokémon, four official saved categories each and a 936-Diamond goal. Automatic supported records/images, compact artwork grids, course/level filters, missing-Diamond toggle and four-photo albums. |
| **Photo tips** | 58 reviewed course-specific methods plus 442 existing scoped instructions, covering 499 star categories; other categories retain general guidance. 258 hint groups show 612 distinct options with clear OR separators. |
| **LenTalk** | Illustrated request cards, supported received/completed statuses, filters, full offline solutions, reward references and optional reviewed route/setup/timing notes for 13 requests. |
| **Guide** | Courses grouped by island and day/night, encounters, collection coverage, photo-category status, forms/gender reference artwork and links into the in-app Photodex. |

Photo Selection now keeps the normal Live dashboard. The automatic Photo Selection comparison page and the grading helper are absent. Browsing reference filters never changes the game.

## What changed since v0.5.35

- Retargeted supported save/image, request, research, game visibility, scheduling/focus and native-reader paths to **Eden Duo 1.4.0**.
- Retained the original companion APK identity, signer, cached data, interface, maps and offline references. It remains a separate companion APK, not a Duo mod.
- Fixed a stale visible-window flag after a game process exits and made native heartbeat publication a single atomic rename.
- Updated the setup notes for Duo's per-title **No Companion = Off** option and a Duo-generated Cocoon shortcut, so the emulator's idle companion page does not cover this APK.
- Documented the checked optional **2× LSFG / SMAA** setup. Frame generation belongs to Duo, not the companion; it does not imply that reported game FPS doubles.

Prior v0.5.35 introduced the bundled reader, illustrated maps/route-camera tracking, asset-based readers, separated photo hints and icon/percentage-only battery display. See [release history](CHANGELOG.md). Earlier releases and assets remain unchanged.

## Compatibility and honest limits

The checked target is **AYN Thor, Android 13, Eden Duo 1.4.0 package `dev.igawa6.edenduo`, and New Pokémon Snap 2.0.1**. The supported AYN firmware service is required. Other devices, firmware, emulator packages and game builds are unverified. English OCR remains in use for supported screen-only information; native course/level readings do not depend on reading those labels.

- Map alignment is schematic and covers **portions of 30 illustrations / 121 of 123 known rails**. Two short rails, rejected intervals and some seasonal/branch variants remain unplaced or unverified. This is not complete all-course gameplay acceptance.
- Pokémon pins are authored reference sightings across research levels, **not live Pokémon positions** or guarantees that a particular pose is currently available.
- Forest/Camp whole-course totals remain neutral where continuation data is incomplete. Fresh section readings may still work. Unknown/stale positions are not replaced by timer-based movement.
- Live target/pose recognition and ranking of ungraded photos are not implemented. Photo examples describe reference opportunities, not the location of your saved photograph.
- Saved metadata can arrive before its image. Copies refresh during supported quiet scenes. An image already black in the game save cannot be reconstructed.
- Event/fork recovery passes local route/model/browser checks; a full in-game split-path run for this latest fix was not completed. Restart, long-session, thermal and full hardware coverage remain incomplete. There is no sustained 60 FPS guarantee.

[Compatibility details](docs/COMPATIBILITY.md) · [Map model](docs/ROUTE_MAP.md) · [Hardware evidence](docs/THOR_READER_VALIDATION.md) · [Current limitations/checklist](docs/AFK_MAP_WORK.md)

## Install or update

1. Run your own compatible Snap 2.0.1 copy in Eden Duo 1.4.0. Back up and migrate/verify existing progress before switching from stock Eden.
2. Download the signed APK from [Releases](https://github.com/gbppimentel-dev/snap-companion/releases/latest) and install it **over the existing app**. Do not uninstall or clear data.
3. Use a Duo-generated Snap shortcut in Cocoon, pair **Use Companion → Snap Companion → bottom screen**, and set Snap's Duo **No Companion** option to **Off**.
4. Open normally. Supported live connections start automatically; respond to Android permission prompts if presented.

The package remains `dev.gbppimentel.snapcompanion` with the original signing certificate and private-data paths. [Setup and troubleshooting](docs/SETUP.md).

## Screenshots

These are current UI previews with **invented progress and simulated readings**. They contain no personal saves/photos and do not measure device performance.

<table>
  <tr>
    <td width="50%"><img src="docs/images/overview.png" width="460" alt="Live dashboard with illustrative course, battery and collection data"><br>Live</td>
    <td width="50%"><img src="docs/images/course-map.png" width="460" alt="Expanded illustrated course map with a simulated NEO-ONE reading"><br>Course map</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/photodex.png" width="460" alt="Compact Photodex and example star medals"><br>Photodex</td>
    <td width="50%"><img src="docs/images/lentalk.png" width="460" alt="LenTalk postcards and illustrative request statuses"><br>LenTalk</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/photo-tips.png" width="460" alt="Photo hints separated into options with OR dividers"><br>Photo tips</td>
    <td width="50%"><img src="docs/images/guide.png" width="460" alt="Course guide with collection coverage and encounters"><br>Guide</td>
  </tr>
</table>

## Build and contribute

Java, bundled HTML/CSS/JavaScript and a Thor-specific ARM64 reader. Prepared assets/catalogs are included; no original game archive, save or console keyset is needed to compile.

```sh
sh scripts/build-local.sh
```

Use JDK 21, Node 22+, Python 3.10+, Android platform/build-tools 36 and NDK 28.2.13676358/r28c. Set `JAVA_HOME`, `ANDROID_HOME` and optionally `ANDROID_NDK_HOME`. The script runs model, native, diagnostic and Android checks, lint and original identity/signing checks, then produces an ignored `dist/development/` APK. It does not install or publish. A build signed with another key cannot update the distributed APK.

[Building/signing/packaging](docs/BUILDING.md) · [Contributing](CONTRIBUTING.md) · [Architecture](docs/ARCHITECTURE.md) · [Privacy](docs/PRIVACY.md)

## Credits and license

Created by [gbppimentel-dev](https://github.com/gbppimentel-dev).

Original companion code/documentation use the [MIT license](LICENSE). Reused native reader/research components identify their GPL-3.0-or-later license; see [third-party notices](THIRD_PARTY_NOTICES.md). Pokémon artwork, branding, game-derived references and fonts remain third-party content and are not relicensed under MIT.

Unofficial fan companion, independent of Nintendo, The Pokémon Company, Creatures, GAME FREAK, Bandai Namco Studios, AYN, Eden and Cocoon. No companion account, Internet permission, analytics, online updater, original game archives, user saves or signing keys are included. Original game saves are not written.
