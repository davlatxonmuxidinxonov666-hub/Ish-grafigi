package uz.ishgrafigi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Qadamlar: TYPE_STEP_COUNTER telefon yoqilgandan beri jami qadamni beradi.
 * Kun boshidagi qiymatni ("base") saqlab, bugungi qadamni hisoblaymiz; o'tgan kunlar "hist" da.
 */
final class Steps {
    private Steps() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("steps", Context.MODE_PRIVATE);
    }

    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 29
                || c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    static Sensor sensor(Context c) {
        SensorManager sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        return sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static JSONObject hist(SharedPreferences sp) {
        try { return new JSONObject(sp.getString("hist", "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    /** Datchikdan kelgan jami qiymatni qayd etadi va bugungi qadamni qaytaradi. */
    static synchronized int update(Context c, float v) {
        SharedPreferences sp = p(c);
        String d = today();
        String day = sp.getString("day", null);
        float base = sp.getFloat("base", v);
        float last = sp.getFloat("last", v);
        JSONObject h = hist(sp);
        if (day == null) {
            base = v;
            last = v;
        } else if (!d.equals(day)) {
            try { h.put(day, Math.max(0, Math.round(last - base))); } catch (Exception ignored) {}
            base = v >= last ? last : 0f;   // telefon qayta yoqilgan bo'lsa hisoblagich 0 dan boshlanadi
        } else if (v < last) {
            base = v - (last - base);       // kun ichida qayta yoqilgan: bugungi hisobni davom ettirish
        }
        last = v;
        // faqat oxirgi 60 kunni saqlaymiz
        if (h.length() > 60) {
            List<String> keys = new ArrayList<>();
            Iterator<String> it = h.keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys);
            for (int i = 0; i < keys.size() - 60; i++) h.remove(keys.get(i));
        }
        sp.edit().putString("day", d).putFloat("base", base).putFloat("last", last).putString("hist", h.toString()).apply();
        return Math.max(0, Math.round(last - base));
    }

    static int todayCount(Context c) {
        SharedPreferences sp = p(c);
        if (!today().equals(sp.getString("day", ""))) return 0;
        return Math.max(0, Math.round(sp.getFloat("last", 0f) - sp.getFloat("base", 0f)));
    }

    static String json(Context c) {
        SharedPreferences sp = p(c);
        int todaySteps = 0;
        if (today().equals(sp.getString("day", ""))) {
            todaySteps = Math.max(0, Math.round(sp.getFloat("last", 0f) - sp.getFloat("base", 0f)));
        }
        try {
            JSONObject o = new JSONObject();
            o.put("today", todaySteps);
            o.put("hist", hist(sp));
            o.put("sensor", sensor(c) != null);
            o.put("allowed", allowed(c));
            o.put("overlay", android.provider.Settings.canDrawOverlays(c));
            return o.toString();
        } catch (Exception e) {
            return "{\"today\":0,\"hist\":{},\"sensor\":false,\"allowed\":false}";
        }
    }

    /** Ilova yopiq bo'lsa ham qadamlarni har soatda qayd etib turish. */
    static void schedule(Context c) {
        if (sensor(c) == null || !allowed(c)) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, 9, new Intent(c, StepReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HOUR, pi);
    }
}
