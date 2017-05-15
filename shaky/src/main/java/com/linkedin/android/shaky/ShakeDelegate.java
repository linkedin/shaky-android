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
import android.support.annotation.WorkerThread;

/**
 * Entry point into this API.
 *
 * This class contains methods that apps can override to customize the behavior of Shaky.
 */
public abstract class ShakeDelegate {

    /**
     * @return true if shake detection should be enabled, false otherwise
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Called from the background thread during the feedback collection flow. This method
     * can be used to collect extra debug information to include in the feedback
     * submission, such as user data, app version, etc.
     */
    @WorkerThread
    public void collectData(Activity activity, Result data) {
    }

    /**
     * Called when the user submits the Feedback form. Creates and starts an email Intent.
     * This method can be overridden to send data to a custom URL endpoint, etc.
     */
    public abstract void submit(Activity activity, Result result);
}
