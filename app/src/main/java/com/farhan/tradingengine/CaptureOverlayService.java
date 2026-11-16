package com.farhan.tradingengine;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import android.graphics.Color;

public class CaptureOverlayService extends Service {
    public static final String ACTION_START="com.farhan.tradingengine.START_CAPTURE";
    public static final String ACTION_STOP="com.farhan.tradingengine.STOP_CAPTURE";
    public static final String EXTRA_RESULT="resultCode";
    public static final String EXTRA_DATA="projectionData";
    private static final String CHANNEL="farhan_capture";
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private WindowManager windowManager;
    private TextView chip;
    private Handler handler;
    private final Runnable refresh=new Runnable(){ public void run(){
        if(chip!=null){
            String d=getSharedPreferences("engine",MODE_PRIVATE).getString("direction","WAIT");
            long until=60-(System.currentTimeMillis()/1000)%60;
            chip.setText(d+"  •  "+until+"s\nOHLC feed required");
            chip.setTextColor("UP".equals(d)?Color.rgb(80,255,150):"DOWN".equals(d)?Color.rgb(255,100,100):Color.YELLOW);
        }
        if(handler!=null)handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(){super.onCreate();handler=new Handler(Looper.getMainLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        if(ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        createChannel();
        startForeground(7,new Notification.Builder(this,CHANNEL).setContentTitle("Farhan Trading Engine").setContentText("Screen capture active • signal-only").setSmallIcon(android.R.drawable.ic_menu_view).build());
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        showOverlay();
        int code=intent.getIntExtra(EXTRA_RESULT,Activity.RESULT_CANCELED);
        Intent data=intent.getParcelableExtra(EXTRA_DATA);
        if(code==Activity.RESULT_OK && data!=null)startCapture(code,data);
        handler.post(refresh);
        return START_NOT_STICKY;
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Screen analysis",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
    }
    private void showOverlay(){
        if(chip!=null)return;
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        chip=new TextView(this);chip.setText("WAIT • 60s\nOHLC feed required");chip.setTextColor(Color.YELLOW);chip.setTextSize(14);chip.setGravity(Gravity.CENTER);chip.setPadding(18,12,18,12);chip.setBackgroundColor(Color.rgb(20,30,48));
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
            display=projection.createVirtualDisplay("FarhanTradingCapture",w,h,d,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        }catch(Exception e){stopSelf();}
    }
    @Override public void onDestroy(){
        if(handler!=null)handler.removeCallbacksAndMessages(null);
        if(display!=null)display.release();
        if(reader!=null)reader.close();
        if(projection!=null)projection.stop();
        if(chip!=null&&windowManager!=null)try{windowManager.removeView(chip);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}