package dev.gbppimentel.snapcompanion;
import org.junit.Test;
import static org.junit.Assert.*;
public class CaptureCadenceTest {
 @Test public void supportedTargetsAreCeilingsAndBadCostsCannotOverflow(){assertEquals(500,CaptureCadence.interval(2,0,0,true));assertEquals(200,CaptureCadence.interval(5,0,0,true));assertEquals(100,CaptureCadence.interval(10,0,0,true));assertEquals(100,CaptureCadence.interval(Integer.MAX_VALUE,-1,0,true));assertEquals(2000,CaptureCadence.interval(10,Long.MAX_VALUE,0,true));}
 @Test public void thermalAndActualReportedDipsReduceWork(){assertEquals(1000,CaptureCadence.interval(10,0,2,true));assertEquals(2000,CaptureCadence.interval(10,0,3,true));assertTrue(CaptureCadence.pause(4,true));assertEquals(1000,CaptureCadence.interval(10,0,0,true,40f));assertEquals(2000,CaptureCadence.interval(10,0,0,true,3f));assertEquals(100,CaptureCadence.interval(10,0,0,true,Float.NaN));}
 @Test public void repeatedFailuresHaveABoundedRetryDelay(){long previous=0;for(int n=1;n<100;n++){long delay=CaptureCadence.failureInterval(n);assertTrue(delay>=previous);assertTrue(delay<=8000);previous=delay;}assertEquals(0,CaptureCadence.failureInterval(0));assertEquals(500,CaptureCadence.failureInterval(1));assertEquals(8000,CaptureCadence.failureInterval(Integer.MAX_VALUE));}
}
