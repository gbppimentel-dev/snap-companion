package dev.gbppimentel.snapcompanion;
import java.io.IOException;import java.util.Set;
/** Validate all bridge fields before publishing presence or reading a snapshot. */
record EdenLinkReply(String pid,String user,String fingerprint,String state,boolean visible,String requestFingerprint,String researchFingerprint,boolean closedCopyAllowed) {
 EdenLinkReply(String pid,String user,String fingerprint,String state,boolean visible){this(pid,user,fingerprint,state,visible,"","",false);}
 static EdenLinkReply parse(String response)throws IOException{
  String[] f=response.split("\\|",-1);
  boolean latest=f.length==9&&f[0].equals("snap3"),extended=latest||f.length==8&&f[0].equals("snap2");
  if(!(extended||f.length==6&&f[0].equals("snap"))||!f[1].matches("0|[1-9][0-9]{0,9}")||!Set.of("copied","same","changing","no-save","unsupported").contains(f[4])||!Set.of("0","1").contains(f[5]))throw new IOException("Unverified game connection");
  if(extended&&(!f[6].matches("[a-f0-9]{64}|")||!f[7].matches("[a-f0-9]{64}|")))throw new IOException("Invalid auxiliary revision");
  if(latest&&(!Set.of("0","1").contains(f[8])||f[8].equals("1")&&(!f[1].equals("0")||!f[5].equals("0"))))throw new IOException("Invalid closed-copy proof");
  try{if(Long.parseLong(f[1])>Integer.MAX_VALUE)throw new IOException("Invalid game process");}catch(NumberFormatException e){throw new IOException("Invalid game process",e);}
  if(f[1].equals("0")&&!f[5].equals("0"))throw new IOException("Closed game cannot be visible");
  if(f[4].equals("no-save")){if(!f[2].isEmpty()||!f[3].isEmpty()||extended&&(!f[6].isEmpty()||!f[7].isEmpty()))throw new IOException("Unexpected save identity");}
  else if(!f[2].matches("[A-Fa-f0-9]{32}")||!f[3].matches("[a-f0-9]{64}"))throw new IOException("Invalid save identity");
  return new EdenLinkReply(f[1],f[2],f[3],f[4],f[5].equals("1"),extended?f[6]:"",extended?f[7]:"",latest&&f[8].equals("1"));
 }
}
