/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Background task to collect user data. Used with {@link CollectDataDialog}.
 */
class CollectDataTask extends AsyncTask<Bitmap, Void, Result> {

    private static final String TAG = CollectDataTask.class.getSimpleName();

    private static final String SCREENSHOT_DIRECTORY = "/screenshots";
    private static final String DEFAULT_OWNER_EMAIL = "defaultOwnerEmail";

    private static final int RGB_MAX = 256;

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

    @Nullable
    @WorkerThread
    private static String getScreenshotDirectoryRoot(@NonNull Context context) {
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }
        return filesDir.getAbsolutePath() + SCREENSHOT_DIRECTORY;
    }

    @Override
    protected Result doInBackground(Bitmap... params) {
        String screenshotDirectoryRoot = getScreenshotDirectoryRoot(activity);
        if (screenshotDirectoryRoot == null) {
            return null;
        }

        // delete any old screenshots that we may have left lying around
        File screenshotDirectory = new File(screenshotDirectoryRoot);
        if (screenshotDirectory.exists()) {
            File[] oldScreenshots = screenshotDirectory.listFiles();
            for (File oldScreenshot : oldScreenshots) {
                if (!oldScreenshot.delete()) {
                    Log.e(TAG, "Could not delete old screenshot:" + oldScreenshot);
                }
            }
        }

        Uri screenshotUri = null;

        Bitmap bitmap = params != null && params.length != 0 ? params[0] : null;
        if (bitmap != null) {
            File screenshotFile = Utils.writeBitmapToDirectory(bitmap, screenshotDirectory);
            screenshotUri = Uri.fromFile(screenshotFile);
        }

        Result result = new Result();
        result.setScreenshotUri(screenshotUri);
        delegate.collectData(activity, result);
        result.setSubViews(collectSubviewsIfNeeded(result.getData().getString(DEFAULT_OWNER_EMAIL)));
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);

        callback.onDataReady(result);
    }

    /**
     * For the root view, identifies the subviews and its owners and returns them if needed to
     * be shown in the selection screen
     */
    @WorkerThread
    private ArrayList<Subview> collectSubviewsIfNeeded(@Nullable String defaultOwnerEmail) {
        View contentView = activity.findViewById(android.R.id.content);
        ArrayList<Subview> subViews = new ArrayList<>();
        identifyVisibleSubviews(contentView, subViews, defaultOwnerEmail);
        return subViews;
    }

    /**
     * Goes through the view hierarchy of the given view, identifies the child views and
     * adds them to the subviews list
     */
    private void identifyVisibleSubviews(@Nullable View view, @NonNull List<Subview> subViews, @Nullable String defaultEmail) {
        if (view == null) {
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            if (childCount == 0) {
                // Check if the leaf view is visible on screen
                Subview subView = createSubview(view, defaultEmail);
                if (subView != null) {
                    subViews.add(subView);
                }
            } else {
                for (int i = 0; i < childCount; i++) {
                    identifyVisibleSubviews(viewGroup.getChildAt(i), subViews, defaultEmail);
                }
            }
        } else {
            Subview subView = createSubview(view, defaultEmail);
            if (subView != null) {
                subViews.add(subView);
            }
        }
    }

    /**
     * Creates a new subview object for the child view by identifying the bounds and owner of the view.
     */
    @Nullable
    private Subview createSubview(@NonNull View view, @Nullable String defaultEmail) {
        Object ownerTag = view.getTag(R.id.shaky_owner_tag);
        String owner = ownerTag != null ? String.valueOf(ownerTag) : null;
        if (!shouldShowSelection(owner, defaultEmail)) {
            return null;
        }

        // Check if the view is visible on screen
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) {
            return null;
        }

        Random random = new Random();
        int color = Color.rgb(random.nextInt(RGB_MAX), random.nextInt(RGB_MAX), random.nextInt(RGB_MAX));
        return new Subview(rect, owner, color, false);
    }

    /**
     * Returns if the screen is eligible to show the sub views in the screens
     * <p>
     * We would only show the sub views if there is a default owner and if there is at least
     * one subview which has a different owner than the default owner.
     */
    private boolean shouldShowSelection(@Nullable String ownerEmail, @Nullable String defaultEmail) {
        if (ownerEmail == null) {
            return false;
        }

        if (defaultEmail == null) {
            return true;
        }

        return !ownerEmail.equals(defaultEmail);
    }

    interface Callback {
        void onDataReady(@Nullable Result result);
    }
}
