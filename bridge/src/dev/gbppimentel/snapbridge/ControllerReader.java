package dev.gbppimentel.snapbridge;
import dev.gbppimentel.snapcompanion.ControllerButtons;
import java.io.*;import java.nio.*;import java.nio.channels.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.JSONObject;
/** Fixed-package passive input observer, owned by the visible companion's heartbeat. */
public final class ControllerReader {
 static final File DIR=new File("/data/user/0/dev.gbppimentel.snapcompanion/files"),WATCH=new File(DIR,"controller.watch"),STATE=new File(DIR,"controller.json");
 static final ControllerButtons buttons=new ControllerButtons();static String token;static volatile boolean axesDirty;static volatile long publishedAt;
 static String text(File f)throws IOException{if(f.length()>8192)throw new IOException("Oversized input state");return new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).trim();}
 static boolean alive(){try{long age=System.currentTimeMillis()-WATCH.lastModified();return WATCH.exists()&&age>=0&&age<4000&&text(WATCH).equals(token);}catch(Exception e){return false;}}
 static String device()throws IOException{File[] entries=new File("/sys/class/input").listFiles();String selected=null;int best=0,count=0;if(entries!=null)for(File f:entries)if(f.getName().matches("event[0-9]+"))try{int score=ControllerButtons.builtInScore(text(new File(f,"device/name")));if(score>best){selected="/dev/input/"+f.getName();best=score;count=1;}else if(score>0&&score==best)count++;}catch(Exception ignored){}if(selected==null||count!=1)throw new IOException("No unique built-in controller");return selected;}
 static synchronized void publish(boolean available)throws Exception{axesDirty=false;publishedAt=System.currentTimeMillis();JSONObject out=new JSONObject().put("token",token).put("at",System.currentTimeMillis()).put("available",available).put("buttons",new JSONObject(buttons.down())).put("pulses",new JSONObject(buttons.pulses())).put("axes",new JSONObject(buttons.axes()));File next=new File(DIR,"controller.next."+android.os.Process.myPid());try(FileOutputStream stream=new FileOutputStream(next)){stream.write(out.toString().getBytes(StandardCharsets.UTF_8));}next.setReadable(true,false);if(!next.renameTo(STATE)){next.delete();throw new IOException("Input frame unavailable");}}
 public static void main(String[] args){try{token=text(WATCH);if(!token.matches("[a-f0-9-]{36}")||!alive())return;try(RandomAccessFile lockFile=new RandomAccessFile(new File(DIR,"controller.lock"),"rw");FileLock lock=lockFile.getChannel().tryLock()){
   if(lock==null)return;try(DataInputStream input=new DataInputStream(new FileInputStream(device()))){
    publish(true);Thread monitor=new Thread(()->{while(true)try{Thread.sleep(33);if(!alive()){publish(false);System.exit(0);}if(axesDirty||System.currentTimeMillis()-publishedAt>=500)publish(true);}catch(Exception e){System.exit(0);}},"SnapInputHeartbeat");monitor.setDaemon(true);monitor.start();
    byte[] record=new byte[24];ByteBuffer b=ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN);while(alive()){input.readFully(record);int type=b.getShort(16)&65535,code=b.getShort(18)&65535;if(buttons.accept(type,code,b.getInt(20),System.currentTimeMillis())){if(type==3&&(code==0||code==1||code==2||code==5))axesDirty=true;else publish(true);}}
   }
  }}catch(Exception e){try{if(token!=null)publish(false);}catch(Exception ignored){}}}
}
