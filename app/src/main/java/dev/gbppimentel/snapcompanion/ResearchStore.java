package dev.gbppimentel.snapcompanion;
import android.content.*;
import java.util.*;
import org.json.*;
/** Separate experimental cache; never merged into a manual notebook or verified completion counts. */
final class ResearchStore {
    private final SharedPreferences prefs;private final EdenLinkStore link;
    ResearchStore(Context context){prefs=context.getSharedPreferences("snap-research",0);link=new EdenLinkStore(context);}
    void publish(String profile,EdenResearch.Snapshot snapshot)throws JSONException{JSONArray rows=new JSONArray();for(EdenResearch.Course c:snapshot.courses())rows.put(new JSONObject().put("id",c.id()).put("status",c.status()).put("plays",c.plays()).put("subjects",c.subjects()).put("points",c.points()).put("level",c.level()));prefs.edit().putString(profile,new JSONObject().put("schema",1).put("at",snapshot.at()).put("user",snapshot.user()).put("hash",snapshot.metadataHash()).put("courses",rows).toString()).commit();}
    JSONObject snapshot(String profile){try{JSONObject saved=new JSONObject(prefs.getString(profile,"{}"));if(saved.optInt("schema")!=1||!saved.optString("user").equals(link.bound(profile))||!saved.optString("hash").equals(link.hash(profile))||!link.source(profile).equals("Eden saved Photodex"))return new JSONObject();return saved;}catch(JSONException e){return new JSONObject();}}
    JSONObject course(String profile,String id){JSONArray rows=snapshot(profile).optJSONArray("courses");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&row.optString("id").equals(id))return row;}return null;}
    void clear(String profile){prefs.edit().remove(profile).apply();}
}
