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
    private static double avgRange(List<Candle> a,int from,int to){
        double sum=0; for(int i=from;i<to;i++) sum+=Math.max(0.0001,a.get(i).high-a.get(i).low);
        return sum/Math.max(1,to-from);
    }
    private static double slope(List<Candle> a,boolean lows){
        int n=a.size(),from=Math.max(0,n-6); double sx=0,sy=0,sxx=0,sxy=0; int count=n-from;
        for(int i=0;i<count;i++){double x=i,y=lows?a.get(from+i).low:a.get(from+i).high;sx+=x;sy+=y;sxx+=x*x;sxy+=x*y;}
        double den=count*sxx-sx*sx; return den==0?0:(count*sxy-sx*sy)/den;
    }
    public static Result analyze(List<Candle> a){
        if(a.size()<6) throw new IllegalArgumentException("At least 6 visible candles are needed.");
        int n=a.size(),up=0,down=0;
        int fastPeriod=Math.min(3,n-1), slowPeriod=Math.min(5,n);
        double fast=ema(a,fastPeriod),slow=ema(a,slowPeriod);
        double rs=rsi(a,Math.min(5,n-1));
        List<Candle> prev=new ArrayList<>(a.subList(0,n-1));
        double macdNow=ema(a,Math.min(3,n-1))-ema(a,Math.min(5,n));
        double macdPrev=ema(prev,Math.min(3,prev.size()))-ema(prev,Math.min(5,prev.size()));
        if(fast>slow)up++; else if(fast<slow)down++;
        if(macdNow>macdPrev && macdNow>0)up++; else if(macdNow<macdPrev && macdNow<0)down++;
        if(rs>53 && rs<75)up++; else if(rs<47 && rs>25)down++;

        int from=Math.max(0,n-6);
        double mean=0; for(int i=from;i<n;i++)mean+=a.get(i).close; mean/=n-from;
        double variance=0; for(int i=from;i<n;i++)variance+=Math.pow(a.get(i).close-mean,2);
        double sd=Math.sqrt(variance/(n-from));
        Candle last=a.get(n-1),prior=a.get(n-2);
        if(last.close>mean+1.4*sd && last.close>prior.close)up++;
        else if(last.close<mean-1.4*sd && last.close<prior.close)down++;

        double range=Math.max(0.0001,last.high-last.low),body=last.close-last.open;
        if(body>0 && last.close>=last.low+range*0.68)up++;
        else if(body<0 && last.close<=last.low+range*0.32)down++;

        double hh=Double.NEGATIVE_INFINITY,ll=Double.POSITIVE_INFINITY;
        for(int i=from;i<n-1;i++){hh=Math.max(hh,a.get(i).high);ll=Math.min(ll,a.get(i).low);}
        if(last.close>hh)up++; else if(last.close<ll)down++;

        // Nearby support/resistance touch and rejection, using recent visible swing levels.
        double support=Double.POSITIVE_INFINITY,resistance=Double.NEGATIVE_INFINITY;
        for(int i=from;i<n-1;i++){support=Math.min(support,a.get(i).low);resistance=Math.max(resistance,a.get(i).high);}
        double tolerance=Math.max(0.0001,avgRange(a,from,n)*0.22);
        double lowerWick=Math.min(last.open,last.close)-last.low;
        double upperWick=last.high-Math.max(last.open,last.close);
        if(Math.abs(last.low-support)<=tolerance && lowerWick>Math.abs(body)*0.7)up++;
        else if(Math.abs(last.high-resistance)<=tolerance && upperWick>Math.abs(body)*0.7)down++;

        // Short trendline direction from recent swing lows/highs.
        double lowSlope=slope(a,true),highSlope=slope(a,false);
        if(lowSlope>tolerance*0.08 && highSlope>0)up++;
        else if(lowSlope< -tolerance*0.08 && highSlope<0)down++;

        // Basic candle patterns: engulfing and hammer / shooting-star rejection.
        Candle p=prior;
        double pBody=Math.abs(p.close-p.open), currBody=Math.abs(body);
        boolean bullEngulf=p.close<p.open && last.close>last.open && last.open<=p.close && last.close>=p.open && currBody>pBody*0.8;
        boolean bearEngulf=p.close>p.open && last.close<last.open && last.open>=p.close && last.close<=p.open && currBody>pBody*0.8;
        boolean hammer=lowerWick>Math.max(currBody*1.8,range*0.35) && upperWick<range*0.25 && last.close>last.low+range*0.55;
        boolean star=upperWick>Math.max(currBody*1.8,range*0.35) && lowerWick<range*0.25 && last.close<last.low+range*0.45;
        if(bullEngulf||hammer)up++;
        else if(bearEngulf||star)down++;

        String dir="WAIT",why="Mixed or weak confirmation. Wait for a clearer setup.";
        if(up>=5 && up-down>=3){dir="UP";why="Bullish confluence: "+up+" votes; bearish votes: "+down+". Includes trend, support/resistance and candle patterns.";}
        else if(down>=5 && down-up>=3){dir="DOWN";why="Bearish confluence: "+down+" votes; bullish votes: "+up+". Includes trend, support/resistance and candle patterns.";}
        else {why="No strong agreement (UP "+up+"/9, DOWN "+down+"/9). WAIT is safer.";}
        return new Result(dir,why,up,down,rs,fast,slow);
    }
}