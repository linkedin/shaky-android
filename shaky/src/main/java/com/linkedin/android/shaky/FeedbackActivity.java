/**
 * Copyright (C) 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * The main activity used capture and send feedback.
 */
public class FeedbackActivity extends AppCompatActivity {

    static final String ACTION_END_FEEDBACK_FLOW = "EndFeedbackFlow";
    static final String ACTION_ACTIVITY_CLOSED_BY_USER = "ActivityClosedByUser";

    static final String SCREENSHOT_URI = "screenshotUri";
    static final String MESSAGE = "message";
    static final String TITLE = "title";
    static final String USER_DATA = "userData";
    static final String RES_MENU = "resMenu";
    static final String SUBCATEGORY = "subcategory";
    static final String THEME = "theme";
    private static final String ACTION = "ACTION_THAT_STARTED_THE_ACTIVITY";
    static final int MISSING_RESOURCE = 0;

    private Uri imageUri;
    private @FeedbackItem.FeedbackType int feedbackType;
    private Bundle userData;
    private @MenuRes int resMenu;
    private @StyleRes Integer customTheme;

    @NonNull
    public static Intent newIntent(@NonNull Context context,
                                   @Nullable Uri screenshotUri,
                                   @Nullable Bundle userData,
                                   @MenuRes int resMenu,
                                   @Nullable String actionThatStartedTheActivity,
                                   @StyleRes int theme) {
        Intent intent = new Intent(context, FeedbackActivity.class);
        intent.putExtra(SCREENSHOT_URI, screenshotUri);
        intent.putExtra(USER_DATA, userData);
        intent.putExtra(RES_MENU, resMenu);
        intent.putExtra(ACTION, actionThatStartedTheActivity);
        intent.putExtra(THEME, theme);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setTheme(R.style.ShakyBaseTheme);
        ViewCompat.setOnApplyWindowInsetsListener(
            getWindow().findViewById(android.R.id.content),
            (v, insets) -> {
                Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            }
        );
        setContentView(R.layout.shaky_feedback);

        customTheme = getIntent().getIntExtra(THEME, MISSING_RESOURCE);
        imageUri = getIntent().getParcelableExtra(SCREENSHOT_URI);
        userData = getIntent().getBundleExtra(USER_DATA);
        resMenu = getIntent().getIntExtra(RES_MENU, FormFragment.DEFAULT_MENU);
        String action = getIntent().getStringExtra(ACTION);

        if (savedInstanceState == null && action != null) {
            if (action.equals(ActionConstants.ACTION_START_FEEDBACK_FLOW)) {
//                getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(R.id.shaky_fragment_container, SelectFragment.newInstance(customTheme))
//                    .commit();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.shaky_fragment_container, BottomSheetFragment.newInstance(customTheme))
                        .commit();
            } else if (action.equals(ActionConstants.ACTION_START_BUG_REPORT)) {
                startFormFragment(FeedbackItem.BUG, false);
                if (imageUri != null) {
                    startDrawFragment();
                }
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FeedbackTypeAdapter.ACTION_FEEDBACK_TYPE_SELECTED);
        filter.addAction(FormFragment.ACTION_SUBMIT_FEEDBACK);
        filter.addAction(FormFragment.ACTION_EDIT_IMAGE);
        filter.addAction(DrawFragment.ACTION_DRAWING_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ACTION_ACTIVITY_CLOSED_BY_USER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onBackPressed();
    }

    /**
     * Attaches this intent's extras to the fragment and transitions to the next fragment.
     *
     * @param fragment Fragment the fragment to swap to
     * @param shouldAddToBackStack if the fragment should be added to backstack or not
     */
    private void changeToFragment(@NonNull Fragment fragment, boolean shouldAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.shaky_fragment_container, fragment);

        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }

        fragmentTransaction.commit();
    }

    /**
     * Swap the view container for a form fragment, restores the previous fragment if one exists.
     *
     * @param feedbackType the FeedbackType for which to start the fragment
     * @param shouldAddToBackStack if the fragment should be added to backstack or not
     */
    private void startFormFragment(
        @FeedbackItem.FeedbackType int feedbackType,
        boolean shouldAddToBackStack
    ) {
        String title = getString(getTitleResId(feedbackType));
        String hint = getString(getHintResId(feedbackType));
        String[] subtypes = null;
        if (feedbackType == FeedbackItem.BUG) {
            subtypes = new String[]{Subcategories.Bug.CRASH, Subcategories.Bug.NON_FATAL};
        }
        changeToFragment(new FormFragment.Builder(title, hint)
            .setScreenshotUri(imageUri)
            .setMenu(resMenu)
            .setSubtypes(subtypes != null ? R.array.shaky_bug_subcategories : null, subtypes)
            .setTheme(customTheme)
            .build(),
            shouldAddToBackStack);
    }

    /**
     * Swap the view container for a draw fragment, restores the previous fragment if one exists.
     */
    private void startDrawFragment() {
        changeToFragment(DrawFragment.newInstance(imageUri, customTheme), true);
    }

    private void setFeedbackType(@FeedbackItem.FeedbackType int feedbackType) {
        this.feedbackType = feedbackType;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FeedbackTypeAdapter.ACTION_FEEDBACK_TYPE_SELECTED.equals(intent.getAction())) {
                @FeedbackItem.FeedbackType int feedbackType =
                        intent.getIntExtra(FeedbackTypeAdapter.EXTRA_FEEDBACK_TYPE, FeedbackItem.GENERAL);

                setFeedbackType(feedbackType);

                startFormFragment(feedbackType, true);
                if (imageUri != null && feedbackType == FeedbackItem.BUG) {
                    startDrawFragment();
                }
            } else if (FormFragment.ACTION_EDIT_IMAGE.equals(intent.getAction())) {
                startDrawFragment();
            } else if (DrawFragment.ACTION_DRAWING_COMPLETE.equals(intent.getAction())) {
                onBackPressed();
            } else if (FormFragment.ACTION_SUBMIT_FEEDBACK.equals(intent.getAction())) {
                submitFeedbackIntent(intent.getStringExtra(FormFragment.EXTRA_USER_MESSAGE),
                    intent.getStringExtra(FormFragment.EXTRA_SUBCATEGORY));
            }
        }
    };

    private void submitFeedbackIntent(@Nullable String userMessage, @Nullable String subcategory) {
        Intent intent = new Intent(ACTION_END_FEEDBACK_FLOW);

        intent.putExtra(SCREENSHOT_URI, imageUri);
        intent.putExtra(TITLE, getString(getTitleResId(feedbackType)));
        intent.putExtra(MESSAGE, userMessage);
        intent.putExtra(USER_DATA, userData);
        intent.putExtra(SUBCATEGORY, subcategory);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        finish();
    }

    @StringRes
    private int getTitleResId(@FeedbackItem.FeedbackType int feedbackType) {
        switch (feedbackType) {
            case FeedbackItem.BUG:
                return R.string.shaky_bug_title;
            case FeedbackItem.FEATURE:
                return R.string.shaky_feature_title;
            default:
            case FeedbackItem.GENERAL:
                return R.string.shaky_general_title;
        }
    }

    @StringRes
    private int getHintResId(@FeedbackItem.FeedbackType int feedbackType) {
        switch (feedbackType) {
            case FeedbackItem.BUG:
                return R.string.shaky_bug_hint;
            case FeedbackItem.FEATURE:
                return R.string.shaky_feature_hint;
            default:
            case FeedbackItem.GENERAL:
                return R.string.shaky_general_hint;
        }
    }
}
