package dev.gbppimentel.snapcompanion.next;
import org.json.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
/** Small factual corrections require the exact active dataset; validation precedes every file write. */
final class GuideUpdates {
    static String resolve(String current,String incoming)throws Exception{
        JSONObject patch=new JSONObject(incoming);if(!patch.optString("format").equals("snap-guide-update"))return incoming;
        JSONObject base=new JSONObject(current);if(patch.getInt("version")!=1||!patch.getString("baseRevision").equals(base.getString("revision"))||!patch.getString("baseSha256").equals(sha256(current)))throw new JSONException("This update does not match the active guide; no data changed");
        String revision=patch.getString("revision");if(revision.isBlank()||revision.length()>80||revision.equals(base.getString("revision")))throw new JSONException("Guide update needs a new revision");JSONArray changes=patch.getJSONArray("changes");if(changes.length()<1||changes.length()>500)throw new JSONException("Guide update needs 1–500 corrections");Set<String> seen=new HashSet<>();
        for(int i=0;i<changes.length();i++){JSONObject change=changes.getJSONObject(i);String section=change.getString("section"),key=change.getString("key");if(!Set.of("species","courses","encounters","photoCues","requests","features","unlocks").contains(section)||!seen.add(section+":"+key))throw new JSONException("Unsupported or repeated correction");JSONArray rows=base.getJSONArray(section);int found=-1;for(int j=0;j<rows.length();j++)if(key(section,rows.getJSONObject(j)).equals(key)){if(found!=-1)throw new JSONException("Ambiguous correction key");found=j;}JSONObject value=change.getJSONObject("value");if(found<0||!key(section,value).equals(key))throw new JSONException("A correction must retain its known identifier");rows.put(found,value);}
        base.put("revision",revision);return base.toString();
    }
    static String sha256(String value)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private static String key(String section,JSONObject r)throws JSONException{return switch(section){case "species","courses","requests"->r.get("id").toString();case "photoCues"->r.getInt("species")+":"+r.getInt("stars");case "encounters"->r.getString("course")+":"+r.getInt("species")+":"+r.optString("form");case "features"->r.getString("id");case "unlocks"->r.getString("kind")+":"+r.getString("name");default->throw new JSONException("Unknown guide section");};}
}
