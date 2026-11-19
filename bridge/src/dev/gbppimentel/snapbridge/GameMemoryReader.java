// SPDX-License-Identifier: GPL-3.0-or-later
package dev.gbppimentel.snapbridge;
import android.os.Build;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;
/** Fixed-game, read-only persistent sampler. The native reader never exposes writes or address arguments. */
public final class GameMemoryReader {
 private static final File DIR=new File("/data/user/0/dev.gbppimentel.snapcompanion/files"),WATCH=new File(DIR,"memory.watch"),STATE=new File(DIR,"memory.json");
 private static native boolean open(int pid);static native String sample();static native boolean physicalReleased(int code);static native boolean pressTool(int code);private static native void close();
 private static String token;
 private static String text(File f)throws Exception{if(f.length()>8192)throw new IOException();return new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).trim();}
 static boolean alive(){try{long age=System.currentTimeMillis()-WATCH.lastModified();return age>=0&&age<4000&&text(WATCH).equals(token);}catch(Exception e){return false;}}
 private static void publish(JSONObject out)throws Exception{
  out.put("token",token).put("at",System.currentTimeMillis());File next=new File(DIR,"memory.next."+android.os.Process.myPid());
  try(FileOutputStream stream=new FileOutputStream(next)){stream.write(out.toString().getBytes(StandardCharsets.UTF_8));}next.setReadable(true,false);if(!next.renameTo(STATE)){next.delete();throw new IOException();}
 }
 public static void main(String[]args){
  try{
   if(android.os.Process.myUid()!=0||!"AYN Thor".equalsIgnoreCase(Build.MODEL.replace('_',' '))||args.length!=1||!args[0].matches("/data/app/[A-Za-z0-9_~+./=-]+/lib/arm64/libsnapmemory\\.so"))throw new SecurityException();
   android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
   token=text(WATCH);if(!token.matches("[a-f0-9-]{36}")||!alive())return;
   try(RandomAccessFile file=new RandomAccessFile(new File(DIR,"memory.lock"),"rw");FileLock lock=file.getChannel().tryLock()){
    if(lock==null)return;
    PrintStream original=System.out;ByteArrayOutputStream bytes=new ByteArrayOutputStream();try{System.setOut(new PrintStream(bytes));GameMemoryProof.main(new String[0]);}finally{System.setOut(original);}
    String[] proof=bytes.toString("UTF-8").trim().split("\\|");
    if(proof.length!=4||!proof[1].equals("compatible-2.0.1")||!proof[2].matches("[0-9]+")){publish(new JSONObject().put("status","unsupported"));return;}
    int pid=Integer.parseInt(proof[2]);System.load(args[0]);if(!open(pid)){publish(new JSONObject().put("status","unavailable"));return;}
    try{int failures=0;while(alive()){long start=System.nanoTime();JSONObject row=new JSONObject(sample()).put("pid",pid);try{GameToolInput.handle(token,row);GameToolInput.addFeedback(row);}catch(Throwable e){android.util.Log.w("SnapTool","tool-guard-error:"+e.getClass().getSimpleName());}publish(row);String status=row.optString("status");failures=status.equals("unavailable")?failures+1:0;if(failures>=20)break;
      // Low-priority discovery advances in short slices; live reads remain 20 Hz.
      long remaining=(status.equals("seeking")?10:50)-(System.nanoTime()-start)/1000000;if(remaining>0)Thread.sleep(remaining);}}
    finally{close();}
   }
  }catch(Throwable e){try{if(token!=null)publish(new JSONObject().put("status","unavailable"));}catch(Exception ignored){}}
 }
}
