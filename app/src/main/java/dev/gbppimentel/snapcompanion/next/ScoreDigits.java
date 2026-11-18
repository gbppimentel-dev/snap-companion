package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.*;

/** Independent fixed-font digit matcher using an upstream CC0 training image, not scanner code. */
final class ScoreDigits {
    record Reading(String text,float similarity,float margin){}
    private record Glyph(char value,boolean[] pixels,double aspect){}
    private record Component(int left,int top,int right,int bottom){}
    private final List<Glyph> templates=new ArrayList<>();
    ScoreDigits(Context context)throws IOException{try(InputStream in=context.getAssets().open("recognition/score-digits.png")){Bitmap bitmap=BitmapFactory.decodeStream(in);if(bitmap==null)throw new IOException("Digit image is unavailable");try{int[] pixels=pixels(bitmap);boolean[] mask=new boolean[pixels.length];for(int i=0;i<mask.length;i++)mask[i]=luma(pixels[i])<128;for(int digit=0;digit<11;digit++){int top=digit*bitmap.getHeight()/12,bottom=(digit+1)*bitmap.getHeight()/12;for(Component c:components(mask,bitmap.getWidth(),bitmap.getHeight(),0,top,bitmap.getWidth(),bottom))templates.add(new Glyph(digit==10?',':(char)('0'+digit),normalize(mask,bitmap.getWidth(),c),aspect(c)));}}finally{bitmap.recycle();}}if(templates.size()<20)throw new IOException("Digit templates failed validation");}
    Reading read(Bitmap bitmap,Rect box,int threshold){
        if(box.left<0||box.top<0||box.right>bitmap.getWidth()||box.bottom>bitmap.getHeight()||box.isEmpty())return null;int width=box.width(),height=box.height();int[] pixels=new int[width*height];bitmap.getPixels(pixels,0,width,box.left,box.top,width,height);boolean[] mask=new boolean[pixels.length];for(int i=0;i<mask.length;i++)mask[i]=luma(pixels[i])>=threshold;
        List<Component> parts=components(mask,width,height,0,0,width,height);if(parts.isEmpty()||parts.size()>8)return null;StringBuilder text=new StringBuilder();float similarity=1,margin=1;int maxHeight=parts.stream().mapToInt(c->c.bottom()-c.top()).max().orElse(0);
        for(Component c:parts){if(c.bottom()-c.top()<maxHeight*.52){if(c.bottom()<height*.45)return null;text.append(',');continue;}boolean[] sample=normalize(mask,width,c);Map<Character,Double> best=new HashMap<>();for(Glyph g:templates){if(g.value()==',')continue;int overlap=0,total=0;for(int i=0;i<sample.length;i++){if(sample[i])total++;if(g.pixels()[i])total++;if(sample[i]&&g.pixels()[i])overlap++;}double distance=1-2.0*overlap/Math.max(1,total)+.15*Math.abs(Math.log(aspect(c)/g.aspect()));best.merge(g.value(),distance,Math::min);}List<Map.Entry<Character,Double>> sorted=new ArrayList<>(best.entrySet());sorted.sort(Map.Entry.comparingByValue());double score=sorted.get(0).getValue(),gap=sorted.get(1).getValue()-score;similarity=Math.min(similarity,(float)(1-score));margin=Math.min(margin,(float)gap);text.append(sorted.get(0).getKey());}
        String value=text.toString();if(!value.matches("[0-9]{1,3}(?:,[0-9]{3})?|[0-9]{1,6}"))return null;return new Reading(value,similarity,margin);
    }
    Reading paired(Bitmap bitmap,Rect box){Reading a=read(bitmap,box,120),b=read(bitmap,box,145);return a!=null&&b!=null&&a.text().replace(",","").equals(b.text().replace(",",""))?new Reading(a.text(),Math.min(a.similarity(),b.similarity()),Math.min(a.margin(),b.margin())):null;}
    private static int[] pixels(Bitmap b){int[] out=new int[b.getWidth()*b.getHeight()];b.getPixels(out,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return out;}
    private static int luma(int c){return(((c>>16)&255)*30+((c>>8)&255)*59+(c&255)*11)/100;}
    private static double aspect(Component c){return(c.right()-c.left())/(double)Math.max(1,c.bottom()-c.top());}
    private static List<Component> components(boolean[] mask,int width,int height,int left,int top,int right,int bottom){List<Component> out=new ArrayList<>();int start=-1;for(int x=left;x<=right;x++){boolean column=false;if(x<right)for(int y=top;y<bottom;y++)if(mask[y*width+x]){column=true;break;}if(column&&start<0)start=x;if(!column&&start>=0){int min=bottom,max=top,count=0;for(int xx=start;xx<x;xx++)for(int y=top;y<bottom;y++)if(mask[y*width+xx]){min=Math.min(min,y);max=Math.max(max,y);count++;}if(count>=3)out.add(new Component(start,min,x,max+1));start=-1;}}return out;}
    private static boolean[] normalize(boolean[] mask,int width,Component c){boolean[] out=new boolean[24*36];int w=c.right()-c.left(),h=c.bottom()-c.top();for(int y=0;y<36;y++)for(int x=0;x<24;x++){int xx=c.left()+Math.min(w-1,(int)((x+.5)*w/24)),yy=c.top()+Math.min(h-1,(int)((y+.5)*h/36));out[y*24+x]=mask[yy*width+xx];}return out;}
}
