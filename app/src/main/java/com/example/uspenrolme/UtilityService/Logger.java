package com.example.uspenrolme.UtilityService;

import android.util.Log;

public class Logger {
    public static void logEvent(String tag, String message) {
        Log.d(tag, message);
        // Optionally, write to file or send to server here
    }
}
