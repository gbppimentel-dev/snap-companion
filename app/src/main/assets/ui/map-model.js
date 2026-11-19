(function(root,factory){'use strict';const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;root.SnapMapModel=api;})(typeof window==='object'?window:globalThis,function(){'use strict';
const finite=v=>Number.isFinite(v),clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
function vector(v){return Array.isArray(v)&&v.length===3&&v.every(x=>finite(x)&&Math.abs(x)<1e6);}
function frame(v,now,course){return !!v&&v.status==='ready'&&['research','paused'].includes(v.phase)&&v.course===course&&finite(v.sampleAt)&&now>=v.sampleAt&&now-v.sampleAt<=1000&&finite(v.distance)&&finite(v.length)&&v.length>0&&v.distance>=0&&v.distance<=v.length+.1;}
function collection(photos,id){const medals=[1,2,3,4].map(n=>photos?.[id+':'+n]?.medal||'Missing');return{medals,found:medals.filter(m=>m!=='Missing').length,diamond:medals.filter(m=>m==='Diamond').length};}
function clusters(points,view,size=56){const groups=new Map();for(const p of points){const x=(p.x*view.width-view.width/2)*view.zoom+view.width/2+view.x,y=(p.y*view.height-view.height/2)*view.zoom+view.height/2+view.y;if(x< -size||y< -size||x>(view.clipWidth||view.width)+size||y>(view.clipHeight||view.height)+size)continue;const key=Math.floor(x/size)+':'+Math.floor(y/size);let g=groups.get(key);if(!g){g={x:0,y:0,points:[]};groups.set(key,g);}g.x+=x;g.y+=y;g.points.push(p);}return Array.from(groups.values(),g=>({x:g.x/g.points.length,y:g.y/g.points.length,points:g.points}));}
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
function prefixes(catalog,target,limit=64){
 const found=[];let visited=0,cyclic=false;
 function walk(id,total,seen){if(++visited>limit)return;const path=catalog.paths.find(p=>p.id===id);if(!path)return;if(seen.has(id)){cyclic=true;return;}if(id===target)found.push(total);seen=new Set(seen);seen.add(id);const end=endOf(path);for(const edge of path.edges||[])if(edge.at<=end+.01)walk(edge.to,total+edge.at,seen);}
 walk(catalog.main,0,new Set());return cyclic||visited>limit||!found.length?null:[Math.min(...found),Math.max(...found)];
}
function calibrationPoint(calibration,d){
 const points=calibration?.anchors;if(!Array.isArray(points)||points.length<2||!finite(d))return null;
 for(let i=1;i<points.length;i++){const a=points[i-1],b=points[i];if(d<a[0]||d>b[0])continue;if(![...a,...b].every(finite)||b[0]<=a[0]||a.length!==3||b.length!==3)return null;const t=(d-a[0])/(b[0]-a[0]);const p={x:a[1]+(b[1]-a[1])*t,y:a[2]+(b[2]-a[2])*t};return p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1?p:null;}return null;
}
class CourseTracker {
 constructor(catalogs=[],calibrations=[]){this.catalogs=catalogs;this.calibrations=calibrations;this.reset();}
 reset(){this.course='';this.run=null;this.last=null;this.path=null;this.prefix=null;this.approximate=false;this.matchKey='';this.matchPath=null;this.past=[];}
 observe(v,now){
  if(!frame(v,now,v?.course)||!vector(v.world)||typeof v.routeKey!=='string'||!v.routeKey||!Number.isSafeInteger(v.run)||v.run<1||!Array.isArray(v.routePoints)||v.routePoints.length<2||v.routePoints.length>97||!v.routePoints.every(vector))return false;
  const catalog=this.catalogs.find(c=>c.id===v.course);if(!catalog)return false;
  if(this.last&&v.course===this.course&&v.run===this.run&&v.sampleAt<=this.last.sampleAt)return false;
  if(this.course!==v.course||this.run!==v.run||this.last&&v.sampleAt-this.last.sampleAt>1000||this.last&&v.routeKey===this.last.routeKey&&v.distance<this.last.distance-.5){this.reset();this.course=v.course;this.run=v.run;}
  const key=v.course+':'+v.routeKey+':'+v.length;
  if(key!==this.matchKey){this.matchKey=key;this.matchPath=matchRail(v,catalog);}
  const path=this.matchPath;
  if(!path){this.path=null;this.prefix=null;this.last=v;return true;}
  if(!this.path){this.prefix=prefixes(catalog,path.id);this.approximate=false;}
  else if(this.path.id!==path.id){
   const edge=(this.path.edges||[]).filter(e=>e.to===path.id&&e.at>=this.last.distance-.5).sort((a,b)=>a.at-b.at)[0];
   if(this.prefix){const at=edge?.at??this.last.distance;this.prefix=this.prefix.map(n=>n+at);this.approximate=this.approximate||!edge;}
   else this.prefix=prefixes(catalog,path.id);
   const calibration=this.calibrations.find(c=>c.course===this.course&&c.rail===this.path.id);if(calibration)this.past.push({calibration,from:0,to:edge?.at??this.last.distance});
   if(this.past.length>12)this.past.shift();
  }
  this.course=v.course;this.run=v.run;this.path=path;this.last=v;return true;
 }
 view(v,now){
  if(!frame(v,now,this.course)||!vector(v.world)||!this.path||!this.last||v.routeKey!==this.last.routeKey||v.sampleAt!==this.last.sampleAt)return null;
  const catalog=this.catalogs.find(c=>c.id===this.course),end=endOf(this.path),calibration=this.calibrations.find(c=>c.course===this.course&&c.rail===this.path.id);
  const sections=calibration?.sections||[{from:0,to:end,name:this.path.id===catalog.main?'Main Trail':'Alternate Trail'}];
  const section=sections.find((s,i)=>v.distance>=s.from&&(v.distance<s.to||i===sections.length-1&&v.distance<=s.to+.1));
  const sectionProgress=section&&section.to>section.from?clamp((v.distance-section.from)/(section.to-section.from),0,1):null;
  const left=remaining(catalog,this.path.id,v.distance);let overall=null;
  if(this.prefix&&left!=null){overall=this.prefix.map(p=>{const travelled=p+v.distance,total=travelled+left;return total>0?clamp(travelled/total,0,1):null;});if(overall.some(x=>x==null))overall=null;}
  const player=calibrationPoint(calibration,v.distance),next=calibrationPoint(calibration,Math.min(v.distance+.3,end)),before=calibrationPoint(calibration,Math.max(0,v.distance-.3));
  const direction=vector(v.forward)&&Math.hypot(v.forward[0],v.forward[2])>1e-4&&next&&before&&Math.hypot(next.x-before.x,next.y-before.y)>1e-6?{x:next.x-before.x,y:next.y-before.y}:null;
  return{path:this.path.id,section:section?.name||'Current Section',sectionProgress,overall,approximate:this.approximate,player,direction,calibration,past:this.past,split:(this.path.edges||[]).some(e=>e.conditional&&e.at>v.distance),alternate:this.path.id!==catalog.main};
 }
}

function follow(x,y,view){return{x:clamp(view.width/2-x*view.width*view.zoom+view.width*(view.zoom-1)/2,-view.width*(view.zoom-1)/2,view.width*(view.zoom-1)/2),y:clamp(view.height/2-y*view.height*view.zoom+view.height*(view.zoom-1)/2,-view.height*(view.zoom-1)/2,view.height*(view.zoom-1)/2)};}
function sanitize(v){if(!v||typeof v!=='object')return{};const out={};for(const k of['status','course','courseId','level','distance','length','world','forward','routeKey','routePoints','sampleAt','phase','tools','lastAction','run'])if(k in v)out[k]=v[k];return out;}
return{CourseTracker,matchRail,endOf,remaining,prefixes,calibrationPoint,RunTracker,finite,clamp,vector,frame,collection,clusters,bounds,point,route,follow,sanitize};
});
