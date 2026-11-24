package uz.ishgrafigi;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Qulflangan ekran ustida ochiladigan budilnik oynasi. */
public class AlarmActivity extends Activity {
    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float volume = 0.3f;
    private String text = "";

    private final Runnable louder = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            volume = Math.min(1f, volume + 0.1f);
            try { player.setVolume(volume, volume); } catch (Exception ignored) {}
            if (volume < 1f) handler.postDelayed(this, 3500);
        }
    };

    private final Runnable autoStop = this::stopAndClose;

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xFF15803D);
        getWindow().setNavigationBarColor(0xFF166534);

        text = getIntent().getStringExtra("text");
        if (text == null) text = "";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(40), dp(28), dp(40));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF16A34A, 0xFF15803D, 0xFF166534}));

        TextView clock = new TextView(this);
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        root.addView(clock);

        TextView title = new TextView(this);
        title.setText(Lang.title(this));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(6));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText(text);
        sub.setTextColor(0xE6FFFFFF);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(36));
        root.addView(sub);

        Button stop = makeButton(Lang.stop(this), Color.WHITE, 0xFFDC2626);
        stop.setOnClickListener(v -> stopAndClose());
        root.addView(stop);

        Button later = makeButton(Lang.snooze(this), 0x33FFFFFF, Color.WHITE);
        later.setOnClickListener(v -> {
            AlarmStore.snooze(this, text, 5 * 60 * 1000L);
            stopAndClose();
        });
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) later.getLayoutParams();
        lp.topMargin = dp(14);
        root.addView(later);

        // Pastki qizil chiziq
        android.view.View stripe = new android.view.View(this);
        stripe.setBackgroundColor(0xFFDC2626);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(80), dp(5));
        sp.topMargin = dp(40);
        root.addView(stripe, sp);

        setContentView(root);
        Ring.cancel(this);
        startSound();
        handler.postDelayed(autoStop, 10 * 60 * 1000L);
    }

    private Button makeButton(String label, int bg, int fg) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(fg);
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(18));
        btn.setBackground(d);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));
        return btn;
    }

    private void startSound() {
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.melody);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.prepare();
            player.setVolume(volume, volume);
            player.start();
            handler.postDelayed(louder, 3500);
        } catch (Exception e) {
            player = null;
        }
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 400, 600, 1900}, 0));
            }
        } catch (Exception ignored) {}
    }

    private void stopSound() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) {}
        }
        Ring.cancel(this);
    }

    private void stopAndClose() {
        stopSound();
        finish();
    }

    @Override
    public void onBackPressed() {
        stopAndClose();
    }

    @Override
    protected void onDestroy() {
        stopSound();
        super.onDestroy();
    }
}
