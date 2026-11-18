package dev.gbppimentel.snapcompanion.next;
import android.graphics.*;
import android.os.SystemClock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual bundled model on local Android: invalid input must not permanently occupy the reader. */
public final class OcrRecoveryDeviceTest {
 @Test public void malformedSmallFrameReleasesTheReaderForTheNextFrame()throws Exception{
  OcrEngine engine=new OcrEngine();AtomicInteger completed=new AtomicInteger();
  try{
   for(boolean tiny:new boolean[]{true,false}){
    CountDownLatch done=new CountDownLatch(1);Bitmap frame=Bitmap.createBitmap(tiny?2:960,tiny?2:540,Bitmap.Config.ARGB_8888);frame.eraseColor(Color.WHITE);
    assertTrue(engine.read(frame,tiny,new OcrEngine.Callback(){public void success(List<LiveReading.Word>w,long ms){completed.incrementAndGet();done.countDown();}public void failure(String reason){completed.incrementAndGet();done.countDown();}}));
    assertTrue("Bundled model completion timed out",done.await(40,TimeUnit.SECONDS));long end=SystemClock.elapsedRealtime()+2000;while(engine.busy()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(10);
    assertFalse("Reader stuck busy after completion",engine.busy());assertTrue("Accepted frame must be released",frame.isRecycled());
   }
   assertEquals(2,completed.get());
  }finally{engine.close();}
 }
}
