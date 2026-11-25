package dev.gbppimentel.snapbridge;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.os.*;
import android.util.SparseArray;
import android.view.accessibility.*;
import java.util.List;

/** Startup-only repair: click exactly Eden's native paused overlay on the upper display. */
public final class ResumeVisibleEden {
    public static void main(String[] args){HandlerThread thread=null;UiAutomation automation=null;
        try{
            if(android.os.Process.myUid()!=0||!"AYN Thor".equalsIgnoreCase(Build.MODEL.replace('_',' '))||args.length!=0)throw new SecurityException();
            thread=new HandlerThread("snap-resume-window");thread.start();Class<?> connectionType=Class.forName("android.app.IUiAutomationConnection");Object connection=Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();
            automation=(UiAutomation)UiAutomation.class.getConstructor(Looper.class,connectionType).newInstance(thread.getLooper(),connection);UiAutomation.class.getMethod("connect",int.class).invoke(automation,UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info=automation.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;automation.setServiceInfo(info);
            SparseArray<List<AccessibilityWindowInfo>> displays=automation.getWindowsOnAllDisplays();List<AccessibilityWindowInfo> windows=displays.get(0);boolean resumed=false;
            if(windows!=null)for(AccessibilityWindowInfo window:windows){AccessibilityNodeInfo root=window.getRoot();if(root==null||!"dev.igawa6.edenduo".contentEquals(root.getPackageName()))continue;
                for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByViewId("dev.igawa6.edenduo:id/paused_icon"))if(node.isVisibleToUser()&&node.isClickable())resumed|=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            System.out.println(resumed?"eden-resumed-native-overlay":"eden-no-paused-overlay");
        }catch(Throwable e){Throwable cause=e.getCause()==null?e:e.getCause();System.out.println("eden-resume-unavailable:"+cause.getClass().getSimpleName());}
        finally{if(automation!=null)try{UiAutomation.class.getMethod("disconnect").invoke(automation);}catch(Exception ignored){}if(thread!=null)thread.quitSafely();System.exit(0);}
    }
}
