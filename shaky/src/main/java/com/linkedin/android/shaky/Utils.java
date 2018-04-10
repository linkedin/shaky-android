/**
 * Copyright (C) 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

final class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    private static final String FILE_NAME_TEMPLATE = "%s_%s.png";
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

    /**
     * @param uri The uri to evaluate.
     * @return Whether or not the given uri points to resource on the device. This check is basic, so it does not
     * guarantee the resource actually exists.
     */
    static boolean isLocalUri(@Nullable Uri uri) {
        if (uri != null) {
            String scheme = uri.getScheme();
            return ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equalsIgnoreCase(scheme)
                || ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme);
        }
        return false;
    }

    /**
     * Tries to determine the current mime type of the pending attachment by reading the file at it's uri. There's
     * fallback logic included.
     *
     * @param context A context used to read the file at the attachment's uri.
     * @param uri     Media uri
     */
    @Nullable
    static String getMimeType(@NonNull Context context, @NonNull Uri uri) {
        if (!isLocalUri(uri)) {
            return null;
        }
        String newMediaType = null;

        // Try to decode the bitmap to get its mime type. In some cases on Android M, the decoder may not return a
        // mime type. Fallback logic kicks in afterwards.
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            if (options.outMimeType != null) {
                newMediaType = options.outMimeType;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error getting mediaType for : " + uri.toString(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        // Fallback to the content resolver if the bitmap decoding hasn't worked.
        if (newMediaType == null) {
            newMediaType = context.getContentResolver().getType(uri);
        }

        // Fallback to the file extension if everything else fails.
        if (newMediaType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null) {
                newMediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }

        return newMediaType;
    }

    /**
     * Make the file uri persistent
     */
    static void persistFilePermissions(@NonNull Context context, @NonNull Uri uri, @Nullable Intent data) {
        // Only works for API level 19 or above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && data != null) {
            final int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // We're not guaranteed to get the permission grants for this uri. It's not fatal if we don't.
            try {
                context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Could not persist permission grants for " + uri);
                }
            }
        }
    }

    @NonNull
    static String getFilename(@NonNull Context context, @NonNull Uri uri) {
        String scheme = uri.getScheme();
        String filename = null;
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        filename = cursor.getString(nameIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        // use the last path segment as the filename
        if (filename == null) {
            filename = uri.getLastPathSegment();
        }
        return filename;
    }
}
