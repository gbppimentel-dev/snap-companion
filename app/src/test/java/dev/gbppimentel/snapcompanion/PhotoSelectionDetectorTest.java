package dev.gbppimentel.snapcompanion;

import android.graphics.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PhotoSelectionDetectorTest {
    private LiveParser parser;
    private static final int GOLD=Color.rgb(250,184,12);
    @Before public void setup()throws Exception{parser=new LiveParser(new Catalog(new String(Files.readAllBytes(java.nio.file.Path.of("src/main/assets/catalog.json")),java.nio.charset.StandardCharsets.UTF_8)));}
    private LiveReading.Word word(String text,double l,double t,double r,double b,float q){return new LiveReading.Word(text,l,t,r,b,q);}
    private List<LiveReading.Word> words(){return List.of(
        word("Dodrio",.12,.30,.20,.34,.96f),word("Pichu",.30,.30,.37,.34,.97f),
        word("Auto",.39,.83,.46,.88,.98f),word("Evaluate",.74,.84,.83,.88,.98f),word("Photos",.84,.84,.92,.88,.98f));}
    private Bitmap board(boolean first){Bitmap b=Bitmap.createBitmap(1000,600,Bitmap.Config.ARGB_8888);b.eraseColor(Color.rgb(119,169,74));Canvas c=new Canvas(b);Paint p=new Paint();p.setColor(first?GOLD:Color.WHITE);c.drawRect(90,130,235,235,p);p.setColor(first?Color.WHITE:GOLD);c.drawRect(270,130,415,235,p);return b;}
    private LiveReading read(Bitmap b,List<LiveReading.Word> words,long at){return parser.parse(PhotoSelectionDetector.annotate(b,words),false,at,"synthetic selection","default");}
    @Test public void goldNameCardSelectsOneOfSeveralVisiblePokemonWithoutTheHelpSentence(){
        try(BitmapClose b=new BitmapClose(board(true))){var tagged=PhotoSelectionDetector.annotate(b.value,words());assertEquals(1,tagged.stream().filter(LiveReading.Word::selectionProbe).count());var r=read(b.value,words(),1000);assertEquals(LiveReading.Scene.PHOTO_SELECTION,r.scene());assertEquals("85",r.fields().get("subject").value());assertEquals(.96f,r.fields().get("subject").confidence(),.001f);}
        try(BitmapClose b=new BitmapClose(board(false))){assertEquals("172",read(b.value,words(),1500).fields().get("subject").value());}
    }
    @Test public void weakHighlightedNameNeverFallsBackToTheOtherStrongName(){
        try(BitmapClose b=new BitmapClose(board(true))){var words=new ArrayList<>(words());words.set(0,word("Dodrio",.12,.30,.20,.34,.45f));var r=read(b.value,words,1000);assertEquals("85",r.fields().get("subject").value());assertEquals(.45f,r.fields().get("subject").confidence(),.001f);var stable=new LiveStabilizer();for(int i=0;i<3;i++)stable.accept(read(b.value,words,1000+i*500),.8f,3);assertFalse(stable.snapshot().containsKey("subject"));}
    }
    @Test public void unreadableHighlightedTextDoesNotSelectAnUnhighlightedPokemon(){
        try(BitmapClose b=new BitmapClose(board(true))){var words=new ArrayList<>(words());words.set(0,word("D0drio?",.12,.30,.20,.34,.96f));assertFalse(read(b.value,words,1000).fields().containsKey("subject"));}
    }
    @Test public void noHighlightOrTwoHighlightsRemainAmbiguous(){
        try(BitmapClose b=new BitmapClose(board(true))){b.value.eraseColor(Color.WHITE);assertEquals(0,PhotoSelectionDetector.annotate(b.value,words()).stream().filter(LiveReading.Word::selectionProbe).count());assertEquals(LiveReading.Scene.UNKNOWN,read(b.value,words(),1000).scene());b.value.eraseColor(GOLD);assertFalse(read(b.value,words(),1500).fields().containsKey("subject"));}
    }
    @Test public void colorAloneWithoutOwnedSelectionControlsCannotTagNames(){
        try(BitmapClose b=new BitmapClose(board(true))){var input=words().subList(0,2);assertTrue(PhotoSelectionDetector.annotate(b.value,input).stream().noneMatch(LiveReading.Word::selectionProbe));assertEquals(LiveReading.Scene.UNKNOWN,read(b.value,input,1000).scene());}
    }
    @Test public void footerAndPartialGoldEdgesAreNotSelectionLabels(){
        try(BitmapClose b=new BitmapClose(board(true))){var bottom=new ArrayList<>(words());bottom.set(0,word("Dodrio",.12,.85,.20,.89,.96f));assertFalse(PhotoSelectionDetector.annotate(b.value,bottom).get(0).selectionProbe());b.value.eraseColor(Color.WHITE);Paint p=new Paint();p.setColor(GOLD);new Canvas(b.value).drawRect(90,130,235,192,p);assertFalse(PhotoSelectionDetector.annotate(b.value,words()).get(0).selectionProbe());}
    }
    @Test public void splitMultiwordNamesUseOnlyTheHighlightedWords(){
        try(BitmapClose b=new BitmapClose(board(true))){var input=new ArrayList<>(words());input.remove(0);input.add(word("Ho-",.12,.30,.15,.34,.95f));input.add(word("Oh",.16,.30,.21,.34,.94f));var r=read(b.value,input,1000);assertEquals("250",r.fields().get("subject").value());assertEquals(.94f,r.fields().get("subject").confidence(),.001f);}
    }
    private static class BitmapClose implements AutoCloseable{final Bitmap value;BitmapClose(Bitmap value){this.value=value;}public void close(){value.recycle();}}
}
