package dev.gbppimentel.snapcompanion.next;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Conservative English screen-text parser. It never infers a Pokémon from its appearance. */
public final class LiveParser {
    private final Catalog catalog;private final CourseText courses;
    public LiveParser(Catalog catalog) { this.catalog = catalog;this.courses=new CourseText(catalog); }
    public static String normalize(String text) { return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " "); }
    private static boolean contains(String text, String phrase) { return (" " + text + " ").contains(" " + normalize(phrase) + " "); }
    private static boolean any(String text, String... phrases) { for (String p : phrases) if (contains(text, p)) return true; return false; }
    public LiveReading parse(List<LiveReading.Word> words, boolean cameraHud, long at, String source, String profileId) {
        Map<String, LiveReading.Value> fields = new LinkedHashMap<>();
        String joined = words.stream().map(LiveReading.Word::text).reduce("", (a,b) -> a + " " + b);
        List<LiveReading.Word> identityWords=CourseText.identityWords(words);
        String text = CourseText.ordered(identityWords);
        CourseText.Match courseMatch=courses.find(identityWords,text);
        String detectedCourse=courseMatch==null?null:courseMatch.id();
        boolean emulatorPaused=any(text,"resume emulation","emulation paused");
        if (any(text, "snap field companion", "simulated recorded event", "snap companion screen input", "new pokemon snap companion", "your live research assistant", "live connection", "selected objectives")) return new LiveReading(at, source, profileId, LiveReading.Scene.UNKNOWN, fields);
        float quality = (float)words.stream().mapToDouble(w -> Math.max(0, Math.min(1, w.confidence()))).average().orElse(0);
        boolean requestList=!cameraHud&&lenTalkList(words);
        int scoreLabels = 0; for (String p : List.of("pose", "size", "direction", "placement", "background")) if (contains(text,p)) scoreLabels++;
        LiveReading.Scene scene;
        if (emulatorPaused || contains(text,"pause menu") && any(text,"continue research","quit research","how to play")) scene=LiveReading.Scene.PAUSED;
        else if (any(text,"request complete", "request completed")) scene=LiveReading.Scene.REQUEST_COMPLETE;
        else if (any(text,"new area unlocked", "new course unlocked", "research level increased", "new research title", "new tool unlocked")) scene=LiveReading.Scene.UNLOCK;
        else if (requestList) scene=LiveReading.Scene.MENU;
        else if (contains(text,"professor s evaluation") || scoreLabels >= 3 && any(text,"total", "total score", "evaluation")) scene=LiveReading.Scene.EVALUATION;
        else if (any(text,"registered to the photodex", "registered in your photodex")) scene=LiveReading.Scene.PHOTODEX;
        else if (any(text,"expedition results", "research results", "course score", "research points", "expedition points")) scene=LiveReading.Scene.RESULTS;
        else if (any(text,"choose photos", "choose photographs", "select photos", "photo selection", "choose a pokemon", "select a pokemon", "choose a photo", "select a photo", "photos to show", "show the selected photos to the professor", "have your photos chosen automatically", "remove checks from all photos", "you cannot select a photo with no subject", "you cannot select a photo in which the subject is not clearly visible", "remove check marks from selected photos", "you cannot select photos in which the illumina phenomenon isn t shown")) scene=LiveReading.Scene.PHOTO_SELECTION;
        else if (any(text,"photodex score", "registered photos")) scene=LiveReading.Scene.PHOTODEX;
        else if(cameraHud) scene=LiveReading.Scene.EXPEDITION;
        else if (any(text,"select a course to research", "select research level and start", "change research level") || any(text,"research level","research lv") && (detectedCourse!=null||courses.hasName(text))) scene=LiveReading.Scene.COURSE_SELECTION;
        else if (cameraHud || any(text,"photos remaining", "remaining photos")) scene=LiveReading.Scene.EXPEDITION;
        else if(courses.isolatedTitle(text,courseMatch))scene=LiveReading.Scene.COURSE_INTRO;
        else if (any(text,"new pokemon snap") && any(text,"press any button", "start")) scene=LiveReading.Scene.TITLE;
        else if(any(text,"lental map")&&any(text,"research","level","florio","belusylva","maricopia","voluca","durice","aurus")) scene=LiveReading.Scene.MENU;
        else if (any(text,"research camp") && any(text,"research", "photodex", "lab", "internet")) scene=LiveReading.Scene.MENU;
        else scene=LiveReading.Scene.UNKNOWN;
        if(scene==LiveReading.Scene.UNKNOWN||emulatorPaused)return new LiveReading(at,source,profileId,scene,fields);
        boolean courseScene=scene==LiveReading.Scene.COURSE_SELECTION||scene==LiveReading.Scene.COURSE_INTRO||scene==LiveReading.Scene.EXPEDITION||scene==LiveReading.Scene.PAUSED;
        if(detectedCourse!=null&&courseScene){
            put(fields,"course",detectedCourse,courseMatch.confidence(),"Exact course identity and explicit time/Illumina variant; weakest identity-token confidence");
            Set<String> levels=new HashSet<>();float q=0;
            Matcher m=Pattern.compile("research (?:level|lv) (?:lv )?([123])\\b").matcher(text);
            while(m.find()){String before=text.substring(0,m.start());if(before.endsWith("next ")||scene==LiveReading.Scene.COURSE_SELECTION&&!before.endsWith("selected ")&&!before.endsWith("current "))continue;levels.add(m.group(1));q=Math.max(q,CourseText.quality(words,(scene==LiveReading.Scene.COURSE_SELECTION?(before.endsWith("selected ")?"selected ":"current "):"")+m.group()));}
            Set<String> visibleLevels=new HashSet<>();Matcher options=Pattern.compile("\\blv ([123])\\b").matcher(text);while(options.find())visibleLevels.add(options.group(1));
            if(levels.size()==1&&visibleLevels.size()<=1){String value=levels.iterator().next();put(fields,"researchLevel",value,q,"Single explicitly labelled research level, excluding Next Research Level and open level choices");}
            Matcher max=Pattern.compile("research (?:level|lv) (?:max lv|lv max|max)\\b").matcher(text);
            if(levels.isEmpty()&&visibleLevels.isEmpty()&&max.find()){String before=text.substring(0,max.start());if(!before.endsWith("next ")&&(scene!=LiveReading.Scene.COURSE_SELECTION||before.endsWith("selected ")||before.endsWith("current ")))put(fields,"researchLevel","4",CourseText.quality(words,(scene==LiveReading.Scene.COURSE_SELECTION?(before.endsWith("selected ")?"selected ":"current "):"")+max.group()),"Explicit MAX research-level label;4 is a display sentinel, never an inferred numeric level");}
        }
        if(scene==LiveReading.Scene.PAUSED||requestList)return new LiveReading(at,source,profileId,scene,fields);
        Set<Integer> subjects=new LinkedHashSet<>();
        for(Catalog.Species s:catalog.species) if(contains(text,s.name())) subjects.add(s.id());
        if(subjects.size()==1) put(fields,"subject",subjects.iterator().next().toString(),CourseText.quality(words,catalog.species(subjects.iterator().next()).name()),"One species name visible; no visual species recognition");
        if(scene==LiveReading.Scene.EXPEDITION) {
            Set<String> counts=new HashSet<>();
            float filmQuality=quality;
            Matcher m=Pattern.compile("(?:photos remaining|remaining photos) (\\d{1,2})\\b").matcher(text); while(m.find()) if(Integer.parseInt(m.group(1))<=72)counts.add(m.group(1));
            if(cameraHud) for(LiveReading.Word w:words) if(w.filmProbe() && w.text().matches("[0-9]{1,2}") && Integer.parseInt(w.text())<=72 && w.confidence()>=.70f) {counts.add(w.text());filmQuality=w.confidence();}
            if(counts.size()==1) put(fields,"remainingPhotos",Integer.toString(Integer.parseInt(counts.iterator().next())),filmQuality,cameraHud?"Film HUD · high-contrast model score; low-contrast reading agrees":"Explicit remaining-photos label");
        }
        if(scene==LiveReading.Scene.PHOTODEX || scene==LiveReading.Scene.PHOTO_SELECTION) {
            Set<String> categories=new HashSet<>(); Matcher m=Pattern.compile("\\b([1-4])(?:[ -]?star| ★)",Pattern.CASE_INSENSITIVE).matcher(joined); while(m.find())categories.add(m.group(1));
            for(LiveReading.Word w:words) if(w.text().matches("[★*]{1,4}")) categories.add(Integer.toString(w.text().length()));
            if(categories.size()==1){String category=categories.iterator().next();float q=0;for(var w:words)if(w.text().matches("[★*]{1,4}")&&w.text().length()==Integer.parseInt(category))q=Math.max(q,w.confidence());for(String label:List.of(category+" star",category+"star",category+" stars",category+"stars"))q=Math.max(q,CourseText.quality(words,label));put(fields,"category",category,q,"Explicit star category; confidence of category tokens only");}
            for(String label:List.of("pose","size","direction","placement","background","total")) {
                String value=singleScore(words,label); if(value!=null)put(fields,label.equals("total")?"totalScore":label+"Score",value,quality,"Unambiguous score beside "+label);
            }
            if(scene==LiveReading.Scene.PHOTODEX && any(text,"registered to the photodex","registered in your photodex")) put(fields,"registration","confirmed-screen",quality,"Explicit registration banner visible");
        }
        if(scene==LiveReading.Scene.PHOTO_SELECTION){
            for(String label:List.of("photos taken","photos selected")){Set<String> values=new HashSet<>();float q=0;Matcher m=Pattern.compile(Pattern.quote(label)+" ([0-9]{1,2})\\b").matcher(text);while(m.find())if(Integer.parseInt(m.group(1))<=72){values.add(m.group(1));q=Math.max(q,CourseText.quality(words,m.group()));}if(values.size()==1)put(fields,label.equals("photos taken")?"photosTaken":"photosSelected",values.iterator().next(),q,"Explicit photo-selection count; confidence of label/value tokens only");}
        }
        if(scene==LiveReading.Scene.RESULTS) {
            String xp=singleScore(words,"points");if(xp!=null)put(fields,"researchPoints",xp,quality,"Visible research points; not assumed to be lifetime XP");
        }
        if(scene==LiveReading.Scene.REQUEST_COMPLETE) {
            List<Catalog.Request> matches=catalog.requests.stream().filter(r -> contains(text,r.title())).collect(java.util.stream.Collectors.toList());
            if(matches.size()==1)put(fields,"completedRequest",matches.get(0).id(),quality,"Explicit request-completion text and one known title");
        }
        if(scene==LiveReading.Scene.UNLOCK) {
            for(String tool:List.of("fluffruit","scan","melody","turbo","illumina orb","shrink")) if(contains(text,tool))put(fields,"unlockNotice",tool,quality,"Visible unlock notice; prerequisites not inferred");
        }
        return new LiveReading(at,source,profileId,scene,fields);
    }
    /** The real request list is a quiet menu, not a completion banner or active course.
     * Independent spatial anchors avoid interpreting titles/art on postcards as gameplay fields. */
    private static boolean lenTalkList(List<LiveReading.Word> words){
        List<LiveReading.Word> title=words.stream().filter(w->w.top()<.10&&w.left()<.40).collect(java.util.stream.Collectors.toList());
        List<LiveReading.Word> counts=words.stream().filter(w->w.top()>=.07&&w.bottom()<.22&&w.left()>=.60).collect(java.util.stream.Collectors.toList());
        return Math.max(CourseText.quality(title,"LenTalk"),CourseText.quality(title,"Len Talk"))>=.8f
            &&CourseText.quality(counts,"Completed Requests")>=.8f&&CourseText.quality(counts,"All Requests")>=.65f;
    }
    private static String singleScore(List<LiveReading.Word> words,String label) {
        Set<String> found=new HashSet<>();
        for(LiveReading.Word w:words) {
            if(!normalize(w.text()).equals(label))continue;
            for(LiveReading.Word n:words) if(n.left()>=w.right()-.01 && Math.abs(n.y()-w.y())<.04 && n.text().matches("[0-9][0-9, .]{0,8}")) {
                String value=n.text().replaceAll("[^0-9]","");
                try {int number=Integer.parseInt(value);if(number>=0&&number<=999999)found.add(Integer.toString(number));}catch(NumberFormatException ignored){}
            }
        }
        return found.size()==1?found.iterator().next():null;
    }
    private static void put(Map<String,LiveReading.Value> fields,String key,String value,float confidence,String evidence) { fields.put(key,new LiveReading.Value(value,Math.min(.99f,confidence),evidence)); }
    private static float phraseQuality(List<LiveReading.Word> words,String phrase) {
        List<String> tokens=new ArrayList<>();List<Float> scores=new ArrayList<>();
        for(LiveReading.Word word:words)for(String token:normalize(word.text()).split(" "))if(!token.isEmpty()){tokens.add(token);scores.add(word.confidence());}
        String[] target=normalize(phrase).split(" ");float best=0;
        for(int i=0;i+target.length<=tokens.size();i++){float low=1;boolean match=true;for(int j=0;j<target.length;j++){if(!target[j].equals(tokens.get(i+j))){match=false;break;}low=Math.min(low,scores.get(i+j));}if(match)best=Math.max(best,low);}
        return best;
    }
}
