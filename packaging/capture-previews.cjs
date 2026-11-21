'use strict';
// Offline documentation previews. All progress/telemetry is invented below.
const {chromium}=require('playwright'),path=require('node:path');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.SNAP_BROWSER_EXECUTABLE?{executablePath:process.env.SNAP_BROWSER_EXECUTABLE}:{})});
 try{
  const page=await browser.newPage({viewport:{width:620,height:540},deviceScaleFactor:2});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/ui/index.html'));
  await page.evaluate(()=>{
   window.SnapTouch={tap(){},mapVisible(){}};
   const photos={};SNAP_CONTENT.species.slice(0,70).forEach((p,i)=>{for(let n=1;n<=4;n++)if((i+n)%4!==0)photos[p.id+':'+n]={score:2600+((i*97+n*211)%1900),medal:['Bronze','Silver','Gold','Diamond'][(i+n)%4]};});
   Object.assign(photos,{'810:1':{score:4100,medal:'Diamond'},'810:2':{score:3800,medal:'Gold'},'810:3':{score:3200,medal:'Silver'},'780:3':{score:3846,medal:'Gold'}});
   window.example={sourceKey:'documentation-only',connected:true,visible:true,phase:'research',course:'park-day',level:3,film:60,fps:60,playtime:{totalMs:28.2*3600000},battery:{percent:72,charging:false},syncedAt:Date.now(),photos,moments:[{key:'780:3',score:3846}],received:SNAP_CONTENT.requests.slice(0,95).map(r=>r.id),requestsKnown:true,completed:SNAP_CONTENT.requests.slice(0,22).map(r=>r.id),completionsKnown:true};SnapApp.update(example);
  });
  await page.evaluate(()=>document.fonts.ready);
  const shot=async name=>{await page.evaluate(()=>document.activeElement?.blur());await page.evaluate(()=>Promise.all([...document.images].filter(im=>{const r=im.getBoundingClientRect();return r.width>0&&r.height>0&&r.top<innerHeight&&r.bottom>0;}).map(im=>im.decode().catch(()=>{}))));await page.screenshot({path:path.resolve(__dirname,'../docs/images/'+name+'.png')});};
  await shot('overview');
  await page.evaluate(()=>{
   const c=SnapRouteData.courses.find(c=>c.id==='park-day'),p=c.paths.find(p=>p.id===c.main);
   const sample=d=>{let t=0;for(let i=1;i<p.points.length;i++){const a=p.points[i-1],b=p.points[i],n=Math.hypot(...a.map((v,k)=>b[k]-v));if(t+n>=d||i===p.points.length-1){const f=n?Math.max(0,Math.min(1,(d-t)/n)):0;return a.map((v,k)=>v+(b[k]-v)*f);}t+=n;}};
   window.mapExample={status:'ready',phase:'research',course:'park-day',level:3,run:1,routeKey:'documentation-simulated-reading',distance:30,length:p.length,routePoints:Array.from({length:41},(_,i)=>sample(p.length*i/40)),camera:{forward:[1,0,0],up:[0,1,0],fovY:.628,aspect:16/9}};
   SnapApp.update({...example,gameReading:{...mapExample,sampleAt:Date.now()}});SnapMaps.open('park-day');window.previewFeed=setInterval(()=>SnapMaps.live({...mapExample,sampleAt:Date.now()}),100);
  });
  await page.waitForFunction(()=>!document.querySelector('.map-player').hidden);await shot('course-map');await page.evaluate(()=>{clearInterval(previewFeed);SnapMaps.back();});
  await page.locator('#nav [data-id=dex]').click();await shot('photodex');
  await page.evaluate(()=>SnapApp.openPokemon(172,3));await page.locator('[data-action=tips]').click();await shot('photo-tips');await page.locator('[data-action=close-photo-tip]').click();await page.locator('#detail [data-action=close]').click();
  await page.locator('#nav [data-id=requests]').click();await shot('lentalk');
  await page.locator('#nav [data-id=guide]').click();await shot('guide');
  if(errors.length)throw new Error(errors.join('\n'));console.log('Six current UI previews created from invented offline fixtures.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
