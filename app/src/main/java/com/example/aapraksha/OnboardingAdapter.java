package com.example.aapraksha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.setOnboardingData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textDescription;
        private ImageView imageIcon;
        private TextView textStepChip;
        private FrameLayout iconContainer;
        private View dividerLine;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textSlideTitle);
            textDescription = itemView.findViewById(R.id.textSlideDescription);
            imageIcon = itemView.findViewById(R.id.imageSlideIcon);
            textStepChip = itemView.findViewById(R.id.textStepChip);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            dividerLine = itemView.findViewById(R.id.dividerLine);
        }

        void setOnboardingData(OnboardingItem onboardingItem) {
            textTitle.setText(onboardingItem.getTitle());
            textDescription.setText(onboardingItem.getDescription());
            imageIcon.setImageResource(onboardingItem.getImageResId());

            // Set step chip text
            textStepChip.setText("Step " + onboardingItem.getStepNumber() + " of 4");

            // Apply emergency theme for slide 4
            if (onboardingItem.isEmergencySlide()) {
                // Emergency red theming
                textStepChip.setBackgroundResource(R.drawable.bg_ob_chip_red);
                textStepChip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ob_emergency));
                iconContainer.setBackgroundResource(R.drawable.bg_ob_icon_container_red);
            } else {
                // Default indigo theming
                textStepChip.setBackgroundResource(R.drawable.bg_ob_chip_indigo);
                textStepChip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ob_primary));
                iconContainer.setBackgroundResource(R.drawable.bg_ob_icon_container_indigo);
            }
        }
    }
}
