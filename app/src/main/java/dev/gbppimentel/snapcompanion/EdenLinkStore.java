package dev.gbppimentel.snapcompanion;

import android.content.*;
import java.util.*;
import org.json.*;

/** Synced game records remain separate from manual records, undo and exports. */
final class EdenLinkStore {
    private final android.content.SharedPreferences prefs;
    private static volatile Set<Integer> roster;
    EdenLinkStore(Context context){if(roster==null)synchronized(EdenLinkStore.class){if(roster==null)try(java.io.InputStream in=context.getAssets().open("catalog.json")){Catalog c=new Catalog(new String(MainActivity.readBounded(in,2_000_000),java.nio.charset.StandardCharsets.UTF_8));Set<Integer> ids=new HashSet<>();for(Catalog.Species p:c.species)ids.add(p.id());roster=Set.copyOf(ids);}catch(Exception e){throw new IllegalStateException("Roster could not load",e);}}prefs=context.getSharedPreferences("snap-eden",Context.MODE_PRIVATE);}
    synchronized void bind(String profile,String user){if(!user.matches("[A-Fa-f0-9]{32}"))throw new IllegalArgumentException("Invalid Eden save profile");prefs.edit().putString("bound."+profile,user).putString("source."+profile,"Eden saved Photodex").remove("document."+profile).remove("history."+profile).remove("photos."+profile).remove("schema."+profile).remove("hash."+profile).remove("updated."+profile).remove("unmapped."+profile).commit();}
    synchronized void clear(String profile){android.content.SharedPreferences.Editor edit=prefs.edit();for(String key:List.of("bound","history","photos","schema","hash","updated","unmapped","source","document"))edit.remove(key+"."+profile);edit.commit();}
    String source(String profile){return prefs.getString("source."+profile,"Eden saved Photodex");}
    String document(String profile){return prefs.getString("document."+profile,"");}
    String bound(String profile){return prefs.getString("bound."+profile,"");}
    long updatedAt(String profile){return prefs.getLong("updated."+profile,0);}
    int unmapped(String profile){return prefs.getInt("unmapped."+profile,0);}
    String hash(String profile){return prefs.getString("hash."+profile,"");}
    Map<String,EdenPhotodex.Photo> photos(String profile){
        Map<String,EdenPhotodex.Photo> result=new TreeMap<>();if(prefs.getInt("schema."+profile,0)!=1)return Map.of();
        try{JSONArray rows=new JSONArray(prefs.getString("photos."+profile,"[]"));for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);EdenPhotodex.Photo p=new EdenPhotodex.Photo(r.getInt("id"),r.getInt("stars"),r.getInt("score"),r.getString("medal"),r.getInt("image"),components(r.optJSONArray("components")),r.optInt("rawScoreField",r.getInt("score")));validate(p);if(result.put(p.key(),p)!=null)throw new JSONException("Duplicate cached photo");}}catch(Exception ignored){return Map.of();}
        return Map.copyOf(result);
    }
    JSONArray history(String profile){try{return new JSONArray(prefs.getString("history."+profile,"[]"));}catch(JSONException e){return new JSONArray();}}
    synchronized void publish(String profile,String edenUser,String hash,EdenPhotodex.Snapshot snapshot)throws JSONException {publish(profile,edenUser,hash,snapshot,"Eden saved Photodex","");}
    synchronized void publishDocument(String profile,String identity,String hash,EdenPhotodex.Snapshot snapshot)throws JSONException{publish(profile,"",hash,snapshot,"Granted save document",identity);}
    private synchronized void publish(String profile,String edenUser,String hash,EdenPhotodex.Snapshot snapshot,String source,String document)throws JSONException {
        String bound=bound(profile);if(!edenUser.isEmpty()&&!bound.isEmpty()&&!bound.equals(edenUser))throw new JSONException("Different Eden profile; existing records preserved");
        Map<String,EdenPhotodex.Photo> previous=photos(profile);JSONArray history=history(profile);long now=System.currentTimeMillis();Set<String> changedKeys=new TreeSet<>(previous.keySet());changedKeys.addAll(snapshot.photos().keySet());for(String key:changedKeys){EdenPhotodex.Photo before=previous.get(key),after=snapshot.photos().get(key);if(before==null||after==null||before.score()!=after.score()||before.imageSlot()!=after.imageSlot())history.put(new JSONObject().put("key",key).put("before",before==null?0:before.score()).put("after",after==null?0:after.score()).put("present",after!=null).put("at",now).put("hash",hash).put("source",source));}JSONArray bounded=new JSONArray();for(int i=Math.max(0,history.length()-500);i<history.length();i++)bounded.put(history.get(i));
        JSONArray rows=new JSONArray();for(EdenPhotodex.Photo p:snapshot.photos().values()){validate(p);rows.put(new JSONObject().put("id",p.national()).put("stars",p.stars()).put("score",p.score()).put("medal",p.medal()).put("image",p.imageSlot()).put("components",new JSONArray(p.components())).put("rawScoreField",p.rawScoreField()));}
        if(!prefs.edit().putString("history."+profile,bounded.toString()).putInt("schema."+profile,1).putString("bound."+profile,edenUser.isEmpty()?bound:edenUser).putString("source."+profile,source).putString("document."+profile,document).putString("photos."+profile,rows.toString()).putString("hash."+profile,hash).putLong("updated."+profile,System.currentTimeMillis()).putInt("unmapped."+profile,snapshot.unmappedEntries()).commit())throw new JSONException("Could not store synchronized records");
    }
    private static void validate(EdenPhotodex.Photo p)throws JSONException{String medal=p.score()>=4000?"Diamond":p.score()>=3500?"Gold":p.score()>=3000?"Silver":"Bronze";if(!roster.contains(p.national())||p.stars()<1||p.stars()>4||p.score()<1||p.score()>99999||p.rawScoreField()<0||p.rawScoreField()>99999||!medal.equals(p.medal())||p.imageSlot()<0||p.imageSlot()>=954)throw new JSONException("Invalid cached game record");if(!p.components().isEmpty()){long sum=0;if(p.components().size()!=6)throw new JSONException("Invalid component count");for(int part:p.components()){if(part<0||part>99999)throw new JSONException("Invalid component");sum+=part;}if(sum!=p.score())throw new JSONException("Cached components do not match total");}}
    private static List<Integer> components(JSONArray a)throws JSONException{if(a==null)return List.of();List<Integer> out=new ArrayList<>();for(int i=0;i<a.length();i++)out.add(a.getInt(i));return List.copyOf(out);}
    Progress effective(String profile,Progress manual){
        Progress p=manual.copy();
        for(EdenPhotodex.Photo photo:photos(profile).values())p.photos.put(photo.key(),new Progress.Photo(photo.score(),photo.medal()));return p;
    }
    /** Live screens use only the game's confirmed collection; legacy entries remain untouched. */
    Progress automatic(String profile,Progress legacy){
        Progress p=legacy.copy();p.photos.clear();p.completedRequests.clear();
        for(EdenPhotodex.Photo photo:photos(profile).values())p.photos.put(photo.key(),new Progress.Photo(photo.score(),photo.medal()));return p;
    }
}
