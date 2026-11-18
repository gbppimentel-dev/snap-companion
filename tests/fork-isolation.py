"""Check the built fork cannot be installed as the stable package or target its private controller files."""
import argparse
import io
import json
import os
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk', type=Path, required=True)
    args = p.parse_args()
    fork = json.loads((ROOT/'fork.json').read_text())
    stable = fork['base']
    assert fork['applicationId'] != stable['applicationId']
    assert fork['applicationId'] == stable['applicationId']+'.next'
    base_commit = subprocess.check_output(['git', 'rev-parse', stable['tag']+'^{commit}'], cwd=ROOT, text=True).strip()
    assert base_commit == stable['commit'], 'Stable tag moved'
    base_build = subprocess.check_output(['git', 'show', stable['tag']+':app/build.gradle'], cwd=ROOT, text=True)
    assert "applicationId '"+stable['applicationId']+"'" in base_build
    assert "versionName '0.5.24'" in base_build
    sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home()/'Library/Android/sdk')))
    aapt = sdk/'build-tools/36.0.0/aapt'
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(args.apk)], text=True)
    assert "name='"+fork['applicationId']+"'" in badging
    assert "versionName='"+fork['version']+"'" in badging
    assert "application-label:'Snap Companion Next'" in badging
    manifest = subprocess.check_output([str(aapt), 'dump', 'xmltree', str(args.apk), 'AndroidManifest.xml'], text=True)
    assert 'sharedUserId' not in manifest
    assert stable['applicationId']+'.MainActivity' not in manifest
    assert fork['applicationId']+'.MainActivity' in manifest
    assert 'android.permission.INTERNET' not in manifest
    assert 'UiAuditActivity' not in manifest
    with zipfile.ZipFile(args.apk) as apk:
        with zipfile.ZipFile(io.BytesIO(apk.read('assets/bridge/live-projection.jar'))) as bridge:
            dex = bridge.read('classes.dex')
        assert (fork['applicationId']+'\0').encode() in dex
        assert (stable['applicationId']+'\0').encode() not in dex
        assert ('/data/user/0/'+fork['applicationId']+'/files').encode() in dex
        assert ('/data/user/0/'+stable['applicationId']+'/files').encode() not in dex
    print('Built fork isolation passed: distinct package/launcher/namespace, private helper paths, no shared UID, stable tag pinned.')


if __name__ == '__main__': main()
