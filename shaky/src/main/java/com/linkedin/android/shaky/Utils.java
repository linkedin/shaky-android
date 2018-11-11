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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;

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
    static Bitmap capture(View view) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
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
}
