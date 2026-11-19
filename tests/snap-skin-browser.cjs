'use strict';
// Optional browser acceptance: NODE_PATH=<Playwright modules> node --test tests/snap-skin-browser.cjs
const {test}=require('node:test'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const {chromium}=require('playwright');
const {PNG}=require('pngjs');
const options={headless:true,...(process.env.SNAP_BROWSER_EXECUTABLE?{executablePath:process.env.SNAP_BROWSER_EXECUTABLE}:{}),args:[]};
// A nonempty element box did not catch WebView's file-origin mask rejection.
// Compare painted pixels with/without the artwork under normal file restrictions.
async function paintedPixels(page,selector){
 const node=page.locator(selector),host=node.locator('..');
 const before=PNG.sync.read(await host.screenshot({animations:'disabled'}));
 await node.evaluate(e=>e.style.visibility='hidden');
 const after=PNG.sync.read(await host.screenshot({animations:'disabled'}));
 await node.evaluate(e=>e.style.removeProperty('visibility'));
 let changed=0;for(let i=0;i<before.data.length;i+=4)if([0,1,2].some(c=>Math.abs(before.data[i+c]-after.data[i+c])>20))changed++;
 return changed;
}
async function start(browser,width=540,height=470){
 const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:2,hasTouch:true});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('file://'+path.resolve('app/src/main/assets/ui/index.html'));
 await page.evaluate(()=>{
  window.SnapTouch={tap(){},mapVisible(){}};const model=SnapModel.create(SNAP_CONTENT);
  window.fixture={sourceKey:'ui-acceptance-fixture',connected:true,visible:true,phase:'research',course:'park-day',level:3,film:72,fps:60,playtime:{totalMs:28.2*3600000},syncedAt:Date.now(),photos:{'810:1':{score:4100,medal:'Diamond'},'810:2':{score:3800,medal:'Gold'},'810:3':{score:3200,medal:'Silver'},'780:3':{score:3846,medal:'Gold'}},moments:[{key:'780:3',score:3846}],received:[...model.requests.keys()],requestsKnown:true,completed:[],completionsKnown:true};
  SnapApp.update(fixture);
 });
 await page.evaluate(()=>document.fonts.ready);
 return {page,errors};
}
async function images(page,selector){
 await page.waitForFunction(selector=>[...document.querySelectorAll(selector)].every(e=>e.complete&&e.naturalWidth>0),selector);
}
async function fits(page,selector){
 return page.locator(selector).evaluateAll(es=>es.every(e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0&&r.x>=-1&&r.y>=-1&&r.right<=innerWidth+1&&r.bottom<=innerHeight+1}));
}
test('Live remains one screen, sprites fit, native feedback expires and Photo Book routes still work',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[620,540],[412,640]]){
   const {page,errors}=await start(browser,width,height);await images(page,'#content img');
   assert(await fits(page,'.scenic-course,.suggestion-card,.moments,#controller-strip'),width+'×'+height+' primary regions');
   assert(await page.locator('#content').evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Live must not need scrolling');
   assert(await page.locator('.scenic-film').isVisible());assert.equal(await page.locator('.scenic-film b').textContent(),'72');
   assert(await page.locator('#controller-keys').evaluate(e=>e.scrollWidth<=e.clientWidth+1),'All controls fit one row');
   assert(await page.evaluate(()=>document.fonts.check('20px SnapControls')));
   await page.evaluate(()=>SnapApp.controller({available:true,at:Date.now(),buttons:{A:true,R2:true,L3:true},axes:{lx:.8,ly:-.4}}));
   assert.equal(await page.locator('[data-pad-key].down').count(),3);
   assert(await page.locator('[data-pad-key=A] .pad-pressed').isVisible());
   assert(await page.locator('[data-stick=l]').evaluate(e=>e.classList.contains('tilted')));
   await page.evaluate(()=>SnapApp.controller({available:true,at:Date.now()-1600,buttons:{A:true},axes:{lx:1}}));
   assert.equal(await page.locator('[data-pad-key].down').count(),0,'Expired feedback must clear');
   assert.equal(await page.locator('.native-stick.tilted').count(),0);
   const first=await page.locator('.photo-book-route').getAttribute('data-id');
   await page.getByRole('button',{name:'Another Photo Suggestion',exact:true}).click();
   assert.notEqual(await page.locator('.photo-book-route').getAttribute('data-id'),first);
   await page.locator('.photo-book-route').click();assert(await page.locator('#detail').evaluate(e=>e.open));
   await page.locator('#detail [data-action=close]').click();assert(await page.locator('#detail').evaluate(e=>!e.open));
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{subject:810,category:2,photosTaken:15,photosSelected:4}}));
   assert.equal(await page.locator('.selection-reference').count(),4);
   assert(await fits(page,'.selection-head,.selection-ledger,.selection-footer'),'Photo Selection remains available at the same viewport');
   // Give a long, narrow cutout the suggestion card without changing app code.
   await page.evaluate(()=>{const model=SnapModel.create(SNAP_CONTENT),photos={};for(const p of model.data.species)if(p.id!==95)for(let n=1;n<=4;n++)photos[p.id+':'+n]={score:4200,medal:'Diamond'};SnapApp.update({...fixture,sourceKey:'long-silhouette-fixture',course:'',level:0,photos});});
   await images(page,'.suggestion-portrait');
   const portrait=await page.locator('.suggestion-portrait').evaluate(e=>{const a=e.getBoundingClientRect(),b=e.closest('.suggestion-print').getBoundingClientRect();return{inside:a.x>=b.x-6&&a.y>=b.y-6&&a.right<=b.right+6&&a.bottom<=b.bottom+6,loaded:e.naturalWidth>0};});
   assert(portrait.loaded&&portrait.inside,'Long portraits retain the whole print area');
   const photo='data:image/webp;base64,'+fs.readFileSync(path.resolve('app/src/main/assets/game-art/courses/stage_001_thumbnail.webp')).toString('base64');
   await page.evaluate(image=>SnapApp.update({...fixture,photos:{...fixture.photos,'780:3':{score:3846,medal:'Gold',image}}}),photo);
   await images(page,'.moment-print.has-photo img');
   const thumb=await page.locator('.moment-print.has-photo img').boundingBox();assert(thumb.width/thumb.height>1.4,width+'×'+height+' saved snapshot keeps a landscape photo area: '+thumb.width+'/'+thumb.height);

   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});
