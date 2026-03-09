package com.cloudnest.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility for managing CloudNest System Notifications.
 * Provides live feedback for uploads in the system tray.
 * UPDATED: Optimized for Android 13+, fixed Glitch 8 (Completion visibility).
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = CloudNestApplication.UPLOAD_CHANNEL_ID;
    private static final int PROGRESS_NOTIFICATION_ID = 4001;
    private static final int FINISHED_NOTIFICATION_ID = 4002;
    private static final String TAG = "NotificationHelper";

    /**
     * Displays or updates the "Uploading" progress bar notification.
     * @param context App context.
     * @param percent Progress (0 to 100).
     * @param status Descriptive text (e.g., "File 5 of 20 - 1.2 MB/s").
     */
    public static void showUploadProgress(Context context, int percent, String status) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_generic) // Ensure this exists
                .setContentTitle("CloudNest: Syncing Files")
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true) // Don't beep on every % update
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(context, R.color.cloudnest_blue))
                .setProgress(100, percent, false);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing: " + e.getMessage());
        }
    }

    /**
     * Shows the final "Success" notification when a task completes.
     * @param context App context.
     * @param fileCount Total number of files uploaded.
     */
    public static void showUploadComplete(Context context, int fileCount) {
        // Cancel the progress notification first
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID);

        String message = (fileCount > 0) 
            ? "Successfully uploaded " + fileCount + " files to Google Drive." 
            : "Sync complete. No new files found.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_generic)
                .setContentTitle("Upload Successful")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for Glitch 8
                .setDefaults(Notification.DEFAULT_ALL) // Beep/Vibrate on finish
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.cloudnest_status_green));

        try {
            notificationManager.notify(FINISHED_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing: " + e.getMessage());
        }
    }

    /**
     * Shows a "Failed" alert in the system tray.
     */
    public static void showUploadFailed(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_generic)
                .setContentTitle("Sync Interrupted")
                .setContentText("Connection lost or Drive full. Tap to retry from Upload Manager.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.cloudnest_status_red));

        try {
            notificationManager.notify(FINISHED_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing: " + e.getMessage());
        }
    }

    /**
     * Ensures the Notification Channel is created for legacy systems.
     */
    public static void checkNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // Ensure the channel from CloudNestApplication is registered
                NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    channel = new NotificationChannel(
                            CHANNEL_ID,
                            "File Transfers",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription("Shows progress of manual and auto-backups.");
                    manager.createNotificationChannel(channel);
                }
            }
        }
    }
}