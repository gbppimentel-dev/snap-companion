package dev.gbppimentel.snapcompanion;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import org.json.JSONObject;

/** Android's current charge only; no health labels or estimated capacity percentage. */
final class DeviceBattery {
    static JSONObject read(Context context) {
        try { return from(context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED))); }
        catch(RuntimeException e) { return new JSONObject(); }
    }
    static JSONObject from(Intent intent) {
        JSONObject out=new JSONObject();
        if(intent==null||!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT,true))return out;
        try {
            int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            if(level>=0&&scale>0&&level<=scale)out.put("percent",Math.round(level*100.0/scale));
            int status=intent.getIntExtra(BatteryManager.EXTRA_STATUS,BatteryManager.BATTERY_STATUS_UNKNOWN);
            out.put("charging",status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL);

        } catch(org.json.JSONException ignored) { return new JSONObject(); }
        return out;
    }
}
