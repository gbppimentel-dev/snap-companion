package dev.gbppimentel.snapcompanion;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.util.concurrent.*;

/** Always-live policy for the owner's Thor. No shell arguments come from UI or external input. */
final class LiveSession {
    static volatile String status="Connecting live research…";
    static volatile String gameIdentity="Running game compatibility not verified yet";
    private static volatile String identityPid="";
    private static String proofPid="";
    private static long proofAt;
    private static boolean proving;
    private static boolean resumeRequested;
    private static boolean starting,suspended;
    private static long nextAttempt;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    static synchronized void appOpened(Context context){
        suspended=false;nextAttempt=0;resumeRequested=true;
        context.getSharedPreferences("snap",0).edit().putBoolean("alwaysLive",true)
                .putBoolean("edenAutoLink",true).putBoolean("autoGallery",true)
                .putBoolean("followLive",true).putBoolean("pauseRecognition",false).apply();
        ensure(context);
    }
    static synchronized void revoked(){suspended=true;status="Android stopped live capture · reopen the companion to restore the session";}
    static synchronized void ensure(Context source){
        if(CaptureService.active){status="Live upper-screen reader running automatically";return;}
        if(!EdenPerformance.supported()||CaptureService.active||starting||suspended||System.currentTimeMillis()<nextAttempt)return;
        starting=true;nextAttempt=System.currentTimeMillis()+10000;Context context=source.getApplicationContext();
        WORK.execute(()->{
            try{
                File jar=bridge(context);
                String path="'"+jar.getAbsolutePath().replace("'","'\\''")+"'";
                String result=AynBridge.run(context.getCacheDir(),"CLASSPATH="+path+" app_process /system/bin dev.gbppimentel.snapbridge.LiveProjection\n");
                status=result.contains("snap-live-started")?"Live session starting automatically":"Live connection retrying · "+result;
            }catch(Exception e){status="Live connection retrying · "+e.getClass().getSimpleName();}
            finally{synchronized(LiveSession.class){starting=false;}}
        });
    }
    static synchronized void verifyGame(Context source,String pid){
        if(!pid.equals(identityPid)){gameIdentity="Current Eden process compatibility not verified yet";identityPid=pid;}
        long now=System.currentTimeMillis();if(!EdenPerformance.supported()||!pid.matches("[0-9]+")||proving||(pid.equals(proofPid)&&now-proofAt<60000))return;
        proofPid=pid;proofAt=now;proving=true;Context context=source.getApplicationContext();
        WORK.execute(()->{try{File jar=bridge(context);String path="'"+jar.getAbsolutePath().replace("'","'\\''")+"'";String result=AynBridge.run(context.getCacheDir(),"CLASSPATH="+path+" app_process /system/bin dev.gbppimentel.snapbridge.GameMemoryProof\n");String[] fields=result.split("\\|");
            if(EdenLinkService.gameRunning&&pid.equals(EdenLinkService.gamePid))gameIdentity=fields.length==4&&fields[1].equals("compatible-2.0.1")&&fields[2].equals(pid)?"2.0.1 memory fingerprints match · read-only check "+fields[3]+" ms":"Game memory compatibility unverified · "+(fields.length>1?fields[1]:"reader unavailable");
        }catch(Exception e){gameIdentity="Game compatibility check unavailable";}finally{synchronized(LiveSession.class){proving=false;}}});
    }
    static String observedIdentity(){return EdenLinkService.gameRunning&&EdenLinkService.gamePid.equals(identityPid)?gameIdentity:"No verified current game identity";}
    static synchronized void resumeOnOpen(Context source){
        if(!resumeRequested||!EdenPerformance.supported()||!EdenLinkService.gameVisible)return;resumeRequested=false;Context context=source.getApplicationContext();
        WORK.execute(()->{try{File jar=bridge(context);String path="'"+jar.getAbsolutePath().replace("'","'\\''")+"'";AynBridge.run(context.getCacheDir(),"CLASSPATH="+path+" app_process /system/bin dev.gbppimentel.snapbridge.ResumeVisibleEden\n");}catch(Exception ignored){}});
    }
    static synchronized File bridge(Context context)throws Exception{
        byte[] bytes;try(InputStream in=context.getAssets().open("bridge/live-projection.jar")){bytes=MainActivity.readBounded(in,65536);}
        byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder id=new StringBuilder();for(byte b:digest)id.append(String.format(java.util.Locale.ROOT,"%02x",b&255));File file=new File(context.getCacheDir(),"live-projection-"+id+".jar");
        if(!file.exists()){AtomicFile target=new AtomicFile(file);FileOutputStream out=null;try{out=target.startWrite();out.write(bytes);target.finishWrite(out);}catch(IOException e){if(out!=null)target.failWrite(out);throw e;}}return file;
    }
}
