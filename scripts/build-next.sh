#!/bin/sh
# Historical entry point; SCN now updates the original app identity.
set -eu
SNAP_BUILD_SCRIPT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec sh "$SNAP_BUILD_SCRIPT_ROOT/build-local.sh" "$@"
