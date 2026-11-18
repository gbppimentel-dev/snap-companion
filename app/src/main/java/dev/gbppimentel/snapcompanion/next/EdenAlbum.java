package dev.gbppimentel.snapcompanion.next;

import android.content.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.json.*;

/** Read-only album slots: metadata6 / full JPEG7 / thumbnail10 share an entry index.
 * Only the verified in-use byte is interpreted. Opaque metadata is kept with each copied photo. */
final class EdenAlbum implements AutoCloseable {
    record Entry(int slot,String fingerprint,byte[] metadata){Entry{metadata=metadata.clone();}public byte[] metadata(){return metadata.clone();}}
    record Snapshot(List<Entry> entries,String hash){Snapshot{entries=List.copyOf(entries);}}
    record Images(byte[] full,byte[] thumbnail){}
    interface Reader{Images read(Context context,String user,String metadataHash,String albumHash,int slot)throws Exception;}
    private record Job(String profile,String user,String metadataHash,String albumHash,Entry entry){}
    static volatile String status="Waiting to check the saved personal album";
    static volatile long changedAt;
    private final Context context;private final SharedPreferences prefs,state,checkpoints;private final EdenLinkStore link;private final BooleanSupplier idle;private final Reader reader;
    private final ArrayDeque<Job> pending=new ArrayDeque<>();private ScheduledExecutorService worker;private volatile boolean closing;private String offered="";private long retryAt;private int retries;
    EdenAlbum(Context context,BooleanSupplier idle){this(context,idle,(c,u,m,a,s)->{byte[] full=EdenGallery.readAlbum(c,u,m,a,s,false),thumbnail;try{thumbnail=EdenGallery.readAlbum(c,u,m,a,s,true);}catch(EdenGallery.EmptyPhoto empty){thumbnail=null;}return new Images(full,thumbnail);});}
    EdenAlbum(Context context,BooleanSupplier idle,Reader reader){this.context=context;this.idle=idle;this.reader=reader;prefs=context.getSharedPreferences("snap",0);state=context.getSharedPreferences("snap-album-state",0);checkpoints=context.getSharedPreferences("snap-album-copies",0);link=new EdenLinkStore(context);}
    void start(){worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(this::tick,0,2,TimeUnit.SECONDS);}
    static Snapshot decode(File file)throws Exception{
        if(file.length()!=912432)throw new IOException("Unsupported album metadata dimensions");List<Entry> rows=new ArrayList<>();
        NewSnapSave.Report report=NewSnapSave.inspect(file,NewSnapSave.NAMES.get(6),entry->{byte[] bytes=entry.payload();if(bytes.length!=328)throw new IOException("Unsupported album record");int inUse=Byte.toUnsignedInt(bytes[0x14]);if(inUse>1)throw new IOException("Unknown album in-use flag");if(inUse==1)try{rows.add(new Entry(entry.index(),hex(MessageDigest.getInstance("SHA-256").digest(bytes)),bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}});
        if(report.header().entryCount()!=2376||report.header().payloadSize()!=328||report.header().encryptedEntrySize()!=384)throw new IOException("Unsupported album metadata layout");return new Snapshot(rows,hex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(file.toPath()))));
    }
    void refresh(String profile,String user,String metadataHash)throws Exception{
        if(closing)return;File snapshot=File.createTempFile("album-metadata-",".snapshot",context.getCacheDir());
        try{String result=AynBridge.run(context.getCacheDir(),script(snapshot,android.os.Process.myUid(),user,metadataHash));if(!result.equals("stable"))throw new IOException("Album snapshot waiting: "+result);Snapshot saved=decode(snapshot);if(closing||!profile.equals(new ProfileStore(prefs).active())||!user.equals(link.bound(profile))||!metadataHash.equals(link.hash(profile))||!link.source(profile).equals("Eden saved Photodex"))return;offer(profile,user,metadataHash,saved);}finally{snapshot.delete();}
    }
    synchronized void offer(String profile,String user,String metadataHash,Snapshot snapshot)throws JSONException{
        String key=profile+":"+user+":"+metadataHash+":"+snapshot.hash();if(key.equals(offered))return;offered=key;pending.clear();retries=0;retryAt=0;
        JSONObject record=new JSONObject().put("schema",1).put("user",user).put("metadataHash",metadataHash).put("albumHash",snapshot.hash()).put("count",snapshot.entries().size()).put("at",System.currentTimeMillis());if(!state.edit().putString(profile,record.toString()).commit())throw new JSONException("Album scan could not be stored");changedAt=System.nanoTime();
        for(Entry e:snapshot.entries()){String identity=identity(user,e);if(!identity.equals(checkpoints.getString(profile+"."+e.slot(),"")))pending.add(new Job(profile,user,metadataHash,snapshot.hash(),e));}
        status=snapshot.entries().size()+" saved album photos · "+(pending.isEmpty()?"copies up to date":pending.size()+" queued for idle copying");
    }
    void tick(){
        if(closing||!prefs.getBoolean("autoGallery",true))return;Job job;synchronized(this){job=pending.peek();if(job==null||System.currentTimeMillis()<retryAt)return;}
        if(!idle.getAsBoolean())return;if(!valid(job)){remove(job);return;}
        try{PersonalGallery gallery=new PersonalGallery(context,job.profile());String identity=identity(job.user(),job.entry());boolean exists=gallery.all().stream().anyMatch(p->p.linkedHash().equals(identity));
            if(!exists){Images images=reader.read(context,job.user(),job.metadataHash(),job.albumHash(),job.entry().slot());if(closing||!valid(job))return;PersonalGallery.Photo added=gallery.addAlbum(images.full(),images.thumbnail(),job.entry().metadata(),job.entry().slot(),identity,()->!closing&&prefs.getBoolean("autoGallery",true)&&valid(job));if(added==null)return;AutoGallery.changedAt=System.nanoTime();}
            checkpoints.edit().putString(job.profile()+"."+job.entry().slot(),identity).apply();remove(job);status=pendingCount()==0?"Saved personal album copies up to date":pendingCount()+" personal album photos queued";
        }catch(Exception e){synchronized(this){if(pending.peek()!=job)return;if(++retries>=3){remove(job);status="An album image could not be read; later slots continue and it can retry after a new save";}else{retryAt=System.currentTimeMillis()+30000L*retries;status="Album copy waiting · retry "+retries+"/3";}}}
    }
    private boolean valid(Job job){try{JSONObject cached=new JSONObject(state.getString(job.profile(),"{}"));return job.profile().equals(new ProfileStore(prefs).active())&&job.user().equals(link.bound(job.profile()))&&job.metadataHash().equals(link.hash(job.profile()))&&link.source(job.profile()).equals("Eden saved Photodex")&&cached.optString("albumHash").equals(job.albumHash())&&cached.optString("user").equals(job.user())&&cached.optString("metadataHash").equals(job.metadataHash());}catch(JSONException e){return false;}}
    private synchronized void remove(Job job){if(pending.peek()==job){pending.remove();retries=0;retryAt=0;}}
    synchronized int pendingCount(){return pending.size();}
    static String identity(String user,Entry e){return "eden-album-v1:"+user+":"+e.slot()+":"+e.fingerprint();}
    static String script(File out,int uid,String user,String hash){
        if(!user.matches("[A-Fa-f0-9]{32}")||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid album save identity");String target="'"+out.getAbsolutePath().replace("'","'\\''")+"'";
        return "snap_dir=/sdcard/Android/data/dev.eden.eden_emulator/files/nand/user/save/0000000000000000/"+user+"/0100F4300BF2C000\n"+
            "[ \"$(stat -c %s \"$snap_dir/04289da492638279\")\" = 912432 ] || { echo unsupported; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "snap_before=$(sha256sum \"$snap_dir/04289da492638279\" | cut -d ' ' -f 1)\ncp \"$snap_dir/04289da492638279\" "+target+" || exit 1\n"+
            "snap_after=$(sha256sum \"$snap_dir/04289da492638279\" | cut -d ' ' -f 1)\n"+
            "[ \"$snap_before\" = \"$snap_after\" ] && [ \"$(sha256sum "+target+" | cut -d ' ' -f 1)\" = \"$snap_before\" ] || { echo changing; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "chown "+uid+":"+uid+" "+target+"\nchmod 600 "+target+"\necho stable\n";
    }
    static String summary(Context context,String profile){try{JSONObject row=new JSONObject(context.getSharedPreferences("snap-album-state",0).getString(profile,"{}"));EdenLinkStore link=new EdenLinkStore(context);if(row.optInt("schema")==1&&row.optString("user").equals(link.bound(profile))&&row.optString("metadataHash").equals(link.hash(profile))&&link.source(profile).equals("Eden saved Photodex"))return row.optInt("count")+" photos in the saved personal album";}catch(JSONException ignored){}return "Saved personal album not scanned yet";}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    public void close(){closing=true;if(worker!=null)worker.shutdownNow();synchronized(this){pending.clear();}}
}
