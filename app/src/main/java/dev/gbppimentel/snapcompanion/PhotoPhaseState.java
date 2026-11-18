package dev.gbppimentel.snapcompanion;
import java.util.*;
import org.json.*;
/** Exposes only current, stabilized selection fields. Never reuses a previous photo's category. */
final class PhotoPhaseState {
 static JSONObject read(LiveReading reading,Map<String,LiveStabilizer.Stable> stable,String profile,long now)throws JSONException{
  JSONObject out=new JSONObject();if(reading==null||!reading.profileId().equals(profile)||now<reading.capturedAt()||now-reading.capturedAt()>5000||reading.scene()!=LiveReading.Scene.PHOTO_SELECTION)return out;
  for(String key:List.of("subject","category","photosTaken","photosSelected")){
   LiveReading.Value raw=reading.fields().get(key);var field=stable.get(key);
   if(raw==null||raw.confidence()<.8f||field==null||!field.profileId().equals(profile)||!field.value().equals(raw.value())||field.confidence()<.8f||field.lastSeen()>reading.capturedAt()||reading.capturedAt()-field.lastSeen()>2500)continue;
   try{int n=Integer.parseInt(raw.value()),max=key.equals("subject")?1025:key.equals("category")?4:key.startsWith("photos")?72:99999,min=key.equals("subject")||key.equals("category")?1:0;if(n>=min&&n<=max)out.put(key,n);}catch(NumberFormatException ignored){}
  }
  if(!out.has("subject"))out.remove("category");
  return out;
 }
}
