package dev.gbppimentel.snapcompanion;

/** Never inspect/resume an emulator while its game is still booting. */
final class ResumeGate {
    static boolean ready(boolean visible, boolean nativeGameplay, LiveReading.Scene scene,
                         long capturedAt, long now) {
        if (!visible) return false;
        if (nativeGameplay) return true;
        return scene != null && scene != LiveReading.Scene.UNKNOWN && capturedAt > 0
                && now >= capturedAt && now - capturedAt <= 2500;
    }
    private ResumeGate() {}
}
