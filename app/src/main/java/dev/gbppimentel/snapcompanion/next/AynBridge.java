package dev.gbppimentel.snapcompanion.next;

import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Fixed-purpose callers only. Never exposed to UI, intents, URLs or user-provided shell text. */
final class AynBridge {
    static synchronized String run(File cache,String scriptText)throws Exception {
        File script=File.createTempFile("snap-bridge-",".sh",cache);
        try {
            try(FileOutputStream out=new FileOutputStream(script)){out.write(scriptText.getBytes(StandardCharsets.UTF_8));}
            IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"PServerBinder");
            if(binder==null)throw new IOException("AYN firmware bridge unavailable");
            Parcel request=Parcel.obtain(),reply=Parcel.obtain();
            try{request.writeStringArray(new String[]{"sh '"+script.getAbsolutePath().replace("'","'\\''")+"'","0"});if(!binder.transact(0,request,reply,0))throw new IOException("Firmware bridge declined");byte[] result=reply.createByteArray();return result==null?"":new String(result,StandardCharsets.UTF_8).trim();}
            finally{request.recycle();reply.recycle();}
        }finally{script.delete();}
    }
}
