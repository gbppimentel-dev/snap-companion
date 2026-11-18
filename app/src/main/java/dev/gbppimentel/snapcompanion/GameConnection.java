package dev.gbppimentel.snapcompanion;
/** Immutable, expiring proof that this process is Snap on the game display. */
record GameConnection(String pid,boolean visible,long checkedAt,long generation) {
 static GameConnection empty(){return new GameConnection("",false,0,0);}
 boolean fresh(long now){return checkedAt>0&&now>=checkedAt&&now-checkedAt<6000;}
 boolean running(long now){return !pid.isEmpty()&&fresh(now);}
 boolean onScreen(long now){return visible&&running(now);}
 boolean closed(long now){return pid.isEmpty()&&fresh(now);}
 String token(){return generation+":"+pid;}
 GameConnection observed(String nextPid,boolean nextVisible,long at){
  String next=nextPid.equals("0")?"":nextPid;
  if(!next.isEmpty()&&!next.matches("[1-9][0-9]{0,9}"))throw new IllegalArgumentException("Invalid game process");
  boolean show=nextVisible&&!next.isEmpty();boolean changed=!next.equals(pid)||show!=visible||!fresh(at);
  return new GameConnection(next,show,at,generation+(changed?1:0));
 }
 GameConnection disconnected(){return new GameConnection("",false,0,generation+1);}
}
