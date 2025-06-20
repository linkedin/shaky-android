package com.linkedin.android.shaky.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * A bottom sheet dialog that covers 50% of the screen with sample text.
 * Includes elevation and styling to appear as an overlay.
 */
public class SampleBottomSheetDialog extends BottomSheetDialogFragment {

    private static final float ELEVATION_DP = 16f;
    private static final float CORNER_RADIUS_DP = 16f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply a style that supports elevation
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Create a CardView to contain our content and provide elevation
        CardView cardView = new CardView(requireContext());
        cardView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Set CardView properties for elevation effect
        cardView.setCardElevation(dpToPx(ELEVATION_DP));
        cardView.setRadius(dpToPx(CORNER_RADIUS_DP));
        cardView.setCardBackgroundColor(Color.WHITE);

        // Create content TextView
        TextView textView = new TextView(getContext());
        textView.setPadding((int)dpToPx(24), (int)dpToPx(32), (int)dpToPx(24), (int)dpToPx(32));
        textView.setText("This is a sample bottom sheet dialog that covers 50% of the screen.\n\n" +
                "It demonstrates how Shaky can capture overlay screens using the MediaProjection API.\n\n" +
                "This dialog has elevation to make it look like an overlay!\n\n" +
                "Try shaking the device or using the feedback button while this sheet is visible!");
        textView.setTextSize(18);

        // Add the TextView to the CardView
        cardView.addView(textView);

        return cardView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the height to 50% of screen when the view is created
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // Get the parent view of the bottom sheet
            View bottomSheet = (View) view.getParent();
            // Set up the bottom sheet behavior
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            // Set the height to 50% of screen
            behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Additional setup for the dialog window for elevation effect
        if (getDialog() != null && getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();

            // Find the bottom sheet view
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set background with rounded top corners
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(Color.WHITE);
                drawable.setCornerRadii(new float[]{
                        dpToPx(CORNER_RADIUS_DP), dpToPx(CORNER_RADIUS_DP),  // top-left
                        dpToPx(CORNER_RADIUS_DP), dpToPx(CORNER_RADIUS_DP),  // top-right
                        0, 0,  // bottom-right
                        0, 0   // bottom-left
                });

                // Apply the drawable and elevation
                ViewCompat.setBackground(bottomSheet, drawable);
                ViewCompat.setElevation(bottomSheet, dpToPx(ELEVATION_DP));
            }
        }
    }

    /**
     * Convert dp to pixels
     */
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
