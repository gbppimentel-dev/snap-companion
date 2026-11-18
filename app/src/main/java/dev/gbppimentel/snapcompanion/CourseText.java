package dev.gbppimentel.snapcompanion;

import java.util.*;

/** Exact English course identities, including the game's short CourseNNN names.
 * A time badge is required for time variants; unlocks and reference levels are never inputs. */
final class CourseText {
    record Match(String id,float confidence){}
    private final Catalog catalog;
    private static final Map<String,String> SHORT=Map.ofEntries(
        Map.entry("park","park"),Map.entry("path","side path"),Map.entry("jungle","jungle"),
        Map.entry("river","river"),Map.entry("beach","beach"),Map.entry("reef","reef"),
        Map.entry("sands","sands"),Map.entry("badlands","badlands"),Map.entry("snow","snowfields"));
    private static final Map<String,String> ILLUMINA_BASE=Map.of(
        "park-illumina","florio nature park","forest-illumina","elsewhere forest",
        "sea-illumina","lental seafloor","volcano-illumina","fireflow volcano",
        "cave-illumina","outaway cave","ruins-illumina","ruins of remembrance");
    CourseText(Catalog catalog){this.catalog=catalog;}
    private static boolean has(String text,String phrase){return(" "+text+" ").contains(" "+phrase+" ");}
    static List<LiveReading.Word> identityWords(List<LiveReading.Word> input){
        boolean overlay=input.stream().anyMatch(w->w.top()<.09&&w.left()<.32&&LiveParser.normalize(w.text()).matches(".*\\b(?:fps|ft)\\b.*"));
        return overlay?input.stream().filter(w->!(w.top()<.09&&w.left()<.32)).collect(java.util.stream.Collectors.toList()):input;
    }
    static String ordered(List<LiveReading.Word> input){StringJoiner out=new StringJoiner(" ");for(var w:orderedWords(input))out.add(w.text());return LiveParser.normalize(out.toString());}
    private static List<LiveReading.Word> orderedWords(List<LiveReading.Word> input){
        // ML Kit supplies block order, which need not be the screen's reading order.
        // Keep overlapping test/one-block words stable; reorder distinct visual lines only.
        List<List<LiveReading.Word>> lines=new ArrayList<>();
        for(var w:input){List<LiveReading.Word> found=null;for(var line:lines){var a=line.get(0);double overlap=Math.min(a.bottom(),w.bottom())-Math.max(a.top(),w.top());if(overlap>0&&overlap>=Math.min(a.bottom()-a.top(),w.bottom()-w.top())*.5){found=line;break;}}if(found==null){found=new ArrayList<>();lines.add(found);}found.add(w);}
        lines.sort(Comparator.comparingDouble(a->a.get(0).top()));
        List<LiveReading.Word> out=new ArrayList<>();for(var line:lines){line.sort(Comparator.comparingDouble(LiveReading.Word::left));out.addAll(line);}return out;
    }
    Match find(List<LiveReading.Word> words,String text){
        Set<String> times=new HashSet<>();for(String t:List.of("day","night","evening"))if(has(text,t))times.add(t);
        boolean illumina=has(text,"illumina spot");Map<String,Float> matches=new LinkedHashMap<>();
        for(var c:catalog.courses){
            String name=LiveParser.normalize(c.name());boolean special=c.id().endsWith("-illumina");
            if(special!=illumina)continue;
            String mode=c.id().endsWith("-night")?"night":c.id().endsWith("-day")?"day":c.id().endsWith("-evening")?"evening":"";
            if(!mode.isEmpty()&&(times.size()!=1||!times.contains(mode)))continue;
            List<String> aliases=new ArrayList<>(List.of(name));
            String base=mode.isEmpty()?name:name.substring(0,name.length()-mode.length()).trim();
            if(!mode.isEmpty()){
                String family=c.id().substring(0,c.id().lastIndexOf('-'));String shortName=SHORT.get(family);
                if(shortName!=null)aliases.add(shortName+" "+mode);
                if(has(text,base)&&has(text,mode))matches.put(c.id(),Math.min(quality(words,base),quality(words,mode)));
            }else if(special){String parent=ILLUMINA_BASE.get(c.id());if(parent!=null&&has(text,parent))matches.put(c.id(),Math.min(quality(words,parent),quality(words,"illumina spot")));}
            else switch(c.id()){case "undersea"->aliases.add("undersea");case "forest"->aliases.add("forest");case "volcano"->aliases.add("volcano");case "cave"->aliases.add("cave");case "ruins"->aliases.add("ruins");default->{}}
            for(String alias:aliases)if(has(text,alias))matches.merge(c.id(),quality(words,alias),Math::max);
        }
        if(matches.size()!=1)return null;var m=matches.entrySet().iterator().next();return new Match(m.getKey(),m.getValue());
    }
    boolean hasName(String text){for(var c:catalog.courses){String name=LiveParser.normalize(c.name()).replaceAll(" (day|night|evening)$","");if(has(text,name))return true;}for(String s:SHORT.values())if(has(text,s))return true;return false;}
    boolean isolatedTitle(String text,Match match){
        if((match!=null&&match.id().equals("camp"))||text.split(" ").length>20)return false;
        if(match==null){if(!hasName(text)||has(text,"research camp"))return false;StringJoiner known=new StringJoiner(" ");for(var c:catalog.courses)known.add(LiveParser.normalize(c.name()));for(String name:SHORT.values())known.add(name);Set<String> allowed=new HashSet<>(Arrays.asList((known+" loading").split(" ")));for(String token:text.split(" "))if(!allowed.contains(token))return false;return true;}
        String allowed=LiveParser.normalize(catalog.course(match.id()).name())+" loading research level lv max";
        String family=match.id().replaceAll("-(day|night|evening)$","");allowed+=" "+SHORT.getOrDefault(family,"")+" "+ILLUMINA_BASE.getOrDefault(match.id(),"");
        Set<String> tokens=new HashSet<>(Arrays.asList(allowed.split(" ")));
        for(String token:text.split(" "))if(!tokens.contains(token)&&!token.matches("[123]"))return false;
        return true;
    }
    static float quality(List<LiveReading.Word> words,String phrase){
        // Identity confidence belongs to its actual words, never the average of a whole screen.
        List<String> tokens=new ArrayList<>();List<Float> scores=new ArrayList<>();
        for(var word:orderedWords(words))for(String token:LiveParser.normalize(word.text()).split(" "))if(!token.isEmpty()){tokens.add(token);scores.add(word.confidence());}
        String[] target=LiveParser.normalize(phrase).split(" ");float best=0;
        for(int i=0;i+target.length<=tokens.size();i++){float low=1;boolean match=true;for(int j=0;j<target.length;j++){if(!target[j].equals(tokens.get(i+j))){match=false;break;}low=Math.min(low,scores.get(i+j));}if(match)best=Math.max(best,low);}
        return best;
    }
}
