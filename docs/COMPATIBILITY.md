# compatibility

## supported scope

| area | status |
| --- | --- |
| AYN Thor / Android 13 development environment | used during private development |
| original Eden package `dev.eden.eden_emulator` | targeted by fixed-purpose helpers |
| New Pokémon Snap 2.0.1 saved-data schemas | independently checked envelope, dimensions, checksums and mappings |
| Photodex registration / medals / selected images | implemented; image availability is reported separately |
| LenTalk receipt/completion | implemented from distinct saved states; receipt is not completion |
| saved research / named Forest season discoveries | specific verified fields only; not current selected level or active season |
| supported English course/phase/photo-selection labels | implemented with generation/freshness guards; broad real-layout coverage remains unverified |
| alternate emulators / languages / firmware / game updates | unverified |

The app uses the existing AYN firmware service (`PServerBinder`) for its fixed-purpose local link and projection startup. That availability is firmware/configuration dependent. This is not a generic Android or physical-Switch companion. The exact Eden/firmware build matrix has not been qualified for public distribution.

## current limitations

- No validated active course/selected-level memory source. Cold mid-run recovery without supported labels is incomplete; unknown context stays unknown.
- No route-position/branch/camera/target/pose/interaction telemetry, trained visual/audio Pokémon recognition, or current-run photo ranking.
- General story/tool/title/cosmetic/form ownership is not decoded. A specific request-reward condition or Forest discovery bit does not prove wider ownership or the active route.
- No universal or sustained60FPS guarantee. Capture/inference rates and sampled emulator-reported FPS are different measurements. Baseline comparisons and broad thermal/long-session acceptance remain incomplete.
- Missing cached images can be retried; already-black original saved photos cannot be recreated without intact source data.
- Physical controller coverage and broad new-save/replacement/completion flows still need wider real-device acceptance.

## release validation

0.5.24 adds credits to the 0.5.23 core installed during development. The credits APK is locally built/checked; no new physical-device acceptance run is claimed. Local validation includes the supported native/model suites, fixture OCR/recovery work, and browser/Android WebView109 presentation reviews recorded with their individual scope. CI checks source builds and synthetic records, not actual gameplay.

The [release notes](releases/v0.5.24.md) and release manifest distinguish local validation from hardware acceptance.
