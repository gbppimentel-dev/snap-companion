#!/usr/bin/env python3
"""Regenerate schematic route alignments from bundled art and rail facts. Requires NumPy, SciPy and Pillow."""
from pathlib import Path
import json,numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt,maximum_filter,gaussian_filter1d
root=Path(__file__).resolve().parents[1];ui=root/'app/src/main/assets/ui';alignments=json.loads((root/'tools/data/route-map-alignments.json').read_text())['matrices']
art=json.loads((ui/'map-data.js').read_text().split('window.SnapMapData=')[1].removesuffix(';\n'));graphs=json.loads((ui/'route-data.js').read_text().split('window.SnapRouteData=')[1].removesuffix(';\n'));out=[];stats=[]
# Disconnected seasonal scenes and the strongly distorted cave/camp drawings need piecewise calibration.
exclude={10,26,30}
work={}
for c in art['courses']:
 gid=c['gameId'];g=next(g for g in graphs['courses']if g['id']==c['id'])
 if str(gid)not in alignments:continue
 matrix=np.array(alignments[str(gid)]);im=Image.new('RGBA',(960,540))
 for r in c['routes']:im.alpha_composite(Image.open(ui/r['asset']).convert('RGBA').resize((960,540)))
 px=np.array(im);mask=(px[:,:,0]<110)&(px[:,:,1]>130)&(px[:,:,2]>130)&(px[:,:,3]>180)
 inside=distance_transform_edt(mask);center=mask&(inside>=maximum_filter(inside,size=3)-.01);dt,indices=distance_transform_edt(~center,return_indices=True)
 for rail in g['paths']:
  p=np.array(rail['points']);ds=np.r_[0,np.cumsum(np.linalg.norm(np.diff(p,axis=0),axis=1))];forced=[e['at']for e in rail['edges']if not e['conditional']];end=min(forced)if forced else ds[-1];ts=np.unique(np.r_[np.linspace(0,end,max(24,round(end*3))),[e['at']for e in rail['edges']if 0<=e['at']<=end]]);samples=np.stack([np.interp(ts,ds,p[:,i])for i in range(3)],1);q=samples[:,[0,2]]@matrix[:4].reshape(2,2)+matrix[4:];bounds=np.all((q>=[0,0])&(q<=[959,539]),1);ii=np.round(q).astype(int);ii[:,0]=np.clip(ii[:,0],0,959);ii[:,1]=np.clip(ii[:,1],0,539);error=dt[ii[:,1],ii[:,0]];valid=bounds&(error<=25)
  snapped=np.stack([indices[1,ii[:,1],ii[:,0]],indices[0,ii[:,1],ii[:,0]]],1).astype(float);snapped=gaussian_filter1d(snapped,2.2,axis=0,mode='nearest')
  # Reject individual gaps and large projection jumps; never interpolate an unmapped area.
  jumps=np.r_[False,np.linalg.norm(np.diff(snapped,axis=0),axis=1)>10];valid&=~jumps
  if valid.mean()<.85:continue
  if rail['id']==g['main']and c['starts']:
   snapped[0]=[c['starts'][0]['x']*960,c['starts'][0]['y']*540];valid[0]=True
  anchors=[[round(float(t),6),round(float(uv[0]/960),7)if ok else None,round(float(uv[1]/540),7)if ok else None]for t,uv,ok in zip(ts,snapped,valid)]
  sections=[dict(from_=0,to=round(end,6),name='Main Trail'if rail['id']==g['main']else'Alternate Trail')]
  if gid in [1,2]:
   names={906114404:'Park Trail',1808152586:'Dam Crossing',1553600842:'Flower Field',1789118890:'Flower Field',2161986298:'Flower Field'}
   sections[0]['name']=names.get(int(rail['id']),'Lakeside Trail')
  if gid in [18,19]:
   sections[0]['name']='Desert Trail'if rail['id']==g['main']else'Alternate Trail'
   if rail['id']==g['main']and gid==19:sections=[dict(from_=0,to=55,name='Opening Dunes'),dict(from_=55,to=125,name='Oasis'),dict(from_=125,to=round(end,6),name='Sand Pits')]
  entry=dict(course=c['id'],rail=rail['id'],schematic=True,projection=matrix.tolist(),anchors=anchors,sections=[{('from'if k=='from_'else k):v for k,v in s.items()}for s in sections]);out.append(entry);work[(c['id'],rail['id'])]=(entry,p,ds);stats.append((gid,rail['id'],round(float(valid.mean()),3)))

# A source event and a destination start that occupy the same measured world point
# must share one illustrated junction. Bound the correction to eight artwork pixels;
# never stitch separate scenes, rejected gaps or distant-looking branches together.
parents={};nodes={};links=[]
def find(k):
 parents.setdefault(k,k)
 if parents[k]!=k:parents[k]=find(parents[k])
 return parents[k]
for g in graphs['courses']:
 for rail in g['paths']:
  source=work.get((g['id'],rail['id']))
  if not source:continue
  entry,p,ds=source;a=np.array([[np.nan if x is None else x for x in row]for row in entry['anchors']])
  for edge in rail['edges']:
   target=work.get((g['id'],edge['to']))
   if not target or edge['at']>a[-1,0]:continue
   dest,tp,_=target;world=np.array([np.interp(edge['at'],ds,p[:,i])for i in range(3)])
   if np.linalg.norm(world-tp[0])>.15:continue
   uv=np.array([np.interp(edge['at'],a[:,0],a[:,i])for i in [1,2]])*[960,540];dv=np.array(dest['anchors'][0][1:],dtype=float)*[960,540]
   if not np.isfinite([*uv,*dv]).all() or np.linalg.norm(uv-dv)>8:continue
   ka=(g['id'],rail['id'],round(edge['at'],6));kb=(g['id'],edge['to'],0.0);nodes[ka]=uv;nodes[kb]=dv;parents[find(ka)]=find(kb);links.append((ka,kb))
groups={}
for k in nodes:groups.setdefault(find(k),[]).append(k)
corrections={};joined=0
for keys in groups.values():
 pts=np.array([nodes[k]for k in keys]);center=pts.mean(axis=0)
 if np.max(np.linalg.norm(pts[:,None]-pts[None,:],axis=2))>8:continue
 for k in keys:corrections.setdefault(k[:2],[]).append((k[2],center-nodes[k]));joined+=1
for key,changes in corrections.items():
 entry,_,_=work[key];changes.sort(key=lambda x:x[0]);a=entry['anchors']
 for i,(d,delta) in enumerate(changes):
  left=min(4,(d-changes[i-1][0])/2)if i else 4;right=min(4,(changes[i+1][0]-d)/2)if i+1<len(changes)else 4
  for row in a:
   if row[1]is None or row[2]is None:continue
   span=left if row[0]<d else right;gap=abs(row[0]-d)
   if span>0 and gap<=span:
    weight=(1+np.cos(np.pi*gap/span))/2;row[1]=round(row[1]+float(delta[0]/960*weight),7);row[2]=round(row[2]+float(delta[1]/540*weight),7)
(ui/'route-calibration.js').write_text('/* Schematic alignment to the authored route artwork; not a surveyed world map. Section names are companion labels. */\nwindow.SnapRouteCalibration='+json.dumps(out,separators=(',',':'))+';\n')
print('Calibrated',len(set(x['course']for x in out)),'course illustrations,',len(out),'rails; rejected intervals stay unknown')
print('Aligned',joined,'bounded junction anchors across',len(corrections),'rails')
