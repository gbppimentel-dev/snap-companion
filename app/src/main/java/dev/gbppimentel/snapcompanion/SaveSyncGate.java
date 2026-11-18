package dev.gbppimentel.snapcompanion;
/** Two equal snapshots before adoption; retry failures without waiting for another game save. */
final class SaveSyncGate {
 private String candidate="",accepted="",attempted="";private long retryAt;
 void reset(){candidate=accepted=attempted="";retryAt=0;}
 boolean skipUnchanged(long now){return !candidate.isEmpty()&&(candidate.equals(accepted)||candidate.equals(attempted)&&now<retryAt&&retryAt-now<=60000);}
 boolean ready(String hash,long now){
  if(!hash.equals(candidate)){candidate=hash;attempted="";retryAt=0;return false;}
  if(hash.equals(accepted)||hash.equals(attempted)&&now<retryAt&&retryAt-now<=60000)return false;
  attempted=hash;retryAt=now+15000;return true;
 }
 void accepted(String hash){accepted=hash;attempted="";retryAt=0;}
 void rejected(long now){retryAt=now+60000;}
}
