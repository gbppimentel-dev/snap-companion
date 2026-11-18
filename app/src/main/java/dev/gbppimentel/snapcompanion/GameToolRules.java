package dev.gbppimentel.snapcompanion;
/** Fixed photography tools only. No arbitrary key, command, process or address surface. */
public final class GameToolRules {
 private GameToolRules(){}
 public static int key(String name){if(name==null)return-1;switch(name){case"fruit":return 97;case"orb":return 100;case"melody":return 103;case"scan":return 99;default:return-1;}}
 public static int scan(String name){if(name==null)return-1;switch(name){case"fruit":return 305;case"orb":return 308;case"melody":return 311;case"scan":return 307;default:return-1;}}
 public static boolean gameplay(String status,long phase){return"ready".equals(status)&&phase==0x62e180L;}
 public static boolean timely(long at,long now){return at>0&&now>=at&&now-at<=1000;}
}
