package dev.gbppimentel.snapcompanion.next;

import java.util.*;

/** Capture-owned run context, independent of an Activity or which companion tab is open. */
final class LiveContext {
    private SnapUiState state=new SnapUiState();
    private LiveReading reading;
    private Map<String,LiveStabilizer.Stable> fields=Map.of();
    private String source="";
    synchronized void observe(String source,LiveReading next,Map<String,LiveStabilizer.Stable> values){
        if(source.isEmpty()||next==null)return;
        if(source.equals(this.source)&&reading!=null&&next.capturedAt()<reading.capturedAt())return;
        this.source=source;reading=next;fields=Map.copyOf(values);
        var course=next.fields().get("course");if(course!=null)state.observedCourse(course.value(),next.capturedAt());
        state.update(next.profileId(),source,true,true,true,next.scene(),next.capturedAt(),fields,next.capturedAt());
    }
    synchronized SnapUiState.View view(String profile,String source,boolean running,boolean visible,long now){
        boolean matching=reading!=null&&reading.profileId().equals(profile)&&this.source.equals(source)&&!source.isEmpty();
        boolean fresh=matching&&now>=reading.capturedAt()&&now-reading.capturedAt()<=5000;
        return state.update(profile,source,running,visible,fresh,matching?reading.scene():LiveReading.Scene.UNKNOWN,matching?reading.capturedAt():0,matching?fields:Map.of(),now);
    }
    synchronized void clear(){reading=null;fields=Map.of();source="";state=new SnapUiState();}
}
