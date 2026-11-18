package dev.gbppimentel.snapbridge;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
/** Fixed-purpose read-only metadata reader. Never opens any game save or changes Cocoon. */
public final class CocoonPlaytime {
 public static void main(String[] args){try(SQLiteDatabase db=SQLiteDatabase.openDatabase("/data/user/0/rip.moth.cocoonshell/databases/cocoon_db",null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
  long id=-1,minutes=0;
  try(Cursor c=db.rawQuery("SELECT id,playTimeMinutes FROM games WHERE displayName COLLATE NOCASE IN ('New Pokémon Snap','New Pokemon Snap') OR UPPER(uri) LIKE '%0100F4300BF2C000%' LIMIT 2",null)){if(c.moveToFirst()){id=c.getLong(0);minutes=Math.max(0,c.getLong(1));if(c.moveToNext())id=-1;}}
  if(id<0){System.out.println("{}");return;}
  long pending=0;boolean active=false;
  try(Cursor c=db.rawQuery("SELECT startTimeMs,pauseStartedAtMs,excludedDurationMs,state FROM pending_game_sessions WHERE gameId=? AND finalizedAtMs IS NULL AND state IN ('ACTIVE','PAUSED','GRACE') ORDER BY createdAtMs DESC LIMIT 1",new String[]{Long.toString(id)})){
   if(c.moveToFirst()){long now=System.currentTimeMillis(),start=c.getLong(0),end=c.isNull(1)?now:Math.min(now,c.getLong(1));if(start>0&&start<=now)pending=Math.max(0,end-start-Math.max(0,c.getLong(2)));active=c.isNull(1)&&"ACTIVE".equals(c.getString(3));}
  }catch(Exception ignored){} // Some launcher versions only store finalized minutes.
  System.out.println(new JSONObject().put("totalMs",Math.addExact(Math.multiplyExact(minutes,60000),pending)).put("at",System.currentTimeMillis()).put("active",active).put("source","Cocoon"));
 }catch(Exception e){System.out.println("{}");}}
}
