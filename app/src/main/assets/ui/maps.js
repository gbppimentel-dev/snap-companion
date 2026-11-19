(function(){'use strict';
const MM=SnapMapModel,DATA=SnapMapData,A='../game-art/';
let api,dialog,cid='',follow=true,autoCourse=true,zoom=1,pan={x:0,y:0},frame={},selected=null,returnToMap=false;
let resizeObserver,gesture=new Map(),drag=null,gestureMoved=false,lastMove=0,lastLiveCourse='',visibility=true;
let layers={pokemon:'all',landmarks:true,routes:true},markerKey='',markerRevision=0,groups=new Map(),trailKey='',trailDistance=-1,pastKey='';
const tracker=new MM.CourseTracker(SnapRouteData.courses,SnapRouteCalibration);
try{const saved=JSON.parse(localStorage.getItem('snap-map-preferences')||'{}').layers||{};
 if(typeof saved.pokemon==='string'&&['all','missing','requests','off'].includes(saved.pokemon))layers.pokemon=saved.pokemon;
 else if(typeof saved.pokemon==='boolean')layers.pokemon=saved.requests?'requests':saved.missing?'missing':saved.pokemon?'all':'off';
 for(const key of ['landmarks','routes'])if(typeof saved[key]==='boolean')layers[key]=saved[key];
}catch{}
const $=s=>dialog?.querySelector(s),e=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const art=(p,cls='',alt='')=>`<img src="${e(p)}" class="${cls}" alt="${e(alt)}" draggable="false">`,icon=(n,cls='')=>art(A+'ui/'+n+'.webp',cls);
function svg(kind){const paths={close:'M6 6l12 12M18 6 6 18',plus:'M12 5v14M5 12h14',minus:'M5 12h14',fit:'M8 3H3v5m13-5h5v5M3 16v5h5m8 0h5v-5',layers:'m3 8 9-5 9 5-9 5zm0 5 9 5 9-5M3 18l9 5 9-5',locate:'M12 2v4m0 12v4M2 12h4m12 0h4M12 6a6 6 0 1 0 0 12 6 6 0 0 0 0-12',route:'M5 20V9a4 4 0 0 1 8 0v7a3 3 0 0 0 6 0V4m-3 3 3-3 3 3',right:'m9 5 7 7-7 7',check:'m5 12 4 4L19 6',pin:'M12 22S4 15 4 9a8 8 0 0 1 16 0c0 6-8 13-8 13zm0-17a4 4 0 1 0 0 8 4 4 0 0 0 0-8',hidden:'m3 3 18 18M10 5a12 12 0 0 1 11 7 15 15 0 0 1-4 5M6 6a15 15 0 0 0-3 6c4 8 11 8 15 5M9 9a4 4 0 0 0 6 6'};
 return`<svg viewBox="0 0 24 24" class="map-glyph" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[kind]||paths.layers}"/></svg>`;
}
function data(){return DATA.courses.find(c=>c.id===cid);}
function course(){return api?.model.courses.get(cid);}
function state(){return api?.state()||{};}
function visible(){return !!dialog?.open;}
function remember(){try{localStorage.setItem('snap-map-preferences',JSON.stringify({schema:2,layers}));}catch{}}
function notify(on){try{window.SnapTouch?.mapVisible(on);}catch{}}
function orbArt(){return 'tool-orb-'+({florio:'blue',belusylva:'pink',maricopia:'purple',voluca:'yellow',durice:'green',aurus:'brown'}[course()?.island]||'blue');}
function pending(id){return api.model.pendingRequests(state(),{course:cid,species:id||0});}
function portrait(id){const p=api.model.species.get(id);return p?art(A+(p.gridPortrait||p.appearances[0]?.image||p.portrait),'map-mon',p.name):icon('camera');}
function setMapCourse(id,preserve=false){if(!DATA.courses.some(c=>c.id===id))return false;
 if(cid!==id||!preserve){zoom=1;pan={x:0,y:0};selected=null;}cid=id;markerKey='';return true;
}
function init(options){api=options;dialog=document.createElement('dialog');dialog.id='course-map';dialog.className='course-map-dialog';dialog.setAttribute('aria-label','Course Map');document.body.append(dialog);
 dialog.addEventListener('cancel',ev=>{ev.preventDefault();back();});dialog.addEventListener('click',click);
 document.getElementById('detail').addEventListener('close',()=>{if(returnToMap){returnToMap=false;queueMicrotask(()=>open(cid,true));}});
}
function open(id,preserve=false){if(!setMapCourse(id,preserve))return;if(!preserve){follow=id===state().course;autoCourse=follow;}
 lastLiveCourse=state().course||'';render();if(!dialog.open)dialog.showModal();notify(true);requestAnimationFrame(()=>{measure();paint();});
}
function close(){if(!visible())return;dialog.close();notify(false);gesture.clear();drag=null;resizeObserver?.disconnect();}
function back(){if(!visible())return false;if($('.map-layers')?.classList.contains('open')){toggleLayers(false);return true;}if(selected){selected=null;pin();return true;}close();return true;}
function render(){const c=course(),d=data();if(!c||!d)return;markerKey='';trailKey='';pastKey='';trailDistance=-1;
 dialog.innerHTML=`<header class="map-head"><span class="map-title-icon">${icon('research')}</span><div class="map-heading"><span>${e(c.courseName||c.name)}</span><h2>${e(c.variant==='Any Time'?'Research Map':c.variant)}</h2></div><span class="map-course-coverage"></span><span class="map-level"></span><button class="map-close" data-map="close" aria-label="Close Course Map">${svg('close')}</button></header>
 <section class="map-stage ${c.variant==='Night'?'night-map':''}" aria-label="Illustrated course map with live route position">
 <div class="map-surface"><div class="map-board">${art(d.maps.at(-1).asset,'map-terrain','')}<div class="map-route-art">${d.routes.map(im=>art(im.asset)).join('')}</div>
 ${(d.starts||[]).map(p=>`<span class="map-start-label" style="left:${p.x*100}%;top:${p.y*100}%">Start</span>`).join('')}
 <svg class="map-live-route" viewBox="0 0 1600 900" aria-hidden="true"><g class="route-past"></g><path class="route-travelled"/></svg><div class="map-marker-layer"></div></div>
 <svg class="map-view-cone" viewBox="-76 -76 152 152" aria-label="Camera View" hidden><defs><radialGradient id="camera-fade"><stop offset="0" stop-color="#fff7a5" stop-opacity=".65"/><stop offset=".65" stop-color="#fff7a5" stop-opacity=".32"/><stop offset="1" stop-color="#fff7a5" stop-opacity="0"/></radialGradient></defs><path fill="url(#camera-fade)" stroke="#fff9b9" stroke-opacity=".4" stroke-width=".8"/></svg><div class="map-player" hidden>${icon('research')}<i></i></div></div>
 <div class="map-toolbar" role="toolbar" aria-label="Map Controls"><button class="map-layer-button" data-map="layers" aria-expanded="false">${svg('layers')}<span>Filters</span><i class="map-filter-dot" hidden></i></button>
 <div class="map-navigation"><button data-map="fit">${svg('fit')}<span>Overview</span></button><button data-map="follow" class="map-follow" aria-pressed="false">${svg('locate')}<span>Follow</span></button></div>
 <div class="map-zoom"><button data-map="zoom" data-value="-1" aria-label="Zoom Out">${svg('minus')}</button><output class="map-scale" aria-label="Map Zoom">100%</output><button data-map="zoom" data-value="1" aria-label="Zoom In">${svg('plus')}</button></div></div>
 <aside class="map-layers" aria-label="Map Filters"><header><h3>On the Map</h3><button data-map="layers" aria-label="Close Map Filters">${svg('close')}</button></header>
 <h4>Pokémon</h4><div class="map-pokemon-modes" role="radiogroup" aria-label="Pokémon to Show">${[['all','All Pokémon','Every recorded sighting','photodex'],['missing','Missing Diamonds','Photos still to find','star-diamond'],['requests','LenTalk','Requests to finish','requests'],['off','Hidden','A clear route map','']].map(([id,label,desc,img])=>`<button data-map="pokemon-mode" data-value="${id}" role="radio" aria-checked="${layers.pokemon===id}">${img?icon(img):svg('hidden')}<span><b>${label}</b><small>${desc}</small></span><i>${svg('check')}</i></button>`).join('')}</div>
 <h4>Map Details</h4><div class="map-detail-modes">${[['landmarks','Landmarks','tool-scan'],['routes','Routes','research']].map(([id,label,img])=>`<button class="map-layer-choice" data-map="layer" data-value="${id}" role="switch" aria-checked="${layers[id]}">${icon(img)}<span>${label}</span><i>${svg('check')}</i></button>`).join('')}</div>
 </aside><div class="map-pin-card" hidden></div><span class="map-position-note" hidden></span></section>
 <footer class="map-foot"><div class="map-journey"><div class="map-progress map-section-progress"><span><b class="map-section-name">Current Section</b><strong>—</strong></span><i><em></em></i></div><div class="map-progress map-overall-progress"><span><b>${svg('route')}Course <small>Current Path</small></b><strong>—</strong></span><i><em></em></i></div></div><button class="map-current" data-map="current" hidden>${svg('locate')}Current Course</button></footer>`;
 const surface=$('.map-surface');for(const [event,handler]of[['pointerdown',pointerDown],['pointermove',pointerMove],['pointerup',pointerUp],['pointercancel',pointerUp]])surface.addEventListener(event,handler);
 surface.addEventListener('wheel',wheel,{passive:false});surface.addEventListener('dblclick',ev=>{if(ev.target.closest('[data-map]'))return;ev.preventDefault();zoomAt(zoom<2?2:1,ev.clientX-box.left,ev.clientY-box.top);});
 resizeObserver?.disconnect();resizeObserver=new ResizeObserver(()=>{measure();paint();});resizeObserver.observe($('.map-stage'));applyLayers();updateCounts();pin();live(frame);
}
let box={width:0,height:0,left:0,top:0,boardW:0,boardH:0,x:0,y:0};
function measure(){const stage=$('.map-stage');if(!stage)return;const r=stage.getBoundingClientRect(),w=Math.min(r.width,r.height*16/9),h=w*9/16;
 box={width:r.width,height:r.height,left:r.left,top:r.top,boardW:w,boardH:h,x:(r.width-w)/2,y:(r.height-h)/2};const board=$('.map-board');Object.assign(board.style,{width:w+'px',height:h+'px',left:box.x+'px',top:box.y+'px'});clampPan();
}
function clampPan(){pan.x=MM.clamp(pan.x,-Math.max(0,(box.boardW*zoom-box.width)/2),Math.max(0,(box.boardW*zoom-box.width)/2));pan.y=MM.clamp(pan.y,-Math.max(0,(box.boardH*zoom-box.height)/2),Math.max(0,(box.boardH*zoom-box.height)/2));}
function transform(p){return{x:(p.x-.5)*box.boardW*zoom+box.width/2+pan.x,y:(p.y-.5)*box.boardH*zoom+box.height/2+pan.y};}
function positionBoard(){clampPan();const board=$('.map-board');board.style.transform=`translate(${pan.x}px,${pan.y}px) scale(${zoom})`;board.style.setProperty('--inverse-zoom',1/zoom);
 $('.map-follow').classList.toggle('following',follow);$('.map-follow').setAttribute('aria-pressed',String(follow));$('.map-scale').textContent=Math.round(zoom*100)+'%';
 dialog.querySelector('[data-map=zoom][data-value="-1"]').disabled=zoom<=1;dialog.querySelector('[data-map=zoom][data-value="1"]').disabled=zoom>=4;
}
function paint(refresh=true){if(!visible()||!box.width)return;positionBoard();if(refresh)markers();player();}
function filteredPoints(){return MM.filterPoints(data().points,layers.pokemon,{photos:state().photos,hasRequest:id=>pending(id).length>0,landmarks:layers.landmarks});}
function markers(){const host=$('.map-marker-layer');if(!host||!box.boardW)return;const bucket=MM.markerBucket(zoom),key=[cid,Math.round(box.boardW),bucket,layers.pokemon,layers.landmarks,markerRevision].join(':');if(key===markerKey)return;markerKey=key;
 const next=MM.markerGroups(filteredPoints(),{width:box.boardW,height:box.boardH,zoom:bucket},43);groups=new Map(next.map(g=>[g.key,g]));
 host.innerHTML=next.map(g=>{const p=g.points[0],ids=[...new Set(g.points.map(p=>p.pokemon).filter(Boolean))],stack=ids.length>1,name=stack?ids.length+' Pokémon':api.model.species.get(p.pokemon)?.name||(p.kind==='crystabloom'?'Crystabloom':'Landmark'),complete=p.kind==='pokemon'&&g.points.every(p=>MM.collection(state().photos,p.pokemon).diamond===4),count=stack?ids.length:g.points.length;
 return`<span class="map-pin-anchor" style="left:${g.x*100}%;top:${g.y*100}%"><button class="map-pin ${stack?'map-stack':p.kind} ${complete?'map-complete':''}" data-map="pin" data-value="${e(g.key)}" aria-label="${e(name)}${count>1?' · '+g.points.length+' Locations':''}">${stack?'<span class="map-stack-preview">'+ids.slice(0,2).map(portrait).join('')+'</span>':p.kind==='pokemon'?portrait(p.pokemon):icon(p.kind==='crystabloom'?orbArt():'tool-scan')}${count>1?`<b>${count}</b>`:complete?'<span class="map-diamond-dot"></span>':''}</button></span>`;
 }).join('');
}
function zoomAt(next,x=box.width/2,y=box.height/2,refresh=true){next=MM.clamp(next,1,4);const k=next/zoom;pan={x:(pan.x+box.width/2-x)*k-(box.width/2-x),y:(pan.y+box.height/2-y)*k-(box.height/2-y)};zoom=next;follow=false;paint(refresh);}
function pointerDown(ev){toggleLayers(false);gesture.set(ev.pointerId,{x:ev.clientX,y:ev.clientY});if(gesture.size===1)gestureMoved=false;drag={points:new Map(gesture),pan:{...pan},zoom};}
function pointerMove(ev){if(!gesture.has(ev.pointerId)||!drag)return;gesture.set(ev.pointerId,{x:ev.clientX,y:ev.clientY});const all=[...gesture.values()],before=[...drag.points.values()];
 if(all.length===1&&before.length===1){const dx=all[0].x-before[0].x,dy=all[0].y-before[0].y;if(Math.hypot(dx,dy)>5)gestureMoved=true;if(!gestureMoved)return;follow=false;pan={x:drag.pan.x+dx,y:drag.pan.y+dy};}
 else if(all.length===2&&before.length===2){const dist=p=>Math.hypot(p[0].x-p[1].x,p[0].y-p[1].y),old=dist(before);if(old<1)return;gestureMoved=true;follow=false;const next=MM.clamp(drag.zoom*dist(all)/old,1,4),cx=(all[0].x+all[1].x)/2-box.left,cy=(all[0].y+all[1].y)/2-box.top,ox=(before[0].x+before[1].x)/2-box.left,oy=(before[0].y+before[1].y)/2-box.top,k=next/drag.zoom;zoom=next;pan={x:(drag.pan.x+box.width/2-ox)*k-(box.width/2-cx),y:(drag.pan.y+box.height/2-oy)*k-(box.height/2-cy)};}
 if(gestureMoved){try{ev.currentTarget.setPointerCapture(ev.pointerId);}catch{}paint(false);}
}
function pointerUp(ev){if(gestureMoved)lastMove=Date.now();gesture.delete(ev.pointerId);drag=gesture.size?{points:new Map(gesture),pan:{...pan},zoom}:null;if(!gesture.size){markers();gestureMoved=false;}}
let wheelTimer;
function wheel(ev){ev.preventDefault();zoomAt(zoom*Math.exp(-ev.deltaY*.002),ev.clientX-box.left,ev.clientY-box.top,false);clearTimeout(wheelTimer);wheelTimer=setTimeout(markers,140);}
function travelled(calibration,to){let started=false;return(calibration?.anchors||[]).filter(a=>a[0]<=to).map(a=>{if(!Number.isFinite(a[1])||!Number.isFinite(a[2])){started=false;return'';}const prefix=started?'L':'M';started=true;return prefix+(a[1]*1600).toFixed(2)+' '+(a[2]*900).toFixed(2);}).join(' ');}
function player(){const fresh=visibility&&MM.frame(frame,Date.now(),cid),r=fresh?tracker.view(frame,Date.now()):null,el=$('.map-player');if(!el)return;
 const note=$('.map-position-note');note.hidden=!fresh;note.textContent=r?.player?'Approximate Route Position':'Position Not Mapped';$('.map-live-route').toggleAttribute('hidden',!r?.player);$('.map-follow').disabled=!r?.player;
 if(!r?.player){el.hidden=true;$('.map-view-cone').setAttribute('hidden','');return;}
 if(follow){pan={x:(.5-r.player.x)*box.boardW*zoom,y:(.5-r.player.y)*box.boardH*zoom};positionBoard();}
 const p=transform(r.player);el.hidden=false;el.style.left=p.x+'px';el.style.top=p.y+'px';el.querySelector('i').hidden=!r.direction;if(r.direction)el.style.setProperty('--heading',Math.atan2(r.direction.x*box.boardW,-r.direction.y*box.boardH)+'rad');
 const key=cid+':'+frame.run+':'+frame.routeKey;if(key!==trailKey||Math.abs(frame.distance-trailDistance)>=.25){trailKey=key;trailDistance=frame.distance;$('.route-travelled').setAttribute('d',travelled(r.calibration,frame.distance));}
 const historyKey=key+':'+r.pastRevision;if(historyKey!==pastKey){pastKey=historyKey;$('.route-past').innerHTML=r.past.map(part=>`<path d="${travelled(part.calibration,part.to)}"/>`).join('');}
 const cone=MM.viewCone(frame,r.calibration),path=MM.conePath(cone,box.boardW,box.boardH),view=$('.map-view-cone');view.toggleAttribute('hidden',!path);if(path){view.style.left=p.x+'px';view.style.top=p.y+'px';view.querySelector('path').setAttribute('d',path);}
}
function progress(selector,lo,hi=lo,approximate=false){const bar=$(selector),value=bar.querySelector('strong'),label=lo==null?'—':(approximate?'≈':'')+(Math.round(lo*100)===Math.round(hi*100)?Math.round(lo*100):Math.round(lo*100)+'–'+Math.round(hi*100))+'%';if(value.textContent!==label)value.textContent=label;
 bar.querySelector('em').style.width=(lo==null?0:lo*100)+'%';bar.classList.toggle('tracking',lo!=null);if(lo==null)bar.removeAttribute('aria-label');else bar.setAttribute('aria-label',bar.querySelector('b').textContent+' '+label);
}
function live(next){const incoming=MM.sanitize(next||{});if(incoming.status==='ready'&&frame.status==='ready'&&incoming.course===frame.course&&incoming.run===frame.run&&incoming.sampleAt<frame.sampleAt)return;frame=incoming;tracker.observe(frame,Date.now());if(!visible())return;
 const fresh=visibility&&MM.frame(frame,Date.now(),cid),r=fresh?tracker.view(frame,Date.now()):null;$('.map-section-name').textContent=r?.section||'Current Section';progress('.map-section-progress',r?.sectionProgress??null);progress('.map-overall-progress',r?.overall?.[0]??null,r?.overall?.[1]??null,r?.approximate===true);
 $('.map-level').textContent=fresh?(frame.level===4?'Lv. MAX':'Lv. '+frame.level):'';$('.map-current').hidden=!state().course||state().course===cid;player();
}
function updateCounts(){if(!$('.map-course-coverage'))return;const ids=[...new Set(data().points.filter(p=>p.pokemon).map(p=>p.pokemon))],diamond=ids.reduce((n,id)=>n+MM.collection(state().photos,id).diamond,0);$('.map-course-coverage').innerHTML=`${icon('star-diamond')}<b>${diamond}</b><span>/ ${ids.length*4}</span>`;}
function pin(){const card=$('.map-pin-card');if(!card)return;if(!selected){card.hidden=true;return;}card.hidden=false;const points=selected.points;if(new Set(points.map(p=>p.pokemon||p.kind)).size>1){const unique=[...new Set(points.map(p=>p.pokemon||p.kind))];card.innerHTML=`<div class="map-pin-head"><span>${svg('pin')}${points.length} Locations</span><button data-map="dismiss" aria-label="Close Location">${svg('close')}</button></div><div class="map-pin-choices">${unique.map((id,i)=>{const p=points.find(p=>(p.pokemon||p.kind)===id);return`<button data-map="choose" data-value="${i}">${p.pokemon?portrait(p.pokemon):icon(p.kind==='crystabloom'?orbArt():'tool-scan')}<span>${e(api.model.species.get(p.pokemon)?.name||(p.kind==='crystabloom'?'Crystabloom':'Landmark'))}</span></button>`;}).join('')}</div>`;card._choices=unique.map(id=>points.find(p=>(p.pokemon||p.kind)===id));return;}const p=points[0],mon=api.model.species.get(p.pokemon),col=MM.collection(state().photos,p.pokemon);card.innerHTML=`<div class="map-pin-head"><span>${svg('pin')}Map Sighting</span><button data-map="dismiss" aria-label="Close Location">${svg('close')}</button></div><div class="map-pin-body">${mon?portrait(mon.id):icon(p.kind==='crystabloom'?orbArt():'tool-scan')}<div><h3>${e(mon?.name||(p.kind==='crystabloom'?'Crystabloom':'Landmark'))}</h3>${mon?`<div class="map-pin-stars">${api.slots(mon.id)}</div><small>${col.diamond===4?'All Diamond Photos':(4-col.diamond)+' Diamond Photos to Find'}</small>`:'<small>A landmark recorded on the course map.</small>'}</div>${mon?`<button class="map-pin-book" data-map="book" data-value="${mon.id}" aria-label="Open ${e(mon.name)} in Photodex">${svg('right')}</button>`:''}</div>${mon?pending(mon.id).slice(0,2).map(r=>`<button class="map-pin-request" data-map="request" data-value="${e(r.id)}">${icon('requests')}<span><small>LenTalk</small><b>${e(r.title)}</b></span>${svg('right')}</button>`).join(''):''}`;}
function toggleLayers(on){const panel=$('.map-layers');on=typeof on==='boolean'?on:!panel.classList.contains('open');panel.classList.toggle('open',on);$('.map-layer-button').setAttribute('aria-expanded',String(on));}
function applyLayers(){if(!dialog)return;$('.map-stage').classList.toggle('hide-routes',!layers.routes);$('.map-filter-dot').hidden=layers.pokemon==='all';
 dialog.querySelectorAll('[data-map=pokemon-mode]').forEach(b=>b.setAttribute('aria-checked',String(layers.pokemon===b.dataset.value)));dialog.querySelectorAll('[data-map=layer]').forEach(b=>b.setAttribute('aria-checked',String(layers[b.dataset.value])));
}
function click(ev){const b=ev.target.closest('[data-map]');if(!b||b.disabled||Date.now()-lastMove<180)return;const a=b.dataset.map,v=b.dataset.value;try{window.SnapTouch?.tap();}catch{}
 if(a==='close'){close();return;}
 if(a==='zoom')zoomAt(zoom*(+v>0?1.5:1/1.5));
 else if(a==='fit'){zoom=1;pan={x:0,y:0};follow=false;paint();}
 else if(a==='follow'){follow=!follow;paint(false);}
 else if(a==='layers')toggleLayers();
 else if(a==='pokemon-mode'&&['all','missing','requests','off'].includes(v)){layers.pokemon=v;selected=null;remember();applyLayers();markers();pin();}
 else if(a==='layer'&&['landmarks','routes'].includes(v)){layers[v]=!layers[v];selected=null;remember();applyLayers();markers();pin();}
 else if(a==='pin'){const g=groups.get(v);if(g){selected=g;pin();}}
 else if(a==='choose'){const p=$('.map-pin-card')._choices?.[+v];if(p){selected={points:[p]};pin();}}
 else if(a==='dismiss'){selected=null;pin();}
 else if(a==='book'){returnToMap=true;close();api.openPokemon(+v);}
 else if(a==='request'){returnToMap=true;close();api.openRequest(v);}
 else if(a==='current')open(state().course);
}
function update(snapshot,changed={}){if(!api)return;const next=snapshot.course||'';
 if(!visible()){frame=MM.sanitize(snapshot.gameReading||{});tracker.observe(frame,Date.now());return;}
 if(autoCourse&&next&&next!==lastLiveCourse&&DATA.courses.some(c=>c.id===next)){setMapCourse(next);render();measure();paint();}lastLiveCourse=next;
 if(changed.collection||changed.requests){markerRevision++;updateCounts();markers();if(selected)pin();}live(snapshot.gameReading||{});
}
function setVisibility(value){visibility=value===true;if(!visibility&&visible())notify(false);else if(visible())notify(true);if(visible())player();}
setInterval(()=>{if(visible()&&!MM.frame(frame,Date.now(),cid))live(frame);},500);
window.SnapMaps={init,open,back,update,live,toolQueued(){},visibility:setVisibility,isOpen:visible};
})();
