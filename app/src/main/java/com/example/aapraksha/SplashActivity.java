package com.example.aapraksha;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;

    private ImageView   logoImage;
    private TextView    appNameText;
    private TextView    taglineText;
    private ProgressBar loadingBar;
    private TextView    versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);
        hideSystemBars();
        bindViews();
        startSplash();
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat ctrl =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctrl.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        ctrl.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void bindViews() {
        logoImage   = findViewById(R.id.logo_image);
        appNameText = findViewById(R.id.app_name_text);
        taglineText = findViewById(R.id.tagline_text);
        loadingBar  = findViewById(R.id.loading_bar);
        versionText = findViewById(R.id.version_text);

        logoImage.setScaleX(0.8f);
        logoImage.setScaleY(0.8f);
    }

    private void startSplash() {
        Handler h = new Handler(Looper.getMainLooper());

        // Logo fades + scales in
        animateIn(logoImage, 0, 700, true);

        // App name fades in after logo
        h.postDelayed(() -> animateIn(appNameText, 0, 500, false), 500);

        // Tagline fades in after name
        h.postDelayed(() -> animateIn(taglineText, 0, 500, false), 750);

        // Loading bar fades in then fills
        h.postDelayed(() -> {
            animateIn(loadingBar, 0, 300, false);
            animateIn(versionText, 0, 300, false);

            // Animate progress 0 → 100 over remaining splash time
            ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
            progressAnim.setDuration(SPLASH_DURATION - 1000); // fills over ~2s
            progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            progressAnim.addUpdateListener(anim ->
                    loadingBar.setProgress((int) anim.getAnimatedValue()));
            progressAnim.start();
        }, 1000);

        // Navigate to Main when done
        h.postDelayed(this::goToMain, SPLASH_DURATION);
    }

    private void animateIn(android.view.View view, int startDelay, int duration, boolean withScale) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        alpha.setDuration(duration);

        if (withScale) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f);
            scaleX.setDuration(duration);
            scaleY.setDuration(duration);
            set.playTogether(alpha, scaleX, scaleY);
        } else {
            set.play(alpha);
        }

        set.setInterpolator(new DecelerateInterpolator());
        set.setStartDelay(startDelay);
        set.start();
    }

    private void goToMain() {
        startActivity(new Intent(this, DashboardActivity.class));
        overridePendingTransition(R.anim.main_enter, R.anim.splash_exit);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Swallowed during splash
    }
}
