package uz.ishgrafigi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

/** Har soatda datchikdan bir marta qiymat olib, qadamlarni qayd etadi. */
public class StepReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        final Context c = ctx.getApplicationContext();
        final SensorManager sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        final Sensor s = Steps.sensor(c);
        if (sm == null || s == null || !Steps.allowed(c)) return;
        final PendingResult pr = goAsync();
        final Handler h = new Handler(Looper.getMainLooper());
        final boolean[] done = {false};
        final SensorEventListener l = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent e) {
                if (done[0]) return;
                done[0] = true;
                Steps.update(c, e.values[0]);
                sm.unregisterListener(this);
                h.removeCallbacksAndMessages(null);
                pr.finish();
            }
            @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL);
        h.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            sm.unregisterListener(l);
            pr.finish();
        }, 8000);
    }
}
