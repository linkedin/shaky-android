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
import android.os.Bundle;
import android.view.View;

import com.jraska.falcon.Falcon;
import com.squareup.seismic.ShakeDetector;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    private Context appContext;
    private long lastShakeTime;
    private CollectDataTask collectDataTask;

    Shaky(@NonNull Context context, @NonNull ShakeDelegate delegate) {
        appContext = context.getApplicationContext();
        this.delegate = delegate;
        shakeDetector = new ShakeDetector(this);

        shakeDetector.setSensitivity(getDetectorSensitivityLevel());

        IntentFilter filter = new IntentFilter();
        filter.addAction(SendFeedbackDialog.ACTION_START_FEEDBACK_FLOW);
        filter.addAction(FeedbackActivity.ACTION_END_FEEDBACK_FLOW);
        filter.addAction(ShakySettingDialog.UPDATE_SHAKY_SENSITIVITY);
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

    public void setSensitivity(@ShakeDelegate.SensitivityLevel int sensitivityLevel) {
        delegate.setSensitivityLevel(sensitivityLevel);
        shakeDetector.setSensitivity(getDetectorSensitivityLevel());
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

        Bundle arguments = new Bundle();
        arguments.putBoolean(SendFeedbackDialog.SHOULD_DISPLAY_SETTING_UI, delegate.shouldShowSettingsUI());
        arguments.putInt(ShakySettingDialog.SHAKY_CURRENT_SENSITIVITY, delegate.getSensitivityLevel());
        SendFeedbackDialog sendFeedbackDialog = new SendFeedbackDialog();
        sendFeedbackDialog.setArguments(arguments);
        sendFeedbackDialog.show(activity.getFragmentManager(), SEND_FEEDBACK_TAG);
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
        try {
            // Attempt to use Falcon to take the screenshot
            return Falcon.takeScreenshotBitmap(activity);
        } catch (Falcon.UnableToTakeScreenshotException exception) {
            // Fallback to using the default screenshot capture mechanism if Falcon does not work (e.g. if it has not
            // been updated to work on newer versions of Android yet)
            View view = activity.getWindow().getDecorView().getRootView();
            return Utils.capture(view);
        }
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
                } else if (ShakySettingDialog.UPDATE_SHAKY_SENSITIVITY.equals(intent.getAction())) {
                    setSensitivity(intent.getIntExtra(ShakySettingDialog.SHAKY_NEW_SENSITIVITY, ShakeDelegate.SENSITIVITY_MEDIUM));
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
        Intent intent = FeedbackActivity.newIntent(activity, result.getScreenshotUri(), result.getData(), delegate.sendIcon);
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
            fileProviderAttachments.add(Utils.getProviderUri(appContext, attachment));
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
