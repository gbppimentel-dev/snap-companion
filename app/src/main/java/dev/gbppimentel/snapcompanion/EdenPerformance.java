package dev.gbppimentel.snapcompanion;

import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** AYN-only, fixed-purpose bridge. Never accepts shell text, paths or package names from input. */
final class EdenPerformance {
    private static int promotedPid;
    private static File scriptDirectory;
    static synchronized void initialize(File cacheDirectory) { scriptDirectory = cacheDirectory; }
    static volatile String status = "Waiting for the game";
    static boolean supported() { return isThorModel(Build.MODEL); }
    static boolean isThorModel(String model) { return model != null && "AYN Thor".equalsIgnoreCase(model.replace('_', ' ').trim()); }
    // Only the visible primary-display Eden activity qualifies. Other emulators and hidden games do not.
    private static final String PROMOTE = """
        snap_pid=$(pidof dev.igawa6.edenduo)
        case "$snap_pid" in ''|*[!0-9]*) echo no-process; exit 0;; esac
        snap_group=$(cat /proc/$snap_pid/cpuset)
        snap_cpu=$(awk -F: '$2 ~ /(^|,)cpu(,|$)/ {print $3;exit}' /proc/$snap_pid/cgroup)
        if [ "$snap_group" = /top-app ] && [ "$snap_cpu" = /top-app ]; then echo already-top:$snap_pid; exit 0; fi
        case "$snap_group:$snap_cpu" in /foreground:/foreground|/foreground:/top-app|/top-app:/foreground) ;; *) echo inactive; exit 0;; esac
        snap_top=$(dumpsys activity activities 2>/dev/null | awk '/^Display #/{primary=($2=="#0")} primary && /topResumedActivity=/ && !found {print;found=1}')
        case "$snap_top" in *' dev.igawa6.edenduo/'*) ;; *) echo not-visible; exit 0;; esac
        [ "$(pidof dev.igawa6.edenduo)" = "$snap_pid" ] || exit 0
        if echo "$snap_pid" > /dev/cpuset/top-app/cgroup.procs && echo "$snap_pid" > /dev/cpuctl/top-app/cgroup.procs; then
          echo promoted:$snap_pid
        else echo unavailable; fi
        """;
    static synchronized void refresh() {
        if (!supported()) { status = "AYN Thor support only"; return; }
        try {
            String result = command(PROMOTE).trim();
            android.util.Log.d("SnapPerformance", "Firmware result: " + result);
            if (result.startsWith("promoted:")) { promotedPid = Integer.parseInt(result.substring(9)); status = "Active for the visible game"; }
            else if (result.startsWith("already-top:")) status = "Game has full priority";
            else if (result.equals("not-visible") || result.equals("no-process") || result.equals("inactive")) status = "Waiting for Eden on the upper screen";
            else status = "Firmware support unavailable";
        } catch (Exception e) { status = "Firmware support unavailable"; android.util.Log.w("SnapPerformance", "Priority check failed", e); }
    }
    static synchronized void release() {
        int pid = promotedPid; promotedPid = 0;
        if (!supported() || pid <= 0) return;
        try {
            // If Android has legitimately made Eden the focused top app, leave it alone.
            command("snap_focus=$(dumpsys input 2>/dev/null | awk '/FocusedDisplayId/ && !found {print $2;found=1}')\n" +
                "[ \"$snap_focus\" = 0 ] && exit 0\n" +
                "[ \"$(pidof dev.igawa6.edenduo)\" = " + pid + " ] || exit 0\n" +
                "[ \"$(cat /proc/" + pid + "/cpuset)\" = /top-app ] || exit 0\n" +
                "echo " + pid + " > /dev/cpuset/foreground/cgroup.procs\n" +
                "echo " + pid + " > /dev/cpuctl/foreground/cgroup.procs\necho released");
            status = "Waiting for the game";
        } catch (Exception ignored) { status = "Priority release unavailable"; }
    }
    private static String command(String fixedCommand) throws Exception {
        if (scriptDirectory == null) throw new IOException("Bridge not initialized");
        // AYN's service drops long command arguments. Execute our fixed script through
        // a short path, as the firmware's own script runner and Wayfinder do.
        File script = File.createTempFile("eden-focus-", ".sh", scriptDirectory);
        try (FileOutputStream stream = new FileOutputStream(script)) { stream.write(fixedCommand.getBytes(StandardCharsets.UTF_8)); }
        IBinder binder = (IBinder)Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "PServerBinder");
        if (binder == null) { script.delete(); throw new IllegalStateException("No AYN firmware service"); }
        Parcel request = Parcel.obtain(), reply = Parcel.obtain();
        try {
            request.writeStringArray(new String[]{"sh '" + script.getAbsolutePath().replace("'", "'\\''") + "'", "0"});
            if (!binder.transact(0, request, reply, 0)) throw new IllegalStateException("Firmware command rejected");
            byte[] bytes = reply.createByteArray();
            return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        } finally { request.recycle(); reply.recycle(); script.delete(); }
    }
}
