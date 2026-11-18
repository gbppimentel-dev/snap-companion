package dev.gbppimentel.snapcompanion;
import java.util.*;
/** Physical Linux gamepad buttons only. No key injection, device grab, or input writes. */
public final class ControllerButtons {
 private final Map<String,Double> axes=new LinkedHashMap<>();
 private final Map<String,Boolean> down=new LinkedHashMap<>();private final Map<String,Long> pulses=new LinkedHashMap<>();
 private boolean dropped;
 public static String name(int code){switch(code){case 304:return "A";case 305:return "B";case 307:return "X";case 308:return "Y";case 310:return "L1";case 311:return "R1";case 312:return "L2";case 313:return "R2";case 314:return "SELECT";case 315:return "START";case 317:return "L3";case 318:return "R3";case 544:return "UP";case 545:return "DOWN";case 546:return "LEFT";case 547:return "RIGHT";default:return null;}}
 public static int builtInScore(String name){String n=name.toLowerCase(Locale.ROOT);boolean pad=n.contains("gamepad")||n.contains("controller")||n.contains("joystick");return pad&&n.contains("odin")?100:pad&&n.contains("ayn")?90:0;}
 private boolean button(String name,boolean pressed,long at){if(Boolean.valueOf(pressed).equals(down.get(name)))return false;down.put(name,pressed);if(pressed)pulses.put(name,at);return true;}
 public synchronized boolean accept(int type,int code,int value,long at){
  if(type==0&&code==3){dropped=true;down.clear();pulses.clear();axes.clear();return true;}
  if(dropped){if(type==0&&code==0)dropped=false;return false;}
  if(type==1){String key=name(code);return key!=null&&(value==0||value==1)&&button(key,value==1,at);}
  if(type==3){String axis=code==0?"lx":code==1?"ly":code==2?"rx":code==5?"ry":null;if(axis!=null){double normalized=Math.max(-1,Math.min(1,value/32767.0));double next=Math.abs(normalized)<.025?0:Math.round(normalized*100)/100.0;Double previous=axes.put(axis,next);return previous==null||Double.compare(previous,next)!=0;}if(code==16){boolean a=button("LEFT",value<0,at);return button("RIGHT",value>0,at)||a;}if(code==17){boolean a=button("UP",value<0,at);return button("DOWN",value>0,at)||a;}if(code==9)return button("R2",value>2000,at);if(code==10)return button("L2",value>2000,at);}
  return false;
 }
 public synchronized Map<String,Double> axes(){return new LinkedHashMap<>(axes);}
 public synchronized Map<String,Boolean> down(){return new LinkedHashMap<>(down);}
 public synchronized Map<String,Long> pulses(){return new LinkedHashMap<>(pulses);}
}
