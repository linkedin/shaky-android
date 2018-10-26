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
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Simple {@link ShakeDelegate} implementation that sends an starts an email intent.
 */
public class EmailShakeDelegate extends ShakeDelegate {
    private String[] to;
    private int sensitivityLevel = ShakeDelegate.SENSITIVITY_MEDIUM;

    public EmailShakeDelegate(@NonNull String[] to) {
        this.to = to;
    }

    public EmailShakeDelegate(@NonNull String to) {
        this(new String[]{to});
    }

    @Override
    public final void submit(@NonNull Activity activity, @NonNull Result result) {
        activity.startActivity(onSubmit(result));
    }

    @Override
    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    /**
     * Optionally override sensitivityLevel to one of ShakeDelegate.SENSITIVITY_*
     */
    public void setSensitivityLevel(@ShakeDelegate.SensitivityLevel int newLevel) {
        sensitivityLevel = newLevel;
    }

    /**
     * Creates the email {@link Intent} and attaches all attachments.
     * Subclasses should override this method to customize the email Intent.
     */
    public Intent onSubmit(@NonNull Result result) {
        return createEmailIntent(to, result.getTitle(), result.getMessage(), result.getAttachments());
    }

    /**
     * Note: All attachments must use FileProvider.getProviderUri so they can be shared with external mail apps.
     */
    @NonNull
    private Intent createEmailIntent(@Nullable String[] to, @Nullable String subject, @Nullable String text, @Nullable ArrayList<Uri> attachments) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, to);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if (attachments != null) {
            if (attachments.size() == 1) {
                intent.putExtra(Intent.EXTRA_STREAM, attachments.get(0));
            } else {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            }
        }
        return intent;
    }
}
