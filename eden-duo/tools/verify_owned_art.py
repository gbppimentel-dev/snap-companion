#!/usr/bin/env python3
"""Private verification only. No own-game paths or pixels are written into the source tree.

Requires numpy and texture2ddecoder plus the owner's independent NUTEXB reference module.
Checks every original map/route through runtime-style archive reads and compares the output
to a separately implemented deswizzler/BC decoder. Not run by public CI.
"""
import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import time
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--owned',type=Path,required=True)
    p.add_argument('--reference',type=Path,required=True)
    p.add_argument('--report',type=Path,required=True,help='Private report path outside package/')
    args=p.parse_args()
    spec=importlib.util.spec_from_file_location('nutexb_reference',args.reference)
    ref=importlib.util.module_from_spec(spec);spec.loader.exec_module(ref)
    rows=[e for e in json.loads((args.owned/'ui-entry-index.json').read_text()) if e['name'].startswith(('course_map_l_','course_arrow_l_'))]
    formats=Counter();errors=Counter();t=time.monotonic()
    with tempfile.TemporaryDirectory(prefix='scn-map-') as tmp:
        output=Path(tmp)/'decoded.pam'
        for i,e in enumerate(rows):
            archive=int(Path(e['archive']).stem.removeprefix('course_l_'))
            subprocess.run([str(ROOT/'build/host/scn-art'),str(args.owned/'romfs-ui'),str(archive),e['name'],str(output)],check=True,stdout=subprocess.DEVNULL)
            data=output.read_bytes();start=data.index(b'ENDHDR\n')+7
            actual=np.frombuffer(data[start:],np.uint8).reshape(1080,1920,4)
            name=e['name'] if e['name'].endswith('.nutexb') else e['name']+'.nutexb'
            tex=ref.parse(args.owned/'decoded-ui'/e['archive']/name)
            lin,_,_=ref.level_linear(tex,0);rgba=ref.decode_blocks(lin,tex['w'],tex['h'],tex['fmt']).astype(np.uint32)
            alpha=rgba[...,3];total=alpha[::2,::2]+alpha[::2,1::2]+alpha[1::2,::2]+alpha[1::2,1::2]
            expected=np.zeros_like(actual)
            for c in range(3):
                v=rgba[...,c]*alpha;s=v[::2,::2]+v[::2,1::2]+v[1::2,::2]+v[1::2,1::2]
                expected[...,c]=np.where(total,(s+total//2)//np.maximum(total,1),0)
            expected[...,3]=(total+2)//4
            difference=np.abs(actual.astype(np.int16)-expected.astype(np.int16))
            peak=int(difference.max());formats[hex(tex['fmt'])]+=1;errors[hex(tex['fmt'])]=max(errors[hex(tex['fmt'])],peak)
            # Independent BC decoders can differ by a few integer rounding levels.
            assert peak<=3,('Independent decoder mismatch',e['name'],peak)
            assert np.array_equal(actual[...,3],expected[...,3]),('Alpha mismatch',e['name'])
            if (i+1)%30==0:print('Verified',i+1,'of',len(rows),flush=True)
    report=dict(scope='Private owned-file verification on Mac; not a Thor/runtime test',count=len(rows),formats=dict(formats),max_channel_difference=dict(errors),alpha_exact=True,seconds=round(time.monotonic()-t,2))
    args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
