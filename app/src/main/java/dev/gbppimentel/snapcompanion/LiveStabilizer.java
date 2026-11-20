package dev.gbppimentel.snapcompanion;

import java.util.*;

/** Frame stability does not inflate the recognizer confidence. Each field expires independently. */
public final class LiveStabilizer {
    public record Stable(String key,String value,float confidence,long firstSeen,long lastSeen,int frames,String source,String profileId,String evidence) {
        public boolean fresh(long now) { long ttl=key.equals("remainingPhotos")?3000:key.equals("subject")?5000:30000;return now>=lastSeen&&now-lastSeen<=ttl; }
    }
    private final Map<String,Stable> pending=new LinkedHashMap<>(),stable=new LinkedHashMap<>();
    private String profile=""; private long previousAt = -1;
    private LiveReading.Scene previousScene=LiveReading.Scene.UNKNOWN;private String selectedSubject="";
    public synchronized List<Stable> accept(LiveReading reading,float threshold,int requiredFrames) {
        if(!profile.equals(reading.profileId()) || reading.capturedAt()<previousAt) { pending.clear();stable.clear();profile=reading.profileId();previousAt=-1; }
        if(reading.capturedAt()==previousAt)return List.of(); previousAt=reading.capturedAt();
        if(!reading.recognizedGame() || (reading.scene()==LiveReading.Scene.PAUSED&&!reading.fields().containsKey("course"))) { pending.clear();if(previousScene==LiveReading.Scene.PHOTO_SELECTION)for(String key:List.of("subject","category","photosTaken","photosSelected"))stable.remove(key);selectedSubject="";previousScene=reading.scene();return List.of(); }
        Map<String,LiveReading.Value> input=new LinkedHashMap<>(reading.fields());input.put("screen",new LiveReading.Value(reading.scene().name(),.99f,"Heuristic screen classification; not measured accuracy"));
        if(reading.scene()==LiveReading.Scene.PHOTO_SELECTION){
            if(previousScene!=LiveReading.Scene.PHOTO_SELECTION)for(String key:List.of("subject","category","photosTaken","photosSelected")){pending.remove(key);stable.remove(key);}
            LiveReading.Value subject=input.get("subject");String next=subject!=null&&subject.confidence()>=threshold?subject.value():"";
            if(next.isEmpty()||!next.equals(selectedSubject))for(String key:List.of("subject","category")){pending.remove(key);stable.remove(key);}
            selectedSubject=next;
        }else selectedSubject="";
        previousScene=reading.scene();
        if(reading.scene()==LiveReading.Scene.COURSE_SELECTION||reading.scene()==LiveReading.Scene.COURSE_INTRO||reading.scene()==LiveReading.Scene.EXPEDITION||reading.scene()==LiveReading.Scene.PAUSED){
            LiveReading.Value choice=input.get("course");Stable oldCourse=stable.get("course");
            if(reading.scene()==LiveReading.Scene.COURSE_SELECTION&&!input.containsKey("researchLevel")){stable.remove("researchLevel");pending.remove("researchLevel");}
            if(reading.scene()==LiveReading.Scene.COURSE_SELECTION&&choice==null){stable.remove("course");stable.remove("researchLevel");pending.remove("researchLevel");}
            else if(choice!=null&&oldCourse!=null&&!oldCourse.value().equals(choice.value())){stable.remove("course");stable.remove("researchLevel");pending.remove("researchLevel");}
        }
        pending.keySet().removeIf(key -> !input.containsKey(key)); List<Stable> changed=new ArrayList<>();
        for(Map.Entry<String,LiveReading.Value> item:input.entrySet()) {
            String key=item.getKey();LiveReading.Value value=item.getValue();
            if(value.confidence()<threshold) {pending.remove(key);continue;}
            Stable old=pending.get(key);boolean same=old!=null&&old.value().equals(value.value())&&reading.capturedAt()-old.lastSeen()<3000;
            Stable next=new Stable(key,value.value(),value.confidence(),same?old.firstSeen():reading.capturedAt(),reading.capturedAt(),same?old.frames()+1:1,reading.source(),reading.profileId(),value.evidence());pending.put(key,next);
            int needed=reading.scene()==LiveReading.Scene.COURSE_INTRO&&value.confidence()>=.9f&&(key.equals("course")||key.equals("researchLevel"))?Math.min(requiredFrames,2):requiredFrames;
            // Menu focus changes faster than course identity. Two distinct strong readings
            // confirm subject/category; a different or missing subject still clears both first.
            if(reading.scene()==LiveReading.Scene.PHOTO_SELECTION&&value.confidence()>=.9f
                &&(key.equals("subject")||key.equals("category")))needed=Math.min(requiredFrames,2);
            if(next.frames()>=needed) {
                Stable existing=stable.put(key,next);
                if(existing==null||!existing.value().equals(next.value())||!existing.fresh(reading.capturedAt()))changed.add(next);
            }
        }
        return List.copyOf(changed);
    }
    public synchronized Map<String,Stable> snapshot() {return Collections.unmodifiableMap(new LinkedHashMap<>(stable));}
    /** Static Android surfaces may emit only one buffer. Dwell can expose it as a single-frame observation,
     * preserving its real timestamp/count/confidence; it is never presented as three independent frames. */
    public synchronized List<Stable> held(LiveReading reading,long now,float threshold) {
        if(!reading.profileId().equals(profile)||reading.capturedAt()!=previousAt||!reading.recognizedGame()||(reading.scene()==LiveReading.Scene.PAUSED&&!reading.fields().containsKey("course"))||now-reading.capturedAt()<1000||now-reading.capturedAt()>2500)return List.of();
        List<Stable> changed=new ArrayList<>();
        for(Stable p:pending.values())if(p.confidence()>=threshold&&(!stable.containsKey(p.key())||!stable.get(p.key()).value().equals(p.value())||p.lastSeen()>stable.get(p.key()).lastSeen())){
            Stable held=new Stable(p.key(),p.value(),p.confidence(),p.firstSeen(),p.lastSeen(),p.frames(),p.source(),p.profileId(),p.evidence()+" · still frame held ≥1s; confidence unchanged");stable.put(p.key(),held);changed.add(held);
        }
        return List.copyOf(changed);
    }
    public synchronized void clear() {pending.clear();stable.clear();previousAt=-1;previousScene=LiveReading.Scene.UNKNOWN;selectedSubject="";}
}
