package com.farhan.tradingengine;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST=701;
    private TextView status,signal,details;\n    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private EditText candles;
    @Override protected void onCreate(Bundle b){super.onCreate(b);buildScreen();}
    private TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(4,12,4,12);return t;}
    private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->action.run());return b;}
    private void buildScreen(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,24,20,24);root.setBackgroundColor(Color.rgb(12,18,30));scroll.addView(root);
        root.addView(new TradingHeroView(this),new LinearLayout.LayoutParams(-1,dp(235)));\n        root.addView(label("FARHAN TRADING ENGINE",20,Color.WHITE));
        signal=label("WAIT",40,Color.YELLOW);root.addView(signal);
        root.addView(label("AUTO STRATEGY MODE • BINOLLA • 1 MIN",12,Color.LTGRAY));
        root.addView(label("AUTO SCAN: Start screen capture, then open Binolla 1-minute chart. The app scans visible green/red candle pixels on-device. If the chart is unclear or fewer than 6 candles are found, it shows WAIT. Manual OHLC entry is also available for testing.",13,Color.LTGRAY));
        candles=new EditText(this);candles.setHint("open,high,low,close\n1.0820,1.0830,1.0810,1.0825");candles.setTextColor(Color.WHITE);candles.setHintTextColor(Color.GRAY);candles.setGravity(Gravity.TOP|Gravity.START);candles.setMinLines(8);candles.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);candles.setPadding(12,12,12,12);candles.setBackgroundColor(Color.rgb(28,38,55));root.addView(candles);
        root.addView(button("ANALYZE CANDLES",this::runAnalysis));
        details=label("Analysis: short EMA trend, MACD-style momentum, RSI, volatility bands, breakout, support/resistance zones, short trendlines and candle patterns (engulfing, hammer, shooting star). Needs 6 visible candles; chart prices are pixel estimates and must be tested.",13,Color.LTGRAY);root.addView(details);
        root.addView(button("1. ALLOW FLOATING WINDOW",()->{Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}));
        root.addView(button("2. START BINOLLA SCREEN CAPTURE + FLOATING PANEL",this::requestCapture));
        root.addView(button("STOP FLOATING PANEL",()->stopService(new Intent(this,CaptureOverlayService.class).setAction(CaptureOverlayService.ACTION_STOP))));
        status=label("Ready • no automatic trade execution",12,Color.rgb(160,190,220));root.addView(status);
        root.addView(label("Signal-only • no account login • no trade-button automation • no guaranteed outcomes",11,Color.GRAY));setContentView(scroll);
    }
    private void requestCapture(){
        if(!Settings.canDrawOverlays(this)){status.setText("First allow floating window permission.");return;}
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(),CAPTURE_REQUEST);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CAPTURE_REQUEST&&result==RESULT_OK&&data!=null){
            Intent s=new Intent(this,CaptureOverlayService.class);s.setAction(CaptureOverlayService.ACTION_START);s.putExtra(CaptureOverlayService.EXTRA_RESULT,result);s.putExtra(CaptureOverlayService.EXTRA_DATA,data);
            if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);
            status.setText("Screen capture started. Open Binolla and keep its 1-minute chart visible; on-device scan is active.");
        }else if(request==CAPTURE_REQUEST)status.setText("Screen capture permission was not granted.");
    }
    private void runAnalysis(){
        try{
            List<SignalEngine.Candle> data=SignalEngine.parse(candles.getText().toString());SignalEngine.Result r=SignalEngine.analyze(data);
            signal.setText(r.direction);signal.setTextColor("UP".equals(r.direction)?Color.rgb(50,220,130):"DOWN".equals(r.direction)?Color.rgb(255,85,85):Color.YELLOW);
            details.setText(r.reason+"\nRSI (short lookback): "+String.format(Locale.US,"%.1f",r.rsi)+"\nFast EMA: "+r.emaFast+"\nSlow EMA: "+r.emaSlow+"\nVotes UP/DOWN: "+r.upVotes+"/"+r.downVotes+"\nCandles: "+data.size());
            getSharedPreferences("engine",MODE_PRIVATE).edit().putString("direction",r.direction).apply();
            status.setText("Analysis complete • floating panel refreshes the latest saved signal and 1-minute countdown.");
        }catch(Exception e){signal.setText("WAIT");signal.setTextColor(Color.YELLOW);details.setText(e.getMessage());getSharedPreferences("engine",MODE_PRIVATE).edit().putString("direction","WAIT").apply();status.setText("Need valid candle data");}
    }
}