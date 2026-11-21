"""Package a verified signed current release; never installs or publishes."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''):
            h.update(chunk)
    return h.hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk',type=Path,required=True)
    parser.add_argument('--output-dir',type=Path)
    args=parser.parse_args()
    config=json.loads((ROOT/'fork.json').read_text())
    version=config['version'];tag='v'+version
    assert config['channel']=='release' and version!='0.5.24' and '-dev' not in version
    assert args.apk.is_file()
    out=args.output_dir or ROOT/'dist/releases'/tag
    assert not out.exists(), 'Output already exists; release assets are not replaced.'
    validation_path=ROOT/f'packaging/validation-{tag}.json'
    validation=json.loads(validation_path.read_text())
    assert validation['version']==version and validation['versionCode']==config['versionCode']
    assert validation['sha256']==digest(args.apk), 'APK differs from the validated release'
    assert validation['includesPersonalData'] is False and validation['noInternetPermission']
    assert validation['releaseHasNoDebugHarness']
    assert validation['certificateSha256']==config['base']['certificateSha256']
    compat=json.loads((ROOT/'compatibility.json').read_text())
    assert compat['version']==version and compat['package']==config['applicationId']
    assert subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()=='', 'Commit intended source before release packaging'
    subprocess.run([sys.executable,str(ROOT/'tests/publication-audit.py')],check=True,cwd=ROOT)
    subprocess.run([sys.executable,str(ROOT/'tests/upgrade-identity.py'),'--apk',str(args.apk.resolve()),'--require-baseline-signer'],check=True,cwd=ROOT)
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    out.mkdir(parents=True)
    apk=out/f'SnapCompanion-{tag}.apk';shutil.copyfile(args.apk,apk)
    compatibility=out/f'compatibility-{tag}.json';shutil.copyfile(ROOT/'compatibility.json',compatibility)
    manifest=out/f'release-manifest-{tag}.json'
    manifest.write_text(json.dumps({'version':version,'versionCode':config['versionCode'],'package':config['applicationId'],'certificateSha256':validation['certificateSha256'],'apk':{'name':apk.name,'sha256':digest(apk),'bytes':apk.stat().st_size},'source':{'repository':'https://github.com/gbppimentel-dev/snap-companion','tag':tag,'commit':commit},'validationFile':validation_path.relative_to(ROOT).as_posix(),'validationSha256':digest(validation_path),'compatibilitySha256':digest(compatibility),'includesPersonalData':False,'limitations':compat['limits']},indent=2)+'\n')
    bundle=out/f'SnapCompanion-{tag}.zip'
    with zipfile.ZipFile(bundle,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,name in [(apk,apk.name),(compatibility,'compatibility.json'),(manifest,'release-manifest.json'),(validation_path,'validation.json'),(ROOT/'packaging/QUICK-START.md','QUICK-START.md'),(ROOT/'packaging/QUICK-START.txt','QUICK-START.txt'),(ROOT/'LICENSE','LICENSE'),(ROOT/'THIRD_PARTY_NOTICES.md','THIRD_PARTY_NOTICES.md'),(ROOT/'docs/releases'/f'{tag}.md',f'docs/releases/{tag}.md')]:
            z.write(p,name)
        for name in ['SETUP.md','COMPATIBILITY.md','PRIVACY.md','ARCHITECTURE.md','BUILDING.md']:
            z.write(ROOT/'docs'/name,'docs/'+name)
        for p in sorted((ROOT/'app/src/main/assets/licenses').glob('*.txt')):
            z.write(p,'licenses/'+p.name)
        z.writestr('SOURCE.txt',f'Corresponding release source: https://github.com/gbppimentel-dev/snap-companion/tree/{tag}\nSource archive: https://github.com/gbppimentel-dev/snap-companion/archive/refs/tags/{tag}.zip\nCommit: {commit}\nSee included notices/licenses for original MIT and reused GPL/third-party components.\nNo private signing key, game archive, user save or personal photo is included.\n')
    sums=out/f'SHA256SUMS-{tag}.txt'
    sums.write_text(''.join(f'{digest(p)}  {p.name}\n' for p in [apk,bundle,compatibility,manifest]))
    with zipfile.ZipFile(bundle) as z:
        assert z.testzip() is None
        assert hashlib.sha256(z.read(apk.name)).hexdigest()==validation['sha256']
        assert json.loads(z.read('validation.json'))==validation
    for p in [apk,bundle,compatibility,manifest,sums]:
        print(p.name,p.stat().st_size,digest(p))
    print('Prepared release assets; no installation, tag creation or publication performed.')

if __name__=='__main__':
    main()
