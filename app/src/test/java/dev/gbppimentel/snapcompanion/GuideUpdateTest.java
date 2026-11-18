package dev.gbppimentel.snapcompanion;
import org.junit.*;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
public class GuideUpdateTest {
    private String current;private Catalog catalog;
    @Before public void setup()throws Exception{current=GuideBookTest.asset("guidebook.json");catalog=new Catalog(GuideBookTest.asset("catalog.json"));}
    private JSONObject patch()throws Exception{JSONObject base=new JSONObject(current);JSONObject cue=base.getJSONArray("photoCues").getJSONObject(0);JSONObject revised=new JSONObject(cue.toString()).put("procedure","Photograph the referenced behavior after checking the linked source.");return new JSONObject().put("format","snap-guide-update").put("version",1).put("baseRevision",base.getString("revision")).put("baseSha256",GuideUpdates.sha256(current)).put("revision","fixture-next").put("changes",new JSONArray().put(new JSONObject().put("section","photoCues").put("key",cue.getInt("species")+":"+cue.getInt("stars")).put("value",revised)));}
    @Test public void smallCorrectionRetainsFullDatasetAndOriginal()throws Exception{String next=GuideUpdates.resolve(current,patch().toString());GuideBook guide=new GuideBook(next,catalog);assertEquals(936,guide.cues.size());assertEquals("fixture-next",guide.revision);assertNotEquals(next,current);assertEquals(new JSONObject(current).getString("revision"),new GuideBook(current,catalog).revision);}
    @Test public void wrongBaseOrArbitraryMutationRejected()throws Exception{JSONObject wrong=patch().put("baseSha256","wrong");assertThrows(JSONException.class,()->GuideUpdates.resolve(current,wrong.toString()));JSONObject p=patch();p.getJSONArray("changes").getJSONObject(0).put("section","personalProgress");JSONObject bad=p;assertThrows(JSONException.class,()->GuideUpdates.resolve(current,bad.toString()));}
    @Test public void captureCadenceHonorsThermalAndCostBudget(){assertEquals(500,CaptureCadence.interval(2,20,0,true));assertEquals(600,CaptureCadence.interval(10,200,0,true));assertEquals(2000,CaptureCadence.interval(10,20,3,true));assertTrue(CaptureCadence.pause(4,true));assertFalse(CaptureCadence.pause(4,false));}
}
