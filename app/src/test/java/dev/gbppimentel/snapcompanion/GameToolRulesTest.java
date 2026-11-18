package dev.gbppimentel.snapcompanion;
import org.junit.Test;
import static org.junit.Assert.*;
public class GameToolRulesTest {
 @Test public void onlyFourPhotographyToolsAreAllowed(){
  assertEquals(97,GameToolRules.key("fruit"));assertEquals(308,GameToolRules.scan("orb"));
  assertEquals(103,GameToolRules.key("melody"));assertEquals(307,GameToolRules.scan("scan"));
  for(String action:new String[]{"photo","start","shell","key:96","FRUIT","",null})assertEquals(-1,GameToolRules.key(action));
 }
 @Test public void pauseTransitionAndOldRequestsCannotSendInput(){
  assertTrue(GameToolRules.gameplay("ready",0x62e180));
  assertFalse(GameToolRules.gameplay("ready",0x630240));assertFalse(GameToolRules.gameplay("idle",0x62e180));
  assertTrue(GameToolRules.timely(10000,11000));assertFalse(GameToolRules.timely(10000,11001));
  assertFalse(GameToolRules.timely(10001,10000));assertFalse(GameToolRules.timely(0,0));
 }
}
