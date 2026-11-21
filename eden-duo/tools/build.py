#!/usr/bin/env python3
"""Build SCN locally. Host builds do not produce an installable Android mod."""
import argparse
import os
import platform
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
OOZ_COMMIT='ff5aeb9e45e362e8d6bb1199aa82406285dd2a18'
SIMDE_COMMIT='dd0b662fd8cf4b1617dbbb4d08aa053e512b08e4'

def run(args, **kw): subprocess.run([str(a) for a in args],check=True,**kw)
def revision(path): return subprocess.check_output(['git','-C',str(path),'rev-parse','HEAD'],text=True).strip()
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--android',action='store_true')
    p.add_argument('--diagnostic-reader',action='store_true',help='Compile the unvalidated Snap candidate reader for explicit development diagnostics only')
    p.add_argument('--fetch-deps',action='store_true',help='Fetch the pinned open-source decoder and SIMD headers')
    args=p.parse_args()
    ooz=Path(os.environ.get('SCN_OOZ_ROOT',ROOT/'.deps/ooz'))
    if args.fetch_deps and not ooz.exists():
        ooz.parent.mkdir(parents=True,exist_ok=True)
        run(['git','init',ooz]); run(['git','-C',ooz,'remote','add','origin','https://github.com/zao/ooz.git'])
        run(['git','-C',ooz,'fetch','--depth','1','origin',OOZ_COMMIT]); run(['git','-C',ooz,'checkout','--detach',OOZ_COMMIT])
        run(['git','-C',ooz,'submodule','update','--init','--depth','1','simde'])
    if not ooz.exists(): p.error('Set SCN_OOZ_ROOT or use --fetch-deps. No game files are downloaded.')
    if revision(ooz)!=OOZ_COMMIT or revision(ooz/'simde')!=SIMDE_COMMIT: p.error('Decoder dependency revision mismatch')
    if args.android:
        ndk=os.environ.get('ANDROID_NDK_HOME')
        if not ndk: p.error('ANDROID_NDK_HOME is required for an Android ARM64 build')
        host='darwin-x86_64' if sys.platform=='darwin' else 'linux-x86_64'
        cxx=Path(ndk)/'toolchains/llvm/prebuilt'/host/'bin/aarch64-linux-android24-clang++'
        if not cxx.is_file(): p.error('NDK Android ARM64 compiler not found')
        out=ROOT/('build/android-arm64-v8a-diagnostic' if args.diagnostic_reader else 'build/android-arm64-v8a'); shared=['-shared','-static-libstdc++','-Wl,--no-undefined']
    else:
        cxx=os.environ.get('CXX','clang++'); out=ROOT/'build/host'
        shared=['-dynamiclib'] if sys.platform=='darwin' else ['-shared']
    out.mkdir(parents=True,exist_ok=True)
    flags=['-std=c++17','-O2','-fPIC','-fvisibility=hidden','-DOOZ_BUILD_DLL=1','-DSIMDE_ENABLE_NATIVE_ALIASES',
           '-ffile-prefix-map='+str(ROOT)+'=scn','-ffile-prefix-map='+str(ooz)+'=ooz',
           '-I'+str(ROOT/'vendor/eden'),'-I'+str(ROOT/'vendor/bcdec'),'-I'+str(ROOT/'native'),'-I'+str(ooz/'simde')]
    # The pinned decoder assumes Linux x86gprintrin.h supplies _rotl; Clang's
    # header does not. Its built-in preserves the decoder's 32-bit rotate,
    # including zero/modulo-32 counts. Apple, GCC and Android paths are unchanged.
    if not args.android and sys.platform.startswith('linux') and platform.machine() in ('x86_64','AMD64') and 'clang' in Path(cxx).name:
        flags+=['-D_rotl=__builtin_rotateleft32']
    if args.diagnostic_reader:flags+=['-DSCN_DIAGNOSTIC_READER=1']
    codec=[ooz/n for n in ['kraken.cpp','lzna.cpp','bitknit.cpp']]
    source=[ROOT/'native'/n for n in ['live_reader.cpp','snap_runtime.cpp','archive.cpp','catalog.cpp','map_art.cpp','scn_module.cpp']]
    module=out/('0100F4300BF2C000.so' if args.android else 'scn-module.dylib' if sys.platform=='darwin' else 'scn-module.so')
    export_flags=['-Wl,--version-script='+str(ROOT/'native/exports.map'),'-Wl,-soname,0100F4300BF2C000.so'] if args.android else []
    run([cxx,*flags,*shared,*export_flags,*source,*codec,'-o',module])
    if args.android:
        run([cxx.parent/'llvm-strip','--strip-all',module])
    if not args.android:
        tests=out/'scn-tests'
        run([cxx,*flags,ROOT/'native/live_reader.cpp',ROOT/'tests/live_reader_test.cpp','-o',tests]); run([tests])
        runtime_tests=out/'scn-runtime-tests'
        run([cxx,*flags,ROOT/'native/snap_runtime.cpp',ROOT/'tests/snap_runtime_test.cpp','-o',runtime_tests]);run([runtime_tests])
        abi_tests=out/'scn-module-tests'
        run([cxx,*flags,ROOT/'tests/module_test.cpp',module,'-o',abi_tests]); run([abi_tests])
        tool=out/'scn-art'
        run([cxx,*flags,ROOT/'native/map_art.cpp',ROOT/'native/archive.cpp',ROOT/'native/catalog.cpp',ROOT/'tools/art.cpp',*codec,'-o',tool]); run([tool,'--self-test'])
        probe=out/('scn-curve-probe.dylib' if sys.platform=='darwin' else 'scn-curve-probe.so')
        run([cxx,*flags,*shared,ROOT/'native/snap_runtime.cpp',ROOT/'tools/curve_probe.cpp','-o',probe])
    print('Built',module)
    print('Development only: current-course root, player pose and map calibration remain unverified.')
if __name__=='__main__': main()
