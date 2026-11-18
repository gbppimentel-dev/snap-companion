package dev.gbppimentel.snapcompanion;
import org.junit.Test;import static org.junit.Assert.*;import java.io.IOException;
public class LinkLifecycleTest {
 private String reply(String state){return "snap|123|"+"A".repeat(32)+"|"+"b".repeat(64)+"|"+state+"|1";}
 @Test public void savingAndFirstSaveWaitStillProvideVerifiedGamePresence()throws Exception{
  for(String state:new String[]{"copied","same","changing","unsupported"}){var r=EdenLinkReply.parse(reply(state));var c=GameConnection.empty().observed(r.pid(),r.visible(),1000);assertTrue(c.onScreen(1500));assertEquals(state,r.state());}
  var r=EdenLinkReply.parse("snap|123|||no-save|1");assertTrue(GameConnection.empty().observed(r.pid(),r.visible(),1000).onScreen(1000));
 }
 @Test public void malformedBridgeResponsesCannotSupplyPresenceOrSaveIdentity(){
  for(String r:new String[]{"snap|123|||copied|1",reply("arbitrary"),reply("copied")+"|extra",reply("copied").replace("|123|","|1;echo x|"),reply("copied").replace("|123|","|9999999999|"),reply("copied").replace("|1","|true"),"snap|0|"+"A".repeat(32)+"|"+"b".repeat(64)+"|same|1","snap|123|"+"A".repeat(32)+"||no-save|1","other-game","changing"})assertThrows(r,IOException.class,()->EdenLinkReply.parse(r));
 }
 @Test public void presenceExpiresOnBridgeStallOrClockRollback(){var c=GameConnection.empty().observed("123",true,1000);assertTrue(c.onScreen(6999));assertFalse(c.onScreen(7000));assertFalse(c.onScreen(999));assertFalse(c.disconnected().running(1100));assertFalse(GameConnection.empty().closed(1100));}
 @Test public void generationChangesOnRestartHiddenGameLostProofAndSamePidReconnection(){
  var c=GameConnection.empty().observed("123",true,1000);String token=c.token();c=c.observed("123",true,1500);assertEquals(token,c.token());
  var next=c.observed("456",true,2000);assertNotEquals(token,next.token());var hidden=c.observed("123",false,2000);assertFalse(hidden.onScreen(2000));assertNotEquals(token,hidden.token());assertNotEquals(token,c.observed("123",true,8000).token());assertNotEquals(token,c.disconnected().observed("123",true,2000).token());
 }
 @Test public void onlyFreshExplicitClosureAllowsBackgroundPhotoCopy(){var c=GameConnection.empty().observed("0",false,1000);assertTrue(c.closed(1500));assertFalse(c.running(1500));assertFalse(c.closed(7000));assertFalse(c.disconnected().closed(1500));}
 @Test public void stableSnapshotsRequireTwoEqualReadsAndSuccessfulAcceptance(){
  var g=new SaveSyncGate();assertFalse(g.ready("first",1000));assertFalse(g.skipUnchanged(1000));assertTrue(g.ready("first",2000));g.accepted("first");assertTrue(g.skipUnchanged(3000));assertFalse(g.ready("first",31000));assertFalse(g.ready("replacement",32000));assertTrue(g.ready("replacement",33000));
 }
 @Test public void transientFailureRetriesWithoutAnotherGameSaveOrTightLoop(){var g=new SaveSyncGate();g.ready("same",1000);assertTrue(g.ready("same",2000));assertTrue(g.skipUnchanged(3000));assertFalse(g.ready("same",16999));assertFalse(g.skipUnchanged(17000));assertTrue(g.ready("same",17000));g.accepted("same");assertFalse(g.ready("same",18000));}
 @Test public void malformedSnapshotBackoffDoesNotBlockNewDataOrProfileChange(){var g=new SaveSyncGate();g.ready("bad",1000);g.ready("bad",2000);g.rejected(2000);assertFalse(g.ready("bad",61000));assertTrue(g.ready("bad",62000));g.rejected(62000);assertFalse(g.ready("good",63000));assertTrue(g.ready("good",64000));g.accepted("good");g.reset();assertFalse(g.ready("good",65000));assertTrue(g.ready("good",66000));}
 @Test public void extendedRevisionsAreValidatedIndependentlyOfLegacyPresence()throws Exception{String value=reply("same").replace("snap|","snap2|")+"|"+"c".repeat(64)+"|"+"d".repeat(64);var r=EdenLinkReply.parse(value);assertEquals("c".repeat(64),r.requestFingerprint());assertEquals("d".repeat(64),r.researchFingerprint());assertEquals("",EdenLinkReply.parse(reply("same")).requestFingerprint());assertThrows(IOException.class,()->EdenLinkReply.parse(value+"|extra"));assertThrows(IOException.class,()->EdenLinkReply.parse(value.replace("c".repeat(64),"bad")));assertThrows(IOException.class,()->EdenLinkReply.parse("snap2|123|||no-save|1|"+"c".repeat(64)+"|"));}
 @Test public void quietClosedCopyNeedsAnExplicitLatestProofAndCannotBeVisibleOrRunning()throws Exception{String prefix="snap3|0|"+"A".repeat(32)+"|"+"b".repeat(64)+"|same|0|||";assertTrue(EdenLinkReply.parse(prefix+"1").closedCopyAllowed());assertFalse(EdenLinkReply.parse(prefix+"0").closedCopyAllowed());assertFalse(EdenLinkReply.parse(reply("same")).closedCopyAllowed());assertThrows(IOException.class,()->EdenLinkReply.parse(prefix+"yes"));assertThrows(IOException.class,()->EdenLinkReply.parse((prefix+"1").replace("|0|","|123|")));}
}
