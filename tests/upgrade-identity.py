"""Verify this APK updates the original app; optionally require its installed-release signer."""
import argparse
import io
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk',type=Path,required=True)
    p.add_argument('--require-baseline-signer',action='store_true')
    args=p.parse_args();config=json.loads((ROOT/'fork.json').read_text());base=config['base']
    assert config['applicationId']==base['applicationId']=='dev.gbppimentel.snapcompanion'
    assert config['versionCode']>base['versionCode']
    assert config['delivery']['kind']=='android-apk' and config['delivery']['updatesExistingApp']
    tag=subprocess.check_output(['git','rev-parse',base['tag']+'^{commit}'],cwd=ROOT,text=True).strip()
    assert tag==base['commit'],'Historical release tag moved'
    sdk=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'Library/Android/sdk')));build=sdk/'build-tools/36.0.0'
    badging=subprocess.check_output([str(build/'aapt'),'dump','badging',str(args.apk)],text=True)
    assert "name='"+base['applicationId']+"'" in badging
    assert "versionName='"+config['version']+"'" in badging
    assert "versionCode='"+str(config['versionCode'])+"'" in badging
    assert "application-label:'Snap Companion'" in badging
    manifest=subprocess.check_output([str(build/'aapt'),'dump','xmltree',str(args.apk),'AndroidManifest.xml'],text=True)
    assert base['applicationId']+'.MainActivity' in manifest
    assert '.snapcompanion.next' not in manifest and 'sharedUserId' not in manifest
    assert 'UiAuditActivity' not in manifest and 'android.permission.INTERNET' not in manifest
    with zipfile.ZipFile(args.apk) as apk:
        with zipfile.ZipFile(io.BytesIO(apk.read('assets/bridge/live-projection.jar'))) as bridge:dex=bridge.read('classes.dex')
        assert (base['applicationId']+'\0').encode() in dex
        assert ('/data/user/0/'+base['applicationId']+'/files').encode() in dex
        assert b'dev.gbppimentel.snapcompanion.next' not in dex
        assert b'Ldev/gbppimentel/snapbridge/GameMemoryReader;' in dex,'Bundled persistent reader missing'
        native=apk.read('lib/arm64-v8a/libsnapmemory.so')
        assert native[:6]==b'\x7fELF\x02\x01','ARM64 native reader missing or invalid'
        assert native[18:20]==b'\xb7\x00','Reader must target AArch64'
        for method in ('open','sample','close','physicalReleased','pressTool'):
            assert ('Java_dev_gbppimentel_snapbridge_GameMemoryReader_'+method).encode() in native,'Missing JNI method: '+method
        assert len([n for n in apk.namelist() if n.startswith('assets/game-art/maps/') and n.endswith('.webp')])==143
        for name in ('assets/ui/map-data.js','assets/licenses/game-memory-GPL-3.0.txt','assets/licenses/NDK-LLVM-NOTICE.txt'):
            assert apk.read(name),'Missing map/license asset: '+name
    if args.require_baseline_signer:
        cert=subprocess.check_output([str(build/'apksigner'),'verify','--print-certs',str(args.apk)],text=True)
        hashes=re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]{64})',cert)
        assert len(hashes)==1 and hashes[0].lower()==base['certificateSha256'],'Signer differs: do not install or replace the existing key'
    print('Original app upgrade identity passed: same package/launcher/private paths, increasing versionCode, historical tag preserved'+('; baseline signing certificate matched.' if args.require_baseline_signer else '.'))
if __name__=='__main__':main()
