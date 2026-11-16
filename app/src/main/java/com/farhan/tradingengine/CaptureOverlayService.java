package com.farhan.tradingengine;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import java.nio.ByteBuffer;

public class CaptureOverlayService extends Service {
    public static final String ACTION_START="com.farhan.tradingengine.START_CAPTURE";
    public static final String ACTION_STOP="com.farhan.tradingengine.STOP_CAPTURE";
    public static final String EXTRA_RESULT="resultCode";
    public static final String EXTRA_DATA="projectionData";
    private static final String CHANNEL="farhan_capture";
    private static final long SIGNAL_WINDOW_MS=4000L;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private WindowManager windowManager;
    private TextView chip;
    private Handler handler;
    private long lastFrameAt=0;
    private long committedMinute=-1;
    private String latestDirection="WAIT";
    private String latestReason="Collecting chart data...";
    private int latestUpVotes=0,latestDownVotes=0;
    private boolean hasFreshAnalysis=false;

    private final Runnable refresh=new Runnable(){ public void run(){
        if(chip!=null){
            long now=System.currentTimeMillis();
            long minute=now/60000L;
            long msToBoundary=60000L-(now%60000L);
            long targetMinute=minute;
            String d="WAIT";
            String detail="Analyzing current candle...";
            long savedMinute=getSharedPreferences("engine",MODE_PRIVATE).getLong("auto_signal_minute",-1L);
            String savedDirection=getSharedPreferences("engine",MODE_PRIVATE).getString("auto_direction","WAIT");
            String savedReason=getSharedPreferences("engine",MODE_PRIVATE).getString("auto_frame_status","Waiting for chart analysis");
            // Show the next-candle call immediately in the final four seconds,
            // then keep that exact call throughout the target candle.
            if(savedMinute==minute || (savedMinute==minute+1L && msToBoundary<=SIGNAL_WINDOW_MS)){
                targetMinute=savedMinute; d=savedDirection; detail=savedReason;
            }
            long secondsLeft=(msToBoundary+999L)/1000L;
            String headline="BUY".equals(d)?"BUY ↑":"SELL".equals(d)?"SELL ↓":"WAIT";
            chip.setText(headline+"  •  "+secondsLeft+"s\n"+detail);
            chip.setTextColor("BUY".equals(d)?Color.rgb(80,255,150):"SELL".equals(d)?Color.rgb(255,100,100):Color.YELLOW);
        }
        if(handler!=null)handler.postDelayed(this,250);
    }};

    @Override public void onCreate(){super.onCreate();handler=new Handler(Looper.getMainLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        if(ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        createChannel();
        startForeground(7,new Notification.Builder(this,CHANNEL).setContentTitle("Farhan Trading Engine").setContentText("Continuous chart analysis • signal-only").setSmallIcon(android.R.drawable.ic_menu_view).build());
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        showOverlay();
        int code=intent.getIntExtra(EXTRA_RESULT,Activity.RESULT_CANCELED);
        Intent data=intent.getParcelableExtra(EXTRA_DATA);
        if(code==Activity.RESULT_OK && data!=null)startCapture(code,data);
        handler.removeCallbacks(refresh); handler.post(refresh);
        return START_NOT_STICKY;
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Screen analysis",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
    }
    private void showOverlay(){
        if(chip!=null)return;
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        chip=new TextView(this);chip.setText("WAIT • Analyzing chart");chip.setTextColor(Color.YELLOW);chip.setTextSize(13);chip.setGravity(Gravity.CENTER);chip.setPadding(18,12,18,12);chip.setBackgroundColor(Color.rgb(20,30,48));
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.END;p.x=12;p.y=160;
        try{windowManager.addView(chip,p);}catch(Exception ignored){chip=null;}
    }
    private void startCapture(int code,Intent data){
        try{
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=m.getMediaProjection(code,data);
            if(projection==null)return;
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},handler);
            int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels,d=getResources().getDisplayMetrics().densityDpi;
            reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
            reader.setOnImageAvailableListener(r->processLatestFrame(r),handler);
            display=projection.createVirtualDisplay("FarhanTradingCapture",w,h,d,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        }catch(Exception e){setStatus("Capture error: "+e.getClass().getSimpleName());stopSelf();}
    }

    private void processLatestFrame(ImageReader source){
        Image image=null; Bitmap padded=null,frame=null;
        try{
            image=source.acquireLatestImage();
            if(image==null)return;
            long now=System.currentTimeMillis();
            if(now-lastFrameAt<1200L)return;
            lastFrameAt=now;
            long currentMinute=now/60000L;
            long msToBoundary=60000L-(now%60000L);

            // Analyze repeatedly through the whole live candle, not only at its end.
            Image.Plane plane=image.getPlanes()[0];
            int w=image.getWidth(),h=image.getHeight(),pixelStride=plane.getPixelStride(),rowStride=plane.getRowStride();
            int paddedWidth=rowStride/pixelStride;
            ByteBuffer buffer=plane.getBuffer();
            padded=Bitmap.createBitmap(paddedWidth,h,Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(buffer);
            frame=Bitmap.createBitmap(padded,0,0,w,h);
            ChartFrameAnalyzer.FrameResult detected=ChartFrameAnalyzer.analyze(frame);

            if(detected.candles.size()>=6){
                SignalEngine.Result result=SignalEngine.analyze(detected.candles);
                latestDirection="UP".equals(result.direction)?"BUY":"DOWN".equals(result.direction)?"SELL":"WAIT";
                latestReason=result.reason+" • "+detected.candles.size()+" candles";
                latestUpVotes=result.upVotes; latestDownVotes=result.downVotes;
                hasFreshAnalysis=true;
            }else{
                latestDirection="WAIT";
                latestReason=detected.message;
                latestUpVotes=0; latestDownVotes=0;
                hasFreshAnalysis=false;
            }

            // Publish only at T-minus 4 seconds, based on analysis accumulated during this candle.
            if(msToBoundary<=SIGNAL_WINDOW_MS && committedMinute!=currentMinute){
                committedMinute=currentMinute;
                String finalDirection=hasFreshAnalysis?latestDirection:"WAIT";
                String finalReason=hasFreshAnalysis?latestReason:"No valid chart analysis before candle close • "+latestReason;
                getSharedPreferences("engine",MODE_PRIVATE).edit()
                    .putString("auto_direction",finalDirection)
                    .putLong("auto_signal_minute",currentMinute+1L)
                    .putString("auto_frame_status","Next candle • "+finalReason)
                    .putInt("up_votes",latestUpVotes).putInt("down_votes",latestDownVotes)
                    .apply();
            }
        }catch(Exception e){
            // A frame error must not overwrite a previously published signal.
            if(committedMinute<0)setStatus("Chart scan error • "+e.getClass().getSimpleName());
        }finally{
            if(frame!=null && frame!=padded)frame.recycle();
            if(padded!=null)padded.recycle();
            if(image!=null)image.close();
        }
    }
    private void setStatus(String message){
        getSharedPreferences("engine",MODE_PRIVATE).edit().putString("auto_direction","WAIT").putString("auto_frame_status",message).apply();
    }
    @Override public void onDestroy(){
        if(handler!=null)handler.removeCallbacksAndMessages(null);
        if(reader!=null)reader.setOnImageAvailableListener(null,null);
        if(display!=null)display.release();
        if(reader!=null)reader.close();
        if(projection!=null)projection.stop();
        if(chip!=null&&windowManager!=null)try{windowManager.removeView(chip);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}