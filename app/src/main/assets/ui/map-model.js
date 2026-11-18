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
function follow(x,y,view){return{x:clamp(view.width/2-x*view.width*view.zoom+view.width*(view.zoom-1)/2,-view.width*(view.zoom-1)/2,view.width*(view.zoom-1)/2),y:clamp(view.height/2-y*view.height*view.zoom+view.height*(view.zoom-1)/2,-view.height*(view.zoom-1)/2,view.height*(view.zoom-1)/2)};}
function sanitize(v){if(!v||typeof v!=='object')return{};const out={};for(const k of['status','course','courseId','level','distance','length','world','forward','routeKey','routePoints','sampleAt','phase','tools','lastAction','run'])if(k in v)out[k]=v[k];return out;}
return{RunTracker,finite,clamp,vector,frame,collection,clusters,bounds,point,route,follow,sanitize};
});
