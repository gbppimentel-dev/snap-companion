package dev.gbppimentel.snapcompanion.next;
import android.content.*;
import android.graphics.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AutoGalleryTest {
    private Context c;private String profile;private EdenLinkStore store;private byte[] jpeg;
    @Before public void setup()throws Exception{c=RuntimeEnvironment.getApplication();c.getSharedPreferences("snap",0).edit().clear().commit();c.getSharedPreferences("snap-eden",0).edit().clear().commit();c.getSharedPreferences("snap-gallery",0).edit().clear().commit();ProfileStore profiles=new ProfileStore(c.getSharedPreferences("snap",0));profile=profiles.create("Automatic gallery fixture");profiles.select(profile);store=new EdenLinkStore(c);Bitmap b=Bitmap.createBitmap(64,48,Bitmap.Config.ARGB_8888);b.eraseColor(Color.GREEN);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,90,out);jpeg=out.toByteArray();b.recycle();}
    private EdenPhotodex.Snapshot snapshot(int score,int image){var p=new EdenPhotodex.Photo(810,1,score,"Diamond",image);return new EdenPhotodex.Snapshot(Map.of(p.key(),p),1,0);}
    @Test public void copiesOnlyWhileIdleDeduplicatesAndPreservesEarlierBest()throws Exception{AtomicBoolean idle=new AtomicBoolean(false);AtomicInteger calls=new AtomicInteger();AutoGallery gallery=new AutoGallery(c,(ctx,user,hash,slot)->{calls.incrementAndGet();return jpeg;},idle::get);String manual="unaltered-manual-data";c.getSharedPreferences("snap",0).edit().putString("progress",manual).commit();var first=snapshot(4200,5);store.publish(profile,"A".repeat(32),"first",first);gallery.offer(profile,"A".repeat(32),"first",first);gallery.tick();assertEquals(0,calls.get());idle.set(true);gallery.tick();assertEquals(1,calls.get());assertEquals(1,new PersonalGallery(c,profile).all().size());gallery.offer(profile,"A".repeat(32),"same-photos-new-metadata",first);gallery.tick();assertEquals(1,calls.get());var better=snapshot(4611,29);store.publish(profile,"A".repeat(32),"better",better);gallery.offer(profile,"A".repeat(32),"better",better);gallery.tick();assertEquals(2,new PersonalGallery(c,profile).all().size());assertEquals(manual,c.getSharedPreferences("snap",0).getString("progress",""));gallery.close();}
    @Test public void documentSourceSwitchCannotReadAnOldRootImage()throws Exception{var snapshot=snapshot(4200,5);store.publish(profile,"A".repeat(32),"first",snapshot);AutoGallery gallery=new AutoGallery(c,(ctx,user,hash,slot)->{fail("Stale root source read");return jpeg;},()->true);gallery.offer(profile,"A".repeat(32),"first",snapshot);store.publishDocument(profile,"content://selected/save","second",snapshot);gallery.tick();assertTrue(new PersonalGallery(c,profile).all().isEmpty());assertEquals(0,gallery.pendingCount());gallery.close();}
    @Test public void unreadableImageKeepsRetryingWithoutBlockingLaterPhotos()throws Exception{var bad=new EdenPhotodex.Photo(25,1,4200,"Diamond",5);var good=new EdenPhotodex.Photo(810,1,4400,"Diamond",6);var snapshot=new EdenPhotodex.Snapshot(Map.of(bad.key(),bad,good.key(),good),2,0);store.publish(profile,"A".repeat(32),"fixture",snapshot);AtomicLong now=new AtomicLong(1000);AtomicInteger failures=new AtomicInteger();try(AutoGallery gallery=new AutoGallery(c,(ctx,user,hash,slot)->{if(slot==5){failures.incrementAndGet();throw new IOException("damaged fixture");}return jpeg;},()->true,now::get)){gallery.offer(profile,"A".repeat(32),"fixture",snapshot);for(int i=0;i<8;i++){int before=failures.get();gallery.tick();int calls=failures.get();if(calls>before){gallery.tick();assertEquals("No immediate retry loop",calls,failures.get());}now.addAndGet(61000);}assertTrue(failures.get()>=3);assertEquals(1,gallery.pendingCount());var copies=new PersonalGallery(c,profile).all();assertEquals(1,copies.size());assertEquals(810,copies.get(0).species());assertFalse(c.getSharedPreferences("snap-gallery",0).contains(profile+"."+bad.key()));}}
    @Test public void noSnapProcessFlagDoesNotMistakeAnotherGameOrStaleClosureForIdle(){boolean allowed=EdenLinkService.closedCopyAllowed;boolean running=EdenLinkService.gameRunning,active=EdenLinkService.active;GameConnection original=EdenLinkService.connection;try{EdenLinkService.active=true;EdenLinkService.gameRunning=false;EdenLinkService.connection=GameConnection.empty();assertFalse(AutoGallery.idle(c));EdenLinkService.connection=GameConnection.empty().observed("0",false,System.currentTimeMillis());EdenLinkService.closedCopyAllowed=false;assertFalse(AutoGallery.idle(c));EdenLinkService.closedCopyAllowed=true;assertTrue(AutoGallery.idle(c));EdenLinkService.connection=GameConnection.empty().observed("0",false,System.currentTimeMillis()-7000);assertFalse(AutoGallery.idle(c));}finally{EdenLinkService.gameRunning=running;EdenLinkService.active=active;EdenLinkService.connection=original;EdenLinkService.closedCopyAllowed=allowed;}}
    @Test public void pausingWhileAnImageIsReadCannotCommitAnAutomaticCopy()throws Exception{var snapshot=snapshot(4200,5);store.publish(profile,"A".repeat(32),"fixture",snapshot);try(AutoGallery gallery=new AutoGallery(c,(ctx,user,hash,slot)->{c.getSharedPreferences("snap",0).edit().putBoolean("autoGallery",false).commit();return jpeg;},()->true)){gallery.offer(profile,"A".repeat(32),"fixture",snapshot);gallery.tick();assertTrue(new PersonalGallery(c,profile).all().isEmpty());assertEquals(1,gallery.pendingCount());assertFalse(c.getSharedPreferences("snap-gallery",0).contains(profile+".810:1"));}}
    @Test public void separateGalleryInstancesCannotLoseConcurrentImports()throws Exception{java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);List<java.util.concurrent.Future<?>> jobs=new ArrayList<>();try{for(int writer=0;writer<2;writer++)jobs.add(workers.submit(()->{try{PersonalGallery gallery=new PersonalGallery(c,profile);start.await();for(int i=0;i<10;i++)gallery.add(jpeg,810,1,"park-day",4200,"Diamond",List.of(),"concurrent fixture","");}catch(Exception e){throw new RuntimeException(e);}}));start.countDown();for(var job:jobs)job.get(30,java.util.concurrent.TimeUnit.SECONDS);PersonalGallery result=new PersonalGallery(c,profile);assertEquals(20,result.all().size());for(var photo:result.all())assertTrue(result.file(photo).exists());}finally{workers.shutdownNow();}}
    @Test public void missingCachedFileIsRecoveredEvenWhenCheckpointSaysCopied()throws Exception{var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"first",saved);AtomicInteger calls=new AtomicInteger();try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{calls.incrementAndGet();return jpeg;},()->true)){g.offer(profile,"A".repeat(32),"first",saved);g.tick();var gallery=new PersonalGallery(c,profile);assertTrue(gallery.file(gallery.all().get(0)).delete());store.publish(profile,"A".repeat(32),"second",saved);g.offer(profile,"A".repeat(32),"second",saved);g.tick();assertEquals(2,calls.get());assertEquals(1,gallery.all().stream().filter(p->gallery.file(p).isFile()).count());}}
    @Test public void newCopyWindowsIncludeEvaluationButExcludeExpeditions(){assertTrue(AutoGallery.copyScene(LiveReading.Scene.EVALUATION));assertTrue(AutoGallery.copyScene(LiveReading.Scene.PHOTO_SELECTION));assertTrue(AutoGallery.copyScene(LiveReading.Scene.RESULTS));assertFalse(AutoGallery.copyScene(LiveReading.Scene.EXPEDITION));assertFalse(AutoGallery.copyScene(LiveReading.Scene.UNKNOWN));}

    @Test public void vanishedCachedFileRecoversWithoutAnotherGameSave()throws Exception{
        var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"unchanged",saved);AtomicLong now=new AtomicLong(1000);AtomicInteger calls=new AtomicInteger();
        try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{calls.incrementAndGet();return jpeg;},()->true,now::get)){
            g.offer(profile,"A".repeat(32),"unchanged",saved);g.tick();var photos=new PersonalGallery(c,profile);assertTrue(photos.file(photos.all().get(0)).delete());g.tick();assertEquals(1,calls.get());
            now.addAndGet(15000);g.tick();assertEquals(2,calls.get());assertEquals(1,photos.all().stream().filter(p->photos.file(p).isFile()).count());assertEquals("unchanged",store.hash(profile));
        }
    }
    @Test public void unchangedSaveRecoveryWaitsUntilAQuietScene()throws Exception{
        var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"unchanged",saved);AtomicLong now=new AtomicLong(1000);AtomicBoolean idle=new AtomicBoolean(true);AtomicInteger calls=new AtomicInteger();
        try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{calls.incrementAndGet();return jpeg;},idle::get,now::get)){
            g.offer(profile,"A".repeat(32),"unchanged",saved);g.tick();var photos=new PersonalGallery(c,profile);assertTrue(photos.file(photos.all().get(0)).delete());idle.set(false);now.addAndGet(15000);g.tick();assertEquals(1,calls.get());idle.set(true);g.tick();assertEquals(2,calls.get());
        }
    }
    @Test public void periodicRecoveryPreservesFailedPhotoBackoffAndOtherCopies()throws Exception{
        var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"unchanged",saved);AtomicLong now=new AtomicLong(1000);AtomicInteger calls=new AtomicInteger();
        try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{calls.incrementAndGet();throw new IOException("Unreadable fixture");},()->true,now::get)){
            g.offer(profile,"A".repeat(32),"unchanged",saved);
            for(long at:new long[]{1000,4000,10000,22000}){now.set(at);g.tick();}assertEquals(4,calls.get());
            now.set(38000);g.tick();assertEquals("Audit must not reset the scheduled retry",4,calls.get());now.set(46000);g.tick();assertEquals(5,calls.get());
            store.publish(profile,"A".repeat(32),"new-metadata-same-photo",saved);g.offer(profile,"A".repeat(32),"new-metadata-same-photo",saved);g.tick();assertEquals("An unrelated save must not reset backoff",5,calls.get());
        }
    }
    @Test public void recoveryDoesNotReimportRemovedSlotsOrReadDocumentSources()throws Exception{
        var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"unchanged",saved);AtomicLong now=new AtomicLong(1000);AtomicInteger calls=new AtomicInteger();
        try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{calls.incrementAndGet();return jpeg;},()->true,now::get)){
            g.offer(profile,"A".repeat(32),"unchanged",saved);g.tick();var photos=new PersonalGallery(c,profile);assertTrue(photos.file(photos.all().get(0)).delete());store.publish(profile,"A".repeat(32),"removed",new EdenPhotodex.Snapshot(Map.of(),0,0));now.addAndGet(15000);g.tick();assertEquals(1,calls.get());assertEquals(0,g.pendingCount());
            store.publishDocument(profile,"content://fixture/save","document",saved);now.addAndGet(15000);g.tick();assertEquals(1,calls.get());assertEquals(0,g.pendingCount());
        }
    }
    @Test public void returningToExpeditionDuringAReadDefersTheImportUntilQuietAgain()throws Exception{
        var saved=snapshot(4200,5);store.publish(profile,"A".repeat(32),"unchanged",saved);AtomicBoolean idle=new AtomicBoolean(true);AtomicInteger calls=new AtomicInteger();
        try(AutoGallery g=new AutoGallery(c,(ctx,u,h,slot)->{if(calls.incrementAndGet()==1)idle.set(false);return jpeg;},idle::get)){
            g.offer(profile,"A".repeat(32),"unchanged",saved);g.tick();assertEquals(1,calls.get());assertTrue(new PersonalGallery(c,profile).all().isEmpty());assertEquals(1,g.pendingCount());assertFalse(c.getSharedPreferences("snap-gallery",0).contains(profile+".810:1"));
            g.tick();assertEquals(1,calls.get());idle.set(true);g.tick();assertEquals(2,calls.get());assertEquals(1,new PersonalGallery(c,profile).all().size());
        }
    }

}
