package dev.gbppimentel.snapcompanion.next;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdenFpsTest {
    private LiveReading.Word w(String text,double left,double right){return new LiveReading.Word(text,left,.01,right,.04,.95f);}
    @Test public void acceptsActualOverlayLabelsWithSeparateOrCombinedDigits(){
        var reading=EdenFps.parse(List.of(w("FPS:",.02,.05),w("59.9",.055,.09),w("FT:16.7ms",.12,.22)));
        assertNotNull(reading);assertEquals(59.9f,reading.fps(),.001);assertEquals(16.7f,reading.frameMillis(),.001);
        assertEquals(40.2f,EdenFps.parse(List.of(w("FPS:40,2",.02,.12))).fps(),.001);
    }
    @Test public void neverTreatsUnlabeledOutOfBoundsOrAmbiguousNumbersAsFps(){
        assertNull(EdenFps.parse(List.of(w("60",.02,.05))));
        assertNull(EdenFps.parse(List.of(w("FPS:60",.02,.12),w("FPS:30",.2,.3))));
        assertNull(EdenFps.parse(List.of(w("FPS:241",.02,.12))));
        assertNull(EdenFps.parse(List.of(new LiveReading.Word("FPS:60",.02,.4,.12,.45,.95f))));
        assertNull(EdenFps.parse(List.of(new LiveReading.Word("FPS:60",.02,.01,.12,.04,.6f))));
        assertNull(EdenFps.parse(List.of(w("FPS:",.02,.05),w("60",.25,.3))));
    }
    @Test public void reducesWorkForFreshReportedDipsWithoutStoppingLiveReading(){
        assertEquals(500,CaptureCadence.interval(2,40,0,true,60f));
        assertEquals(1000,CaptureCadence.interval(2,40,0,true,40f));
        assertEquals(2000,CaptureCadence.interval(2,40,0,true,0f));
        assertEquals(500,CaptureCadence.interval(2,40,0,true,null));
        assertEquals(1000,CaptureCadence.interval(2,40,0,true,60f,25f));
        assertEquals(500,CaptureCadence.interval(2,40,0,true,Float.NaN,Float.NaN));
        assertEquals(2000,CaptureCadence.interval(2,40,3,true,60f));
        assertEquals(500,CaptureCadence.interval(2,40,0,false,40f));
        var sample=new EdenFps.Sample(10000,"default","MENU",60f,null,.95f);
        assertTrue(sample.fresh(15000));assertFalse(sample.fresh(15001));assertFalse(sample.fresh(9999));
    }
}
