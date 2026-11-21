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
  if(now<lastBeat||now-lastBeat>=1000){try{writeHeartbeat(watch,token);lastBeat=now;}catch(IOException e){return;}}
  if(starting||now<nextStart||samplerAlive(now))return;starting=true;nextStart=now+10000;String generation=token;
  work.execute(()->{try{
   File jar=LiveSession.bridge(context);String library=context.getApplicationInfo().nativeLibraryDir+"/libsnapmemory.so";
   synchronized(GameMemoryLink.this){if(!active||!token.equals(generation))return;}
   String script="CLASSPATH="+quote(jar.getAbsolutePath())+" app_process /system/bin dev.gbppimentel.snapbridge.GameMemoryReader "+quote(library)+" >/dev/null 2>&1 &\necho memory-reader-started\n";
   AynBridge.run(context.getCacheDir(),script);
  }catch(Exception ignored){}finally{starting=false;}});
 }
 static void writeHeartbeat(File watch,String token)throws IOException{
  android.util.AtomicFile file=new android.util.AtomicFile(watch);FileOutputStream out=file.startWrite();
  try{out.write(token.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
 }
 synchronized JSONObject current(long now){
  if(!active||token.isEmpty())return new JSONObject();
  try(FileInputStream input=new FileInputStream(state)){JSONObject out=validate(new JSONObject(new String(MainActivity.readBounded(input,8192),StandardCharsets.UTF_8)),token,EdenLinkService.gamePid,now);if(out.optString("status").equals("ready")){String id=courses.get(out.getInt("courseId"));if(id==null)return new JSONObject();out.put("course",id);}return out;}catch(Exception e){return new JSONObject();}
 }
 synchronized boolean requestTool(String action){
  long now=System.currentTimeMillis();JSONObject current=current(now);
  if(!active||!EdenLinkService.trusted(now)||!EdenLinkService.gameVisible||GameToolRules.key(action)<0||!GameToolRules.gameplay(current.optString("status"),current.optLong("phaseRva"))||!current.optBoolean("inputAvailable"))return false;
  try{JSONObject request=new JSONObject().put("token",token).put("id",UUID.randomUUID().toString()).put("at",now).put("action",action);android.util.AtomicFile file=new android.util.AtomicFile(new File(context.getFilesDir(),"tool.request"));FileOutputStream out=null;try{out=file.startWrite();out.write(request.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}return true;}catch(Exception e){return false;}
 }
 static JSONObject uiSnapshot(JSONObject row){
  JSONObject out=new JSONObject();try{
   for(String key:new String[]{"status","course","courseId","level","distance","length","routeKey","run"})if(row.has(key))out.put(key,row.get(key));
   if(row.has("at"))out.put("sampleAt",row.getLong("at"));
   long phase=row.optLong("phaseRva");out.put("tools",GameToolRules.gameplay(row.optString("status"),phase)&&row.optBoolean("inputAvailable"));JSONObject feedback=row.optJSONObject("lastAction");if(feedback!=null&&GameToolRules.key(feedback.optString("action"))>=0)out.put("lastAction",new JSONObject().put("action",feedback.optString("action")).put("accepted",feedback.optBoolean("accepted")).put("at",feedback.optLong("at")));out.put("phase",phase==0x62e180?"research":phase==0x630240?"paused":row.optString("status").equals("ready")&&phase>=0x624420&&phase<0x633000?"event":"transition");
   for(String key:new String[]{"world","forward"}){JSONArray value=row.optJSONArray(key);if(vector(value))out.put(key,value);}
   Integer remaining=remainingPhotos(row);if(remaining!=null)out.put("remainingPhotos",remaining);
   JSONObject camera=row.optString("status").equals("ready")&&(phase==0x62e180||phase==0x630240)?cameraSnapshot(row.optJSONObject("camera")):null;if(camera!=null)out.put("camera",camera);
   JSONArray points=row.optJSONArray("routePoints");if(points!=null&&points.length()>=2&&points.length()<=97){boolean valid=true;for(int i=0;i<points.length();i++)if(!vector(points.optJSONArray(i)))valid=false;if(valid)out.put("routePoints",points);}
  }catch(JSONException ignored){return new JSONObject();}return out;
 }
 static Integer remainingPhotos(JSONObject row){
  long phase=row.optLong("phaseRva");Object value=row.opt("remainingPhotos");
  if(!row.optString("status").equals("ready")||(phase!=0x62e180&&phase!=0x630240)||!(value instanceof Number))return null;
  double n=((Number)value).doubleValue();return Double.isFinite(n)&&n>=0&&n<=72&&n==Math.rint(n)?(int)n:null;
 }
 static JSONObject cameraSnapshot(JSONObject source){
  if(source==null)return null;try{
   JSONArray forward=source.optJSONArray("forward"),up=source.optJSONArray("up");double fov=source.optDouble("fovY"),aspect=source.optDouble("aspect");
   if(!vector(forward)||!vector(up)||!Double.isFinite(fov)||fov<.1||fov>2.8||!Double.isFinite(aspect)||aspect<.5||aspect>3)return null;
   double norm=0,upNorm=0,dot=0;for(int i=0;i<3;i++){norm+=forward.getDouble(i)*forward.getDouble(i);upNorm+=up.getDouble(i)*up.getDouble(i);dot+=forward.getDouble(i)*up.getDouble(i);}
   if(Math.abs(norm-1)>.02||Math.abs(upNorm-1)>.02||Math.abs(dot)>.1)return null;
   return new JSONObject().put("forward",forward).put("up",up).put("fovY",fov).put("aspect",aspect);
  }catch(JSONException e){return null;}
 }
 private static boolean vector(JSONArray values){if(values==null||values.length()!=3)return false;for(int i=0;i<3;i++){double n=values.optDouble(i,Double.NaN);if(!Double.isFinite(n)||Math.abs(n)>=1e6)return false;}return true;}
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
