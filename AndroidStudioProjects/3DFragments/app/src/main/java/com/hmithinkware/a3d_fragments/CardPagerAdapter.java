package com.hmithinkware.a3d_fragments;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class CardPagerAdapter extends FragmentStateAdapter {
    private final List<CardFragment> fragments = new ArrayList<>();
    private int currentPosition = 0;

    public CardPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addCardFragment(String title, String description, int imageResId) {
        fragments.add(CardFragment.newInstance(fragments.size(), title, description, imageResId));
        notifyItemInserted(fragments.size() - 1);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public void setCurrentPosition(int position) {
        // First, turn off glow for the previous position
        if (currentPosition < fragments.size()) {
            fragments.get(currentPosition).setGlowVisibility(false);
        }

        // Update current position and turn on glow
        currentPosition = position;
        if (currentPosition < fragments.size()) {
            fragments.get(currentPosition).setGlowVisibility(true);
        }
    }
}