test('map assets, filter choices, progress and sighting routes remain usable with the new skin',async()=>{
 const browser=await chromium.launch(options);
 try{
  const {page,errors}=await start(browser);
  await page.evaluate(()=>{
   const c=SnapRouteData.courses.find(c=>c.id==='park-day'),p=c.paths.find(p=>p.id===c.main);
   const sample=d=>{let travelled=0;for(let i=1;i<p.points.length;i++){const a=p.points[i-1],b=p.points[i],n=Math.hypot(...a.map((v,k)=>b[k]-v));if(travelled+n>=d||i===p.points.length-1){const t=n?Math.max(0,Math.min(1,(d-travelled)/n)):0;return a.map((v,k)=>v+(b[k]-v)*t)}travelled+=n}};
   window.reading={status:'ready',phase:'paused',course:'park-day',courseId:1,level:3,run:1,routeKey:'ui-acceptance-rail',distance:30,length:p.length,world:sample(30),forward:[1,0,0],routePoints:Array.from({length:41},(_,i)=>sample(p.length*i/40)),camera:{forward:[1,0,0],up:[0,1,0],fovY:.628,aspect:1.77778}};
   window.feed=setInterval(()=>SnapMaps.live({...reading,sampleAt:Date.now()}),50);SnapMaps.open('park-day');
  });
  await page.waitForFunction(()=>!document.querySelector('.map-player').hidden);
  assert(await page.locator('.map-view-cone').isVisible());assert.notEqual(await page.locator('.map-section-progress strong').textContent(),'—');
  const scale=()=>page.locator('.map-board').evaluate(e=>new DOMMatrix(getComputedStyle(e).transform).a);
  await page.getByRole('button',{name:'Zoom In',exact:true}).click();const zoom=await scale();assert(zoom>1);
  await page.getByRole('button',{name:'Filters',exact:true}).click();
  assert(await page.locator('.map-layers').evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Filter note fits without scrolling at Thor size');
  for(const mode of ['missing','requests','all']){
   await page.locator('[data-map=pokemon-mode][data-value='+mode+']').click();
   assert.equal(await page.locator('[data-map=pokemon-mode][aria-checked=true]').count(),1);
   assert.equal(await scale(),zoom,'Layer changes retain user zoom');
  }
  await page.getByRole('button',{name:'Close Map Filters',exact:true}).click();
  await page.getByRole('button',{name:'Overview',exact:true}).click();
  // Programmatic pin activation avoids panning-dependent hit positions in this routing check.
  await page.locator('.map-pin').filter({has:page.locator('img[alt=Grookey]')}).first().evaluate(e=>e.click());
  const choose=page.locator('.map-pin-choices button').filter({hasText:'Grookey'});if(await choose.count())await choose.click();
  assert.equal(await page.locator('.map-sighting-copy h3').textContent(),'Grookey');
  assert.equal(await page.locator('.map-pin-stars .slot').count(),4);
  assert(await fits(page,'.map-pin-card,.map-pin-book'));
  await page.locator('.map-pin-book').click();assert(await page.locator('#detail').evaluate(e=>e.open));
  await page.locator('#detail [data-action=close]').click();await page.waitForFunction(()=>document.querySelector('#course-map').open);
  await page.evaluate(()=>clearInterval(feed));
  for(const id of await page.evaluate(()=>SnapMapData.courses.map(c=>c.id))){
   await page.evaluate(id=>SnapMaps.open(id),id);await images(page,'#course-map img');
   assert(await fits(page,'.map-close,.map-heading,.map-toolbar,.map-foot'),id+' header/controls fit');
  }
  await page.getByRole('button',{name:'Close Course Map',exact:true}).click();assert(await page.locator('#course-map').evaluate(e=>!e.open));
  assert.deepEqual(errors,[]);
 }finally{await browser.close();}
});
test('corrected controls keep inner artwork clear, playtime readable and NEO-ONE distinct',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   assert.equal(await page.locator('#playtime small').textContent(),'Playtime');
   for(const hours of [0,28.2,999.9,9999.9,10000,1200000]){
    await page.evaluate(hours=>SnapApp.update({...fixture,playtime:{totalMs:hours*3600000}}),hours);
    const bounds=await page.locator('#playtime').evaluate(e=>{const a=e.querySelector('.hours-value').getBoundingClientRect(),b=e.querySelector('b').getBoundingClientRect(),h=e.querySelector('em').getBoundingClientRect(),caption=e.querySelector('small'),c=caption.getBoundingClientRect();return{clear:b.left>=a.left&&h.right<=a.right&&b.top>=a.top&&b.bottom<=a.bottom&&h.top>=a.top&&h.bottom<=a.bottom,caption:caption.scrollWidth<=caption.clientWidth&&c.bottom<=innerHeight,screen:e.getBoundingClientRect().right<=innerWidth}});
    assert(bounds.clear&&bounds.caption&&bounds.screen,width+'px playtime '+hours+' fits its compact readout');
   }
   assert(await page.locator('#nav .tab-art').evaluateAll(es=>es.every(e=>getComputedStyle(e).backgroundImage==='none')));
   assert(await page.locator('#nav').evaluate(e=>parseFloat(getComputedStyle(e).columnGap)<=1));
   await page.evaluate(()=>SnapApp.update(fixture));
   const course=await page.locator('.scenic-course').evaluate(e=>{const a=e.getBoundingClientRect(),label=e.querySelector('.scenic-label').getBoundingClientRect();const button=e.querySelector('.scenic-map-button').getBoundingClientRect();return{label:label.left-a.left>=17&&label.top-a.top>=16,contents:[...e.querySelectorAll('.scenic-map-symbol,.map-expand')].every(i=>{const r=i.getBoundingClientRect();return r.left>=button.left+4&&r.right<=button.right-4&&r.top>=button.top+3&&r.bottom<=button.bottom-3})}});
   assert(course.label&&course.contents,'Course controls clear the viewfinder and their own edges');
   const book=await page.locator('.photo-book-route').evaluate(e=>{const b=e.getBoundingClientRect();return[...e.querySelectorAll('img,svg')].every(c=>{const r=c.getBoundingClientRect();return r.left>=b.left+4&&r.right<=b.right-4&&r.top>=b.top+3&&r.bottom<=b.bottom-3})});
   assert(book,'Photo Book emblem/arrow stays inside its frame');
   await page.evaluate(()=>SnapApp.controller({available:true,at:Date.now(),buttons:{L3:true},axes:{lx:1,ly:-1,rx:-1,ry:1}}));
   const controls=await page.locator('#controller-strip').evaluate(e=>{const r=e.getBoundingClientRect();return[...e.querySelectorAll('kbd')].every(k=>k.getBoundingClientRect().top>=r.top+6)&&[...e.querySelectorAll('.analog-cap')].every(k=>{const a=k.getBoundingClientRect(),b=k.parentElement.getBoundingClientRect();return a.left>=b.left&&a.right<=b.right&&a.top>=b.top&&a.bottom<=b.bottom})});
   assert(controls,'Controller row has upper clearance and bounded stick travel');
   await page.getByRole('button',{name:/Open .* Course Map/}).click();
   await page.getByRole('button',{name:'Zoom In',exact:true}).click();
   await page.getByRole('button',{name:'Filters',exact:true}).click();
   const symbols=await page.locator('.map-close,.map-layers header>button,.map-zoom button').evaluateAll(es=>es.map(e=>{const b=e.getBoundingClientRect(),i=e.querySelector('.ui-symbol').getBoundingClientRect();return{dx:i.x+i.width/2-b.x-b.width/2,dy:i.y+i.height/2-b.y-b.height/2,inside:i.left>b.left&&i.right<b.right&&i.top>b.top&&i.bottom<b.bottom}}));
   assert(symbols.every(s=>Math.abs(s.dx)<=.2&&Math.abs(s.dy)<=1&&s.inside),'Original glyph artwork is optically centered and inset: '+JSON.stringify(symbols));
   const choices=await page.locator('[data-map=pokemon-mode]').evaluateAll(es=>es.every(e=>{const b=e.getBoundingClientRect();return[...e.children].every(c=>{const r=c.getBoundingClientRect();return r.left>=b.left+3&&r.right<=b.right-3&&r.top>=b.top+2&&r.bottom<=b.bottom-2})}));
   assert(choices,'Filter artwork and checkmarks stay clear of every option edge');
   assert.deepEqual(errors,[]);await page.close();
  }
  const {page}=await start(browser);
  await page.evaluate(()=>{const c=SnapRouteData.courses.find(c=>c.id==='park-day'),p=c.paths.find(p=>p.id===c.main);const sample=d=>{let t=0;for(let i=1;i<p.points.length;i++){const a=p.points[i-1],b=p.points[i],n=Math.hypot(...a.map((v,k)=>b[k]-v));if(t+n>=d||i===p.points.length-1){const f=n?Math.max(0,Math.min(1,(d-t)/n)):0;return a.map((v,k)=>v+(b[k]-v)*f)}t+=n}};window.markerReading={status:'ready',phase:'paused',course:'park-day',level:3,run:1,routeKey:'visibility-fixture',distance:30,length:p.length,world:sample(30),forward:[1,0,0],routePoints:Array.from({length:41},(_,i)=>sample(p.length*i/40))};window.markerFeed=setInterval(()=>SnapMaps.live({...markerReading,sampleAt:Date.now()}),50);SnapMaps.open('park-day');});
  await page.waitForFunction(()=>!document.querySelector('.map-player').hidden);
  assert.equal(await page.locator('.neo-label').textContent(),'NEO-ONE');
  const size=await page.locator('.map-player').boundingBox();assert(size.width>=44);
  await page.getByRole('button',{name:'Zoom In',exact:true}).click();const zoomed=await page.locator('.map-player').boundingBox();assert.equal(zoomed.width,size.width,'Visibility marker stays fixed in screen pixels');
  assert(await page.locator('.map-player').evaluate(e=>getComputedStyle(e).animationName==='none'),'Visibility needs no continuous animation');
  await page.evaluate(()=>{clearInterval(markerFeed);SnapMaps.live({...markerReading,sampleAt:Date.now(),phase:'transition'})});
  assert(await page.locator('.map-player').isHidden());assert(await page.locator('.neo-label').isHidden(),'Transition also hides the new marker label');
 }finally{await browser.close();}
});

