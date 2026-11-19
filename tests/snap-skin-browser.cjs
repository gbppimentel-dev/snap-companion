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

test('compact Photodex keeps clear star mounts, pinned search and a one-tap missing view',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[620,540],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   await page.evaluate(()=>{const photos={};for(let n=1;n<=4;n++)photos['666:'+n]={score:4300,medal:'Diamond'};photos['172:1']={score:3900,medal:'Gold'};SnapApp.update({...fixture,photos});});
   await page.locator('#nav [data-id=dex]').click();await page.evaluate(()=>document.fonts.ready);
   const density=await page.locator('.dex-grid').evaluate(e=>{const area=e.closest('.browse-scroll').getBoundingClientRect();return{columns:getComputedStyle(e).gridTemplateColumns.split(' ').length,height:e.firstElementChild.getBoundingClientRect().height,visible:[...e.children].filter(c=>{const r=c.getBoundingClientRect();return r.top>=area.top&&r.bottom<=area.bottom}).length}});
   assert(density.columns>=(width>=500?5:4),'Compact column count at '+width+': '+JSON.stringify(density));assert(density.height<=96);
   assert(density.visible>=(height>=470?10:5),'More complete entries visible: '+JSON.stringify(density));
   assert((await page.locator('.dex-totals').boundingBox()).height<=56,'Collection counts stay compact');
   const stars=await page.locator('.dex-card .slot').evaluateAll(es=>es.every(e=>{const b=e.getBoundingClientRect(),c=e.querySelector('.star-cluster').getBoundingClientRect();return c.left>=b.left-.5&&c.right<=b.right+.5&&c.top>=b.top-.5&&c.bottom<=b.bottom+.5}));assert(stars,'Every category cluster stays inside its own mount');
   const names=await page.locator('.dex-card h3').evaluateAll(es=>es.every(e=>e.scrollHeight<=e.clientHeight+1&&e.scrollWidth<=e.clientWidth+1));assert(names,'All Pokémon names fit their namecards');assert(await page.locator('.dex-card').evaluateAll(es=>es.every(e=>e.querySelector('.portrait').getBoundingClientRect().bottom<=e.querySelector('h3').getBoundingClientRect().top+1)),'Cutout image areas stay clear of the Pokémon names');assert(await page.locator('.dex-card').evaluateAll(es=>es.every(e=>e.querySelector('.portrait').getBoundingClientRect().bottom<=e.querySelector('.dex-number').getBoundingClientRect().top)),'All index badges are below the sprite area, including Taillow');assert.equal(await page.locator('.dex-card[data-id="276"] .dex-number').textContent(),'018');assert(await page.locator('.dex-complete .completion-medallion>img').evaluateAll(es=>es.every(e=>getComputedStyle(e).transform==='none')),'Completion crowns stay upright on their diagonal sashes');
   const tools=await page.locator('.browse-tools').boundingBox();await page.locator('.browse-scroll').evaluate(e=>e.scrollTop=500);assert.deepEqual(await page.locator('.browse-tools').boundingBox(),tools,'Search is outside the scrolling list');
   await page.locator('.missing-quick').click();assert.equal(await page.locator('.dex-card[data-id="666"]').count(),0);assert.equal(await page.locator('.dex-card[data-id="172"]').count(),1);assert.equal(await page.locator('.missing-quick').getAttribute('aria-pressed'),'true');
   await page.locator('.missing-quick').click();assert.equal(await page.locator('.dex-card[data-id="666"]').count(),1);
   await page.locator('#browse-search').fill('Grookey');await page.waitForFunction(()=>document.querySelectorAll('.dex-card').length===1);assert.equal(await page.locator('.dex-card h3').textContent(),'Grookey');
   await page.getByRole('button',{name:'Clear Search',exact:true}).click();assert.equal(await page.locator('.dex-card').count(),234);
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('shared filters fit the six islands and preserve draft, location, time and level choices',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);await page.locator('#nav [data-id=dex]').click();await page.locator('.compact-filter-trigger').click();
   assert.equal(await page.locator('[data-action=pick-island]').count(),6);
   assert(await page.locator('#filters .filter-options').evaluate(e=>e.scrollHeight<=e.clientHeight+1),'All six islands fit at '+width+'×'+height);
   assert(await paintedPixels(page,'#filters .close .ui-symbol')>30,'Native filter close paints');
   await page.getByRole('button',{name:'Use Current Course',exact:true}).click();assert.equal(await page.locator('[data-action=pick-course][data-id=park-day]').getAttribute('aria-pressed'),'true');assert.equal(await page.locator('[data-action=filter-level][data-id="3"]').getAttribute('aria-pressed'),'true');
   await page.locator('.filter-apply').click();assert((await page.locator('.active-filter').textContent()).includes('Day'));
   await page.locator('.compact-filter-trigger').click();await page.locator('#option-search').fill('Beach Night');
   assert.equal(await page.locator('[data-action=pick-course]').count(),1);await page.locator('[data-action=pick-course]').click();
   await page.getByRole('button',{name:'Cancel Filters',exact:true}).click();
   assert((await page.locator('.active-filter').textContent()).includes('Day'),'Cancel discards the new night-course draft');
   await page.locator('#nav [data-id=requests]').click();assert((await page.locator('.active-filter').first().textContent()).includes('Day'),'LenTalk shares the same location selection');
   await page.locator('.compact-filter-trigger').click();assert.equal(await page.locator('#filters .level-choices').count(),0);await page.locator('[data-action=filter-section][data-id=receipt]').click();assert.equal(await page.locator('#filters .filter-option').count(),5);await page.getByRole('button',{name:'Cancel Filters',exact:true}).click();
   await page.locator('#nav [data-id=dex]').click();await page.locator('.compact-filter-trigger').click();await page.locator('[data-action=reset-draft]').click();await page.locator('.filter-apply').click();assert.equal(await page.locator('.dex-card').count(),234);
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'camp',course:'',level:0}));await page.locator('.compact-filter-trigger').click();assert.equal(await page.locator('.course-current').count(),0,'Unknown course never supplies a guessed shortcut');await page.evaluate(()=>SnapApp.update(fixture));assert.equal(await page.locator('.course-current').count(),1,'A newly acquired course appears without reopening filters');await page.evaluate(()=>SnapApp.update({...fixture,visible:false}));assert.equal(await page.locator('.course-current').count(),0,'Background game context cannot supply a current-course shortcut');
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('Photodex entry keeps landscape photos and four official prints fixed, with grouped notes and loose appearances',async()=>{
 const browser=await chromium.launch(options);
 const photo='data:image/webp;base64,'+fs.readFileSync(path.resolve('app/src/main/assets/game-art/courses/stage_001_thumbnail.webp')).toString('base64');
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   await page.evaluate(image=>{const photos={};for(let n=1;n<=4;n++)photos['666:'+n]={score:4000+n*100,medal:['Bronze','Silver','Gold','Diamond'][n-1],image};window.bookFixture={...fixture,photos};SnapApp.update(bookFixture);},photo);
   await page.locator('#nav [data-id=dex]').click();await page.locator('.dex-card[data-id="666"]').click();await images(page,'.photo-canvas img');
   const body=()=>page.locator('#detail .book-body');assert(await body().evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Photodex photo spread is fixed');
   const canvas=await page.locator('.photo-canvas').boundingBox();assert(Math.abs(canvas.width/canvas.height-1.5)<.01,'Registered photo has its original 3:2 area');
   assert(await page.locator('.entry-head').evaluate(e=>{const b=e.getBoundingClientRect();return[...e.querySelectorAll('.entry-identity,.type-list')].every(c=>{const r=c.getBoundingClientRect();return r.top>=b.top&&r.bottom<=b.bottom})}),'Name and types clear the book header');
   assert(await page.locator('.photo-hint').evaluate(e=>{const a=e.getBoundingClientRect(),b=e.querySelector('[data-action=tips]').getBoundingClientRect();return b.top>=a.top&&b.bottom<=a.bottom}),'Photo Tips is inside its note');
   await page.locator('[data-action=tips]').click();assert(await page.locator('#photo-tip').evaluate(e=>e.open));assert(await page.locator('#photo-tip .reference-box').evaluate(e=>e.scrollWidth<=e.clientWidth+1),'Photo Tip reference artwork also fits');await page.locator('[data-action=close-photo-tip]').click();
   await page.locator('[data-action=category][data-id="4"]').click();assert.equal(await page.locator('.photo-canvas img').getAttribute('alt'),'Vivillon 4 Star Photo');
   await page.locator('.book-nav [data-id=album]').click();
   for(const count of [4,3,1]){
    await page.evaluate(count=>{const photos={};for(let n=1;n<=count;n++)photos['666:'+n]=bookFixture.photos['666:'+n];SnapApp.update({...bookFixture,photos});},count);await page.waitForTimeout(50);
    assert.equal(await page.locator('.album-print').count(),count);assert(await body().evaluate(e=>e.scrollHeight<=e.clientHeight+1),'My Photos remains fixed for '+count+' prints');
    const prints=await page.locator('.album-image').evaluateAll(es=>es.map(e=>{const r=e.getBoundingClientRect(),b=e.closest('.album-grid').getBoundingClientRect();return{ratio:r.width/r.height,inside:r.top>=b.top-1&&r.bottom<=b.bottom+1&&r.left>=b.left-1&&r.right<=b.right+1}}));assert(prints.every(p=>p.inside&&Math.abs(p.ratio-1.5)<.03),JSON.stringify(prints));
   }
   await page.locator('.album-print').click();assert(await page.locator('.album-expanded-image').isVisible());await page.locator('[data-action=album-back]').click();
   await page.locator('.book-nav [data-id=locations]').click();
   const grouping=await page.evaluate(()=>{const p=SNAP_CONTENT.species.find(p=>p.id===666),unique=new Set(p.locations.map(l=>l.course+':'+(l.form||''))),courses=new Set(p.locations.map(l=>l.course));return{moments:unique.size,courses:courses.size}});
   assert.equal(await page.locator('.about-place').count(),grouping.courses);assert.equal(await page.locator('.place-moment').count(),grouping.moments,'Grouping preserves every former location/form note');
   assert.equal(await page.locator('#detail a[href]').count(),0);await page.locator('.about-place summary').first().click();assert(await page.locator('.about-place[open] .place-moment-head').count()>0);
   await page.locator('.book-nav [data-id=appearances]').click();assert.equal(await page.locator('.loose-look').count(),20);assert(await page.locator('.loose-look').evaluateAll(es=>es.every(e=>getComputedStyle(e).borderTopWidth==='0px')),'Appearance cutouts stay free of card containers');
   const next=await page.locator('.book-turn.next b').textContent();await page.locator('.book-turn.next').click();assert.equal(await page.locator('.entry-identity h2').textContent(),next);
   await page.locator('.book-nav [data-id=photos]').click();assert(await page.locator('.photo-score-card.missing-photo').isVisible());assert(await page.locator('.photo-score-card.missing-photo').evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Unregistered-photo caption does not overflow');
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});
