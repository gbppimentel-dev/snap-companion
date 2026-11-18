package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import android.graphics.*;
import java.util.*;

/** Experimental English comparison-screen adapter; paired digit masks and exact component sums. */
final class EvaluationReader {
    record Reading(long at,String profile,int subject,int previousTotal,int total,List<Integer> previous,List<Integer> current,float minimumSimilarity,float minimumMargin){
        Reading{previous=List.copyOf(previous);current=List.copyOf(current);if(previous.size()!=6||current.size()!=6||previousTotal<1||total<1||previous.stream().anyMatch(v->v< -1||v>99999)||current.stream().anyMatch(v->v< -1||v>99999)||previous.stream().filter(v->v>=0).mapToInt(Integer::intValue).sum()!=previousTotal||current.stream().filter(v->v>=0).mapToInt(Integer::intValue).sum()!=total)throw new IllegalArgumentException("Comparison component totals do not match");}
        String signature(){return profile+":"+subject+":"+previousTotal+":"+total+":"+previous+":"+current;}
    }
    static volatile Reading latest;
    private final ScoreDigits digits;
    EvaluationReader(Context context)throws java.io.IOException{digits=new ScoreDigits(context);}
    Reading read(Bitmap bitmap,long at,String profile,int subject){
        if(Math.abs(bitmap.getWidth()/(double)bitmap.getHeight()-16.0/9)>.03)return null;
        List<ScoreDigits.Reading> recognized=new ArrayList<>();Integer old=number(bitmap,.29,.68,.43,.77,recognized),total=number(bitmap,.57,.68,.72,.77,recognized);if(old==null||total==null||total<1||old<1)return null;
        double[] centers={.272,.337,.401,.466,.531,.596};List<Integer> previous=new ArrayList<>(),current=new ArrayList<>();
        for(double y:centers){Rect before=box(bitmap,.335,y-.023,.398,y+.023),after=box(bitmap,.595,y-.023,.675,y+.023);Integer a=number(bitmap,before,recognized),b=number(bitmap,after,recognized);if(a==null&&dash(bitmap,before))a=-1;if(b==null&&dash(bitmap,after))b=-1;if(a==null||b==null)return null;previous.add(a);current.add(b);}
        if(previous.stream().filter(n->n>=0).mapToInt(Integer::intValue).sum()!=old||current.stream().filter(n->n>=0).mapToInt(Integer::intValue).sum()!=total)return null;
        float similarity=1,margin=1;for(ScoreDigits.Reading r:recognized){similarity=Math.min(similarity,r.similarity());margin=Math.min(margin,r.margin());}return new Reading(at,profile,subject,old,total,List.copyOf(previous),List.copyOf(current),similarity,margin);
    }
    private Integer number(Bitmap b,double l,double t,double r,double bottom,List<ScoreDigits.Reading> recognized){return number(b,box(b,l,t,r,bottom),recognized);}
    private Integer number(Bitmap b,Rect box,List<ScoreDigits.Reading> recognized){ScoreDigits.Reading reading=digits.paired(b,box);if(reading==null||reading.similarity()<.68f||reading.margin()<.025f)return null;try{int value=Integer.parseInt(reading.text().replace(",",""));if(value<0||value>99999)return null;recognized.add(reading);return value;}catch(NumberFormatException e){return null;}}
    private static Rect box(Bitmap b,double l,double t,double r,double bottom){return new Rect((int)(l*b.getWidth()),(int)(t*b.getHeight()),(int)(r*b.getWidth()),(int)(bottom*b.getHeight()));}
    private static boolean dash(Bitmap b,Rect box){for(int threshold:new int[]{120,165}){int minX=box.right,minY=box.bottom,maxX=-1,maxY=-1,count=0;for(int y=box.top;y<box.bottom;y++)for(int x=box.left;x<box.right;x++){int c=b.getPixel(x,y);int luma=(((c>>16)&255)*30+((c>>8)&255)*59+(c&255)*11)/100;if(luma>=threshold){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);count++;}}int w=maxX-minX+1,h=maxY-minY+1;if(count<3||h<1||h>box.height()*.25||w<2*h)return false;}return true;}
    static boolean anchors(LiveReading reading,List<LiveReading.Word> words){if(reading.scene()!=LiveReading.Scene.EVALUATION)return false;String text=LiveParser.normalize(words.stream().map(LiveReading.Word::text).reduce("",(a,b)->a+" "+b)).replace(" ","");return text.contains("photodexphoto")&&text.contains("newphoto");}
}
