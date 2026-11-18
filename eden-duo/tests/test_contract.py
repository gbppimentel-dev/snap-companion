"""Source/package constraints that protect stable and prevent invented live tracking."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('scn_manifest',ROOT/'tools/generate_manifest.py')
generator=importlib.util.module_from_spec(spec);spec.loader.exec_module(generator)

class Contract(unittest.TestCase):
    def test_generated_manifest_current(self):
        self.assertEqual(json.loads((ROOT/'package/dualscreen/manifest.json').read_text()),generator.manifest())
    def test_native_delivery_has_no_gameplay_writes(self):
        m=generator.manifest()
        self.assertEqual(m['title_id'],'0100F4300BF2C000')
        self.assertEqual(m['module']['build_ids'],[generator.BUILD])
        self.assertFalse(m['module_tick_hidden'])
        self.assertTrue(m['requires_module'])
        self.assertEqual(m['min_runtime'],25)
        for action in m['actions'].values():self.assertIn(action['kind'],['page','view_reset','flag'])
    def test_no_stale_map_layers(self):
        m=generator.manifest()
        for page in m['pages']:
            for w in page['widgets']:
                if w['type']=='map':
                    self.assertEqual(w['need_bind'],'scn.map_valid')
                    for layer in w['overlays']:self.assertIs(layer['src_hold'],False)
    def test_release_refuses_unverified_state(self):
        evidence=json.loads((ROOT/'research/builds.json').read_text())
        if evidence['release_ready']:self.skipTest('Release evidence has since been supplied')
        r=subprocess.run([sys.executable,str(ROOT/'tools/package.py')],capture_output=True,text=True)
        self.assertNotEqual(r.returncode,0)
        self.assertIn('unverified',r.stderr)
        self.assertFalse((ROOT/'build/dist/0100F4300BF2C000.dsmod.zip').exists())
    def test_package_contains_no_dump_or_android_artifacts(self):
        allowed={'package.json','dualscreen/manifest.json'}
        actual={str(p.relative_to(ROOT/'package')) for p in (ROOT/'package').rglob('*') if p.is_file()}
        self.assertEqual(actual,allowed)
    def test_stable_identity_and_tag_preserved(self):
        fork=json.loads((ROOT.parent/'fork.json').read_text())
        self.assertEqual(fork['delivery']['kind'],'android-apk')
        self.assertTrue(fork['delivery']['updatesExistingApp'])
        self.assertFalse(fork['delivery']['requiredDuoMod'])
        result=subprocess.check_output(['git','rev-parse','v0.5.24^{commit}'],cwd=ROOT,text=True).strip()
        self.assertEqual(result,fork['base']['commit'])
        self.assertEqual(fork['applicationId'],fork['base']['applicationId'])
if __name__=='__main__':unittest.main()