test('native close, zoom and stick artwork actually paints with file access restricted',async()=>{
 const browser=await chromium.launch(options);
 try{
  const {page,errors}=await start(browser);
  await page.evaluate(()=>SnapApp.controller({available:true,at:Date.now(),buttons:{},axes:{}}));
  for(const selector of ['[data-stick=l] .analog-cap','[data-stick=r] .analog-cap','[data-pad-key=SELECT] .pad-rest','[data-pad-key=START] .pad-rest'])assert(await paintedPixels(page,selector)>30,selector+' paints visible pixels');
  await page.getByRole('button',{name:/Open .* Course Map/}).click();
  await page.getByRole('button',{name:'Filters',exact:true}).click();
  for(const selector of ['.map-close .ui-symbol','.map-layers header>button .ui-symbol','[aria-label="Zoom In"] .ui-symbol','[aria-label="Zoom Out"] .ui-symbol'])assert(await paintedPixels(page,selector)>30,selector+' paints visible pixels');
  await page.getByRole('button',{name:'Close Course Map',exact:true}).click();
  await page.locator('.photo-book-route').click();
  assert(await paintedPixels(page,'#detail .close .ui-symbol')>30,'Photodex close paints visible pixels');
  await page.locator('#detail .close').click();
  await page.locator('.credits-button').click();
  assert(await paintedPixels(page,'#detail .close .ui-symbol')>30,'Credits close paints visible pixels');
  assert.deepEqual(errors,[]);
 }finally{await browser.close();}
});
