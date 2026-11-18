package dev.gbppimentel.snapcompanion;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Narrow SAF read grant, metadata polling, two equal private snapshots, then native validation. */
public final class SaveWatchService extends Service {
    public static volatile boolean active;
    public static volatile String status="Save watcher stopped";
    public static volatile long checks,parses,lastCheckedAt;
    private ScheduledExecutorService worker;
    private SharedPreferences prefs;
    private Uri uri;
    private String profileId;
    private EdenPhotodex decoder;
    private String previousHash="",acceptedHash="",attemptedHash="",lastValidationError="",lastMetadata="";
    private long lastCopied;
    private volatile boolean closing;
    private record Metadata(String name,long size,long modified){String signature(){return name+":"+size+":"+modified;}}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(intent==null||active){if(!active)stopSelf();return START_NOT_STICKY;}
        try{
            prefs=getSharedPreferences("snap",MODE_PRIVATE);profileId=new ProfileStore(prefs).active();decoder=SaveDecoders.photodex(this);uri=intent.getData();if(uri==null||!"content".equals(uri.getScheme()))throw new IllegalArgumentException("Select a document first");
            NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("save-watch","Read-only save watcher",NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop=PendingIntent.getService(this,8,new Intent(this,SaveWatchService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent open=PendingIntent.getActivity(this,9,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            startForeground(8,new Notification.Builder(this,"save-watch").setSmallIcon(android.R.drawable.ic_menu_save).setContentTitle("Snap read-only save watcher").setContentText("Checks your selected document; never writes the save").setContentIntent(open).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).setOngoing(true).build(),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            int seconds=prefs.getInt("savePollSeconds",5);if(seconds!=5&&seconds!=15&&seconds!=30)seconds=5;
            checks=parses=lastCheckedAt=0;active=true;status="Waiting for two matching snapshots · "+seconds+" second metadata checks";
            worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(this::check,0,seconds,TimeUnit.SECONDS);
        }catch(Exception e){status="Save watcher could not start: "+e.getClass().getSimpleName();stopSelf();}
        return START_NOT_STICKY;
    }
    private Metadata metadata(){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED},null,null,null)){
            if(c!=null&&c.moveToFirst())return new Metadata(c.getString(0),c.isNull(1)?-1:c.getLong(1),c.isNull(2)?0:c.getLong(2));
        }catch(RuntimeException ignored){}
        return new Metadata("selected document",-1,0);
    }
    private void check(){
        if(closing)return;File snapshot=null;
        try{
            if(!new ProfileStore(prefs).active().equals(profileId)){status="Profile changed · watcher stopped";stopSelf();return;}checks++;lastCheckedAt=System.currentTimeMillis();Metadata before=metadata();
            if(before.size()>NewSnapSave.MAX_FILE)throw new IOException("Selected document exceeds 2 GiB");
            // Periodic rehash also catches providers that keep an unchanged/unknown modification timestamp.
            if(!previousHash.isEmpty()&&previousHash.equals(attemptedHash)&&before.signature().equals(lastMetadata)&&lastCheckedAt-lastCopied<rehashInterval(before.size())){status=unchanged();return;}
            snapshot=File.createTempFile("snap-save-",".snapshot",getCacheDir());MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream raw=getContentResolver().openInputStream(uri);OutputStream out=new SparseOutputStream(snapshot)){
                if(raw==null)throw new IOException("Provider did not open the selected document");try(InputStream in=new DigestInputStream(raw,digest)){SaveInspector.copy(in,out,NewSnapSave.MAX_FILE);}
            }
            lastCopied=System.currentTimeMillis();Metadata after=metadata();String hash=hex(digest.digest());
            if(!before.signature().equals(after.signature())||(before.size()>=0&&before.size()!=snapshot.length())){previousHash="";status="Save changed during copy · waiting for a stable snapshot";return;}
            lastMetadata=after.signature();
            if(!hash.equals(previousHash)){previousHash=hash;status="New snapshot · waiting for a second identical read";return;}
            if(hash.equals(attemptedHash)){status=unchanged();return;}
            attemptedHash=hash;parses++;SaveInspector.Result result;
            try{result=SaveInspector.inspect(snapshot,after.name(),getCacheDir(),decoder);lastValidationError="";}catch(IOException e){lastValidationError=e.getMessage();throw e;}
            if(closing||!new ProfileStore(prefs).active().equals(profileId))return;acceptedHash=hash;
            if(result.photodex()!=null)new EdenLinkStore(this).publishDocument(profileId,uri.toString(),hash,result.photodex());
            prefs.edit().putString("saveSource","Source: watched Android document · "+after.name()+"\nValidated: "+new Date(lastCopied)+" · "+snapshot.length()+" bytes\nSHA-256: "+hash+"\n"+result.summary()).apply();
            status="Stable snapshot verified · "+result.files().size()+" save container(s)"+(result.photodex()==null?" · no verified progress fields": " · "+result.photodex().photos().size()+" photo categories synchronized");
        }catch(SecurityException e){previousHash="";status="Save not accepted: access revoked · select the document again";stopSelf();}
        catch(Exception e){if(!previousHash.equals(attemptedHash))previousHash="";status="Save not accepted: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
        finally{if(snapshot!=null&&!snapshot.delete())snapshot.deleteOnExit();}
    }
    static long rehashInterval(long size){return size<0||size>16L*1024*1024?15*60_000L:30_000L;}
    private String unchanged(){return attemptedHash.equals(acceptedHash)?"Watching · metadata unchanged · native parses "+parses:"Save not accepted: "+lastValidationError+" · unchanged data not reparsed";}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    @Override public void onTimeout(int startId,int fgsType){status="Android ended the background watcher time allowance; start it again when needed";stopSelf();}
    @Override public void onDestroy(){closing=true;active=false;if(worker!=null)worker.shutdownNow();if(!status.startsWith("Save not")&&!status.startsWith("Android")&&!status.startsWith("Save watcher could"))status="Save watcher stopped";super.onDestroy();}
}
