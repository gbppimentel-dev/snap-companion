'use strict';
// Optional real-browser regression: NODE_PATH=<playwright modules> node --test tests/map-browser.cjs
const {test}=require('node:test'),assert=require('node:assert/strict'),path=require('node:path');
const {chromium}=require('playwright');
test('map retains user zoom through missing/recovered readings and renders portraits without ancestor rescaling',async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.SNAP_BROWSER_EXECUTABLE?{executablePath:process.env.SNAP_BROWSER_EXECUTABLE}:{}),args:['--allow-file-access-from-files']});
 try{
  const page=await browser.newPage({viewport:{width:540,height:470},deviceScaleFactor:2,hasTouch:true});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('file://'+path.resolve('app/src/main/assets/ui/index.html'));
  await page.evaluate(()=>{window.SnapTouch={tap(){},mapVisible(){}};window.snapshot={connected:true,visible:true,sourceKey:'browser-fixture',phase:'research',course:'park-day',level:2,photos:{},received:[],completed:[],requestsKnown:true,completionsKnown:true,playtime:{},gameReading:{}};SnapApp.update(snapshot);SnapMaps.open('park-day');});
  await page.waitForFunction(()=>document.querySelectorAll('.map-pin').length>0);
  await page.getByRole('button',{name:'Zoom In',exact:true}).tap();await page.getByRole('button',{name:'Zoom In',exact:true}).tap();
  const scale=()=>page.locator('.map-board').evaluate(e=>new DOMMatrix(getComputedStyle(e).transform).a);const zoom=await scale();assert(zoom>2);
  await page.evaluate(()=>{window.originalBoard=document.querySelector('.map-board');SnapApp.update({...snapshot,course:'',gameReading:{status:'unavailable'}});SnapApp.update(snapshot);});
  assert.equal(await scale(),zoom,'A temporary reading gap must not reset zoom');assert(await page.evaluate(()=>document.querySelector('.map-board')===originalBoard),'A recovered reading must not rebuild the same course');
  await page.waitForFunction(()=>[...document.querySelectorAll('.map-pin img')].every(e=>e.complete&&e.naturalWidth>0));
  const pins=await page.locator('.map-pin').evaluateAll(es=>es.map(e=>{const r=e.getBoundingClientRect(),a=e.parentElement.getBoundingClientRect();let scaled=false;for(let p=e.parentElement;p;p=p.parentElement){const m=new DOMMatrix(getComputedStyle(p).transform);if(Math.abs(m.a-1)>.001||Math.abs(m.d-1)>.001)scaled=true;}return{width:r.width,height:r.height,dx:r.x+r.width/2-a.x,dy:r.y+r.height/2-a.y,scaled};}));
  assert(pins.every(p=>p.width<=35.1&&p.height<=35.1&&Math.abs(p.dx)<.1&&Math.abs(p.dy)<.1&&!p.scaled),'Portrait pins must remain sharp fixed-size screen overlays at map zoom');
  assert(await page.locator('.map-pin img').evaluateAll(es=>es.every(e=>e.naturalWidth>=e.getBoundingClientRect().width*devicePixelRatio&&e.naturalHeight>=e.getBoundingClientRect().height*devicePixelRatio)),'Portrait sources must supply native display resolution');
  await page.evaluate(()=>SnapApp.update({...snapshot,course:'park-night'}));assert.equal(await scale(),1,'A different course deliberately starts at its overview');assert.deepEqual(errors,[]);
 }finally{await browser.close();}
});

test('NEO-ONE remains visible through owned events and a bounded handoff without drifting or losing zoom',async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.SNAP_BROWSER_EXECUTABLE?{executablePath:process.env.SNAP_BROWSER_EXECUTABLE}:{}),args:[]});
 try{
  const page=await browser.newPage({viewport:{width:540,height:470},deviceScaleFactor:2});const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.clock.install();
  await page.goto('file://'+path.resolve('app/src/main/assets/ui/index.html'));
  await page.evaluate(()=>{
   window.SnapTouch={tap(){},mapVisible(){}};const c=SnapRouteData.courses.find(c=>c.id==='park-day'),p=c.paths.find(p=>p.id===c.main);
   const sample=d=>{let t=0;for(let i=1;i<p.points.length;i++){const a=p.points[i-1],b=p.points[i],n=Math.hypot(...a.map((v,k)=>b[k]-v));if(t+n>=d||i===p.points.length-1){const f=n?Math.max(0,Math.min(1,(d-t)/n)):0;return a.map((v,k)=>v+(b[k]-v)*f)}t+=n}};
   window.row={status:'ready',phase:'research',course:'park-day',level:3,run:1,routeKey:'handoff-fixture',distance:30,length:p.length,routePoints:Array.from({length:41},(_,i)=>sample(p.length*i/40)),sampleAt:Date.now()};
   window.snapshot={sourceKey:'handoff',connected:true,visible:true,phase:'research',course:'park-day',level:3,photos:{},playtime:{},gameReading:row};SnapApp.update(snapshot);SnapMaps.open('park-day');
  });
  await page.waitForFunction(()=>!document.querySelector('.map-player').hidden);await page.getByRole('button',{name:'Zoom In',exact:true}).click();
  const scale=await page.locator('.map-board').evaluate(e=>new DOMMatrix(getComputedStyle(e).transform).a);
  await page.evaluate(()=>SnapMaps.live({...row,phase:'event',distance:31,sampleAt:Date.now()}));assert(await page.locator('.map-player').isVisible());assert(await page.locator('.map-view-cone').isHidden());
  const measured=await page.locator('.map-player').evaluate(e=>({left:e.style.left,top:e.style.top}));
  await page.evaluate(()=>SnapMaps.live({}));assert(await page.locator('.map-player.last-position').isVisible());assert.equal(await page.locator('.map-position-note').textContent(),'Last Position · Reconnecting');
  await page.clock.fastForward(1500);assert.deepEqual(await page.locator('.map-player').evaluate(e=>({left:e.style.left,top:e.style.top})),measured,'The held marker never advances from time alone');
  await page.evaluate(()=>SnapMaps.live({...row,phase:'event',distance:32,sampleAt:Date.now()}));assert(await page.locator('.map-player').isVisible());assert.equal(await page.locator('.map-player.last-position').count(),0);
  await page.evaluate(()=>SnapMaps.live({}));await page.clock.fastForward(3000);assert(await page.locator('.map-player').isHidden());assert.equal(await page.locator('.map-board').evaluate(e=>new DOMMatrix(getComputedStyle(e).transform).a),scale);
  await page.evaluate(()=>{SnapMaps.live({...row,sampleAt:Date.now()});SnapMaps.live({status:'idle'});});assert(await page.locator('.map-player').isHidden());
  await page.evaluate(()=>{SnapMaps.live({...row,sampleAt:Date.now()});SnapApp.update({...snapshot,sourceKey:'new-process',gameReading:{}});});assert(await page.locator('.map-player').isHidden());assert.deepEqual(errors,[]);
 }finally{await browser.close();}
});
