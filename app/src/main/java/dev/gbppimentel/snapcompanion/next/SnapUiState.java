package dev.gbppimentel.snapcompanion.next;
import java.util.*;
/** UI context follows observations. Legacy manual course selection is never an input. */
final class SnapUiState {
    record View(String phase,String course,int level,int subject,Integer film){}
    private String identity="",course="";private int level;private long resetAt,lastFrameAt,lastRecognizedAt,courseAt,finishedAt;private LiveReading.Scene previous=LiveReading.Scene.UNKNOWN;
    View update(String profile,String pid,boolean running,boolean visible,boolean fresh,LiveReading.Scene scene,long sceneAt,Map<String,LiveStabilizer.Stable> fields,long now){
        String next=profile+":"+pid;if(!next.equals(identity)){identity=next;previous=LiveReading.Scene.UNKNOWN;lastRecognizedAt=lastFrameAt=finishedAt=0;clear(Math.max(0,sceneAt-2500));}
        if(!running||!visible){previous=LiveReading.Scene.UNKNOWN;lastRecognizedAt=lastFrameAt=finishedAt=0;clear(now);return new View(running?"background":"waiting","",0,0,null);}
        if(!fresh){if(now<lastFrameAt||now-lastFrameAt>10000)clear(now);return new View("playing",course,level,0,null);}
        if(lastFrameAt>0&&sceneAt-lastFrameAt>10000)clear(sceneAt-2500);
        if(scene==LiveReading.Scene.UNKNOWN){if(sceneAt<lastRecognizedAt||sceneAt-lastRecognizedAt>10000)clear(Math.max(resetAt,sceneAt));return new View("playing",course,Math.max(0,level),0,null);}
        lastRecognizedAt=sceneAt;
        if((scene==LiveReading.Scene.COURSE_SELECTION||scene==LiveReading.Scene.COURSE_INTRO)&&previous!=scene)clear(Math.max(resetAt,sceneAt));
        if(scene==LiveReading.Scene.EXPEDITION&&previous!=scene&&finishedAt>courseAt)clear(Math.max(resetAt,finishedAt));
        if(Set.of(LiveReading.Scene.PHOTO_SELECTION,LiveReading.Scene.EVALUATION,LiveReading.Scene.RESULTS).contains(scene))finishedAt=Math.max(finishedAt,sceneAt);
        previous=scene;lastFrameAt=sceneAt;
        if(scene==LiveReading.Scene.MENU||scene==LiveReading.Scene.TITLE){clear(Math.max(resetAt,sceneAt));return new View(scene==LiveReading.Scene.TITLE?"loading":"camp","",0,0,null);}
        LiveStabilizer.Stable c=value(fields,"course",profile,now),l=value(fields,"researchLevel",profile,now);
        if(scene==LiveReading.Scene.COURSE_SELECTION&&l==null)level=0;
        if(scene==LiveReading.Scene.COURSE_SELECTION&&(c==null||c.lastSeen()<sceneAt-2500))clear(Math.max(resetAt,sceneAt-2500));
        if(c!=null&&c.lastSeen()>=resetAt){if(!c.value().equals(course))level=0;course=c.value();courseAt=c.lastSeen();if(l!=null&&l.lastSeen()>=Math.max(resetAt,c.firstSeen())&&Math.abs(c.lastSeen()-l.lastSeen())<=2500)level=number(l.value(),1,4);}
        int subject=0;LiveStabilizer.Stable s=value(fields,"subject",profile,now);if(s!=null&&now-s.lastSeen()<=5000)subject=number(s.value(),1,1025);
        Integer film=null;LiveStabilizer.Stable f=value(fields,"remainingPhotos",profile,now);if(f!=null&&now-f.lastSeen()<=3000){int n=number(f.value(),0,72);if(n>=0)film=n;}
        String phase=switch(scene){case COURSE_SELECTION->"course";case COURSE_INTRO, EXPEDITION->"research";case PHOTO_SELECTION->"selection";case EVALUATION->"evaluation";case RESULTS->"results";case PHOTODEX->"photodex";case REQUEST_COMPLETE->"request";case UNLOCK->"discovery";case PAUSED->"paused";default->"playing";};
        return new View(phase,course,Math.max(0,level),subject,film);
    }
    private void clear(long at){course="";level=0;resetAt=at;courseAt=0;}
    void observedCourse(String next,long at){if(!course.isEmpty()&&!course.equals(next))clear(at);}
    private static int number(String value,int min,int max){try{int n=Integer.parseInt(value);return n>=min&&n<=max?n:-1;}catch(NumberFormatException e){return -1;}}
    private static LiveStabilizer.Stable value(Map<String,LiveStabilizer.Stable> values,String key,String profile,long now){var v=values.get(key);return v!=null&&v.profileId().equals(profile)&&v.fresh(now)?v:null;}
}
