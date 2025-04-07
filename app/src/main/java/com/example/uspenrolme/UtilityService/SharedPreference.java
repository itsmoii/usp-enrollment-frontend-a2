package com.example.uspenrolme.UtilityService;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreference {
    private static final String USER_PREF = "user";
    private SharedPreferences appShared;
    private SharedPreferences.Editor prefEditor;

    public SharedPreference(Context context) {
        appShared = context.getSharedPreferences(USER_PREF, Activity.MODE_PRIVATE);
        this.prefEditor = appShared.edit();
    }

    public int getValue_int(String key) {
        return appShared.getInt(key, 0);
    }

    public void setValue_int(String key, int value) {
        prefEditor.putInt(key, value).apply();
    }

    public String getValue_string(String key) {
        return appShared.getString(key, "");
    }

    public void setValue_string(String key, String value) {
        prefEditor.putString(key, value).apply();
    }

    public boolean getValue_bool(String key) {
        return appShared.getBoolean(key, false);
    }

    public void setValue_bool(String key, Boolean value) {
        prefEditor.putBoolean(key, value).apply();
    }

    public void clear() {
        prefEditor.clear().apply();
    }
}