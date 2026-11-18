package dev.gbppimentel.snapcompanion.next;
import org.junit.Test;import static org.junit.Assert.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Execute the generated read-only protocol against local files and fixed process-command fixtures. */
public class LinkBridgeScriptTest {
 @Test public void actualScriptSeparatesPresenceFromCopiedChangingMissingAndUnsupportedSaves()throws Exception{
  Path root=Files.createTempDirectory("snap-link-protocol-"),bin=Files.createDirectory(root.resolve("bin"));
  try{
   Path game=Files.createDirectories(root.resolve("game")),log=Files.createDirectories(game.resolve("log"));write(log.resolve("eden_log.txt"),"Core Loader Load: Loading New Pokemon Snap (0100F4300BF2C000)\n");
   String user="A".repeat(32);Path save=Files.createDirectories(game.resolve("nand/user/save/0000000000000000/"+user+"/0100F4300BF2C000")),metadata=save.resolve("6706e7d0f3dab2ab"),out=root.resolve("copy.bin");Files.write(metadata,new byte[240496]);Files.write(save.resolve("75006a27cf0c9275"),new byte[19248]);Files.write(save.resolve("4b1a0e859a3f7816"),new byte[15568]);Files.write(save.resolve("76401e93e6565bd5"),new byte[3056]);
   command(bin,"pidof","echo 123");command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 dev.eden.eden_emulator/org.yuzu.yuzu_emu.activities.EmulationActivity}\\n'");
   command(bin,"stat","wc -c < \"$3\" | tr -d ' '\nif [ \"$2\" != %s ] && [ -f \"$3.rev\" ]; then cat \"$3.rev\"; fi");
   command(bin,"sha256sum",System.getProperty("os.name").toLowerCase().contains("mac")?"exec /usr/bin/shasum -a 256 \"$@\"":"exec /usr/bin/sha256sum \"$@\"");command(bin,"chown","exit 0");
   command(bin,"cp","/bin/cp \"$@\"\nif [ -f '"+root.resolve("mutate")+"' ]; then printf x >> '"+metadata+"'; fi");
   String script=EdenLinkService.script(out.toFile(),123,user,"").replace("snap_root=/sdcard/Android/data/dev.eden.eden_emulator/files","snap_root='"+game+"'");
   var first=EdenLinkReply.parse(run(script,bin));assertEquals("copied",first.state());assertTrue(first.visible());assertEquals(240496,Files.size(out));
   String skip=EdenLinkService.script(out.toFile(),123,user,first.fingerprint()).replace("snap_root=/sdcard/Android/data/dev.eden.eden_emulator/files","snap_root='"+game+"'");assertEquals("same",EdenLinkReply.parse(run(skip,bin)).state());
   assertFalse(first.requestFingerprint().isEmpty());assertFalse(first.researchFingerprint().isEmpty());
   Files.delete(out);write(save.resolve("4b1a0e859a3f7816.rev"),"research-revision2");write(save.resolve("75006a27cf0c9275.rev"),"request-revision2");
   var changed=EdenLinkReply.parse(run(skip,bin));assertEquals("same",changed.state());assertEquals(first.fingerprint(),changed.fingerprint());assertNotEquals(first.requestFingerprint(),changed.requestFingerprint());assertNotEquals(first.researchFingerprint(),changed.researchFingerprint());assertFalse(Files.exists(out));
   Files.delete(save.resolve("76401e93e6565bd5"));assertEquals("",EdenLinkReply.parse(run(skip,bin)).researchFingerprint());
   command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 dev.eden.eden_emulator/.MainActivity}\\n'");assertFalse(EdenLinkReply.parse(run(skip,bin)).visible());
   command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 dev.eden.eden_emulator/org.yuzu.yuzu_emu.activities.EmulationActivity}\\n'");
   command(bin,"pidof","exit 0");command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 dev.igawa6.edenduo/org.yuzu.yuzu_emu.activities.EmulationActivity}\\n'");assertFalse(EdenLinkReply.parse(run(skip,bin)).closedCopyAllowed());
   command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 rip.moth.cocoonshell/.MainActivity}\\n'");assertTrue(EdenLinkReply.parse(run(skip,bin)).closedCopyAllowed());
   command(bin,"pidof","echo 123");command(bin,"dumpsys","printf 'Display #0 (activities):\\n topResumedActivity=ActivityRecord{x u0 dev.eden.eden_emulator/org.yuzu.yuzu_emu.activities.EmulationActivity}\\n'");
   Files.createFile(root.resolve("mutate"));assertEquals("changing",EdenLinkReply.parse(run(script,bin)).state());assertFalse(Files.exists(out));Files.delete(root.resolve("mutate"));assertEquals("unsupported",EdenLinkReply.parse(run(script,bin)).state());assertFalse(Files.exists(out));
   Files.delete(metadata);assertEquals("no-save",EdenLinkReply.parse(run(script,bin)).state());write(log.resolve("eden_log.txt"),"Core Loader Load: Loading another game (0000000000000001)\n");assertEquals("other-game",run(script,bin));
  }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 private void write(Path file,String text)throws Exception{Files.write(file,text.getBytes(StandardCharsets.UTF_8));}
 private void command(Path bin,String name,String body)throws Exception{Path f=bin.resolve(name);write(f,"#!/bin/sh\n"+body+"\n");assertTrue(f.toFile().setExecutable(true));}
 private String run(String script,Path bin)throws Exception{ProcessBuilder b=new ProcessBuilder("/bin/sh","-c",script);b.environment().put("PATH",bin+":"+System.getenv("PATH"));b.redirectError(new java.io.File("/dev/null"));Process p=b.start();assertTrue(p.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();assertEquals(out,0,p.exitValue());return out;}
}
