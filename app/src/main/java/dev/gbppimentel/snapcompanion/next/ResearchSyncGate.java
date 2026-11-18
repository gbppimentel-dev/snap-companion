package dev.gbppimentel.snapcompanion.next;

/** Event-driven saved research refresh. No periodic expedition/unchanged-idle polling. */
final class ResearchSyncGate {
    private String key="";private boolean pending,wasQuiet;private long readyAt,lastAt;private int failures;
    boolean ready(String key,boolean quiet,long now){
        if(key.isEmpty()){reset();return false;}
        if(!key.equals(this.key)){this.key=key;pending=true;readyAt=0;failures=0;}
        if(quiet&&!wasQuiet)pending=true;
        if(now<lastAt){pending=true;readyAt=now;failures=0;}lastAt=now;wasQuiet=quiet;
        return quiet&&pending&&now>=readyAt;
    }
    void accepted(long now){pending=false;failures=0;readyAt=now+15000;}
    void failed(long now){pending=true;failures++;readyAt=now+Math.min(60000,3000L*(1L<<Math.min(5,failures-1)));}
    void reset(){key="";pending=wasQuiet=false;readyAt=lastAt=0;failures=0;}
}
