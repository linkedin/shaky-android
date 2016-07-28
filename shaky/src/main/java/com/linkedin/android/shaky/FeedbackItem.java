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

import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data wrapper for a single row in the select feedback type view.
 */
class FeedbackItem {
    @IntDef({
            BUG,
            FEATURE,
            GENERAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FeedbackType {}

    public static final int BUG = 0;
    public static final int FEATURE = 1;
    public static final int GENERAL = 2;

    @DrawableRes public final int icon;
    public final String title;
    public final String description;
    public final int feedbackType;

    public FeedbackItem(@NonNull String title, @NonNull String description, @DrawableRes int icon,
                        @FeedbackType int feedbackType) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.feedbackType = feedbackType;
    }
}
