#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SNAP_SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
SNAP_JDK=${SNAP_JAVA_HOME:-${JAVA_HOME:-$PROJECT_DIR/.toolchains/temurin-21/Contents/Home}}
SNAP_OUT=$PROJECT_DIR/app/build/generated/liveBridge
rm -rf "$SNAP_OUT/classes" "$SNAP_OUT/dex"
mkdir -p "$SNAP_OUT/classes" "$SNAP_OUT/assets/bridge" "$SNAP_OUT/dex"
"$SNAP_JDK/bin/javac" --release 8 -classpath "$SNAP_SDK/platforms/android-36/android.jar" -d "$SNAP_OUT/classes" "$PROJECT_DIR"/bridge/src/dev/gbppimentel/snapbridge/*.java "$PROJECT_DIR/app/src/main/java/dev/gbppimentel/snapcompanion/ControllerButtons.java" "$PROJECT_DIR/app/src/main/java/dev/gbppimentel/snapcompanion/GameToolRules.java"
"$SNAP_JDK/bin/jar" cf "$SNAP_OUT/input.jar" -C "$SNAP_OUT/classes" .
JAVA_HOME="$SNAP_JDK" "$SNAP_SDK/build-tools/36.0.0/d8" --min-api 30 --lib "$SNAP_SDK/platforms/android-36/android.jar" --output "$SNAP_OUT/dex" "$SNAP_OUT/input.jar"
"$SNAP_JDK/bin/jar" cf "$SNAP_OUT/assets/bridge/live-projection.jar" -C "$SNAP_OUT/dex" classes.dex
