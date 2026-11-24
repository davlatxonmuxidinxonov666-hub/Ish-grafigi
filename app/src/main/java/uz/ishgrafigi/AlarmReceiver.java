package uz.ishgrafigi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String text = intent.getStringExtra("text");
        if (text == null || text.isEmpty()) text = "Ish kuni boshlanmoqda";
        Ring.show(c, text);
        if (!intent.getBooleanExtra("snooze", false)) {
            AlarmStore.scheduleNext(c);
        }
    }
}
