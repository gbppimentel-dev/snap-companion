package dev.gbppimentel.snapcompanion;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.FrameLayout;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** The product shell: a local, asset-backed dashboard with no setup or bookkeeping pages. */
public final class MainActivity extends Activity {
    private static final String PAGE="file:///android_asset/ui/index.html",PHOTO_HOST="snap.companion.invalid";
    private ControllerFeed controllerFeed;private GameMemoryLink gameMemory;
    private WebView web;private boolean visible,loaded,reading,closed;private long viewGeneration;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor(),performanceIo=Executors.newSingleThreadExecutor();
    private EdenLinkStore saves;private EdenRequests requests;private CourseResearch research;private ProfileStore profiles;
    private volatile Map<String,File> photoFiles=Map.of();
    private String lastJson="";private long imagesAt,seenGallery;private String imageSource="";
    private Map<String,PersonalGallery.Photo> cachedImages=Map.of();
    private final PriorityMonitor priority=new PriorityMonitor(new PriorityMonitor.Scheduler(){public void post(Runnable r,long delay){main.postDelayed(r,delay);}public void cancel(Runnable r){main.removeCallbacks(r);}},performanceIo,EdenPerformance::refresh,EdenPerformance::release);
    private final Runnable pulse=new Runnable(){public void run(){if(!visible||closed)return;gameMemory.beat();refresh();main.postDelayed(this,500);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);EdenPerformance.initialize(getCacheDir());saves=new EdenLinkStore(this);requests=new EdenRequests(this);research=new CourseResearch(this);profiles=new ProfileStore(getSharedPreferences("snap",0));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xFFF4F0E4);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);settings.setSupportMultipleWindows(false);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);settings.setTextZoom(Math.round(100*getResources().getConfiguration().fontScale));
        web.addJavascriptInterface(new TouchFeedback(),"SnapTouch");
        web.setBackgroundColor(Color.TRANSPARENT);web.setOverScrollMode(View.OVER_SCROLL_NEVER);web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){if(PAGE.equals(url)){loaded=true;viewGeneration++;lastJson="";web.evaluateJavascript("window.SnapApp.visibility("+visible+")",null);refresh();}}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !PAGE.equals(request.getUrl().toString());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return resource(request.getUrl());}
        });
        gameMemory=new GameMemoryLink(this);
        controllerFeed=new ControllerFeed(this,json->main.post(()->{if(visible&&loaded&&!closed&&controllerFeed.current(json))web.evaluateJavascript("window.SnapApp.controller("+json+")",null);}));
        web.loadUrl(PAGE);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
    }
    @Override protected void onStart(){super.onStart();visible=true;viewGeneration++;if(loaded)web.evaluateJavascript("window.SnapApp.visibility(true)",null);controllerFeed.start();gameMemory.start();LiveSession.appOpened(this);if(EdenPerformance.supported())try{startForegroundService(new Intent(this,EdenLinkService.class));}catch(RuntimeException ignored){}main.removeCallbacks(pulse);main.post(pulse);}
    @Override protected void onResume(){super.onResume();if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}if(EdenPerformance.supported())priority.start();}
    @Override protected void onPause(){priority.stop();super.onPause();}
    @Override protected void onStop(){visible=false;viewGeneration++;controllerFeed.stop();gameMemory.stop();main.removeCallbacks(pulse);if(loaded&&!closed)web.evaluateJavascript("window.SnapApp.visibility(false)",null);super.onStop();}
    @Override protected void onDestroy(){closed=true;if(controllerFeed!=null)controllerFeed.close();if(gameMemory!=null)gameMemory.close();priority.stop();main.removeCallbacksAndMessages(null);io.shutdownNow();performanceIo.shutdownNow();if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
    // Android 13+ uses the registered gesture callback; this is the API30–32 fallback.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){back();}
    public final class TouchFeedback { @JavascriptInterface public void tap(){main.post(()->{if(visible&&!closed)web.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);});} }
    private void back(){if(!loaded){finish();return;}web.evaluateJavascript("Boolean(window.SnapApp && SnapApp.back())",result->{if(!"true".equals(result))finish();});}
    private void refresh(){
        if(!loaded||reading||closed||!visible)return;reading=true;long generation=viewGeneration;
        io.execute(()->{String json=null;try{json=snapshot().toString();}catch(Exception ignored){}String next=json;main.post(()->{reading=false;if(closed||!loaded||!visible||generation!=viewGeneration||next==null)return;if(!next.equals(lastJson)){lastJson=next;web.evaluateJavascript("window.SnapApp.update("+next+")",null);}});});
    }
    JSONObject snapshot()throws Exception{
        long now=System.currentTimeMillis();String profile=profiles.active();Map<String,EdenPhotodex.Photo> photos=saves.photos(profile);
        boolean connected=EdenLinkService.trusted(now);
        boolean fresh=CaptureService.currentSource(profile,now)&&CaptureService.active&&now>=CaptureService.sceneAt&&now-CaptureService.sceneAt<=5000&&CaptureService.sceneAt>0;
        var stable=new LiveStore(this).snapshot();var reading=CaptureService.currentReading();
        var current=CaptureService.currentContext(profile,connected,EdenLinkService.gameVisible,now);
        JSONObject memory=gameMemory==null?new JSONObject():gameMemory.current(now);
        if(connected&&EdenLinkService.gameVisible&&memory.optString("status").equals("ready")){
            String phase=Set.of("selection","evaluation","results").contains(current.phase())?current.phase():"research";
            current=new SnapUiState.View(phase,memory.getString("course"),memory.getInt("level"),current.subject(),current.film());
        }else if(memory.optString("status").equals("idle")&&Set.of("research","playing","paused").contains(current.phase())){
            current=new SnapUiState.View(current.phase(),"",0,current.subject(),current.film());
        }
        String user=saves.bound(profile),imageKey=profile+":"+user+":"+saves.hash(profile);
        if(!imageSource.equals(imageKey)||now<imagesAt||now-imagesAt>3000||AutoGallery.changedAt!=seenGallery){PhotodexImages.Index index=new PhotodexImages.Index(Map.of(),Map.of());try{if(!photos.isEmpty()&&!user.isEmpty()){PersonalGallery gallery=new PersonalGallery(this,profile);index=PhotodexImages.current(photos.values(),user,gallery.all(),gallery::file);}}catch(IOException ignored){}cachedImages=index.images();photoFiles=index.files();imageSource=imageKey;imagesAt=now;seenGallery=AutoGallery.changedAt;}
        JSONObject collection=new JSONObject();for(var p:photos.values()){JSONObject row=new JSONObject().put("score",p.score()).put("medal",p.medal()).put("components",new JSONArray(p.components()));var image=cachedImages.get(AutoGallery.identity(user,p));if(image!=null)row.put("image","https://"+PHOTO_HOST+"/photo/"+image.id());collection.put(p.key(),row);}
        EdenRequests.Snapshot received=requests.snapshot(profile);JSONArray ids=new JSONArray(),completed=new JSONArray(),rewardReady=new JSONArray();if(received!=null){for(String id:new TreeSet<>(received.received()))ids.put(id);for(String id:new TreeSet<>(received.completed()))completed.put(id);for(String id:new TreeSet<>(received.rewardReady()))rewardReady.put(id);}
        EdenFps.Sample fps=EdenFps.latest;Float value=connected&&EdenLinkService.gameVisible&&CaptureService.active&&fps!=null&&fresh&&fps.profile().equals(profile)&&fps.fresh(now)?fps.fps():null;
        JSONObject out=new JSONObject().put("sourceKey",profile+":"+saves.bound(profile)).put("connected",connected).put("visible",connected&&EdenLinkService.gameVisible).put("phase",current.phase()).put("course",current.course()).put("level",current.level()).put("subject",current.subject()).put("film",current.film()==null?JSONObject.NULL:current.film()).put("fps",value==null?JSONObject.NULL:value).put("playtime",CocoonTime.latest).put("checkedAt",EdenLinkService.lastConnectedAt).put("syncedAt",saves.updatedAt(profile)).put("requestSyncedAt",requests.updatedAt(profile)).put("requestVersion",saves.hash(profile)).put("requestsKnown",received!=null).put("received",ids).put("completed",completed).put("rewardReady",rewardReady).put("rewardConditionsKnown",received!=null&&received.rewardConditionsKnown()).put("completionsKnown",received!=null&&received.completionKnown()).put("photos",collection).put("researchCourses",research.snapshot(profile)).put("photoPhase",connected&&EdenLinkService.gameVisible&&fresh?PhotoPhaseState.read(reading,stable,profile,now):new JSONObject());
        JSONArray moments=new JSONArray();JSONArray history=saves.history(profile);for(int i=history.length()-1;i>=0&&moments.length()<3;i--){JSONObject h=history.optJSONObject(i);if(h==null||!h.optBoolean("present")||now-h.optLong("at")>86400000L)continue;moments.put(new JSONObject().put("key",h.optString("key")).put("before",h.optInt("before")).put("score",h.optInt("after")).put("at",h.optLong("at")));}out.put("moments",moments).put("gameReading",memory);return out;
    }
    WebResourceResponse resource(Uri uri){
        if("file".equals(uri.getScheme())&&uri.toString().startsWith("file:///android_asset/"))return null;
        if("https".equals(uri.getScheme())&&PHOTO_HOST.equals(uri.getHost())){List<String> segments=uri.getPathSegments();if(segments.size()==2&&segments.get(0).equals("photo")){File f=photoFiles.get(segments.get(1));if(f!=null)try{return new WebResourceResponse("image/jpeg",null,new FileInputStream(f));}catch(IOException ignored){}}}
        return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
    }
    static byte[] readBounded(InputStream in,int maximum)throws IOException{if(in==null)throw new IOException("Document unavailable");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>maximum)throw new IOException("Document exceeds limit");out.write(buffer,0,n);}return out.toByteArray();}
}
