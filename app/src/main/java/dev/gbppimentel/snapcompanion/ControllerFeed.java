package dev.gbppimentel.snapcompanion;
import android.content.Context;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import java.util.function.Consumer;import org.json.JSONObject;
/** Passive input-state feed. Frames belong to the current Activity/profile/game generation. */
final class ControllerFeed implements AutoCloseable {
 private final Context context;private final Consumer<String> deliver;private ScheduledExecutorService worker;
 private volatile String owner="",session="",source="";private long beatAt,attemptAt;private String last="";
 ControllerFeed(Context context,Consumer<String> deliver){this.context=context.getApplicationContext();this.deliver=deliver;}
 synchronized void start(){if(worker!=null||!EdenPerformance.supported())return;owner=UUID.randomUUID().toString();rotate("");worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(this::poll,0,40,TimeUnit.MILLISECONDS);}
 private File file(String name){return new File(context.getFilesDir(),"controller."+name);}
 private String gameSource(long now){return EdenLinkService.visible(now)?new ProfileStore(context.getSharedPreferences("snap",0)).active()+":"+EdenLinkService.connection.token():"";}
 private synchronized void rotate(String next){source=next;session=UUID.randomUUID().toString();last="";beatAt=attemptAt=0;file("watch").delete();}
 /** Also checked on the UI thread: a queued old callback cannot survive stop/reopen or a game change. */
 synchronized boolean current(String json){try{JSONObject frame=new JSONObject(json);return !owner.isEmpty()&&session.equals(frame.optString("token"))&&(!frame.optBoolean("available")||!source.isEmpty()&&source.equals(gameSource(System.currentTimeMillis())));}catch(Exception e){return false;}}
 private void poll(){String lifetime=owner;if(lifetime.isEmpty())return;long now=System.currentTimeMillis();String token="";try{
  synchronized(this){if(!lifetime.equals(owner))return;String next=gameSource(now);if(!next.equals(source))rotate(next);token=session;}
  if(source.isEmpty()){send(token,new JSONObject().put("available",false));return;}
  synchronized(this){if(!lifetime.equals(owner)||!token.equals(session))return;if(beatAt==0||now<beatAt||now-beatAt>=1000){android.util.AtomicFile watch=new android.util.AtomicFile(file("watch"));FileOutputStream out=watch.startWrite();try{out.write(token.getBytes(StandardCharsets.UTF_8));watch.finishWrite(out);}catch(Exception e){watch.failWrite(out);throw e;}beatAt=now;}}
  JSONObject frame=new JSONObject();File state=file("json");try{if(state.isFile()&&state.length()<=8192)frame=new JSONObject(new String(java.nio.file.Files.readAllBytes(state.toPath()),StandardCharsets.UTF_8));}catch(Exception ignored){}
  long at=frame.optLong("at"),readNow=System.currentTimeMillis();boolean fresh=token.equals(frame.optString("token"))&&readNow>=at&&readNow-at<1500;
  if(fresh&&frame.optBoolean("available"))send(token,frame);else send(token,new JSONObject().put("available",false));
  synchronized(this){if(!lifetime.equals(owner)||!token.equals(session)||!source.equals(gameSource(System.currentTimeMillis())))return;}
  if((!fresh||!frame.optBoolean("available"))&&(attemptAt==0||now<attemptAt||now-attemptAt>=(fresh?10000:2000))){attemptAt=now;File jar=LiveSession.bridge(context);String path="'"+jar.getAbsolutePath().replace("'","'\\''")+"'";synchronized(this){if(!lifetime.equals(owner)||!token.equals(session))return;}AynBridge.run(context.getCacheDir(),"CLASSPATH="+path+" app_process /system/bin dev.gbppimentel.snapbridge.ControllerReader </dev/null >/dev/null 2>&1 &\necho snap-input-started\n");}
 }catch(Exception e){try{send(token,new JSONObject().put("available",false));}catch(Exception ignored){}}}
 private synchronized void send(String token,JSONObject frame)throws Exception{if(owner.isEmpty()||!token.equals(session))return;frame.put("token",token);String json=frame.toString();if(current(json)&&!json.equals(last)){last=json;deliver.accept(json);}}
 synchronized void stop(){owner=session=source="";if(worker!=null){worker.shutdownNow();worker=null;}file("watch").delete();last="";}
 @Override public void close(){stop();}
}
