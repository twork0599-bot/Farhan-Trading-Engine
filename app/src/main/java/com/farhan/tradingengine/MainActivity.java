package com.farhan.tradingengine;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private TextView status, signal, details;
    private EditText candles;
    @Override protected void onCreate(Bundle b){super.onCreate(b); buildScreen();}
    private TextView label(String value,int size,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(4,12,4,12);return t;
    }
    private void buildScreen(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,24,20,24);root.setBackgroundColor(Color.rgb(12,18,30));scroll.addView(root);
        root.addView(label("FARHAN TRADING ENGINE",22,Color.WHITE));
        signal=label("WAIT",40,Color.rgb(255,193,7));root.addView(signal);
        root.addView(label("AUTO STRATEGY MODE • BINOLLA • 1 MIN",12,Color.LTGRAY));
        TextView info=label("Paste candle OHLC data below (oldest first). One candle per line: open,high,low,close. The app currently analyzes entered data; it does not read the Binolla screen automatically yet.",14,Color.LTGRAY);root.addView(info);
        candles=new EditText(this);candles.setHint("open,high,low,close\n1.0820,1.0830,1.0810,1.0825");candles.setTextColor(Color.WHITE);candles.setHintTextColor(Color.GRAY);candles.setGravity(Gravity.TOP|Gravity.START);candles.setMinLines(8);candles.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);candles.setPadding(12,12,12,12);candles.setBackgroundColor(Color.rgb(28,38,55));root.addView(candles);
        Button analyze=new Button(this);analyze.setText("ANALYZE CANDLES");analyze.setOnClickListener(v->runAnalysis());root.addView(analyze);
        details=label("Strategy votes and indicator readings will appear here.",14,Color.LTGRAY);root.addView(details);
        Button overlay=new Button(this);overlay.setText("ENABLE FLOATING WINDOW PERMISSION");overlay.setOnClickListener(v->{Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);});root.addView(overlay);
        status=label("Status: analysis engine ready • screen recognition not connected",12,Color.rgb(160,190,220));root.addView(status);
        root.addView(label("Signal-only • No auto-trading • No login • No guaranteed outcomes",11,Color.GRAY));
        setContentView(scroll);
    }
    private void runAnalysis(){
        try{
            List<SignalEngine.Candle> data=SignalEngine.parse(candles.getText().toString());
            SignalEngine.Result r=SignalEngine.analyze(data);
            signal.setText(r.direction);signal.setTextColor("UP".equals(r.direction)?Color.rgb(50,220,130):"DOWN".equals(r.direction)?Color.rgb(255,85,85):Color.rgb(255,193,7));
            details.setText(r.reason+"\nRSI(14): "+String.format(Locale.US,"%.1f",r.rsi)+"\nEMA 9: "+r.emaFast+"\nEMA 20: "+r.emaSlow+"\nCandles analyzed: "+data.size());
            status.setText("Status: analysis complete • votes are rule-based, not a proven win rate");
        }catch(Exception e){signal.setText("WAIT");signal.setTextColor(Color.rgb(255,193,7));details.setText(e.getMessage());status.setText("Status: need valid candle data");}
    }
}