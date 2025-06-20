package com.linkedin.android.shaky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * Manages screen capture operations using MediaProjection API.
 * This class handles the permission request and screen capturing process.
 */
public class ScreenCaptureManager {

    private static final int REQUEST_CODE_MEDIA_PROJECTION = 1000;

    private Context appContext;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private CaptureCallback captureCallback;

    public interface CaptureCallback {
        void onCaptureComplete(Bitmap bitmap);
        void onCaptureFailed();
    }

    public ScreenCaptureManager(Context context) {
        this.appContext = context.getApplicationContext();
        projectionManager = (MediaProjectionManager)
                appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    /**
     * Request screen capture permission and start the capturing process.
     * This needs to be called from an Activity.
     *
     * @param activity Activity to request permission from
     * @param callback Callback to receive the captured screenshot or failure
     */
    public void requestCapturePermission(Activity activity, CaptureCallback callback) {
        if (activity == null || callback == null) {
            return;
        }

        this.captureCallback = callback;

        // Get screen metrics
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        display.getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        // Request permission
        Intent intent = projectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION);
    }

    /**
     * Handle activity result from permission request.
     * This should be called from the activity's onActivityResult method.
     *
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @param data Intent data from onActivityResult
     * @return true if handled by this manager, false otherwise
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_MEDIA_PROJECTION) {
            return false;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            // Start the foreground service before creating MediaProjection
            ScreenCaptureService.start(appContext);

            // Wait briefly to ensure the service is fully started
            new Handler().postDelayed(() -> {
                startCapture(resultCode, data);
            }, 100); // Small delay to ensure service starts
            return true;
        } else {
            if (captureCallback != null) {
                captureCallback.onCaptureFailed();
            }
            return true;
        }
    }

    private void startCapture(int resultCode, Intent data) {
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "Shaky Screenshot",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        // Capture the image with a delay to ensure display is ready
        new Handler().postDelayed(() -> captureImage(), 100);
    }

    private void captureImage() {
        if (imageReader == null) {
            if (captureCallback != null) {
                captureCallback.onCaptureFailed();
            }
            stopCapture();
            return;
        }

        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image == null) {
                if (captureCallback != null) {
                    captureCallback.onCaptureFailed();
                }
                stopCapture();
                return;
            }

            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();

            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            // Create bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            // Crop if needed due to rowPadding
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, screenWidth, screenHeight);

            if (bitmap != croppedBitmap) {
                bitmap.recycle();
            }

            if (captureCallback != null) {
                captureCallback.onCaptureComplete(croppedBitmap);
            }

        } catch (Exception e) {
            if (captureCallback != null) {
                captureCallback.onCaptureFailed();
            }
        } finally {
            if (image != null) {
                image.close();
            }
            stopCapture();
        }
    }

    private void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        // Stop the foreground service
        ScreenCaptureService.stop(appContext);
    }
}
