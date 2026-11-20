'use strict';const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');const dir=path.join(__dirname,'../app/src/main/assets');vm.runInThisContext(fs.readFileSync(dir+'/ui/content.js','utf8'));const M=require(dir+'/ui/model.js').create(global.SNAP_CONTENT);
const s={sourceKey:'same',photos:{'810:1':{score:4200,medal:'Diamond'},'810:2':{score:3600,medal:'Gold'},'810:3':{score:4000,medal:'Diamond'}},received:[M.data.requests[0].id],requestsKnown:true};
test('course, missing-star and Diamond filters combine against actual saved slots',()=>{assert(M.filterDex(s,{query:'Grookey',status:'missing',star:4,course:'park-day'}).some(p=>p.id===810));assert(!M.filterDex(s,{query:'Grookey',status:'missing',star:1}).length);assert(!M.filterDex(s,{query:'Grookey',status:'diamond',star:1}).length);assert(M.filterDex(s,{query:'Grookey',status:'diamond',star:2}).length);assert(!M.filterDex(s,{query:'Grookey',status:'complete'}).length);});
test('receipt filters use the saved registry and never infer completion',()=>{assert.equal(M.filterRequests(s,{receipt:'received'}).length,1);assert.equal(M.filterRequests(s,{receipt:'unreceived'}).length,233);assert.equal(M.filterRequests(s,{receipt:'all'}).length,234);assert.equal(s.completed,undefined);});
test('a short same-save receipt refresh preserves cards; source changes and stale data do not',()=>{const a=M.settle({},s,1000);const b=M.settle(a,{...s,requestsKnown:false,received:[]},1500);assert.equal(b.received.length,1);assert(b.receiptsPending);assert.equal(M.settle(a,{...s,sourceKey:'another',requestsKnown:false,received:[]},1500).received.length,0);assert.equal(M.settle(a,{...s,requestsKnown:false,received:[]},10000).received.length,0);});
test('opportunities require a known course; missing scores are not invented',()=>{assert.equal(M.opportunity({...s,course:''}),null);const p=M.opportunity({...s,course:'park-day',level:1});assert(p.locations.some(l=>l.course==='park-day'&&l.level<=1));assert.equal(M.photo(s,810,4),null);assert.deepEqual(M.missing(s,810),[4]);});
test('all static art paths exist and course banners follow all30 owned course IDs',()=>{assert.equal(M.data.courses.length,30);assert.equal(M.data.species.length,234);for(const c of M.data.courses)assert(fs.existsSync(dir+'/game-art/'+c.image),c.id);for(const p of M.data.species){assert(fs.existsSync(dir+'/game-art/'+p.portrait),p.name);assert.equal(p.cues.length,4);}assert.equal(M.courses.get('park-day').image,'courses/stage_001_thumbnail.webp');assert.equal(M.courses.get('cave').image,'courses/stage_010_thumbnail.webp');assert.equal(M.courses.get('camp').image,'courses/stage_101_thumbnail.webp');});
test('normal UI has no removed routes, hyperlinks or spoiler modes',()=>{const html=fs.readFileSync(dir+'/ui/index.html','utf8'),app=fs.readFileSync(dir+'/ui/app.js','utf8');assert(!/<a\b|href="https?:/i.test(html));for(const word of ['screen reader','still-frame','Eden','Research Planner','Notebook','Manual Route'])assert(!app.includes(word),word);for(const r of M.data.requests)assert(r.solution.length>10);});

test('Thor hours use Cocoon totals and never use session uptime',()=>{assert.equal(M.thorHours({totalMs:27563825}),'7.7');assert.equal(M.thorHours({totalMs:0}),'0.0');assert.equal(M.thorHours({}),'—');assert.equal(M.thorHours({totalMs:-1}),'—');assert.equal(M.thorHours({totalMs:1234567*3600000}),'1.2M');});

test('search accepts accents, number prefixes and name/type combinations',()=>{assert(M.filterDex(s,{query:'méganium'}).some(p=>p.id===154));assert(M.filterDex(s,{query:'#666 bug'}).some(p=>p.id===666));assert(M.filterDex(s,{query:'grookey grass'}).some(p=>p.id===810));assert(!M.filterDex(s,{query:'grookey water'}).length);assert.equal(M.searchText('  Pokémon · NIGHT  '),'pokemon night');});
test('keyword search keeps each location and form together under active filters',()=>{
 const model=require(dir+'/ui/model.js').create({islands:[{id:'one',name:'First Island'},{id:'two',name:'Second Island'}],requests:[],courses:[{id:'park',name:'Park Day',courseName:'Park',variant:'Day',island:'one',family:'park',levels:[1,2]},{id:'forest',name:'Forest Night',courseName:'Forest',variant:'Night',island:'two',family:'forest',levels:[1,2]}],species:[{id:1,number:1,name:'Tester',entry:'A curious Pokémon.',aliases:[],types:['Grass'],appearances:[{label:'Female',gender:'Female'},{label:'Winter Form'}],locations:[{course:'park',level:1,form:'Spring Form',description:'Dancing near a bridge.',tools:['melody']},{course:'forest',level:2,form:'Summer Form',description:'Resting beside a waterfall.',tools:['fluffruit']}],cues:[{stars:1,course:'',level:0,description:'Ordinary pose',tools:[]}]}]});
 assert.equal(model.filterDex({}, {query:'tester bridge melody'}).length,1);
 assert.equal(model.filterDex({}, {query:'park night'}).length,0);
 assert.equal(model.filterDex({}, {query:'winter bridge'}).length,0);
 assert.equal(model.filterDex({}, {query:'male'}).length,0);
 assert.equal(model.filterDex({}, {query:'female'}).length,1);
 assert.equal(model.filterDex({}, {query:'waterfall',course:'park'}).length,0);
 assert.equal(model.filterDex({}, {query:'waterfall',island:'two',level:1}).length,0);
 assert.equal(model.filterDex({}, {query:'waterfall',island:'two',level:2}).length,1);
 assert.deepEqual(model.filterCourses('tester melody').map(c=>c.id),['park']);
 assert.deepEqual(model.filterCourses('tester waterfall').map(c=>c.id),['forest']);
 assert.deepEqual(model.filterCourses('park night'),[]);
});
test('course, tools and pose keywords work offline without broadening an exact Pokémon name',()=>{
 assert.deepEqual(M.filterDex(s,{query:'grookey'}).map(p=>p.id),[810]);
 assert(M.filterDex(s,{query:'grookey melody'}).some(p=>p.id===810));
 assert(M.filterCourses('grookey').some(c=>c.id==='park-day'));
 assert(M.filterCourses('fluffruit').length>0);
 assert.equal(M.filterCourses('a-string-with-no-matching-location').length,0);
 assert.deepEqual(M.filterCourses(''),M.data.courses);
});
test('general photo tips never inherit a species encounter level or course',()=>{
 const p=M.species.get(666),cue=p.cues.find(c=>!c.course);assert(cue);
 const loc=M.location(p,cue.stars,'park-night');assert.equal(loc.course,'');assert.equal(loc.level,0);assert.equal(loc.description,cue.description);
 for(const p of M.data.species)for(const cue of p.cues){assert.equal(cue.levelKind,'example');if(cue.course)assert(p.locations.some(l=>l.course===cue.course),p.name);if(cue.level)assert(M.courses.get(cue.course)?.levels.includes(cue.level),p.name);}
 for(const [id,stars]of [[25,1],[706,1],[706,2],[527,2]]){const cue=M.location(M.species.get(id),stars,'park-day');assert.equal(cue.course,'');assert.equal(cue.level,0);}
});
test('all cached photo example headings are accounted for without exposing disputed levels',()=>{
 const report=JSON.parse(fs.readFileSync(path.join(__dirname,'../data/photo-example-provenance.json'),'utf8'));
 assert.equal(report.files.length,214);assert.equal(report.headings,856);assert.equal(report.accepted,845);assert.equal(report.excluded.length,11);assert.equal(report.qualifications.length,61);
 assert.equal(report.primaryCueCourseConflicts.length,4);
 let count=0;for(const p of M.data.species)for(const row of p.photoExamples){count++;assert(p.locations.some(l=>l.course===row.course));assert([1,2,3,4].includes(row.stars));assert(!row.exampleLevel||M.courses.get(row.course).levels.includes(row.exampleLevel));assert.equal(row.minimumLevel,undefined);assert.equal(row.description,undefined);}
 assert.equal(count,report.accepted);
 for(const q of report.qualifications)assert(M.species.get(q.species).photoExamples.some(row=>row.course===q.course&&row.stars===q.stars&&!row.exampleLevel));
 const examples=M.photoExamples(M.species.get(666),1,'park-day');assert.equal(examples[0].course,'park-day');assert(examples.every(row=>row.stars===1));
});
test('request Pokémon filters include supporting subjects, not just the lead',()=>{const r=M.data.requests.find(r=>r.subjects.length>1);assert(r);assert(M.filterRequests(s,{receipt:'all',species:r.subjects[1]}).some(x=>x.id===r.id));});

test('photo helper never treats unknown subject/category as a saved selection',()=>{const h=M.photoHelper({...s,phase:'selection',subject:810,photoPhase:{}});assert.equal(h.p,undefined);assert.equal(h.category,null);assert.equal(h.registered,null);const current=M.photoHelper({...s,phase:'selection',photoPhase:{subject:810,category:2,photosTaken:24}});assert.equal(current.registered.score,3600);assert.equal(current.photosTaken,24);});
test('grading does not activate the selection helper or expose comparison UI',()=>{const h=M.photoHelper({...s,phase:'evaluation',photoPhase:{subject:810,category:1},evaluation:{subject:810,total:4611}});assert.equal(h.p,undefined);assert.equal(h.category,null);assert.equal(h.registered,null);const app=fs.readFileSync(dir+'/ui/app.js','utf8');assert(!app.includes('function gradingHelper'));assert(!app.includes('Photo Grading'));});

test('physical input feedback expires and preserves a brief real tap without latching',()=>{assert.deepEqual(M.controllerView({available:false,at:1000,buttons:{A:true}},1100).pressed,[]);assert.deepEqual(M.controllerView({available:true,at:1000,buttons:{A:true}},3000).pressed,[]);assert.deepEqual(M.controllerView({available:true,at:1200,buttons:{A:false},pulses:{A:1180}},1250).pressed,['A']);assert.deepEqual(M.controllerView({available:true,at:1200,buttons:{A:false},pulses:{A:1180}},1400).pressed,[]);assert.deepEqual(M.controllerView({available:true,at:1200,buttons:{L1:true,R2:true,KEY_A:true}},1250).pressed,['L1','R2']);assert(!M.controllerView({available:true,at:1500,buttons:{A:true}},1000).available);});

test('island, course family, time and level must match the same encounter',()=>{
 for(const filter of [{island:'florio',level:1},{family:'park',level:2},{course:'park-night',level:1},{island:'durice',course:'snow-night',level:3}]){
  const actual=M.filterDex(s,filter).map(p=>p.id),expected=M.data.species.filter(p=>(p.locations.length?p.locations:p.cues).some(l=>{const c=M.courses.get(l.course);return(!filter.island||c.island===filter.island)&&(!filter.family||c.family===filter.family)&&(!filter.course||c.id===filter.course)&&c.levels.includes(filter.level)&&(l.level||1)<=filter.level;})).map(p=>p.id);
  assert.deepEqual(actual,expected);
 }
 assert.equal(M.filterDex(s,{island:'florio',course:'snow-night'}).length,0);
 assert(M.filterDex(s,{course:'park-day',level:1}).length<M.filterDex(s,{course:'park-day',level:3}).length);
 assert(!M.filterRequests(s,{course:'park-day',level:1,receipt:'all'}).some(r=>r.level>1));
});
test('course hierarchy and available levels cover every course including single-level routes',()=>{assert.equal(M.data.islands.length,6);for(const c of M.data.courses){assert(M.data.islands.some(i=>i.id===c.island));assert(c.family&&c.courseName&&c.variant);assert(c.levels.includes(1));}assert.deepEqual(M.courses.get('path-day').levels,[1]);assert.deepEqual(M.courses.get('park-night').levels,[1,2]);assert.deepEqual(M.courses.get('park-day').levels,[1,2,3]);});
test('named appearances use real form art and male/female images are distinct references',()=>{let pairs=0;for(const p of M.data.species){for(const a of p.appearances){assert(fs.existsSync(dir+'/game-art/'+a.image));assert(a.width>0&&a.height>0);assert(a.label);}const male=p.appearances.find(a=>a.gender==='Male'),female=p.appearances.find(a=>a.gender==='Female');if(male&&female){pairs++;assert.notEqual(male.image,female.image);assert(male.width>=150||male.height>=150);assert(female.width>=150||female.height>=150);}}assert.equal(pairs,36);assert.equal(M.species.get(666).appearances.length,20);assert(M.species.get(26).appearances.every(a=>a.label==='Alolan Form'));assert(M.species.get(592).appearances.find(a=>a.gender==='Female').image.includes('female'));});

test('medal totals and current photos use only the four official slots',()=>{const input={photos:{'810:1':{score:4200,medal:'Diamond'},'810:2':{score:3500,medal:'Gold'},'810:3':{score:3000,medal:'Silver'},'810:4':{score:2000,medal:'Bronze'}},album:[{species:810,stars:1,score:1000},{species:810,stars:1,score:2000}]};const rows=M.currentPhotos(input,810);assert.equal(rows.length,4);assert.deepEqual(rows.map(x=>x.stars),[1,2,3,4]);assert.equal(rows[0].score,4200);const sum=M.summary(input);assert.equal(sum.total,936);assert.equal(sum.missing,932);assert.deepEqual(sum.medals,{Bronze:1,Silver:1,Gold:1,Diamond:1});});
test('Show Missing hides only four-Diamond Pokémon, including when all stars are registered',()=>{const input={photos:Object.fromEntries([1,2,3,4].map(n=>['810:'+n,{score:4200,medal:'Diamond'}]))};assert(!M.filterDex(input,{status:'diamond'}).some(p=>p.id===810));input.photos['810:4'].medal='Gold';assert(M.filterDex(input,{status:'diamond'}).some(p=>p.id===810));const ps=M.courseSpecies('park-day');assert(ps.some(p=>p.id===810));const sum=M.courseSummary(input,'park-day');assert.equal(sum.total,ps.length*4);assert.equal(sum.count,4);assert.equal(sum.diamond,3);assert.equal(new Set(ps.map(p=>p.id)).size,ps.length);});
test('analogue view clamps invalid values and clears stale positions',()=>{const state={available:true,at:1200,axes:{lx:4,ly:-2,rx:.5,ry:NaN}};assert.deepEqual(M.controllerView(state,1250).axes,{lx:1,ly:-1,rx:.5,ry:0});assert.deepEqual(M.controllerView(state,3000).axes,{lx:0,ly:0,rx:0,ry:0});});
test('controller repaint waits for a real expiry instead of polling idle controls',()=>{assert.equal(M.controllerDelay({available:false},1000),null);assert.equal(M.controllerDelay({available:true,at:1200},3000),null);assert.equal(M.controllerDelay({available:true,at:1200},1100),null);assert.equal(M.controllerDelay({available:true,at:1200,buttons:{A:true},pulses:{A:1180}},1250),1450);assert.equal(M.controllerDelay({available:true,at:1200,buttons:{A:false},pulses:{A:1180}},1250),70);assert.equal(M.controllerDelay({available:true,at:1200,buttons:{A:false},pulses:{A:1180}},1320),1380);});
test('unknown request registry never labels requests received or not received',()=>{const unknown={requestsKnown:false,received:[]};assert.equal(M.filterRequests(unknown,{receipt:'received'}).length,0);assert.equal(M.filterRequests(unknown,{receipt:'unreceived'}).length,0);assert.equal(M.filterRequests(unknown,{receipt:'all'}).length,234);});

test('suggestions stay in the detected course/level, include medal improvements and exclude four-Diamond entries',()=>{
 const state={...s,course:'park-day',level:1};const rows=M.suggestions(state);assert(rows.length>1);assert(rows.every(p=>p.locations.some(l=>l.course==='park-day'&&(l.level||1)<=1)));assert.equal(new Set(rows.map(p=>p.id)).size,rows.length);
 const mon=rows[0];const gold={...state,photos:{...state.photos,...Object.fromEntries([1,2,3,4].map(n=>[mon.id+':'+n,{score:3600,medal:'Gold'}]))}};assert(M.suggestions(gold).some(p=>p.id===mon.id));const complete={...gold,photos:{...gold.photos,...Object.fromEntries([1,2,3,4].map(n=>[mon.id+':'+n,{score:4200,medal:'Diamond'}]))}};assert(!M.suggestions(complete).some(p=>p.id===mon.id));assert(M.suggestions({...state,course:''}).length>rows.length);assert.equal(state.course,'park-day');
});
test('reward previews are local and preserve the independently verified reward type',()=>{
 const rewards=M.data.requests.flatMap(r=>r.rewardArt||[]);assert.equal(rewards.length,111);for(const r of rewards){assert(r.image&&fs.existsSync(dir+'/game-art/'+r.image),r.label);assert(['Profile Icon','Sticker','Frame','Filter'].includes(r.kind));}const corrected=M.data.requests.find(r=>r.id==='request-munching-mandibuzz').rewardArt[0];assert.equal(corrected.kind,'Profile Icon');assert(corrected.correctedReferenceKind);assert.equal(corrected.image,'rewards/profile-items-127.webp');
});


test('request status distinguishes completion from receipt and keeps legacy completion unknown',()=>{
 const a=M.data.requests[0].id,b=M.data.requests[1].id,state={requestsKnown:true,completionsKnown:true,received:[a,b],completed:[b]};assert.equal(M.requestStatus(state,a),'progress');assert.equal(M.requestStatus(state,b),'completed');assert.equal(M.requestStatus({...state,completionsKnown:false},a),'received');assert.equal(M.requestStatus({...state,requestsKnown:false},a),'unknown');assert.deepEqual(M.filterRequests(state,{receipt:'completed'}).map(r=>r.id),[b]);assert.deepEqual(M.filterRequests(state,{receipt:'progress'}).map(r=>r.id),[a]);assert.equal(M.filterRequests({...state,completionsKnown:false},{receipt:'completed'}).length,0);assert.equal(M.filterRequests(state,{receipt:'received'}).length,2);
});
test('a new save revision never inherits old completed requests through receipt grace',()=>{
 const old=M.settle({}, {...s,requestVersion:'old',completionsKnown:true,completed:s.received},1000);const next={...s,requestVersion:'new',requestsKnown:false,completionsKnown:false,received:[],completed:[]};assert.equal(M.settle(old,next,1500).completionsKnown,false);assert.deepEqual(M.settle(old,next,1500).completed,[]);
});
test('every Pokémon has an original offline description and measured game reference facts',()=>{for(const p of M.data.species){assert(p.entry.length>30,p.name);assert(p.height&&p.weight,p.name);}const total=M.summary(s);assert.equal(total.missing+Object.values(total.medals).reduce((a,b)=>a+b,0),936);assert.equal(total.medals.Diamond,total.diamond);});

test('LenTalk keyword search includes tools and solution text without changing saved status',()=>{const rows=M.filterRequests(s,{receipt:'all',query:'fluffruit'});assert(rows.length>10);assert(rows.every(r=>M.searchText([r.title,r.solution,...r.tools,...r.rewards].join(' ')).includes('fluffruit')));assert.equal(s.received.length,1);});


test('live selection opportunities exclude only confirmed completed requests and react to a new save',()=>{
 const r=M.data.requests.find(r=>r.subjects.includes(r.species)),state={phase:'selection',course:r.course,level:r.automationMinimumLevel||r.level,photoPhase:{subject:r.species,category:1},photos:{},requestsKnown:true,completionsKnown:true,received:[r.id],completed:[r.id]};
 assert.equal(M.photoHelper(state).requests.length,0);assert.equal(M.pendingRequests(state).length,0);
 assert.equal(M.filterRequests(state,{receipt:'received'}).length,1);assert.equal(M.filterRequests(state,{receipt:'completed'}).length,1);
 assert.equal(M.photoHelper({...state,completed:[]}).requests.length,1);
 assert.equal(M.pendingRequests({...state,completionsKnown:false}).length,1);
 assert.equal(M.pendingRequests({...state,requestsKnown:false}).length,0);
 assert.equal(M.pendingRequests({...state,completed:[]},{course:'not-a-course'}).length,0);
});


test('saved course research is distinct from live identity and supports skipped levels and MAX',()=>{
 const a={course:'',level:0,researchCourses:{'park-day':{points:105000,base:40000,next:110000,nextLevel:3,level:2}}};assert.equal(M.savedResearch(a,'park-day').remaining,5000);assert.equal(a.course,'');assert.equal(a.level,0);assert.equal(M.savedResearch(a,'park-night'),null);
 const done={researchCourses:{'park-day':{points:200000,base:200000,next:0,nextLevel:0,level:4}}};assert.equal(M.savedResearch(done,'park-day').ratio,1);
 assert.equal(M.savedResearch({researchCourses:{'park-day':{points:20,base:40,next:110,nextLevel:3,level:2}}},'park-day'),null);
 assert.equal(M.savedResearch({researchCourses:{'park-day':{points:105000,base:40000,next:100000,nextLevel:3,level:2}}},'park-day'),null);
});


test('automatic course requests require live identity and level, then advance after confirmed completion',()=>{
 const rows=M.data.requests.filter(r=>r.course==='park-day'&&r.level<=2),received=rows.map(r=>r.id),state={connected:true,visible:true,course:'park-day',level:2,requestsKnown:true,completionsKnown:true,received,completed:[]};assert(rows.length>1);
 const first=M.nextRequest(state);assert(first&&first.course===state.course&&first.level<=2);const second=M.nextRequest({...state,completed:[first.id]});assert(second&&second.id!==first.id);
 for(const change of [{course:''},{level:0},{visible:false},{connected:false},{requestsKnown:false}])assert.equal(M.nextRequest({...state,...change}),null);
 assert.equal(M.nextRequest({...state,level:0,researchCourses:{'park-day':{level:4}}}),null);
 assert.equal(M.nextRequest({...state,completed:received}),null);
});

test('reward conditions require the actual verified dependency and saved state4, never just completion',()=>{const owner=M.data.requests.find(r=>r.rewardArt.some(a=>a.unlockRequest)),item=owner.rewardArt.find(a=>a.unlockRequest),id=item.unlockRequest;assert(id);const base={requestsKnown:true,completionsKnown:true,rewardConditionsKnown:true,received:[id],completed:[id],rewardReady:[]};assert.equal(M.rewardConditionMet(base,item),false);assert.equal(M.rewardConditionMet({...base,rewardReady:[id]},item),true);assert.equal(M.rewardConditionMet({...base,rewardReady:[id],rewardConditionsKnown:false},item),false);assert.equal(M.rewardConditionMet({...base,rewardReady:[id],completed:[]},item),false);assert.equal(M.rewardConditionMet({...base,rewardReady:[id]},{...item,conditionCode:101}),false);for(const r of M.data.requests)for(const a of r.rewardArt)if(a.unlockRequest)assert(M.requests.has(a.unlockRequest));});
test('short same-source registry refresh preserves known reward conditions without promoting old caches',()=>{const id=M.data.requests[0].id,base={...s,requestVersion:'a',rewardConditionsKnown:true,rewardReady:[id],completed:[id],completionsKnown:true};const first=M.settle({},base,1000);const held=M.settle(first,{...base,requestsKnown:false,rewardConditionsKnown:false,rewardReady:[]},2000);assert.equal(held.rewardConditionsKnown,true);assert.deepEqual(held.rewardReady,[id]);assert.equal(M.settle(first,{...base,requestVersion:'changed',requestsKnown:false,rewardConditionsKnown:false},2000).rewardConditionsKnown,false);});

test('forest season discoveries stay distinct from Pokemon forms, research levels and other courses',()=>{const state={photos:{},researchCourses:{forest:{seasonsKnown:true,seasons:[0,3]}}};const found=M.forestSeasons(state,'forest');assert.deepEqual(found.map(r=>r.found),[true,false,false,true]);assert.equal(M.forestSeasons(state,'park-day'),null);assert.equal(M.forestSeasons({researchCourses:{forest:{points:10000}}},'forest'),null);assert.equal(M.forestSeasons({researchCourses:{forest:{seasonsKnown:true,seasons:[4]}}},'forest'),null);assert.equal(M.savedResearch(state,'forest'),null);assert.equal(M.currentPhotos(state,585).length,0);});

test('selection request help requires the same known course and a sufficient selected level',()=>{const r=M.data.requests.find(r=>r.level>1),state={phase:'selection',course:r.course,level:r.automationMinimumLevel||r.level,photoPhase:{subject:r.species},requestsKnown:true,completionsKnown:true,received:[r.id],completed:[]};assert.equal(M.photoHelper(state).requests.length,1);assert.equal(M.photoHelper({...state,course:''}).requests.length,0);assert.equal(M.photoHelper({...state,level:0}).requests.length,0);assert.equal(M.photoHelper({...state,level:1}).requests.length,0);const other=M.data.courses.find(c=>c.id!==r.course);assert.equal(M.photoHelper({...state,course:other.id}).requests.length,0);});
test('disputed reference levels cannot become lower-level automatic request opportunities',()=>{for(const r of M.data.requests.filter(r=>r.automationMinimumLevel>r.level)){const state={connected:true,visible:true,phase:'selection',course:r.course,level:r.level,photoPhase:{subject:r.species},requestsKnown:true,completionsKnown:true,received:[r.id],completed:[]};assert.equal(M.nextRequest(state),null,r.title);assert.equal(M.photoHelper(state).requests.length,0,r.title);assert.equal(M.nextRequest({...state,level:r.automationMinimumLevel}).id,r.id);assert.equal(M.filterRequests(state,{query:r.title}).length,1);}});

test('star-photo methods keep category, subject, course ownership and example levels separate',()=>{
 const context={window:{}};vm.runInNewContext(fs.readFileSync(dir+'/ui/photo-details.js','utf8'),context);const reviewed=JSON.parse(JSON.stringify(context.window.SnapPhotoDetails));
 let authored=0,reference=0;for(const p of M.data.species)for(let n=1;n<=4;n++)for(const r of M.photoMethods(p,n,reviewed)){
  assert.equal(r.species,p.id);assert.equal(r.stars,n);assert(p.locations.some(l=>l.course===r.course));assert(r.steps.length&&r.steps.every(t=>typeof t==='string'&&t.trim()));assert(!r.exampleLevel||M.courses.get(r.course).levels.includes(r.exampleLevel));if(r.reviewed)authored++;else reference++;
 }
 assert.equal(authored,58);assert(reference>400,'Existing course-specific star instructions remain accessible');
 const vivillon=M.species.get(666),before=JSON.stringify(reviewed);assert.equal(M.photoMethods(vivillon,4,reviewed,'cave')[0].course,'cave');assert.equal(JSON.stringify(reviewed),before,'Choosing a current-course example does not reorder the source');assert.equal(M.photoMethods(vivillon,4,reviewed)[0].course,'camp');assert.deepEqual(M.photoMethods(vivillon,0,reviewed),[]);
 assert.equal(M.photoMethods(M.species.get(10),3,reviewed)[0].exampleLevel,0,'Impossible source Lv.3 is withheld for the two-level night course');
 const invalid={'666:4':[{...reviewed['666:4'][0],species:172},{...reviewed['666:4'][0],stars:3},{...reviewed['666:4'][0],course:'not-a-course'},{...reviewed['666:4'][0],steps:['']}]};assert.deepEqual(M.photoMethods(vivillon,4,invalid),[],'Invalid details never borrow another subject, category or course');
 const existing=M.photoMethods(M.species.get(810),1,reviewed);assert.equal(existing.length,1);assert.equal(existing[0].course,'park-day');assert.equal(existing[0].reviewed,false);assert.deepEqual(existing[0].steps,[M.location(M.species.get(810),1).description],'Existing scoped instructions are retained exactly');
});
