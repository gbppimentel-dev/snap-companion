package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import static org.junit.Assert.*;

/** Exercise the real queued publication boundary without projection, OCR or device access. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class CaptureDeliveryTest {
 private CaptureService capture;private GameConnection original;private boolean active;private String source;
 @Before public void setup()throws Exception{
  ShadowBuild.setModel("AYN Thor");Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("snap",0).edit().clear().commit();
  original=EdenLinkService.connection;active=EdenLinkService.active;EdenLinkService.active=true;EdenLinkService.connection=GameConnection.empty().observed("123",true,System.currentTimeMillis());
  capture=Robolectric.buildService(CaptureService.class).create().get();source="default:"+EdenLinkService.connection.token();
  ReflectionHelpers.setField(capture,"prefs",c.getSharedPreferences("snap",0));ReflectionHelpers.setField(capture,"store",new LiveStore(c));ReflectionHelpers.setField(capture,"parser",new LiveParser(new Catalog(GuideBookTest.asset("catalog.json"))));ReflectionHelpers.setField(capture,"captureSource",source);ReflectionHelpers.setField(capture,"work",new Handler(Looper.getMainLooper()));
 }
 @After public void cleanup(){capture.onDestroy();Shadows.shadowOf(Looper.getMainLooper()).idle();EdenLinkService.connection=original;EdenLinkService.active=active;EdenFps.latest=null;}
 private CaptureService.FrameTicket ticket(){return capture.ticket(System.currentTimeMillis());}
 @Test public void resultIsQueuedThenPublishedOnlyOnTheCaptureWorker(){var ticket=ticket();capture.enqueueSuccess(ticket,List.of(),12,false,false,null);assertNull(CaptureService.currentReading());Shadows.shadowOf(Looper.getMainLooper()).idle();assertNotNull(CaptureService.currentReading());assertTrue(CaptureService.currentSource("default",System.currentTimeMillis()));}
 @Test public void resizeOrPauseResetRejectsAnAlreadyQueuedResult(){var ticket=ticket();capture.enqueueSuccess(ticket,List.of(),12,false,false,null);ReflectionHelpers.callInstanceMethod(capture,"clearGameContext");Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(CaptureService.currentReading());assertFalse(CaptureService.currentSource("default",System.currentTimeMillis()));assertFalse(capture.accepts(ticket,System.currentTimeMillis()));assertTrue(capture.accepts(ticket(),System.currentTimeMillis()));}
 @Test public void gameRestartAndLostProofCannotPublishQueuedOldText(){var ticket=ticket();capture.enqueueSuccess(ticket,List.of(),12,false,false,null);EdenLinkService.connection=EdenLinkService.connection.disconnected().observed("123",true,System.currentTimeMillis());Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(CaptureService.currentReading());assertFalse(capture.accepts(ticket,System.currentTimeMillis()));}
 @Test public void expiredOrFutureCapturedFramesAreRejectedAtDelivery(){long now=System.currentTimeMillis();assertFalse(capture.accepts(capture.ticket(now-5001),now));assertFalse(capture.accepts(capture.ticket(now+1),now));assertTrue(capture.accepts(capture.ticket(now-5000),now));}
 @Test public void shutdownCannotRepublishQueuedFields(){var ticket=ticket();capture.enqueueSuccess(ticket,List.of(),12,false,false,null);capture.onDestroy();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(CaptureService.currentReading());assertFalse(capture.accepts(ticket,System.currentTimeMillis()));}
 @Test public void failedInferenceCostBacksOffAndAValidResultRecovers(){var ticket=ticket();ReflectionHelpers.callInstanceMethod(capture,"failed",org.robolectric.util.ReflectionHelpers.ClassParameter.from(CaptureService.FrameTicket.class,ticket),org.robolectric.util.ReflectionHelpers.ClassParameter.from(long.class,750L),org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"fixture"),org.robolectric.util.ReflectionHelpers.ClassParameter.from(android.graphics.Bitmap.class,null));assertEquals(1,(int)ReflectionHelpers.getField(capture,"consecutiveFailures"));assertEquals(750,CaptureService.inferenceMs);capture.enqueueSuccess(ticket,List.of(),20,false,false,null);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,(int)ReflectionHelpers.getField(capture,"consecutiveFailures"));}
}
