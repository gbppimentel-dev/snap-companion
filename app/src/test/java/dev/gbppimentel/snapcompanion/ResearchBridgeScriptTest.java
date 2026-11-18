package dev.gbppimentel.snapcompanion;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Execute the generated small-copy protocol against temporary files, never a real save/device. */
public class ResearchBridgeScriptTest {
 @Test public void boundedCopiesRejectChangingSourcesCorruptCopiesAndWrongSaveRevision()throws Exception{
  Path root=Files.createTempDirectory("snap-research-protocol-"),bin=Files.createDirectory(root.resolve("bin")),save=Files.createDirectory(root.resolve("save")),out=Files.createDirectory(root.resolve("copy"));
  try{
   Path records=save.resolve("4b1a0e859a3f7816"),flags=save.resolve("76401e93e6565bd5"),metadata=save.resolve("6706e7d0f3dab2ab");Files.write(records,new byte[15568]);Files.write(flags,new byte[3056]);Files.write(metadata,new byte[]{1,2,3});String hash=hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(metadata)));
   command(bin,"stat","wc -c < \"$3\" | tr -d ' '");command(bin,"sha256sum",System.getProperty("os.name").toLowerCase().contains("mac")?"exec /usr/bin/shasum -a 256 \"$@\"":"exec /usr/bin/sha256sum \"$@\"");command(bin,"chown","exit 0");command(bin,"cp","/bin/cp \"$@\"\nif [ -f '"+root.resolve("mutate")+"' ]; then printf x >> '"+records+"'; fi\nif [ -f '"+root.resolve("corrupt")+"' ]; then printf x >> \"$2\"; fi");
   String script=EdenResearch.script(out.toFile(),123,"A".repeat(32),hash).replace("snap_dir=/sdcard/Android/data/dev.eden.eden_emulator/files/nand/user/save/0000000000000000/"+"A".repeat(32)+"/0100F4300BF2C000","snap_dir='"+save+"'");
   assertEquals("stable",run(script,bin));assertArrayEquals(Files.readAllBytes(records),Files.readAllBytes(out.resolve("records")));assertArrayEquals(Files.readAllBytes(flags),Files.readAllBytes(out.resolve("flags")));
   Files.createFile(root.resolve("corrupt"));assertEquals("incomplete",run(script,bin));Files.delete(root.resolve("corrupt"));
   Files.createFile(root.resolve("mutate"));assertEquals("changing",run(script,bin));Files.delete(root.resolve("mutate"));assertEquals("unsupported",run(script,bin));
   Files.write(records,new byte[15568]);Files.write(metadata,new byte[]{9});assertEquals("changed",run(script,bin));
  }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))Files.deleteIfExists(p);}}
 }
 private void command(Path bin,String name,String body)throws Exception{Path f=bin.resolve(name);Files.write(f,("#!/bin/sh\n"+body+"\n").getBytes(StandardCharsets.UTF_8));assertTrue(f.toFile().setExecutable(true));}
 private String run(String script,Path bin)throws Exception{ProcessBuilder b=new ProcessBuilder("/bin/sh","-c",script);b.environment().put("PATH",bin+":"+System.getenv("PATH"));b.redirectError(new java.io.File("/dev/null"));Process p=b.start();assertTrue(p.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));String value=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();assertEquals(value,0,p.exitValue());return value;}
 private String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
}
