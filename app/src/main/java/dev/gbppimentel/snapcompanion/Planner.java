package dev.gbppimentel.snapcompanion;

import java.util.*;

public final class Planner {
    public record Opportunity(Catalog.Encounter encounter, boolean later) {}
    public static Optional<Opportunity> next(Catalog.Course course, int level, int step, Progress progress) {
        return course.encounters().stream()
            .filter(e -> e.level() <= level && e.step() >= step && progress.relevant(e.id()))
            .min(Comparator.comparingInt(Catalog.Encounter::step))
            .map(e -> new Opportunity(e, e.step() > step));
    }
}
