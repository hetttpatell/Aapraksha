package com.example.aapraksha;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * SplashActivity — AapRaksha Redefined Splash Screen
 *
 * Timeline (4 000 ms total):
 *   0    ms  — rings + glow fade in, arc starts spinning
 *   200  ms  — logo scales in with overshoot
 *   600  ms  — app name slides up
 *   800  ms  — tagline fades in
 *   900  ms  — loading bar + label appear; bar fills over 2 800 ms
 *   1 000 ms — lighthouse glow ramps up
 *   3 600 ms — exit fade starts
 *   4 000 ms — navigate to MainActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TOTAL    = 4000;
    private static final int EXIT_START      = 3600;
    private static final int EXIT_DURATION   =  400;

    // ── Views ────────────────────────────────────────────────────────────────
    private SplashRingView ringView;
    private ImageView      logoImage;
    private TextView       appNameText;
    private TextView       taglineText;
    private TextView       loadingLabel;
    private ProgressBar    loadingBar;
    private TextView       versionText;

    // ── Arc spin animator (kept to cancel on destroy) ────────────────────────
    private ValueAnimator arcAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);
        hideSystemBars();
        bindViews();
        startSplashSequence();
    }

    // ── System bars ──────────────────────────────────────────────────────────
    private void hideSystemBars() {
        WindowInsetsControllerCompat ctrl =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctrl.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        ctrl.hide(WindowInsetsCompat.Type.systemBars());
    }

    // ── View binding + initial invisible state ───────────────────────────────
    private void bindViews() {
        ringView    = findViewById(R.id.ring_view);
        logoImage   = findViewById(R.id.logo_image);
        appNameText = findViewById(R.id.app_name_text);
        taglineText = findViewById(R.id.tagline_text);
        loadingLabel = findViewById(R.id.loading_label);
        loadingBar  = findViewById(R.id.loading_bar);
        versionText = findViewById(R.id.version_text);

        // All start invisible; SplashRingView alpha is set in XML already (0)
        logoImage.setScaleX(0.25f);
        logoImage.setScaleY(0.25f);
        appNameText.setTranslationY(60f);
        taglineText.setTranslationY(40f);
    }

    // ── Master sequence ──────────────────────────────────────────────────────
    private void startSplashSequence() {
        Handler h = new Handler(Looper.getMainLooper());

        // t = 0: rings fade in and arc begins spinning
        showRingsAndStartArc();

        // t = 200: logo appears
        h.postDelayed(this::animateLogo, 200);

        // t = 600: app name slides up
        h.postDelayed(this::animateAppName, 600);

        // t = 800: tagline
        h.postDelayed(this::animateTagline, 800);

        // t = 900: loading bar
        h.postDelayed(this::animateLoadingBar, 900);

        // t = 1000: lighthouse glow ramps up
        h.postDelayed(this::animateGlow, 1000);

        // t = EXIT_START: begin exit fade
        h.postDelayed(this::beginExit, EXIT_START);
    }

    // ── Step 1: rings + arc ──────────────────────────────────────────────────
    private void showRingsAndStartArc() {
        // Fade ring view in
        ObjectAnimator ringFade = ObjectAnimator.ofFloat(ringView, "alpha", 0f, 1f);
        ringFade.setDuration(700);
        ringFade.setInterpolator(new DecelerateInterpolator());
        ringFade.start();

        // Continuous arc rotation: 0 → 1080° over ~3 s, then repeat
        arcAnimator = ValueAnimator.ofFloat(0f, 1080f);
        arcAnimator.setDuration(3000);
        arcAnimator.setRepeatCount(ValueAnimator.INFINITE);
        arcAnimator.setRepeatMode(ValueAnimator.RESTART);
        arcAnimator.setInterpolator(new LinearInterpolator());
        arcAnimator.addUpdateListener(anim ->
                ringView.setArcAngle((Float) anim.getAnimatedValue()));
        arcAnimator.start();
    }

    // ── Step 2: logo ─────────────────────────────────────────────────────────
    private void animateLogo() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.25f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.25f, 1f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(logoImage, "alpha",  0f,    1f);
        scaleX.setDuration(700);
        scaleY.setDuration(700);
        alpha.setDuration(500);
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new OvershootInterpolator(1.3f));
        set.start();
    }

    // ── Step 3: app name ─────────────────────────────────────────────────────
    private void animateAppName() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator ty    = ObjectAnimator.ofFloat(appNameText, "translationY", 60f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(appNameText, "alpha", 0f, 1f);
        ty.setDuration(550);
        alpha.setDuration(550);
        set.playTogether(ty, alpha);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
    }

    // ── Step 4: tagline ──────────────────────────────────────────────────────
    private void animateTagline() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator ty    = ObjectAnimator.ofFloat(taglineText, "translationY", 40f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
        ty.setDuration(450);
        alpha.setDuration(450);
        set.playTogether(ty, alpha);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    // ── Step 5: loading bar fills left-to-right over 2 800 ms ───────────────
    private void animateLoadingBar() {
        // Fade in container + label
        ObjectAnimator barAlpha   = ObjectAnimator.ofFloat(loadingBar,   "alpha", 0f, 1f);
        ObjectAnimator labelAlpha = ObjectAnimator.ofFloat(loadingLabel,  "alpha", 0f, 1f);
        ObjectAnimator verAlpha   = ObjectAnimator.ofFloat(versionText,   "alpha", 0f, 1f);
        barAlpha.setDuration(400);
        labelAlpha.setDuration(400);
        verAlpha.setDuration(600);
        barAlpha.start();
        labelAlpha.start();
        verAlpha.start();

        // Fill progress 0 → 1000 over 2 800 ms with slight ease-in
        ObjectAnimator fill = ObjectAnimator.ofInt(loadingBar, "progress", 0, 1000);
        fill.setDuration(2800);
        fill.setInterpolator(new AccelerateDecelerateInterpolator());
        fill.start();
    }

    // ── Step 6: lighthouse glow ───────────────────────────────────────────────
    private void animateGlow() {
        ObjectAnimator glow = ObjectAnimator.ofFloat(ringView, "glowIntensity", 0f, 1f);
        glow.setDuration(1200);
        glow.setInterpolator(new DecelerateInterpolator(2f));
        glow.start();
    }

    // ── Exit ─────────────────────────────────────────────────────────────────
    private void beginExit() {
        View root = findViewById(R.id.splash_root);

        // Scale logo out slightly while fading everything
        ObjectAnimator logoScale = ObjectAnimator.ofFloat(logoImage, "scaleX", 1f, 1.08f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 1f, 1.08f);
        ObjectAnimator rootFade = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f);

        logoScale.setDuration(EXIT_DURATION);
        logoScaleY.setDuration(EXIT_DURATION);
        rootFade.setDuration(EXIT_DURATION);

        AnimatorSet exit = new AnimatorSet();
        exit.playTogether(logoScale, logoScaleY, rootFade);
        exit.setInterpolator(new AccelerateDecelerateInterpolator());
        exit.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                goToMain();
            }
        });
        exit.start();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.main_enter, R.anim.splash_exit);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arcAnimator != null) arcAnimator.cancel();
    }

    @Override
    public void onBackPressed() {
        // Intentionally swallowed during splash
    }
}
