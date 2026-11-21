package dev.gbppimentel.snapcompanion;
import android.content.Intent;
import android.os.BatteryManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class DeviceBatteryTest {
 @Test public void reportsChargeSeparatelyFromHealthWithoutInventingCapacity()throws Exception{
  Intent i=new Intent().putExtra(BatteryManager.EXTRA_LEVEL,61).putExtra(BatteryManager.EXTRA_SCALE,100).putExtra(BatteryManager.EXTRA_HEALTH,BatteryManager.BATTERY_HEALTH_GOOD).putExtra(BatteryManager.EXTRA_STATUS,BatteryManager.BATTERY_STATUS_CHARGING);
  var b=DeviceBattery.from(i);assertEquals(61,b.getInt("percent"));assertEquals("Good",b.getString("health"));assertTrue(b.getBoolean("charging"));assertFalse(b.has("capacity"));
  assertEquals("Too Warm",DeviceBattery.from(i.putExtra(BatteryManager.EXTRA_HEALTH,BatteryManager.BATTERY_HEALTH_OVERHEAT)).getString("health"));
 }
 @Test public void absentOrInvalidBatteryNeverLooksFullOrHealthy()throws Exception{
  assertEquals(0,DeviceBattery.from(null).length());assertEquals(0,DeviceBattery.from(new Intent().putExtra(BatteryManager.EXTRA_PRESENT,false)).length());
  var b=DeviceBattery.from(new Intent().putExtra(BatteryManager.EXTRA_LEVEL,150).putExtra(BatteryManager.EXTRA_SCALE,100));assertFalse(b.has("percent"));assertEquals("Unknown",b.getString("health"));assertFalse(b.getBoolean("charging"));
 }
}
