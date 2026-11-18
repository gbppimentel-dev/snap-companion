package dev.gbppimentel.snapcompanion.next;
import android.content.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Reads one small image record per non-expedition tick; never the whole photo archive. */
final class AutoGallery implements AutoCloseable {
 interface Reader{byte[] read(Context context,String user,String hash,int slot)throws Exception;}
 private static final class Job {final String profile,user,hash;final EdenPhotodex.Photo photo;long readyAt;int attempts;Job(String p,String u,String h,EdenPhotodex.Photo f){profile=p;user=u;hash=h;photo=f;}}
 static volatile long changedAt;
 static volatile String status="Saved photos ready";
 private final Context context;private final Reader reader;private final BooleanSupplier idle;private final java.util.function.LongSupplier clock;private final SharedPreferences prefs,checkpoints;private final EdenLinkStore store;
 private final ArrayDeque<Job> pending=new ArrayDeque<>();private ScheduledExecutorService worker;private volatile boolean closing;private String offered="";private long reconciledAt=Long.MIN_VALUE;
 AutoGallery(Context context){this(context,EdenGallery::read,()->idle(context));}
 AutoGallery(Context context,Reader reader,BooleanSupplier idle){this(context,reader,idle,System::currentTimeMillis);}
 AutoGallery(Context context,Reader reader,BooleanSupplier idle,java.util.function.LongSupplier clock){this.context=context;this.reader=reader;this.idle=idle;this.clock=clock;prefs=context.getSharedPreferences("snap",0);checkpoints=context.getSharedPreferences("snap-gallery",0);store=new EdenLinkStore(context);}
 void start(){worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(this::tick,0,1,TimeUnit.SECONDS);}
 synchronized void offer(String profile,String user,String hash,EdenPhotodex.Snapshot snapshot){
  if(closing)return;long now=clock.getAsLong();String key=profile+":"+user+":"+hash;if(key.equals(offered)&&now>=reconciledAt&&now-reconciledAt<15000)return;
  Map<String,Job> earlier=new HashMap<>();for(Job job:pending)if(job.profile.equals(profile)&&job.user.equals(user))earlier.put(identity(user,job.photo),job);
  offered=key;reconciledAt=now;pending.clear();
  Set<String> copies=new HashSet<>();try{PersonalGallery g=new PersonalGallery(context,profile);for(var p:g.all())if(g.file(p).isFile()&&g.file(p).length()>0)copies.add(p.linkedHash());}catch(Exception ignored){}
  // Replacements and newly registered images come before the older uncopied backlog.
  Set<String> changes=new HashSet<>();try{var history=store.history(profile);for(int i=Math.max(0,history.length()-12);i<history.length();i++){var h=history.optJSONObject(i);if(h!=null)changes.add(h.optString("key"));}}catch(Exception ignored){}
  var photos=new ArrayList<>(snapshot.photos().values());photos.sort(Comparator.<EdenPhotodex.Photo>comparingInt(p->changes.contains(p.key())?0:1).thenComparingInt(p->checkpoints.contains(profile+"."+p.key())?0:1).thenComparing(EdenPhotodex.Photo::key));
  for(var p:photos)if(!copies.contains(identity(user,p))){Job next=new Job(profile,user,hash,p),old=earlier.get(identity(user,p));if(old!=null){next.attempts=old.attempts;next.readyAt=old.readyAt;}pending.add(next);}
  status=pending.isEmpty()?"Saved photos ready":pending.size()+" photos waiting";
 }
 void tick(){
  if(closing||!prefs.getBoolean("autoGallery",true)||!idle.getAsBoolean())return;
  reconcile();
  Job job=null;synchronized(this){long now=clock.getAsLong();for(Job candidate:pending)if(candidate.readyAt<=now){job=candidate;break;}}
  if(job==null)return;final Job selected=job;if(!valid(job)){remove(job);return;}
  try{
   PersonalGallery gallery=new PersonalGallery(context,job.profile);String identity=identity(job.user,job.photo);boolean exists=gallery.all().stream().anyMatch(p->p.linkedHash().equals(identity)&&gallery.file(p).isFile()&&gallery.file(p).length()>0);
   if(!exists){byte[] bytes=reader.read(context,job.user,job.hash,job.photo.imageSlot());if(closing||!idle.getAsBoolean()||!valid(job))return;var p=job.photo;if(gallery.addGuarded(bytes,p.national(),p.stars(),"",p.score(),p.medal(),p.components(),"Saved Photodex",identity,()->!closing&&prefs.getBoolean("autoGallery",true)&&idle.getAsBoolean()&&valid(selected))==null)return;changedAt=System.nanoTime();}
   checkpoints.edit().putString(job.profile+"."+job.photo.key(),identity).apply();remove(job);status=pendingCount()==0?"Saved photos ready":pendingCount()+" photos waiting";
  }catch(Exception e){synchronized(this){if(!pending.contains(job))return;job.attempts++;job.readyAt=clock.getAsLong()+Math.min(60000L,3000L*(1L<<Math.min(5,job.attempts-1)));pending.remove(job);pending.addLast(job);status="Retrying a saved photo · "+pending.size()+" waiting";}}
 }
 /** Revisit missing local files on an unchanged save, bounded to quiet-scene intervals. */
 private void reconcile(){
  synchronized(this){long now=clock.getAsLong();if(reconciledAt!=Long.MIN_VALUE&&now>=reconciledAt&&now-reconciledAt<15000)return;}
  String profile=new ProfileStore(prefs).active(),user=store.bound(profile),hash=store.hash(profile);
  if(user.isEmpty()||hash.isEmpty()||!store.source(profile).equals("Eden saved Photodex")){synchronized(this){pending.clear();offered="";reconciledAt=clock.getAsLong();}return;}
  var photos=store.photos(profile);offer(profile,user,hash,new EdenPhotodex.Snapshot(photos,photos.size(),store.unmapped(profile)));
 }
 private boolean valid(Job job){return new ProfileStore(prefs).active().equals(job.profile)&&store.bound(job.profile).equals(job.user)&&store.hash(job.profile).equals(job.hash)&&store.source(job.profile).equals("Eden saved Photodex");}
 private synchronized void remove(Job job){pending.remove(job);}
 synchronized int pendingCount(){return pending.size();}
 static String identity(String user,EdenPhotodex.Photo photo){return "eden-photo-v1:"+user+":"+photo.key()+":"+photo.imageSlot()+":"+photo.score()+":"+photo.components();}
 static boolean copyScene(LiveReading.Scene scene){return Set.of(LiveReading.Scene.MENU,LiveReading.Scene.PHOTODEX,LiveReading.Scene.COURSE_SELECTION,LiveReading.Scene.PHOTO_SELECTION,LiveReading.Scene.EVALUATION,LiveReading.Scene.RESULTS,LiveReading.Scene.PAUSED,LiveReading.Scene.REQUEST_COMPLETE).contains(scene);}
 static boolean idle(Context context){
  if(!EdenLinkService.gameRunning)return EdenLinkService.active&&EdenLinkService.closedCopyAllowed&&EdenLinkService.connection.closed(System.currentTimeMillis());
  long now=System.currentTimeMillis();if(!EdenLinkService.visible(now)||!CaptureService.currentSource(new ProfileStore(context.getSharedPreferences("snap",0)).active(),now)||!CaptureService.active||!CaptureService.recognizedLatest||!copyScene(CaptureService.latestScene)||now-CaptureService.sceneAt>5000||now<CaptureService.sceneAt)return false;
  var screen=new LiveStore(context).snapshot().get("screen");return screen!=null&&screen.profileId().equals(new ProfileStore(context.getSharedPreferences("snap",0)).active())&&screen.fresh(now)&&screen.value().equals(CaptureService.latestScene.name());
 }
 public void close(){closing=true;if(worker!=null)worker.shutdownNow();synchronized(this){pending.clear();}}
}
