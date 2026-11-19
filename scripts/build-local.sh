#!/bin/sh
set -eu
SNAP_BUILD_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$SNAP_BUILD_ROOT"
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21)
    export JAVA_HOME
fi
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Library/Android/sdk}
SNAP_BUILD_VERSION=$(python3 -c 'import json; print(json.load(open("fork.json"))["version"])')
node --test tests/ui-model.cjs tests/maps.cjs
python3 -m unittest discover -s diagnostics -p 'test_*.py'
sh scripts/test-memory-reader.sh
python3 tests/publication-audit.py
./gradlew :app:testDebugUnitTest :app:assembleRelease :app:lintRelease --console=plain
mkdir -p dist/development
SNAP_BUILD_APK="$SNAP_BUILD_ROOT/dist/development/SnapCompanion-$SNAP_BUILD_VERSION.apk"
cp app/build/outputs/apk/release/app-release.apk "$SNAP_BUILD_APK"
python3 tests/upgrade-identity.py --apk "$SNAP_BUILD_APK" --require-baseline-signer
shasum -a 256 "$SNAP_BUILD_APK" > "$SNAP_BUILD_APK.sha256"
echo "Built local development update: $SNAP_BUILD_APK"
echo "Original app identity and signing certificate verified. Historical v0.5.24 retained. No installation or publication performed."
