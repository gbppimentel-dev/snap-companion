package dev.gbppimentel.snapcompanion.next;
import android.content.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Samples Eden's own visible FPS/FT labels. These are reported values, not unique-frame measurements. */
final class EdenFps implements AutoCloseable {
    record Reading(Float fps,Float frameMillis,float confidence){}
    record Sample(long at,String profile,String scene,Float fps,Float frameMillis,float confidence){boolean fresh(long now){return now>=at&&now-at<=5000;}}
    static volatile Sample latest;
    private static final Object LOCK=new Object();
    private final SharedPreferences prefs;private final String build;private final ArrayDeque<JSONObject> history=new ArrayDeque<>();private long savedAt,lastAt;
    EdenFps(Context context){prefs=context.getSharedPreferences("snap-fps",0);build=LiveStore.appBuild(context);try{JSONArray old=new JSONArray(prefs.getString("samples","[]"));for(int i=Math.max(0,old.length()-600);i<old.length();i++)history.add(old.getJSONObject(i));}catch(JSONException ignored){}}
    static Reading parse(List<LiveReading.Word> words){
        Float fps=null,ft=null;float quality=1;boolean found=false;
        for(String label:List.of("FPS","FT")){
            Map<Float,Float> values=new HashMap<>();Pattern whole=Pattern.compile("(?i)"+label+"\\s*[:：]?\\s*([0-9]{1,3}(?:[.,][0-9]{1,2})?)(?:ms)?");
            for(LiveReading.Word word:words){if(!top(word))continue;Matcher m=whole.matcher(word.text());if(m.matches())add(values,m.group(1),word.confidence(),label);else if(word.text().matches("(?i)"+label+"[:：]?"))for(LiveReading.Word n:words)if(top(n)&&n.left()>=word.right()-.005&&n.left()-word.right()<.055&&Math.abs(n.y()-word.y())<.025&&n.text().matches("[0-9]{1,3}(?:[.,][0-9]{1,2})?(?:ms)?"))add(values,n.text().replace("ms",""),Math.min(word.confidence(),n.confidence()),label);}
            if(values.size()==1){var value=values.entrySet().iterator().next();if(label.equals("FPS"))fps=value.getKey();else ft=value.getKey();quality=Math.min(quality,value.getValue());found=true;}
        }return found?new Reading(fps,ft,quality):null;
    }
    private static boolean top(LiveReading.Word w){return w.top()>=0&&w.bottom()<=.085&&w.left()>=0&&w.right()<=.65&&Float.isFinite(w.confidence())&&w.confidence()>=.70f;}
    private static void add(Map<Float,Float> values,String raw,float confidence,String label){try{float value=Float.parseFloat(raw.replace(',','.'));if(Float.isFinite(value)&&value>=0&&value<=(label.equals("FPS")?240:999))values.merge(value,confidence,Math::min);}catch(NumberFormatException ignored){}}
    void accept(List<LiveReading.Word> words,long at,String profile,LiveReading.Scene scene){Reading reading=parse(words);if(reading==null||at<=lastAt)return;lastAt=at;Sample sample=new Sample(at,profile,scene.name(),reading.fps(),reading.frameMillis(),reading.confidence());latest=sample;
        synchronized(LOCK){try{JSONObject row=new JSONObject().put("at",at).put("profile",profile).put("scene",scene.name()).put("fps",reading.fps()==null?JSONObject.NULL:reading.fps()).put("frameMillis",reading.frameMillis()==null?JSONObject.NULL:reading.frameMillis()).put("modelConfidence",reading.confidence()).put("source","Visible Eden FPS/FT overlay").put("appBuild",build).put("gameCompatibility",LiveSession.observedIdentity());history.add(row);while(history.size()>600)history.remove();if(at-savedAt>=5000){persist();savedAt=at;}}catch(JSONException ignored){}}
    }
    private void persist(){JSONArray rows=new JSONArray();for(JSONObject row:history)rows.put(row);prefs.edit().putString("samples",rows.toString()).apply();}
    @Override public void close(){synchronized(LOCK){persist();}latest=null;}
    static List<Sample> samples(Context context,String profile){List<Sample> out=new ArrayList<>();synchronized(LOCK){try{JSONArray rows=new JSONArray(context.getSharedPreferences("snap-fps",0).getString("samples","[]"));for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!profile.equals(r.optString("profile")))continue;Float fps=r.isNull("fps")?null:(float)r.getDouble("fps"),ft=r.isNull("frameMillis")?null:(float)r.getDouble("frameMillis");if(fps!=null&&(!Float.isFinite(fps)||fps<0||fps>240)||ft!=null&&(!Float.isFinite(ft)||ft<0||ft>999))continue;out.add(new Sample(r.getLong("at"),profile,r.optString("scene"),fps,ft,(float)r.optDouble("modelConfidence")));}}catch(JSONException ignored){}}Sample current=latest;if(current!=null&&current.profile().equals(profile)&&(out.isEmpty()||current.at()>out.get(out.size()-1).at()))out.add(current);return List.copyOf(out);}
    static String export(Context context,String profile)throws JSONException{JSONArray rows=new JSONArray(context.getSharedPreferences("snap-fps",0).getString("samples","[]")),out=new JSONArray();for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(profile.equals(r.optString("profile")))out.put(r);}return new JSONObject().put("format","snap-eden-reported-performance").put("version",1).put("measurement","Sampled visible Eden FPS/FT values; not independent unique-frame or frame-time-distribution measurements").put("samples",out).toString(2);}
}
