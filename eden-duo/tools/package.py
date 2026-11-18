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
    p.add_argument('--module',type=Path)
    p.add_argument('--validation-build',action='store_true',help='Create a clearly labelled diagnostic package; never a release or completed-feature claim')
    args=p.parse_args()
    if args.module is None:args.module=ROOT/('build/android-arm64-v8a-diagnostic' if args.validation_build else 'build/android-arm64-v8a')/'0100F4300BF2C000.so'
    evidence=json.loads((ROOT/'research/builds.json').read_text())
    if not args.validation_build and (not evidence['release_ready'] or evidence['status']!='verified-on-device'):
        p.error('Live Snap mappings and map calibration are unverified. No installable package was created.')
    if not args.validation_build:
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
    if args.validation_build and b'SCN Read-only Validation reader 1' not in binary:p.error('Build the diagnostic reader, not the production module, for validation')
    manifest=json.loads((ROOT/'package/dualscreen/manifest.json').read_text())
    package=json.loads((ROOT/'package/package.json').read_text())
    if args.validation_build:
        package['name']=manifest['name']='SCN Read-only Validation'
        package['version']=manifest['version']='0.1.0-validation.1'
        for page in manifest['pages']:
            for widget in page.get('widgets',[]):
                if widget.get('text')=='Snap Companion Next':widget['text']='SCN • Read-only Validation'
        manifest['actions']['reader_check']=dict(kind='page',page='reader_check')
        manifest['actions']['reader_home']=dict(kind='page',page='home')
        manifest['pages'][0]['widgets'].append(dict(type='button',rect=[40,865,500,56],text='Reader Check • Development',on_tap='reader_check',text_scale=4,color='#FFFFFFFF',bg='#FFA53637',align='center',text_center_h=56,border=0))
        def label(rect,text=None,**kw):
            w=dict(type='label',rect=rect,text_scale=4,color='#FF573C2C',wrap_width=rect[2],max_lines=2)
            w.update(kw)
            if text is not None:w['text']=text
            return w
        manifest['pages'].append(dict(id='reader_check',title='Reader Check',no_auto_leave=True,widgets=[
            dict(type='rect',rect=[0,0,1240,1080],bg='#FFFFFCF0',color='#00000000',frame=0),
            label([40,40,1160,90],'Read-only Validation',text_scale=7),
            label([40,140,1160,80],'Unverified readings • Compare with the game. This is not the finished companion.'),
            label([40,270,1160,100],bind_text='scn.candidate.course_name',need_bind='scn.candidate.present'),
            label([40,390,400,60],'Selected Level',need_bind='scn.candidate.present'),
            dict(type='value',rect=[460,390,300,60],bind='scn.candidate.level',text_scale=6,color='#FF573C2C',need_bind='scn.candidate.present'),
            label([40,500,1160,80],bind_text='scn.candidate.position',need_bind='scn.candidate.present'),
            label([40,650,1160,100],'No unique active course object found yet.',need_bind='!scn.candidate.present'),
            label([40,790,1160,100],'Map alignment, route variants and the original interface remain unfinished. No game controls are sent.'),
            dict(type='button',rect=[40,960,240,64],text='Back',on_tap='reader_home',text_scale=4,color='#FFFFFFFF',bg='#FF3B9271',align='center',text_center_h=64,border=0)
        ]))
    manifest['module']['libraries']={'android-arm64-v8a':dict(path='modules/android-arm64-v8a/0100F4300BF2C000.so',sha256=hashlib.sha256(binary).hexdigest())}
    package['module']=manifest['module']
    # Positive allowlist: no recursive copy of the project, private dump, images or APKs.
    entries={'package.json':(json.dumps(package,indent=2)+'\n').encode(),
             'dualscreen/manifest.json':(json.dumps(manifest,indent=2)+'\n').encode(),
             'dualscreen/modules/android-arm64-v8a/0100F4300BF2C000.so':binary,
             'LICENSE.txt':(ROOT/'LICENSE').read_bytes(),
             'THIRD_PARTY_NOTICES.txt':(ROOT/'THIRD_PARTY_NOTICES.md').read_bytes(),
             'NDK-LLVM-NOTICE.txt':(ROOT/'vendor/ndk/LLVM-NOTICE').read_bytes(),
             'bcdec-LICENSE.txt':(ROOT/'vendor/bcdec/LICENSE').read_bytes()}
    out=ROOT/'build'/('validation' if args.validation_build else 'dist')/'0100F4300BF2C000.dsmod.zip';out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(entries.items()):
            z=zipfile.ZipInfo(name,(2026,1,1,0,0,0));z.external_attr=0o100644<<16;z.compress_type=zipfile.ZIP_DEFLATED;archive.writestr(z,data)
    print(out)
    if args.validation_build:print('READ-ONLY VALIDATION ONLY. Requested companion features are not complete or device-verified.')
if __name__=='__main__':main()
