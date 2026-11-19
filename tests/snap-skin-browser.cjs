'use strict';
// Optional browser acceptance: NODE_PATH=<Playwright modules> node --test tests/snap-skin-browser.cjs
const {test}=require('node:test'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const {chromium}=require('playwright');
const options={headless:true,...(process.env.SNAP_BROWSER_EXECUTABLE?{executablePath:process.env.SNAP_BROWSER_EXECUTABLE}:{}),args:['--allow-file-access-from-files']};
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
