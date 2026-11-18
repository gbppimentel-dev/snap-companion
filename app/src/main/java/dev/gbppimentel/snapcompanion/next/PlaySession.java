package dev.gbppimentel.snapcompanion.next;
/** Observed foreground session time, not lifetime game playtime. */
final class PlaySession {
    private String process="";private long total,last;private boolean running;
    synchronized void sample(String pid,boolean visible,long now){
        if(!pid.isEmpty()&&!process.equals(pid)){process=pid;total=0;last=now;running=visible;return;}
        if(running&&now>=last&&now-last<=5000)total+=now-last;
        running=visible&&!pid.isEmpty();last=now;
    }
    synchronized long millis(long now){return total+(running&&now>=last&&now-last<=5000?now-last:0);}
}
