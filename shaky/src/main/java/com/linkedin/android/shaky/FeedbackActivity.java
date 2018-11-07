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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * The main activity used capture and send feedback.
 */
public class FeedbackActivity extends AppCompatActivity {

    static final String ACTION_END_FEEDBACK_FLOW = "EndFeedbackFlow";

    static final String SCREENSHOT_URI = "screenshotUri";
    static final String MESSAGE = "message";
    static final String TITLE = "title";
    static final String USER_DATA = "userData";
    static final String EXTRA_ATTACHMENTS = "extraAttachments";
    static final String ADD_ATTACHMENT = "addAttachment";

    private Uri imageUri;
    private @FeedbackItem.FeedbackType int feedbackType;
    private Bundle userData;
    private boolean addAttachmentShown;

    // Replace a fragment won't call fragment.onSaveInstanceState() so we need to have a activity scope states
    // to maintain the fragment's states, such as attachments
    final Bundle fragmentStates = new Bundle();

    @NonNull
    public static Intent newIntent(@NonNull Context context,
                                   @Nullable Uri screenshotUri,
                                   @Nullable Bundle userData,
                                   boolean addAttachmentShown) {
        Intent intent = new Intent(context, FeedbackActivity.class);
        intent.putExtra(SCREENSHOT_URI, screenshotUri);
        intent.putExtra(USER_DATA, userData);
        intent.putExtra(ADD_ATTACHMENT, addAttachmentShown);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.shaky_feedback);

        imageUri = getIntent().getParcelableExtra(SCREENSHOT_URI);
        userData = getIntent().getBundleExtra(USER_DATA);
        addAttachmentShown = getIntent().getBooleanExtra(ADD_ATTACHMENT, false);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.shaky_fragment_container, new SelectFragment())
                    .commit();
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

    /**
     * Attaches this intent's extras to the fragment and transitions to the next fragment.
     *
     * @param fragment Fragment the fragment to swap to
     */
    private void changeToFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.shaky_fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    /**
     * Swap the view container for a form fragment, restores the previous fragment if one exists.
     */
    private void startFormFragment(@FeedbackItem.FeedbackType int feedbackType) {
        String title = getString(getTitleResId(feedbackType));
        String hint = getString(getHintResId(feedbackType));
        changeToFragment(FormFragment.newInstance(title, hint, imageUri, addAttachmentShown));
    }

    /**
     * Swap the view container for a draw fragment, restores the previous fragment if one exists.
     */
    private void startDrawFragment() {
        changeToFragment(DrawFragment.newInstance(imageUri));
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

                startFormFragment(feedbackType);
                if (imageUri != null && feedbackType == FeedbackItem.BUG) {
                    startDrawFragment();
                }
            } else if (FormFragment.ACTION_EDIT_IMAGE.equals(intent.getAction())) {
                startDrawFragment();
            } else if (DrawFragment.ACTION_DRAWING_COMPLETE.equals(intent.getAction())) {
                onBackPressed();
            } else if (FormFragment.ACTION_SUBMIT_FEEDBACK.equals(intent.getAction())) {
                submitFeedbackIntent(intent.getStringExtra(FormFragment.EXTRA_USER_MESSAGE),
                                     intent.getParcelableArrayListExtra(FormFragment.EXTRA_ATTACHMENTS));
            }
        }
    };

    private void submitFeedbackIntent(@Nullable String userMessage,
                                      @Nullable ArrayList<Parcelable> attachments) {
        Intent intent = new Intent(ACTION_END_FEEDBACK_FLOW);

        intent.putExtra(SCREENSHOT_URI, imageUri);
        intent.putExtra(TITLE, getString(getTitleResId(feedbackType)));
        intent.putExtra(MESSAGE, userMessage);
        intent.putExtra(USER_DATA, userData);
        if (attachments != null && !attachments.isEmpty()) {
            intent.putParcelableArrayListExtra(EXTRA_ATTACHMENTS, attachments);
        }

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
