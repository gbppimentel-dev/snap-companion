package dev.gbppimentel.snapcompanion.next;

import org.json.*;
import java.util.*;

/** Versioned offline reference. A zero requirement means unknown, never unlocked. */
public final class GuideBook {
    public record Cue(int species,int stars,String course,int minimumLevel,String procedure,List<String> tools,String source){}
    public record Encounter(String course,int species,String form,int minimumLevel,int segment,boolean estimated,String direction,String procedure,List<String> tools,String source){}
    public record RequestDetail(String id,List<Integer> subjects,List<String> tools,List<String> rewards,String source,int unlockLevel,boolean postgame,boolean rewardSpecified,String requirementSource){}
    public record Feature(String id,String course,String name,String procedure,String source){}
    public record Unlock(String id,String kind,String name,String requirement,String source,String effect,String requirementSource,String requiresRequest){}
    public record SpeciesInfo(List<String> types,List<String> aliases){}
    public record Variant(int id,int model,int species,String form,String gender){}
    public final List<Variant> variants=new ArrayList<>();
    public final Map<String,Integer> gameCourses=new HashMap<>();
    public final List<Cue> cues=new ArrayList<>();
    public final List<Encounter> encounters=new ArrayList<>();
    public final List<Feature> features=new ArrayList<>();
    public final List<Unlock> unlocks=new ArrayList<>();
    public final Map<Integer,SpeciesInfo> species=new HashMap<>();
    public final Map<String,RequestDetail> requests=new HashMap<>();
    public final Map<String,String> courseUnlocks=new HashMap<>();
    public final Map<String,Map<String,Integer>> thresholds=new HashMap<>();
    public final Map<String,List<Integer>> cumulativeThresholds=new HashMap<>();
    private final List<Catalog.Request> catalogRequests=new ArrayList<>();
    public final String revision,gameVersion;
    public GuideBook(String json,Catalog catalog)throws JSONException {
        JSONObject root=new JSONObject(json);
        if(root.getInt("schema")!=1)throw new JSONException("Unsupported reference schema");
        revision=root.getString("revision");gameVersion=root.getString("gameVersion");
        JSONArray a=root.getJSONArray("species");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);int id=r.getInt("id");require(catalog.species(id)!=null&&!species.containsKey(id),"Unknown or duplicate species metadata");List<String> aliases=new ArrayList<>();JSONObject names=r.getJSONObject("aliases");for(String language:JsonKeys.of(names))aliases.addAll(strings(names.getJSONArray(language)));species.put(id,new SpeciesInfo(strings(r.getJSONArray("types")),List.copyOf(aliases)));}
        a=root.getJSONArray("courses");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String id=r.getString("id");require(catalog.hasCourse(id)&&!thresholds.containsKey(id),"Unknown or duplicate reference course");JSONObject t=r.getJSONObject("thresholds");Map<String,Integer> values=new TreeMap<>();for(String k:JsonKeys.of(t)){int xp=t.getInt(k);require(xp>=0,"Invalid research threshold");values.put(k,xp);}thresholds.put(id,Map.copyOf(values));courseUnlocks.put(id,r.optString("unlock","Unspecified"));}
        a=root.getJSONArray("encounters");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String course=r.getString("course");int id=r.getInt("species"),level=r.getInt("minimumLevel"),segment=r.getInt("segment");require(catalog.hasCourse(course)&&catalog.species(id)!=null&&level>=1&&level<=3&&segment>=0&&segment<=2,"Invalid reference encounter");encounters.add(new Encounter(course,id,r.optString("form"),level,segment,!r.optString("segmentConfidence").equals("source-cue"),r.optString("direction"),bounded(r,"procedure"),strings(r.getJSONArray("tools")),url(r)));}
        a=root.getJSONArray("photoCues");Set<String> cueKeys=new HashSet<>();
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);int id=r.getInt("species"),stars=r.getInt("stars"),level=r.getInt("minimumLevel");String course=r.optString("course");require(catalog.species(id)!=null&&stars>=1&&stars<=4&&level>=0&&level<=3&&(course.isEmpty()||catalog.hasCourse(course)),"Invalid photo cue");require(cueKeys.add(Progress.key(id,stars)),"Duplicate photo cue");cues.add(new Cue(id,stars,course,level,bounded(r,"procedure"),strings(r.getJSONArray("tools")),url(r)));}
        a=root.getJSONArray("requests");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String id=r.getString("id"),course=r.getString("course");List<Integer> subjects=new ArrayList<>();JSONArray ss=r.getJSONArray("subjects");for(int j=0;j<ss.length();j++){int n=ss.getInt(j);require(catalog.species(n)!=null,"Unknown request subject");subjects.add(n);}int level=r.getInt("minimumLevel"),segment=r.getInt("segment");require(!subjects.isEmpty()&&catalog.hasCourse(course)&&level>=0&&level<=3&&segment>=0&&segment<=2&&!requests.containsKey(id),"Invalid request detail");String title=bounded(r,"title"),procedure=bounded(r,"procedure");requests.put(id,new RequestDetail(id,List.copyOf(subjects),strings(r.getJSONArray("tools")),strings(r.getJSONArray("reward")),url(r),r.optInt("unlockLevel",0),r.optBoolean("postgame",false),r.optBoolean("rewardSpecified",false),r.optString("requirementSource","")));int step=routeStep(catalog.course(course),segment);catalogRequests.add(new Catalog.Request(id,title,subjects.get(0),course,Math.max(level,r.optInt("unlockLevel",0)),step,"Look for "+catalog.species(subjects.get(0)).name()+" during this course. Reveal action hints when ready.",procedure));}
        a=root.getJSONArray("features");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String course=r.getString("course");require(catalog.hasCourse(course),"Invalid landmark course");String name=bounded(r,"name");features.add(new Feature(r.optString("id",course+":"+i),course,name,bounded(r,"procedure"),url(r)));}
        a=root.getJSONArray("unlocks");
        for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String kind=r.getString("kind"),name=bounded(r,"name");require(name.length()<=160&&!name.contains("|"),"Invalid unlock name");unlocks.add(new Unlock(kind+":"+name,kind,name,bounded(r,"requirement"),url(r),r.optString("effect",""),r.optString("requirementSource",""),r.optString("requiresRequest","")));}
        require(encounters.stream().map(Encounter::course).distinct().count()==30&&encounters.stream().map(Encounter::species).distinct().count()==234,"Incomplete encounter coverage");
        require(species.size()==234&&thresholds.size()==30&&cueKeys.size()==936&&requests.size()==234,"Incomplete reference dataset; active data preserved");
        a=root.optJSONArray("variants");Set<Integer> variantIds=new HashSet<>();
        if(a!=null)for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);int id=r.getInt("id"),model=r.getInt("modelId"),national=r.getInt("national");String form=r.getString("form"),gender=r.getString("gender");require(id>0&&id<=1000&&model>0&&model<=289&&catalog.species(national)!=null&&variantIds.add(id)&&form.length()<=100&&Set.of("Male","Female","GenderCommon","GenderUnknown").contains(gender),"Invalid game variant");variants.add(new Variant(id,model,national,form,gender));}
        a=root.getJSONArray("courses");Set<Integer> courseIds=new HashSet<>();for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(r.has("gameId")){int id=r.getInt("gameId");require(id>=1&&id<=30&&courseIds.add(id),"Invalid game course ID");gameCourses.put(r.getString("id"),id);}JSONArray values=r.optJSONArray("rawResearchThresholds");if(values!=null){require(values.length()==3,"Invalid cumulative threshold count");List<Integer> xp=new ArrayList<>();for(int j=0;j<3;j++){int n=values.getInt(j);require(n>=0&&n<=1000000,"Invalid cumulative threshold");xp.add(n);}require(xp.get(2)>0&&xp.get(2)>=Math.max(xp.get(0),xp.get(1)),"Invalid maximum threshold");cumulativeThresholds.put(r.getString("id"),List.copyOf(xp));}}
    }
    public void apply(Catalog catalog){
        for(int i=0;i<catalog.courses.size();i++){
            Catalog.Course c=catalog.courses.get(i);Map<Integer,Catalog.Encounter> merged=new LinkedHashMap<>();
            for(Encounter e:encounters)if(e.course().equals(c.id()))merged.putIfAbsent(e.species(),new Catalog.Encounter(e.species(),e.minimumLevel(),routeStep(c,e.segment()),"Reference location: "+e.procedure()+"\nRoute segment "+(e.estimated()?"estimated":"inferred from the location wording")+"; check the guide for branch and level details."));
            for(Catalog.Encounter e:c.encounters())merged.put(e.id(),e);
            catalog.courses.set(i,new Catalog.Course(c.id(),c.name(),c.steps(),List.copyOf(merged.values()),merged.size()+" species with reference locations. Availability depends on selected research level and branch; no exact live position is implied."));
        }
        catalog.requests.clear();catalog.requests.addAll(catalogRequests);
    }
    public static int routeStep(Catalog.Course c,int segment){return segment==2?c.steps().size()-1:segment==0?0:1;}
    public Cue cue(int species,int stars){return cues.stream().filter(c->c.species()==species&&c.stars()==stars).findFirst().orElse(null);}
    public List<Encounter> locations(int id){return encounters.stream().filter(e->e.species()==id).collect(java.util.stream.Collectors.toList());}
    public List<Feature> features(String course){return features.stream().filter(e->e.course().equals(course)).collect(java.util.stream.Collectors.toList());}
    public List<String> forms(int id){return variants.stream().filter(v->v.species()==id&&!v.form().isEmpty()).map(Variant::form).distinct().collect(java.util.stream.Collectors.toList());}
    public String thresholdLabel(String course){List<Integer> xp=cumulativeThresholds.get(course);if(xp==null)return "Reference XP needed to advance from each preceding level: "+thresholds.get(course);List<String> labels=new ArrayList<>();if(xp.get(0)>0)labels.add("Lv. 2: "+xp.get(0));if(xp.get(1)>0)labels.add("Lv. 3: "+xp.get(1));labels.add("MAX: "+xp.get(2));return "Cumulative research thresholds · "+String.join(" · ",labels);}
    public boolean matches(int id,String query){SpeciesInfo p=species.get(id);String q=nameKey(query);return p!=null&&!q.isEmpty()&&(p.types().stream().anyMatch(s->nameKey(s).contains(q))||p.aliases().stream().anyMatch(s->nameKey(s).contains(q)));}
    private static String nameKey(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
    private static List<String> strings(JSONArray a)throws JSONException{List<String> out=new ArrayList<>();for(int i=0;i<a.length();i++){String s=a.getString(i);require(s.length()<10000,"Reference text too long");out.add(s);}return List.copyOf(out);}
    private static String bounded(JSONObject r,String key)throws JSONException{String s=r.getString(key);require(!s.isBlank()&&s.length()<=12000,"Missing or oversized reference text: "+key);return s;}
    private static String url(JSONObject r)throws JSONException{String s=r.getString("source");require(s.matches("owned-game://2\\.0\\.1/(?:PhotoFrameData|PhotoFilterData|PhotoStampData|ProfileIconData)")||s.matches("https://(?:www\\.)?(?:serebii\\.net|powerpyx\\.com|bisafans\\.de|bulbapedia\\.bulbagarden\\.net)/[^\\s]+"),"Unrecognized reference source");return s;}
    private static void require(boolean ok,String message)throws JSONException{if(!ok)throw new JSONException(message);}
}
