# building & signing

## requirements

- JDK21; `JAVA_HOME` points to it.
- Android NDK 28.2.13676358/r28c; optionally set `ANDROID_NDK_HOME`.
- A host C++17 compiler for native scope/counter regression checks.
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

CI runs publication checks, JavaScript model/map checks, offline diagnostics, JVM/Robolectric checks, Android release assembly/lint and native reader/research checks. Test save vectors are synthetic or small independently published ISC vectors; they are not user saves. Original-font OCR/recovery instrumentation sources are provided for an isolated local Android emulator. No CI job connects to a user's Thor.

## release packaging

Build and verify the original-package update locally:

```sh
sh scripts/build-local.sh
```

It checks model/map, diagnostic, native and Android behavior, release lint and the
baseline signing certificate. Output: `dist/development/SnapCompanion-0.5.36-duo.1.apk`.
The original private signing key must be configured locally for that identity
check to pass. It is not included in this repository or CI.

After committing the intended source, package an already verified release APK:

```sh
python3 packaging/package_release.py --apk dist/development/SnapCompanion-0.5.36-duo.1.apk
```

The packager reads current `fork.json`, compatibility and versioned validation
metadata; checks the baseline signer; and writes the APK, quick-start ZIP,
compatibility JSON, release manifest and SHA256 sums under ignored
`dist/releases/v0.5.36-duo.1/`. It refuses to replace an existing output directory or
package v0.5.24. It does not install, create tags or publish. GitHub's automatically
provided source archives correspond to the release tag; the bundle links to that
source and includes the third-party notices/licenses.

The historical v0.5.24 APK, release assets, tag and validation remain immutable.
Release metadata must distinguish local checks, device observations and untested
course/branch/thermal flows. CI's disposable signing key cannot replace a local
owner-signed APK and CI does not publish APKs automatically.

## Regenerating schematic route alignments

The APK build uses committed calibration assets and does not require scientific Python packages. To regenerate them, use a local Python environment with NumPy, SciPy and Pillow, then run `python tools/test_route_alignment.py` and `python tools/prepare_route_calibrations.py`. Inputs are the committed factual rail catalog, bundled route artwork, affine guides and reviewed trace/landmark JSON. No game archives or live packets are published by this workflow. Keep the two withheld rails and uncertain seasonal scenes neutral until their placement is established; reducing rejection checks to inflate coverage is not validation.

## Map interaction regression

With Playwright available locally, run `node --test tests/map-browser.cjs`. Set `NODE_PATH` if Playwright is installed outside this checkout, and optionally `SNAP_BROWSER_EXECUTABLE` for a local Chromium executable. This isolated browser test checks zoom retention through missing/recovered game readings, actual course changes, full-resolution portrait sources and fixed-size unscaled map overlays. It does not contact Thor or count as gameplay acceptance.

The local build also runs the read-only native scope/counter regression executable with a host C++17 compiler. Run it alone with `sh scripts/test-memory-reader.sh`; it uses synthetic memory and does not access a device or game files.
