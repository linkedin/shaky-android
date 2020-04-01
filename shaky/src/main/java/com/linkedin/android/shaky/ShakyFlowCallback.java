/*
 * Copyright (C) 2020 LinkedIn Corp.
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

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Callbacks during the feedback submission flow.
 */
public interface ShakyFlowCallback {
    @IntDef({SHAKY_STARTED_BY_SHAKE, SHAKY_STARTED_MANUALLY})
    @Retention(RetentionPolicy.SOURCE)
    @interface ShakyStartedReason {
    }

    /**
     * The flow is started, because the device is shaken.
     */
    int SHAKY_STARTED_BY_SHAKE = 1;
    /**
     * The flow is started, because {@link Shaky#startFeedbackFlow()} is called.
     */
    int SHAKY_STARTED_MANUALLY = 2;

    @IntDef({
            SHAKY_FINISHED_TOO_FREQUENT,
            SHAKY_FINISHED_NO_RESUMED_ACTIVITY,
            SHAKY_FINISHED_ALREADY_STARTED,
            SHAKY_FINISHED_BY_USER,
            SHAKY_FINISHED_SENSITIVITY_UPDATED,
            SHAKY_FINISHED_SUBMITTED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ShakyFinishedReason {
    }

    /**
     * The flow is finished, because the previous shake was just recently detected.
     */
    int SHAKY_FINISHED_TOO_FREQUENT = 1;
    /**
     * The flow is finished, because there is no {@link Activity} resumed.
     */
    int SHAKY_FINISHED_NO_RESUMED_ACTIVITY = 2;
    /**
     * The flow is finished, because the flow is already started.
     */
    int SHAKY_FINISHED_ALREADY_STARTED = 3;
    /**
     * The flow is finished, because the user dismissed the dialog.
     */
    int SHAKY_FINISHED_BY_USER = 4;
    /**
     * The flow is finished, because the user updated Shaky sensitivity.
     */
    int SHAKY_FINISHED_SENSITIVITY_UPDATED = 5;
    /**
     * The flow is finished, and user has submitted the feedback through {@link ShakeDelegate#submit(Activity, Result)}.
     */
    int SHAKY_FINISHED_SUBMITTED = 6;

    /**
     * Called when the flow is started.
     */
    void onShakyStarted(@ShakyStartedReason int reason);

    /**
     * Called when the flow is finished.
     */
    void onShakyFinished(@ShakyFinishedReason int reason);

    /**
     * Called when the dialog that prompts the user to kick-off the feedback flow is shown.
     */
    void onUserPromptShown();

    /**
     * Called when Shaky starts collecting data.
     */
    void onCollectingData();

    /**
     * Called when {@link FeedbackActivity} is launched and the user is configuring the feedback.
     */
    void onConfiguringFeedback();
}
