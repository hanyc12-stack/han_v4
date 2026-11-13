package com.example.batteryalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;

public class BatteryService extends Service {
    private static final String CH = "battery_watch";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { handle(i); }
    };

    @Override public void onCreate() {
        super.onCreate();
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent sticky;
        if (Build.VERSION.SDK_INT >= 33) {
            sticky = registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            sticky = registerReceiver(receiver, f);
        }
        if (sticky != null) handle(sticky);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "배터리 감시", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, CH)
                .setContentTitle("배터리 알림이 작동 중")
                .setContentText("배터리가 부족하면 가족에게 문자를 보냅니다")
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setOngoing(true)
                .build();
        startForeground(1, n);
        return START_STICKY;
    }

    private void handle(Intent i) {
        Prefs p = new Prefs(this);
        if (!p.enabled()) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) return;
        int pct = level * 100 / scale;
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
                || i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int th = p.threshold();

        if (!charging && pct <= th) {
            if (!p.sent()) {
                String msg = "[배터리 알림] " + p.name() + " 휴대폰 배터리가 " + pct
                        + "% 남았습니다. 충전이 필요합니다.";
                SmsSender.sendToAll(this, msg);
                p.setSent(true);   // 같은 상황에서 중복 발송 방지
            }
        } else if (charging || pct >= th + 5) {
            p.setSent(false);      // 충전하거나 충분히 올라가면 다시 알림 가능
        }
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
