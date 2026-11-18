package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import android.graphics.*;
import java.io.*;

/** Fixed-layout Camp detection from three independent game icon masks. No 3D subject inference. */
final class MenuDetector {
    private final boolean[][] masks=new boolean[3][];
    MenuDetector(Context context)throws IOException{
        try(InputStream in=context.getAssets().open("game-art/recognition/camp-icons.png")){
            Bitmap reference=BitmapFactory.decodeStream(in);if(reference==null||reference.getWidth()!=24||reference.getHeight()!=72)throw new IOException("Menu masks missing");
            for(int i=0;i<3;i++){masks[i]=new boolean[576];for(int y=0;y<24;y++)for(int x=0;x<24;x++)masks[i][y*24+x]=Color.red(reference.getPixel(x,y+i*24))<130;}reference.recycle();
        }
    }
    boolean camp(Bitmap frame){if(Math.abs(frame.getWidth()/(double)frame.getHeight()-16.0/9)>.04)return false;int[] centers={214,396,578};int matched=0;for(int i=0;i<3;i++)if(similarity(frame,masks[i],centers[i])>=.86)matched++;return matched>=2;}
    private double similarity(Bitmap frame,boolean[] mask,int cy){
        double best=0;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){
            int overlap=0,union=0;for(int y=0;y<24;y++)for(int x=0;x<24;x++){
                int px=Math.max(0,Math.min(frame.getWidth()-1,Math.round((100+(x+.5f)*88/24+dx)*frame.getWidth()/1920f)));
                int py=Math.max(0,Math.min(frame.getHeight()-1,Math.round((cy-44+(y+.5f)*88/24+dy)*frame.getHeight()/1080f)));
                int color=frame.getPixel(px,py);boolean dark=(Color.red(color)+Color.green(color)+Color.blue(color))/3<130,expected=mask[y*24+x];if(dark&&expected)overlap++;if(dark||expected)union++;
            }best=Math.max(best,overlap/(double)Math.max(1,union));
        }return best;
    }
}
