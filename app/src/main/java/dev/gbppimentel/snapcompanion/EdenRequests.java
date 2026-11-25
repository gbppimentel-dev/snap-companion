package dev.gbppimentel.snapcompanion;

import android.content.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Owned2.0.1 request state byte:1/2 received,3/4 completed. State4 satisfies independently verified condition103 only; no general ownership inference. */
final class EdenRequests {
    record Snapshot(Set<String> received,Set<String> completed,Set<String> rewardReady,int unmapped,boolean completionKnown,boolean rewardConditionsKnown){Snapshot{received=Set.copyOf(received);completed=Set.copyOf(completed);rewardReady=Set.copyOf(rewardReady);if(!received.containsAll(completed)||!completed.containsAll(rewardReady))throw new IllegalArgumentException("Invalid request state subsets");}Snapshot(Set<String> received,Set<String> completed,int unmapped,boolean completionKnown){this(received,completed,Set.of(),unmapped,completionKnown,false);}Snapshot(Set<String> received,int unmapped){this(received,Set.of(),unmapped,false);}}
    static final class Decoder {
        final Map<Integer,String> ids;
        Decoder(String json)throws JSONException{
            JSONObject root=new JSONObject(json);if(root.getInt("schema")!=1||root.getInt("archive")!=14||!root.getString("gameVersion").equals("2.0.1"))throw new JSONException("Unsupported request map");
            Map<Integer,String> map=new HashMap<>();Set<String> names=new HashSet<>();JSONArray rows=root.getJSONArray("requests");
            for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);int index=row.getInt("index");String id=row.getString("id");if(index<0||index>=240||!id.matches("[a-z0-9-]{1,100}")||map.put(index,id)!=null||!names.add(id))throw new JSONException("Invalid request identity map");}
            if(map.size()!=234)throw new JSONException("Incomplete request identity map");ids=Map.copyOf(map);
        }
        Snapshot read(File file)throws IOException{
            if(file.length()!=19248)throw new IOException("Unsupported request registry dimensions");Set<String> received=new HashSet<>(),completed=new HashSet<>(),ready=new HashSet<>();int[] unmapped={0};
            NewSnapSave.Report report=NewSnapSave.inspect(file,NewSnapSave.NAMES.get(14),entry->{if(entry.payload().length!=24)throw new IOException("Unsupported request entry");int state=entry.payload()[1]&255;if(state>4)throw new IOException("Unsupported request state");if(state==0)return;String id=ids.get(entry.index());if(id==null){unmapped[0]++;return;}received.add(id);if(state>=3)completed.add(id);if(state==4)ready.add(id);});
            if(report.header().entryCount()!=240||report.header().payloadSize()!=24||report.header().encryptedEntrySize()!=80)throw new IOException("Unsupported request registry layout");return new Snapshot(received,completed,ready,unmapped[0],true,true);
        }
    }
    private static volatile Decoder sharedDecoder;
    static volatile String status="Waiting for saved request registry";
    private final Context context;private final SharedPreferences prefs;private final EdenLinkStore link;private final Decoder decoder;
    EdenRequests(Context context){this.context=context.getApplicationContext();prefs=context.getSharedPreferences("snap-requests",0);link=new EdenLinkStore(context);
        if(sharedDecoder==null)synchronized(EdenRequests.class){if(sharedDecoder==null)try(InputStream in=context.getAssets().open("request-map.json")){sharedDecoder=new Decoder(new String(MainActivity.readBounded(in,100000),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("Request map missing",e);}}decoder=sharedDecoder;
    }
    private JSONObject current(String profile){try{JSONObject row=new JSONObject(prefs.getString(profile,"{}"));if((row.optInt("schema")<1||row.optInt("schema")>3)||!row.optString("user").equals(link.bound(profile))||!row.optString("metadataHash").equals(link.hash(profile))||!link.source(profile).equals("Eden saved Photodex"))return null;return row;}catch(JSONException e){return null;}}
    Snapshot snapshot(String profile){JSONObject row=current(profile);if(row==null)return null;try{Set<String> ids=new HashSet<>(),done=new HashSet<>();JSONArray values=row.getJSONArray("received");for(int i=0;i<values.length();i++){String id=values.getString(i);if(!decoder.ids.containsValue(id)||!ids.add(id))return null;}boolean known=row.optInt("schema")>=2;if(known){JSONArray completed=row.getJSONArray("completed");for(int i=0;i<completed.length();i++){String id=completed.getString(i);if(!ids.contains(id)||!done.add(id))return null;}}Set<String> ready=new HashSet<>();boolean rewardKnown=row.optInt("schema")==3;if(rewardKnown){JSONArray a=row.getJSONArray("rewardReady");for(int i=0;i<a.length();i++){String id=a.getString(i);if(!done.contains(id)||!ready.add(id))return null;}}return new Snapshot(ids,done,ready,row.getInt("unmapped"),known,rewardKnown);}catch(JSONException e){return null;}}
    long updatedAt(String profile){JSONObject row=current(profile);return row==null?0:row.optLong("at");}
    void refresh(String profile,String user,String metadataHash)throws Exception{
        File file=File.createTempFile("request-registry-",".snapshot",context.getCacheDir());
        try{String result=AynBridge.run(context.getCacheDir(),script(file,android.os.Process.myUid(),user,metadataHash));if(!result.equals("stable"))throw new IOException("Request snapshot waiting: "+result);Snapshot saved=decoder.read(file);String registryHash=hex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(file.toPath())));
            if(!new ProfileStore(context.getSharedPreferences("snap",0)).active().equals(profile)||!link.bound(profile).equals(user)||!link.hash(profile).equals(metadataHash)||!link.source(profile).equals("Eden saved Photodex"))return;
            JSONObject previous=current(profile);if(previous==null||!registryHash.equals(previous.optString("registryHash"))||previous.optInt("schema")!=3){JSONObject row=new JSONObject().put("schema",3).put("at",System.currentTimeMillis()).put("user",user).put("metadataHash",metadataHash).put("registryHash",registryHash).put("received",new JSONArray(new TreeSet<>(saved.received()))).put("completed",new JSONArray(new TreeSet<>(saved.completed()))).put("rewardReady",new JSONArray(new TreeSet<>(saved.rewardReady()))).put("unmapped",saved.unmapped());if(!prefs.edit().putString(profile,row.toString()).commit())throw new IOException("Could not store request registry");}
            status=saved.received().size()+" received · "+saved.completed().size()+" completed";
        }finally{file.delete();}
    }
    static String script(File output,int uid,String user,String hash){
        if(!user.matches("[A-Fa-f0-9]{32}")||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid request save identity");String out="'"+output.getAbsolutePath().replace("'","'\\''")+"'";
        return "snap_dir=/sdcard/Android/data/dev.igawa6.edenduo/files/nand/user/save/0000000000000000/"+user+"/0100F4300BF2C000\n"+
            "[ \"$(stat -c %s \"$snap_dir/75006a27cf0c9275\")\" = 19248 ] || { echo unsupported; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "snap_before=$(sha256sum \"$snap_dir/75006a27cf0c9275\" | cut -d ' ' -f 1)\n"+
            "cp \"$snap_dir/75006a27cf0c9275\" "+out+" || exit 1\n"+
            "snap_after=$(sha256sum \"$snap_dir/75006a27cf0c9275\" | cut -d ' ' -f 1)\n"+
            "[ \"$snap_before\" = \"$snap_after\" ] || { echo changing; exit 0; }\n"+
            "[ \"$(sha256sum "+out+" | cut -d ' ' -f 1)\" = \"$snap_before\" ] || { echo incomplete; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "chown "+uid+":"+uid+" "+out+"\nchmod 600 "+out+"\necho stable\n";
    }
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
}
