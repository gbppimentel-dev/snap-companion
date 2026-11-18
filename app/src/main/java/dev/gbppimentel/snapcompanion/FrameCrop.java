package dev.gbppimentel.snapcompanion;

import android.graphics.Bitmap;
import android.graphics.Rect;

/** Conservative black-bar trimming plus explicit user margins. Coordinates remain inside the source. */
public final class FrameCrop {
    public static Rect bounds(Bitmap b,int horizontalPercent,int verticalPercent,boolean autoBars) {
        int w=b.getWidth(),h=b.getHeight(),left=0,right=w,top=0,bottom=h;
        if(autoBars) {
            while(top<h/5 && blackRow(b,top))top++;
            while(bottom>h-h/5 && blackRow(b,bottom-1))bottom--;
            // Require a matched pair of substantial bars; do not trim a single dark HUD strip.
            if(top<3 || h-bottom<3 || Math.abs(top-(h-bottom))>Math.max(3,h/100)){top=0;bottom=h;}
            while(left<w/5 && blackColumn(b,left))left++;
            while(right>w-w/5 && blackColumn(b,right-1))right--;
            if(left<3 || w-right<3 || Math.abs(left-(w-right))>Math.max(3,w/100)){left=0;right=w;}
        }
        int dx=(right-left)*Math.max(0,Math.min(20,horizontalPercent))/100,dy=(bottom-top)*Math.max(0,Math.min(20,verticalPercent))/100;
        return new Rect(left+dx,top+dy,right-dx,bottom-dy);
    }
    private static boolean dark(int c){return ((c>>16)&255)<9 && ((c>>8)&255)<9 && (c&255)<9;}
    private static boolean blackRow(Bitmap b,int y){for(int i=1;i<64;i++)if(!dark(b.getPixel(i*(b.getWidth()-1)/64,y)))return false;return true;}
    private static boolean blackColumn(Bitmap b,int x){for(int i=1;i<64;i++)if(!dark(b.getPixel(x,i*(b.getHeight()-1)/64)))return false;return true;}
    /** Exact sampled-pixel hash is an optimization only; unchanged frames never count as new proof. */
    public static long fingerprint(Bitmap b){int[] pixels=new int[b.getWidth()*b.getHeight()];b.getPixels(pixels,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());long hash=0xcbf29ce484222325L;for(int pixel:pixels){hash^=pixel;hash*=0x100000001b3L;}return hash;}
}
