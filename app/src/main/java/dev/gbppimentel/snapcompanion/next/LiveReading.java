package dev.gbppimentel.snapcompanion.next;

import java.util.*;

/** Immutable OCR observations. Confidence is a recognition/heuristic score, not a measured probability. */
public record LiveReading(long capturedAt, String source, String profileId, Scene scene, Map<String, Value> fields) {
    public enum Scene { UNKNOWN, TITLE, MENU, COURSE_SELECTION, COURSE_INTRO, EXPEDITION, PHOTO_SELECTION, EVALUATION, RESULTS, PHOTODEX, REQUEST_COMPLETE, UNLOCK, PAUSED }
    public record Word(String text, double left, double top, double right, double bottom, float confidence,boolean filmProbe) {
        public Word(String text,double left,double top,double right,double bottom,float confidence){this(text,left,top,right,bottom,confidence,false);}
        public double x() { return (left + right) / 2; }
        public double y() { return (top + bottom) / 2; }
    }
    public record Value(String value, float confidence, String evidence) {}
    public LiveReading { fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields)); }
    public boolean recognizedGame() { return scene != Scene.UNKNOWN; }
}
