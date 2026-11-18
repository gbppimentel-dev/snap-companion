#!/bin/sh
set -eu
SNAP_MEMORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SNAP_MEMORY_NDK=${ANDROID_NDK_HOME:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/ndk/28.2.13676358}
SNAP_MEMORY_HOST=linux-x86_64
[ "$(uname -s)" != Darwin ] || SNAP_MEMORY_HOST=darwin-x86_64
SNAP_MEMORY_CXX=$SNAP_MEMORY_NDK/toolchains/llvm/prebuilt/$SNAP_MEMORY_HOST/bin/aarch64-linux-android30-clang++
SNAP_MEMORY_OUT=$SNAP_MEMORY_ROOT/app/build/generated/memoryReader/jniLibs/arm64-v8a
mkdir -p "$SNAP_MEMORY_OUT"
"$SNAP_MEMORY_CXX" -std=c++17 -O2 -fPIC -shared -static-libstdc++ -fvisibility=hidden -Wl,--no-undefined -Wl,-z,max-page-size=16384 -Wl,-soname,libsnapmemory.so \
  -ffile-prefix-map="$SNAP_MEMORY_ROOT"=snap \
  -I"$SNAP_MEMORY_ROOT/eden-duo/native" -I"$SNAP_MEMORY_ROOT/eden-duo/vendor/eden" \
  "$SNAP_MEMORY_ROOT/app/src/main/cpp/snap_memory.cpp" "$SNAP_MEMORY_ROOT/eden-duo/native/snap_runtime.cpp" -o "$SNAP_MEMORY_OUT/libsnapmemory.so"
"$SNAP_MEMORY_NDK/toolchains/llvm/prebuilt/$SNAP_MEMORY_HOST/bin/llvm-strip" --strip-all "$SNAP_MEMORY_OUT/libsnapmemory.so"
