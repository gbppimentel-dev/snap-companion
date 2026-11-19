#!/usr/bin/env python3
"""Export bounded NEO-ONE route facts from the owner's already extracted level metadata.
Only factual rail points, rail-change events and course identities are emitted. No executables,
archives, keys, saves, process addresses, session packets or device access.
"""
import argparse,json,math
from pathlib import Path
from prepare_owned_maps import Flat
ROOT=Path(__file__).resolve().parents[1]

class RouteFlat(Flat):
    def u(self,t,s,default=0):
        p=self.field(t,s);return default if p is None else self.read(p,'<I')
    def byte(self,t,s):
        p=self.field(t,s);return 0 if p is None else self.read(p,'<B')
    def table(self,t,s):return self.target(self.field(t,s))
    def points(self,t,s):
        p=self.table(t,s);n=self.read(p,'<I')
        if n<1 or n>8192 or p+4+n*12>len(self.b):raise ValueError('Rail point bounds')
        out=[self.read(p+4+12*i,'<3f')for i in range(n)]
        if any(not math.isfinite(v)or abs(v)>1e6 for p in out for v in p):raise ValueError('Rail coordinates')
        return out
    def rows(self,t,s):return self.tables(t,s)if self.field(t,s)is not None else[]

def distance(a,b):return math.sqrt(sum((x-y)**2 for x,y in zip(a,b)))

def read_course(path,course):
    f=RouteFlat(path);r=f.root()
    if f.u(r,0)!=6:raise ValueError('Level schema')
    entities={f.u(e,1):e for e in f.rows(r,2)}
    nodes={f.u(n,1):f.u(n,2)for n in f.rows(r,3)}
    groups={f.u(g,0):g for g in f.rows(r,4)}
    main=f.u(f.table(r,6),0);paths={}
    def changes(group):
        # Path action payload s1 identifies another actor. s2 selects the NEO-ONE.
        # Both checks are required: ordinary Pokémon path actions share this payload type.
        result=[]
        for a in f.rows(group,1):
            if f.byte(a,0)!=4 or f.field(a,4)is None:continue
            p=f.table(a,4);node=f.u(p,0)
            if node in nodes and f.u(p,1)==0 and f.byte(p,2)==1:result.append(node)
        return result
    def load(node):
        if node in paths:return
        paths[node]=None;entity=entities.get(nodes.get(node))
        if entity is None:raise ValueError('Missing NEO-ONE node')
        body=next((f.table(c,8)for c in f.rows(entity,0)if f.byte(c,0)==9 and f.field(c,8)is not None),None)
        if body is None:raise ValueError('Missing NEO-ONE rail')
        points=[];events=[];offset=0.
        for segment in f.rows(body,1):
            ps=f.points(segment,1);ds=[0.]
            for a,b in zip(ps,ps[1:]):ds.append(ds[-1]+distance(a,b))
            for i,event_list in enumerate(f.rows(segment,2)):
                if i>=len(ds):raise ValueError('Rail event index')
                for event in f.rows(event_list,0):
                    group=groups.get(f.u(event,1))
                    if group is None:continue
                    for dest in changes(group):events.append({'at':round(offset+ds[i],6),'to':str(dest),'conditional':bool(f.rows(event,0))})
            points.extend(ps);offset+=ds[-1]
        if not 0<offset<1e6:raise ValueError('Rail length')
        paths[node]={'id':str(node),'length':round(offset,6),'points':[[round(v,6)for v in p]for p in points],'edges':events}
        for event in events:load(int(event['to']))
    load(main)
    for group in groups.values():
        for node in changes(group):load(node)
    return {'id':course['id'],'gameId':course['gameId'],'main':str(main),'paths':list(paths.values())}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--level-root',type=Path,required=True);a=p.parse_args()
    data=json.loads((ROOT/'app/src/main/assets/ui/map-data.js').read_text().split('window.SnapMapData=')[1].removesuffix(';\n'))
    out=[]
    for c in data['courses']:
        folder=a.level_root/str(c['gameId']);path=folder/('LV_MAP_'+c['stem']+'.levelb')
        if not path.exists():
            choices=list(folder.glob('*.levelb'))
            if len(choices)!=1:raise ValueError('Ambiguous map level identity')
            path=choices[0]
        out.append(read_course(path,c))
    value={'schema':1,'courses':out}
    (ROOT/'app/src/main/assets/ui/route-data.js').write_text('/* Factual NEO-ONE rail geometry and rail-change events from owned course metadata. */\nwindow.SnapRouteData='+json.dumps(value,separators=(',',':'))+';\n')
    print(json.dumps({'courses':len(out),'rails':sum(len(c['paths'])for c in out),'changes':sum(len(p['edges'])for c in out for p in c['paths'])}))
if __name__=='__main__':main()
