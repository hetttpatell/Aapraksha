package com.example.aapraksha;

public class OnboardingItem {
    private String title;
    private String description;
    private int imageResId;
    private int stepNumber;
    private boolean isEmergencySlide;

    public OnboardingItem(String title, String description, int imageResId, int stepNumber, boolean isEmergencySlide) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
        this.stepNumber = stepNumber;
        this.isEmergencySlide = isEmergencySlide;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
    public int getStepNumber() { return stepNumber; }
    public boolean isEmergencySlide() { return isEmergencySlide; }
}
