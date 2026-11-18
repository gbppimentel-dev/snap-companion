package dev.gbppimentel.snapcompanion;

import java.util.concurrent.Executor;

/** One bounded check at a time, only for the visible companion's lifecycle. */
final class PriorityMonitor {
    interface Scheduler {
        void post(Runnable task, long delayMillis);
        void cancel(Runnable task);
    }
    private final Scheduler scheduler;
    private final Executor worker;
    private final Runnable refresh, release;
    private boolean running, inFlight;
    private long generation;
    private final Runnable tick = this::tick;

    PriorityMonitor(Scheduler scheduler, Executor worker, Runnable refresh, Runnable release) {
        this.scheduler = scheduler; this.worker = worker; this.refresh = refresh; this.release = release;
    }
    synchronized void start() {
        if (running) return;
        running = true; generation++;
        scheduler.cancel(tick); scheduler.post(tick, 300);
    }
    synchronized void stop() {
        if (!running) return;
        running = false;
        long stopped = ++generation;
        scheduler.cancel(tick);
        worker.execute(() -> { if (isStopped(stopped)) release.run(); });
    }
    private synchronized boolean isStopped(long version) { return !running && generation == version; }
    private synchronized boolean isCurrent(long version) { return running && generation == version; }
    private synchronized void tick() {
        if (!running) return;
        if (inFlight) { scheduler.post(tick, 200); return; }
        inFlight = true;
        long version = generation;
        worker.execute(() -> {
            try { if (isCurrent(version)) refresh.run(); }
            finally { scheduler.post(() -> completed(version), 0); }
        });
    }
    private synchronized void completed(long version) {
        inFlight = false;
        if (running && generation == version) {
            scheduler.cancel(tick); scheduler.post(tick, 2000);
        }
    }
}
