package dev.gbppimentel.snapcompanion.next;
/** Targets are ceilings. Adaptive mode uses measured OCR cost and Android thermal severity. */
final class CaptureCadence {
    static long interval(int hz,long inferenceMillis,int thermal,boolean adaptive,Float gameFps){return interval(hz,inferenceMillis,thermal,adaptive,gameFps,null);}
    static long interval(int hz,long inferenceMillis,int thermal,boolean adaptive,Float gameFps,Float gameMillis){long result=interval(hz,inferenceMillis,thermal,adaptive);if(adaptive){if(gameFps!=null&&Float.isFinite(gameFps)&&gameFps>=0&&gameFps<55)result=Math.max(result,gameFps<5?2000:1000);if(gameMillis!=null&&Float.isFinite(gameMillis)&&gameMillis>20)result=Math.max(result,1000);}return result;}
    static long interval(int hz,long inferenceMillis,int thermal,boolean adaptive){long base=1000L/Math.max(1,Math.min(10,hz));if(!adaptive)return base;long budget=Math.max(base,Math.min(2000,Math.max(0,Math.min(2000,inferenceMillis))*3));if(thermal>=3)budget=Math.max(2000,budget);else if(thermal>=2)budget=Math.max(1000,budget);return budget;}
    static long failureInterval(int consecutiveFailures){return consecutiveFailures<=0?0:500L<<Math.min(4,consecutiveFailures-1);}
    static boolean pause(int thermal,boolean adaptive){return adaptive&&thermal>=4;}
}
