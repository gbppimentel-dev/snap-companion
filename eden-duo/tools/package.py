#!/usr/bin/env python3
"""Package a validated SCN mod. Never installs it or touches stable Android artifacts."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile

ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--module',type=Path,default=ROOT/'build/android-arm64-v8a/0100F4300BF2C000.so')
    args=p.parse_args()
    evidence=json.loads((ROOT/'research/builds.json').read_text())
    if not evidence['release_ready'] or evidence['status']!='verified-on-device':
        p.error('Live Snap mappings and map calibration are unverified. No installable package was created.')
    if any(v is None for v in evidence['live_fields'].values()):p.error('Required live-field evidence is missing')
    validation=ROOT/'research/device-validation.json'
    if not validation.is_file():p.error('Missing device-validation receipt')
    receipt=json.loads(validation.read_text())
    if receipt.get('build_id')!=evidence['build_id'] or receipt.get('result')!='passed':p.error('Wrong or incomplete device-validation receipt')
    required=['course_variants','selected_level','routes','second_boot_aslr','map_projection','transitions','performance','nce','touch_map','stale_clear']
    if any(receipt.get('checks',{}).get(k)!='passed' for k in required):p.error('Device validation is incomplete')
    if not args.module.is_file():p.error('Build the Android ARM64 module first')
    binary=args.module.read_bytes()
    if binary[:5]!=b'\x7fELF\x02' or binary[5]!=1 or struct.unpack_from('<H',binary,18)[0]!=183:p.error('Expected a 64-bit little-endian ARM64 ELF, not a host build')
    if any(s in binary for s in [b'/Users/',b'/home/',b'.debug_info',b'.debug_line',b'.symtab']):p.error('Private paths or unstripped symbols in module')
    manifest=json.loads((ROOT/'package/dualscreen/manifest.json').read_text())
    package=json.loads((ROOT/'package/package.json').read_text())
    manifest['module']['libraries']={'android-arm64-v8a':dict(path='modules/android-arm64-v8a/0100F4300BF2C000.so',sha256=hashlib.sha256(binary).hexdigest())}
    package['module']=manifest['module']
    # Positive allowlist: no recursive copy of the project, private dump, images or APKs.
    entries={'package.json':(json.dumps(package,indent=2)+'\n').encode(),
             'dualscreen/manifest.json':(json.dumps(manifest,indent=2)+'\n').encode(),
             'dualscreen/modules/android-arm64-v8a/0100F4300BF2C000.so':binary,
             'LICENSE.txt':(ROOT/'LICENSE').read_bytes(),
             'THIRD_PARTY_NOTICES.txt':(ROOT/'THIRD_PARTY_NOTICES.md').read_bytes(),
             'bcdec-LICENSE.txt':(ROOT/'vendor/bcdec/LICENSE').read_bytes()}
    out=ROOT/'build/dist/0100F4300BF2C000.dsmod.zip';out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(entries.items()):
            z=zipfile.ZipInfo(name,(2026,1,1,0,0,0));z.external_attr=0o100644<<16;z.compress_type=zipfile.ZIP_DEFLATED;archive.writestr(z,data)
    print(out)
if __name__=='__main__':main()
