package dev.gbppimentel.snapcompanion;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Color;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bundled, offline Latin OCR. Owns every accepted bitmap until inference finishes. No queued frames. */
public final class OcrEngine implements AutoCloseable {
    public interface Callback { void success(List<LiveReading.Word> words, long milliseconds); void failure(String reason); }
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final AtomicBoolean busy = new AtomicBoolean();
    private boolean closed;
    private long overlayAt;
    public boolean busy() { return busy.get(); }
    public boolean read(Bitmap bitmap, Callback callback) {return read(bitmap,false,callback);}
    public synchronized boolean read(Bitmap bitmap,boolean cameraHud, Callback callback) {
        return read(bitmap,cameraHud,false,callback);
    }
    public synchronized boolean read(Bitmap bitmap,boolean cameraHud,boolean edenOverlay, Callback callback) {
        if (closed || !busy.compareAndSet(false,true)) return false;
        OcrDelivery delivery=new OcrDelivery(callback,()->finish(bitmap));
        long start = android.os.SystemClock.elapsedRealtime();
        delivery.run(()->{
            int width=bitmap.getWidth(),height=bitmap.getHeight();
            recognizer.process(InputImage.fromBitmap(bitmap,0))
                .addOnSuccessListener(Runnable::run, result -> delivery.run(()->{
                    List<LiveReading.Word> words=new ArrayList<>();
                    for(Text.TextBlock block:result.getTextBlocks()) for(Text.Line line:block.getLines()) for(Text.Element e:line.getElements()) {
                        Rect b=e.getBoundingBox();
                        if(b!=null)words.add(new LiveReading.Word(e.getText(),b.left/(double)width,b.top/(double)height,b.right/(double)width,b.bottom/(double)height,e.getConfidence()));
                    }
                    EdenFps.Reading performance=edenOverlay?EdenFps.parse(words):null;
                    if(edenOverlay&&(performance==null||performance.fps()==null)&&start-overlayAt>=1000){overlayAt=start;readOverlay(bitmap,words,start,cameraHud,delivery);}
                    else deliver(bitmap,words,start,cameraHud,delivery);
                }))
                .addOnFailureListener(Runnable::run,e -> delivery.failure(e.getClass().getSimpleName()));
        });
        return true;
    }
    private void deliver(Bitmap bitmap,List<LiveReading.Word> words,long start,boolean cameraHud,OcrDelivery callback){if(cameraHud)readFilm(bitmap,words,start,callback);else callback.success(List.copyOf(words),android.os.SystemClock.elapsedRealtime()-start);}
    private void readOverlay(Bitmap bitmap,List<LiveReading.Word> words,long start,boolean cameraHud,OcrDelivery callback){
        // Eden's small magenta overlay disappears in a full-frame 960px read. Reuse this
        // frame, isolate the configured-color pixels and magnify; at most once/second.
        int w=Math.max(1,Math.round(bitmap.getWidth()*.28f)),h=Math.max(1,Math.round(bitmap.getHeight()*.07f));int[] pixels=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);
        for(int i=0;i<pixels.length;i++){int c=pixels[i],r=Color.red(c),g=Color.green(c),b=Color.blue(c);pixels[i]=r>70&&r>g*1.3f&&b>g*1.2f?Color.BLACK:Color.WHITE;}
        Bitmap mask=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888),large=Bitmap.createScaledBitmap(mask,w*4,h*4,false);mask.recycle();Bitmap input=Bitmap.createBitmap(w*4+64,h*4+48,Bitmap.Config.ARGB_8888);input.eraseColor(Color.WHITE);new Canvas(input).drawBitmap(large,32,24,null);large.recycle();
        try{recognizer.process(InputImage.fromBitmap(input,0)).addOnSuccessListener(Runnable::run,result->callback.run(()->{
            List<LiveReading.Word> candidate=new ArrayList<>();for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines())for(Text.Element e:line.getElements()){Rect r=e.getBoundingBox();if(r!=null)candidate.add(new LiveReading.Word(e.getText(),(r.left-32)/4.0/bitmap.getWidth(),(r.top-24)/4.0/bitmap.getHeight(),(r.right-32)/4.0/bitmap.getWidth(),(r.bottom-24)/4.0/bitmap.getHeight(),e.getConfidence()));}
            if(EdenFps.parse(candidate)!=null)words.addAll(candidate);deliver(bitmap,words,start,cameraHud,callback);
        })).addOnFailureListener(Runnable::run,e->callback.run(()->deliver(bitmap,words,start,cameraHud,callback))).addOnCompleteListener(Runnable::run,t->input.recycle());}
        catch(RuntimeException e){input.recycle();deliver(bitmap,words,start,cameraHud,callback);}
    }
    private record Film(String value,float confidence){}
    private void readFilm(Bitmap bitmap,List<LiveReading.Word> words,long start,OcrDelivery callback){filmPass(bitmap,words,start,callback,0,null);}
    private void filmPass(Bitmap bitmap,List<LiveReading.Word> words,long start,OcrDelivery callback,int pass,Film first) {
        // A low-contrast cross-check must agree with the high-contrast reading. Report the high-contrast model score; repetition never raises it.
        int x=Math.round(bitmap.getWidth()*.924f),y=Math.round(bitmap.getHeight()*.022f);
        int width=Math.max(1,Math.round(bitmap.getWidth()*.040f)),height=Math.max(1,Math.round(bitmap.getHeight()*.045f));
        Bitmap crop=Bitmap.createBitmap(bitmap,x,y,width,height);int[] pixels=new int[width*height];crop.getPixels(pixels,0,width,0,0,width,height);crop.recycle();
        int threshold=pass==0?130:210;
        for(int i=0;i<pixels.length;i++){int c=pixels[i],r=(c>>16)&255,g=(c>>8)&255,b=c&255;pixels[i]=Math.min(r,Math.min(g,b))>=threshold?Color.BLACK:Color.WHITE;}
        Bitmap binary=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888),enlarged=Bitmap.createScaledBitmap(binary,width*4,height*4,false);binary.recycle();
        Bitmap scaled=Bitmap.createBitmap(width*4+128,height*4+96,Bitmap.Config.ARGB_8888);scaled.eraseColor(Color.WHITE);new Canvas(scaled).drawBitmap(enlarged,64,48,null);enlarged.recycle();
        try{recognizer.process(InputImage.fromBitmap(scaled,0)).addOnSuccessListener(Runnable::run,result->callback.run(()->{
            Map<String,Float> candidates=new LinkedHashMap<>();
            for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines())for(Text.Element e:line.getElements())if(e.getText().matches("[0-9]{1,2}")&&Integer.parseInt(e.getText())<=72&&e.getConfidence()>=.70f){String value=Integer.toString(Integer.parseInt(e.getText()));candidates.merge(value,e.getConfidence(),Math::min);}
            Film film=candidates.size()==1?new Film(candidates.keySet().iterator().next(),candidates.values().iterator().next()):null;
            if(pass==0){filmPass(bitmap,words,start,callback,1,film);return;}
            if(first!=null&&film!=null&&first.value().equals(film.value()))words.add(new LiveReading.Word(film.value(),.924,.022,.964,.067,film.confidence(),true));callback.success(List.copyOf(words),android.os.SystemClock.elapsedRealtime()-start);
        })).addOnFailureListener(Runnable::run,e->callback.failure(e.getClass().getSimpleName())).addOnCompleteListener(Runnable::run,task->scaled.recycle());}
        catch(RuntimeException e){scaled.recycle();callback.failure(e.getClass().getSimpleName());}
    }
    private synchronized void finish(Bitmap bitmap) { if(!bitmap.isRecycled())bitmap.recycle();busy.set(false);if(closed)recognizer.close(); }
    @Override public synchronized void close() {closed=true;if(!busy.get())recognizer.close();}
}
