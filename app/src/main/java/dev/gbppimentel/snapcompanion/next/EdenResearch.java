package dev.gbppimentel.snapcompanion.next;

import android.content.Context;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.json.*;

/** Owned2.0.1 saved course research. Never changes live selected context or manual progress. */
final class EdenResearch {
    record Course(String id,int status,long plays,int subjects,int points,String level,int grossPoints,int pointOffset,int discoveryMask){}
    record Snapshot(List<Course> courses,long at,String user,String metadataHash){Snapshot{courses=List.copyOf(courses);}}
    private static byte[] payload(File file,int index,int expected)throws IOException{
        List<byte[]> found=new ArrayList<>();NewSnapSave.Report report=NewSnapSave.inspect(file,NewSnapSave.NAMES.get(index),e->{if(e.index()!=0||e.payload().length!=expected)throw new IOException("Unsupported course ledger dimensions");found.add(e.payload());});
        if(report.header().entryCount()!=1||report.header().payloadSize()!=expected||found.size()!=1)throw new IOException("Incomplete course ledger");return found.get(0);
    }
    static List<Course> decode(byte[] records,byte[] flags,String guide)throws Exception{
        if(records.length!=15464||flags.length!=2960)throw new IOException("Unsupported course ledger dimensions");
        JSONArray courses=new JSONObject(guide).getJSONArray("courses");ByteBuffer data=ByteBuffer.wrap(records).order(ByteOrder.LITTLE_ENDIAN);List<Course> out=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        for(int i=0;i<courses.length();i++){
            JSONObject row=courses.getJSONObject(i);int number=row.getInt("gameId"),base=(number-1)*504;if(number<1||number>30||!ids.add(number))throw new IOException("Invalid course mapping");
            int status=Byte.toUnsignedInt(flags[number-1]);long plays=Integer.toUnsignedLong(data.getInt(base));if(status>2||plays>1000000)throw new IOException("Unsupported course state");
            int points=0,count=0;Set<String> subjects=new HashSet<>();boolean ended=false;
            // The second u16 distinguishes variants. Florges has separate entries for colors.
            for(int k=0;k<40;k++){int offset=base+8+k*12,id=Short.toUnsignedInt(data.getShort(offset)),variant=Short.toUnsignedInt(data.getShort(offset+2));if(id==0){ended=true;continue;}if(ended||id>1000||variant>255||!subjects.add(id+":"+variant))throw new IOException("Malformed course research entries");count++;for(int star=0;star<4;star++)points+=Short.toUnsignedInt(data.getShort(offset+4+star*2));}
            if(status<2&&(plays!=0||points!=0||count!=0))throw new IOException("Course state conflicts with its research entries");
            JSONArray thresholds=row.getJSONArray("rawResearchThresholds");int two=thresholds.getInt(0),three=thresholds.getInt(1),max=thresholds.getInt(2);if(two<0||three<0||max<=0||max<two||max<three)throw new IOException("Invalid research thresholds");
            int gross=points,offset=(records[base+497]&255)|((records[base+498]&255)<<8)|((records[base+499]&255)<<16);points=Math.max(0,gross-offset);
            String level=points>=max?"MAX":three>0&&points>=three?"Lv. 3":two>0&&points>=two?"Lv. 2":"Lv. 1";
            out.add(new Course(row.getString("id"),status,plays,count,points,level,gross,offset,Short.toUnsignedInt(data.getShort(base+488))));
        }
        if(out.size()!=30)throw new IOException("Incomplete course mapping");return List.copyOf(out);
    }
    static Snapshot read(Context context,String user,String hash)throws Exception{
        if(!user.matches("[A-Fa-f0-9]{32}")||!hash.matches("[a-f0-9]{64}"))throw new IOException("Invalid course source identity");
        File directory=new File(context.getCacheDir(),"research-"+UUID.randomUUID());if(!directory.mkdir())throw new IOException("Cannot create private course snapshot");
        try{String result=AynBridge.run(context.getCacheDir(),script(directory,android.os.Process.myUid(),user,hash));if(!result.equals("stable"))throw new IOException("Course snapshot waiting: "+result);String guide;try(InputStream in=context.getAssets().open("guidebook.json")){guide=new String(MainActivity.readBounded(in,2000000),java.nio.charset.StandardCharsets.UTF_8);}return new Snapshot(decode(payload(new File(directory,"records"),9,15464),payload(new File(directory,"flags"),11,2960),guide),System.currentTimeMillis(),user,hash);}finally{File[] files=directory.listFiles();if(files!=null)for(File file:files)file.delete();directory.delete();}
    }
    static String script(File output,int uid,String user,String hash){
        if(!user.matches("[A-Fa-f0-9]{32}")||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid course source");String dir="'"+output.getAbsolutePath().replace("'","'\\''")+"'";
        return "snap_dir=/sdcard/Android/data/dev.eden.eden_emulator/files/nand/user/save/0000000000000000/"+user+"/0100F4300BF2C000\n"+
            "[ \"$(stat -c %s \"$snap_dir/4b1a0e859a3f7816\")\" = 15568 ] && [ \"$(stat -c %s \"$snap_dir/76401e93e6565bd5\")\" = 3056 ] || { echo unsupported; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "snap_records=$(sha256sum \"$snap_dir/4b1a0e859a3f7816\" | cut -d ' ' -f 1)\nsnap_flags=$(sha256sum \"$snap_dir/76401e93e6565bd5\" | cut -d ' ' -f 1)\n"+
            "cp \"$snap_dir/4b1a0e859a3f7816\" "+dir+"/records || exit 1\ncp \"$snap_dir/76401e93e6565bd5\" "+dir+"/flags || exit 1\n"+
            "[ \"$(sha256sum \"$snap_dir/4b1a0e859a3f7816\" | cut -d ' ' -f 1)\" = \"$snap_records\" ] && [ \"$(sha256sum \"$snap_dir/76401e93e6565bd5\" | cut -d ' ' -f 1)\" = \"$snap_flags\" ] || { echo changing; exit 0; }\n"+
            "[ \"$(sha256sum "+dir+"/records | cut -d ' ' -f 1)\" = \"$snap_records\" ] && [ \"$(sha256sum "+dir+"/flags | cut -d ' ' -f 1)\" = \"$snap_flags\" ] || { echo incomplete; exit 0; }\n"+
            "[ \"$(sha256sum \"$snap_dir/6706e7d0f3dab2ab\" | cut -d ' ' -f 1)\" = '"+hash+"' ] || { echo changed; exit 0; }\n"+
            "chown "+uid+":"+uid+" "+dir+"/records "+dir+"/flags\nchmod 600 "+dir+"/records "+dir+"/flags\necho stable\n";
    }
}
