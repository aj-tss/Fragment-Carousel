package com.hmithinkware.a3d_fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CardFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE_RES_ID = "image_res_id";

    private int position;
    private String title;
    private String description;
    private int imageResId;

    public CardFragment() {
        // Required empty constructor
    }

    public static CardFragment newInstance(int position, String title, String description, int imageResId) {
        CardFragment fragment = new CardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_IMAGE_RES_ID, imageResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESCRIPTION);
            imageResId = getArguments().getInt(ARG_IMAGE_RES_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleTextView = view.findViewById(R.id.cardTitle);
        TextView descriptionTextView = view.findViewById(R.id.cardDescription);
        ImageView imageView = view.findViewById(R.id.cardImage);
        ImageView glowImageView = view.findViewById(R.id.cardGlow);

        titleTextView.setText(title);
        descriptionTextView.setText(description);
        imageView.setImageResource(imageResId);

        // The glow will be controlled by the adapter
    }

    public void setGlowVisibility(boolean visible) {
        if (getView() != null) {
            ImageView glowImageView = getView().findViewById(R.id.cardGlow);
            glowImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}