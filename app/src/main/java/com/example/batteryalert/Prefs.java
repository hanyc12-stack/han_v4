package com.example.batteryalert;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private final SharedPreferences sp;
    public Prefs(Context c) { sp = c.getSharedPreferences("battery_alert", Context.MODE_PRIVATE); }

    public int threshold() { return sp.getInt("threshold", 20); }
    public void setThreshold(int v) { sp.edit().putInt("threshold", v).apply(); }

    public String numbers() { return sp.getString("numbers", ""); }
    public void setNumbers(String v) { sp.edit().putString("numbers", v).apply(); }

    public String name() { return sp.getString("name", "부모님"); }
    public void setName(String v) { sp.edit().putString("name", v).apply(); }

    public boolean enabled() { return sp.getBoolean("enabled", false); }
    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    public boolean sent() { return sp.getBoolean("sent", false); }
    public void setSent(boolean v) { sp.edit().putBoolean("sent", v).apply(); }
}
