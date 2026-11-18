package dev.gbppimentel.snapcompanion;

import java.util.*;
import org.json.*;

/** An authored recorded event fixture, never live game telemetry. */
public final class DemoTimeline {
    public record Event(int at, int step, String subject, int photos) {}
    public final List<Event> events = new ArrayList<>();
    public final String label;
    private int index = 0;
    public DemoTimeline(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        label = root.getString("label");
        JSONArray es = root.getJSONArray("events");
        for (int i = 0; i < es.length(); i++) {
            JSONObject e = es.getJSONObject(i);
            events.add(new Event(e.getInt("at"), e.getInt("step"), e.getString("subject"), e.getInt("photos")));
        }
        if (events.isEmpty()) throw new JSONException("Empty demo");
    }
    public Event current() { return events.get(index); }
    public int index() { return index; }
    public boolean advance() { if (index + 1 >= events.size()) return false; index++; return true; }
    public void reset() { index = 0; }
}
