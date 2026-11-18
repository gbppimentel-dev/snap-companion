package dev.gbppimentel.snapcompanion;
import android.content.Context;
import java.io.File;
import org.json.JSONObject;
/** Cocoon is the only source of Thor playtime. No elapsed-session fallback. */
final class CocoonTime {
 static volatile JSONObject latest=new JSONObject();
 static JSONObject decode(String text,long now)throws Exception{
  JSONObject input=new JSONObject(text),out=new JSONObject();long ms=input.optLong("totalMs",-1),at=input.optLong("at",0);
  if(ms>=0&&at>0&&at<=now+2000&&now-at<60000&&"Cocoon".equals(input.optString("source")))out.put("totalMs",ms).put("at",at).put("active",input.optBoolean("active")).put("source","Cocoon");
  return out;
 }
 static void refresh(Context context){try{File jar=LiveSession.bridge(context);String path="'"+jar.getAbsolutePath().replace("'","'\\''")+"'";latest=decode(AynBridge.run(context.getCacheDir(),"CLASSPATH="+path+" app_process /system/bin dev.gbppimentel.snapbridge.CocoonPlaytime\n"),System.currentTimeMillis());}catch(Exception e){latest=new JSONObject();}}
}
