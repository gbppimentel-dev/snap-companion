package dev.gbppimentel.snapcompanion.next;

import java.util.*;
import org.json.*;

/** Confirmed collection data and selected route context. Capture can follow course/level, never confirm collection. */
public final class Progress {
    public record Photo(int score, String medal) {}
    public final Map<String, Photo> photos = new TreeMap<>();
    public final Set<String> completedRequests = new TreeSet<>();
    public final Set<String> objectives = new TreeSet<>();
    public String course = "park-day";
    public int level = 1;
    public int step = 0;
    public boolean spoilers = false;
    public String notes = "";
    public long updatedAt = 0;
    public Notebook notebook = new Notebook();
    public Notebook.HintMode hintMode(){return notebook.hintsConfigured?notebook.hints:spoilers?Notebook.HintMode.SOLUTION:Notebook.HintMode.DISCOVERY;}
    public Progress copy(){try{return copyUnchecked();}catch(JSONException e){throw new IllegalStateException(e);}}
    private Progress copyUnchecked()throws JSONException{Progress p=new Progress();p.photos.putAll(photos);p.completedRequests.addAll(completedRequests);p.objectives.addAll(objectives);p.course=course;p.level=level;p.step=step;p.spoilers=spoilers;p.notes=notes;p.updatedAt=updatedAt;p.notebook=Notebook.read(notebook.json(),null);return p;}
    public static final Set<String> MEDALS = Set.of("Unspecified", "Bronze", "Silver", "Gold", "Diamond");

    public static String key(int species, int stars) {
        if (species < 1 || stars < 1 || stars > 4) throw new IllegalArgumentException("Invalid photo category");
        return species + ":" + stars;
    }
    public boolean has(int species, int stars) { return photos.containsKey(key(species, stars)); }
    public void setPhoto(int species, int stars, boolean complete) {
        String key = key(species, stars);
        if (complete) photos.putIfAbsent(key, new Photo(0, "Unspecified")); else photos.remove(key);
    }
    public void score(int species, int stars, int score, String medal) {
        if (score < 0 || score > 99999 || !MEDALS.contains(medal)) throw new IllegalArgumentException("Invalid score or medal");
        photos.put(key(species, stars), new Photo(score, medal));
    }
    public int missing(int species) {
        int count = 0;
        for (int star = 1; star <= 4; star++) if (!has(species, star)) count++;
        return count;
    }
    public boolean relevant(int species) {
        if (objectives.isEmpty()) return missing(species) > 0;
        for (int stars = 1; stars <= 4; stars++) if (objectives.contains(key(species, stars)) && !has(species, stars)) return true;
        return false;
    }
    public String toJson() throws JSONException {
        JSONObject root = new JSONObject().put("format", "snap-companion-progress").put("version", 1).put("updatedAt", updatedAt);
        root.put("notebook",notebook.json());
        root.put("course", course).put("level", level).put("step", step).put("spoilers", spoilers).put("notes", notes);
        JSONObject ps = new JSONObject();
        for (Map.Entry<String, Photo> p : photos.entrySet()) ps.put(p.getKey(), new JSONObject().put("score", p.getValue().score()).put("medal", p.getValue().medal()));
        return root.put("photos", ps).put("requests", new JSONArray(completedRequests)).put("objectives", new JSONArray(objectives)).toString(2);
    }
    public static Progress fromJson(String json, Catalog catalog) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"snap-companion-progress".equals(root.getString("format")) || exactInt(root,"version") != 1) throw new JSONException("This is not a supported companion progress backup. Use the save inspector for game archives.");
        Progress p = new Progress();
        p.course = root.getString("course");
        p.level = exactInt(root, "level");
        p.step = exactInt(root, "step");
        if (!catalog.hasCourse(p.course) || p.level < 1 || p.level > 3 || p.step < 0 || p.step >= catalog.course(p.course).steps().size()) throw new JSONException("Invalid course, research level or route step");
        Object spoilerValue = root.get("spoilers");
        if (!(spoilerValue instanceof Boolean)) throw new JSONException("Invalid spoiler preference");
        p.spoilers = (Boolean) spoilerValue;
        p.notes = root.optString("notes", "");
        if (p.notes.length() > 10000) throw new JSONException("Notes exceed 10,000 characters");
        p.updatedAt = root.optLong("updatedAt", 0);
        JSONObject photos = root.getJSONObject("photos");
        Iterator<String> keys = photos.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            validatePhotoKey(key, catalog);
            JSONObject photo = photos.getJSONObject(key);
            int score = exactInt(photo, "score");
            String medal = photo.getString("medal");
            if (score < 0 || score > 99999 || !MEDALS.contains(medal)) throw new JSONException("Invalid photo result");
            p.photos.put(key, new Photo(score, medal));
        }
        JSONArray requests = root.getJSONArray("requests");
        for (int i = 0; i < requests.length(); i++) {
            String id = requests.getString(i);
            if (!catalog.hasRequest(id)) throw new JSONException("Unknown request: " + id);
            p.completedRequests.add(id);
        }
        JSONArray goals = root.getJSONArray("objectives");
        for (int i = 0; i < goals.length(); i++) { String key = goals.getString(i); validatePhotoKey(key, catalog); p.objectives.add(key); }
        p.notebook=Notebook.read(root.optJSONObject("notebook"),catalog);
        return p;
    }
    private static int exactInt(JSONObject root, String key) throws JSONException {
        Object value = root.get(key);
        if (!(value instanceof Number) || ((Number)value).doubleValue() != ((Number)value).intValue()) throw new JSONException("Expected an integer for " + key);
        return ((Number)value).intValue();
    }
    private static void validatePhotoKey(String key, Catalog catalog) throws JSONException {
        if (!key.matches("[1-9][0-9]*:[1-4]")) throw new JSONException("Invalid photo category: " + key);
        try { if (catalog.species(Integer.parseInt(key.split(":")[0])) == null) throw new JSONException("Unknown species"); }
        catch (NumberFormatException e) { throw new JSONException("Invalid species number"); }
    }
}
