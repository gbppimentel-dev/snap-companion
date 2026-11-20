package dev.gbppimentel.snapcompanion;

import java.nio.file.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class PhotoSelectionTextTest {
    private LiveParser parser;
    @Before public void setup()throws Exception{parser=new LiveParser(new Catalog(new String(Files.readAllBytes(java.nio.file.Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8)));}
    private LiveReading read(String text,long at){return parser.parse(List.of(new LiveReading.Word(text,.1,.2,.9,.3,.96f)),false,at,"synthetic inner picker","default");}
    @Test public void innerPickerUsesItsIndependentLabelsWithoutTheOverviewHelp(){var r=read("Grookey Photos Taken 12 Photodex Selected Compare All Total",1000);assertEquals(LiveReading.Scene.PHOTO_SELECTION,r.scene());assertEquals("810",r.fields().get("subject").value());}
    @Test public void partialLabelsDoNotTurnPhotodexOrMenusIntoSelection(){assertEquals(LiveReading.Scene.UNKNOWN,read("Grookey Photodex Selected Compare All Total",1000).scene());assertEquals(LiveReading.Scene.UNKNOWN,read("Evaluate Photos Auto Remove All",1000).scene());assertEquals(LiveReading.Scene.UNKNOWN,read("Grookey Photos Taken 12 Photodex Compare Total",1000).scene());}
    @Test public void gradingAndNoSubjectHavePriorityOverPickerWords(){assertEquals(LiveReading.Scene.EVALUATION,read("Professor's Evaluation Photos Taken 12 Photodex Selected Compare All Grookey",1000).scene());assertFalse(read("Choose photos to show to the professor You cannot select a photo with no subject Pichu",1000).fields().containsKey("subject"));}
    @Test public void strongInnerLabelIsNotSuppressedByWeakStrayPokemonText(){var r=parser.parse(List.of(new LiveReading.Word("Photos Taken 12 Photodex Selected Compare All Grookey",.1,.2,.8,.3,.96f),new LiveReading.Word("Pichu",.1,.5,.2,.55,.3f)),false,1000,"fixture","default");assertEquals("810",r.fields().get("subject").value());}
    @Test public void twoStrongDistinctReadingsConfirmTheSubjectAndAChangeClearsItsCategory()throws Exception{
        var stable=new LiveStabilizer();var a=read("Choose a photo Grookey 3 star",1000);stable.accept(a,.8f,3);stable.accept(a,.8f,3);assertFalse(stable.snapshot().containsKey("subject"));
        var b=read("Choose a photo Grookey 3 star",1500);stable.accept(b,.8f,3);assertEquals(810,PhotoPhaseState.read(b,stable.snapshot(),"default",1550).getInt("subject"));
        var next=read("Choose a photo Pichu 3 star",2000);stable.accept(next,.8f,3);assertFalse(stable.snapshot().containsKey("subject"));assertFalse(stable.snapshot().containsKey("category"));
        next=read("Choose a photo Pichu 3 star",2500);stable.accept(next,.8f,3);assertEquals(172,PhotoPhaseState.read(next,stable.snapshot(),"default",2550).getInt("subject"));
    }
    @Test public void mediumConfidenceStillRequiresThreeReadings(){var stable=new LiveStabilizer();for(int i=0;i<2;i++){var r=new LiveReading(1000+i*500,"fixture","default",LiveReading.Scene.PHOTO_SELECTION,Map.of("subject",new LiveReading.Value("810",.85f,"fixture")));stable.accept(r,.8f,3);}assertFalse(stable.snapshot().containsKey("subject"));stable.accept(new LiveReading(2000,"fixture","default",LiveReading.Scene.PHOTO_SELECTION,Map.of("subject",new LiveReading.Value("810",.85f,"fixture"))),.8f,3);assertEquals(3,stable.snapshot().get("subject").frames());}
}
