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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for advanced screenshot capture operations.
 * Handles bitmap merging, positioning, and rendering of complex window hierarchies.
 *
 * Package-private - internal use only.
 */
final class ScreenshotCaptureUtils {
    private static final String TAG = "ScreenshotCaptureUtils";

    // Prevent instantiation
    private ScreenshotCaptureUtils() {
    }

    /**
     * Captures screenshots of all visible windows (activity, dialogs, bottom sheets) asynchronously
     * and merges them into a single bitmap representing the complete screen state.
     */
    @UiThread
    static void captureAndMergeAsync(@NonNull Activity activity, @NonNull final ScreenshotCapture.CaptureCallback callback) {
        final List<ScreenshotCapture.ViewRootData> rootViews = ScreenshotCapture.getRootViews(activity);

        if (rootViews.isEmpty()) {
            callback.onCaptureComplete(null);
            return;
        }

        // Each DecorView represents a separate rendering surface (activity, dialog, bottom sheet, etc.)
        // Capture each surface separately, then merge them to create the final screenshot
        final Bitmap[] bitmaps = new Bitmap[rootViews.size()];
        final AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < rootViews.size(); i++) {
            final int index = i;
            final ScreenshotCapture.ViewRootData rootView = rootViews.get(i);

            // Try to get the specific window for this root view (dialog/bottom sheet may have their own)
            // Fall back to activity window if not available
            Window windowForView = rootView.getWindow();
            if (windowForView == null) {
                windowForView = activity.getWindow();
            }

            final Window finalWindow = windowForView;

            // Capture each window asynchronously
            ScreenshotCapture.captureAsync(rootView, finalWindow, new ScreenshotCapture.CaptureCallback() {
                @Override
                public void onCaptureComplete(Bitmap bitmap) {
                    bitmaps[index] = bitmap;
                    int completed = completedCount.incrementAndGet();

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to capture view " + index);
                    }

                    // When all captures complete, merge them into final screenshot
                    if (completed == rootViews.size()) {
                        Bitmap mergedBitmap = mergeBitmaps(bitmaps, rootViews);
                        callback.onCaptureComplete(mergedBitmap);
                    }
                }
            });
        }
    }

    /**
     * Merges multiple bitmaps by drawing them at their correct screen positions.
     * @param bitmaps array of bitmaps to merge
     * @param rootViews list of ViewRootData containing position information for each bitmap
     * @return a single bitmap with all bitmaps drawn at their correct positions, or null if all bitmaps are null
     */
    @Nullable
    static Bitmap mergeBitmaps(@NonNull Bitmap[] bitmaps, @NonNull List<ScreenshotCapture.ViewRootData> rootViews) {
        if (bitmaps.length == 0) {
            return null;
        }

        // Find the first non-null bitmap
        Bitmap firstBitmap = null;
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null) {
                firstBitmap = bitmap;
                break;
            }
        }

        if (firstBitmap == null) {
            return null;
        }

        // Calculate canvas size - determine screen height to distinguish full-screen vs sized bitmaps
        int screenHeight = getScreenHeight(bitmaps, rootViews);
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < rootViews.size(); i++) {
            if (bitmaps[i] != null) {
                Rect originalFrame = rootViews.get(i)._originalWinFrame;
                int bitmapWidth = bitmaps[i].getWidth();
                int bitmapHeight = bitmaps[i].getHeight();

                maxWidth = Math.max(maxWidth, bitmapWidth);

                // Full-screen bitmaps use screen height, sized bitmaps use position + height
                if (bitmapHeight == screenHeight) {
                    maxHeight = Math.max(maxHeight, screenHeight);
                } else {
                    maxHeight = Math.max(maxHeight, originalFrame.top + bitmapHeight);
                }
            }
        }

        // Create merged bitmap
        Bitmap mergedBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedBitmap);

        // Check if we need to apply dim overlay (for Traditional View dialogs)
        DimOverlayInfo dimInfo = getDimOverlayInfo(bitmaps, rootViews);

        // Draw each bitmap at its correct position
        for (int i = 0; i < bitmaps.length; i++) {
            Bitmap bitmap = bitmaps[i];
            if (bitmap == null || i >= rootViews.size()) {
                continue;
            }

            Rect originalFrame = rootViews.get(i)._originalWinFrame;
            if (originalFrame == null) {
                Log.e(TAG, "Null frame for bitmap " + i + ", skipping");
                continue;
            }

            drawBitmapAtPosition(canvas, bitmap, originalFrame, screenHeight);

            // Add dim overlay after activity but before dialog (Traditional Views only)
            if (dimInfo.shouldApplyDim && i == dimInfo.dialogIndex - 1 && rootViews.get(i).isActivityType()) {
                applyDimOverlay(canvas, dimInfo, bitmaps[i + 1], screenHeight);
            }
        }

        return mergedBitmap;
    }

    /**
     * Determines screen height from activity bitmap or first available bitmap.
     */
    private static int getScreenHeight(@NonNull Bitmap[] bitmaps, @NonNull List<ScreenshotCapture.ViewRootData> rootViews) {
        // Try to find activity bitmap first
        for (int i = 0; i < rootViews.size(); i++) {
            if (bitmaps[i] != null && rootViews.get(i).isActivityType()) {
                return bitmaps[i].getHeight();
            }
        }

        // Fallback to first non-null bitmap
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null) {
                return bitmap.getHeight();
            }
        }

        return 0;
    }

    /**
     * Analyzes view hierarchy to determine if dim overlay should be applied.
     */
    private static DimOverlayInfo getDimOverlayInfo(@NonNull Bitmap[] bitmaps, @NonNull List<ScreenshotCapture.ViewRootData> rootViews) {
        DimOverlayInfo info = new DimOverlayInfo();

        for (int i = 0; i < rootViews.size(); i++) {
            if (i < bitmaps.length && bitmaps[i] != null && !rootViews.get(i).isActivityType()) {
                info.shouldApplyDim = true;
                info.dialogIndex = i;

                // Get actual dim amount from Android's LayoutParams (proper Android way)
                WindowManager.LayoutParams layoutParams = rootViews.get(i).getLayoutParams();
                if ((layoutParams.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) == WindowManager.LayoutParams.FLAG_DIM_BEHIND) {
                    info.dimAmount = layoutParams.dimAmount;
                }
                break;
            }
        }

        return info;
    }

    /**
     * Draws a bitmap at its correct position on the canvas.
     * Handles both full-screen Compose bitmaps and sized Traditional View bitmaps.
     */
    private static void drawBitmapAtPosition(@NonNull Canvas canvas, @NonNull Bitmap bitmap,
                                             @NonNull Rect originalFrame, int screenHeight) {
        int left = originalFrame.left;
        int top = originalFrame.top;
        int frameWidth = originalFrame.right - originalFrame.left;
        int frameHeight = originalFrame.bottom - originalFrame.top;

        // Check if this is a Compose full-screen bitmap
        if (bitmap.getHeight() == screenHeight && originalFrame.top > 0) {
            // Full-screen Compose bitmap: draw at (0,0) for overlay effect
            canvas.drawBitmap(bitmap, 0, 0, null);
            return;
        }

        // Check if this is a bottom sheet (positioned at screen bottom)
        boolean isBottomSheet = isBottomSheetPosition(originalFrame, screenHeight);

        // Handle traditional bottom sheets with transparent left/right edges
        if (isBottomSheet) {
            // Try to crop transparent edges and scale to fill width
            if (handleTransparentEdges(canvas, bitmap, left, top, frameWidth, frameHeight)) {
                return; // Already drawn
            }
        }

        // For dialogs or views without transparent edges: draw at natural size and position
        canvas.drawBitmap(bitmap, left, top, null);
    }

    /**
     * Determines if a view is positioned like a bottom sheet (at the bottom of the screen).
     */
    private static boolean isBottomSheetPosition(@NonNull Rect frame, int screenHeight) {
        boolean extendsToBottom = frame.bottom >= screenHeight - 10;
        int height = frame.bottom - frame.top;
        boolean isSignificantHeight = height > screenHeight * 0.1f;
        return extendsToBottom && isSignificantHeight;
    }

    /**
     * Handles traditional bottom sheets with transparent left/right edges.
     * Crops the transparent edges and scales to fill the frame width while preserving aspect ratio.
     */
    private static boolean handleTransparentEdges(@NonNull Canvas canvas, @NonNull Bitmap bitmap,
                                                  int left, int top, int frameWidth, int frameHeight) {
        int leftEdge = findLeftEdge(bitmap);
        int rightEdge = findRightEdge(bitmap);
        int actualContentWidth = rightEdge - leftEdge;

        // Validate edge detection
        if (leftEdge < 0 || rightEdge <= leftEdge || rightEdge > bitmap.getWidth() || actualContentWidth <= 0) {
            return false; // Invalid, use normal drawing
        }

        // Check if there are transparent edges
        if (leftEdge > 0 || rightEdge < bitmap.getWidth()) {
            // Crop to content and scale to fill frame width, preserving aspect ratio
            Rect srcRect = new Rect(leftEdge, 0, rightEdge, bitmap.getHeight());

            // Scale to fill width, calculate new height to preserve aspect ratio
            float scaleX = (float) frameWidth / actualContentWidth;
            int scaledHeight = (int) (bitmap.getHeight() * scaleX);

            Rect destRect = new Rect(left, top, left + frameWidth, top + scaledHeight);
            canvas.drawBitmap(bitmap, srcRect, destRect, null);
            return true; // Bitmap was drawn
        }

        return false; // No transparent edges, use normal drawing
    }

    /**
     * Applies dim overlay for Traditional View dialogs.
     * Compose views handle dimming internally, so we skip them.
     */
    private static void applyDimOverlay(@NonNull Canvas canvas,
                                        @NonNull DimOverlayInfo dimInfo, @Nullable Bitmap dialogBitmap,
                                        int screenHeight) {
        if (dialogBitmap == null || dimInfo.dimAmount <= 0) {
            return;
        }

        boolean isComposeLikeView = dialogBitmap.getHeight() == screenHeight;

        if (!isComposeLikeView) {
            // Traditional View: apply dim overlay using Android's dimAmount
            int alpha = (int) (255 * dimInfo.dimAmount);
            canvas.drawARGB(alpha, 0, 0, 0);
        }
    }

    /**
     * Find the leftmost column with non-transparent pixels.
     * Samples pixels for performance (checks every 10th row).
     */
    private static int findLeftEdge(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int sampleStep = Math.max(1, height / 10); // Sample ~10 rows

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y += sampleStep) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 0) {
                    return x;
                }
            }
        }
        return width; // All transparent
    }

    /**
     * Find the rightmost column with non-transparent pixels.
     * Samples pixels for performance (checks every 10th row).
     */
    private static int findRightEdge(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int sampleStep = Math.max(1, height / 10); // Sample ~10 rows

        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y += sampleStep) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 0) {
                    return x + 1; // Return exclusive right edge
                }
            }
        }
        return 0; // All transparent
    }

    /**
     * Helper class to store dim overlay information.
     */
    private static class DimOverlayInfo {
        boolean shouldApplyDim = false;
        int dialogIndex = -1;
        float dimAmount = 0.0f;
    }
}
