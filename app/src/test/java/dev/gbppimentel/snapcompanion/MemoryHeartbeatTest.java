package dev.gbppimentel.snapcompanion;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class MemoryHeartbeatTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 @Test public void readerNeverSeesAnEmptyOrPartialHeartbeatDuringRefresh()throws Exception{
  File watch=new File(folder.getRoot(),"memory.watch");String token="c8b5798a-7011-486a-ab44-81af70fe7190";
  GameMemoryLink.writeHeartbeat(watch,token);
  ExecutorService reader=Executors.newSingleThreadExecutor();CountDownLatch started=new CountDownLatch(1);AtomicInteger samples=new AtomicInteger();AtomicBoolean stop=new AtomicBoolean();
  Future<?> reading=reader.submit(()->{started.countDown();while(!stop.get())try{assertEquals(token,new String(Files.readAllBytes(watch.toPath()),java.nio.charset.StandardCharsets.UTF_8));samples.incrementAndGet();}catch(java.io.IOException e){throw new AssertionError(e);}});
  try{assertTrue(started.await(2,TimeUnit.SECONDS));for(int i=0;i<300;i++){GameMemoryLink.writeHeartbeat(watch,token);if(reading.isDone())reading.get();}assertTrue(samples.get()>0);assertEquals(token,new String(Files.readAllBytes(watch.toPath()),java.nio.charset.StandardCharsets.UTF_8));}
  finally{stop.set(true);reader.shutdown();assertTrue(reader.awaitTermination(2,TimeUnit.SECONDS));}
  // Surface an assertion from the concurrent reader rather than silently losing it.
  if(reading.isDone())reading.get();
 }
}
