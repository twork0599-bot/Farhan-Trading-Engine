package com.farhan.tradingengine;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 36, 24, 24);
        root.setBackgroundColor(Color.rgb(12, 18, 30));

        TextView title = new TextView(this);
        title.setText("FARHAN TRADING ENGINE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 28);
        root.addView(title);

        TextView signal = new TextView(this);
        signal.setText("WAIT");
        signal.setTextColor(Color.rgb(255, 193, 7));
        signal.setTextSize(38);
        signal.setGravity(Gravity.CENTER);
        signal.setPadding(0, 24, 0, 8);
        root.addView(signal);

        TextView note = new TextView(this);
        note.setText("Chart recognition is not active yet. No UP or DOWN signal will be shown until reliable candle data is available.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(14);
        note.setGravity(Gravity.CENTER);
        note.setPadding(8, 8, 8, 24);
        root.addView(note);

        Button overlay = new Button(this);
        overlay.setText("Enable floating-window permission");
        overlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        root.addView(overlay);

        Button capture = new Button(this);
        capture.setText("Screen-capture permission (setup test)");
        capture.setOnClickListener(v -> {
            status.setText("Screen capture is not started. This button only opens Android's consent screen.");
            android.media.projection.MediaProjectionManager manager =
                (android.media.projection.MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            if (manager != null) {
                startActivityForResult(manager.createScreenCaptureIntent(), 4101);
            }
        });
        root.addView(capture);

        status = new TextView(this);
        status.setText("Status: waiting for setup");
        status.setTextColor(Color.rgb(160, 190, 220));
        status.setTextSize(13);
        status.setPadding(0, 24, 0, 8);
        root.addView(status);

        TextView footer = new TextView(this);
        footer.setText("Signal-only • No auto-trading • No account login • Use demo/paper testing first");
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 24, 0, 0);
        root.addView(footer);

        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4101) {
            status.setText(resultCode == RESULT_OK
                ? "Permission granted for this setup test only; capture is not running."
                : "Screen-capture permission was not granted.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null && Settings.canDrawOverlays(this)) {
            status.setText("Overlay permission is enabled. Signal engine is still inactive.");
        }
    }
}
