package dev.gbppimentel.snapcompanion;
import java.nio.file.*;import java.util.*;import org.junit.*;import static org.junit.Assert.*;
public class LenTalkSceneTest {
 private LiveParser parser;
 @Before public void setup()throws Exception{parser=new LiveParser(new Catalog(new String(Files.readAllBytes(Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8)));}
 private List<LiveReading.Word> words(){return new ArrayList<>(List.of(
  new LiveReading.Word("FPS:59.9",.02,.002,.12,.025,.99f),
  new LiveReading.Word("LenTalk",.065,.03,.14,.065,.98f),
  new LiveReading.Word("Completed Requests",.69,.09,.83,.12,.98f),new LiveReading.Word("6",.86,.09,.88,.12,.99f),
  new LiveReading.Word("All Requests",.74,.14,.83,.17,.98f),new LiveReading.Word("75",.86,.14,.89,.17,.99f),
  new LiveReading.Word("Park (Night) Research Level Lv. 2",.1,.4,.5,.46,.99f),
  new LiveReading.Word("Pikachu",.1,.5,.3,.55,.99f)));
 }
 private LiveReading read(List<LiveReading.Word> w){return parser.parse(w,false,3000,"real-layout fixture","default");}
 @Test public void realRequestListIsQuietWithoutImportingPostcardFieldsOrTotals(){var r=read(words());assertEquals(LiveReading.Scene.MENU,r.scene());assertTrue(r.fields().isEmpty());assertTrue(AutoGallery.copyScene(r.scene()));}
 @Test public void smallAllLabelMayHaveLowerConfidenceOnlyWithBothStrongAnchors(){var w=words();var a=w.get(4);w.set(4,new LiveReading.Word(a.text(),a.left(),a.top(),a.right(),a.bottom(),.67f));assertEquals(LiveReading.Scene.MENU,read(w).scene());w.remove(1);assertNotEquals(LiveReading.Scene.MENU,read(w).scene());}
 @Test public void missingWeakOrWrongPositionAnchorsDoNotDeclareAQuietMenu(){
  for(int index:new int[]{1,2,4}){var w=words();var a=w.remove(index);assertNotEquals(LiveReading.Scene.MENU,read(w).scene());w=words();w.set(index,new LiveReading.Word(a.text(),a.left(),a.top(),a.right(),a.bottom(),.6f));assertNotEquals(LiveReading.Scene.MENU,read(w).scene());}
  var w=words();w.set(1,new LiveReading.Word("LenTalk",.3,.4,.5,.45,.99f));assertNotEquals(LiveReading.Scene.MENU,read(w).scene());
 }
 @Test public void leavingAContinuousRunForTheListClearsOldCourseAndLevel(){
  var c=new LiveContext();var s=new LiveStabilizer();for(int i=0;i<3;i++){var r=parser.parse(List.of(new LiveReading.Word("Park Night Research Level Lv. 2",.1,.2,.8,.3,.99f)),false,1000+i*500,"fixture","default");s.accept(r,.8f,3);c.observe("default:game1",r,s.snapshot());}
  assertEquals("park-night",c.view("default","default:game1",true,true,2000).course());
  var r=read(words());s.accept(r,.8f,3);c.observe("default:game1",r,s.snapshot());var v=c.view("default","default:game1",true,true,3000);assertEquals("",v.course());assertEquals(0,v.level());
 }
 @Test public void requestCompletionBannerStillRequiresItsOwnEvidence(){var r=parser.parse(List.of(new LiveReading.Word("Request Complete!",.1,.2,.8,.3,.99f)),false,1000,"fixture","default");assertEquals(LiveReading.Scene.REQUEST_COMPLETE,r.scene());assertFalse(read(words()).fields().containsKey("completedRequest"));}
}
