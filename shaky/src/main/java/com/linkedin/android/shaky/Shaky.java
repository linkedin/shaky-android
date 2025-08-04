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
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private static final String CUSTOM_DIALOG_TAG = "CustomDialog";

    private static final long SHAKE_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(5);
    private final ShakeDelegate delegate;
    private final ShakeDetector shakeDetector;
    @Nullable
    private final ShakyFlowCallback shakyFlowCallback;

    // Screen capture related fields
    private ScreenCaptureManager screenCaptureManager;
    private boolean useMediaProjection = false;
    private boolean isScreenCaptureInProgress = false;
    private CollectDataTask.Callback pendingDataCollectionCallback = null;

    @Nullable
    private Activity activity;
    private Context appContext;
    private long lastShakeTime;
    private CollectDataTask collectDataTask;
    private String actionThatStartedTheActivity;

    Shaky(@NonNull Context context, @NonNull ShakeDelegate delegate, @Nullable ShakyFlowCallback callback) {
        appContext = context.getApplicationContext();
        this.delegate = delegate;
        this.shakyFlowCallback = callback;
        shakeDetector = new ShakeDetector(this);

        shakeDetector.setSensitivity(getDetectorSensitivityLevel());

        // Initialize the screen capture manager
        screenCaptureManager = new ScreenCaptureManager(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ActionConstants.ACTION_START_FEEDBACK_FLOW);
        filter.addAction(ActionConstants.ACTION_START_BUG_REPORT);
        filter.addAction(ActionConstants.ACTION_START_GENERAL_FEEDBACK);
        filter.addAction(ActionConstants.ACTION_DIALOG_DISMISSED_BY_USER);
        filter.addAction(FeedbackActivity.ACTION_END_FEEDBACK_FLOW);
        filter.addAction(FeedbackActivity.ACTION_ACTIVITY_CLOSED_BY_USER);
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
        return with(application, delegate, null);
    }

    /**
     * Entry point into this API.
     *
     * Registers shaky to the current application.
     */
    @NonNull
    public static Shaky with(@NonNull Application application,
                             @NonNull ShakeDelegate delegate,
                             @Nullable ShakyFlowCallback callback) {
        Shaky shaky = new Shaky(application.getApplicationContext(), delegate, callback);
        LifecycleCallbacks lifecycleCallbacks = new LifecycleCallbacks(shaky);
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        return shaky;
    }

    /**
     * Start the shaky feedback flow manually.
     */
    public void startFeedbackFlow() {
        startFeedbackFlow(null);
    }

    /**
     * Start shaky manually for a custom flow.
     *
     * @param action the flow to start. If null, starts the feedback flow by default. Otherwise
     *               starts the custom flow (if valid).
     */
    public void startFeedbackFlow(@Nullable String action) {
        if (shakyFlowCallback != null) {
            shakyFlowCallback.onShakyStarted(ShakyFlowCallback.SHAKY_STARTED_MANUALLY);
        }
        if (!canStartFeedbackFlow()) {
            return;
        }
        if (action != null) {
            if (isValidStartAction(action)) {
                actionThatStartedTheActivity = action;
            }
        } else {
            actionThatStartedTheActivity = ActionConstants.ACTION_START_FEEDBACK_FLOW;
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

    /**
     * Enable or disable MediaProjection API for taking screenshots.
     * When enabled, Shaky will capture screenshots that include system overlays.
     *
     * @param useMediaProjection true to use MediaProjection, false to use default screenshot method
     */
    public void setUseMediaProjection(boolean useMediaProjection) {
        this.useMediaProjection = useMediaProjection;
    }

    /**
     * Handle activity result for MediaProjection permission request.
     * This must be called from the host activity's onActivityResult method.
     *
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @param data Intent data from onActivityResult
     * @return true if handled by Shaky, false otherwise
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (isScreenCaptureInProgress) {
            boolean handled = screenCaptureManager.handleActivityResult(requestCode, resultCode, data);
            if (handled) {
                isScreenCaptureInProgress = false;
            }
            return handled;
        }
        return false;
    }

    private void doStartFeedbackFlow() {
        new CollectDataDialog().show(activity.getFragmentManager(), COLLECT_DATA_TAG);
        if (shakyFlowCallback != null) {
            shakyFlowCallback.onCollectingData();
        }

        if (useMediaProjection) {
            // For MediaProjection API, we'll start the permission request first
            isScreenCaptureInProgress = true;
            screenCaptureManager.requestCapturePermission(activity, new ScreenCaptureManager.CaptureCallback() {
                @Override
                public void onCaptureComplete(Bitmap bitmap) {
                    // Once we have the bitmap, start the data collection task
                    collectDataTask = new CollectDataTask(activity, delegate, createCallback());
                    collectDataTask.execute(bitmap);
                }

                @Override
                public void onCaptureFailed() {
                    // If capture fails, continue with normal flow (without screenshot)
                    collectDataTask = new CollectDataTask(activity, delegate, createCallback());
                    collectDataTask.execute((Bitmap) null);
                }
            });
        } else {
            // Use the regular screenshot method
            collectDataTask = new CollectDataTask(activity, delegate, createCallback());
            collectDataTask.execute(getScreenshotBitmap());
        }
    }

    /**
     * Start listening for shakes. Should be called when the {@link Activity} is resumed.
     */
    private void start() {
        if (!delegate.isEnabled()) {
            return;
        }

        shakeDetector.start((SensorManager) activity.getSystemService(Context.SENSOR_SERVICE), SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Stop listening for shakes. Safe to call when already stopped. Should be called when the
     * {@link Activity} is paused.
     */
    private void stop() {
        shakeDetector.stop();
    }

    /**
     * Checks If the flow to start is a valid flow or not.
     * @param action the provided flow to start
     *
     * @return true if the flow is a valid flow.
     */
    private boolean isValidStartAction(String action) {
        return action.equals(ActionConstants.ACTION_START_FEEDBACK_FLOW)
                || action.equals(ActionConstants.ACTION_START_BUG_REPORT)
                || action.equals(ActionConstants.ACTION_START_GENERAL_FEEDBACK);
    }

    @Override
    public void hearShake() {
        launchShakeBottomSheet(ShakyFlowCallback.SHAKY_STARTED_BY_SHAKE);
    }

    private void launchShakeBottomSheet(@ShakyFlowCallback.ShakyStartedReason int reason) {
        if (shakyFlowCallback != null) {
            shakyFlowCallback.onShakyStarted(reason);
        }

        if ((reason != ShakyFlowCallback.SHAKY_STARTED_MANUALLY && shouldIgnoreShake()) || !canStartFeedbackFlow()) {
            return;
        }

        if (delegate.shouldUseBottomSheet()) {
            if (activity != null) {
                BottomSheetDialog bottomSheetDialog;
                bottomSheetDialog = new BottomSheetDialog(activity, delegate.getBottomSheetTheme());

                View sheetView = LayoutInflater.from(activity).inflate(R.layout.shaky_feedback_bottomsheet, null);

                RecyclerView recyclerView = sheetView.findViewById(R.id.shaky_recyclerView_bottomsheet);
                recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                recyclerView.setAdapter(
                        new BottomSheetFeedbackTypeAdapter(
                                getData(activity),
                                bottomSheetDialog,
                                delegate.getBottomSheetTheme())
                );

                bottomSheetDialog.setContentView(sheetView);
                bottomSheetDialog.show();
            }
        } else {
            launchShakePopupDialog();
        }

        if (shakyFlowCallback != null) {
            shakyFlowCallback.onUserPromptShown();
        }
    }

    private void launchShakePopupDialog() {
        Bundle arguments = new Bundle();
        if (delegate.getDialogTitle() != null) {
            arguments.putString(SendFeedbackDialog.CUSTOM_TITLE, delegate.getDialogTitle());
        }
        if (delegate.getDialogMessage() != null) {
            arguments.putString(SendFeedbackDialog.CUSTOM_MESSAGE, delegate.getDialogMessage());
        }
        if (delegate.getPopupTheme() != null) {
            arguments.putInt(SendFeedbackDialog.THEME, delegate.getPopupTheme());
        }
        arguments.putBoolean(SendFeedbackDialog.SHOULD_DISPLAY_SETTING_UI, delegate.shouldShowSettingsUI());
        arguments.putInt(ShakySettingDialog.SHAKY_CURRENT_SENSITIVITY, delegate.getSensitivityLevel());
        if (delegate.getCustomDialog() != null) {
            DialogFragment customDialog = delegate.getCustomDialog();
            if (delegate.getCustomDialog() instanceof SendFeedbackDialog) {
                customDialog.setArguments(arguments);
            }
            customDialog.show(activity.getFragmentManager(), CUSTOM_DIALOG_TAG);
        } else {
            SendFeedbackDialog sendFeedbackDialog = new SendFeedbackDialog();
            sendFeedbackDialog.setArguments(arguments);
            sendFeedbackDialog.show(activity.getFragmentManager(), SEND_FEEDBACK_TAG);
        }
    }

    @NonNull
    private BottomSheetFeedbackItem[] getData(@NonNull Activity activity) {
        return new BottomSheetFeedbackItem[]{
                new BottomSheetFeedbackItem(
                        activity.getString(R.string.shaky_row1_title),
                        activity.getString(R.string.shaky_row1_subtitle),
                        R.drawable.shaky_img_magnifying_glass_56dp,
                        BottomSheetFeedbackItem.BUG,
                        ActionConstants.ACTION_START_BUG_REPORT
                ),
                new BottomSheetFeedbackItem(
                        activity.getString(R.string.shaky_row3_title),
                        activity.getString(R.string.shaky_row3_subtitle),
                        R.drawable.shaky_img_message_bubbles_56dp,
                        BottomSheetFeedbackItem.GENERAL,
                        ActionConstants.ACTION_START_GENERAL_FEEDBACK
                ),
                new BottomSheetFeedbackItem(
                        activity.getString(R.string.shaky_dismiss_title),
                        activity.getString(R.string.shaky_dismiss_subtitle),
                        R.drawable.shaky_dismiss,
                        BottomSheetFeedbackItem.DISMISS,
                        ActionConstants.ACTION_DISMISS
                ),
        };
    }

    /**
     * @return true if a shake happened in the last {@link #SHAKE_COOLDOWN_MS}, false otherwise.
     */
    @VisibleForTesting
    boolean shouldIgnoreShake() {
        long now = System.currentTimeMillis();
        if (now < lastShakeTime + SHAKE_COOLDOWN_MS) {
            if (shakyFlowCallback != null) {
                shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_TOO_FREQUENT);
            }
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
        if (activity == null) {
            if (shakyFlowCallback != null) {
                shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_NO_RESUMED_ACTIVITY);
            }
            return false;
        }
        boolean canStart = !(activity instanceof FeedbackActivity)
                && activity.getFragmentManager().findFragmentByTag(SEND_FEEDBACK_TAG) == null
                && activity.getFragmentManager().findFragmentByTag(COLLECT_DATA_TAG) == null;
        if (!canStart && shakyFlowCallback != null) {
            shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_ALREADY_STARTED);
        }
        return canStart;
    }

    @Nullable
    @UiThread
    private Bitmap getScreenshotBitmap() {
        if (activity == null) {
            return null;
        }

        // If MediaProjection is not enabled, use the default method
        if (!useMediaProjection) {
            View view = activity.getWindow().getDecorView().getRootView();
            return Utils.capture(view, activity.getWindow());
        }

        // For MediaProjection, return null and handle it separately
        return null;
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
                if (ActionConstants.ACTION_START_FEEDBACK_FLOW.equals(intent.getAction())
                        || ActionConstants.ACTION_START_BUG_REPORT.equals(intent.getAction())
                        || ActionConstants.ACTION_START_GENERAL_FEEDBACK.equals(intent.getAction())) {
                    if (activity != null) {
                        actionThatStartedTheActivity = intent.getAction();
                        doStartFeedbackFlow();
                    }
                } else if (ActionConstants.ACTION_DIALOG_DISMISSED_BY_USER.equals(intent.getAction())
                        || FeedbackActivity.ACTION_ACTIVITY_CLOSED_BY_USER.equals(intent.getAction())) {
                    if (shakyFlowCallback != null) {
                        shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_BY_USER);
                    }
                } else if (FeedbackActivity.ACTION_END_FEEDBACK_FLOW.equals(intent.getAction())) {
                    if (activity != null) {
                        delegate.submit(activity, unpackResult(intent));
                    }
                    if (shakyFlowCallback != null) {
                        shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_SUBMITTED);
                    }
                } else if (ShakySettingDialog.UPDATE_SHAKY_SENSITIVITY.equals(intent.getAction())) {
                    setSensitivity(intent.getIntExtra(ShakySettingDialog.SHAKY_NEW_SENSITIVITY, ShakeDelegate.SENSITIVITY_MEDIUM));
                    if (shakyFlowCallback != null) {
                        shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_SENSITIVITY_UPDATED);
                    }
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
                    return;
                }

                if (shakyFlowCallback != null) {
                    shakyFlowCallback.onShakyFinished(ShakyFlowCallback.SHAKY_FINISHED_NO_RESUMED_ACTIVITY);
                }
            }
        };
    }

    /**
     * Launches the main feedback activity with the bundle extra data.
     */
    private void startFeedbackActivity(@NonNull Result result) {
        Intent intent = FeedbackActivity.newIntent(activity,
                result.getScreenshotUri(),
                result.getData(),
                delegate.resMenu,
                actionThatStartedTheActivity,
                delegate.getTheme() != null ? delegate.getTheme() : FeedbackActivity.MISSING_RESOURCE);
        activity.startActivity(intent);

        if (shakyFlowCallback != null) {
            shakyFlowCallback.onConfiguringFeedback();
        }
    }

    private Result unpackResult(Intent intent) {
        Result result = new Result(intent.getBundleExtra(FeedbackActivity.USER_DATA));
        result.setScreenshotUri((Uri) intent.getParcelableExtra(FeedbackActivity.SCREENSHOT_URI));
        result.setTitle(intent.getStringExtra(FeedbackActivity.TITLE));
        result.setMessage(intent.getStringExtra(FeedbackActivity.MESSAGE));
        result.setSubcategory(intent.getStringExtra(FeedbackActivity.SUBCATEGORY));

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

    /**
     * Start shake flow manually to launch bottom sheet
     */
    public void startShakeBottomSheetFlowManually() {
        launchShakeBottomSheet(ShakyFlowCallback.SHAKY_STARTED_MANUALLY);
    }
}
