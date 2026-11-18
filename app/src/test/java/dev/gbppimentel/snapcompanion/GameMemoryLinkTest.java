package dev.gbppimentel.snapcompanion;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class GameMemoryLinkTest {
 private JSONObject sample()throws Exception{return new JSONObject().put("token","session").put("pid",123).put("at",10000).put("status","ready").put("courseId",2).put("level",1).put("distance",12).put("length",100);}
 @Test public void acceptsCurrentSelectedLevelAndCourse()throws Exception{JSONObject r=GameMemoryLink.validate(sample(),"session","123",10500);assertEquals(2,r.getInt("courseId"));assertEquals(1,r.getInt("level"));}
 @Test public void rejectsOldSessionProcessStaleAndFuture()throws Exception{assertTrue(GameMemoryLink.validate(sample(),"old","123",10500).length()==0);assertTrue(GameMemoryLink.validate(sample(),"session","124",10500).length()==0);assertTrue(GameMemoryLink.validate(sample(),"session","123",11001).length()==0);assertTrue(GameMemoryLink.validate(sample(),"session","123",9999).length()==0);}
 @Test public void discoveryAndInvalidFieldsNeverBecomeCurrentCourse()throws Exception{for(String state:new String[]{"seeking","unsupported","ambiguous","unavailable"})assertTrue(GameMemoryLink.validate(sample().put("status",state),"session","123",10500).length()==0);assertTrue(GameMemoryLink.validate(sample().put("level",0),"session","123",10500).length()==0);assertTrue(GameMemoryLink.validate(sample().put("courseId",31),"session","123",10500).length()==0);assertTrue(GameMemoryLink.validate(sample().put("distance",101),"session","123",10500).length()==0);}
 @Test public void idleHasNoInventedCourseRequirement()throws Exception{assertEquals("idle",GameMemoryLink.validate(new JSONObject().put("token","session").put("pid",123).put("at",10000).put("status","idle"),"session","123",10500).getString("status"));}
}
