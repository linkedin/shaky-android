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
package com.linkedin.android.shaky.app;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.linkedin.android.shaky.EmailShakeDelegate;
import com.linkedin.android.shaky.Shaky;
import com.linkedin.android.shaky.ShakyFlowCallback;

/**
 * Hello world example.
 */
public class ShakyApplication extends Application {
    private static final String TAG = ShakyApplication.class.getSimpleName();

    private Shaky shaky;
    private @StyleRes Integer theme = null;
    private @StyleRes Integer popupTheme = null;

    @Override
    public void onCreate() {
        super.onCreate();
        shaky = Shaky.with(this, new EmailShakeDelegate("hello@world.com") {
            @Override
            @Nullable
            public Integer getTheme() {
                return theme;
            }

            @Override
            @Nullable
            public Integer getPopupTheme() {
                return popupTheme;
            }

        }, null, new ShakyFlowCallback() {
            @Override
            public void onShakyStarted(@ShakyFlowCallback.ShakyStartedReason int reason) {
                Log.d(TAG, "onShakyStarted: " + reason);
            }

            @Override
            public void onShakyFinished(@ShakyFinishedReason int reason) {
                Log.d(TAG, "onShakyFinished: " + reason);
            }

            @Override
            public void onUserPromptShown() {
                Log.d(TAG, "onUserPromptShown");
            }

            @Override
            public void onCollectingData() {
                Log.d(TAG, "onCollectingData");
            }

            @Override
            public void onConfiguringFeedback() {
                Log.d(TAG, "onConfiguringFeedback");
            }
        });
    }

    @NonNull
    public Shaky getShaky() {
        return shaky;
    }

    public void setShakyTheme(@StyleRes Integer theme) {
        this.theme = theme;
    }

    public void setShakyPopupTheme(@StyleRes Integer theme) {
        this.popupTheme = theme;
    }
}
