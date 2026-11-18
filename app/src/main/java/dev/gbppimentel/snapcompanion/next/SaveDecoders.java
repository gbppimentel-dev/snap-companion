package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class SaveDecoders {
    static EdenPhotodex photodex(Context context)throws Exception{try(InputStream a=context.getAssets().open("catalog.json");InputStream b=context.getAssets().open("eden-species.json")){return new EdenPhotodex(new String(MainActivity.readBounded(b,100000),StandardCharsets.UTF_8),new Catalog(new String(MainActivity.readBounded(a,2000000),StandardCharsets.UTF_8)));}}
}
