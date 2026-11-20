"""Check the public source/release boundary without device or network access."""
from pathlib import Path
import json
import re
import subprocess
import runpy

ROOT = Path(__file__).resolve().parents[1]
IGNORED = {'.git', '.gradle', '.idea', 'build', 'dist', '__pycache__', 'node_modules'}
FORBIDDEN = {'.nsp', '.nca', '.xci', '.sav', '.sqlite', '.db', '.keystore', '.jks', '.pem', '.p12', '.pfx', '.apk'}
required = ['README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md', 'compatibility.json', '.github/workflows/ci.yml',
            'docs/SETUP.md', 'docs/COMPATIBILITY.md', 'docs/PRIVACY.md', 'docs/BUILDING.md', 'packaging/package_release.py']
for name in required: assert (ROOT/name).is_file(), name
count = 0
files=subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard'],cwd=ROOT).decode().split('\0')
for name in sorted(set(filter(None,files))):
    path=ROOT/name
    relative = path.relative_to(ROOT)
    if not path.is_file() or any(p in IGNORED for p in relative.parts) or relative.as_posix() == 'local.properties': continue
    assert relative.parts[0] not in {'artifacts', 'research', 'device-tuning', '.toolchains', '.local-android'}, relative
    assert 'private' not in relative.parts and '.deps' not in relative.parts, relative
    assert path.suffix.lower() not in FORBIDDEN, relative
    assert not path.name.startswith('.env'), relative
    assert path.stat().st_size < 100_000_000, relative
    if path.suffix.lower() in {'.md', '.py', '.java', '.js', '.cjs', '.html', '.json', '.yml', '.xml', '.txt', '.sh', '.gradle'}:
        text = path.read_text()
        assert not re.search(r'gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{25,}|-----BEGIN [A-Z ]*PRIVATE KEY-----', text), relative
        assert not re.search(r'/Users/[A-Za-z0-9_.-]+/', text), relative
        assert not re.search(r'[A-Za-z0-9_.+-]+@(?:icloud|gmail|outlook)\.com', text), relative
    count += 1
compat = json.loads((ROOT/'compatibility.json').read_text())
validation = json.loads((ROOT/'packaging/validation-v0.5.24.json').read_text())
assert compat['version'] == validation['version'] == '0.5.24'
assert validation['includesPersonalData'] is False
assert validation['noInternetPermission'] and validation['releaseHasNoDebugHarness']
assert validation['certificateSha256']
assert not (ROOT/'app/src/androidTest/assets').exists(), 'Private fixture captures must not be published'
assert (ROOT/'app/src/main/assets/ui/github.svg').is_file()
# Reviewed reference notes ship without their source URLs and match the generator.
notes = runpy.run_path(str(ROOT/'tools/prepare_request_walkthroughs.py'))['prepare']()
assert (ROOT/'app/src/main/assets/ui/request-details.js').read_text() == notes
assert 'https://' not in notes and 'http://' not in notes
print(f'Publication audit passed: {count} source/documentation files; private inputs excluded.')
