package dev.gbppimentel.snapcompanion;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** Code-native plot of measured app observations, styled with the game's typography and palette. */
final class FpsTrace extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final GameAssets art;private String profile;
    FpsTrace(Context c,GameAssets art,String profile){super(c);this.art=art;this.profile=profile;setContentDescription("Recent Eden reported FPS samples; horizontal target at 60 FPS");}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);List<EdenFps.Sample> samples=EdenFps.samples(getContext(),profile);List<EdenFps.Sample> points=new ArrayList<>();for(var s:samples)if(s.fps()!=null)points.add(s);if(points.size()>120)points=points.subList(points.size()-120,points.size());float left=art.dp(28),right=getWidth()-art.dp(8),top=art.dp(10),bottom=getHeight()-art.dp(20),maximum=60;for(var s:points)maximum=Math.max(maximum,s.fps());maximum=(float)Math.ceil(maximum/30)*30;
        int textColor=getContext().getSharedPreferences("snap",0).getBoolean("darkMode",false)?0xFFC0D4D8:0xFF7A6F63;p.setTypeface(art.body);p.setTextSize(art.dp(10));p.setStrokeWidth(art.dp(1));p.setColor(0xFFDBD1B8);for(int value:new int[]{0,30,60}){float y=bottom-(bottom-top)*value/maximum;canvas.drawLine(left,y,right,y,p);p.setColor(textColor);canvas.drawText(Integer.toString(value),0,y+art.dp(3),p);p.setColor(0xFFDBD1B8);}
        if(points.isEmpty()){p.setColor(textColor);canvas.drawText("Waiting for the visible Eden FPS label…",left,top+art.dp(30),p);return;}
        long start=points.get(0).at(),span=Math.max(1,points.get(points.size()-1).at()-start);Path line=new Path();for(int i=0;i<points.size();i++){var s=points.get(i);float x=left+(right-left)*(s.at()-start)/span,y=bottom-(bottom-top)*s.fps()/maximum;if(i==0)line.moveTo(x,y);else line.lineTo(x,y);}p.setColor(0xFFD69A19);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(art.dp(2));canvas.drawPath(line,p);p.setStyle(Paint.Style.FILL);for(var s:points)if(s.fps()<55){float x=left+(right-left)*(s.at()-start)/span,y=bottom-(bottom-top)*s.fps()/maximum;p.setColor(0xFFC3553C);canvas.drawCircle(x,y,art.dp(2.5f),p);}p.setColor(textColor);canvas.drawText("Recent sampled values · target 60",left,getHeight()-art.dp(3),p);
    }
}
