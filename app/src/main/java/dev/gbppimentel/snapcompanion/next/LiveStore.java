package dev.gbppimentel.snapcompanion.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;
import org.json.*;

/** Only structured recognized game fields are retained, never raw OCR text from other apps. */
public final class LiveStore {
    private static final Object LOCK=new Object();
    private final SharedPreferences prefs;
    private final String build;
    public LiveStore(Context context) {prefs=context.getSharedPreferences("snap-live",Context.MODE_PRIVATE);build=appBuild(context);}
    static String appBuild(Context context){try{android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);return info.versionName+" ("+info.getLongVersionCode()+")";}catch(Exception e){return "Unknown app build";}}
    public void publish(Map<String,LiveStabilizer.Stable> values,List<LiveStabilizer.Stable> changed) {
        synchronized(LOCK) {
            try {
                JSONObject current=new JSONObject();
                for(LiveStabilizer.Stable s:values.values())current.put(s.key(),json(s));
                JSONArray history=history();
                for(LiveStabilizer.Stable s:changed)if(!s.key().equals("screen")){
                    boolean duplicate=false;
                    for(int i=history.length()-1;i>=0;i--){JSONObject old=history.getJSONObject(i);if(old.getString("key").equals(s.key())&&old.getString("profile").equals(s.profileId())){duplicate=old.getString("value").equals(s.value())&&s.lastSeen()-old.getLong("lastSeen")<30000;break;}}
                    if(!duplicate)history.put(json(s));
                }
                JSONArray bounded=new JSONArray();for(int i=Math.max(0,history.length()-200);i<history.length();i++)bounded.put(history.get(i));
                prefs.edit().putString("current",current.toString()).putString("history",bounded.toString()).apply();
            }catch(JSONException ignored) { /* Prior valid state is retained. */ }
        }
    }
    public Map<String,LiveStabilizer.Stable> snapshot() {
        synchronized(LOCK) {
            Map<String,LiveStabilizer.Stable> result=new LinkedHashMap<>();
            try {JSONObject current=new JSONObject(prefs.getString("current","{}"));Iterator<String> keys=current.keys();while(keys.hasNext()){String key=keys.next();result.put(key,read(current.getJSONObject(key)));}}catch(JSONException ignored){}
            return result;
        }
    }
    public JSONArray history() {try{return new JSONArray(prefs.getString("history","[]"));}catch(JSONException ignored){return new JSONArray();}}
    public void clearCurrent() {synchronized(LOCK){prefs.edit().remove("current").apply();}}
    public void deleteAll() {synchronized(LOCK){prefs.edit().clear().apply();}}
    private JSONObject json(LiveStabilizer.Stable s)throws JSONException{return new JSONObject().put("key",s.key()).put("value",s.value()).put("confidence",s.confidence()).put("firstSeen",s.firstSeen()).put("lastSeen",s.lastSeen()).put("frames",s.frames()).put("source",s.source()).put("profile",s.profileId()).put("evidence",s.evidence()).put("appBuild",build).put("gameCompatibility",LiveSession.observedIdentity());}
    private static LiveStabilizer.Stable read(JSONObject j)throws JSONException{return new LiveStabilizer.Stable(j.getString("key"),j.getString("value"),(float)j.getDouble("confidence"),j.getLong("firstSeen"),j.getLong("lastSeen"),j.getInt("frames"),j.getString("source"),j.getString("profile"),j.getString("evidence"));}
}
