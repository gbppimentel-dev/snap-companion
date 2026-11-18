package dev.gbppimentel.snapcompanion;

import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public final class LiveTest {
    private LiveParser parser;
    @Before public void setup()throws Exception{parser=new LiveParser(new Catalog(new String(Files.readAllBytes(Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8)));}
    private List<LiveReading.Word> words(String text,float confidence){List<LiveReading.Word> words=new ArrayList<>();for(String w:text.split(" "))words.add(new LiveReading.Word(w,.1,.1,.3,.15,confidence));return words;}
    private LiveReading parse(String text){return parser.parse(words(text,.98f),false,1000,"fixture","default");}
    @Test public void courseAndLevelRequireUnambiguousGameContext(){LiveReading r=parse("Park (Day) Selected Research Level 2");assertEquals(LiveReading.Scene.COURSE_SELECTION,r.scene());assertEquals("park-day",r.fields().get("course").value());assertEquals("2",r.fields().get("researchLevel").value());assertTrue(parse("Park Day Park Night Selected Research Level 2").fields().isEmpty());}
    @Test public void ownAppAndUnrelatedTextNeverBecomeGameState(){assertFalse(parse("SNAP FIELD COMPANION Park Day Selected Research Level 2").recognizedGame());assertFalse(parse("Shopping list Pichu 72 camera").recognizedGame());}
    @Test public void filmUsesExplicitLabelAndRejectsImpossibleAndConflictingCounts(){assertEquals("17",parse("Photos remaining 17").fields().get("remainingPhotos").value());assertFalse(parse("Photos remaining 99").fields().containsKey("remainingPhotos"));assertFalse(parse("Photos remaining 17 remaining photos 18").fields().containsKey("remainingPhotos"));}
    @Test public void multipleSpeciesStayAmbiguous(){assertFalse(parse("Photos remaining 17 Pichu Pikachu").fields().containsKey("subject"));assertEquals("172",parse("Photos remaining 17 Pichu").fields().get("subject").value());}
    @Test public void actualPhotodexBannerIsNotMistakenForTheCompanion(){LiveReading r=parse("Registered in your Photodex Pichu 1 star");assertEquals(LiveReading.Scene.PHOTODEX,r.scene());assertEquals("confirmed-screen",r.fields().get("registration").value());assertFalse(r.fields().containsKey("course"));}
    @Test public void explicitRequestTitleAndCompletionBannerBothRequired(){assertFalse(parse("Hide-and-Seek in the Flowers").fields().containsKey("completedRequest"));assertEquals("hide-flowers",parse("Request complete Hide-and-Seek in the Flowers").fields().get("completedRequest").value());}
    @Test public void stabilizesWithoutInflatingConfidenceAndExpiresFields(){LiveStabilizer s=new LiveStabilizer();for(int i=1;i<=3;i++){List<LiveStabilizer.Stable> changed=s.accept(parser.parse(words("Photos remaining 17",.93f),false,i*500,"fixture","default"),.90f,3);if(i<3)assertTrue(changed.isEmpty());else assertEquals(2,changed.size());}LiveStabilizer.Stable film=s.snapshot().get("remainingPhotos");assertEquals(.93f,film.confidence(),.001f);assertTrue(film.fresh(4000));assertFalse(film.fresh(5000));}
    @Test public void repeatedTimestampAndLowQualityNeverSupplyExtraEvidence(){LiveStabilizer s=new LiveStabilizer();LiveReading r=parse("Photos remaining 17");for(int i=0;i<8;i++)s.accept(r,.90f,3);assertTrue(s.snapshot().isEmpty());for(int i=0;i<5;i++)s.accept(parser.parse(words("Photos remaining 17",.70f),false,2000+i*500,"fixture","default"),.90f,3);assertFalse(s.snapshot().containsKey("remainingPhotos"));}
    @Test public void profilesAndTimeRollbackClearPreviousFields(){LiveStabilizer s=new LiveStabilizer();for(int i=1;i<=3;i++)s.accept(parser.parse(words("Photos remaining 17",.98f),false,1000+i*100,"fixture","default"),.9f,3);assertFalse(s.snapshot().isEmpty());s.accept(parser.parse(words("Photos remaining 17",.98f),false,1300,"fixture","second"),.9f,3);assertTrue(s.snapshot().isEmpty());}
    @Test public void staticDwellPreservesSingleFrameTimestampAndConfidence(){LiveStabilizer s=new LiveStabilizer();LiveReading r=parse("Photos remaining 17");s.accept(r,.9f,3);assertTrue(s.held(r,1500,.9f).isEmpty());assertFalse(s.held(r,2100,.9f).isEmpty());LiveStabilizer.Stable value=s.snapshot().get("remainingPhotos");assertEquals(1,value.frames());assertEquals(1000,value.lastSeen());assertEquals(.98f,value.confidence(),.001);assertTrue(value.evidence().contains("confidence unchanged"));assertTrue(s.held(r,2200,.9f).isEmpty());}
    @Test public void interruptionRequiresConsecutiveEvidenceAgain(){LiveStabilizer s=new LiveStabilizer();s.accept(parse("Photos remaining 17"),.9f,3);s.accept(parser.parse(words("unrelated screen",.99f),false,1500,"fixture","default"),.9f,3);for(int i=1;i<=2;i++)s.accept(parser.parse(words("Photos remaining 17",.98f),false,1500+i*500,"fixture","default"),.9f,3);assertFalse(s.snapshot().containsKey("remainingPhotos"));}
    @Test public void separateNightBadgeAndExpeditionTitleDoNotFallBackToDay(){LiveReading r=parse("Florio Nature Park Selected Research Level 2 Night");assertEquals(LiveReading.Scene.COURSE_SELECTION,r.scene());assertEquals("park-night",r.fields().get("course").value());var words=List.of(new LiveReading.Word("Florio Nature Park (Night)",.1,.2,.8,.3,.98f));var live=parser.parse(words,true,1000,"fixture","default");assertEquals(LiveReading.Scene.EXPEDITION,live.scene());assertEquals("park-night",live.fields().get("course").value());assertFalse(parse("Florio Nature Park Selected Research Level 2 Day Night").fields().containsKey("course"));}
    @Test public void ambiguousCourseSelectionInvalidatesTheEarlierDayReading(){LiveStabilizer stable=new LiveStabilizer();for(int i=1;i<=3;i++)stable.accept(parser.parse(words("Park Day Selected Research Level 2",.98f),false,i*1000,"fixture","default"),.8f,3);assertEquals("park-day",stable.snapshot().get("course").value());stable.accept(parser.parse(words("Florio Nature Park Selected Research Level 2 Day Night",.98f),false,4000,"fixture","default"),.8f,3);assertFalse(stable.snapshot().containsKey("course"));for(int i=5;i<=7;i++)stable.accept(parser.parse(words("Florio Nature Park Selected Research Level 2 Night",.98f),false,i*1000,"fixture","default"),.8f,3);assertEquals("park-night",stable.snapshot().get("course").value());}
    @Test public void photoSelectionRecognizesExplicitCategoryCountsAndRejectsConflicts(){var r=parse("Choose a photo Grookey 3 star Photos taken 24 Photos selected 5");assertEquals(LiveReading.Scene.PHOTO_SELECTION,r.scene());assertEquals("810",r.fields().get("subject").value());assertEquals("3",r.fields().get("category").value());assertEquals("24",r.fields().get("photosTaken").value());assertEquals("5",r.fields().get("photosSelected").value());assertFalse(parse("Choose a photo Grookey 1 star 4 star Photos taken 99").fields().containsKey("category"));assertFalse(parse("Choose a photo Photos taken 12 Photos taken 14").fields().containsKey("photosTaken"));}
    @Test public void expeditionTitleCanSupplyExplicitResearchLevel(){var r=parser.parse(words("Florio Nature Park Night Research Level 2 Photos remaining 17",.98f),true,1000,"fixture","default");assertEquals(LiveReading.Scene.EXPEDITION,r.scene());assertEquals("park-night",r.fields().get("course").value());assertEquals("2",r.fields().get("researchLevel").value());assertEquals("17",r.fields().get("remainingPhotos").value());}

    @Test public void explicitSelectedLevelKeepsItsActualConfidenceAndStabilizes(){
        var input=words("Park (Night) Selected Research Level Lv. 2",.96f);var r=parser.parse(input,false,1000,"explicit-selection fixture","default");
        assertEquals("2",r.fields().get("researchLevel").value());assertEquals(.96f,r.fields().get("researchLevel").confidence(),.001f);
        LiveStabilizer stable=new LiveStabilizer();for(int n=0;n<3;n++)stable.accept(parser.parse(input,false,1000+n*500,"fixture","default"),.8f,3);
        assertEquals("2",stable.snapshot().get("researchLevel").value());
        var ui=new SnapUiState().update("default","12",true,true,true,LiveReading.Scene.COURSE_SELECTION,2000,stable.snapshot(),2000);
        assertEquals("park-night",ui.course());assertEquals(2,ui.level());
    }
    @Test public void weakLevelTokenAndOpenLevelChoicesDoNotBecomeSelectedLevel(){
        var input=new ArrayList<>(words("Park (Night) Selected Research Level Lv.",.99f));input.add(new LiveReading.Word("2",.2,.2,.3,.3,.45f));
        var r=parser.parse(input,false,1000,"fixture","default");assertEquals(.45f,r.fields().get("researchLevel").confidence(),.001f);
        LiveStabilizer stable=new LiveStabilizer();for(int n=0;n<3;n++)stable.accept(parser.parse(input,false,1000+n*500,"fixture","default"),.8f,3);assertFalse(stable.snapshot().containsKey("researchLevel"));
        assertFalse(parse("Park Night Selected Research Level Lv. 2 Change Research Level Lv. 1 Lv. 2 Lv. 3 Select research level and start").fields().containsKey("researchLevel"));
        assertEquals("4",parse("Park Night Selected Research Level MAX Lv.").fields().get("researchLevel").value());
    }
    @Test public void ownedCoursePromptsClassifyAmbiguousSelectionWithoutGuessingDay(){
        for(String prompt:List.of("Select a course to research","Select research level and start","Change Research Level")){
            var r=parse("Founja Jungle Day Night "+prompt);assertEquals(prompt,LiveReading.Scene.COURSE_SELECTION,r.scene());assertFalse(r.fields().containsKey("course"));
        }
        assertFalse(parse("Florio Nature Park Day Night Selected Research Level Lv. 2").fields().containsKey("course"));
        assertFalse(parse("Park (Day) Night Change Research Level").fields().containsKey("course"));
    }
    @Test public void ambiguousCoursePromptClearsEarlierVariantBeforeStartingAnotherRun(){
        LiveStabilizer stable=new LiveStabilizer();var ui=new SnapUiState();
        for(int n=1;n<=3;n++)stable.accept(parser.parse(words("Park Day Selected Research Level Lv. 2",.98f),false,n*500,"fixture","default"),.8f,3);
        assertEquals("park-day",ui.update("default","12",true,true,true,LiveReading.Scene.COURSE_SELECTION,1500,stable.snapshot(),1500).course());
        ui.update("default","12",true,true,true,LiveReading.Scene.EXPEDITION,2000,stable.snapshot(),2000);
        var next=parser.parse(words("Founja Jungle Day Night Select a course to research",.98f),false,3000,"fixture","default");stable.accept(next,.8f,3);
        assertEquals("",ui.update("default","12",true,true,true,next.scene(),3000,stable.snapshot(),3000).course());
    }
    @Test public void ownedSelectionPromptsOpenHelperWithoutNeedingTheTopLevelHelp(){
        for(String prompt:List.of("Choose photos to show to the professor","Show the selected photos to the professor","Have your photos chosen automatically","Remove checks from all photos","You cannot select a photo with no subject","You cannot select a photo in which the subject is not clearly visible","Remove check marks from selected photos"))assertEquals(prompt,LiveReading.Scene.PHOTO_SELECTION,parse(prompt).scene());
        assertEquals(LiveReading.Scene.PHOTO_SELECTION,parse("Choose photos to show to the professor Registered Photos Grookey").scene());
        assertEquals(LiveReading.Scene.PHOTO_SELECTION,parse("Select a Pokémon Grookey").scene());
        assertEquals(LiveReading.Scene.UNKNOWN,parse("Evaluate Photos Auto Remove All").scene());
        assertEquals(LiveReading.Scene.UNKNOWN,parse("Photodex Selected Compare All Total Grookey").scene());
    }
    @Test public void ownedEvaluationPauseAndResultsLabelsCannotTriggerSelection(){
        assertEquals(LiveReading.Scene.EVALUATION,parse("Professor’s Evaluation Photodex Photo New Photo Grookey").scene());
        assertEquals(LiveReading.Scene.PAUSED,parse("Pause Menu Continue Research Quit Research Evaluate Photos").scene());
        assertEquals(LiveReading.Scene.RESULTS,parse("Expedition Points Research Level Lv. 2 Course Score").scene());
        assertEquals(LiveReading.Scene.PHOTODEX,parse("Registered in your Photodex Choose a photo").scene());
    }
}
