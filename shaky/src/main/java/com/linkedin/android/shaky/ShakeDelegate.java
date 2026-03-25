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
import android.app.DialogFragment;

import androidx.annotation.IntDef;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Entry point into this API.
 *
 * This class contains methods that apps can override to customize the behavior of Shaky.
 */
public abstract class ShakeDelegate {
    @IntDef({
            SENSITIVITY_LIGHT,
            SENSITIVITY_MEDIUM,
            SENSITIVITY_HARD
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensitivityLevel {}

    public static final int SENSITIVITY_LIGHT = 22;
    public static final int SENSITIVITY_MEDIUM = 23;
    public static final int SENSITIVITY_HARD = 24;

    @SensitivityLevel
    private static int sensitivityLevel = SENSITIVITY_MEDIUM;
    /** Allows user to customize the send icon at the end. Needs to be the menu itself because you cannot
     * change the send icon unless you send in a new menu, Also needs a value or else it crashes
     */
    @MenuRes protected int resMenu = FormFragment.DEFAULT_MENU;

    /**
     * Allows user to enable performing a custom action on detecting shake.
     */
    private boolean enableCustomHandlingOfShake = false;

    /**
     * @return true if shake detection should be enabled, false otherwise
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * @return true if a button to access Shaky settings should be shown in the UI, false otherwise
     */
    public boolean shouldShowSettingsUI() {
        return false;
    }

    /**
     * @return the title of the dialog that appears on shake, or null if the default title should be
     * used
     */
    @Nullable
    public String getDialogTitle() {
        return null;
    }

    /**
     * @return the message of the dialog that appears on shake, or null if the default message should
     * be used
     */
    @Nullable
    public String getDialogMessage() {
        return null;
    }

    /**
     * @return desired sensitivity level, defaults to 'medium'
     */
    @SensitivityLevel
    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    /**
     * Sets the shake sensitivity in memory. Please note that it is caller's responsibility to
     * persist the setting in storage such as {@link android.content.SharedPreferences}.
     *
     * @param sensitivityLevel
     */
    public void setSensitivityLevel(@SensitivityLevel int sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    /**
     * @return a custom theme to apply to the screens of the feedback flow. Look at shaky_attrs.xml
     * for possible attributes to set
     *
     * @return
     */
    @Nullable
    public Integer getTheme() {
        return null;
    }

    /**
     * @return a custom theme to apply to the pop-up that appears when the user shakes. Look at
     * shaky_attrs.xml for possible attributes to set
     */
    @Nullable
    public Integer getPopupTheme() {
        return null;
    }

    /**
     * @return a custom theme to apply to the bottom sheet that appears when the user shakes. Look
     * at shaky_attrs.xml for possible attributes to set
     */
    public int getBottomSheetTheme() {
        return R.style.ShakyBaseBottomSheetTheme;
    }

    /**
     * Called from the background thread during the feedback collection flow. This method
     * can be used to collect extra debug information to include in the feedback
     * submission, such as user data, app version, etc.
     */
    @WorkerThread
    public void collectData(@NonNull Activity activity, @NonNull Result data) {
    }

    /**
     * @return a custom dialog to be shown before the feedback flow
     */
    @Nullable
    public DialogFragment getCustomDialog() {
        return null;
    }

    /**
     * Returns whether custom handling of shake events is enabled.
     *
     * @return true if custom handling of shake is enabled, false otherwise
     */
    public boolean isCustomHandlingOfShakeEnabled() {
        return enableCustomHandlingOfShake;
    }

    /**
     * Enables or disables custom handling of shake events.
     *
     * @param enableCustomHandlingOfShake true to enable custom handling, false to disable
     */
    public void enableCustomHandlingOfShake(boolean enableCustomHandlingOfShake) {
        this.enableCustomHandlingOfShake = enableCustomHandlingOfShake;
    }

    /**
     * Called immediately when shake is detected and custom handling is enabled.
     *
     * @param activity the current activity
     * @deprecated Use {@link #performCustomActionOnShake(Activity, Result)} instead.
     *             This method is called before data collection and does not provide access
     *             to screenshots or collected debug data. The new method is called after
     *             data collection completes with full access to the Result object.
     */
    @Deprecated
    public void performCustomActionOnShake(@NonNull Activity activity) {
        // Default: do nothing
    }

    /**
     * Called after data collection is complete when custom shake handling is enabled.
     * Override this method to perform custom actions with access to collected data including
     * screenshots, debug info, and attachments.
     *
     * @param activity the current activity
     * @param result the collected data including screenshots, attachments, and debug info
     */
    public void performCustomActionOnShake(@NonNull Activity activity, @NonNull Result result) {
        // Default implementation: fall back to the deprecated method for backward compatibility
        performCustomActionOnShake(activity);
    }

    /**
     * @return if the dialog should be shown on shake or the shake-to-feedback bottom sheet.
     */
    public boolean shouldUseBottomSheet() {
        return true;
    }

    /**
     * Controls whether multi-window screenshot capture is enabled.
     * When enabled, uses PixelCopy API to capture multiple windows (activity, dialogs, bottom sheets)
     * separately. This handles hardware bitmaps correctly but may result in multiple screenshot attachments.
     * When disabled (default), uses the original single-screenshot fallback behavior for backward
     * compatibility.
     *
     * @return true to enable multi-window capture, false to use single-screenshot fallback (default)
     */
    public boolean enableMultiWindowCapture() {
        return false;
    }

    /**
     * Called when the user submits the Feedback form. Creates and starts an email Intent.
     * This method can be overridden to send data to a custom URL endpoint, etc.
     */
    public abstract void submit(@NonNull Activity activity, @NonNull Result result);

    /**
     * Called when the user completes edit screenshot action. Provides edited screenshot Uri
     * in result.
     * This method can be overridden to send edited screenshot to the expected endpoint, etc.
     */
    public void submitScreenshot(@NonNull Activity activity, @NonNull Result result) {
    }
}
