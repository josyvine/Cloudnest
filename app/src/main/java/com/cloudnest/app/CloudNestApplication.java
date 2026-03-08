package com.cloudnest.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Global Application class for CloudNest.
 * Initializes the notification channels required for the 
 * background WorkManager upload queues and auto-backup services.
 */
public class CloudNestApplication extends Application {

    // Unique IDs for the notification channels
    public static final String UPLOAD_CHANNEL_ID = "cloudnest_upload_channel";
    public static final String BACKUP_CHANNEL_ID = "cloudnest_auto_backup_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the notification channels required for Foreground Services and WorkManager
        createNotificationChannels();
    }

    /**
     * Creates Notification Channels.
     * Required for Android 8.0 (API 26) and above to display upload progress and backup statuses.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // 1. Upload Manager Channel (Shows live progress bars)
            CharSequence uploadName = "Upload Manager";
            String uploadDescription = "Displays live progress of manual and folder uploads to Google Drive.";
            int uploadImportance = NotificationManager.IMPORTANCE_LOW; // Low so it doesn't constantly beep during progress updates
            
            NotificationChannel uploadChannel = new NotificationChannel(UPLOAD_CHANNEL_ID, uploadName, uploadImportance);
            uploadChannel.setDescription(uploadDescription);
            notificationManager.createNotificationChannel(uploadChannel);

            // 2. Auto-Backup Channel (Shows silent background sync status)
            CharSequence backupName = "Auto Backup (Preset Folders)";
            String backupDescription = "Notifies when background preset folders are syncing new files.";
            int backupImportance = NotificationManager.IMPORTANCE_MIN; // Min importance for silent background operations
            
            NotificationChannel backupChannel = new NotificationChannel(BACKUP_CHANNEL_ID, backupName, backupImportance);
            backupChannel.setDescription(backupDescription);
            notificationManager.createNotificationChannel(backupChannel);
        }
    }
}