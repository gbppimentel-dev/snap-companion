package dev.gbppimentel.snapcompanion;
import java.nio.file.*;import java.util.*;import org.junit.*;import static org.junit.Assert.*;
/** Parser/stabilizer/output replay, including rapid photo changes and weak/unrelated OCR. */
public class SelectionContinuityTest {
 private LiveParser parser;private final LiveStabilizer stable=new LiveStabilizer();
 @Before public void setup()throws Exception{parser=new LiveParser(new Catalog(new String(Files.readAllBytes(Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8)));}
 private LiveReading read(String text,long at){return parser.parse(List.of(new LiveReading.Word(text,.1,.2,.8,.3,.96f)),false,at,"fixture","default");}
 private LiveReading feed(String text,long at){var r=read(text,at);stable.accept(r,.8f,3);return r;}
 @Test public void anotherPokemonWithTheSameStarCategoryNeedsItsOwnConfirmation()throws Exception{
  LiveReading r=null;for(int i=0;i<3;i++)r=feed("Choose a photo Grookey 3 star Photos taken 24",1000+i*500);assertEquals(810,PhotoPhaseState.read(r,stable.snapshot(),"default",2100).getInt("subject"));
  r=feed("Choose a photo Pikachu 3 star Photos taken 24",2500);assertFalse(PhotoPhaseState.read(r,stable.snapshot(),"default",2600).has("category"));assertFalse(stable.snapshot().containsKey("subject"));
  for(int i=0;i<2;i++)r=feed("Choose a photo Pikachu 3 star Photos taken 24",3000+i*500);var out=PhotoPhaseState.read(r,stable.snapshot(),"default",3600);assertEquals(25,out.getInt("subject"));assertEquals(3,out.getInt("category"));
 }
 @Test public void missingSubjectAndUnrecognizedScreenClearSelectionEvidence()throws Exception{
  for(int i=0;i<3;i++)feed("Choose a photo Grookey 3 star",1000+i*500);var missing=feed("Choose a photo 3 star",2500);assertFalse(PhotoPhaseState.read(missing,stable.snapshot(),"default",2600).has("category"));
  for(int i=0;i<3;i++)feed("Choose a photo Grookey 3 star",3000+i*500);feed("unrelated screen",4500);var r=feed("Choose a photo Grookey 3 star",5000);assertEquals(0,PhotoPhaseState.read(r,stable.snapshot(),"default",5100).length());
 }
 @Test public void selectionFieldsUseTheirOwnConfidenceAndIgnoreUnrelatedWeakText(){
  var words=new ArrayList<LiveReading.Word>();words.add(new LiveReading.Word("Choose a photo Grookey 3 star Photos taken 24",.1,.2,.8,.3,.95f));for(int i=0;i<20;i++)words.add(new LiveReading.Word("blurred",.1,.4,.3,.45,.1f));var r=parser.parse(words,false,1000,"fixture","default");assertEquals(.95f,r.fields().get("category").confidence(),.001f);assertEquals(.95f,r.fields().get("photosTaken").confidence(),.001f);
  words= new ArrayList<>(List.of(new LiveReading.Word("Choose a photo Grookey",.1,.1,.8,.15,.99f),new LiveReading.Word("3 star",.1,.2,.3,.25,.4f),new LiveReading.Word("Photos taken",.1,.3,.3,.35,.99f),new LiveReading.Word("24",.4,.3,.5,.35,.4f)));r=parser.parse(words,false,1000,"fixture","default");assertEquals(.4f,r.fields().get("category").confidence(),.001f);assertEquals(.4f,r.fields().get("photosTaken").confidence(),.001f);
 }
 @Test public void ownedIlluminaRestrictionKeepsSelectionOpenWithoutInventingASubject(){var r=read("You cannot select photos in which the Illumina phenomenon isn’t shown.",1000);assertEquals(LiveReading.Scene.PHOTO_SELECTION,r.scene());assertFalse(r.fields().containsKey("subject"));}
}
