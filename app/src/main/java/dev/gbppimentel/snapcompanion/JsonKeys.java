package dev.gbppimentel.snapcompanion;
import org.json.JSONObject;
import java.util.*;
final class JsonKeys {
    private JsonKeys(){}
    static List<String> of(JSONObject object){List<String> out=new ArrayList<>();Iterator<String> it=object.keys();while(it.hasNext())out.add(it.next());return out;}
}
