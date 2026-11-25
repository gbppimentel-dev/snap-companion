package dev.gbppimentel.snapcompanion;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Thor-native, read-only Eden link. Copies only the small Photodex metadata file, never writes Eden. */
public final class EdenLinkService extends Service {
    static volatile boolean active,gameRunning,gameVisible;
    static volatile boolean gameClosedConfirmed;
    static volatile boolean closedCopyAllowed;
    static volatile String status="Disconnected",gamePid="";
    static volatile long checks,lastCheckedAt,lastConnectedAt;
    static volatile GameConnection connection=GameConnection.empty();
    static boolean trusted(long now){return active&&connection.running(now);}
    static boolean visible(long now){return active&&connection.onScreen(now);}
    private static void presence(EdenLinkReply reply,long now){connection=connection.observed(reply.pid(),reply.visible(),now);gameRunning=connection.running(now);gameVisible=connection.onScreen(now);gameClosedConfirmed=connection.closed(now);closedCopyAllowed=reply.closedCopyAllowed();gamePid=connection.pid();lastConnectedAt=now;}
    private static void disconnected(){connection=connection.disconnected();gameRunning=gameVisible=gameClosedConfirmed=closedCopyAllowed=false;gamePid="";}
    private final String metricsSession=UUID.randomUUID().toString();
    private long playtimeAt;
    private volatile boolean closing;
    private ScheduledExecutorService worker;
    private EdenPhotodex decoder;
    private AutoGallery gallery;
    private EdenRequests requests;
    private CourseResearch research;private final ResearchSyncGate researchGate=new ResearchSyncGate(),requestGate=new ResearchSyncGate();
    
