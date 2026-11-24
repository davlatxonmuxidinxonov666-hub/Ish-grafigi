package uz.ishgrafigi;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF15803D);

        web = new WebView(this);
        web.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(getApplicationContext()), "Android");
        setContentView(web);

        if (b != null) web.restoreState(b);
        else web.loadUrl("file:///android_asset/index.html");

        Ring.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        AlarmStore.scheduleNext(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmStore.scheduleNext(this);
    }

    /** Budilnik ishonchli chalishi uchun kerakli ruxsatlarni bir marta so'raydi. */
    private void askAlarmPermissions() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 31 && am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (Build.VERSION.SDK_INT >= 34 && nm != null && !nm.canUseFullScreenIntent()) {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName())));
            }
        } catch (Exception ignored) {}
    }

    private class Bridge {
        private final Context app;
        private boolean asked = false;

        Bridge(Context app) { this.app = app; }

        @JavascriptInterface
        public void setAlarms(String json) {
            AlarmStore.save(app, json);
            AlarmStore.scheduleNext(app);
            if (!asked && json != null && json.length() > 2) {
                asked = true;
                runOnUiThread(MainActivity.this::askAlarmPermissions);
            }
        }

        @JavascriptInterface
        public void test(String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, AlarmActivity.class);
                i.putExtra("text", text);
                startActivity(i);
            });
        }

        /** Mavzuga qarab tepa va pastki tizim panellarining rangi. */
        @JavascriptInterface
        public void setBars(String theme) {
            runOnUiThread(() -> {
                boolean night = (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                int top = 0xFF15803D, bottom;
                if ("violet".equals(theme)) { top = 0xFF2A1466; bottom = 0xFF110828; }
                else if ("dark".equals(theme) || (!"light".equals(theme) && night)) bottom = 0xFF0E1411;
                else bottom = 0xFFFFFFFF;
                getWindow().setStatusBarColor(top);
                getWindow().setNavigationBarColor(bottom);
                android.view.View d = getWindow().getDecorView();
                int f = d.getSystemUiVisibility();
                if (bottom == 0xFFFFFFFF) f |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                else f &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                d.setSystemUiVisibility(f);
            });
        }

        @JavascriptInterface
        public String nextAlarm() {
            return String.valueOf(AlarmStore.next(app));
        }
    }
}
