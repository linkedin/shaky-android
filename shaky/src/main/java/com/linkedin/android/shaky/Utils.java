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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.WorkerThread;
import androidx.core.content.FileProvider;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

final class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static final String FILE_NAME_TEMPLATE = "%s_%s.jpg";
    private static final String BITMAP_PREFIX = "bitmap";
    private static final String FILE_PROVIDER_SUFFIX = ".fileprovider";

    private static final String SCREENSHOT_DIRECTORY = "/screenshots";

    // prevent instantiation
    private Utils() {}

    /**
     * Create a unique file name starting with the prefix.
     */
    @NonNull
    static String createUniqueFilename(String prefix) {
        String randomId = Long.toString(System.currentTimeMillis());
        return String.format(Locale.US, FILE_NAME_TEMPLATE, prefix, randomId);
    }

    /**
     * Writes the bitmap the directory, creating the directory if it doesn't exist.
     */
    @Nullable
    @WorkerThread
    static File writeBitmapToDirectory(@NonNull Bitmap bitmap, @NonNull File directory) {
        if (!directory.mkdirs() && !directory.exists()) {
            Log.e(TAG, "Failed to create directory for bitmap.");
            return null;
        }
        return writeBitmapToFile(bitmap, new File(directory, createUniqueFilename(BITMAP_PREFIX)));
    }

    /**
     * Writes the bitmap to disk and returns the new file.
     *
     * @param bitmap Bitmap the bitmap to write
     * @param file   the file to write to
     */
    @Nullable
    @WorkerThread
    // suppress lint check for AGP 3.2 https://issuetracker.google.com/issues/116776070
    @SuppressLint("WrongThread")
    static File writeBitmapToFile(@NonNull Bitmap bitmap, @NonNull File file) {
        FileOutputStream fileStream = null;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
            fileStream = new FileOutputStream(file);
            fileStream.write(byteStream.toByteArray());
            return file;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * Saves the view as a Bitmap screenshot.
     */
    @Nullable
    static Bitmap capture(View view, Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            int[] location = new int[2];
            view.getLocationInWindow(location);
            PixelCopy.request(window,
                    new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight()),
                    bitmap, copyResult -> {},
                    new Handler(Looper.getMainLooper()));
            return bitmap;
        } else {
            if (view.getWidth() == 0 || view.getHeight() == 0) {
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        }
    }

    /**
     * Get the file provider Uri, so that internal files can be temporarily shared with other apps.
     *
     * Requires AndroidManifest permission: android.support.v4.content.FileProvider
     */
    @NonNull
    static Uri getProviderUri(@NonNull Context context, @NonNull File file) {
        String authority = context.getPackageName() + FILE_PROVIDER_SUFFIX;
        return FileProvider.getUriForFile(context, authority, file);
    }

    /**
     * Get the file provider Uri, so that internal files can be temporarily shared with other apps.
     *
     * Requires AndroidManifest permission: android.support.v4.content.FileProvider
     */
    @NonNull
    static Uri getProviderUri(@NonNull Context context, @NonNull Uri uri) {
        File file = new File(uri.getPath());
        return getProviderUri(context, file);
    }

    /**
     * Return a new {@link android.view.LayoutInflater} that uses the given theme if the theme is valid,
     * or the given inflater otherwise
     */
    @NonNull
    static LayoutInflater applyThemeToInflater(@NonNull LayoutInflater inflater, @StyleRes int theme) {
        if (theme != FeedbackActivity.MISSING_RESOURCE) {
            return inflater.cloneInContext(new ContextThemeWrapper(inflater.getContext(), theme));
        }
        return inflater;
    }

    @Nullable
    static String getScreenshotDirectoryRoot(@NonNull Context context) {
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }
        return filesDir.getAbsolutePath() + SCREENSHOT_DIRECTORY;
    }
}
