package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import java.util.*;
final class EdenProfiles {
    record Save(String user,long bytes){}
    static List<Save> discover(Context context)throws Exception{
        String result=AynBridge.run(context.getCacheDir(),"""
            snap_root=/sdcard/Android/data/dev.eden.eden_emulator/files/nand/user/save/0000000000000000
            for snap_file in "$snap_root"/*/0100F4300BF2C000/6706e7d0f3dab2ab; do
              [ -f "$snap_file" ] || continue
              snap_user=${snap_file%/0100F4300BF2C000/6706e7d0f3dab2ab}; snap_user=${snap_user##*/}
              [ ${#snap_user} = 32 ] || continue
              case "$snap_user" in *[!A-Fa-f0-9]*) continue;; esac
              echo "$snap_user|$(stat -c '%s' "$snap_file")"
            done
            """);List<Save> saves=new ArrayList<>();for(String line:result.split("\\R")){String[] values=line.split("\\|",-1);if(values.length==2&&values[0].matches("[A-Fa-f0-9]{32}")){try{long size=Long.parseLong(values[1]);if(size==240496)saves.add(new Save(values[0],size));}catch(NumberFormatException ignored){}}}return List.copyOf(saves);
    }
}
