package dev.gbppimentel.snapcompanion;
import java.nio.file.*;
import java.io.*;import java.util.Set;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;

public class EdenRequestsTest {
    private String mapping()throws Exception{return new String(Files.readAllBytes(Path.of("src/main/assets/request-map.json")),java.nio.charset.StandardCharsets.UTF_8);}
    private byte[] fixture()throws Exception{return Files.readAllBytes(Path.of("src/test/resources/newsnap/synthetic-requests-index14.bin"));}
    @Test public void independentlyEncryptedReceiptRecordsMapWithoutInventingCompletion()throws Exception{
        var decoder=new EdenRequests.Decoder(mapping());var snapshot=decoder.read(Path.of("src/test/resources/newsnap/synthetic-requests-index14.bin").toFile());
        assertEquals(3,snapshot.received().size());for(int index:new int[]{6,23,26})assertTrue(snapshot.received().contains(decoder.ids.get(index)));assertFalse(snapshot.received().contains(decoder.ids.get(0)));assertEquals(1,snapshot.unmapped());
    }
    @Test public void corruptOrPartialRegistryNeverReturnsPartialStatuses()throws Exception{
        byte[] raw=fixture();Path file=Files.createTempFile("snap-request-test-",".bin");try{Files.write(file,java.util.Arrays.copyOf(raw,raw.length-1));var decoder=new EdenRequests.Decoder(mapping());assertThrows(IOException.class,()->decoder.read(file.toFile()));raw[48+6*80+20]^=1;Files.write(file,raw);assertThrows(IOException.class,()->decoder.read(file.toFile()));}finally{Files.deleteIfExists(file);}
    }
    @Test public void rejectsIncompleteOrAmbiguousIdentityMap()throws Exception{JSONObject source=new JSONObject(mapping());source.getJSONArray("requests").getJSONObject(1).put("index",0);String duplicate=source.toString();assertThrows(JSONException.class,()->new EdenRequests.Decoder(duplicate));source=new JSONObject(mapping());source.getJSONArray("requests").remove(0);String incomplete=source.toString();assertThrows(JSONException.class,()->new EdenRequests.Decoder(incomplete));}

    @Test public void actualStateByteSeparatesReceivedAndCompletedWithoutUsingTheUnreadMarker()throws Exception{var decoder=new EdenRequests.Decoder(mapping());var saved=decoder.read(Path.of("src/test/resources/newsnap/synthetic-request-states.bin").toFile());assertTrue(saved.completionKnown());assertEquals(4,saved.received().size());assertEquals(Set.of(decoder.ids.get(23),decoder.ids.get(26)),saved.completed());assertFalse(saved.received().contains(decoder.ids.get(0)));assertFalse(saved.completed().contains(decoder.ids.get(6)));assertFalse(saved.completed().contains(decoder.ids.get(8)));assertEquals(1,saved.unmapped());}
    @Test public void unknownRequestStateRejectsTheEntireRegistry()throws Exception{assertThrows(IOException.class,()->new EdenRequests.Decoder(mapping()).read(Path.of("src/test/resources/newsnap/synthetic-request-invalid.bin").toFile()));}
    @Test public void onlyViewedCompletedStateSatisfiesTheRequestRewardCondition()throws Exception{var decoder=new EdenRequests.Decoder(mapping());var saved=decoder.read(Path.of("src/test/resources/newsnap/synthetic-request-states.bin").toFile());assertTrue(saved.rewardConditionsKnown());assertEquals(Set.of(decoder.ids.get(26)),saved.rewardReady());assertTrue(saved.completed().containsAll(saved.rewardReady()));assertFalse(saved.rewardReady().contains(decoder.ids.get(23)));}
}
