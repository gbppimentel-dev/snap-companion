package dev.gbppimentel.snapcompanion.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class CocoonTimeTest {
 @Test public void keepsLauncherTotalAndActiveSessionWithoutReplacingItWithAppUptime()throws Exception{var p=CocoonTime.decode("{\"totalMs\":27563825,\"at\":100000,\"source\":\"Cocoon\",\"active\":true}",101000);assertEquals(27563825,p.getLong("totalMs"));assertEquals("Cocoon",p.getString("source"));}
 @Test public void unavailableAmbiguousOrStaleReadDoesNotInventPlaytime()throws Exception{assertEquals(0,CocoonTime.decode("{}",100000).length());assertEquals(0,CocoonTime.decode("{\"totalMs\":10,\"at\":1,\"source\":\"Cocoon\"}",100000).length());assertEquals(0,CocoonTime.decode("{\"totalMs\":-5,\"at\":100000,\"source\":\"Cocoon\"}",100000).length());}
}
