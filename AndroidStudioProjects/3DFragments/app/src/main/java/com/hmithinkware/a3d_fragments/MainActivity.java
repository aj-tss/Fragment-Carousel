package com.hmithinkware.a3d_fragments;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private CardPagerAdapter adapter;
    private View[] indicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up ViewPager2
        viewPager = findViewById(R.id.viewPager);
        adapter = new CardPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Add sample data
        adapter.addCardFragment("Nature", "Beautiful nature landscapes", R.drawable.image1);
        adapter.addCardFragment("City", "Urban city views", R.drawable.image2);
        adapter.addCardFragment("Space", "Amazing space views", R.drawable.image3);

        // Set up page transformer for 3D effect
        viewPager.setPageTransformer(new CarouselPageTransformer());

        // Set off-screen page limit to keep all fragments alive
        viewPager.setOffscreenPageLimit(3);

        // Set up indicators
        indicators = new View[]{
                findViewById(R.id.indicator1),
                findViewById(R.id.indicator2),
                findViewById(R.id.indicator3)
        };

        // Set up page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                adapter.setCurrentPosition(position);
            }
        });

        // Set initial state
        adapter.setCurrentPosition(0);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackground(getDrawable(
                    i == position ? R.drawable.indicator_active : R.drawable.indicator_inactive
            ));
        }
    }
}