package dev.gbppimentel.snapbridge;
import android.os.Build;
import android.os.Process;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Fixed-process, read-only compatibility proof. Never attaches, stops, writes or accepts addresses. */
public final class GameMemoryProof {
    private static final long[] OFFSETS={4096,5308416,10485760,10719232};
    private static final int[] SIZES={4096,4096,4096,2259908};
    // Eden NCE strengthens these four exclusive instructions to acquire/release variants.
    // Normalize only bit 15 at these owned-code offsets, in the private hash buffer, never in game memory.
    private static final int[][] ATOMIC_BITS={{},{0x534,0x53c,0x968,0x970},{},{}};
    private static final String[] HASHES={"18adab43dcc6e9e0561f0da2dfcfca36143b179fc45231ed4d04c3b04df814b4","a333569de219a40911c975b44d0b7ac314227d98f26363836811a3fc2628d118","34534168e6bd42a057ecb980c9654a326ccb930c5435cb399952c19961006aab","fa3b8fc266923cb318c64779d145184842da51ef9f1057767f09fe34f79187d1"};
    public static void main(String[] args){
        long started=System.nanoTime();
        try{
            if(Process.myUid()!=0||!"AYN Thor".equalsIgnoreCase(Build.MODEL.replace('_',' '))||args.length!=0)throw new SecurityException();
            int pid=0;File[] processes=new File("/proc").listFiles();if(processes==null)throw new IOException();
            for(File process:processes){if(!process.getName().matches("[0-9]+"))continue;try(InputStream in=new FileInputStream(new File(process,"cmdline"))){byte[] name=new byte[128];int n=in.read(name);if(n<=0)continue;int end=0;while(end<n&&name[end]!=0)end++;if(new String(name,0,end,"UTF-8").equals("dev.eden.eden_emulator")){if(pid!=0)throw new IOException("ambiguous");pid=Integer.parseInt(process.getName());}}catch(FileNotFoundException ignored){}}
            if(pid==0){System.out.println("memory|no-game");return;}
            long base=0;int matches=0;List<long[]> readable=new ArrayList<>();
            try(BufferedReader maps=new BufferedReader(new FileReader("/proc/"+pid+"/maps"))){String line;while((line=maps.readLine())!=null){String[] fields=line.trim().split("\\s+");if(!line.contains("/memfd:HostMemory")||!fields[1].startsWith("r"))continue;String[] range=fields[0].split("-");long a=Long.parseLong(range[0],16),b=Long.parseLong(range[1],16);readable.add(new long[]{a,b});if(fields[1].contains("x")&&b-a==0xa39000L){base=a;matches++;}}}
            if(matches!=1){System.out.println("memory|unsupported-map");return;}
            byte[] buffer=new byte[65536];
            try(RandomAccessFile memory=new RandomAccessFile("/proc/"+pid+"/mem","r")){
                for(int i=0;i<OFFSETS.length;i++){long at=base+OFFSETS[i],end=at+SIZES[i];boolean mapped=false;for(long[] r:readable)if(at>=r[0]&&end<=r[1])mapped=true;if(!mapped){System.out.println("memory|unmapped-page");return;}
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");memory.seek(at);int left=SIZES[i],consumed=0;while(left>0){int n=memory.read(buffer,0,Math.min(buffer.length,left));if(n<=0)throw new EOFException();for(int position:ATOMIC_BITS[i])if(position+1>=consumed&&position+1<consumed+n)buffer[position+1-consumed]&=0x7f;digest.update(buffer,0,n);left-=n;consumed+=n;}if(!hex(digest.digest()).equals(HASHES[i])){System.out.println("memory|fingerprint-mismatch-"+i);return;}
                }
            }
            System.out.println("memory|compatible-2.0.1|"+pid+"|"+((System.nanoTime()-started)/1000000));
        }catch(Throwable e){System.out.println("memory|unavailable-"+e.getClass().getSimpleName());}
    }
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
}
