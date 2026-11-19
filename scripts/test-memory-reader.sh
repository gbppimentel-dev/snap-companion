#!/bin/sh
set -eu
SNAP_RUNTIME_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SNAP_RUNTIME_TEMP=$(mktemp -d)
trap 'rm -rf "$SNAP_RUNTIME_TEMP"' EXIT HUP INT TERM
"${CXX:-c++}" -std=c++17 -O2 -I"$SNAP_RUNTIME_ROOT/eden-duo/native" -I"$SNAP_RUNTIME_ROOT/eden-duo/vendor/eden" \
  "$SNAP_RUNTIME_ROOT/eden-duo/native/snap_runtime.cpp" "$SNAP_RUNTIME_ROOT/eden-duo/tests/snap_runtime_test.cpp" \
  -o "$SNAP_RUNTIME_TEMP/scn-runtime-tests"
"$SNAP_RUNTIME_TEMP/scn-runtime-tests"
