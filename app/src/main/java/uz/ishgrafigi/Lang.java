package uz.ishgrafigi;

import android.content.Context;

/** Budilnik oynasi va bildirishnoma matnlari: o'zbek, rus, ingliz. */
public final class Lang {
    private Lang() {}

    static void set(Context c, String lang) {
        c.getSharedPreferences("alarms", Context.MODE_PRIVATE).edit().putString("lang", lang).apply();
    }

    private static String get(Context c) {
        return c.getSharedPreferences("alarms", Context.MODE_PRIVATE).getString("lang", "uz");
    }

    private static String pick(Context c, String uz, String ru, String en) {
        switch (get(c)) {
            case "ru": return ru;
            case "en": return en;
            default: return uz;
        }
    }

    static String title(Context c) { return pick(c, "Bugun ish kuni!", "Сегодня рабочий день!", "Workday today!"); }
    static String stop(Context c) { return pick(c, "To'xtatish", "Остановить", "Stop"); }
    static String snooze(Context c) { return pick(c, "Yana 5 daqiqa", "Ещё 5 минут", "5 more minutes"); }
    static String starting(Context c) { return pick(c, "Ish kuni boshlanmoqda", "Начинается рабочий день", "Your workday is starting"); }
    static String stepsChannel(Context c) { return pick(c, "Qadamlar", "Шаги", "Steps"); }
    static String stepsTitle(Context c, String n) { return pick(c, "Qadamlar: " + n, "Шаги: " + n, "Steps: " + n); }
    static String stepsSub(Context c, String goal, int pct, String km) {
        return pick(c, "Maqsad " + goal + " · " + pct + "% · " + km + " km",
                "Цель " + goal + " · " + pct + "% · " + km + " км",
                "Goal " + goal + " · " + pct + "% · " + km + " km");
    }
    static String channel(Context c) { return pick(c, "Budilnik", "Будильник", "Alarm"); }
}
