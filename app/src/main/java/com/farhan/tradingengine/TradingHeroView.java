package com.farhan.tradingengine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

public final class TradingHeroView extends View {
    private final Paint p = new Paint(3);
    private final int green = Color.rgb(0, 235, 125);
    private final int red = Color.rgb(255, 65, 82);
    public TradingHeroView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        p.setShader(null); p.setColor(color); p.setTextSize(size);
        p.setTypeface(bold ? android.graphics.Typeface.create("sans-serif", 1) : android.graphics.Typeface.create("sans-serif", 0));
        c.drawText(s, x, y, p);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        p.setShader(new LinearGradient(0,0,w,h, new int[]{Color.rgb(2,10,22),Color.rgb(0,28,48),Color.rgb(3,5,13)},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(0,0,w,h,22,22,p); p.setShader(null);
        p.setColor(Color.argb(35,40,160,210)); p.setStrokeWidth(1);
        for(int i=1;i<6;i++) c.drawLine(0,h*i/6f,w,h*i/6f,p);
        for(int i=1;i<8;i++) c.drawLine(w*i/8f,0,w*i/8f,h,p);
        float[] vals={.72f,.66f,.69f,.55f,.59f,.45f,.49f,.36f,.41f,.25f,.31f,.18f,.22f,.12f};
        float base=h*.78f, step=w*.047f, start=w*.34f;
        for(int i=0;i<vals.length;i++){
            float x=start+i*step, y=vals[i]*h;
            float top=Math.min(y,base), bottom=Math.max(y,base);
            int col=(i==2||i==5||i==8)?red:green;
            p.setColor(col); p.setStrokeWidth(2.2f); c.drawLine(x,top-9,x,bottom+8,p);
            c.drawRoundRect(x-3,top,x+3,bottom,2,2,p);
            base += (i%3==0?-h*.018f:h*.006f);
        }
        // dark trader silhouette and monitor shapes
        p.setColor(Color.argb(220,2,7,16)); c.drawRoundRect(w*.53f,h*.56f,w*.98f,h*.94f,26,26,p);
        p.setColor(Color.rgb(3,17,31)); c.drawCircle(w*.76f,h*.52f,h*.20f,p);
        p.setColor(Color.argb(230,0,5,12));
        Path hood=new Path(); hood.moveTo(w*.60f,h*.78f); hood.quadTo(w*.57f,h*.42f,w*.76f,h*.37f);
        hood.quadTo(w*.96f,h*.42f,w*.93f,h*.78f); hood.quadTo(w*.82f,h*.89f,w*.60f,h*.78f); c.drawPath(hood,p);
        p.setColor(Color.argb(210,0,35,53)); c.drawRoundRect(w*.08f,h*.59f,w*.48f,h*.84f,8,8,p);
        p.setColor(Color.argb(160,0,200,255)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.2f);
        c.drawRoundRect(w*.08f,h*.59f,w*.48f,h*.84f,8,8,p); p.setStyle(Paint.Style.FILL);
        text(c,"FARHAN",w*.07f,h*.22f,h*.105f,Color.WHITE,true);
        text(c,"TRADING ENGINE",w*.07f,h*.34f,h*.072f,green,true);
        text(c,"ANALYZE  •  PREDICT  •  TRADE",w*.07f,h*.45f,h*.035f,Color.LTGRAY,false);
        p.setColor(Color.argb(190,0,235,125)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
        c.drawRoundRect(w*.06f,h*.88f,w*.94f,h*.975f,18,18,p); p.setStyle(Paint.Style.FILL);
        text(c,"AI-POWERED CHART ANALYSIS",w*.10f,h*.94f,h*.042f,Color.WHITE,true);
    }
}
