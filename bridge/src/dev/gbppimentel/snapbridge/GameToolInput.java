// SPDX-License-Identifier: GPL-3.0-or-later
package dev.gbppimentel.snapbridge;
import android.os.HandlerThread;
import android.os.Looper;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.TimeUnit;
import android.view.InputDevice;
import dev.gbppimentel.snapcompanion.ControllerButtons;
import dev.gbppimentel.snapcompanion.GameToolRules;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
/** Fixed short tool presses targeted to display0's verified game window. Never writes guest memory. */
final class GameToolInput {
 private static final File REQUEST=new File("/data/user/0/dev.gbppimentel.snapcompanion/files/tool.request");
 private static String lastId="";private static JSONObject feedback;
 private static int device=-1;private static long lastRequest;
 static boolean available(){try{
  if(device<0||InputDevice.getDevice(device)==null){int selected=-1,best=0,count=0;for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d==null)continue;int score=ControllerButtons.builtInScore(d.getName());if(score>best){selected=id;best=score;count=1;}else if(score>0&&score==best)count++;}device=count==1?selected:-1;}
  return device>=0;
 }catch(Throwable e){device=-1;return false;}}
 private static int gameTask(){Process process=null;try{
  process=new ProcessBuilder("dumpsys","-t","1","activity","activities").redirectErrorStream(true).start();boolean primary=false;int total=0;
  try(BufferedReader input=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=input.readLine())!=null){total+=line.length();if(total>1000000)return -1;String t=line.trim();if(t.startsWith("Display #"))primary=t.startsWith("Display #0 ")||t.equals("Display #0")||t.startsWith("Display #0 (");if(primary&&line.contains("topResumedActivity=")){if(!line.contains("dev.igawa6.edenduo/org.yuzu.yuzu_emu.activities.EmulationActivity"))return -1;Matcher m=Pattern.compile(" t([0-9]+)[} ]").matcher(line);return m.find()?Integer.parseInt(m.group(1)):-1;}}}
 }catch(Exception ignored){}finally{if(process!=null)process.destroy();}return -1;}
 private static boolean focusGame(int task){if(task<0)return false;Process p=null;try{p=new ProcessBuilder("am","task","focus",Integer.toString(task)).redirectErrorStream(true).start();return p.waitFor(400,TimeUnit.MILLISECONDS)&&p.exitValue()==0;}catch(Exception e){return false;}finally{if(p!=null)p.destroy();}}
 private static boolean clearGameWindow(){HandlerThread thread=null;UiAutomation ui=null;try{
  thread=new HandlerThread("snap-tool-window");thread.start();Class<?> type=Class.forName("android.app.IUiAutomationConnection");Object connection=Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();ui=(UiAutomation)UiAutomation.class.getConstructor(Looper.class,type).newInstance(thread.getLooper(),connection);UiAutomation.class.getMethod("connect",int.class).invoke(ui,UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
  AccessibilityServiceInfo info=ui.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;ui.setServiceInfo(info);
  List<AccessibilityWindowInfo> windows=ui.getWindowsOnAllDisplays().get(0);if(windows==null)return false;boolean surface=false;
  for(AccessibilityWindowInfo w:windows){if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;AccessibilityNodeInfo root=w.getRoot();if(root==null)return false;
   if(!"dev.igawa6.edenduo".contentEquals(root.getPackageName()))return false;
   for(String id:new String[]{"in_game_menu","quick_settings_sheet","paused_icon"})for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("dev.igawa6.edenduo:id/"+id))if(n.isVisibleToUser())return false;
   for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("dev.igawa6.edenduo:id/surface_emulation"))if(n.isVisibleToUser())surface=true;
  }return surface;
 }catch(Throwable e){return false;}finally{if(ui!=null)try{UiAutomation.class.getMethod("disconnect").invoke(ui);}catch(Exception ignored){}if(thread!=null)thread.quitSafely();}}
 static void handle(String token,JSONObject sample)throws Exception{
  long now=System.currentTimeMillis();if(REQUEST.lastModified()==lastRequest)return;lastRequest=REQUEST.lastModified();
  if(!REQUEST.isFile()||REQUEST.length()>2048)return;
  JSONObject request;try(InputStream in=new FileInputStream(REQUEST)){byte[] bytes=new byte[2049];int n=in.read(bytes);if(n<=0||n>2048)return;request=new JSONObject(new String(bytes,0,n,StandardCharsets.UTF_8));}catch(Exception e){return;}
  String id=request.optString("id"),action=request.optString("action");if(!id.matches("[a-f0-9-]{36}")||id.equals(lastId))return;lastId=id;
  if(!token.equals(request.optString("token"))||!GameToolRules.timely(request.optLong("at"),now)||GameToolRules.key(action)<0)return;
  boolean accepted=false;int scan=GameToolRules.scan(action);
  String outcome="not-ready";
  if(GameToolRules.gameplay(sample.optString("status"),sample.optLong("phaseRva"))&&available()){
   if(!GameMemoryReader.physicalReleased(scan))outcome="controller-unavailable";
   else if(!clearGameWindow())outcome="window-not-clear";
   else if(!focusGame(gameTask()))outcome="focus-unavailable";
   else {
    JSONObject current=new JSONObject(GameMemoryReader.sample());
    if(!GameToolRules.timely(request.optLong("at"),System.currentTimeMillis()))outcome="request-expired";
    else if(GameToolRules.gameplay(current.optString("status"),current.optLong("phaseRva"))&&GameMemoryReader.physicalReleased(scan)&&GameMemoryReader.alive()){
     accepted=GameMemoryReader.pressTool(scan);
     outcome=accepted?"tool-dispatched":"controller-unavailable";
    }
   }
  }
  android.util.Log.i("SnapTool",outcome);
  feedback=new JSONObject().put("action",action).put("accepted",accepted).put("at",System.currentTimeMillis());
 }
 static void addFeedback(JSONObject row)throws Exception{row.put("inputAvailable",available());if(feedback!=null&&System.currentTimeMillis()-feedback.optLong("at")<=2000)row.put("lastAction",feedback);}
}
