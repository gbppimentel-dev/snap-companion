#!/usr/bin/env python3
"""Read-only Pass4 collector for the companion's standard service metrics. Never installs or sends input."""
import argparse,json,pathlib,re,subprocess,time
PACKAGE='dev.gbppimentel.snapcompanion.next'
COUNTERS=['frames','readings','successfulInferenceMillis','failedInferenceMillis','inferenceFailures','dropped','duplicates']
def parse(text,kind):
 rows=[]
 for line in text.splitlines():
  line=line.strip()
  if line.startswith('snap-metrics|'):
   row=json.loads(line.split('|',1)[1])
   if row.get('schema')==2 and row.get('kind')==kind and row.get('available') is not False:rows.append(row)
 if len(rows)!=1:raise ValueError('No unique current '+kind+' metrics; requires the new build and active service')
 return rows[0]
def delta(before,after):
 for r in [before,after]:
  if r.get('schema')!=2 or not isinstance(r.get('session'),str)or not re.fullmatch(r'[a-f0-9-]{36}',r['session'])or any(type(r.get(k))is not int or r[k]<0 for k in COUNTERS+['elapsedRealtimeMillis','sourceEpoch','gameEpoch'])or type(r.get('targetHz'))is not int or r['targetHz']not in [2,5,10]:return None
 if any(before[k]!=after[k]for k in ['session','sourceEpoch','gameEpoch','targetHz']):return None
 elapsed=after['elapsedRealtimeMillis']-before['elapsedRealtimeMillis']
 if elapsed<=0 or any(after[k]<before[k]for k in COUNTERS):return None
 return {'elapsedMillis':elapsed,'sampledFrameHz':(after['frames']-before['frames'])*1000/elapsed,'successfulInferenceHz':(after['readings']-before['readings'])*1000/elapsed,'successfulInferenceWallTimeRatio':(after['successfulInferenceMillis']-before['successfulInferenceMillis'])/elapsed,'totalInferenceWallTimeRatio':sum(after[k]-before[k]for k in ['successfulInferenceMillis','failedInferenceMillis'])/elapsed,'inferenceFailures':after['inferenceFailures']-before['inferenceFailures'],'dropped':after['dropped']-before['dropped'],'duplicates':after['duplicates']-before['duplicates']}
def segments(rows):
 groups=[]
 for row in rows:
  if not groups or delta(groups[-1][-1]['capture'],row['capture'])is None or any(groups[-1][-1]['link'].get(k)!=row['link'].get(k)for k in ['session','sourceEpoch']):groups.append([])
  groups[-1].append(row)
 return [{'samples':len(g),'configuredHz':g[0]['capture']['targetHz'],'captureDelta':delta(g[0]['capture'],g[-1]['capture'])if len(g)>1 else None}for g in groups]
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--adb',default='adb');p.add_argument('--serial');p.add_argument('--duration',type=int,default=60);p.add_argument('--interval',type=float,default=5);p.add_argument('--output',type=pathlib.Path);p.add_argument('--label',default='companion-active');p.add_argument('--self-test',action='store_true');a=p.parse_args()
 if a.self_test:
  assert parse('  snap-metrics|{"schema":2,"kind":"link","gameVerified":true}','link')['gameVerified']
  try:parse('snap-metrics|{"schema":1,"kind":"link"}','link');raise AssertionError('accepted incompatible metrics')
  except ValueError:pass
  base={k:0 for k in COUNTERS};base.update(schema=2,session='a'*36,sourceEpoch=1,gameEpoch=2,targetHz=2,elapsedRealtimeMillis=1000)
  end={**base,'elapsedRealtimeMillis':3000,'frames':4,'readings':3,'successfulInferenceMillis':600};r=delta(base,end);assert r['sampledFrameHz']==2 and r['successfulInferenceHz']==1.5 and r['successfulInferenceWallTimeRatio']==.3;assert delta(end,base)is None
  assert delta(base,{**end,'session':'b'*36})is None;assert delta(base,{**end,'sourceEpoch':2})is None
  print('Metrics parser/delta self-checks passed; no device access.');return 0
 if not a.serial or not re.fullmatch(r'[a-zA-Z0-9_.:-]{1,100}',a.serial)or not a.output:p.error('--serial and --output are required')
 if not 10<=a.duration<=3600 or not 1<=a.interval<=30:p.error('duration10–3600s, interval1–30s')
 def dump(service,kind):
  r=subprocess.run([a.adb,'-s',a.serial,'shell','dumpsys','activity','service',PACKAGE+'/.'+service],capture_output=True,text=True,timeout=15,check=True)
  return parse(r.stdout,kind)
 rows=[];started=time.monotonic();reason='duration-complete'
 try:
  while time.monotonic()-started<a.duration:
   link=dump('EdenLinkService','link')
   if link.get('gameVerified')is not True or link.get('gameVisible')is not True:reason='verified-visible-Snap-required';break
   capture=dump('CaptureService','capture')
   if capture.get('active')is not True or capture.get('sourceCurrent')is not True:reason='current-capture-source-required';break
   if capture.get('gameEpoch')!=link.get('sourceEpoch'):reason='source-changed-between-service-reads';break
   if delta(capture,{**capture,'elapsedRealtimeMillis':capture['elapsedRealtimeMillis']+1})is None:reason='invalid-capture-counters';break
   rows.append({'atHostMonotonic':time.monotonic(),'capture':capture,'link':link});print('Read-only sample',len(rows),'target',capture.get('targetHz'),'Hz',flush=True);time.sleep(min(a.interval,max(0,a.duration-(time.monotonic()-started))))
 except(KeyboardInterrupt,Exception)as error:reason=type(error).__name__+': '+str(error)
 grouped=segments(rows)
 result={'schema':2,'label':a.label,'endReason':reason,'readOnly':True,'elapsedHostSeconds':time.monotonic()-started,'samples':rows,'segments':grouped,'captureDelta':grouped[0]['captureDelta']if len(grouped)==1 else None,'limits':['Capture/input processing rates are not game rendered FPS or emulation speed.','Inference wall-time ratios are latency, not CPU utilization; total includes failed inference latency.','reportedFps/reportedFrameMillis are sampled visible overlay values, not independently measured frame distributions.','Labels do not verify a disabled/enabled condition; configured target/source fields and contiguous segments are authoritative.','Session/source/cadence changes and counter resets split measurements. No aggregate spans those boundaries.','Metric collection itself has overhead; sustained gameplay and baseline comparison remain separate Pass4 checks.']}
 a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,indent=2)+'\n');print('Saved',a.output,'end:',reason)
 return 0 if reason=='duration-complete'and len(rows)>=2 else 2
if __name__=='__main__':raise SystemExit(main())
