package dev.gbppimentel.snapcompanion.next;

import org.json.*;
import java.util.*;

/** Additional personal planning state. All manual until a separately verified game source supplies it. */
public final class Notebook {
    public enum RequestState { UNKNOWN, UNAVAILABLE, AVAILABLE, ATTEMPTED, OBSERVED_CANDIDATE, COMPLETE }
    public enum HintMode { DISCOVERY, ACTION, SOLUTION }
    public enum PlanMode { SHORT_RUN, COMPLETION, DIAMOND, REQUESTS, PERSONAL_BEST }
    public enum Access { UNKNOWN, LOCKED, UNLOCKED }
    public record CourseRecord(int highestLevel,int researchPoints,int bestScore){}
    public final Map<String,RequestState> requests=new TreeMap<>();
    public final Map<String,CourseRecord> courses=new TreeMap<>();
    public final Map<String,Access> access=new TreeMap<>();
    public final Set<String> story=new TreeSet<>(),routes=new TreeSet<>();
    public static final List<String> STORY=List.of("florio-illumina","belusylva-illumina","maricopia-illumina","voluca-illumina","durice-illumina","aurus-illumina","main-story-complete");
    public final Set<String> discoveries=new TreeSet<>(),forms=new TreeSet<>(),unlocks=new TreeSet<>(),tools=new TreeSet<>(),skipped=new TreeSet<>();
    public final Map<String,String> actions=new TreeMap<>();
    public HintMode hints=HintMode.DISCOVERY;
    public PlanMode mode=PlanMode.COMPLETION;
    public boolean hintsConfigured;
    public RequestState state(String id,Set<String> completed){return completed.contains(id)?RequestState.COMPLETE:requests.getOrDefault(id,RequestState.UNKNOWN);}
    public JSONObject json()throws JSONException{
        JSONObject r=new JSONObject().put("version",1).put("hints",hints.name()).put("hintsConfigured",hintsConfigured).put("mode",mode.name());JSONObject rs=new JSONObject(),cs=new JSONObject(),as=new JSONObject();
        for(var e:requests.entrySet())rs.put(e.getKey(),e.getValue().name());for(var e:courses.entrySet())cs.put(e.getKey(),new JSONObject().put("level",e.getValue().highestLevel()).put("points",e.getValue().researchPoints()).put("score",e.getValue().bestScore()));for(var e:actions.entrySet())as.put(e.getKey(),e.getValue());
        JSONObject accessJson=new JSONObject();for(var e:access.entrySet())accessJson.put(e.getKey(),e.getValue().name());
        return r.put("requests",rs).put("courses",cs).put("actions",as).put("discoveries",new JSONArray(discoveries)).put("forms",new JSONArray(forms)).put("unlocks",new JSONArray(unlocks)).put("tools",new JSONArray(tools)).put("skipped",new JSONArray(skipped)).put("access",accessJson).put("story",new JSONArray(story)).put("routes",new JSONArray(routes));
    }
    public static Notebook read(JSONObject r,Catalog catalog)throws JSONException{
        Notebook n=new Notebook();if(r==null)return n;if(r.getInt("version")!=1)throw new JSONException("Unsupported notebook schema");
        try{n.hints=HintMode.valueOf(r.getString("hints"));n.mode=PlanMode.valueOf(r.getString("mode"));n.hintsConfigured=r.optBoolean("hintsConfigured",false);
            JSONObject rs=r.getJSONObject("requests");for(String id:JsonKeys.of(rs)){if(catalog!=null&&!catalog.hasRequest(id))throw new JSONException("Unknown request state");n.requests.put(id,RequestState.valueOf(rs.getString(id)));}
            JSONObject cs=r.getJSONObject("courses");for(String id:JsonKeys.of(cs)){JSONObject c=cs.getJSONObject(id);int level=integer(c,"level"),points=integer(c,"points"),score=integer(c,"score");if((catalog!=null&&!catalog.hasCourse(id))||level<1||level>3||points<0||score<0)throw new JSONException("Invalid course record");n.courses.put(id,new CourseRecord(level,points,score));}
            add(r,"discoveries",n.discoveries);add(r,"forms",n.forms);add(r,"unlocks",n.unlocks);add(r,"tools",n.tools);add(r,"skipped",n.skipped);
            if(r.has("story"))add(r,"story",n.story);if(r.has("routes"))add(r,"routes",n.routes);if(!new HashSet<>(STORY).containsAll(n.story))throw new JSONException("Unknown story milestone");
            JSONObject access=r.optJSONObject("access");if(r.has("access")&&access==null)throw new JSONException("Invalid course access");if(access!=null)for(String id:JsonKeys.of(access)){if(catalog!=null&&!catalog.hasCourse(id))throw new JSONException("Unknown access course");n.access.put(id,Access.valueOf(access.getString(id)));}
            if(!Set.of("fluffruit","illumina-orb","melody","scan","turbo","shrink").containsAll(n.tools))throw new JSONException("Unknown tool");
            JSONObject as=r.getJSONObject("actions");for(String id:JsonKeys.of(as)){String state=as.getString(id);if(!Set.of("suggested","attempted","observed","skipped").contains(state)||id.length()>300)throw new JSONException("Invalid action state");n.actions.put(id,state);}
        }catch(IllegalArgumentException e){throw new JSONException("Invalid notebook choice");}return n;
    }
    private static int integer(JSONObject r,String key)throws JSONException{Object v=r.get(key);if(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).intValue())throw new JSONException("Invalid record number");return((Number)v).intValue();}
    private static void add(JSONObject r,String key,Set<String> target)throws JSONException{JSONArray a=r.getJSONArray(key);if(a.length()>5000)throw new JSONException("Too many notebook entries");for(int i=0;i<a.length();i++){String s=a.getString(i);if(s.isBlank()||s.length()>300)throw new JSONException("Invalid notebook entry");target.add(s);}}
}
