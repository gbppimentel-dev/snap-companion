#!/bin/sh
set -eu
SNAP_NEXT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$SNAP_NEXT_ROOT"
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21)
    export JAVA_HOME
fi
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Library/Android/sdk}
SNAP_NEXT_VERSION=$(python3 -c 'import json; print(json.load(open("fork.json"))["version"])')
node --test tests/ui-model.cjs
python3 -m unittest discover -s diagnostics -p 'test_*.py'
python3 tests/publication-audit.py
./gradlew :app:testDebugUnitTest :app:assembleRelease :app:lintRelease --console=plain
mkdir -p dist/next
SNAP_NEXT_APK="$SNAP_NEXT_ROOT/dist/next/SnapCompanionNext-$SNAP_NEXT_VERSION.apk"
cp app/build/outputs/apk/release/app-release.apk "$SNAP_NEXT_APK"
python3 tests/fork-isolation.py --apk "$SNAP_NEXT_APK"
shasum -a 256 "$SNAP_NEXT_APK" > "$SNAP_NEXT_APK.sha256"
echo "Built optional Next APK only: $SNAP_NEXT_APK"
echo "Stable v0.5.24 and Thor were not changed. No installation or publication performed."
