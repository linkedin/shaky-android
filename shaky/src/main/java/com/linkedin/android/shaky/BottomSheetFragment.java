package com.linkedin.android.shaky;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private static final String KEY_THEME = "theme";
    @Nullable private LayoutInflater inflater;

    @NonNull
    public static BottomSheetFragment newInstance(@Nullable @StyleRes Integer theme) {
        BottomSheetFragment fragment = new BottomSheetFragment();
        Bundle args = new Bundle();
        if (theme != null) {
            args.putInt(KEY_THEME, theme);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTheme() {
        return getArguments().getInt(KEY_THEME, FeedbackActivity.MISSING_RESOURCE);
    }

    @Override
    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = Utils.applyThemeToInflater(inflater,
                getArguments().getInt(KEY_THEME, FeedbackActivity.MISSING_RESOURCE));
        return this.inflater.inflate(R.layout.shaky_feedback_bottomsheet, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomSheetFeedbackTypeAdapter adapter = new BottomSheetFeedbackTypeAdapter(inflater, getData());

        RecyclerView recyclerView = view.findViewById(R.id.shaky_recyclerView_bottomsheet);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @NonNull
    private BottomSheetFeedbackItem[] getData() {
        return new BottomSheetFeedbackItem[]{
                new BottomSheetFeedbackItem(
                        getString(R.string.shaky_row1_title),
                        getString(R.string.shaky_row1_subtitle),
                        R.drawable.shaky_img_magnifying_glass_56dp,
                        BottomSheetFeedbackItem.BUG,
                        ActionConstants.ACTION_START_BUG_REPORT
                ),
                new BottomSheetFeedbackItem(
                        getString(R.string.shaky_row3_title),
                        getString(R.string.shaky_row3_subtitle),
                        R.drawable.shaky_img_message_bubbles_56dp,
                        BottomSheetFeedbackItem.GENERAL,
                        ActionConstants.ACTION_START_FEEDBACK_FLOW
                ),
                new BottomSheetFeedbackItem(
                        getString(R.string.shaky_dismiss_title),
                        getString(R.string.shaky_dismiss_subtitle),
                        R.drawable.shaky_dismiss,
                        BottomSheetFeedbackItem.DISMISS,
                        ActionConstants.ACTION_DISMISS
                ),
        };
    }
}
