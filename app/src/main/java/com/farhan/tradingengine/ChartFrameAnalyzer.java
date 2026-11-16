package com.farhan.tradingengine;

import android.graphics.Bitmap;
import java.util.*;

/**
 * Lightweight on-device chart reader.
 * It detects green/red candle-shaped pixel groups in a central chart region.
 * Values are relative pixel prices, not broker OHLC prices.
 */
public final class ChartFrameAnalyzer {
    public static final class FrameResult {
        public final List<SignalEngine.Candle> candles;
        public final String message;
        FrameResult(List<SignalEngine.Candle> c,String m){candles=c;message=m;}
    }
    private static final class Column {
        int x, top=Integer.MAX_VALUE, bottom=Integer.MIN_VALUE, green=0, red=0;
        int[] rows;
        Column(int x,int h){this.x=x;rows=new int[h];}
    }
    private ChartFrameAnalyzer(){}

    public static FrameResult analyze(Bitmap bitmap) {
        if(bitmap==null || bitmap.getWidth()<200 || bitmap.getHeight()<200)
            return new FrameResult(Collections.emptyList(),"Screen frame too small");
        int w=bitmap.getWidth(), h=bitmap.getHeight();
        int left=(int)(w*0.06), right=(int)(w*0.88);
        int top=(int)(h*0.16), bottom=(int)(h*0.78);
        int rh=bottom-top;
        ArrayList<Column> active=new ArrayList<>();
        for(int x=left;x<right;x++){
            Column col=new Column(x,rh);
            for(int y=top;y<bottom;y++){
                int p=bitmap.getPixel(x,y);
                int r=(p>>16)&255,g=(p>>8)&255,b=p&255;
                boolean green=(g>105 && g>r*1.22 && g>b*1.08 && g-r>35);
                boolean red=(r>125 && r>g*1.28 && r>b*1.18 && r-g>45);
                if(green||red){
                    col.rows[y-top]++;
                    col.top=Math.min(col.top,y-top);
                    col.bottom=Math.max(col.bottom,y-top);
                    if(green)col.green++; else col.red++;
                }
            }
            if(col.green+col.red>=4) active.add(col);
        }
        if(active.isEmpty()) return new FrameResult(Collections.emptyList(),"No clear green/red candle pixels found");

        ArrayList<ArrayList<Column>> groups=new ArrayList<>();
        ArrayList<Column> current=null; int lastX=-99;
        for(Column c:active){
            if(current==null || c.x-lastX>3){
                current=new ArrayList<>();groups.add(current);
            }
            current.add(c);lastX=c.x;
        }

        ArrayList<SignalEngine.Candle> out=new ArrayList<>();
        for(ArrayList<Column> group:groups){
            int count=0, green=0, red=0, min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            int[] rowCounts=new int[rh];
            for(Column c:group){
                count+=c.green+c.red;green+=c.green;red+=c.red;
                min=Math.min(min,c.top);max=Math.max(max,c.bottom);
                for(int y=0;y<rh;y++)rowCounts[y]+=c.rows[y];
            }
            int groupWidth=group.size();
            if(groupWidth>18 || count<6 || max-min<4 || Math.max(green,red)<count*0.68) continue;
            int peak=0;
            for(int v:rowCounts)peak=Math.max(peak,v);
            int bodyThreshold=Math.max(2,(int)Math.ceil(groupWidth*0.45));
            int bodyTop=-1,bodyBottom=-1;
            for(int y=0;y<rh;y++) if(rowCounts[y]>=bodyThreshold){
                if(bodyTop<0)bodyTop=y;bodyBottom=y;
            }
            if(bodyTop<0 || bodyBottom-bodyTop<2){bodyTop=min;bodyBottom=max;}
            boolean bullish=green>red;
            double hi= h-top-min, lo=h-top-max;
            double open=bullish ? h-top-bodyBottom : h-top-bodyTop;
            double close=bullish ? h-top-bodyTop : h-top-bodyBottom;
            if(hi<=lo || open<lo || open>hi || close<lo || close>hi) continue;
            out.add(new SignalEngine.Candle(open,hi,lo,close));
        }
        if(out.size()<6)
            return new FrameResult(out,"Only "+out.size()+" candles detected; need at least 6 clear candles.");
        // A phone screen may include non-chart colored UI; retain the most recent 80 groups.
        if(out.size()>80)out=new ArrayList<>(out.subList(out.size()-80,out.size()));
        return new FrameResult(out,"Detected "+out.size()+" candle groups (pixel-derived estimates)");
    }
}