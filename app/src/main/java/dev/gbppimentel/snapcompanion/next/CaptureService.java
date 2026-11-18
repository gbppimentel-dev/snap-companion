package dev.gbppimentel.snapcompanion.next;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.Display;
import java.nio.ByteBuffer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consent-gated, offline screen reader. One inference in flight; captured pixels never go to disk. */
public final class CaptureService extends Service {
    private static volatile CaptureService instance;
    private volatile boolean internalRestart;
    public static volatile boolean active,recognizedLatest;
    public static volatile LiveReading.Scene latestScene=LiveReading.Scene.UNKNOWN;
    public static volatile long sceneAt;
    public static volatile int requestedHz = 2;
    public static volatile long frames,lastFrameAt,readings,inferenceMs,dropped,duplicates;
    public static volatile long successfulInferenceMillis,failedInferenceMillis,inferenceFailures;
    public static volatile double measuredHz;
    public static volatile long cadenceMillis=500;
    static volatile int sourceWidth,sourceHeight;
    public static volatile String status = "Not authorized · capture stopped";
    private static Bitmap preview;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private HandlerThread worker;
    private Handler work;
    private OcrEngine ocr;
    private LiveParser parser;
    private LiveStore store;
    private LiveEvents events;
    private MenuDetector menus;
    private EdenFps performance;
    private final LiveStabilizer stabilizer=new LiveStabilizer();
    private final LiveContext context=new LiveContext();
    static SnapUiState.View currentContext(String profile,boolean running,boolean visible,long now){CaptureService c=instance;return c==null?new SnapUiState.View(running?(visible?"playing":"background"):"waiting","",0,0,null):c.context.view(profile,c.gameSource(),running,visible,now);}
    private android.content.SharedPreferences prefs;
    private String profileId="default";
    private long lastSample,windowStart,windowFrames,lastHash,lastOcrAt;
    private int repeatCount,width,height,density;
    private final Object imageLock=new Object();
    private Image pendingImage;
    private long pendingAt;
    private volatile LiveReading lastReading;
    private volatile String captureSource="";private static volatile String readingSource="";
    record FrameTicket(long generation,String source,long capturedAt){}
    private long frameGeneration;
    private int consecutiveFailures;
    private final String metricsSession=UUID.randomUUID().toString();
    synchronized FrameTicket ticket(long at){return new FrameTicket(frameGeneration,captureSource,at);}
    synchronized boolean accepts(FrameTicket ticket,long now){return !closing&&ticket!=null&&ticket.generation()==frameGeneration&&!ticket.source().isEmpty()&&ticket.source().equals(captureSource)&&ticket.source().equals(gameSource())&&ticket.capturedAt()>0&&now>=ticket.capturedAt()&&now-ticket.capturedAt()<=5000;}
    private String gameSource(){if(!EdenPerformance.supported())return "local:"+profileId;GameConnection connection=EdenLinkService.connection;return EdenLinkService.active&&connection.onScreen(System.currentTimeMillis())&&new ProfileStore(prefs).active().equals(profileId)?profileId+":"+connection.token():"";}
    static boolean currentSource(String profile,long now){CaptureService c=instance;return c!=null&&profile.equals(c.profileId)&&!readingSource.isEmpty()&&readingSource.equals(c.gameSource());}
    static LiveReading currentReading(){CaptureService c=instance;return c==null?null:c.lastReading;}
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(closing)return;
        try{
            if(!gamePermitted()){cadenceMillis=1000;synchronized(imageLock){if(pendingImage!=null){pendingImage.close();pendingImage=null;}}clearGameContext();return;}
            int thermal=getSystemService(PowerManager.class).getCurrentThermalStatus();boolean adaptive=prefs.getBoolean("adaptiveCapture",true);
            EdenFps.Sample fps=EdenFps.latest;
            Float gameFps=fps!=null&&fps.profile().equals(profileId)&&fps.fresh(System.currentTimeMillis())?fps.fps():null;
            Float gameMillis=fps!=null&&fps.profile().equals(profileId)&&fps.fresh(System.currentTimeMillis())?fps.frameMillis():null;
            cadenceMillis=CaptureCadence.interval(requestedHz,inferenceMs,thermal,adaptive,gameFps,gameMillis);
            cadenceMillis=Math.max(cadenceMillis,CaptureCadence.failureInterval(consecutiveFailures));
            if(prefs.getBoolean("pauseRecognition",false)||CaptureCadence.pause(thermal,adaptive)){synchronized(imageLock){if(pendingImage!=null){pendingImage.close();pendingImage=null;}}clearGameContext();status="Recognition paused · capture session retained";return;}
            String source=gameSource();if(!source.equals(captureSource)){clearGameContext();captureSource=source;synchronized(imageLock){if(pendingImage!=null){pendingImage.close();pendingImage=null;}}}
            if(ocr.busy()){dropped++;return;}
            Image image;long at;synchronized(imageLock){image=pendingImage;at=pendingAt;pendingImage=null;}
            if(image!=null)sample(image,at);
            else {LiveReading reading=lastReading;String heldSource=gameSource();if(reading!=null&&!heldSource.isEmpty()&&heldSource.equals(readingSource)&&heldSource.equals(captureSource)){List<LiveStabilizer.Stable> changed=stabilizer.held(reading,System.currentTimeMillis(),prefs.getInt("ocrThreshold",80)/100f);if(!changed.isEmpty()){context.observe(heldSource,reading,stabilizer.snapshot());store.publish(stabilizer.snapshot(),changed);}}}
        }catch(RuntimeException error){clearGameContext();cadenceMillis=1000;status="Recognition waiting to retry: "+error.getClass().getSimpleName();}finally{if(!closing)work.postDelayed(this,cadenceMillis);}
    }};
    private volatile boolean closing;
    static void stopForContextChange(Context context){CaptureService current=instance;if(current!=null)current.internalRestart=true;context.stopService(new Intent(context,CaptureService.class));}
    @Override public void onCreate(){super.onCreate();instance=this;}
    private boolean gamePermitted(){return !gameSource().isEmpty();}
    private synchronized void clearGameContext(){frameGeneration++;consecutiveFailures=0;boolean hadContext=lastReading!=null||!readingSource.isEmpty();recognizedLatest=false;latestScene=LiveReading.Scene.UNKNOWN;sceneAt=0;lastReading=null;readingSource="";stabilizer.clear();context.clear();if(store!=null&&hadContext)store.clearCurrent();if(events!=null)events.resetSession();EdenFps.latest=null;replacePreview(null);lastHash=lastOcrAt=0;repeatCount=0;status="Waiting for a verified Snap connection on the upper screen";}
    private final MediaProjection.Callback callback = new MediaProjection.Callback() {
        @Override public void onStop() { if(!closing&&!internalRestart)LiveSession.revoked();status = internalRestart?"Reconnecting after a context change":"Capture ended by Android or user"; stopSelf(); }
        @Override public void onCapturedContentResize(int w,int h) {if(work!=null)work.post(()->{if(EdenPerformance.supported()){android.graphics.Point size=primarySize();resize(size.x,size.y);}else resize(w,h);});}
    };
    public static synchronized Bitmap previewCopy() { return preview == null || preview.isRecycled() ? null : preview.copy(Bitmap.Config.ARGB_8888, false); }
    private static synchronized void replacePreview(Bitmap next) { if (preview != null) preview.recycle(); preview = next; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) { LiveSession.revoked();stopSelf(); return START_NOT_STICKY; }
        if (intent == null || active) { if (!active) stopSelf(); return START_NOT_STICKY; }
        requestedHz = intent.getIntExtra("hz", 2);
        if (requestedHz != 2 && requestedHz != 5 && requestedHz != 10) requestedHz = 2;
        try {
            NotificationManager notifications = getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel("capture", "Live View", NotificationManager.IMPORTANCE_LOW));
            PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, CaptureService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this, "capture").setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Snap Companion Next").setContentText("Live Companion").setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).setOngoing(true).build();
            startForeground(7, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            prefs=getSharedPreferences("snap",MODE_PRIVATE);profileId=new ProfileStore(prefs).active();store=new LiveStore(this);events=new LiveEvents(this);store.clearCurrent();
            try(InputStream in=getAssets().open("catalog.json");InputStream data=getAssets().open("guidebook.json")){Catalog catalog=new Catalog(new String(MainActivity.readBounded(in,2_000_000),StandardCharsets.UTF_8));new GuideBook(new String(MainActivity.readBounded(data,2_000_000),StandardCharsets.UTF_8),catalog).apply(catalog);parser=new LiveParser(catalog);}
            ocr=new OcrEngine();menus=new MenuDetector(this);performance=new EdenFps(this);
            Intent data = intent.getParcelableExtra("data");
            if (data == null) throw new IllegalArgumentException("Missing Android capture authorization");
            projection = getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("result", 0), data);
            if (projection == null) throw new IllegalStateException("Android declined capture");
            projection.registerCallback(callback, new Handler(Looper.getMainLooper()));
            // This Service can live on Thor's lower display. Its Resources are not the upper game display.
            Display primary=getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
            DisplayMetrics metrics=new DisplayMetrics();
            if(primary!=null)primary.getRealMetrics(metrics);else metrics.setTo(getResources().getDisplayMetrics());
            density=metrics.densityDpi;android.graphics.Point size=primarySize();setSize(size.x,size.y);
            worker=new HandlerThread("snap-capture");worker.start();work=new Handler(worker.getLooper());
            reader=newReader();
            frames=readings=inferenceMs=dropped=duplicates=lastFrameAt=sceneAt=successfulInferenceMillis=failedInferenceMillis=inferenceFailures=0;measuredHz=0;windowFrames=lastSample=0;windowStart=SystemClock.elapsedRealtimeNanos();
            display=projection.createVirtualDisplay("Snap offline input",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,work);
            active=true;recognizedLatest=false;work.post(tick);status="Waiting for recognizable English game text · target chosen in Android";
        } catch (Exception e) { status = "Capture could not start: " + e.getClass().getSimpleName(); stopSelf(); }
        return START_NOT_STICKY;
    }
    private void setSize(int w,int h){float scale=Math.min(1f,960f/Math.max(w,h));width=Math.max(2,Math.round(w*scale));height=Math.max(2,Math.round(h*scale));sourceWidth=width;sourceHeight=height;}
    private android.graphics.Point primarySize(){
        Display primary=getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        if(primary==null)return new android.graphics.Point(1920,1080);
        // getRealMetrics() can inherit the lower Activity's compatibility dimensions on the Thor.
        // The physical mode and primary display rotation identify the upper panel independently.
        Display.Mode mode=primary.getMode();int rotation=primary.getRotation();boolean sideways=rotation==android.view.Surface.ROTATION_90||rotation==android.view.Surface.ROTATION_270;
        return new android.graphics.Point(sideways?mode.getPhysicalHeight():mode.getPhysicalWidth(),sideways?mode.getPhysicalWidth():mode.getPhysicalHeight());
    }
    private ImageReader newReader(){ImageReader next=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,3);next.setOnImageAvailableListener(this::incoming,work);return next;}
    private void resize(int w,int h){
        if(closing || display==null || w<2 || h<2)return;
        int oldW=width,oldH=height;setSize(w,h);if(oldW==width&&oldH==height)return;
        try {ImageReader old=reader;ImageReader next=newReader();display.resize(width,height,density);display.setSurface(next.getSurface());reader=next;old.setOnImageAvailableListener(null,null);old.close();synchronized(imageLock){if(pendingImage!=null){pendingImage.close();pendingImage=null;}}clearGameContext();}
        catch(RuntimeException e){status="Screen resize interrupted capture; authorize again";stopSelf();}
    }
    private void incoming(ImageReader source) {
        try{Image newest=source.acquireLatestImage();if(newest==null)return;
            synchronized(imageLock){if(closing||source!=reader){newest.close();return;}if(pendingImage!=null)pendingImage.close();pendingImage=newest;pendingAt=System.currentTimeMillis();}
        }catch(RuntimeException e){if(!closing)status="Frame unavailable: "+e.getClass().getSimpleName();}
    }
    private void sample(Image image,long capturedAt) {
        try(image) {
            if(closing)return;
            if(!gamePermitted()){clearGameContext();return;}
            final String source=gameSource();if(source.isEmpty())return;
            long now=SystemClock.elapsedRealtimeNanos();
            Image.Plane plane=image.getPlanes()[0];ByteBuffer buffer=plane.getBuffer();int strideWidth=plane.getRowStride()/plane.getPixelStride();
            Bitmap padded=Bitmap.createBitmap(strideWidth,image.getHeight(),Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(buffer);
            // Ignore row padding before detecting black bars.
            Bitmap content=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight());if(content!=padded)padded.recycle();
            Rect crop=FrameCrop.bounds(content,prefs.getInt("cropHorizontal",0),prefs.getInt("cropVertical",0),prefs.getBoolean("cropBars",true));
            Bitmap frame=Bitmap.createBitmap(content,crop.left,crop.top,crop.width(),crop.height());if(frame!=content)content.recycle();
            frames++;windowFrames++;lastFrameAt=capturedAt;
            if(now-windowStart>=2_000_000_000L){measuredHz=windowFrames*1_000_000_000.0/(now-windowStart);windowStart=now;windowFrames=0;}
            long hash=FrameCrop.fingerprint(frame),at=lastFrameAt;
            if(hash==lastHash){repeatCount++;if(repeatCount>=3 && at-lastOcrAt<2000){duplicates++;frame.recycle();return;}}else{lastHash=hash;repeatCount=0;}
            lastOcrAt=at;
            Bitmap candidate=prefs.getBoolean("livePreview",false)?frame.copy(Bitmap.Config.ARGB_8888,false):null;
            FrameTicket ticket=ticket(at);
            boolean hud=HudDetector.camera(frame);boolean camp=menus.camp(frame);long started=SystemClock.elapsedRealtime();
            boolean accepted=ocr.read(frame,hud,EdenPerformance.supported()&&EdenLinkService.visible(System.currentTimeMillis()),new OcrEngine.Callback(){
                @Override public void success(List<LiveReading.Word> words,long milliseconds){
                    enqueueSuccess(ticket,words,milliseconds,hud,camp,candidate);
                }
                @Override public void failure(String reason){long cost=SystemClock.elapsedRealtime()-started;postResult(()->failed(ticket,cost,reason,candidate),candidate);}
            });
            if(!accepted){frame.recycle();if(candidate!=null)candidate.recycle();dropped++;}
        }catch(RuntimeException e){if(!closing)status="Frame unavailable: "+e.getClass().getSimpleName();}
    }
    private static void discard(Bitmap bitmap){if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
    private void postResult(Runnable result,Bitmap candidate){Handler handler=work;if(closing||handler==null||!handler.post(result))discard(candidate);}
    void enqueueSuccess(FrameTicket ticket,List<LiveReading.Word> words,long milliseconds,boolean hud,boolean camp,Bitmap candidate){postResult(()->succeeded(ticket,words,milliseconds,hud,camp,candidate),candidate);}
    private synchronized void failed(FrameTicket ticket,long milliseconds,String reason,Bitmap candidate){
        discard(candidate);if(closing)return;inferenceFailures++;inferenceMs=Math.max(0,milliseconds);failedInferenceMillis+=inferenceMs;
        if(!accepts(ticket,System.currentTimeMillis()))return;
        consecutiveFailures=Math.min(5,consecutiveFailures+1);status="Recognition waiting to retry: "+reason;
    }
    private synchronized void succeeded(FrameTicket ticket,List<LiveReading.Word> words,long milliseconds,boolean hud,boolean camp,Bitmap candidate){
        if(closing){discard(candidate);return;}inferenceMs=Math.max(0,milliseconds);successfulInferenceMillis+=inferenceMs;readings++;
        if(!accepts(ticket,System.currentTimeMillis())){discard(candidate);return;}consecutiveFailures=0;
        try{
            long at=ticket.capturedAt();LiveReading reading=parser.parse(words,hud,at,"Screen · bundled Latin OCR",profileId);if(camp&&reading.scene()==LiveReading.Scene.UNKNOWN)reading=new LiveReading(at,"Screen · three Camp icon masks",profileId,LiveReading.Scene.MENU,Map.of());
            lastReading=reading;readingSource=ticket.source();latestScene=reading.scene();sceneAt=at;
            if(performance!=null&&EdenPerformance.supported()&&EdenLinkService.visible(System.currentTimeMillis()))performance.accept(words,at,profileId,reading.scene());
            if(reading.scene()==LiveReading.Scene.MENU&&EdenPerformance.supported())LiveSession.verifyGame(CaptureService.this,EdenLinkService.gamePid);
            if(reading.recognizedGame())replacePreview(candidate);else{discard(candidate);replacePreview(null);}
            List<LiveStabilizer.Stable> changed=stabilizer.accept(reading,prefs.getInt("ocrThreshold",80)/100f,3);context.observe(ticket.source(),reading,stabilizer.snapshot());
            recognizedLatest=reading.recognizedGame()&&reading.scene()!=LiveReading.Scene.PAUSED;
            if(reading.recognizedGame()){store.publish(stabilizer.snapshot(),changed);if(events!=null)events.accept(reading,stabilizer.snapshot());}
            status=reading.recognizedGame()?"Text anchors: "+reading.scene():"No supported game text · previous values are aging";
        }catch(RuntimeException error){discard(candidate);clearGameContext();status="Recognition waiting to retry: "+error.getClass().getSimpleName();}
    }
    /** Standard ADB/service diagnostics only; no raw OCR, images, profile IDs or saved records. */
    @Override protected synchronized void dump(FileDescriptor fd,PrintWriter out,String[]args){
        try{long now=System.currentTimeMillis();boolean current=prefs!=null&&!readingSource.isEmpty()&&readingSource.equals(gameSource());EdenFps.Sample sample=EdenFps.latest;boolean hasFps=current&&sample!=null&&sample.profile().equals(profileId)&&sample.fresh(now);org.json.JSONObject r=new org.json.JSONObject().put("schema",2).put("kind","capture").put("session",metricsSession).put("sourceEpoch",frameGeneration).put("gameEpoch",EdenLinkService.connection.generation()).put("elapsedRealtimeMillis",SystemClock.elapsedRealtime()).put("active",active).put("sourceCurrent",current).put("reportedFps",hasFps&&sample.fps()!=null?sample.fps():org.json.JSONObject.NULL).put("reportedFrameMillis",hasFps&&sample.frameMillis()!=null?sample.frameMillis():org.json.JSONObject.NULL).put("targetHz",requestedHz).put("intervalMillis",cadenceMillis).put("frames",frames).put("readings",readings).put("lastInferenceMillis",inferenceMs).put("successfulInferenceMillis",successfulInferenceMillis).put("failedInferenceMillis",failedInferenceMillis).put("inferenceFailures",inferenceFailures).put("dropped",dropped).put("duplicates",duplicates).put("frameAgeMillis",lastFrameAt>0&&now>=lastFrameAt?now-lastFrameAt:-1).put("width",sourceWidth).put("height",sourceHeight);
            out.println("snap-metrics|"+r);
        }catch(Exception e){out.println("snap-metrics|{\"schema\":2,\"kind\":\"capture\",\"available\":false}");}
    }
    @Override public void onDestroy(){
        if(instance==this)instance=null;
        synchronized(this){closing=true;frameGeneration++;active=false;recognizedLatest=false;latestScene=LiveReading.Scene.UNKNOWN;readingSource="";}
        if(work!=null)work.removeCallbacksAndMessages(null);
        synchronized(imageLock){if(pendingImage!=null){pendingImage.close();pendingImage=null;}}
        if(display!=null)display.release();
        if(reader!=null){reader.setOnImageAvailableListener(null,null);reader.close();}
        if(projection!=null){projection.unregisterCallback(callback);projection.stop();}
        if(ocr!=null)ocr.close();if(performance!=null)performance.close();if(worker!=null)worker.quitSafely();replacePreview(null);
        if(status.startsWith("Capture ended")||status.startsWith("Screen resize"))status+=" · no frames retained";
        else if(!status.startsWith("Capture could"))status="Capture stopped · values below are historical; no frames retained";
        super.onDestroy();
    }
}
