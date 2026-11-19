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
