package dev.gbppimentel.snapbridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import java.lang.reflect.Method;

/** Root-side, fixed-package broker for the owner's explicitly authorized always-live Thor setup. */
public final class LiveProjection {
    private static final String PACKAGE = "dev.gbppimentel.snapcompanion.next";
    public static void main(String[] args) {
        try {
            if (Process.myUid() != 0 || !"AYN Thor".equalsIgnoreCase(Build.MODEL.replace('_', ' '))) {
                throw new SecurityException("Authorized Thor root bridge required");
            }
            android.os.Looper.prepareMainLooper();
            Class<?> thread = Class.forName("android.app.ActivityThread");
            Object system = thread.getMethod("systemMain").invoke(null);
            Context context = (Context) thread.getMethod("getSystemContext").invoke(system);
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(PACKAGE, 0);
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class).invoke(null, "media_projection");
            Class<?> managerType = Class.forName("android.media.projection.IMediaProjectionManager");
            Object manager = Class.forName("android.media.projection.IMediaProjectionManager$Stub")
                    .getMethod("asInterface", IBinder.class).invoke(null, binder);
            Method create = managerType.getMethod("createProjection", int.class, String.class, int.class, boolean.class);
            Object projection = create.invoke(manager, app.uid, PACKAGE, 0, true);
            IBinder token = (IBinder) Class.forName("android.os.IInterface").getMethod("asBinder").invoke(projection);
            Bundle extras = new Bundle();
            extras.putBinder("android.media.projection.extra.EXTRA_MEDIA_PROJECTION", token);
            Intent authorization = new Intent().putExtras(extras);
            Intent capture = new Intent().setClassName(PACKAGE, PACKAGE + ".CaptureService")
                    .putExtra("result", Activity.RESULT_OK).putExtra("data", authorization)
                    .putExtra("hz", 2).putExtra("automatic", true);
            Object activities = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
            Class<?> applicationThread = Class.forName("android.app.IApplicationThread");
            Class.forName("android.app.IActivityManager")
                    .getMethod("startService", applicationThread, Intent.class, String.class, boolean.class,
                            String.class, String.class, int.class)
                    .invoke(activities, null, capture, null, true, "com.android.shell", null, 0);
            System.out.println("snap-live-started");
            System.exit(0);
        } catch (Throwable error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            System.out.println("snap-live-unavailable:" + cause.getClass().getSimpleName() + ":" + cause.getMessage());
            System.exit(1);
        }
    }
}
