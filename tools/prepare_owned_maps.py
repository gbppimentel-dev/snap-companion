#!/usr/bin/env python3
"""Prepare selected course presentation assets and factual location records from owned game files.
No game archives, executables, saves, keysets or runtime packets are emitted. Requires Pillow.
"""
import argparse
import json
from pathlib import Path
import re
import struct
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
class Flat:
    def __init__(self,path):self.b=Path(path).read_bytes()
    def read(self,p,fmt):
        n=struct.calcsize(fmt)
        if p is None or p<0 or p+n>len(self.b):raise ValueError('Metadata bounds')
        out=struct.unpack_from(fmt,self.b,p);return out[0] if len(out)==1 else out
    def fields(self,t):
        vt=t-self.read(t,'<i');n,size=self.read(vt,'<HH')
        if n<4 or n%2 or n>512 or size<4 or t+size>len(self.b):raise ValueError('Metadata table')
        return {i:t+off for i in range((n-4)//2) if (off:=self.read(vt+4+2*i,'<H')) and off<size}
    def field(self,t,s):return self.fields(t).get(s)
    def target(self,p):return p+self.read(p,'<I')
    def tables(self,t,s):
        p=self.target(self.field(t,s));n=self.read(p,'<I')
        if n>16384 or p+4+4*n>len(self.b):raise ValueError('Metadata vector')
        return [self.target(p+4+4*i)for i in range(n)]
    def string(self,t,s):
        p=self.target(self.field(t,s));n=self.read(p,'<I')
        if n>65536 or p+4+n>=len(self.b)or self.b[p+4+n]:raise ValueError('Metadata string')
        return self.b[p+4:p+4+n].decode('utf-8')
    def root(self):return self.read(0,'<I')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--owned-root',type=Path,required=True)
    parser.add_argument('--art-tool',type=Path,default=ROOT/'eden-duo/build/host/scn-art')
    args=parser.parse_args();owned=args.owned_root
    from PIL import Image,ImageChops
    target=ROOT/'app/src/main/assets/game-art/maps';target.mkdir(parents=True,exist_ok=True)
    ids={r['gameId']:r['id']for r in json.loads((ROOT/'app/src/main/assets/guidebook.json').read_text())['courses']}
    species={r['index']+1:r['national']for r in json.loads((ROOT/'app/src/main/assets/eden-species.json').read_text())['mapping']}
    route=Flat(owned/'decoded-metadata/Resident.drp/RouteData.rtd')
    eco=Flat(owned/'decoded-metadata/Resident.drp/EcologicalBookData.ecbd')
    courses={}
    for row in route.tables(route.root(),0):
        gid=route.read(route.field(row,0),'<B');stem=route.string(row,16)
        if gid not in ids or not re.fullmatch(r'\d{3}_\d_[A-Z]',stem):raise ValueError('Course identity')
        courses[gid]={'id':ids[gid],'gameId':gid,'stem':stem,'maps':[],'routes':[],'points':[]}
    unmapped=set()
    for row in eco.tables(eco.root(),0):
        gid=eco.read(eco.field(row,0),'<B');seen=set()
        for point in eco.tables(row,1):
            model,serial,x,y=[eco.read(eco.field(point,k),'<H')if eco.field(point,k)is not None else 0 for k in range(4)]
            if not model or not 0<serial<=32 or not 0<=x<=3840 or not 0<=y<=2160 or (model,serial)in seen:raise ValueError('Location dimensions/identity')
            seen.add((model,serial));kind='pokemon' if model in species else 'crystabloom' if model in {1001,1003,1005,1007,1009,1011} else 'landmark'
            if model<1000 and model not in species:unmapped.add(model);continue
            courses[gid]['points'].append({'key':f'{model}-{serial}','model':model,'pokemon':species.get(model,0),'kind':kind,'serial':serial,'x':round(x/3840,7),'y':round(y/2160,7)})
    index=json.loads((owned/'ui-entry-index.json').read_text());textures=[r for r in index if r['name'].startswith(('course_map_l_','course_arrow_l_'))]
    with tempfile.TemporaryDirectory(prefix='snap-map-prepare-')as tmp:
        pam=Path(tmp)/'map.pam'
        for i,row in enumerate(textures):
            archive=int(Path(row['archive']).stem.removeprefix('course_l_'));name=row['name'].removesuffix('.nutexb')
            if archive not in courses:raise ValueError('Map archive identity')
            subprocess.run([str(args.art_tool),str(owned/'romfs-ui'),str(archive),name,str(pam)],check=True,stdout=subprocess.DEVNULL)
            b=pam.read_bytes();head,end=b.split(b'ENDHDR\n',1)
            if b'WIDTH 1920\n'not in head or b'HEIGHT 1080\n'not in head or len(end)!=1920*1080*4:raise ValueError('Image dimensions')
            image=Image.frombytes('RGBA',(1920,1080),end)
            rel=name+'.webp';arrow=name.startswith('course_arrow_')
            image.save(target/rel,'WEBP',lossless=arrow,quality=90,method=6)
            key='routes' if arrow else 'maps';num=int(re.search(r'(?:Route|_)(\d+)$',name)[1])
            courses[archive][key].append({'id':num,'asset':'../game-art/maps/'+rel})
            if (i+1)%25==0:print('Prepared',i+1,'/',len(textures),flush=True)
    if len(courses)!=30 or any(not c['maps']for c in courses.values()):raise ValueError('Incomplete course maps')
    for c in courses.values():
        c['maps'].sort(key=lambda r:r['id']);c['routes'].sort(key=lambda r:r['id'])
        # The white ring baked into the route art denotes the fixed start, never the live vehicle.
        starts=[]
        for route_image in c['routes']:
            red,green,blue,alpha=Image.open(ROOT/'app/src/main/assets/ui'/route_image['asset']).convert('RGBA').split()
            mask=ImageChops.darker(ImageChops.darker(red.point(lambda x:255 if x>160 else 0),green.point(lambda x:255 if x>180 else 0)),ImageChops.darker(blue.point(lambda x:255 if x>160 else 0),alpha.point(lambda x:255 if x>180 else 0)))
            box=mask.getbbox()
            if box:
                x0,y0,x1,y1=box;width,height=x1-x0,y1-y0
                if 40<width<95 and 40<height<95 and abs(width-height)<5:starts.append({'x':(x0+x1)/3840,'y':(y0+y1)/2160})
        unique=[]
        for point in starts:
            if not any(abs(p['x']-point['x'])+abs(p['y']-point['y'])<.01 for p in unique):unique.append(point)
        c['starts']=unique
    out={'schema':1,'width':3840,'height':2160,'courses':list(courses.values())}
    (ROOT/'app/src/main/assets/ui/map-data.js').write_text('/* Authored map locations from owned game tables; presentation rights remain with their owners. */\nwindow.SnapMapData='+json.dumps(out,separators=(',',':'))+';\n')
    print(json.dumps({'courses':len(courses),'textures':len(textures),'points':sum(len(c['points'])for c in courses.values()),'unmappedSpecies':sorted(unmapped),'imageBytes':sum(p.stat().st_size for p in target.glob('*.webp'))}))
if __name__=='__main__':main()
