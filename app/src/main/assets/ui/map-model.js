(function(root,factory){'use strict';const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;root.SnapMapModel=api;})(typeof window==='object'?window:globalThis,function(){'use strict';
const finite=v=>Number.isFinite(v),clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
function vector(v){return Array.isArray(v)&&v.length===3&&v.every(x=>finite(x)&&Math.abs(x)<1e6);}
function frame(v,now,course){return !!v&&v.status==='ready'&&['research','paused'].includes(v.phase)&&v.course===course&&finite(v.sampleAt)&&now>=v.sampleAt&&now-v.sampleAt<=1000&&finite(v.distance)&&finite(v.length)&&v.length>0&&v.distance>=0&&v.distance<=v.length+.1;}
function collection(photos,id){const medals=[1,2,3,4].map(n=>photos?.[id+':'+n]?.medal||'Missing');return{medals,found:medals.filter(m=>m!=='Missing').length,diamond:medals.filter(m=>m==='Diamond').length};}
function clusters(points,view,size=56){const groups=new Map();for(const p of points){const x=(p.x*view.width-view.width/2)*view.zoom+view.width/2+view.x,y=(p.y*view.height-view.height/2)*view.zoom+view.height/2+view.y;if(x< -size||y< -size||x>(view.clipWidth||view.width)+size||y>(view.clipHeight||view.height)+size)continue;const key=Math.floor(x/size)+':'+Math.floor(y/size);let g=groups.get(key);if(!g){g={x:0,y:0,points:[]};groups.set(key,g);}g.x+=x;g.y+=y;g.points.push(p);}return Array.from(groups.values(),g=>({x:g.x/g.points.length,y:g.y/g.points.length,points:g.points}));}
// Cluster against map coordinates, not viewport cells. Panning never changes membership.
function markerBucket(zoom){return 2**(Math.floor(Math.log2(clamp(zoom,1,4))*2)/2);}
function markerGroups(points,view,size=43){
 const sorted=[...points].sort((a,b)=>String(a.kind).localeCompare(String(b.kind))||String(a.key).localeCompare(String(b.key))),groups=[];
 for(const p of sorted){if(!finite(p.x)||!finite(p.y)||p.x<0||p.x>1||p.y<0||p.y>1)continue;const x=p.x*view.width*view.zoom,y=p.y*view.height*view.zoom;
 let nearest=null,best=size;for(const g of groups){if(g.kind!==p.kind)continue;const d=Math.hypot(x-g.ax,y-g.ay);if(d<best){nearest=g;best=d;}}
 if(!nearest){nearest={key:p.kind+':'+p.key,kind:p.kind,ax:x,ay:y,x:0,y:0,points:[]};groups.push(nearest);}nearest.points.push(p);nearest.x+=p.x;nearest.y+=p.y;
 }return groups.map(g=>({key:g.key,x:g.x/g.points.length,y:g.y/g.points.length,points:g.points}));
}
function filterPoints(points,mode,{photos={},hasRequest=()=>false,landmarks=true}={}){
 if(!['all','missing','requests','off'].includes(mode))mode='all';
 return points.filter(p=>p.kind!=='pokemon'?landmarks:mode==='off'?false:mode==='missing'?collection(photos,p.pokemon).diamond<4:mode==='requests'?hasRequest(p.pokemon):true);
}
function bounds(points){const p=(points||[]).filter(vector);if(p.length<2)return null;const xs=p.map(p=>p[0]),zs=p.map(p=>p[2]);let x0=Math.min(...xs),x1=Math.max(...xs),z0=Math.min(...zs),z1=Math.max(...zs);const span=Math.max(x1-x0,z1-z0,1),pad=span*.12;x0-=pad;x1+=pad;z0-=pad;z1+=pad;const wide=x1-x0,tall=z1-z0;if(wide/tall<16/9){const extra=(tall*16/9-wide)/2;x0-=extra;x1+=extra;}else{const extra=(wide*9/16-tall)/2;z0-=extra;z1+=extra;}return{x0,x1,z0,z1};}
function point(world,b){return vector(world)&&b?{x:(world[0]-b.x0)/(b.x1-b.x0),y:(world[2]-b.z0)/(b.z1-b.z0)}:null;}
function route(v){const b=bounds(v?.routePoints);if(!b)return null;return{bounds:b,points:v.routePoints.filter(vector).map(p=>point(p,b)),player:point(v.world,b),direction:vector(v.forward)&&Math.hypot(v.forward[0],v.forward[2])>1e-4?{x:v.forward[0]/(b.x1-b.x0),y:v.forward[2]/(b.z1-b.z0)}:null};}
// Keeps only the portions actually travelled before a rail change. No clock-based position.
class RunTracker {
 constructor(){this.reset();}
 reset(){this.course='';this.run=null;this.last=null;this.parts=[];this.revision=(this.revision||0)+1;}
 observe(v,now){
  if(!frame(v,now,v?.course)||!Array.isArray(v.routePoints)||v.routePoints.length<2||v.routePoints.length>97||!v.routePoints.every(vector)||!vector(v.world)||typeof v.routeKey!=='string')return false;
  const run=Number.isSafeInteger(v.run)&&v.run>0?v.run:null;
  if(this.course!==v.course||(this.run!==null&&run!==null&&this.run!==run)||(this.last&&v.sampleAt-this.last.sampleAt>1000)){this.reset();this.course=v.course;this.run=run;}
  if(this.last&&v.sampleAt<=this.last.sampleAt)return false;
  if(this.last&&v.routeKey===this.last.routeKey&&this.last.distance>this.last.length*.8&&v.distance<v.length*.1){this.reset();this.course=v.course;this.run=run;}
  if(this.last&&v.routeKey!==this.last.routeKey){const old=this.last,end=clamp(old.distance/old.length,0,1),points=old.routePoints.filter((p,i)=>i/(old.routePoints.length-1)<end);points.push(old.world);if(points.length>=2)this.parts.push(points);if(this.parts.length>12)this.parts.shift();this.revision++;}
  this.last=v;return true;
 }
 view(v){const points=(v?.routePoints||[]).filter(vector),b=bounds([...this.parts.flat(),...points]);if(!b)return null;return{bounds:b,points:points.map(p=>point(p,b)),past:this.parts.map(ps=>ps.map(p=>point(p,b))),player:point(v.world,b),direction:vector(v.forward)&&Math.hypot(v.forward[0],v.forward[2])>1e-4?{x:v.forward[0]/(b.x1-b.x0),y:v.forward[2]/(b.z1-b.z0)}:null};}
}
// Rail identities are matched by length and directed 3-D geometry, never by elapsed time.
function distance3(a,b){return Math.hypot(a[0]-b[0],a[1]-b[1],a[2]-b[2]);}
function pathSample(path,d){
 const ps=path.points;if(!Array.isArray(ps)||ps.length<2||!ps.every(vector))return null;
 let travelled=0;for(let i=1;i<ps.length;i++){const length=distance3(ps[i-1],ps[i]);if(travelled+length>=d||i===ps.length-1){const t=length?clamp((d-travelled)/length,0,1):0;return ps[i-1].map((x,k)=>x+(ps[i][k]-x)*t);}travelled+=length;}return null;
}
function matchRail(v,catalog){
 if(!catalog||!Array.isArray(v?.routePoints)||v.routePoints.length<2||v.routePoints.length>97||!v.routePoints.every(vector)||!finite(v.length))return null;
 const candidates=[];
 for(const path of catalog.paths){
  if(Math.abs(path.length-v.length)>Math.max(.04,v.length*.0003))continue;
  const errors=v.routePoints.map((p,i)=>distance3(p,pathSample(path,path.length*i/(v.routePoints.length-1))||[Infinity,Infinity,Infinity]));
  const mean=errors.reduce((a,b)=>a+b,0)/errors.length,max=Math.max(...errors);
  if(mean<=Math.max(.3,Math.min(2,v.length*.01))&&max<=Math.max(1,Math.min(6,v.length*.035)))candidates.push({path,error:mean});
 }
 candidates.sort((a,b)=>a.error-b.error);return candidates.length===1||candidates.length>1&&candidates[1].error-candidates[0].error>.15?candidates[0].path:null;
}
function endOf(path){const forced=(path.edges||[]).filter(e=>!e.conditional&&finite(e.at)&&e.at>=0&&e.at<=path.length);return forced.length?Math.min(...forced.map(e=>e.at)):path.length;}
// Remaining length of the default continuation. Conditional forks are followed only after observed.
function remaining(catalog,id,d,seen=new Set()){
 const path=catalog?.paths.find(p=>p.id===id);if(!path||seen.has(id)||seen.size>=16)return null;seen=new Set(seen);seen.add(id);
 const end=endOf(path);if(d>end+.5)return null;
 const edges=(path.edges||[]).filter(e=>!e.conditional&&Math.abs(e.at-end)<.01),targets=[...new Set(edges.map(e=>e.to))];
 if(targets.length>1)return null;
 if(!targets.length)return Math.max(0,end-d);
 const rest=remaining(catalog,targets[0],0,seen);return rest==null?null:Math.max(0,end-d)+rest;
}
function validEdges(path){const end=endOf(path);return(path.edges||[]).filter(e=>finite(e.at)&&e.at>=0&&e.at<=end+.01);}
function canReach(catalog,target){const set=new Set([target]);let changed=true;while(changed){changed=false;for(const p of catalog.paths)if(!set.has(p.id)&&validEdges(p).some(e=>set.has(e.to))){set.add(p.id);changed=true;}}return set;}
function prefixes(catalog,target,limit=256){
 const found=[],relevant=canReach(catalog,target);let visited=0,cyclic=false,overflow=false;
 function walk(id,total,seen){if(++visited>limit){overflow=true;return;}if(!relevant.has(id))return;const path=catalog.paths.find(p=>p.id===id);if(!path)return;if(seen.has(id)){cyclic=true;return;}if(id===target)found.push(total);seen=new Set(seen);seen.add(id);
  for(const edge of validEdges(path))walk(edge.to,total+edge.at,seen);
 }walk(catalog.main,0,new Set());return cyclic||overflow||!found.length?null:[Math.min(...found),Math.max(...found)];
}
// Enumerate only graph-confirmed changes after the last measured distance. This also
// handles a short intervening rail missed between two native/UI observations.
function transitions(catalog,source,after,target,limit=256){
 const relevant=canReach(catalog,target),found=[];let visited=0,unsafe=false;
 function walk(id,minimum,total,parts,seen){if(++visited>limit){unsafe=true;return;}const path=catalog.paths.find(p=>p.id===id);if(!path)return;
  for(const edge of validEdges(path)){if(edge.at<minimum-.1||!relevant.has(edge.to))continue;const nextParts=[...parts,{id,to:edge.at}],distance=total+edge.at;
   if(edge.to===target){found.push({distance,parts:nextParts});continue;}
   if(seen.has(edge.to)){unsafe=true;continue;}walk(edge.to,0,distance,nextParts,new Set([...seen,edge.to]));
  }
 }walk(source,after,0,[],new Set([source]));return unsafe?null:found;
}
function calibrationPoint(calibration,d){
 const points=calibration?.anchors;if(!Array.isArray(points)||points.length<2||!finite(d))return null;if(d<points[0][0]&&d>=points[0][0]-.01)d=points[0][0];if(d>points.at(-1)[0]&&d<=points.at(-1)[0]+.1)d=points.at(-1)[0];
 let lo=1,hi=points.length-1;while(lo<hi){const mid=(lo+hi)>>1;if(points[mid][0]<d)lo=mid+1;else hi=mid;}const a=points[lo-1],b=points[lo];if(d<a[0]||d>b[0]||![...a,...b].every(finite)||b[0]<=a[0]||a.length!==3||b.length!==3)return null;const t=(d-a[0])/(b[0]-a[0]);const p={x:a[1]+(b[1]-a[1])*t,y:a[2]+(b[2]-a[2])*t};return p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1?p:null;
}
function trailBase(calibration,d,chunk=16){const a=calibration?.anchors;if(!a?.length||!finite(d))return null;let lo=0,hi=a.length;while(lo<hi){const mid=(lo+hi)>>1;if(a[mid][0]<=d)lo=mid+1;else hi=mid;}const index=Math.floor(Math.max(0,lo-1)/chunk)*chunk;return{index,distance:a[index][0]};}
function trailPath(calibration,to,from=0){
 if(!finite(to)||!finite(from)||from<0||to<from)return'';const a=calibration?.anchors||[],start=trailBase(calibration,from,1);if(!start)return'';let started=false,path='';const append=p=>{if(!p){started=false;return;}path+=(started?'L':'M')+(p.x*1600).toFixed(2)+' '+(p.y*900).toFixed(2);started=true;};
 append(calibrationPoint(calibration,from));for(let i=start.index+1;i<a.length;i++){const row=a[i];if(row[0]>to)break;append(finite(row[1])&&finite(row[2])?{x:row[1],y:row[2]}:null);}append(calibrationPoint(calibration,to));return path;
}
class CourseTracker {
 constructor(catalogs=[],calibrations=[]){this.catalogs=catalogs;this.calibrations=calibrations;this.reset();}
 reset(){this.course='';this.run=null;this.last=null;this.path=null;this.prefix=null;this.approximate=false;this.matchKey='';this.matchPath=null;this.past=[];this.revision=(this.revision||0)+1;}
 observe(v,now){
  if(!frame(v,now,v?.course)||!vector(v.world)||typeof v.routeKey!=='string'||!v.routeKey||!Number.isSafeInteger(v.run)||v.run<1||!Array.isArray(v.routePoints)||v.routePoints.length<2||v.routePoints.length>97||!v.routePoints.every(vector))return false;
  const catalog=this.catalogs.find(c=>c.id===v.course);if(!catalog)return false;
  if(this.last&&v.course===this.course&&v.run===this.run&&v.sampleAt<=this.last.sampleAt)return false;
  const backwards=this.last&&v.routeKey===this.last.routeKey&&v.distance<this.last.distance-.5;
  const loop=backwards&&this.path&&validEdges(this.path).some(e=>e.to===this.path.id&&e.at>=this.last.distance-.1);
  if(this.course!==v.course||this.run!==v.run||this.last&&v.sampleAt-this.last.sampleAt>1000||backwards&&!loop){this.reset();this.course=v.course;this.run=v.run;}
  const key=v.course+':'+v.routeKey+':'+v.length;if(key!==this.matchKey){this.matchKey=key;this.matchPath=matchRail(v,catalog);}const path=this.matchPath;
  if(!path){this.path=null;this.prefix=null;this.past=[];this.revision++;this.last=v;return true;}
  if(!this.path){this.prefix=prefixes(catalog,path.id);this.approximate=false;}
  else if(this.path.id!==path.id||loop){
   const choices=transitions(catalog,this.path.id,this.last.distance,path.id);
   if(choices?.length){
    const costs=choices.map(c=>c.distance);if(this.prefix)this.prefix=[this.prefix[0]+Math.min(...costs),this.prefix[1]+Math.max(...costs)];else this.prefix=prefixes(catalog,path.id);
    if(choices.length===1){for(const part of choices[0].parts){const calibration=this.calibrations.find(c=>c.course===this.course&&c.rail===part.id);if(calibration)this.past.push({calibration,from:0,to:part.to});}}
    else this.addMeasuredPast();
   }else if(choices===null){this.prefix=null;this.approximate=false;this.addMeasuredPast();}
   else{if(this.prefix)this.prefix=[this.prefix[0]+this.last.distance,this.prefix[1]+endOf(this.path)];else this.prefix=prefixes(catalog,path.id);this.approximate=true;this.addMeasuredPast();}
   if(this.past.length>12)this.past.splice(0,this.past.length-12);this.revision++;
  }
  this.course=v.course;this.run=v.run;this.path=path;this.last=v;return true;
 }
 addMeasuredPast(){const calibration=this.calibrations.find(c=>c.course===this.course&&c.rail===this.path.id);if(calibration)this.past.push({calibration,from:0,to:this.last.distance});}
 view(v,now){
  if(!frame(v,now,this.course)||!vector(v.world)||!this.path||!this.last||v.routeKey!==this.last.routeKey||v.sampleAt!==this.last.sampleAt)return null;
  const catalog=this.catalogs.find(c=>c.id===this.course),end=endOf(this.path),calibration=this.calibrations.find(c=>c.course===this.course&&c.rail===this.path.id);
  const sections=calibration?.sections||[{from:0,to:end,name:this.path.id===catalog.main?'Main Trail':'Alternate Trail'}],section=sections.find((s,i)=>v.distance>=s.from&&(v.distance<s.to||i===sections.length-1&&v.distance<=s.to+.1));
  const sectionProgress=section&&section.to>section.from?clamp((v.distance-section.from)/(section.to-section.from),0,1):null,left=remaining(catalog,this.path.id,v.distance);let overall=null;
  if(this.prefix&&left!=null){overall=this.prefix.map(p=>{const travelled=p+v.distance,total=travelled+left;return total>0?clamp(travelled/total,0,1):null;});if(overall.some(x=>x==null))overall=null;}
  if(this.calibrations.some(c=>c.course===this.course&&c.courseProgress===false))overall=null;
  const player=calibrationPoint(calibration,v.distance),next=calibrationPoint(calibration,Math.min(v.distance+.3,end)),before=calibrationPoint(calibration,Math.max(0,v.distance-.3));
  const direction=vector(v.forward)&&Math.hypot(v.forward[0],v.forward[2])>1e-4&&next&&before&&Math.hypot(next.x-before.x,next.y-before.y)>1e-6?{x:next.x-before.x,y:next.y-before.y}:null;
  return{path:this.path.id,section:section?.name||'Current Section',sectionProgress,overall,approximate:this.approximate,player,direction,calibration,past:this.past,pastRevision:this.revision,split:validEdges(this.path).some(e=>e.conditional&&e.at>v.distance),alternate:this.path.id!==catalog.main};
 }
}

function follow(x,y,view){return{x:clamp(view.width/2-x*view.width*view.zoom+view.width*(view.zoom-1)/2,-view.width*(view.zoom-1)/2,view.width*(view.zoom-1)/2),y:clamp(view.height/2-y*view.height*view.zoom+view.height*(view.zoom-1)/2,-view.height*(view.zoom-1)/2,view.height*(view.zoom-1)/2)};}
function cameraReading(v){
 if(!v||!vector(v.forward)||!vector(v.up)||!finite(v.fovY)||v.fovY<.1||v.fovY>2.8||!finite(v.aspect)||v.aspect<.5||v.aspect>3)return null;
 const dot=(a,b)=>a.reduce((sum,n,i)=>sum+n*b[i],0);
 if(Math.abs(dot(v.forward,v.forward)-1)>.02||Math.abs(dot(v.up,v.up)-1)>.02||Math.abs(dot(v.forward,v.up))>.1)return null;
 return{forward:[...v.forward],up:[...v.up],fovY:v.fovY,aspect:v.aspect};
}
// Render-camera rays projected through the same schematic transform as the route.
// Vehicle travel direction is deliberately not a fallback for the player's view.
function viewCone(v,calibration,now=Date.now()){
 const camera=cameraReading(v?.camera),matrix=calibration?.projection,m=matrix?.length===8?[matrix[0],matrix[1],matrix[4],matrix[5],matrix[6],matrix[7]]:matrix;
 if(!frame(v,now,calibration?.course)||!camera||!Array.isArray(matrix)||![6,8].includes(matrix.length)||!Array.isArray(m)||m.length!==6||!matrix.every(finite)||Math.abs(m[0]*m[3]-m[1]*m[2])<1e-6||!calibrationPoint(calibration,v.distance))return null;
 let angle=0;if(calibration.heading){const h=calibration.heading;let lo=1,hi=h.length-1;while(lo<hi){const mid=(lo+hi)>>1;if(h[mid][0]<v.distance)lo=mid+1;else hi=mid;}const a=h[lo-1],b=h[lo];if(!a||!b||![...a,...b].every(finite)||b[0]<=a[0])return null;angle=a[1]+(b[1]-a[1])*clamp((v.distance-a[0])/(b[0]-a[0]),0,1);}
 const f=camera.forward,u=camera.up;if(Math.hypot(f[0],f[2])<.25)return null;
 const right=[f[1]*u[2]-f[2]*u[1],f[2]*u[0]-f[0]*u[2],f[0]*u[1]-f[1]*u[0]],spread=Math.tan(camera.fovY/2)*camera.aspect;
 const project=a=>{const x=a[0]*m[0]+a[2]*m[2],y=a[0]*m[1]+a[2]*m[3];return{x:(x*Math.cos(angle)-y*Math.sin(angle))/960,y:(x*Math.sin(angle)+y*Math.cos(angle))/540};};
 const cone={center:project(f),left:project(f.map((n,i)=>n-right[i]*spread)),right:project(f.map((n,i)=>n+right[i]*spread))};
 return Object.values(cone).every(p=>Math.hypot(p.x,p.y)>1e-7)?cone:null;
}
function conePath(cone,width,height,radius=72){
 if(!cone||![width,height,radius].every(n=>finite(n)&&n>0))return'';
 const turn=Math.PI*2,angle=p=>Math.atan2(p.y*height,p.x*width),positive=n=>(n%turn+turn)%turn;
 if(!['left','right','center'].every(k=>cone[k]&&finite(cone[k].x)&&finite(cone[k].y)))return'';
 const l=angle(cone.left),r=angle(cone.right),c=angle(cone.center),sweep=positive(c-l)<=positive(r-l)?1:0,span=sweep?positive(r-l):positive(l-r);
 if(span<.01||span>Math.PI+.01)return'';
 const xy=a=>(Math.cos(a)*radius).toFixed(2)+' '+(Math.sin(a)*radius).toFixed(2);
 return'M0 0L'+xy(l)+'A'+radius+' '+radius+' 0 0 '+sweep+' '+xy(r)+'Z';
}
function sanitize(v){if(!v||typeof v!=='object')return{};const out={};for(const k of['status','course','courseId','level','distance','length','world','forward','routeKey','routePoints','sampleAt','phase','tools','lastAction','run'])if(k in v)out[k]=v[k];const camera=cameraReading(v.camera);if(camera)out.camera=camera;return out;}
return{trailPath,trailBase,cameraReading,viewCone,conePath,markerGroups,markerBucket,filterPoints,transitions,CourseTracker,matchRail,endOf,remaining,prefixes,calibrationPoint,RunTracker,finite,clamp,vector,frame,collection,clusters,bounds,point,route,follow,sanitize};
});
