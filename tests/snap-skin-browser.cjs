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
   await images(page,'.category-saved-photo,.type-art');assert.equal(await page.locator('.category-saved-photo').count(),4,'Each category previews its registered photo');
   assert(await paintedPixels(page,'.book-turn.previous .turn-arrow .glyph')>10,'Separate native arrow button paints');
   const turns=await page.evaluate(()=>SNAP_CONTENT.species.map(p=>{SnapApp.openPokemon(p.id);return[...document.querySelectorAll('.book-turn')].every(e=>{const b=e.getBoundingClientRect(),a=e.querySelector('.turn-arrow').getBoundingClientRect(),c=e.querySelector('.turn-copy').getBoundingClientRect(),i=e.querySelector('.turn-portrait').getBoundingClientRect();return a.left>=b.left&&a.right<=b.right&&c.left>=b.left&&c.right<=b.right&&(e.classList.contains('next')?c.right<=i.left&&i.right<=a.left:a.right<=i.left&&i.right<=c.left)});}));assert(turns.every(Boolean),'All 234 entries keep arrow, portrait and text in separate footer areas');
   await page.evaluate(()=>SnapApp.openPokemon(666));await images(page,'.photo-canvas img');
   const body=()=>page.locator('#detail .book-body');assert(await body().evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Photodex photo spread is fixed');
   const canvas=await page.locator('.photo-canvas').boundingBox();assert(Math.abs(canvas.width/canvas.height-1.5)<.01,'Registered photo has its original 3:2 area');
   assert(await page.locator('.entry-head').evaluate(e=>{const b=e.getBoundingClientRect();return[...e.querySelectorAll('.entry-identity,.type-list')].every(c=>{const r=c.getBoundingClientRect();return r.top>=b.top&&r.bottom<=b.bottom})}),'Name and types clear the book header');
   assert(await page.locator('.photo-hint').evaluate(e=>{const a=e.getBoundingClientRect(),b=e.querySelector('[data-action=tips]').getBoundingClientRect();return b.top>=a.top&&b.bottom<=a.bottom}),'Photo Tips is inside its note');
   await page.locator('[data-action=tips]').click();assert(await page.locator('#photo-tip').evaluate(e=>e.open));assert(await page.locator('#photo-tip .photo-examples').evaluate(e=>e.scrollWidth<=e.clientWidth+1),'Photo Tip reference artwork also fits');await page.locator('[data-action=close-photo-tip]').click();
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
   await page.locator('.book-nav [data-id=photos]').click();assert(await page.locator('.photo-score-card.missing-photo').isVisible());assert(await page.locator('.photo-score-card.missing-photo').evaluate(e=>e.scrollHeight<=e.clientHeight+1),'Unregistered-photo caption does not overflow');await images(page,'.category-tabs img');assert(await page.locator('.category-print').evaluateAll(es=>es.every(e=>{const a=e.getBoundingClientRect(),b=e.parentElement.getBoundingClientRect();return a.width>=b.width-8&&a.height>=30})), 'Missing category placeholders keep full-width tracks');assert.equal(await page.locator('.category-stars img').count(),10);assert(await paintedPixels(page,'.category-tabs button:last-child .category-stars')>20,'Missing four-star category actually paints');assert(await page.locator('.photo-display').evaluate(e=>{const r=e.getBoundingClientRect(),canvas=e.querySelector('.photo-canvas').getBoundingClientRect(),page=e.parentElement.getBoundingClientRect();return r.width>=page.width-20&&canvas.width>=Math.min(r.width-4,(r.height-4)*1.5)-2}),'Missing photos use the full available photo pane');
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('LenTalk postcards retain course art, clear completion badges and full live-status solutions',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);await page.locator('#nav [data-id=requests]').click();
   const first=await page.locator('.talk-card').first().getAttribute('data-id');
   await page.evaluate(id=>SnapApp.update({...fixture,completed:[id]}),first);await images(page,'.talk-course-art,.talk-cutout');
   assert.equal(await page.locator('.talk-card').count(),234);
   assert.equal(await page.locator('.request-grid').evaluate(e=>getComputedStyle(e).gridTemplateColumns.split(' ').length),width>460?4:3);
   const art=await page.locator('.talk-art').evaluateAll(es=>es.every(e=>{const b=e.getBoundingClientRect(),p=e.querySelector('.talk-cutout').getBoundingClientRect();return Math.abs(b.width/b.height-16/9)<.02&&p.left>=b.left&&p.right<=b.right&&p.top>=b.top&&p.bottom<=b.bottom}));assert(art,'Landscape backgrounds and whole Pokémon cutouts fit every postcard');
   const card=page.locator('.talk-card').first();assert(await card.locator('.request-complete').isVisible());assert.equal(await card.locator('.request-complete').getAttribute('aria-label'),'Request Completed');
   assert(await card.evaluate(e=>e.querySelector('.talk-level-stamp').getBoundingClientRect().top>=e.querySelector('.talk-art').getBoundingClientRect().bottom),'Level stays clear of the corner ribbon');
   const pinned=await page.locator('.browse-tools').boundingBox();await page.locator('.browse-scroll').evaluate(e=>e.scrollTop=400);assert.deepEqual(await page.locator('.browse-tools').boundingBox(),pinned);
   await page.evaluate(id=>SnapApp.openRequest(id),first);assert.equal(await page.locator('.talk-head-actions .receipt-status').textContent(),'Completed');
   assert((await page.locator('#detail').boundingBox()).height<height*.8,'A one-step request shrinks to its content');
   assert(await paintedPixels(page,'#detail .close .ui-symbol')>30);
   await page.evaluate(()=>SnapApp.update(fixture));assert.equal(await page.locator('.talk-head-actions .receipt-status').textContent(),'In Progress');assert.equal(await page.locator('.talk-hero-image .request-complete').count(),0,'Completion changes update the open request');
   const longest=await page.evaluate(()=>SNAP_CONTENT.requests.slice().sort((a,b)=>b.solution.length-a.solution.length)[0]);await page.evaluate(id=>SnapApp.openRequest(id),longest.id);await images(page,'#detail img');
   assert.equal((await page.locator('.solution-steps p').allTextContents()).join(' ').replace(/\s+/g,' ').trim(),longest.solution.replace(/\s+/g,' ').trim(),'Every word of the full solution remains available');
   assert(await fits(page,'.talk-dialog-head,.talk-hero,.talk-reader'),'Long solutions stay inside the screen');assert(await page.locator('.talk-reader').evaluate(e=>e.scrollHeight>e.clientHeight),'Long solutions scroll inside their own reader');
   assert.equal(await page.locator('#detail a[href]').count(),0);
   const rewarded=await page.evaluate(()=>SNAP_CONTENT.requests.find(r=>r.rewardArt?.some(a=>a.image)));await page.evaluate(id=>SnapApp.openRequest(id),rewarded.id);await images(page,'.reward-asset');assert(await page.locator('.reward-asset').count()>0,'Native reward preview loads');
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('Guide keeps compact island courses, separate time badges and large four-category encounter cards',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[412,640]]){
   const {page,errors}=await start(browser,width,height);await page.locator('#nav [data-id=guide]').click();await images(page,'.course-row img,.island-section>h2>img');
   assert.equal(await page.locator('.course-row').count(),30);assert.equal(await page.locator('.island-section').count(),6);
   assert(await page.locator('.island-section').evaluateAll(es=>es.every(e=>getComputedStyle(e).gridTemplateColumns.split(' ').length===2)));
   const day=page.locator('.course-row[data-id=park-day]'),night=page.locator('.course-row[data-id=park-night]');assert((await day.textContent()).includes('(Day)'));assert((await night.textContent()).includes('(Night)'));assert.notEqual(await day.locator('.time-emblem img').getAttribute('src'),await night.locator('.time-emblem img').getAttribute('src'));
   await day.click();await images(page,'#detail img');assert.equal(await page.locator('.encounter-grid').evaluate(e=>getComputedStyle(e).gridTemplateColumns.split(' ').length),width>460?5:4);
   assert(await page.locator('.encounter .slot').evaluateAll(es=>es.every(e=>{const b=e.getBoundingClientRect(),s=e.querySelector('.star-cluster').getBoundingClientRect();return s.left>=b.left-.5&&s.right<=b.right+.5&&s.top>=b.top-.5&&s.bottom<=b.bottom+.5})),'Four star categories stay within each encounter');
   for(const n of [1,3,0]){await page.locator('[data-action=course-level][data-id="'+n+'"]').click();const expected=await page.evaluate(n=>SnapModel.create(SNAP_CONTENT).courseSpecies('park-day',n).length,n);assert.equal(await page.locator('.encounter').count(),expected);assert.equal(await page.locator('[data-action=course-level][data-id="'+n+'"]').getAttribute('aria-pressed'),'true');}
   await page.getByRole('button',{name:'Open Course Map',exact:true}).click();assert(await page.locator('#course-map').evaluate(e=>e.open));await page.getByRole('button',{name:'Close Course Map',exact:true}).click();
   await page.evaluate(()=>SnapApp.openCourse('park-day'));const name=await page.locator('.encounter strong').first().textContent();await page.locator('.encounter').first().click();assert.equal(await page.locator('.entry-identity h2').textContent(),name);
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('Live bounds developing snapshots, replaces them when ready, and keeps sync as a label',async()=>{
 const browser=await chromium.launch(options);
 try{
  const image='data:image/webp;base64,'+fs.readFileSync(path.resolve('app/src/main/assets/game-art/courses/stage_001_thumbnail.webp')).toString('base64');
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   assert.equal(await page.locator('#last-sync').evaluate(e=>e.tagName),'SPAN');assert.equal(await page.locator('#last-sync').getAttribute('data-action'),null);await page.locator('#last-sync').click();assert(await page.locator('#detail').evaluate(e=>!e.open));
   for(const id of [95,321,666]){
    await page.evaluate(id=>SnapApp.update({...fixture,moments:[{key:id+':1',score:4336}],photos:{}}),id);await images(page,'.moment-print img');
    assert(await page.locator('.moment-print').evaluate(e=>{const b=e.getBoundingClientRect(),p=e.querySelector('img').getBoundingClientRect();return getComputedStyle(e).overflow==='hidden'&&p.left>=b.left&&p.right<=b.right&&p.top>=b.top&&p.bottom<=b.bottom&&getComputedStyle(e.querySelector('img')).objectFit==='contain'}),'Developing portrait stays inside its print for '+id);
   }
   await page.evaluate(image=>SnapApp.update({...fixture,moments:[{key:'666:1',score:4336}],photos:{'666:1':{score:4336,medal:'Diamond',image}}}),image);await images(page,'.moment-print.has-photo img');assert.equal(await page.locator('.moment-print.has-photo img').getAttribute('src'),image,'Available saved photograph replaces the placeholder on the next update');
   await page.evaluate(image=>SnapApp.update({...fixture,moments:[{key:'666:1',score:4400}],photos:{'666:1':{score:4400,medal:'Diamond',image}}}),image);assert.equal((await page.locator('.snapshot-score b').textContent()).replace(',',''),'4400');
   assert(await fits(page,'.moment-print,.suggestion-print,.photo-book-route'));assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('Photo Tips and reviewed request expansions keep reference context, centered close artwork and every step',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);await page.evaluate(()=>SnapApp.openPokemon(666));await images(page,'.entry-head img');
   assert((await page.locator('.entry-head').boundingBox()).height<=46,'Shorter entry header leaves room for the photographs');
   assert(await page.locator('.entry-head').evaluate(e=>{const name=e.querySelector('.entry-identity').getBoundingClientRect(),types=e.querySelector('.type-list').getBoundingClientRect();return types.left>=name.right&&types.top>=e.getBoundingClientRect().top}),'Typing has its own header area');
   assert((await page.locator('.entry-spots .example-caption').textContent()).includes('not your saved'));
   await page.locator('[data-action=tips]').click();await images(page,'#photo-tip img');assert(await fits(page,'#photo-tip .tip-dialog-head,#photo-tip .tip-cover,#photo-tip .photo-tip-reader'));
   assert((await page.locator('#photo-tip .example-caption').textContent()).includes('location is not recorded'));assert.equal(await page.locator('#photo-tip a[href]').count(),0);
   const centered=selector=>page.locator(selector).evaluate(e=>{const b=e.getBoundingClientRect(),r=e.querySelector('.ui-symbol').getBoundingClientRect();return Math.abs(r.left+r.width/2-b.left-b.width/2)<.1&&Math.abs(r.top+r.height/2-b.top-b.height/2)<.1&&r.width<=b.width*.4;});
   assert(await centered('#photo-tip .close'));assert(await paintedPixels(page,'#photo-tip .close .ui-symbol')>10);await page.locator('[data-action=close-photo-tip]').click();assert(await centered('#detail .close'));await page.locator('#detail .close').click();
   const details=await page.evaluate(()=>Object.entries(SnapRequestDetails));for(const [id,note]of details){
    await page.evaluate(id=>SnapApp.openRequest(id),id);assert.equal(await page.locator('.request-walkthrough').getAttribute('open'),null,'Extra route steps are optional');
    const original=await page.evaluate(id=>SNAP_CONTENT.requests.find(r=>r.id===id).solution,id);assert.equal((await page.locator('.talk-solution>.solution-steps p').allTextContents()).join(' '),original.replace(/\s+/g,' ').trim());
    await page.locator('.request-walkthrough summary').click();assert.equal(await page.locator('.walkthrough-body .solution-steps p').count(),note.steps.length);assert.deepEqual(await page.locator('.walkthrough-body .solution-steps p').allTextContents(),note.steps);assert.equal(await page.locator('.walkthrough-shot b').textContent(),note.shot);assert(await fits(page,'.talk-reader'));assert.equal(await page.locator('#detail a[href]').count(),0);
   }
   const basic=await page.evaluate(()=>SNAP_CONTENT.requests.find(r=>!SnapRequestDetails[r.id]).id);await page.evaluate(id=>SnapApp.openRequest(id),basic);assert.equal(await page.locator('.request-walkthrough').count(),0,'No invented detailed route for unreviewed requests');assert(await centered('#detail .close'));assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('star-photo specifics follow the selected category and course, preserve steps and route out of Photo Tips',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   for(const [id,n]of [[666,4],[810,4],[813,3],[10,3],[346,3],[346,4],[810,1]]){
    await page.evaluate(([id,n])=>SnapApp.openPokemon(id,n),[id,n]);await page.locator('[data-action=tips]').click();
    const expected=await page.evaluate(([id,n])=>SnapModel.create(SNAP_CONTENT).photoMethods(SNAP_CONTENT.species.find(p=>p.id===id),n,SnapPhotoDetails,'park-day'),[id,n]);
    assert.equal(await page.locator('.photo-method').count(),expected.length);assert((await page.locator('.photo-methods>header').boundingBox()).height<=26,'Method heading stays compact');assert.equal(await page.locator('#photo-tip .tip-category .star-cluster img').count(),n,'The reader retains the selected star count');
    for(let i=0;i<expected.length;i++){
     const method=page.locator('.photo-method').nth(i),r=expected[i];assert((await method.locator('.photo-method-name b').textContent()).includes(r.title));assert.equal(await method.locator('.photo-method-level').count(),r.exampleLevel?1:0);
     assert.equal(await method.getAttribute('open'),null);await method.locator('summary').click();assert.deepEqual(await method.locator('.solution-steps p').allTextContents(),r.steps);if(r.shot)assert.equal(await method.locator('.photo-method-shot b').textContent(),r.shot);
     assert.equal(await method.locator('.photo-method-tools .tool-chip').count(),r.tools.length);assert(await fits(page,'#photo-tip .tip-dialog-head,#photo-tip .tip-cover,#photo-tip .photo-tip-reader'));
     await method.locator('summary').click();
    }
    assert.equal(await page.locator('#photo-tip a[href]').count(),0);await page.locator('[data-action=close-photo-tip]').click();
   }
   // A second valid photo method is not a second tab or a guessed source for a saved image.
   await page.evaluate(()=>{SnapApp.update({...fixture,course:'cave',level:2});SnapApp.openPokemon(666,4);});assert((await page.locator('.photo-hint-copy p').textContent()).includes('Jungle Pattern'));
   await page.locator('[data-action=tips]').click();assert.equal(await page.locator('.photo-method-name b').first().textContent(),'Jungle Pattern');assert.equal(await page.locator('.photo-method-name em').first().textContent(),'Current Course');
   await page.locator('.photo-method summary').first().click();await page.locator('.photo-method-guide').first().click();assert(await page.locator('#photo-tip').evaluate(e=>!e.open),'Course Guide closes the tip before routing');assert(await page.locator('#detail.course-reader').evaluate(e=>e.open));assert.equal(await page.locator('.course-identity h2').textContent(),'Outaway Cave');
   assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});

test('Photo Selection replaces subjects immediately, clears unknown references and updates its notice',async()=>{
 const browser=await chromium.launch(options);
 try{
  for(const [width,height]of [[540,470],[540,405],[412,640]]){
   const {page,errors}=await start(browser,width,height);
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{subject:810,category:1,photosTaken:12}}));assert.equal(await page.locator('.selection-identity h2').textContent(),'Grookey');assert.equal(await page.locator('.selection-reference.registered').count(),3);
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{subject:172,category:3,photosTaken:12}}));assert.equal(await page.locator('.selection-identity h2').textContent(),'Pichu');assert.equal(await page.locator('.selection-reference.registered').count(),0);assert.equal(await page.locator('.selection-reference.current').getAttribute('data-id'),'3');
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{}}));assert.equal(await page.locator('.selection-identity h2').textContent(),'Reading Selection');assert.equal(await page.locator('.selection-reference.registered,.selection-reference.current').count(),0);assert.equal(await page.locator('.selection-reference:disabled').count(),4);
   assert(await fits(page,'.selection-head,.selection-ledger,.selection-footer'));await page.locator('#nav [data-id=dex]').click();assert(await page.locator('#selection-notice').isVisible());assert.equal(await page.locator('#selection-notice small').count(),0);
   await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{subject:810,category:4}}));assert.equal(await page.locator('#selection-notice small').textContent(),'Grookey');await page.evaluate(()=>SnapApp.update({...fixture,phase:'selection',photoPhase:{subject:172}}));assert.equal(await page.locator('#selection-notice small').textContent(),'Pichu');
   await page.locator('#selection-notice').click();assert.equal(await page.locator('.selection-identity h2').textContent(),'Pichu');assert.equal(await page.locator('.selection-reference.current').count(),0);await page.evaluate(()=>SnapApp.update({...fixture,phase:'evaluation',photoPhase:{subject:172,category:4}}));assert.equal(await page.locator('.selection-desk').count(),0);assert.deepEqual(errors,[]);await page.close();
  }
 }finally{await browser.close();}
});
