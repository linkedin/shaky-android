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
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.util.Log;

import java.io.File;

/**
 * Background task to collect user data. Used with {@link CollectDataDialog}.
 */
class CollectDataTask extends AsyncTask<Bitmap, Void, Result> {

    private static final String TAG = CollectDataTask.class.getSimpleName();

    private static final String SCREENSHOT_DIRECTORY = "/screenshots";

    private final Activity activity;
    private final ShakeDelegate delegate;
    private final Callback callback;

    CollectDataTask(@NonNull Activity activity,
                    @NonNull ShakeDelegate delegate,
                    @NonNull Callback callback) {
        this.activity = activity;
        this.delegate = delegate;
        this.callback = callback;
    }

    @Override
    protected Result doInBackground(Bitmap... params) {
        String screenshotDirectoryRoot = getScreenshotDirectoryRoot(activity);
        if (screenshotDirectoryRoot == null) {
            return null;
        }

        File screenshotDirectory = new File(screenshotDirectoryRoot);

        // delete any old screenshots that we may have left lying around
        if (screenshotDirectory.exists()) {
            File[] oldScreenshots = screenshotDirectory.listFiles();
            for (File oldScreenshot : oldScreenshots) {
                if (!oldScreenshot.delete()) {
                    Log.e(TAG, "Could not delete old screenshot:" + oldScreenshot);
                }
            }
        }

        Result result = new Result();

        // Process all bitmaps
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                Bitmap bitmap = params[i];
                if (bitmap != null) {
                    File screenshotFile = Utils.writeBitmapToDirectory(bitmap, screenshotDirectory);

                    if (screenshotFile != null) {
                        Uri screenshotUri = Uri.fromFile(screenshotFile);

                        // First bitmap becomes the main screenshot (for UI preview)
                        if (i == 0) {
                            result.setScreenshotUri(screenshotUri);
                        } else {
                            // Subsequent screenshots (dialogs/bottom sheets) are only attachments
                            result.getAttachments().add(screenshotUri);
                        }
                    } else {
                        Log.e(TAG, "Failed to write bitmap " + i + " to file");
                    }
                }
            }
            Log.d(TAG, "Saved " + result.getAttachments().size() + " screenshot(s) total");
        }

        delegate.collectData(activity, result);
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);

        callback.onDataReady(result);
    }

    @Nullable
    @WorkerThread
    private static String getScreenshotDirectoryRoot(@NonNull Context context) {
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }
        return filesDir.getAbsolutePath() + SCREENSHOT_DIRECTORY;
    }

    interface Callback {
        void onDataReady(@Nullable Result result);
    }
}
