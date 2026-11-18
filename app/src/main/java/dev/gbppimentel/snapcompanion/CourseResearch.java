package dev.gbppimentel.snapcompanion;

import android.content.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Source-bound saved research summaries; never a live-course/selected-level input. */
final class CourseResearch {
    private final Context context;private final SharedPreferences prefs;private final EdenLinkStore link;
    private final Map<String,int[]> thresholds=new LinkedHashMap<>();
    CourseResearch(Context context){this.context=context.getApplicationContext();prefs=context.getSharedPreferences("snap-course-research",0);link=new EdenLinkStore(context);
        try(InputStream in=context.getAssets().open("guidebook.json")){JSONArray rows=new JSONObject(new String(MainActivity.readBounded(in,2000000),StandardCharsets.UTF_8)).getJSONArray("courses");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);JSONArray t=row.getJSONArray("rawResearchThresholds");int[] values={t.getInt(0),t.getInt(1),t.getInt(2)};if(values[0]<0||values[1]<0||values[2]<=0||values[2]<values[0]||values[2]<values[1]||thresholds.put(row.getString("id"),values)!=null)throw new IOException("Invalid course reference");}if(thresholds.size()!=30)throw new IOException("Incomplete course reference");}
        catch(Exception e){throw new IllegalStateException("Saved research references unavailable",e);}
    }
    private boolean current(String profile,String user,String hash){return user.matches("[A-Fa-f0-9]{32}")&&hash.matches("[a-f0-9]{64}")&&new ProfileStore(context.getSharedPreferences("snap",0)).active().equals(profile)&&link.bound(profile).equals(user)&&link.hash(profile).equals(hash)&&link.source(profile).equals("Eden saved Photodex");}
    void refresh(String profile,String user,String hash)throws Exception{publish(profile,EdenResearch.read(context,user,hash));}
    void publish(String profile,EdenResearch.Snapshot saved)throws Exception{
        if(!current(profile,saved.user(),saved.metadataHash()))return;
        JSONObject courses=new JSONObject();Set<String> seen=new HashSet<>();
        for(var c:saved.courses()){if(!thresholds.containsKey(c.id())||!seen.add(c.id())||c.discoveryMask()<0||c.discoveryMask()>65535)throw new IOException("Invalid saved course identity");JSONObject item=new JSONObject().put("discoveryMask",c.discoveryMask());if(c.subjects()>0)item.put("gross",c.grossPoints()).put("offset",c.pointOffset());courses.put(c.id(),item);}
        if(seen.size()!=30)throw new IOException("Incomplete saved course snapshot");
        JSONObject row=new JSONObject().put("schema",2).put("at",saved.at()).put("user",saved.user()).put("hash",saved.metadataHash()).put("courses",courses);
        // Validate the serialized form through the same boundary used by the UI.
        if(decode(row)==null)throw new IOException("Invalid saved research summary");
        if(current(profile,saved.user(),saved.metadataHash())&&!prefs.edit().putString(profile,row.toString()).commit())throw new IOException("Could not store saved research");
    }
    JSONObject snapshot(String profile){try{JSONObject row=new JSONObject(prefs.getString(profile,"{}"));if(!current(profile,row.optString("user"),row.optString("hash")))return new JSONObject();JSONObject decoded=decode(row);return decoded==null?new JSONObject():decoded;}catch(JSONException e){return new JSONObject();}}
    private JSONObject decode(JSONObject row)throws JSONException{
        int schema=row.optInt("schema");if((schema!=1&&schema!=2)||row.optLong("at")<=0)return null;JSONObject values=row.getJSONObject("courses"),out=new JSONObject();Iterator<String> keys=values.keys();
        while(keys.hasNext()){String id=keys.next();int[] t=thresholds.get(id);if(t==null)return null;JSONObject c=values.getJSONObject(id),item=new JSONObject().put("at",row.getLong("at"));
            if(schema==2){int mask=c.getInt("discoveryMask");if(mask<0||mask>65535)return null;if(id.equals("forest")){JSONArray seasons=new JSONArray();for(int bit=3;bit<=6;bit++)if((mask&(1<<bit))!=0)seasons.put(bit-3);item.put("seasonsKnown",true).put("seasons",seasons);}}
            if(c.has("gross")||c.has("offset")||schema==1){long gross=c.getLong("gross"),offset=c.getLong("offset");if(gross<0||gross>10485600||offset<0||offset>16777215)return null;int points=(int)Math.max(0,gross-offset),level=points>=t[2]?4:t[1]>0&&points>=t[1]?3:t[0]>0&&points>=t[0]?2:1;int next=0,base=0,nextLevel=0;
            for(int i=0;i<t.length;i++){int threshold=t[i];if(threshold>0){if(threshold<=points)base=Math.max(base,threshold);else if(next==0||threshold<next){next=threshold;nextLevel=i+2;}}}
            item.put("points",points).put("level",level).put("base",base).put("next",next).put("nextLevel",nextLevel);}
            if(item.has("points")||item.has("seasonsKnown"))out.put(id,item);
        }return out;
    }
}
