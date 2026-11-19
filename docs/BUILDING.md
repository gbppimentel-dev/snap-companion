# building & signing

## requirements

- JDK21; `JAVA_HOME` points to it.
- Android SDK platform36 and build-tools36.0.0; set `ANDROID_HOME` (or an ignored `local.properties` containing `sdk.dir=...`).
- Node22+ and Python3.10+ for the supplied checks.
- Network access for Gradle's declared dependencies on the first build. The built app has no Internet permission.

Prepared application assets are included. No original game executable/archive, console keyset, personal save, private extraction toolchain or device connection is required to compile.

```sh
node --test tests/ui-model.cjs
python3 -m unittest discover -s diagnostics -p 'test_*.py'
python3 tests/publication-audit.py
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Release shrinking is available through `./gradlew :app:assembleRelease`. Builds use a local development key by default. Signing keys are not in the repository; a development APK cannot update an installed release signed with another key.

The native bridge is built from `bridge/src` and `ControllerButtons.java` by `scripts/build-live-bridge.sh`; D8 packages its small dex JAR as a generated asset. The SDK/JDK paths come from environment variables.

## checks

CI runs publication checks, JavaScript model checks, offline diagnostic-boundary checks, the supported JVM/Robolectric suite and an Android debug build/lint. Test save vectors are synthetic or small independently published ISC vectors; they are not user saves. Original-font OCR/recovery instrumentation sources are provided for an isolated local Android emulator. No CI job connects to a user's Thor.

## release packaging

Only package an already signed APK matching `packaging/validation-v0.5.24.json`:

```sh
python3 packaging/package_release.py --apk /path/to/SnapCompanion-v0.5.24.apk
```

The command creates an APK, a ZIP with quick-start/notices/compatibility/validation, a compatibility JSON, release manifest and SHA256 checksums in ignored `dist/`. It does not install or publish. Release metadata states the actual test scope and does not convert local checks into hardware acceptance.

## Regenerating schematic route alignments

The APK build uses committed calibration assets and does not require scientific Python packages. To regenerate them, use a local Python environment with NumPy, SciPy and Pillow, then run `python tools/test_route_alignment.py` and `python tools/prepare_route_calibrations.py`. Inputs are the committed factual rail catalog, bundled route artwork, affine guides and reviewed trace/landmark JSON. No game archives or live packets are published by this workflow. Keep the two withheld rails and uncertain seasonal scenes neutral until their placement is established; reducing rejection checks to inflate coverage is not validation.
