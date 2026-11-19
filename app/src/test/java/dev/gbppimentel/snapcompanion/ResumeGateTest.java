package dev.gbppimentel.snapcompanion;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResumeGateTest {
 @Test public void loadingAndOldFramesCannotStartWindowInspection(){
  assertFalse(ResumeGate.ready(true,false,LiveReading.Scene.UNKNOWN,1000,1000));
  assertFalse(ResumeGate.ready(true,false,LiveReading.Scene.TITLE,1000,3501));
  assertFalse(ResumeGate.ready(true,false,LiveReading.Scene.MENU,1001,1000));
  assertFalse(ResumeGate.ready(true,false,null,1000,1000));
 }
 @Test public void aVerifiedRenderedGameCanRestoreTheNativeOverlay(){
  assertTrue(ResumeGate.ready(true,false,LiveReading.Scene.TITLE,1000,1500));
  assertTrue(ResumeGate.ready(true,false,LiveReading.Scene.MENU,1000,1500));
  assertTrue(ResumeGate.ready(true,true,LiveReading.Scene.UNKNOWN,0,1500));
 }
 @Test public void hiddenGamesCannotResumeEvenWithAReading(){
  assertFalse(ResumeGate.ready(false,true,LiveReading.Scene.EXPEDITION,1000,1500));
 }
}
