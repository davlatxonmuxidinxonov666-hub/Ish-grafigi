package uz.ishgrafigi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Telefon qayta yoqilganda yoki vaqt o'zgarganda budilnikni qayta qo'yadi. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        AlarmStore.scheduleNext(c);
    }
}
