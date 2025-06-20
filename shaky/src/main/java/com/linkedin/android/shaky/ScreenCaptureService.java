package com.linkedin.android.shaky;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service required for Media Projection API
 * starting from Android 10.
 */
public class ScreenCaptureService extends Service {

    public static final String NOTIFICATION_CHANNEL_ID = "ShakyScreenCapture";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_START = "com.linkedin.android.shaky.action.START_CAPTURE";
    private static final String ACTION_STOP = "com.linkedin.android.shaky.action.STOP_CAPTURE";

    /**
     * Starts the foreground service for screen capture
     *
     * @param context Application context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.setAction(ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stops the foreground service
     *
     * @param context Application context
     */
    public static void stop(Context context) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Screen Capture",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Used for capturing screenshots");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Capturing Screenshot")
                .setContentText("Processing your feedback...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(android.R.drawable.ic_menu_camera);

        // Set the correct foreground service type for MediaProjection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }
}
