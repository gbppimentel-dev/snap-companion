package dev.gbppimentel.snapcompanion.next;

import java.nio.file.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Parser → stabilizer → capture-owned context. No device or current-course memory assumptions. */
public class CourseAutomationTest {
    private Catalog catalog;private LiveParser parser;
    @Before public void setup()throws Exception{catalog=new Catalog(new String(Files.readAllBytes(Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8));parser=new LiveParser(catalog);}
    private List<LiveReading.Word> words(String text){return List.of(new LiveReading.Word(text,.1,.2,.8,.3,.97f));}
    private LiveReading read(String text,boolean hud,long at){return parser.parse(words(text),hud,at,"fixture","default");}
    private void feed(LiveContext run,LiveStabilizer stable,String text,boolean hud,long at){var r=read(text,hud,at);stable.accept(r,.8f,3);run.observe("default:game-1",r,stable.snapshot());}
    private SnapUiState.View view(LiveContext run,long at){return run.view("default","default:game-1",true,true,at);}
    @Test public void everyCourseHasAnUnambiguousExplicitIdentity(){
        for(var c:catalog.courses){var r=read(c.name()+" Selected Research Level Lv. 1",false,1000);assertEquals(c.name(),LiveReading.Scene.COURSE_SELECTION,r.scene());assertEquals(c.name(),c.id(),r.fields().get("course").value());}
        for(var row:Map.of("Florio Nature Park Illumina Spot","park-illumina","Elsewhere Forest Illumina Spot","forest-illumina","Lental Seafloor Illumina Spot","sea-illumina","Fireflow Volcano Illumina Spot","volcano-illumina","Outaway Cave Illumina Spot","cave-illumina","Ruins of Remembrance Illumina Spot","ruins-illumina").entrySet())assertEquals(row.getValue(),read(row.getKey()+" Selected Research Level Lv. 1",false,1000).fields().get("course").value());
        assertFalse(read("Illumina Spot Selected Research Level Lv. 1",false,1000).fields().containsKey("course"));
    }
    @Test public void titleOnlyTransitionsCanEstablishAnExpeditionWithoutAHud(){
        var run=new LiveContext();var stable=new LiveStabilizer();
        feed(run,stable,"Florio Nature Park Night",false,1000);assertEquals("",view(run,1000).course());
        feed(run,stable,"Florio Nature Park Night",false,1500);assertEquals("park-night",view(run,1500).course());assertEquals(0,view(run,1500).level());
        assertEquals(LiveReading.Scene.COURSE_INTRO,read("Founja Jungle Night",false,2000).scene());
        assertEquals(LiveReading.Scene.UNKNOWN,read("My holiday plans Park Night tomorrow",false,2000).scene());
        assertFalse(read("Florio Nature Park Day Night",false,2000).fields().containsKey("course"));
    }
    @Test public void gamePauseRecoversContextButEmulatorPauseDoesNot(){
        var run=new LiveContext();var stable=new LiveStabilizer();
        for(int n=0;n<3;n++)feed(run,stable,"Pause Menu Continue Research Quit Research Park (Night) Selected Research Level Lv. 2",false,1000+n*500);
        assertEquals("park-night",view(run,2000).course());assertEquals(2,view(run,2000).level());assertEquals("paused",view(run,2000).phase());
        var raw=read("Emulation paused Resume emulation Park Night Selected Research Level 2",false,2500);assertTrue(raw.fields().isEmpty());
    }
    @Test public void aHeldGamePauseRetainsOneRealFrameAsOneFrame(){
        var stable=new LiveStabilizer();var raw=read("Pause Menu Continue Research Park Night Selected Research Level Lv. 1",false,1000);stable.accept(raw,.8f,3);assertFalse(stable.held(raw,2100,.8f).isEmpty());assertEquals(1,stable.snapshot().get("course").frames());assertEquals(1000,stable.snapshot().get("course").lastSeen());
    }
    @Test public void courseIdentityUsesItsWeakTimeTokenNotWholeScreenAverage(){
        var words=List.of(new LiveReading.Word("Florio Nature Park",.1,.2,.5,.24,.99f),new LiveReading.Word("Night",.6,.2,.8,.24,.41f),new LiveReading.Word("Research Level Lv. 2",.1,.3,.5,.34,.99f));
        var r=parser.parse(words,false,1000,"fixture","default");assertEquals(.41f,r.fields().get("course").confidence(),.001f);
        var stable=new LiveStabilizer();for(int i=0;i<3;i++)stable.accept(parser.parse(words,false,1000+i*500,"fixture","default"),.8f,3);assertFalse(stable.snapshot().containsKey("course"));
    }
    @Test public void shuffledOcrBlocksUseTheActualVisualLineOrder(){
        var words=List.of(new LiveReading.Word("Lv. 2",.45,.4,.6,.44,.96f),new LiveReading.Word("Night",.6,.2,.8,.24,.94f),new LiveReading.Word("Selected Research Level",.1,.4,.4,.44,.98f),new LiveReading.Word("Florio Nature Park",.1,.2,.55,.24,.99f));
        var r=parser.parse(words,false,1000,"fixture","default");assertEquals("park-night",r.fields().get("course").value());assertEquals("2",r.fields().get("researchLevel").value());assertEquals(.96f,r.fields().get("researchLevel").confidence(),.001f);
    }
    @Test public void nextLevelAndOpenChoicesCannotBecomeSelectedLevel(){
        var r=read("Park Night Next research level: 2",false,1000);assertEquals("park-night",r.fields().get("course").value());assertFalse(r.fields().containsKey("researchLevel"));
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Night Selected Research Level Lv. 2",false,1000+n*500);assertEquals(2,view(run,2000).level());
        feed(run,stable,"Park Night Change Research Level Lv. 1 Lv. 2 Lv. 3",false,2500);assertEquals(0,view(run,2500).level());
        var max=read("Park Night Selected Research Level MAX Lv.",false,3000);assertEquals("4",max.fields().get("researchLevel").value());
        assertFalse(read("Park Night Next Research Level MAX Lv.",false,3000).fields().containsKey("researchLevel"));
    }
    @Test public void courseCaptureContinuesWithoutAnActivityAndSurvivesReopening(){
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Night Selected Research Level Lv. 2",false,1000+n*500);
        // No view() calls for more than a minute: capture owns the run lifecycle.
        for(long at=3000;at<=63000;at+=2000)feed(run,stable,"",true,at);
        assertEquals("park-night",view(run,63000).course());assertEquals(2,view(run,63000).level());
        assertEquals("",run.view("default","default:game-2",true,true,63500).course());
    }
    @Test public void anotherCourseCannotShowThePreviousDayWhileStabilizing(){
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Day Selected Research Level Lv. 2",false,1000+n*500);assertEquals("park-day",view(run,2000).course());
        feed(run,stable,"Pause Menu Continue Research Jungle Night Selected Research Level Lv. 1",false,2500);assertEquals("",view(run,2500).course());
        for(int n=1;n<=2;n++)feed(run,stable,"Pause Menu Continue Research Jungle Night Selected Research Level Lv. 1",false,2500+n*500);
        assertEquals("jungle-night",view(run,3500).course());assertEquals(1,view(run,3500).level());
    }
    @Test public void campResultsGapsAndProfileChangesEndTheOldRun(){
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Day Selected Research Level Lv. 2",false,1000+n*500);
        feed(run,stable,"Expedition Results",false,2500);feed(run,stable,"",true,3000);assertEquals("",view(run,3000).course());
        for(int n=0;n<3;n++)feed(run,stable,"Park Night Selected Research Level Lv. 1",false,4000+n*500);
        assertEquals("",run.view("other","other:game-1",true,true,5500).course());
        run.clear();assertEquals("",view(run,6000).course());
    }
    @Test public void labelledFpsOverlayDoesNotHideAValidTransitionTitle(){
        var words=new ArrayList<>(words("Florio Nature Park Night"));words.add(new LiveReading.Word("FPS:59.9 FT:16.7",.01,.01,.25,.05,.98f));
        var r=parser.parse(words,false,1000,"fixture","default");assertEquals(LiveReading.Scene.COURSE_INTRO,r.scene());assertEquals("park-night",r.fields().get("course").value());
    }
    @Test public void maximumIsExplicitAndOldActivityDataCannotInventIt(){
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Night Selected Research Level MAX Lv.",false,1000+n*500);assertEquals(4,view(run,2000).level());
        assertEquals("",run.view("default","default:game-1",true,false,2500).course());
        assertEquals(0,view(run,3000).level());
    }
    @Test public void anAmbiguousOrWeakTransitionDropsThePreviousCourseWithoutGuessing(){
        var run=new LiveContext();var stable=new LiveStabilizer();for(int n=0;n<3;n++)feed(run,stable,"Park Day Selected Research Level Lv. 2",false,1000+n*500);
        feed(run,stable,"Florio Nature Park Day Night",false,2500);assertEquals("",view(run,2500).course());
        var weak=List.of(new LiveReading.Word("Founja Jungle Night",.1,.2,.8,.3,.42f));var r=parser.parse(weak,false,3000,"fixture","default");assertEquals(LiveReading.Scene.COURSE_INTRO,r.scene());stable.accept(r,.8f,3);run.observe("default:game-1",r,stable.snapshot());assertEquals("",view(run,3000).course());
    }
    @Test public void mapResearchProgressIsNotEvidenceOfTheSelectedRunLevel(){for(String label:List.of("Research Level Lv. 2","Research Level MAX Lv.","Next Research Level Lv. 3")){var r=read("Park (Night) "+label,false,1000);assertEquals("park-night",r.fields().get("course").value());assertFalse(label,r.fields().containsKey("researchLevel"));}}
}
