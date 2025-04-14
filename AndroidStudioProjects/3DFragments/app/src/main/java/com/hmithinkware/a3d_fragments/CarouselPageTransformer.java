package com.hmithinkware.a3d_fragments;


import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class CarouselPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;
    private static final float MAX_ROTATION = 30f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();

        if (position < -1) { // Page is far off-screen to the left
            page.setAlpha(0f);
            page.setRotationY(0f);
        } else if (position <= 1) { // Page is visible or entering/leaving the screen
            // Scale the page down (between MIN_SCALE and 1)
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));

            // Fade the page relative to its size
            float alphaFactor = Math.max(MIN_ALPHA, 1 - Math.abs(position));
            page.setAlpha(alphaFactor);

            // Position the page
            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
            if (position < 0) {
                page.setTranslationX(horzMargin - vertMargin / 2);
            } else {
                page.setTranslationX(-horzMargin + vertMargin / 2);
            }

            // Scale the page
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);

            // Rotate the page
            page.setRotationY(position * -MAX_ROTATION);
        } else { // Page is far off-screen to the right
            page.setAlpha(0f);
            page.setRotationY(0f);
        }
    }
}