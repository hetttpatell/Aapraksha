package com.example.aapraksha;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutIndicators;
    private ViewPager2 viewPagerOnboarding;
    private TextView buttonAction;
    private TextView textSwipeHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent and match the dark background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.ob_background_start));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.ob_background_end));
        }

        setContentView(R.layout.activity_onboarding);

        layoutIndicators = findViewById(R.id.layoutIndicators);
        buttonAction = findViewById(R.id.buttonAction);
        textSwipeHint = findViewById(R.id.textSwipeHint);
        viewPagerOnboarding = findViewById(R.id.viewPagerOnboarding);

        setupOnboardingItems();
        setupIndicators();
        setCurrentIndicator(0);

        viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                updateButtonForPosition(position);
                animateImage(position);
            }
        });

        buttonAction.setOnClickListener(v -> {
            if (viewPagerOnboarding.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                // Skip: go directly to last slide
                viewPagerOnboarding.setCurrentItem(onboardingAdapter.getItemCount() - 1);
            } else {
                // Get Started: navigate forward
                navigateToDashboard();
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        OnboardingItem slide1 = new OnboardingItem(
                "One Tap Away\nFrom Safety",
                "Tap the SOS button in-app or hold the volume key for 3 seconds. No unlocking. No delay. Help is triggered the instant you need it.",
                R.drawable.ic_ob_sos,
                1,
                false
        );

        OnboardingItem slide2 = new OnboardingItem(
                "Live Location.\nEvery Minute.",
                "From the moment SOS fires, your real-time GPS is streamed to your emergency contacts — refreshed every 60 seconds until you're confirmed safe.",
                R.drawable.ic_ob_location,
                2,
                false
        );

        OnboardingItem slide3 = new OnboardingItem(
                "Your Circle\nIs Notified.",
                "An emergency SMS with a live Google Maps link is dispatched instantly to all your trusted contacts — so help is always on the way.",
                R.drawable.ic_ob_contacts,
                3,
                false
        );

        OnboardingItem slide4 = new OnboardingItem(
                "Only Your PIN\nStops the Alert.",
                "Your SOS stays active until you're truly safe. Enter your private 4-digit Emergency PIN to cancel — a layer no attacker can bypass without you.",
                R.drawable.ic_ob_lock,
                4,
                true
        );

        onboardingItems.add(slide1);
        onboardingItems.add(slide2);
        onboardingItems.add(slide3);
        onboardingItems.add(slide4);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        viewPagerOnboarding.setAdapter(onboardingAdapter);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(6, 0, 6, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.ob_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        boolean isLastSlide = (index == onboardingAdapter.getItemCount() - 1);

        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                // Active dot: wide pill, colored per slide
                if (isLastSlide) {
                    imageView.setImageDrawable(
                            ContextCompat.getDrawable(getApplicationContext(), R.drawable.ob_indicator_active_red)
                    );
                } else {
                    imageView.setImageDrawable(
                            ContextCompat.getDrawable(getApplicationContext(), R.drawable.ob_indicator_active_indigo)
                    );
                }
            } else {
                // Inactive dot: small square
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.ob_indicator_inactive)
                );
            }
        }
    }

    private void updateButtonForPosition(int position) {
        boolean isLastSlide = (position == onboardingAdapter.getItemCount() - 1);

        if (isLastSlide) {
            // Slide 4: Filled red "Get Started" button
            buttonAction.setText("Get Started");
            buttonAction.setBackgroundResource(R.drawable.bg_ob_btn_get_started);
            buttonAction.setTextColor(ContextCompat.getColor(this, R.color.surface_white));
            textSwipeHint.setVisibility(View.INVISIBLE);
        } else {
            // Slides 1-3: FilledTonal indigo "Skip" button
            buttonAction.setText("Skip");
            buttonAction.setBackgroundResource(R.drawable.bg_ob_btn_skip);
            buttonAction.setTextColor(ContextCompat.getColor(this, R.color.ob_primary));
            textSwipeHint.setVisibility(View.VISIBLE);
        }
    }

    private void animateImage(int position) {
        View view = viewPagerOnboarding.getChildAt(0);
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                ImageView imageIcon = holder.itemView.findViewById(R.id.imageSlideIcon);
                if (imageIcon != null) {
                    // Clear any existing animation
                    imageIcon.animate().cancel();
                    imageIcon.setTranslationY(0f);

                    // Gentle floating animation
                    ObjectAnimator animator = ObjectAnimator.ofFloat(imageIcon, "translationY", -12f, 12f);
                    animator.setDuration(2000);
                    animator.setRepeatCount(ValueAnimator.INFINITE);
                    animator.setRepeatMode(ValueAnimator.REVERSE);
                    animator.start();
                }
            }
        }
    }

    private void navigateToDashboard() {
        SharedPreferences prefs = getSharedPreferences("AaprakshaPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasSeenOnboarding", true).apply();

        boolean isNewUser = getIntent().getBooleanExtra("isNewUser", false);
        Intent intent;

        if (isNewUser) {
            intent = new Intent(this, SetPinActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