    private String requestFingerprint="",researchFingerprint="";
    private EdenLinkStore store;
    private android.content.SharedPreferences prefs;
    private File snapshot;
    private String lastBound="",profile="",fingerprint="",error="";private final SaveSyncGate saveGate=new SaveSyncGate();
    private long copiedAt;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){getSharedPreferences("snap",MODE_PRIVATE).edit().putBoolean("edenAutoLink",false).apply();stopSelf();return START_NOT_STICKY;}
        if(active)return START_STICKY;
        if(!EdenPerformance.supported()){status="Direct Eden link is available on AYN Thor";stopSelf();return START_NOT_STICKY;}
        try{
            LiveSession.ensure(this);
            NotificationManager n=getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel("eden-link","Live Eden connection",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,13,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stop=PendingIntent.getService(this,14,new Intent(this,EdenLinkService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            startForeground(13,new Notification.Builder(this,"eden-link").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("Snap Companion").setContentText("Companion Active").setContentIntent(open).setOngoing(true).build(),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            prefs=getSharedPreferences("snap",MODE_PRIVATE);store=new EdenLinkStore(this);
            try(InputStream a=getAssets().open("catalog.json");InputStream b=getAssets().open("eden-species.json")){decoder=new EdenPhotodex(new String(MainActivity.readBounded(b,100000),StandardCharsets.UTF_8),new Catalog(new String(MainActivity.readBounded(a,2000000),StandardCharsets.UTF_8)));}
            requests=new EdenRequests(this);research=new CourseResearch(this);gallery=new AutoGallery(this);gallery.start();snapshot=new File(getCacheDir(),"eden-linked-metadata.snapshot");active=true;checks=0;disconnected();status="Looking for New Pokémon Snap in Eden";
            worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(this::check,0,1,TimeUnit.SECONDS);
        }catch(Exception e){status="Link unavailable: "+e.getMessage();stopSelf();}return START_STICKY;
    }
    private void check(){
        if(closing)return;boolean presenceVerified=false;
        try{
            LiveSession.ensure(this);if(System.currentTimeMillis()-playtimeAt>=15000){playtimeAt=System.currentTimeMillis();CocoonTime.refresh(this);}checks++;lastCheckedAt=System.currentTimeMillis();String current=new ProfileStore(prefs).active();
            if(!current.equals(profile)){profile=current;saveGate.reset();fingerprint="";error="";disconnected();}
            String bound=store.bound(profile);if(!bound.equals(lastBound)){lastBound=bound;saveGate.reset();fingerprint="";disconnected();}if(!profile.equals("default")&&bound.isEmpty()){disconnected();status="This companion profile has no Eden save linked";return;}
            String skip=saveGate.skipUnchanged(System.currentTimeMillis())&&System.currentTimeMillis()-copiedAt<30000?fingerprint:"";
            snapshot.createNewFile();
            String response=AynBridge.run(getCacheDir(),script(snapshot,android.os.Process.myUid(),bound,skip));
            EdenLinkReply reply;try{reply=EdenLinkReply.parse(response);}catch(IOException invalid){disconnected();status=switch(response){case "no-game"->"Waiting for Eden · last saved records remain available";case "other-game"->"Eden is running another game · Snap sync paused";case "multiple-saves"->"Multiple Eden saves found · choose a profile before linking";default->"Waiting for a verified Snap connection";};return;}
            presence(reply,System.currentTimeMillis());presenceVerified=true;if(gameRunning)LiveSession.resumeOnOpen(this);
            requestFingerprint=reply.requestFingerprint();researchFingerprint=reply.researchFingerprint();
            if(reply.state().equals("changing")){status="Connected · waiting for the game to finish saving";return;}
            if(reply.state().equals("unsupported")){status="Connected · saved Photodex format not accepted";return;}
            if(reply.state().equals("no-save")){status="Snap connected · waiting for its first saved Photodex";return;}
            fingerprint=reply.fingerprint();
            if(reply.state().equals("same")){status=error.isEmpty()?(gameVisible?"Connected · Photodex up to date":gameRunning?"Game in background · saved Photodex up to date":"Saved Photodex connected · game is closed"):"Connected · "+error;syncRequests(profile,reply.user(),store.hash(profile));syncResearch(profile,reply.user(),store.hash(profile));return;}
            copiedAt=System.currentTimeMillis();if(snapshot.length()!=240496)throw new IOException("Unsupported Photodex file dimensions");
            String hash;try(InputStream in=new FileInputStream(snapshot)){MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);hash=hex(digest.digest());}
            if(!saveGate.ready(hash,System.currentTimeMillis())){syncRequests(profile,reply.user(),store.hash(profile));syncResearch(profile,reply.user(),store.hash(profile));return;}
            EdenPhotodex.Snapshot decoded;try{decoded=decoder.read(snapshot);}catch(Exception invalid){saveGate.rejected(System.currentTimeMillis());throw invalid;}
            if(closing||!prefs.getBoolean("edenAutoLink",true)||!new ProfileStore(prefs).active().equals(profile))return;
            store.publish(profile,reply.user(),hash,decoded);saveGate.accepted(hash);gallery.offer(profile,reply.user(),hash,decoded);error="";status="Connected to Eden · "+decoded.photos().size()+" photo categories synchronized"+(decoded.unmappedEntries()>0?" · "+decoded.unmappedEntries()+" record(s) not mapped":"")+(gameVisible?"":" · game in background");
            syncRequests(profile,reply.user(),hash);syncResearch(profile,reply.user(),hash);
        }catch(Exception e){if(!presenceVerified)disconnected();error="Save not accepted: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());status=error;}
        finally{if(snapshot!=null)snapshot.delete();}
    }
    private void syncRequests(String profile,String user,String hash){long now=System.currentTimeMillis();String key=hash.isEmpty()?"":profile+":"+user+":"+hash+":"+requestFingerprint;if(!requestGate.ready(key,AutoGallery.idle(this),now))return;try{requests.refresh(profile,user,hash);requestGate.accepted(System.currentTimeMillis());}catch(Exception e){requestGate.failed(System.currentTimeMillis());EdenRequests.status="Request registry waiting: "+e.getMessage();}}
    private void syncResearch(String profile,String user,String hash){long now=System.currentTimeMillis();String key=hash.isEmpty()?"":profile+":"+user+":"+hash+":"+researchFingerprint;if(!researchGate.ready(key,AutoGallery.idle(this),now))return;try{research.refresh(profile,user,hash);researchGate.accepted(System.currentTimeMillis());}catch(Exception e){researchGate.failed(System.currentTimeMillis());}}
    static String script(File output,int uid,String bound,String skip){
        if(!bound.matches("[A-Fa-f0-9]{32}|"))throw new IllegalArgumentException("Invalid bound Eden user");
        if(!skip.matches("[a-f0-9]{64}|"))throw new IllegalArgumentException("Invalid metadata fingerprint");
        String out="'"+output.getAbsolutePath().replace("'","'\\''")+"'";
        return """
            snap_pid=$(pidof dev.igawa6.edenduo)
            case "$snap_pid" in '') snap_pid=0;; *[!0-9]*) echo no-game; exit 0;; esac
            snap_root=/sdcard/Android/data/dev.igawa6.edenduo/files
            snap_game=$(grep 'Core .*Load: Loading ' "$snap_root/log/eden_log.txt" | tail -n 1)
            if [ "$snap_pid" != 0 ]; then
              case "$snap_game" in *'(0100F4300BF2C000)'*) ;; *) echo other-game; exit 0;; esac
            fi
            snap_top=$(dumpsys activity activities 2>/dev/null | awk '/^Display #/{primary=($2=="#0")} primary && /topResumedActivity=/ && !found {print;found=1}')
            snap_visible=0
            if [ "$snap_pid" != 0 ]; then
              case "$snap_top" in *' dev.igawa6.edenduo/org.yuzu.yuzu_emu.activities.EmulationActivity'*) snap_visible=1;; esac
            fi
            snap_closed_copy=0
            if [ "$snap_pid" = 0 ]; then
              case "$snap_top" in *' rip.moth.cocoonshell/.MainActivity}'*|*' com.android.launcher3/'*) snap_closed_copy=1;; esac
            fi
            snap_count=0
            for snap_file in "$snap_root"/nand/user/save/0000000000000000/*/0100F4300BF2C000/6706e7d0f3dab2ab; do
              [ -f "$snap_file" ] || continue
              snap_user=${snap_file%/0100F4300BF2C000/6706e7d0f3dab2ab}; snap_user=${snap_user##*/}
              [ ${#snap_user} = 32 ] || continue
              case "$snap_user" in *[!A-Fa-f0-9]*) continue;; esac
            """+(bound.isEmpty()?"":"  [ \"$snap_user\" = '"+bound+"' ] || continue\n")+"""
              snap_count=$((snap_count+1)); snap_source=$snap_file; snap_profile=$snap_user
            done
            [ "$snap_count" -gt 0 ] || { echo "snap|$snap_pid|||no-save|$snap_visible"; exit 0; }
            [ "$snap_count" = 1 ] || { echo multiple-saves; exit 0; }
            snap_dir=${snap_source%/*}
            snap_stamp() {
              [ -f "$1" ] && [ "$(stat -c %s "$1" 2>/dev/null)" = "$2" ] || return 0
              stat -c '%i:%s:%Y:%y' "$1" 2>/dev/null | sha256sum | cut -d ' ' -f 1
            }
            snap_requests=$(snap_stamp "$snap_dir/75006a27cf0c9275" 19248)
            snap_records=$(snap_stamp "$snap_dir/4b1a0e859a3f7816" 15568)
            snap_flags=$(snap_stamp "$snap_dir/76401e93e6565bd5" 3056)
            snap_research=
            if [ -n "$snap_records" ] && [ -n "$snap_flags" ]; then snap_research=$(printf '%s:%s' "$snap_records" "$snap_flags" | sha256sum | cut -d ' ' -f 1); fi
            snap_before=$(stat -c '%i:%s:%Y:%y' "$snap_source" | sha256sum | cut -d ' ' -f 1)
            """+"if [ \"$snap_before\" = '"+skip+"' ]; then echo \"snap3|$snap_pid|$snap_profile|$snap_before|same|$snap_visible|$snap_requests|$snap_research|$snap_closed_copy\"; exit 0; fi\n"+
            "[ \"$(stat -c %s \"$snap_source\")\" = 240496 ] || { echo \"snap|$snap_pid|$snap_profile|$snap_before|unsupported|$snap_visible\"; exit 0; }\n"+
            "cp \"$snap_source\" "+out+" || exit 1\n"+"""
            snap_after=$(stat -c '%i:%s:%Y:%y' "$snap_source" | sha256sum | cut -d ' ' -f 1)
            """+"[ \"$snap_before\" = \"$snap_after\" ] || { rm -f "+out+"; echo \"snap|$snap_pid|$snap_profile|$snap_before|changing|$snap_visible\"; exit 0; }\n"+
            "chown "+uid+":"+uid+" "+out+"\nchmod 600 "+out+"\n"+
            "echo \"snap3|$snap_pid|$snap_profile|$snap_before|copied|$snap_visible|$snap_requests|$snap_research|$snap_closed_copy\"\n";
    }
    private static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    /** Read-only standard service dump for later authorized benchmarks, outside product navigation. */
    @Override protected void dump(FileDescriptor fd,PrintWriter out,String[]args){try{long now=System.currentTimeMillis();out.println("snap-metrics|"+new org.json.JSONObject().put("schema",2).put("kind","link").put("session",metricsSession).put("sourceEpoch",connection.generation()).put("elapsedRealtimeMillis",SystemClock.elapsedRealtime()).put("active",active).put("gameVerified",trusted(now)).put("gameVisible",visible(now)).put("checks",checks).put("pendingImages",gallery==null?0:gallery.pendingCount()));}catch(Exception e){out.println("snap-metrics|{\"schema\":2,\"kind\":\"link\",\"available\":false}");}}
    @Override public void onTimeout(int id,int type){status="Android ended the link session; reconnect in Camp";stopSelf();}
    @Override public void onDestroy(){closing=true;active=false;disconnected();if(worker!=null)worker.shutdownNow();if(gallery!=null)gallery.close();if(snapshot!=null)snapshot.delete();super.onDestroy();}
}
