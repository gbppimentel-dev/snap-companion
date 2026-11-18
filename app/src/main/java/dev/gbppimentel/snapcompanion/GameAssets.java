package dev.gbppimentel.snapcompanion;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Artwork extracted from the owner's game for this private device build. */
final class GameAssets {
    private final Context context;
    private final LruCache<String,Bitmap> images=new LruCache<String,Bitmap>(14*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
    private final Map<Integer,String> portraits=new HashMap<>();
    private final Map<Integer,List<String>> appearances=new HashMap<>();
    private final Map<String,String> courses=new HashMap<>();
    final Typeface body,heading;
    GameAssets(Context context){
        this.context=context;
        body=font("snap-body.ttf");heading=font("snap-heading.ttf");
        try(InputStream in=context.getAssets().open("game-art/index.json")){
            JSONObject index=new JSONObject(new String(MainActivity.readBounded(in,200000),java.nio.charset.StandardCharsets.UTF_8));
            JSONObject p=index.getJSONObject("portraits");for(Iterator<String> it=p.keys();it.hasNext();){String id=it.next();portraits.put(Integer.parseInt(id),p.getString(id));}
            JSONObject a=index.getJSONObject("appearances");for(Iterator<String> it=a.keys();it.hasNext();){String id=it.next();JSONArray array=a.getJSONArray(id);List<String> paths=new ArrayList<>();for(int i=0;i<array.length();i++)paths.add(array.getString(i));appearances.put(Integer.parseInt(id),List.copyOf(paths));}
            JSONObject c=index.optJSONObject("courses");if(c!=null)for(Iterator<String> it=c.keys();it.hasNext();){String id=it.next();courses.put(id,c.getString(id));}
        }catch(Exception e){throw new IllegalStateException("The game artwork pack is missing",e);}
    }
    private Typeface font(String name){return Typeface.createFromAsset(context.getAssets(),"game-art/fonts/"+name);}
    int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    Bitmap bitmap(String name){Bitmap result=images.get(name);if(result!=null)return result;try(InputStream in=context.getAssets().open("game-art/"+name)){result=BitmapFactory.decodeStream(in);if(result!=null)images.put(name,result);return result;}catch(IOException e){return null;}}
    ImageView image(String name,String description){ImageView v=new ImageView(context);v.setImageBitmap(bitmap(name));v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setContentDescription(description);return v;}
    ImageView pokemon(int id,String name){return image(portraits.getOrDefault(id,""),name+" · game portrait");}
    ImageView course(String id,String name){return image(courses.getOrDefault(id,""),name+" · game course thumbnail");}
    ImageView savedPhoto(File file,String description){String key="saved:"+file.getPath();Bitmap thumbnail=images.get(key);if(thumbnail==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;thumbnail=BitmapFactory.decodeFile(file.getPath(),options);if(thumbnail!=null)images.put(key,thumbnail);}ImageView view=new ImageView(context);view.setImageBitmap(thumbnail);view.setScaleType(ImageView.ScaleType.FIT_CENTER);view.setContentDescription(description);return view;}
    List<String> appearances(int id){return appearances.getOrDefault(id,List.of());}
    Drawable texture(String name){Bitmap b=bitmap("ui/"+name+".webp");if(b==null)return new android.graphics.drawable.ColorDrawable(0xFFF6F0E3);BitmapDrawable d=new BitmapDrawable(context.getResources(),b);if(name.equals("pattern")){d.setTileModeX(Shader.TileMode.REPEAT);d.setTileModeY(Shader.TileMode.REPEAT);}return d;}
    Drawable ribbon(boolean selected){return new Ribbon(bitmap("ui/"+(selected?"ribbon-selected":"ribbon")+".webp"));}
    Drawable button(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_pressed},ribbon(true));d.addState(new int[]{android.R.attr.state_focused},ribbon(true));d.addState(new int[]{},ribbon(false));return d;}
    LinearLayout stars(int count,String medal,int size){
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);
        String tier=Set.of("Bronze","Silver","Gold","Diamond").contains(medal)?medal.toLowerCase(Locale.ROOT):"empty";
        row.setContentDescription(count+" star category · "+(tier.equals("empty")?"not registered":medal));
        for(int i=0;i<count;i++){ImageView star=image("ui/star-"+tier+".webp",null);star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(star,new LinearLayout.LayoutParams(dp(size),dp(size)));}
        return row;
    }
    View diamond(String icon,String description,Runnable action,int size){
        FrameLayout frame=new FrameLayout(context);frame.setContentDescription(description);frame.setClickable(true);frame.setFocusable(true);
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_focused},texture("diamond-selected"));states.addState(new int[]{android.R.attr.state_pressed},texture("diamond-selected"));states.addState(new int[]{},texture("diamond"));frame.setBackground(states);
        ImageView picture=image("ui/"+icon+".webp",null);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(size*.55f),dp(size*.55f),Gravity.CENTER);lp.bottomMargin=dp(4);frame.addView(picture,lp);frame.setOnClickListener(v->action.run());return frame;
    }
    void fonts(View view){if(view instanceof TextView t&&!heading.equals(t.getTypeface()))t.setTypeface(body);if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)fonts(group.getChildAt(i));}
    /** Preserve the pointed ends and border while stretching only the middle of the game ribbon. */
    private static final class Ribbon extends Drawable {
        private final Bitmap image;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Ribbon(Bitmap b){image=b;}
        public void draw(Canvas canvas){if(image==null)return;Rect b=getBounds();int sourceEdge=58;float edge=Math.min(b.width()/3f,b.height()*sourceEdge/image.getHeight());
            canvas.drawBitmap(image,new Rect(0,0,sourceEdge,image.getHeight()),new RectF(b.left,b.top,b.left+edge,b.bottom),paint);
            canvas.drawBitmap(image,new Rect(sourceEdge,0,image.getWidth()-sourceEdge,image.getHeight()),new RectF(b.left+edge,b.top,b.right-edge,b.bottom),paint);
            canvas.drawBitmap(image,new Rect(image.getWidth()-sourceEdge,0,image.getWidth(),image.getHeight()),new RectF(b.right-edge,b.top,b.right,b.bottom),paint);
        }
        public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
