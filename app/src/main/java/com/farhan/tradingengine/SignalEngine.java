package com.farhan.tradingengine;

import java.util.*;

public final class SignalEngine {
    public static final class Candle {
        public final double open, high, low, close;
        Candle(double o,double h,double l,double c){open=o;high=h;low=l;close=c;}
    }
    public static final class Result {
        public final String direction, reason;
        public final int upVotes, downVotes;
        public final double rsi, emaFast, emaSlow;
        Result(String d,String r,int u,int dn,double rs,double ef,double es){
            direction=d;reason=r;upVotes=u;downVotes=dn;rsi=rs;emaFast=ef;emaSlow=es;
        }
    }
    private SignalEngine(){}
    public static List<Candle> parse(String csv) {
        List<Candle> out=new ArrayList<>();
        for(String line:csv.split("\\r?\\n")){
            String s=line.trim(); if(s.isEmpty() || s.toLowerCase(Locale.US).startsWith("open")) continue;
            String[] p=s.split("[,;\\s]+"); if(p.length<4) throw new IllegalArgumentException("Each row must be: open,high,low,close");
            try {
                double o=Double.parseDouble(p[0]),h=Double.parseDouble(p[1]),l=Double.parseDouble(p[2]),c=Double.parseDouble(p[3]);
                if(!Double.isFinite(o)||!Double.isFinite(h)||!Double.isFinite(l)||!Double.isFinite(c)||h<l||h<Math.max(o,c)||l>Math.min(o,c))
                    throw new IllegalArgumentException("Invalid OHLC values in row: "+s);
                out.add(new Candle(o,h,l,c));
            } catch(NumberFormatException e){throw new IllegalArgumentException("Invalid number in row: "+s);}
        }
        return out;
    }
    private static double ema(List<Candle> a,int period){
        double k=2.0/(period+1),v=a.get(0).close;
        for(int i=1;i<a.size();i++) v=a.get(i).close*k+v*(1-k);
        return v;
    }
    private static double rsi(List<Candle>a,int period){
        double gain=0,loss=0;
        for(int i=a.size()-period;i<a.size();i++){double d=a.get(i).close-a.get(i-1).close;if(d>0)gain+=d;else loss-=d;}
        if(loss==0)return gain==0?50:100;
        return 100-(100/(1+(gain/period)/(loss/period)));
    }
    public static Result analyze(List<Candle> a){
        if(a.size()<8) throw new IllegalArgumentException("At least 8 visible candles are needed.");
        int n=a.size(),up=0,down=0;
        int fastPeriod=Math.min(3,n-1), slowPeriod=Math.min(6,n);
        double e9=ema(a,fastPeriod),e20=ema(a,slowPeriod),e50=ema(a,Math.min(9,n));
        double rs=rsi(a,Math.min(7,n-1));
        // MACD 12/26 with signal line from historical prefixes.
        double macdNow=ema(a,Math.min(4,n-1))-ema(a,Math.min(8,n));
        List<Candle> prev=new ArrayList<>(a.subList(0,n-1));
        double macdPrev=ema(prev,Math.min(4,prev.size()))-ema(prev,Math.min(8,prev.size()));
        if(e9>e20 && e20>e50)up++; else if(e9<e20 && e20<e50)down++;
        if(macdNow>macdPrev && macdNow>0)up++; else if(macdNow<macdPrev && macdNow<0)down++;
        if(rs>52 && rs<70)up++; else if(rs<48 && rs>30)down++;
        double mean=0;for(Candle c:a.subList(Math.max(0,n-8),n))mean+=c.close;mean/=Math.min(8,n);
        double variance=0;for(Candle c:a.subList(n-20,n))variance+=Math.pow(c.close-mean,2);double sd=Math.sqrt(variance/Math.min(8,n));
        Candle last=a.get(n-1),prior=a.get(n-2);
        if(last.close>mean+2*sd && last.close>prior.close)up++;
        else if(last.close<mean-2*sd && last.close<prior.close)down++;
        // Candle body direction and close location.
        double range=last.high-last.low, body=last.close-last.open;
        if(range>0 && body>0 && last.close>=last.low+range*0.7)up++;
        else if(range>0 && body<0 && last.close<=last.low+range*0.3)down++;
        // 20-candle breakout confirmation.
        double hh=Double.NEGATIVE_INFINITY,ll=Double.POSITIVE_INFINITY;
        for(Candle c:a.subList(Math.max(0,n-8),n-1)){hh=Math.max(hh,c.high);ll=Math.min(ll,c.low);}
        if(last.close>hh)up++; else if(last.close<ll)down++;
        String dir="WAIT",why="Mixed or weak confirmation. Wait for a clearer setup.";
        if(up>=4 && up-down>=3){dir="UP";why="Bullish confluence: "+up+" strategy votes; bearish votes: "+down+".";}
        else if(down>=4 && down-up>=3){dir="DOWN";why="Bearish confluence: "+down+" strategy votes; bullish votes: "+up+".";}
        else {why="No strong agreement (UP "+up+"/6, DOWN "+down+"/6). WAIT is safer.";}
        return new Result(dir,why,up,down,rs,e9,e20);
    }
}