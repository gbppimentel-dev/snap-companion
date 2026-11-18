package dev.gbppimentel.snapcompanion;

import java.io.*;
import java.nio.*;
import java.util.*;
import org.json.*;

/** Read-only, dimension-guarded metadata schema. Selected-photo totals are derived from six components. */
public final class EdenPhotodex {
    public record Photo(int national,int stars,int score,String medal,int imageSlot,List<Integer> components,int rawScoreField) { public Photo{components=List.copyOf(components);} public Photo(int national,int stars,int score,String medal,int imageSlot,List<Integer> components){this(national,stars,score,medal,imageSlot,components,score);} public Photo(int national,int stars,int score,String medal,int imageSlot){this(national,stars,score,medal,imageSlot,List.of(),score);} public String key(){return Progress.key(national,stars);} }
    public record Snapshot(Map<String,Photo> photos,int mappedSpecies,int unmappedEntries) { public Snapshot{photos=Map.copyOf(photos);} }
    private final Map<Integer,Integer> species=new HashMap<>();
    public EdenPhotodex(String mapping,Catalog catalog)throws JSONException {
        JSONObject root=new JSONObject(mapping);if(root.getInt("schema")!=1)throw new JSONException("Unsupported species map");JSONArray rows=root.getJSONArray("mapping");
        Set<Integer> ids=new HashSet<>();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);int index=row.getInt("index"),national=row.getInt("national");if(index<0||index>=289||catalog.species(national)==null||!ids.add(national)||species.put(index,national)!=null)throw new JSONException("Invalid species map");}
    }
    public Snapshot read(File file)throws IOException {
        Map<String,Photo> photos=new TreeMap<>();int[] counts={0,0};
        NewSnapSave.Report report=NewSnapSave.inspect(file,NewSnapSave.NAMES.get(2),entry->{
            byte[] bytes=entry.payload();if(bytes.length!=784)throw new IOException("Unsupported Photodex record size");
            Integer national=species.get(entry.index());if(national==null){counts[1]++;return;}
            counts[0]++;for(Photo p:decode(bytes,national))photos.put(p.key(),p);
        });
        if(report.header().payloadSize()!=784||report.header().entryCount()!=289)throw new IOException("Unsupported Photodex schema dimensions");
        return new Snapshot(photos,counts[0],counts[1]);
    }
    public static List<Photo> decode(byte[] bytes,int national)throws IOException {
        if(bytes.length!=784||national<1)throw new IOException("Unsupported Photodex record");
        ByteBuffer data=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        // Leading bookkeeping flags vary as photos are replaced; their meaning is not inferred.
        List<Photo> result=new ArrayList<>();String[] medals={"Unspecified","Bronze","Silver","Gold","Diamond"};
        for(int star=0;star<4;star++){
            int image=Short.toUnsignedInt(data.getShort(4+star*2)),total=data.getInt(200+star*4),medal=Byte.toUnsignedInt(bytes[112+star]);
            List<Integer> components=new ArrayList<>();long sum=0;for(int part=0;part<6;part++){int value=data.getInt(12+star*24+part*4);if(value<0||value>99999)throw new IOException("Invalid photo score component");sum+=value;components.add(value);}
            if(image==65535){if(total!=0||sum!=0||medal!=0)throw new IOException("Incomplete photo registration");continue;}
            if(image>=954||total<0||total>99999||sum<=0||sum>99999||medal<1||medal>4)throw new IOException("Photo score, medal or image reference failed validation");
            // +200 is a separate saved score: real replacement records retain 4491 with
            // current components totaling 3752 (Gold). Preserve it without guessing its
            // lifetime/last-evaluation semantics or rejecting the valid selected photo.
            // Stored medal values must agree with independently observed score thresholds.
            int expected=sum>=4000?4:sum>=3500?3:sum>=3000?2:1;
            if(medal!=expected)throw new IOException("Photo medal does not match its score");
            result.add(new Photo(national,star+1,(int)sum,medals[medal],image,components,total));
        }
        return List.copyOf(result);
    }
}
