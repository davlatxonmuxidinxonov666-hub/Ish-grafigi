package uz.ishgrafigi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Ish kunlari budilniklari ro'yxatini saqlaydi va navbatdagisini telefonga qo'yadi. */
public final class AlarmStore {
    private static final String PREFS = "alarms";
    private static final int RC_NEXT = 1;
    private static final int RC_SNOOZE = 3;

    private AlarmStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void save(Context c, String json) {
        prefs(c).edit().putString("list", json == null ? "[]" : json).apply();
    }

    private static JSONArray load(Context c) {
        try {
            return new JSONArray(prefs(c).getString("list", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static long next(Context c) {
        return prefs(c).getLong("next", 0L);
    }

    private static boolean canExact(AlarmManager am) {
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    }

    private static void setAt(Context c, AlarmManager am, long when, PendingIntent pi) {
        if (canExact(am)) {
            Intent open = new Intent(c, MainActivity.class);
            PendingIntent show = PendingIntent.getActivity(c, 2, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    /** Saqlangan ro'yxatdan eng yaqin kelajakdagi vaqtni topib, budilnik qo'yadi. */
    public static void scheduleNext(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent base = new Intent(c, AlarmReceiver.class);
        PendingIntent old = PendingIntent.getBroadcast(c, RC_NEXT, base,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (old != null) {
            am.cancel(old);
            old.cancel();
        }

        JSONArray a = load(c);
        long now = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        String text = "";
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            long t = o.optLong("t", 0L);
            if (t > now + 1000L && t < best) {
                best = t;
                text = o.optString("x", "");
            }
        }

        if (best == Long.MAX_VALUE) {
            prefs(c).edit().putLong("next", 0L).apply();
            return;
        }

        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("text", text);
        PendingIntent pi = PendingIntent.getBroadcast(c, RC_NEXT, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setAt(c, am, best, pi);
        prefs(c).edit().putLong("next", best).apply();
    }

    /** "Yana 5 daqiqa" tugmasi uchun bir martalik budilnik. */
    public static void snooze(Context c, String text, long delayMs) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("text", text);
        i.putExtra("snooze", true);
        PendingIntent pi = PendingIntent.getBroadcast(c, RC_SNOOZE, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setAt(c, am, System.currentTimeMillis() + delayMs, pi);
    }
}
