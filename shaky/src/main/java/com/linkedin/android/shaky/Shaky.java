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

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import com.squareup.seismic.ShakeDetector;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Listens for a shake and then starts the feedback submission flow.
 * <p/>
 * Note: this class supports attaching and detaching activities for configuration changes, but if
 * the {@link CollectDataTask} completes while there is no activity attached, the feedback
 * flow will be terminated.
 */
public class Shaky implements ShakeDetector.Listener {

    private static final String SEND_FEEDBACK_TAG = "SendFeedback";
    private static final String COLLECT_DATA_TAG = "CollectFeedbackData";

    private static final long SHAKE_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(5);

    private final ShakeDelegate delegate;
    private final ShakeDetector shakeDetector;

    private Activity activity;
    private long lastShakeTime;
    private CollectDataTask collectDataTask;

    Shaky(@NonNull Context context, @NonNull ShakeDelegate delegate) {
        Context appContext = context.getApplicationContext();
        this.delegate = delegate;
        shakeDetector = new ShakeDetector(this);

        shakeDetector.setSensitivity(getDetectorSensitivityLevel());

        IntentFilter filter = new IntentFilter();
        filter.addAction(SendFeedbackDialog.ACTION_START_FEEDBACK_FLOW);
        filter.addAction(FeedbackActivity.ACTION_END_FEEDBACK_FLOW);
        LocalBroadcastManager.getInstance(appContext).registerReceiver(createReceiver(), filter);
    }

    /**
     * Entry point into this API.
     *
     * Registers shaky to the current application.
     */
    @NonNull
    public static Shaky with(@NonNull Application application, @NonNull ShakeDelegate delegate) {
        Shaky shaky = new Shaky(application.getApplicationContext(), delegate);
        LifecycleCallbacks lifecycleCallbacks = new LifecycleCallbacks(shaky);
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        return shaky;
    }

    public void startFeedbackFlow() {
        if (!canStartFeedbackFlow()) {
            return;
        }

        doStartFeedbackFlow();
    }

    void setActivity(@Nullable Activity activity) {
        this.activity = activity;
        if (activity != null) {
            start();
            // we're attaching to a new Activity instance
            // make sure the UI is in sync with the AsyncTask state
            dismissCollectFeedbackDialogIfNecessary();
        } else {
            stop();
        }
    }

    private void doStartFeedbackFlow() {
        new CollectDataDialog().show(activity.getFragmentManager(), COLLECT_DATA_TAG);

        collectDataTask = new CollectDataTask(activity, delegate, createCallback());
        collectDataTask.execute(getScreenshotBitmap());
    }

    /**
     * Start listening for shakes. Should be called when the {@link Activity} is resumed.
     */
    private void start() {
        if (!delegate.isEnabled()) {
            return;
        }

        shakeDetector.start((SensorManager) activity.getSystemService(Context.SENSOR_SERVICE));
    }

    /**
     * Stop listening for shakes. Safe to call when already stopped. Should be called when the
     * {@link Activity} is paused.
     */
    private void stop() {
        shakeDetector.stop();
    }

    @Override
    public void hearShake() {
        if (shouldIgnoreShake() || !canStartFeedbackFlow()) {
            return;
        }

        new SendFeedbackDialog().show(activity.getFragmentManager(), SEND_FEEDBACK_TAG);
    }

    /**
     * @return true if a shake happened in the last {@link #SHAKE_COOLDOWN_MS}, false otherwise.
     */
    @VisibleForTesting
    boolean shouldIgnoreShake() {
        long now = System.currentTimeMillis();
        if (now < lastShakeTime + SHAKE_COOLDOWN_MS) {
            return true;
        }
        lastShakeTime = now;
        return false;
    }

    /**
     * @return true if we're not currently in a feedback flow, false otherwise.
     */
    @VisibleForTesting
    boolean canStartFeedbackFlow() {
        return activity != null
                && !(activity instanceof FeedbackActivity)
                && activity.getFragmentManager().findFragmentByTag(SEND_FEEDBACK_TAG) == null
                && activity.getFragmentManager().findFragmentByTag(COLLECT_DATA_TAG) == null;
    }

    @Nullable
    @UiThread
    private Bitmap getScreenshotBitmap() {
        View view = activity.getWindow().getDecorView().getRootView();
        return Utils.capture(view);
    }

    private void dismissCollectFeedbackDialogIfNecessary() {
        if (collectDataTask != null || activity == null) {
            return;
        }

        CollectDataDialog dialog = (CollectDataDialog) activity.getFragmentManager()
                                                               .findFragmentByTag(COLLECT_DATA_TAG);

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Receiver for start and end of feedback flow. When the user accepts the
     * dialog, starts the collect data background task.
     */
    @NonNull
    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SendFeedbackDialog.ACTION_START_FEEDBACK_FLOW.equals(intent.getAction())) {
                    if (activity != null) {
                        doStartFeedbackFlow();
                    }
                } else if (FeedbackActivity.ACTION_END_FEEDBACK_FLOW.equals(intent.getAction())) {
                    delegate.submit(activity, unpackResult(intent));
                }
            }
        };
    }

    /**
     * Callback for after the background task finishes gathering information.
     */
    @NonNull
    private CollectDataTask.Callback createCallback() {
        return new CollectDataTask.Callback() {
            @Override
            public void onDataReady(@Nullable Result result) {
                boolean shouldStartFeedbackActivity = activity != null && collectDataTask != null;
                collectDataTask = null;
                dismissCollectFeedbackDialogIfNecessary();

                if (shouldStartFeedbackActivity) {
                    startFeedbackActivity(result == null ? new Result() : result);
                }
            }
        };
    }

    /**
     * Launches the main feedback activity with the bundle extra data.
     */
    private void startFeedbackActivity(@NonNull Result result) {
        Intent intent = FeedbackActivity.newIntent(activity, result.getScreenshotUri(), result.getData());
        activity.startActivity(intent);
    }

    private Result unpackResult(Intent intent) {
        Result result = new Result(intent.getBundleExtra(FeedbackActivity.USER_DATA));
        result.setScreenshotUri((Uri) intent.getParcelableExtra(FeedbackActivity.SCREENSHOT_URI));
        result.setTitle(intent.getStringExtra(FeedbackActivity.TITLE));
        result.setMessage(intent.getStringExtra(FeedbackActivity.MESSAGE));

        // add file provider data to all attachments
        ArrayList<Uri> fileProviderAttachments = new ArrayList<>();
        for (Uri attachment : result.getAttachments()) {
            fileProviderAttachments.add(Utils.getProviderUri(activity, attachment));
        }
        result.setAttachments(fileProviderAttachments);

        return result;
    }

    @VisibleForTesting
    public int getDetectorSensitivityLevel() {
        int delegateLevel = delegate.getSensitivityLevel();

        if (delegateLevel == ShakeDelegate.SENSITIVITY_LIGHT) {
            return ShakeDetector.SENSITIVITY_LIGHT;
        } else if (delegateLevel == ShakeDelegate.SENSITIVITY_HARD) {
            return ShakeDetector.SENSITIVITY_HARD;
        } else {
            return ShakeDetector.SENSITIVITY_MEDIUM;
        }
    }
}
