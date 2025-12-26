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
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-window screenshot capture for Android applications.
 *
 * Uses PixelCopy API (Android O+) to capture hardware-accelerated bitmaps that cannot be
 * accessed via traditional Canvas rendering. This is required for Jetpack Compose screens
 * and Coil-loaded images which create GPU-stored bitmaps.
 *
 * Captures each window separately (activity, dialogs, bottom sheets) by reflecting into
 * WindowManager's internal data structures to enumerate all root views.
 *
 * Requirements: Android API 26+ (PixelCopy API)
 */
final class MultiWindowScreenshotCapture {
    private static final String TAG = "MultiWindowCapture";

    private MultiWindowScreenshotCapture() {
    }

    /**
     * Captures screenshots of all visible windows asynchronously.
     * <p>
     * Uses reflection to enumerate all windows, then captures each via PixelCopy.
     * Callback is invoked on main thread when all captures complete.
     *
     * @param activity the activity to capture screenshots from
     * @param callback receives list of bitmaps (one per window), or null if all captures failed
     */
    static void captureMultipleAsync(@NonNull Activity activity,
                                     @NonNull MultiBitmapCallback callback
    ) {
        final List<ViewRootData> rootViews = getRootViews(activity);

        if (rootViews.isEmpty()) {
            callback.onCaptureComplete(null);
            return;
        }

        Log.d(TAG, "Found " + rootViews.size() + " window(s) to capture");

        final Bitmap[] bitmaps = new Bitmap[rootViews.size()];
        final AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < rootViews.size(); i++) {
            final int index = i;
            final ViewRootData rootView = rootViews.get(i);

            Window windowForView = rootView.getWindow();
            if (windowForView == null) {
                windowForView = activity.getWindow();
            }

            captureAsync(rootView, windowForView, new CaptureCallback() {
                @Override
                public void onCaptureComplete(Bitmap bitmap) {
                    bitmaps[index] = bitmap;
                    int completed = completedCount.incrementAndGet();

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to capture window " + index);
                    }

                    if (completed == rootViews.size()) {
                        List<Bitmap> bitmapList = new ArrayList<>();
                        for (Bitmap bmp : bitmaps) {
                            if (bmp != null) {
                                bitmapList.add(bmp);
                            }
                        }

                        callback.onCaptureComplete(bitmapList.isEmpty() ? null : bitmapList);
                    }
                }
            });
        }
    }

    /**
     * Gets all root views using reflection to access WindowManager internals.
     * Includes activity, dialogs, and bottom sheets currently visible.
     *
     * @param activity the activity context
     * @return List of ViewRootData, or empty list if reflection fails
     */
    @SuppressWarnings("unchecked")
    static List<ViewRootData> getRootViews(@NonNull Activity activity) {
        Object globalWindowManager = getFieldValueSafe("mGlobal", activity.getWindowManager());
        if (globalWindowManager == null) {
            return Collections.emptyList();
        }

        Object rootObjects = getFieldValueSafe("mRoots", globalWindowManager);
        Object paramsObject = getFieldValueSafe("mParams", globalWindowManager);

        if (rootObjects == null || paramsObject == null) {
            return Collections.emptyList();
        }

        Object[] roots = ((List<?>) rootObjects).toArray();
        List<WindowManager.LayoutParams> paramsList =
                (List<WindowManager.LayoutParams>) paramsObject;
        WindowManager.LayoutParams[] params = paramsList.toArray(new WindowManager.LayoutParams[0]);

        List<ViewRootData> rootViews = extractViewRootData(roots, params);
        if (rootViews.isEmpty()) {
            return Collections.emptyList();
        }

        offsetRootsTopLeft(rootViews);
        ensureDialogsAreAfterActivities(rootViews);

        return rootViews;
    }

    /**
     * Captures a screenshot of a view using the associated window.
     * Uses PixelCopy API (Android O+) for hardware bitmap support.
     *
     * @param viewRootData information about the view root to capture
     * @param window       the window containing the view
     * @param callback     callback to receive the captured bitmap
     */
    private static void captureAsync(@NonNull ViewRootData viewRootData,
                                     @Nullable Window window,
                                     @NonNull CaptureCallback callback) {
        final View view = viewRootData._view.getRootView();

        if (view.getWidth() == 0 || view.getHeight() == 0) {
            callback.onCaptureComplete(null);
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || window == null) {
            Log.e(TAG, "PixelCopy not available (API < 26) or window is null");
            callback.onCaptureComplete(null);
            return;
        }

        final Bitmap bitmap =
                Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Try to get the Surface from ViewRoot for per-window capture
        Surface surface = getSurfaceFromViewRoot(viewRootData._viewRoot);

        if (surface != null && surface.isValid()) {
            // Use Surface directly - captures specific window (activity/dialog/bottom sheet)
            PixelCopy.request(surface, bitmap, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    callback.onCaptureComplete(bitmap);
                } else {
                    Log.e(TAG, "PixelCopy from Surface failed with result: " + copyResult);
                    callback.onCaptureComplete(null);
                }
            }, new Handler(Looper.getMainLooper()));
        } else {
            // Fallback to Window
            PixelCopy.request(window, bitmap, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    callback.onCaptureComplete(bitmap);
                } else {
                    Log.e(TAG, "PixelCopy from Window failed with result: " + copyResult);
                    callback.onCaptureComplete(null);
                }
            }, new Handler(Looper.getMainLooper()));
        }
    }

    /**
     * Safely extracts Surface from ViewRootImpl using reflection.
     *
     * @param viewRoot the ViewRootImpl object
     * @return Surface if available and valid, null otherwise
     */
    @Nullable
    private static Surface getSurfaceFromViewRoot(@Nullable Object viewRoot) {
        if (viewRoot == null) {
            return null;
        }

        Object surfaceObj = getFieldValueSafe("mSurface", viewRoot);
        return surfaceObj instanceof Surface ? (Surface) surfaceObj : null;
    }

    /**
     * Extracts view root data from WindowManager's internal arrays. Filters out hidden views and
     * calculates content bounds for dialogs/bottom sheets.
     *
     * @param roots  ViewRootImpl objects from WindowManager
     * @param params corresponding LayoutParams for each root
     * @return List of ViewRootData with screen positions
     */
    private static List<ViewRootData> extractViewRootData(Object[] roots,
                                                          WindowManager.LayoutParams[] params) {
        List<ViewRootData> rootViews = new ArrayList<>();

        for (int i = 0; i < roots.length; i++) {
            Object root = roots[i];
            View rootView = (View) getFieldValueSafe("mView", root);

            if (rootView == null || !rootView.isShown()) {
                continue;
            }

            WindowManager.LayoutParams layoutParams = params[i];
            int[] location = new int[2];
            rootView.getLocationOnScreen(location);

            int left = location[0];
            int top = location[1];
            int width = rootView.getWidth();
            int height = rootView.getHeight();

            // For dialogs/bottom sheets, find actual content bounds
            if (layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION) {
                View contentView = findBottomSheetContent(rootView, 0);
                if (contentView != null && contentView != rootView) {
                    int[] childLocation = new int[2];
                    contentView.getLocationOnScreen(childLocation);
                    left = childLocation[0];
                    top = childLocation[1];
                    width = contentView.getWidth();
                    height = contentView.getHeight();
                }
            }

            Rect area = new Rect(left, top, left + width, top + height);
            rootViews.add(new ViewRootData(rootView, area, layoutParams, root));
        }

        return rootViews;
    }

    /**
     * Recursively searches for bottom sheet content view.
     * Bottom sheets typically have a non-zero top position from screen.
     */
    private static View findBottomSheetContent(View view, int depth) {
        if (depth > 10) return null; // Prevent deep recursion

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (location[1] > 100) { // Bottom sheets typically start below y=100
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE) {
                    View result = findBottomSheetContent(child, depth + 1);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Normalizes all root view coordinates so top-left starts at (0,0).
     * Required for consistent positioning across different screen configurations.
     */
    private static void offsetRootsTopLeft(List<ViewRootData> rootViews) {
        int minTop = Integer.MAX_VALUE;
        int minLeft = Integer.MAX_VALUE;

        for (ViewRootData rootView : rootViews) {
            minTop = Math.min(minTop, rootView._winFrame.top);
            minLeft = Math.min(minLeft, rootView._winFrame.left);
        }

        for (ViewRootData rootView : rootViews) {
            rootView._winFrame.offset(-minLeft, -minTop);
        }
    }

    /**
     * Sorts view roots so activities come before their dialogs.
     * Ensures correct capture order when multiple windows belong to same activity.
     */
    private static void ensureDialogsAreAfterActivities(List<ViewRootData> viewRoots) {
        if (viewRoots.size() <= 1) {
            return;
        }

        for (int dialogIndex = 0; dialogIndex < viewRoots.size() - 1; dialogIndex++) {
            ViewRootData viewRoot = viewRoots.get(dialogIndex);
            if (!viewRoot.isDialogType() || viewRoot.getWindowToken() == null) {
                continue;
            }

            for (int parentIndex = dialogIndex + 1; parentIndex < viewRoots.size(); parentIndex++) {
                ViewRootData possibleParent = viewRoots.get(parentIndex);
                if (possibleParent.isActivityType()
                        && possibleParent.getWindowToken() == viewRoot.getWindowToken()) {
                    viewRoots.remove(possibleParent);
                    viewRoots.add(dialogIndex, possibleParent);
                    break;
                }
            }
        }
    }

    /**
     * Safely gets a field value using reflection, returns null on failure.
     */
    private static Object getFieldValueSafe(String fieldName, Object target) {
        try {
            Field field = findField(fieldName, target.getClass());
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get field " + fieldName, e);
            return null;
        }
    }

    /**
     * Finds a field in a class hierarchy.
     */
    private static Field findField(String name, Class<?> clazz) throws NoSuchFieldException {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (name.equals(field.getName())) {
                    return field;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new NoSuchFieldException("Field " + name + " not found for class " + clazz);
    }

    //region Data Classes & Interfaces

    /**
     * Contains information about a view root in the window hierarchy.
     * Each window (activity, dialog, bottom sheet) has its own ViewRoot.
     */
    static class ViewRootData {
        final View _view;
        final Rect _winFrame;
        final Rect _originalWinFrame;
        final WindowManager.LayoutParams _layoutParams;
        final Object _viewRoot;

        ViewRootData(View view, Rect winFrame, WindowManager.LayoutParams layoutParams,
                     Object viewRoot) {
            _view = view;
            _winFrame = winFrame;
            _originalWinFrame = new Rect(winFrame);
            _layoutParams = layoutParams;
            _viewRoot = viewRoot;
        }

        /**
         * Returns true if this is a dialog or bottom sheet window.
         */
        boolean isDialogType() {
            return _layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION;
        }

        /**
         * Returns true if this is the main activity window.
         */
        boolean isActivityType() {
            return _layoutParams.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        }

        /**
         * Returns the window token for matching dialogs to their parent activities.
         */
        IBinder getWindowToken() {
            return _layoutParams.token;
        }


        /**
         * Gets the Window associated with this view root.
         * Tries multiple approaches to find the window.
         */
        @Nullable
        Window getWindow() {
            // Try to get from context
            Context context = _view.getContext();

            if (context instanceof Activity) {
                return ((Activity) context).getWindow();
            }

            // Unwrap ContextWrapper to find Activity
            Context ctx = context;
            while (ctx instanceof ContextWrapper && !(ctx instanceof Activity)) {
                ctx = ((ContextWrapper) ctx).getBaseContext();
                if (ctx == null) break;
            }

            if (ctx instanceof Activity) {
                return ((Activity) ctx).getWindow();
            }

            return null;
        }
    }

    /**
     * Callback interface for asynchronous screenshot capture.
     */
    interface CaptureCallback {
        /**
         * Called when screenshot capture completes.
         *
         * @param bitmap the captured bitmap, or null if capture failed
         */
        void onCaptureComplete(@Nullable Bitmap bitmap);
    }

    /**
     * Callback interface for receiving multiple captured bitmaps.
     */
    interface MultiBitmapCallback {
        /**
         * Called when all screenshots have been captured.
         *
         * @param bitmaps list of captured bitmaps, one per window, or null if capture failed
         */
        void onCaptureComplete(@Nullable List<Bitmap> bitmaps);
    }

    //endregion
}
