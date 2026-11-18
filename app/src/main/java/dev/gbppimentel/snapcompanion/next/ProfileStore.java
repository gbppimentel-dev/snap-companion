package dev.gbppimentel.snapcompanion.next;

import android.content.SharedPreferences;
import java.util.*;
import org.json.*;

/** Legacy "progress" is the default profile; new profiles never overwrite that key. */
public final class ProfileStore {
    public record Profile(String id,String name,String identity){}
    private final SharedPreferences prefs;
    public ProfileStore(SharedPreferences prefs){this.prefs=prefs;}
    public List<Profile> all(){
        List<Profile> list=new ArrayList<>();list.add(new Profile("default","Main game",prefs.getString("identity.default","")));
        try{JSONArray saved=new JSONArray(prefs.getString("profiles","[]"));for(int i=0;i<Math.min(7,saved.length());i++){JSONObject p=saved.getJSONObject(i);String id=p.getString("id");if(id.matches("[a-f0-9-]{36}"))list.add(new Profile(id,p.getString("name"),prefs.getString("identity."+id,"")));}}catch(JSONException ignored){}
        return List.copyOf(list);
    }
    public String active(){String id=prefs.getString("activeProfile","default");return all().stream().anyMatch(p->p.id().equals(id))?id:"default";}
    public String key(String id){return id.equals("default")?"progress":"progress."+id;}
    public String currentKey(){return key(active());}
    public void select(String id){if(all().stream().noneMatch(p->p.id().equals(id)))throw new IllegalArgumentException("Unknown profile");prefs.edit().putString("activeProfile",id).commit();}
    public String create(String name)throws JSONException{
        name=name.trim();if(name.isEmpty()||name.length()>40||all().size()>=8)throw new IllegalArgumentException("Use a name of 1–40 characters; maximum eight profiles");
        String id=UUID.randomUUID().toString();JSONArray list=new JSONArray(prefs.getString("profiles","[]"));list.put(new JSONObject().put("id",id).put("name",name));if(!prefs.edit().putString("profiles",list.toString()).commit())throw new IllegalStateException("Profile storage failed");return id;
    }
    public void identity(String text){if(text.length()>120)throw new IllegalArgumentException("Identity is limited to 120 characters");prefs.edit().putString("identity."+active(),text).apply();}
    public void remember(String before,String after)throws JSONException{
        if(before.equals(after))return;String key="undo."+active();JSONArray old=new JSONArray(prefs.getString(key,"[]")),next=new JSONArray();for(int i=Math.max(0,old.length()-9);i<old.length();i++)next.put(old.get(i));next.put(new JSONObject().put("at",System.currentTimeMillis()).put("snapshot",before));prefs.edit().putString(key,next.toString()).commit();
    }
    public String undo(Catalog catalog)throws JSONException{
        String key="undo."+active();JSONArray old=new JSONArray(prefs.getString(key,"[]"));if(old.length()==0)return "";String candidate=old.getJSONObject(old.length()-1).getString("snapshot");Progress.fromJson(candidate,catalog);JSONArray next=new JSONArray();for(int i=0;i<old.length()-1;i++)next.put(old.get(i));if(!prefs.edit().putString(currentKey(),candidate).putString(key,next.toString()).commit())throw new JSONException("Undo storage failed");return candidate;
    }
}
