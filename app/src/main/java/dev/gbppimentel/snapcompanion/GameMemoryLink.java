package dev.gbppimentel.snapcompanion;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
/** Owns the private, automatically started read-only memory sampler while the dashboard is visible. */
final class GameMemoryLink implements AutoCloseable {
 private final Context context;private final File watch,state;private final ExecutorService work=Executors.newSingleThreadExecutor();
 private String token="";private volatile boolean active,starting;private long nextStart,lastBeat;private String seenPid="";
 private final Map<Integer,String> courses=new HashMap<>();
 GameMemoryLink(Context source){context=source.getApplicationContext();watch=new File(context.getFilesDir(),"memory.watch");state=new File(context.getFilesDir(),"memory.json");
  try(InputStream in=context.getAssets().open("guidebook.json")){JSONArray rows=new JSONObject(new String(MainActivity.readBounded(in,2000000),StandardCharsets.UTF_8)).getJSONArray("courses");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(courses.put(row.getInt("gameId"),row.getString("id"))!=null)throw new IOException();}if(courses.size()!=30)throw new IOException();}catch(Exception e){throw new IllegalStateException("Course identities unavailable",e);}
 }
 synchronized void start(){if(active)return;active=true;token=UUID.randomUUID().toString();nextStart=lastBeat=0;seenPid="";beat();}
 synchronized void beat(){
  if(!active||!EdenPerformance.supported())return;long now=System.currentTimeMillis();
  String pid=EdenLinkService.gamePid;if(!pid.isEmpty()&&!seenPid.isEmpty()&&!pid.equals(seenPid)){token=UUID.randomUUID().toString();lastBeat=nextStart=0;}if(!pid.isEmpty())seenPid=pid;
  if(now<lastBeat||now-lastBeat>=1000){try{try(FileOutputStream out=new FileOutputStream(watch)){out.write(token.getBytes(StandardCharsets.UTF_8));}lastBeat=now;}catch(IOException e){return;}}
  if(starting||now<nextStart||samplerAlive(now))return;starting=true;nextStart=now+10000;String generation=token;
  work.execute(()->{try{
   File jar=LiveSession.bridge(context);String library=context.getApplicationInfo().nativeLibraryDir+"/libsnapmemory.so";
   synchronized(GameMemoryLink.this){if(!active||!token.equals(generation))return;}
   String script="CLASSPATH="+quote(jar.getAbsolutePath())+" app_process /system/bin dev.gbppimentel.snapbridge.GameMemoryReader "+quote(library)+" >/dev/null 2>&1 &\necho memory-reader-started\n";
   AynBridge.run(context.getCacheDir(),script);
  }catch(Exception ignored){}finally{starting=false;}});
 }
 synchronized JSONObject current(long now){
  if(!active||token.isEmpty())return new JSONObject();
  try(FileInputStream input=new FileInputStream(state)){JSONObject out=validate(new JSONObject(new String(MainActivity.readBounded(input,8192),StandardCharsets.UTF_8)),token,EdenLinkService.gamePid,now);if(out.optString("status").equals("ready")){String id=courses.get(out.getInt("courseId"));if(id==null)return new JSONObject();out.put("course",id);}return out;}catch(Exception e){return new JSONObject();}
 }
 private boolean samplerAlive(long now){
  try(FileInputStream input=new FileInputStream(state)){JSONObject row=new JSONObject(new String(MainActivity.readBounded(input,8192),StandardCharsets.UTF_8));String status=row.optString("status");return Set.of("ready","idle","seeking","ambiguous").contains(status)&&sameGeneration(row,token,EdenLinkService.gamePid,now);}catch(Exception e){return false;}
 }
 private static boolean sameGeneration(JSONObject row,String token,String pid,long now){
  long at=row.optLong("at");return !token.isEmpty()&&token.equals(row.optString("token"))&&pid.matches("[0-9]+")&&pid.equals(String.valueOf(row.optInt("pid")))&&at>0&&now>=at&&now-at<=1000;
 }
 static JSONObject validate(JSONObject row,String token,String pid,long now){
  try{
   if(!sameGeneration(row,token,pid,now))return new JSONObject();
   String status=row.getString("status");if(!Set.of("ready","idle").contains(status))return new JSONObject();
   if(status.equals("ready")){int course=row.getInt("courseId"),level=row.getInt("level");double d=row.getDouble("distance"),length=row.getDouble("length");if(course<1||course>30||level<1||level>4||!Double.isFinite(d)||!Double.isFinite(length)||d<0||length<=0||length>1e6||d>length+0.1)return new JSONObject();}
   return row;
  }catch(JSONException e){return new JSONObject();}
 }
 private static String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
 synchronized void stop(){active=false;token="";watch.delete();}
 @Override public void close(){stop();work.shutdownNow();}
}
