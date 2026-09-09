package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openLogin = () -> {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(openLogin, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openLogin);
        super.onDestroy();
    }
}
