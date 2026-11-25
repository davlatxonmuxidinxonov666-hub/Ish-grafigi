package uz.ishgrafigi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

/**
 * Doimiy qadam sanagich: ekran o'chiq yoki ilova yopiq bo'lsa ham ishlaydi.
 * Bildirishnomada (qulf ekranida ham) jonli ko'rsatadi; xohlansa — boshqa ilovalar ustida qalqib turuvchi oyna.
 */
public class StepService extends Service implements SensorEventListener {
    static final String CH = "steps_v1";
    static final int NID = 21;
    static final String STOP = "uz.ishgrafigi.STOP_STEPS";

    private SensorManager sm;
    private long lastNotify = 0;
    private WindowManager wm;
    private TextView bubble;
    private WindowManager.LayoutParams blp;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("steps", Context.MODE_PRIVATE);
    }

    /** Sozlamaga qarab xizmatni yoqadi yoki o'chiradi. */
    static void sync(Context c) {
        boolean on = prefs(c).getBoolean("fg", true) && Steps.allowed(c) && Steps.sensor(c) != null;
        Intent i = new Intent(c, StepService.class);
        try {
            if (on) c.startForegroundService(i); else c.stopService(i);
        } catch (Exception ignored) {}
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n).replace(',', ' ');
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.getNotificationChannel(CH) == null) {
            NotificationChannel ch = new NotificationChannel(CH, Lang.stepsChannel(this), NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        Notification n = build(Steps.todayCount(this));
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
            else startForeground(NID, n);
        } catch (Exception e) {
            stopSelf();
            return;
        }
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor s = Steps.sensor(this);
        if (sm != null && s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            prefs(this).edit().putBoolean("fg", false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        updateBubble();
        refresh(Steps.todayCount(this));
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        int n = Steps.update(this, e.values[0]);
        if (bubble != null) bubble.setText(fmt(n));
        long now = SystemClock.elapsedRealtime();
        if (now - lastNotify > 3000) {
            lastNotify = now;
            refresh(n);
        }
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}

    private void refresh(int n) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NID, build(n));
        if (bubble != null) bubble.setText(fmt(n));
    }

    private Notification build(int n) {
        int goal = Math.max(1000, prefs(this).getInt("goal", 10000));
        int pct = Math.min(100, Math.round(n * 100f / goal));
        String km = String.format(Locale.US, "%.1f", n * 0.75 / 1000).replace('.', ',');
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(this, 23, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pStop = PendingIntent.getService(this, 22, new Intent(this, StepService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_stat_steps)
                .setColor(0xFF16A34A)
                .setContentTitle(Lang.stepsTitle(this, fmt(n)))
                .setContentText(Lang.stepsSub(this, fmt(goal), pct, km))
                .setProgress(goal, Math.min(n, goal), false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(pOpen)
                .addAction(new Notification.Action.Builder((Icon) null, Lang.stop(this), pStop).build())
                .build();
    }

    // ---------- Qalqib turuvchi oyna ----------
    private void updateBubble() {
        boolean want = prefs(this).getBoolean("bubble", false) && Settings.canDrawOverlays(this);
        if (want && bubble == null) addBubble();
        else if (!want && bubble != null) removeBubble();
    }

    private void addBubble() {
        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            TextView tv = new TextView(this);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setPadding(dp(12), dp(8), dp(14), dp(8));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xEE16A34A);
            g.setCornerRadius(dp(24));
            g.setStroke(dp(1), 0x55FFFFFF);
            tv.setBackground(g);
            tv.setElevation(dp(6));
            Drawable ic = getDrawable(R.drawable.ic_stat_steps);
            if (ic != null) {
                ic.setBounds(0, 0, dp(18), dp(18));
                tv.setCompoundDrawables(ic, null, null, null);
                tv.setCompoundDrawablePadding(dp(6));
            }
            tv.setText(fmt(Steps.todayCount(this)));
            SharedPreferences p = prefs(this);
            blp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            blp.gravity = Gravity.TOP | Gravity.START;
            blp.x = p.getInt("bx", dp(12));
            blp.y = p.getInt("by", dp(180));
            tv.setOnTouchListener(new android.view.View.OnTouchListener() {
                float dx, dy; int sx, sy; boolean moved;
                @Override public boolean onTouch(android.view.View v, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            dx = e.getRawX(); dy = e.getRawY(); sx = blp.x; sy = blp.y; moved = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int nx = sx + Math.round(e.getRawX() - dx), ny = sy + Math.round(e.getRawY() - dy);
                            if (Math.abs(nx - sx) > dp(6) || Math.abs(ny - sy) > dp(6)) moved = true;
                            blp.x = Math.max(0, nx); blp.y = Math.max(0, ny);
                            try { wm.updateViewLayout(v, blp); } catch (Exception ignored) {}
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (moved) {
                                prefs(StepService.this).edit().putInt("bx", blp.x).putInt("by", blp.y).apply();
                            } else {
                                Intent i = new Intent(StepService.this, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                try { startActivity(i); } catch (Exception ignored) {}
                            }
                            return true;
                    }
                    return false;
                }
            });
            wm.addView(tv, blp);
            bubble = tv;
        } catch (Exception e) {
            bubble = null;
        }
    }

    private void removeBubble() {
        try { if (wm != null && bubble != null) wm.removeView(bubble); } catch (Exception ignored) {}
        bubble = null;
    }

    @Override
    public void onDestroy() {
        if (sm != null) sm.unregisterListener(this);
        removeBubble();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
