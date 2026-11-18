package dev.gbppimentel.snapcompanion;

import android.content.Context;
import android.graphics.*;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original-font synthetic plates through the actual bundled model. Not gameplay acceptance. */
public final class CourseOcrDeviceTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private LiveParser parser()throws Exception{try(var in=context().getAssets().open("catalog.json")){return new LiveParser(new Catalog(new String(MainActivity.readBounded(in,2000000),StandardCharsets.UTF_8)));}}
    private Bitmap plate(String... lines){
        Bitmap b=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);b.eraseColor(0xfff3edda);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(Typeface.createFromAsset(context().getAssets(),"game-art/fonts/snap-heading.ttf"));p.setColor(0xff35483d);p.setTextSize(27);
        int y=125;for(String line:lines){c.drawText(line,80,y,p);y+=57;}return b;
    }
    private LiveReading read(Bitmap b)throws Exception{
        var engine=new OcrEngine();var done=new CountDownLatch(1);List<LiveReading.Word> words=new ArrayList<>();List<String> failures=new ArrayList<>();
        try{assertTrue(engine.read(b,new OcrEngine.Callback(){public void success(List<LiveReading.Word> value,long ms){words.addAll(value);done.countDown();}public void failure(String reason){failures.add(reason);done.countDown();}}));assertTrue(done.await(40,TimeUnit.SECONDS));assertTrue(failures.toString(),failures.isEmpty());}
        finally{engine.close();}
        var r=parser().parse(words,false,1000,"original-font synthetic plate","default");android.os.Bundle report=new android.os.Bundle();report.putString("stream",r.toString()+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,report);return r;
    }
    @Test public void transitionTitleAndNightUseTheBundledModel()throws Exception{
        var r=read(plate("Florio Nature Park","Park (Night)"));assertEquals(r.toString(),LiveReading.Scene.COURSE_INTRO,r.scene());assertEquals("park-night",r.fields().get("course").value());
        var stable=new LiveStabilizer();stable.accept(r,.8f,3);stable.held(r,2100,.8f);assertEquals(1,stable.snapshot().get("course").frames());assertEquals("park-night",stable.snapshot().get("course").value());
    }
    @Test public void gamePauseProvidesExplicitCourseAndSelectedLevel()throws Exception{
        var r=read(plate("Pause Menu","Continue Research","Founja Jungle (Night)","Research Level Lv. 2"));assertEquals(r.toString(),LiveReading.Scene.PAUSED,r.scene());assertEquals("jungle-night",r.fields().get("course").value());assertEquals("2",r.fields().get("researchLevel").value());assertTrue(r.fields().get("course").confidence()>=.8f);
    }
    @Test public void nextResearchLevelIsNotTheSelectedLevel()throws Exception{
        var r=read(plate("Park (Night)","Next research level: 2"));assertEquals(r.toString(),"park-night",r.fields().get("course").value());assertFalse(r.fields().containsKey("researchLevel"));
    }
}
