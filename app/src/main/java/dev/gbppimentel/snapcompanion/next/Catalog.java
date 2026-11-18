package dev.gbppimentel.snapcompanion.next;

import org.json.*;
import java.util.*;

/** Factual reference data. Empty encounter lists mean no authored guide, not no Pokémon. */
public final class Catalog {
    public record Species(int id, int lental, String name, String source) {}
    public record Encounter(int id, int level, int step, String hint) {}
    public record Course(String id, String name, List<String> steps, List<Encounter> encounters, String coverage) {}
    public record Request(String id, String title, int pokemon, String course, int level, int step, String hint, String solution) {}
    public record Source(String title, String url) {}
    public final List<Species> species = new ArrayList<>();
    public final List<Course> courses = new ArrayList<>();
    public final List<Request> requests = new ArrayList<>();
    public final List<Source> sources = new ArrayList<>();
    private final Map<Integer, Species> byId = new HashMap<>();

    public Catalog(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.getInt("schema") != 1) throw new JSONException("Unsupported catalog version");
        JSONArray roster = root.getJSONArray("roster");
        Set<Integer> numbers = new HashSet<>();
        for (int i = 0; i < roster.length(); i++) {
            JSONObject row = roster.getJSONObject(i);
            Species p = new Species(row.getInt("id"), row.getInt("lental"), row.getString("name"), row.getString("source"));
            if (byId.put(p.id(), p) != null || !numbers.add(p.lental())) throw new JSONException("Duplicate species");
            species.add(p);
        }
        JSONArray cs = root.getJSONArray("courses");
        Set<String> courseIds = new HashSet<>();
        for (int i = 0; i < cs.length(); i++) {
            JSONObject c = cs.getJSONObject(i);
            String id = c.getString("id");
            if (!courseIds.add(id)) throw new JSONException("Duplicate course");
            List<String> steps = new ArrayList<>();
            JSONArray ss = c.getJSONArray("steps");
            for (int j = 0; j < ss.length(); j++) steps.add(ss.getString(j));
            if (steps.isEmpty()) throw new JSONException("Course needs a route step");
            List<Encounter> encounters = new ArrayList<>();
            JSONArray es = c.getJSONArray("encounters");
            for (int j = 0; j < es.length(); j++) {
                JSONObject e = es.getJSONObject(j);
                Encounter encounter = new Encounter(e.getInt("id"), e.getInt("level"), e.getInt("step"), e.getString("hint"));
                if (!byId.containsKey(encounter.id()) || encounter.step() < 0 || encounter.step() >= steps.size() || encounter.level() < 1 || encounter.level() > 3)
                    throw new JSONException("Invalid encounter");
                encounters.add(encounter);
            }
            courses.add(new Course(id, c.getString("name"), List.copyOf(steps), List.copyOf(encounters), c.optString("coverage", "Manual course only. Encounter and request coverage arrives in Batch 2.")));
        }
        JSONArray rs = root.getJSONArray("requests");
        Set<String> requestIds = new HashSet<>();
        for (int i = 0; i < rs.length(); i++) {
            JSONObject r = rs.getJSONObject(i);
            Request request = new Request(r.getString("id"), r.getString("title"), r.getInt("pokemon"), r.getString("course"), r.getInt("level"), r.getInt("step"), r.getString("hint"), r.getString("solution"));
            if (!byId.containsKey(request.pokemon()) || !courseIds.contains(request.course()) || !requestIds.add(request.id())) throw new JSONException("Invalid request");
            requests.add(request);
        }
        JSONArray src = root.getJSONArray("sources");
        for (int i = 0; i < src.length(); i++) sources.add(new Source(src.getJSONObject(i).getString("title"), src.getJSONObject(i).getString("url")));
    }
    public Species species(int id) { return byId.get(id); }
    public Course course(String id) { return courses.stream().filter(c -> c.id().equals(id)).findFirst().orElse(courses.get(0)); }
    public boolean hasCourse(String id) { return courses.stream().anyMatch(c -> c.id().equals(id)); }
    public boolean hasRequest(String id) { return requests.stream().anyMatch(r -> r.id().equals(id)); }
}
