package dev.gbppimentel.snapcompanion;

import android.graphics.Bitmap;
import java.util.*;

/** Tags OCR labels surrounded by the overview's gold selection paper; never identifies artwork. */
final class PhotoSelectionDetector {
    static List<LiveReading.Word> annotate(Bitmap bitmap,List<LiveReading.Word> words){
        if(!PhotoSelectionText.overviewControls(words))return List.copyOf(words);
        List<LiveReading.Word> out=new ArrayList<>(words.size());
        for(var w:words){
            boolean marked=label(bitmap,w);
            out.add(marked?new LiveReading.Word(w.text(),w.left(),w.top(),w.right(),w.bottom(),w.confidence(),w.filmProbe(),true):w);
        }
        return List.copyOf(out);
    }
    private static boolean label(Bitmap b,LiveReading.Word w){
        double h=w.bottom()-w.top(),width=w.right()-w.left();
        if(!Double.isFinite(h)||!Double.isFinite(width)||h<.008||h>.07||width<=0||width>.30
            ||w.left()<.04||w.right()>.96||w.top()<.10||w.bottom()>.81)return false;
        // Sample outside the text's ink. Both sides and both horizontal margins must agree.
        double dx=h*.18*b.getHeight()/b.getWidth(),dy=h*.18;
        int left=0,right=0,above=0,below=0;
        for(int i=0;i<3;i++){
            double y=w.top()+h*(i+.5)/3;
            if(gold(b,w.left()-dx,y))left++;if(gold(b,w.right()+dx,y))right++;
        }
        for(int i=0;i<5;i++){
            double x=w.left()+width*(i+.5)/5;
            if(gold(b,x,w.top()-dy))above++;if(gold(b,x,w.bottom()+dy))below++;
        }
        return left>=2&&right>=2&&above>=3&&below>=3&&left+right+above+below>=12;
    }
    private static boolean gold(Bitmap b,double x,double y){
        if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>=1||y<0||y>=1)return false;
        int c=b.getPixel((int)(x*b.getWidth()),(int)(y*b.getHeight())),r=(c>>16)&255,g=(c>>8)&255,blue=c&255;
        return r>=170&&g>=95&&r>=g*.96&&r<=g*2.1&&blue<g*.62&&g-blue>=55;
    }
}
