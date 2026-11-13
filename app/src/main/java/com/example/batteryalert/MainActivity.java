package com.example.batteryalert;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private Prefs prefs;
    private EditText nameEt, numbersEt;
    private SeekBar seek;
    private TextView thTv, statusTv;
    private Button toggleBtn;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private TextView label(String t, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(Color.BLACK);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dp(14), 0, dp(6));
        return tv;
    }

    private Button button(String t, int color) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextSize(22);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(40));
        sv.addView(root);

        TextView title = label("🔋 배터리 알림이", 30, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        statusTv = label("", 22, true);
        statusTv.setGravity(Gravity.CENTER);
        root.addView(statusTv);

        // 1. 호칭
        root.addView(label("1. 문자에 쓸 호칭", 20, true));
        nameEt = new EditText(this);
        nameEt.setTextSize(22);
        nameEt.setText(prefs.name());
        nameEt.setSingleLine(true);
        root.addView(nameEt);

        // 2. 배터리 기준
        root.addView(label("2. 알림 보낼 배터리 기준", 20, true));
        thTv = label("", 24, true);
        thTv.setTextColor(Color.parseColor("#C62828"));
        root.addView(thTv);
        seek = new SeekBar(this);
        seek.setMax(45); // 5% ~ 50%
        seek.setProgress(prefs.threshold() - 5);
        seek.setPadding(dp(8), dp(12), dp(8), dp(12));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { updateTh(); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        root.addView(seek);
        updateTh();

        // 3. 자녀 번호
        root.addView(label("3. 자녀 전화번호 (한 줄에 한 명)", 20, true));
        numbersEt = new EditText(this);
        numbersEt.setTextSize(22);
        numbersEt.setInputType(InputType.TYPE_CLASS_PHONE | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        numbersEt.setMinLines(3);
        numbersEt.setGravity(Gravity.TOP);
        numbersEt.setHint("01012345678\n01098765432");
        numbersEt.setText(prefs.numbers());
        root.addView(numbersEt);

        toggleBtn = button("", Color.parseColor("#2E7D32"));
        toggleBtn.setOnClickListener(v -> {
            if (prefs.enabled()) stopMonitoring(); else startMonitoring();
        });
        root.addView(toggleBtn);

        Button test = button("테스트 문자 보내기", Color.parseColor("#1565C0"));
        test.setOnClickListener(v -> sendTest());
        root.addView(test);

        Button opt = button("배터리 최적화 끄기 (꼭 눌러주세요)", Color.parseColor("#6D4C41"));
        opt.setTextSize(18);
        opt.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });
        root.addView(opt);

        setContentView(sv);
        refreshUi();
    }

    private void updateTh() {
        thTv.setText("배터리 " + (seek.getProgress() + 5) + "% 이하");
    }

    private void refreshUi() {
        boolean on = prefs.enabled();
        statusTv.setText(on ? "● 작동 중" : "○ 꺼져 있음");
        statusTv.setTextColor(on ? Color.parseColor("#2E7D32") : Color.GRAY);
        toggleBtn.setText(on ? "알림 끄기" : "알림 시작하기");
        toggleBtn.setBackgroundColor(on ? Color.parseColor("#C62828") : Color.parseColor("#2E7D32"));
    }

    private boolean saveInputs() {
        ArrayList<String> nums = SmsSender.parseNumbers(numbersEt.getText().toString());
        if (nums.isEmpty()) {
            Toast.makeText(this, "자녀 전화번호를 입력해 주세요", Toast.LENGTH_LONG).show();
            return false;
        }
        String name = nameEt.getText().toString().trim();
        prefs.setName(name.isEmpty() ? "부모님" : name);
        prefs.setThreshold(seek.getProgress() + 5);
        prefs.setNumbers(String.join("\n", nums));
        numbersEt.setText(prefs.numbers());
        return true;
    }

    private boolean ensureSmsPermission(int req) {
        ArrayList<String> need = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        if (need.isEmpty()) return true;
        requestPermissions(need.toArray(new String[0]), req);
        return false;
    }

    private void startMonitoring() {
        if (!saveInputs()) return;
        if (!ensureSmsPermission(1)) return;
        doStart();
    }

    private void doStart() {
        prefs.setSent(false);
        prefs.setEnabled(true);
        startForegroundService(new Intent(this, BatteryService.class));
        refreshUi();
        Toast.makeText(this, "시작했습니다. 배터리 " + prefs.threshold() + "% 이하에서 문자를 보냅니다.",
                Toast.LENGTH_LONG).show();
    }

    private void stopMonitoring() {
        prefs.setEnabled(false);
        stopService(new Intent(this, BatteryService.class));
        refreshUi();
    }

    private void sendTest() {
        if (!saveInputs()) return;
        if (!ensureSmsPermission(2)) return;
        doTest();
    }

    private void doTest() {
        int n = SmsSender.sendToAll(this, "[테스트] 배터리 알림이가 정상 작동합니다. (" + prefs.name() + ")");
        Toast.makeText(this, n + "명에게 테스트 문자를 보냈습니다", Toast.LENGTH_LONG).show();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "문자 발송 권한이 필요합니다", Toast.LENGTH_LONG).show();
            return;
        }
        if (code == 1) doStart(); else if (code == 2) doTest();
    }
}
