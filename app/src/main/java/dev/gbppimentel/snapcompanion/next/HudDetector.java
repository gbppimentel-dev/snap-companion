package dev.gbppimentel.snapcompanion.next;

import android.graphics.Bitmap;

/** Experimental geometry check for Snap's 16:9 photo stack plus central finder. No species inference. */
public final class HudDetector {
    private static final int[][] STACK={{1712,25},{1725,25},{1740,25},{1705,35},{1705,48},{1715,56},{1730,56},{1748,35},{1748,48},{1755,60},{1761,67}};
    private static final int[][] FINDER={{860,540},{880,540},{900,540},{1025,540},{1045,540},{1060,540},{960,491},{960,590}};
    private static final int[][] CORNERS={{720,375},{740,375},{714,387},{714,405},{1180,375},{1205,390},{714,685},{735,704},{1185,704},{1205,680}};
    public static boolean camera(Bitmap bitmap){
        double aspect=bitmap.getWidth()/(double)bitmap.getHeight();if(Math.abs(aspect-16.0/9)>.08)return false;
        return hit(bitmap,STACK)>=.90 && (hit(bitmap,FINDER)>=.50 || hit(bitmap,CORNERS)>=.60);
    }
    static String evidence(Bitmap bitmap){return "stack="+hit(bitmap,STACK)+" finder="+hit(bitmap,FINDER)+" corners="+hit(bitmap,CORNERS);}
    private static double hit(Bitmap b,int[][] points){int hits=0;for(int[] p:points)if(edge(b,p[0],p[1]))hits++;return hits/(double)points.length;}
    private static boolean edge(Bitmap b,int cx,int cy){
        int x=Math.round(cx*b.getWidth()/1920f),y=Math.round(cy*b.getHeight()/1080f),radius=Math.max(1,b.getWidth()/700);
        int peak=0;for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)peak=Math.max(peak,light(b.getPixel(clamp(x+dx,b.getWidth()),clamp(y+dy,b.getHeight()))));
        int low=255;for(int[] d:new int[][]{{0,-7},{0,7},{-7,0},{7,0}})low=Math.min(low,light(b.getPixel(clamp(x+Math.round(d[0]*b.getWidth()/1920f),b.getWidth()),clamp(y+Math.round(d[1]*b.getHeight()/1080f),b.getHeight()))));
        return peak>=160&&peak-low>=20;
    }
    private static int light(int c){int r=(c>>16)&255,g=(c>>8)&255,b=c&255;return (r*299+g*587+b*114)/1000;}
    private static int clamp(int n,int maximum){return Math.max(0,Math.min(maximum-1,n));}
}
