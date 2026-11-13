package com.example.batteryalert;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;

import java.util.ArrayList;

public class SmsSender {

    /** 줄바꿈/쉼표/세미콜론으로 구분된 번호 목록 -> 정리된 리스트 */
    public static ArrayList<String> parseNumbers(String raw) {
        ArrayList<String> out = new ArrayList<>();
        for (String s : raw.split("[\\n,;]+")) {
            String n = s.replaceAll("[^0-9+]", "");
            if (n.length() >= 8 && !out.contains(n)) out.add(n);
        }
        return out;
    }

    /** 모든 수신자에게 발송. 성공적으로 요청한 수신자 수를 반환 */
    public static int sendToAll(Context ctx, String message) {
        if (ctx.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return 0;
        SmsManager sm = Build.VERSION.SDK_INT >= 31
                ? ctx.getSystemService(SmsManager.class)
                : SmsManager.getDefault();
        int ok = 0;
        for (String n : parseNumbers(new Prefs(ctx).numbers())) {
            try {
                ArrayList<String> parts = sm.divideMessage(message);
                sm.sendMultipartTextMessage(n, null, parts, null, null);
                ok++;
            } catch (Exception ignored) { }
        }
        return ok;
    }
}
