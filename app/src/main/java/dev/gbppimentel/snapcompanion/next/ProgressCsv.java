package dev.gbppimentel.snapcompanion.next;
import java.util.*;
import org.json.*;

/** RFC 4180 CSV. Companion JSON is embedded as the authoritative lossless backup record. */
final class ProgressCsv {
    static String export(Progress p,Map<String,EdenPhotodex.Photo> linked)throws JSONException{
        StringBuilder out=new StringBuilder("kind,id,stars,score,medal,value,source\r\n");row(out,"backup","snap-companion-progress","1","","",p.toJson(),"manual notebook; lossless restore record");
        for(var e:p.photos.entrySet()){String[] key=e.getKey().split(":");row(out,"photo",key[0],key[1],""+e.getValue().score(),e.getValue().medal(),"","manual");}
        for(var e:linked.values())row(out,"photo",""+e.national(),""+e.stars(),""+e.score(),e.medal(),"","Eden read-only reference");
        for(String id:p.completedRequests)row(out,"request",id,"","","","COMPLETE","manual");
        for(var e:p.notebook.requests.entrySet())if(!p.completedRequests.contains(e.getKey()))row(out,"request",e.getKey(),"","","",e.getValue().name(),"manual");
        for(String key:p.objectives)row(out,"objective",key,"","","","selected","manual");return out.toString();
    }
    static Progress restore(String text,Catalog catalog)throws JSONException{
        List<List<String>> rows=parse(text);if(rows.isEmpty()||!rows.get(0).equals(List.of("kind","id","stars","score","medal","value","source")))throw new JSONException("Unsupported CSV header");
        Progress result=null;for(int i=1;i<rows.size();i++){List<String> row=rows.get(i);if(row.size()!=7)throw new JSONException("CSV row "+(i+1)+" has the wrong number of fields");if(row.get(0).equals("backup")){if(result!=null||!row.get(1).equals("snap-companion-progress"))throw new JSONException("Ambiguous CSV restore records");result=Progress.fromJson(row.get(5),catalog);}}
        if(result==null)throw new JSONException("CSV needs the app's backup row for a lossless restore. Human-readable rows are reference exports.");return result;
    }
    private static void row(StringBuilder out,String... values){for(int i=0;i<values.length;i++){if(i>0)out.append(',');out.append('"').append(values[i].replace("\"","\"\"")).append('"');}out.append("\r\n");}
    static List<List<String>> parse(String text)throws JSONException{
        if(text.length()>2_000_000)throw new JSONException("CSV exceeds 2 MB");if(text.startsWith("\ufeff"))text=text.substring(1);List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++){char c=text.charAt(i);if(quoted){if(c=='"'){if(i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}else cell.append(c);continue;}
            if(c=='"'&&cell.length()==0&&!closed){quoted=true;continue;}if(c==','||c=='\n'||c=='\r'){row.add(cell.toString());cell.setLength(0);closed=false;if(c!=','){rows.add(List.copyOf(row));row.clear();if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;}continue;}if(closed||c=='"')throw new JSONException("Malformed CSV quoting");cell.append(c);
        }if(quoted)throw new JSONException("Unclosed CSV quote");if(cell.length()>0||!row.isEmpty()||closed){row.add(cell.toString());rows.add(List.copyOf(row));}return List.copyOf(rows);
    }
}
