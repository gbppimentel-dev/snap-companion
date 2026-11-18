package dev.gbppimentel.snapcompanion;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** On-demand read of one encrypted image record; never copies the full 439 MB archive. */
final class EdenGallery {
    static final class EmptyPhoto extends IOException {EmptyPhoto(){super("Saved image slot is empty");}}
    static byte[] read(Context context,String user,String metadataHash,int slot)throws Exception{
        return readRecord(context,user,metadataHash,"",slot,3);
    }
    static byte[] readAlbum(Context context,String user,String metadataHash,String albumHash,int slot,boolean thumbnail)throws Exception{return readRecord(context,user,metadataHash,albumHash,slot,thumbnail?10:7);}
    private static byte[] readRecord(Context context,String user,String metadataHash,String albumHash,int slot,int archive)throws Exception{
        int count=archive==3?954:2376,payload=archive==10?30736:460816,encrypted=((payload+15)/16)*16+48;
        if(!Set.of(3,7,10).contains(archive)||!user.matches("[A-Fa-f0-9]{32}")||!metadataHash.matches("[a-f0-9]{64}")||slot<0||slot>=count||archive!=3&&!albumHash.matches("[a-f0-9]{64}"))throw new IOException("No verified linked photo reference");
        File a=File.createTempFile("snap-photo-a-",".record",context.getCacheDir()),b=File.createTempFile("snap-photo-b-",".record",context.getCacheDir());
        try{
            String result=AynBridge.run(context.getCacheDir(),recordScript(a,b,android.os.Process.myUid(),user,metadataHash,albumHash,slot,archive));if(!result.equals("ok"))throw new IOException(switch(result){case "changed"->"The game saved a newer photo. Wait for sync and try again.";case "missing"->"This Eden profile's photo archive is unavailable.";default->"Photo read not accepted: "+result;});
            if(a.length()!=encrypted||b.length()!=encrypted)throw new IOException("Unsupported image record dimensions");byte[] first=java.nio.file.Files.readAllBytes(a.toPath()),second=java.nio.file.Files.readAllBytes(b.toPath());if(!MessageDigest.isEqual(first,second))throw new IOException("Photo changed during read; try again after saving");
            NewSnapSave.Entry entry=NewSnapSave.entry(first,archive,slot,payload);if(!entry.checksumVerified())throw new EmptyPhoto();byte[] jpeg=entry.jpeg();if(jpeg.length==0)throw new IOException("Saved photo does not contain a complete JPEG");return jpeg;
        }finally{a.delete();b.delete();}
    }
    private static String quote(File file){return "'"+file.getAbsolutePath().replace("'","'\\''")+"'";}
    static String script(File a,File b,int uid,String user,String hash,int slot){
        return recordScript(a,b,uid,user,hash,"",slot,3);
    }
    static String recordScript(File a,File b,int uid,String user,String hash,String albumHash,int slot,int archive){
        int count=archive==3?954:2376,payload=archive==10?30736:460816,blocks=(((payload+15)/16)*16+48)/16;
        if(!Set.of(3,7,10).contains(archive)||!user.matches("[A-Fa-f0-9]{32}")||!hash.matches("[a-f0-9]{64}")||slot<0||slot>=count||archive!=3&&!albumHash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid gallery bridge request");long skip=3L+(long)blocks*slot;
        String root="/sdcard/Android/data/dev.eden.eden_emulator/files/nand/user/save/0000000000000000/"+user+"/0100F4300BF2C000/";
        String albumGuard=archive==3?"":"[ \"$(sha256sum '"+root+NewSnapSave.NAMES.get(6)+"' | cut -d ' ' -f 1)\" = '"+albumHash+"' ] || { echo changed; exit 0; }\n";
        return "snap_photo='"+root+NewSnapSave.NAMES.get(archive)+"'\nsnap_metadata='"+root+"6706e7d0f3dab2ab'\n"+
            "[ -f \"$snap_photo\" ] || { echo missing; exit 0; }\n"+
            "[ \"$(stat -c %s \"$snap_photo\")\" = "+(48L+(long)blocks*16*count)+" ] || { echo unsupported; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_metadata\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            albumGuard+
            "dd if=\"$snap_photo\" of="+quote(a)+" bs=16 skip="+skip+" count="+blocks+" 2>/dev/null || exit 1\n"+
            "dd if=\"$snap_photo\" of="+quote(b)+" bs=16 skip="+skip+" count="+blocks+" 2>/dev/null || exit 1\n"+
            "chown "+uid+":"+uid+" "+quote(a)+" "+quote(b)+"\nchmod 600 "+quote(a)+" "+quote(b)+"\n"+
            "[ \"$(sha256sum \"$snap_metadata\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+albumGuard+"echo ok\n";
    }
}
