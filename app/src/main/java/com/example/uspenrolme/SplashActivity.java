package com.example.uspenrolme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uspenrolme.UtilityService.SharedPreference;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check if the user is logged in
        SharedPreference sharedPref = new SharedPreference(this);
        String token = sharedPref.getValue_string("token");

        // Delay for 3 seconds, then navigate to the appropriate activity
        new Handler().postDelayed(() -> {
            Intent intent;
            if (token != null && !token.isEmpty()) {
                // Navigate to the dashboard if the user is logged in
                intent = new Intent(SplashActivity.this, StudentDashboardActivity.class);
            } else {
                // Navigate to the login screen if the user is not logged in
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}