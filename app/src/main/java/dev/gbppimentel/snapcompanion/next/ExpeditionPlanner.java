package dev.gbppimentel.snapcompanion.next;

import java.util.*;

/** Conservative goal planner. Reference route order is an estimate, not game telemetry. */
public final class ExpeditionPlanner {
    public record Target(String id,int species,int stars,String request,int step,String direction,String discovery,String action,String solution,List<String> tools,List<String> cautions,String source){
        public String hint(Notebook.HintMode mode){return switch(mode){case DISCOVERY->discovery;case ACTION->action;case SOLUTION->solution;};}
    }
    public record Plan(String course,int level,List<Target> targets,List<Target> passed,int estimatedShots,int verifiedLocations,int estimatedLocations,List<String> warnings){}
    public record Ranked(Plan plan,int value){}
    public static Plan build(Catalog catalog,GuideBook guide,Progress progress,String courseId,int level,int step,Integer film){
        Catalog.Course course=catalog.course(courseId);List<Target> targets=new ArrayList<>(),passed=new ArrayList<>();Set<String> seen=new HashSet<>();int verified=0,estimated=0;
        for(GuideBook.Encounter e:guide.encounters){
            if(!e.course().equals(courseId)||e.minimumLevel()>level)continue;
            for(int stars=1;stars<=4;stars++){
                String key=Progress.key(e.species(),stars);Progress.Photo old=progress.photos.get(key);
                boolean needed=switch(progress.notebook.mode){case DIAMOND->old==null||!old.medal().equals("Diamond");case PERSONAL_BEST->true;case REQUESTS->false;default->old==null;};
                if(!needed||(!progress.objectives.isEmpty()&&!progress.objectives.contains(key))||progress.notebook.skipped.contains(key)||seen.contains(key))continue;
                GuideBook.Cue cue=guide.cue(e.species(),stars);if(cue==null||(!cue.course().isEmpty()&&!cue.course().equals(courseId))||cue.minimumLevel()>level)continue;
                seen.add(key);boolean specific=cue.course().equals(courseId);if(specific)verified++;else estimated++;
                List<String> cautions=new ArrayList<>();if(!specific)cautions.add("Pose source does not name this course; confirm its location before spending film.");if(cue.minimumLevel()==0)cautions.add("Pose research-level requirement is not specified.");if(e.estimated())cautions.add("Route segment estimated; use the source location and adjust the landmark.");
                List<String> tools=cue.tools();String name=catalog.species(e.species()).name();int at=GuideBook.routeStep(course,e.segment());
                String action=name+" · "+stars+"★ · "+(tools.isEmpty()?"watch its behavior":String.join(" → ",tools))+". Reveal the solution for the exact setup.";
                Target target=new Target(key,e.species(),stars,"",at,e.direction(),"Look for "+name+" around "+course.steps().get(at)+".",action,cue.procedure(),tools,List.copyOf(cautions),cue.source());(at<step?passed:targets).add(target);
            }
        }
        if(progress.notebook.mode!=Notebook.PlanMode.DIAMOND&&progress.notebook.mode!=Notebook.PlanMode.PERSONAL_BEST){
            for(Catalog.Request r:catalog.requests){if(!r.course().equals(courseId)||r.level()>level||progress.completedRequests.contains(r.id())||progress.notebook.skipped.contains(r.id()))continue;GuideBook.RequestDetail detail=guide.requests.get(r.id());List<String> tools=detail==null?List.of():detail.tools();
                List<String> cautions=new ArrayList<>();if(r.level()==0)cautions.add("Request unlock/research requirement is unknown; verify availability in game.");if(detail!=null&&detail.postgame()&&!progress.notebook.story.contains("main-story-complete"))cautions.add("Postgame access has not been confirmed in this notebook.");
                Target t=new Target(r.id(),r.pokemon(),0,r.id(),r.step(),"","Request: "+r.title(),catalog.species(r.pokemon()).name()+" · "+(tools.isEmpty()?"observe the requested behavior":String.join(" → ",tools)),r.solution(),tools,List.copyOf(cautions),detail==null?"":detail.source());(r.step()<step?passed:targets).add(t);}
        }
        targets.sort(Comparator.comparingInt(Target::step).thenComparingInt(t->t.request().isEmpty()?1:0).thenComparing(Target::id));
        if(progress.notebook.mode==Notebook.PlanMode.SHORT_RUN&&targets.size()>5)targets=new ArrayList<>(targets.subList(0,5));
        List<String> warnings=new ArrayList<>();Set<String> missingTools=new TreeSet<>();for(Target t:targets)for(String tool:t.tools())if(!progress.notebook.tools.contains(tool))missingTools.add(tool);if(!missingTools.isEmpty())warnings.add("Tool access not confirmed in this notebook: "+String.join(", ",missingTools)+".");
        warnings.add("Branch compatibility and timing are not fully mapped. This is a candidate list; it does not promise every target fits one run.");if(!passed.isEmpty())warnings.add(passed.size()+" targets are behind the selected landmark; keep them for the next run.");
        int shots=targets.size();if(film!=null&&shots>film)warnings.add("Plan has "+shots+" candidate shots with "+film+" film remaining. Prioritize or skip targets.");
        return new Plan(courseId,level,List.copyOf(targets),List.copyOf(passed),shots,verified,estimated,List.copyOf(warnings));
    }
    public static List<Ranked> rank(Catalog catalog,GuideBook guide,Progress progress){
        List<Ranked> out=new ArrayList<>();for(Catalog.Course course:catalog.courses){if(progress.notebook.access.getOrDefault(course.id(),Notebook.Access.UNKNOWN)==Notebook.Access.LOCKED)continue;Notebook.CourseRecord record=progress.notebook.courses.get(course.id());int highest=record==null?1:record.highestLevel();if(course.id().equals(progress.course))highest=Math.max(highest,progress.level);
            for(int level=1;level<=highest;level++){Plan p=build(catalog,guide,progress,course.id(),level,0,null);int value=p.verifiedLocations()*3+p.estimatedLocations()+p.targets().stream().mapToInt(t->t.request().isEmpty()?0:2).sum();if(value>0)out.add(new Ranked(p,value));}}
        out.sort(Comparator.comparingInt(Ranked::value).reversed().thenComparing(r->r.plan().course()).thenComparingInt(r->r.plan().level()));return List.copyOf(out);
    }
}
