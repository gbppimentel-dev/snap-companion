"""Package an already signed, hash-validated APK; never installs or publishes."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def checksums(path, files):
    path.write_text(''.join(f'{sha(file)}  {file.name}\n' for file in files))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    validation = json.loads((ROOT/'packaging/validation-v0.5.24.json').read_text())
    compat = json.loads((ROOT/'compatibility.json').read_text())
    version = validation['version']
    assert version == compat['version']
    assert sha(args.apk) == validation['sha256'], 'APK differs from the locally validated signed release'
    with zipfile.ZipFile(args.apk) as apk:
        assert apk.testzip() is None
        for name in apk.namelist():
            assert Path(name).suffix.lower() not in {'.keystore', '.sav', '.nsp', '.nca'}, name
            assert not any(value in Path(name).name.lower() for value in ['diagnostic-lentalk', 'runtime.conf', 'prod.keys']), name
        assert apk.read('assets/ui/github.svg')
        assert b'gbppimentel-dev' in apk.read('assets/ui/app.js')
    name = f'SnapCompanion-v{version}'
    destination = (args.output or ROOT/'dist'/f'release-v{version}').resolve()
    destination.mkdir(parents=True, exist_ok=True)
    stage = destination/name
    stage.mkdir(exist_ok=True)
    signed = destination/(name+'.apk')
    shutil.copy2(args.apk, signed)
    shutil.copy2(signed, stage/signed.name)
    for source, target in [('packaging/QUICK-START.md', 'READ-ME-FIRST.md'), ('packaging/QUICK-START.txt', 'READ-ME-FIRST.txt'),
                           ('LICENSE', 'LICENSE'), ('THIRD_PARTY_NOTICES.md', 'THIRD_PARTY_NOTICES.md'),
                           ('compatibility.json', 'compatibility.json'), ('packaging/validation-v0.5.24.json', 'validation.json')]:
        shutil.copy2(ROOT/source, stage/target)
    commit = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=ROOT, capture_output=True, text=True)
    manifest = dict(name='Snap Companion', version=version, versionCode=validation['versionCode'],
                    package='dev.gbppimentel.snapcompanion', sourceRepository='https://github.com/gbppimentel-dev/snap-companion',
                    sourceCommit=commit.stdout.strip() if commit.returncode == 0 else None,
                    apkSha256=sha(signed), signingCertificateSha256=validation['certificateSha256'],
                    validation=validation['status'], internetPermission=False, includesPersonalData=False)
    release_manifest = destination/f'release-manifest-v{version}.json'
    release_manifest.write_text(json.dumps(manifest, indent=2)+'\n')
    shutil.copy2(release_manifest, stage/'release-manifest.json')
    allowed = {signed.name, 'READ-ME-FIRST.md', 'READ-ME-FIRST.txt', 'LICENSE', 'THIRD_PARTY_NOTICES.md',
               'compatibility.json', 'validation.json', 'release-manifest.json', 'SHA256SUMS.txt'}
    assert {p.name for p in stage.iterdir()} <= allowed, 'Unexpected file in the release bundle directory'
    checksums(stage/'SHA256SUMS.txt', sorted(p for p in stage.iterdir() if p.name != 'SHA256SUMS.txt'))
    bundle = destination/(name+'.zip')
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(stage.iterdir()): archive.write(file, name+'/'+file.name)
    matrix = destination/f'compatibility-v{version}.json'
    shutil.copy2(ROOT/'compatibility.json', matrix)
    checksums(destination/f'SHA256SUMS-v{version}.txt', [signed, bundle, matrix, release_manifest])
    print(f'Release assets prepared: {destination}')


if __name__ == '__main__': main()
