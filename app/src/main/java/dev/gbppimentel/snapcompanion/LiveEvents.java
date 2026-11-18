package dev.gbppimentel.snapcompanion;
import android.content.*;
import org.json.*;
import java.util.*;

/** Structured observations only: a possible shot or evaluation is never a collection confirmation. */
final class LiveEvents {
    private static final Object LOCK=new Object();private final android.content.SharedPreferences prefs;private final String build;
    private LiveStabilizer.Stable previousFilm;
    LiveEvents(Context context){prefs=context.getSharedPreferences("snap-events",0);build=LiveStore.appBuild(context);}
    void resetSession(){previousFilm=null;}
    void accept(LiveReading reading,Map<String,LiveStabilizer.Stable> stable){
        try{
            if(reading.scene()==LiveReading.Scene.EXPEDITION){LiveStabilizer.Stable film=stable.get("remainingPhotos");if(film!=null&&film.lastSeen()==reading.capturedAt()&&film.profileId().equals(reading.profileId())){if(previousFilm!=null&&previousFilm.profileId().equals(film.profileId())&&film.lastSeen()>previousFilm.lastSeen()&&film.lastSeen()-previousFilm.lastSeen()<=5000){int before=Integer.parseInt(previousFilm.value()),after=Integer.parseInt(film.value());if(before>after&&before-after<=6){JSONObject fields=new JSONObject().put("before",before).put("after",after).put("decrease",before-after).put("modelScore",film.confidence()).put("frames",film.frames());LiveStabilizer.Stable subject=stable.get("subject"),course=stable.get("course");if(subject!=null&&subject.fresh(film.lastSeen()))fields.put("subject",subject.value());if(course!=null&&course.fresh(film.lastSeen()))fields.put("course",course.value());append("possible-shot",film.lastSeen(),film.profileId(),fields,film.source(),"Film decreased; shutter/action and successful category were not directly confirmed");}}previousFilm=film;}}
            else previousFilm=null;
            if(reading.scene()==LiveReading.Scene.REQUEST_COMPLETE){LiveStabilizer.Stable request=stable.get("completedRequest");if(request!=null&&request.lastSeen()==reading.capturedAt()&&request.profileId().equals(reading.profileId()))append("request-candidate",request.lastSeen(),request.profileId(),new JSONObject().put("request",request.value()).put("modelScore",request.confidence()).put("frames",request.frames()),request.source(),request.evidence()+"; save flag not verified");}
        }catch(JSONException|NumberFormatException ignored){}
    }
    void evaluation(EvaluationReader.Reading r){try{append("evaluation-candidate",r.at(),r.profile(),new JSONObject().put("subject",r.subject()).put("previousTotal",r.previousTotal()).put("total",r.total()).put("previousComponents",new JSONArray(r.previous())).put("components",new JSONArray(r.current())).put("minimumTemplateSimilarity",r.minimumSimilarity()).put("minimumTemplateMargin",r.minimumMargin()),"Screen · paired digit templates","Both digit masks agree numerically and both component sums equal their totals; template similarity is not an accuracy probability; experimental comparison layout; no submission inferred");}catch(JSONException ignored){}}
    private void append(String type,long at,String profile,JSONObject fields,String source,String evidence)throws JSONException{synchronized(LOCK){JSONArray old=all();String signature=type+":"+profile+":"+(type.equals("request-candidate")?fields.optString("request"):type.equals("evaluation-candidate")?fields.optInt("subject")+":"+fields.optInt("previousTotal")+":"+fields.optInt("total")+":"+fields.optJSONArray("previousComponents")+":"+fields.optJSONArray("components"):fields.toString());for(int i=old.length()-1;i>=Math.max(0,old.length()-25);i--){JSONObject event=old.getJSONObject(i);if(event.optString("signature").equals(signature)&&at-event.optLong("at")<30000)return;}old.put(new JSONObject().put("type",type).put("at",at).put("profile",profile).put("fields",fields).put("source",source).put("evidence",evidence).put("appBuild",build).put("gameBuild",LiveSession.observedIdentity()).put("signature",signature));JSONArray next=new JSONArray();for(int i=Math.max(0,old.length()-200);i<old.length();i++)next.put(old.get(i));prefs.edit().putString("events",next.toString()).apply();}}
    JSONArray all(){synchronized(LOCK){try{return new JSONArray(prefs.getString("events","[]"));}catch(JSONException e){return new JSONArray();}}}
    Map<String,JSONObject> requests(String profile){Map<String,JSONObject> found=new HashMap<>();JSONArray rows=all();for(int i=rows.length()-1;i>=0;i--){JSONObject row=rows.optJSONObject(i);if(row!=null&&row.optString("profile").equals(profile)&&row.optString("type").equals("request-candidate")){JSONObject fields=row.optJSONObject("fields");if(fields!=null)found.putIfAbsent(fields.optString("request"),row);}}return Map.copyOf(found);}
    void clear(){synchronized(LOCK){prefs.edit().clear().apply();}previousFilm=null;}
}
