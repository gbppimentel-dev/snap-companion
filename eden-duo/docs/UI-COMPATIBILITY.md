# Preserve the original companion experience

**Latest override — 2026-11-18:** the user chose APK delivery and lifted the separate-package/freeze requirement. The original Android interface is retained, future builds update the original app identity, and the new reader will be bundled with the APK. A Duo mod and native UI port are not mandatory. Earlier integration alternatives below are historical; implement the standalone Thor reader adapter and validate its live fields, preserving the existing UI and data.

The user's clarification on 2026-11-18 is binding: SCN should still look like the original companion, with the new direct game-reading features added. The native map foundation does not authorize a redesign or the loss of existing app features.

## Presentation contract

- Use stable v0.5.24 as the visual and behavior reference, including the Live, Photodex, LenTalk and Guide navigation, photo-selection helper, entry modals, filters, fonts, colours, icons, borders, sprites, progress displays, controller feedback and compact header.
- Preserve the user's existing information hierarchy and familiar flow. Do not expose memory addresses, decoder state, development diagnostics or technical instructions in the normal UI.
- Upgrade the main screen's course information to verified current-run readings. Add expansion into the course map without reorganizing the rest of the app.
- Unknown or stale fields remain neutral. No manual course entry, inferred route timing or default day/level should be presented as detected.
- The current `package/dualscreen/manifest.json` is an engineering placeholder for testing the native host and map pipeline. It is not the approved SCN UI and must not be described as a visual port of the original app.

## Compatibility findings

The original interface uses an Android WebView and local HTML/CSS/JavaScript. Eden Duo's documented companion surface uses native manifest widgets and a native module ABI. The inspected ABI does not provide a ready-made connection that feeds module output into the existing Android app. A mod-only version needs a faithful interface port; an Android-interface version needs a separately implemented and validated local data connection.

The original app's Eden helpers target `dev.eden.eden_emulator`, its process, log/activity names and save paths. The inspected Duo source uses that default application ID too, but has other build variants. Matching IDs alone do not establish installation, signing, save-layout, telemetry, focus, capture or runtime compatibility. No emulator replacement, save migration or device check has been authorized by this design clarification.

The frozen v0.5.24 APK has no implemented receiver for SCN native module readings. Do not claim it can receive the new course/position output unchanged. Its existing functions must be tested separately against the intended Duo build. Any Android changes belong only in the optional Next fork, preserving stable v0.5.24 and its records.

## Next work, in dependency order

1. Confirm whether the familiar interface runs inside Duo as a mod, stays in Android with a reader mod, or both. The user has been asked; no answer has been assumed.
2. Lock the integration contract and original-screen reference. For a paired design, first prove one bounded local state transfer into the optional Next UI without starting screenshot polling or changing stable. For mod-only, first prove a faithful representative screen in Duo's native widget system.
3. Check the exact Duo build/package and save/runtime compatibility when device work is explicitly authorized. Do not replace regular Eden or migrate its saves as an implicit step.
4. Find and validate Snap's current course/variant/selected level, then NEO-ONE pose and map projection. Keep static asset decoding work separate from live-reader proof.
5. Connect verified readings to the preserved interface and expanded map. Verify existing Photodex, LenTalk, Guide, photo selection, images, progress, controller feedback and playtime behavior before calling the new version compatible.

Stable v0.5.24 stays a working, unchanged fallback throughout. No APK or mod has been installed, pushed or published for this clarification.
