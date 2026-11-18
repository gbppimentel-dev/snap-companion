package dev.gbppimentel.snapcompanion;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** One terminal callback and resource release, including exceptions in asynchronous stages. */
final class OcrDelivery implements OcrEngine.Callback {
    private final OcrEngine.Callback callback;
    private final Runnable release;
    private final AtomicBoolean ended=new AtomicBoolean();
    OcrDelivery(OcrEngine.Callback callback,Runnable release){this.callback=callback;this.release=release;}
    void run(Runnable stage){if(ended.get())return;try{stage.run();}catch(RuntimeException error){failure(error.getClass().getSimpleName());}}
    @Override public void success(List<LiveReading.Word> words,long milliseconds){
        if(!ended.compareAndSet(false,true))return;
        try{callback.success(words,milliseconds);}finally{release.run();}
    }
    @Override public void failure(String reason){
        if(!ended.compareAndSet(false,true))return;
        try{callback.failure(reason);}finally{release.run();}
    }
}
