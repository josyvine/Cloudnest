package com.cloudnest.app;

import android.content.Context;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to manage background scheduling for Preset Folders.
 * This ensures that folders are scanned for new files automatically.
 * UPDATED: Optimized for recursive triggers and immediate system-folder detection.
 */
public class SyncScheduler {

    private static final String TAG = "SyncScheduler";
    private static final String TAG_PREFIX = "SYNC_JOB_";

    /**
     * Schedules a periodic background sync (every 1 hour) for a specific preset folder.
     * This acts as a safety net to ensure all files are synced even if a real-time event was missed.
     */
    public static void scheduleFolderSync(Context context, PresetFolderEntity folder) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putString("FOLDER_PATH", folder.localPath)
                .putString("PRESET_ID", String.valueOf(folder.id))
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class, 
                1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("AUTO_BACKUP")
                .addTag(TAG_PREFIX + folder.id)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG_PREFIX + folder.id,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
        );
        
        Log.d(TAG, "Scheduled periodic sync for: " + folder.folderName);
    }

    /**
     * Triggers an IMMEDIATE sync for a folder.
     * Logic for Glitch 1: Called by FolderWatcherService when a new file is detected.
     * UPDATED: Using APPEND_OR_REPLACE to handle rapid subfolder changes without cancelling active tasks.
     */
    public static void triggerImmediateSync(Context context, PresetFolderEntity folder) {
        Data inputData = new Data.Builder()
                .putString("FOLDER_PATH", folder.localPath)
                .putString("PRESET_ID", String.valueOf(folder.id))
                .build();

        OneTimeWorkRequest instantRequest = new OneTimeWorkRequest.Builder(AutoBackupWorker.class)
                .setInputData(inputData)
                .addTag("AUTO_BACKUP")
                .build();

        // UniqueWork with APPEND_OR_REPLACE ensures that if multiple files are added 
        // to subfolders quickly, the requests are queued up rather than dropped.
        WorkManager.getInstance(context).enqueueUniqueWork(
                "INSTANT_" + folder.id,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                instantRequest
        );
        
        Log.d(TAG, "Triggered instant sync for: " + folder.folderName);
    }

    /**
     * Cancels the background schedule for a specific folder.
     */
    public static void stopFolderSync(Context context, long folderId) {
        WorkManager.getInstance(context).cancelUniqueWork(TAG_PREFIX + folderId);
        WorkManager.getInstance(context).cancelUniqueWork("INSTANT_" + folderId);
    }

    /**
     * Loops through all presets in the database and ensures they are scheduled.
     * Called in MainActivity to start the system and the Watcher service.
     */
    public static void refreshAllSchedules(Context context) {
        CloudNestDatabase db = CloudNestDatabase.getInstance(context);
        
        // Use a background thread to access Room database
        new Thread(() -> {
            try {
                // Get all presets from the DAO
                List<PresetFolderEntity> presets = db.presetFolderDao().getAllPresetsSync();
                
                if (presets != null) {
                    for (PresetFolderEntity folder : presets) {
                        scheduleFolderSync(context, folder);
                    }
                }
                
                // Start the Instant Watcher Service to monitor real-time changes
                FolderWatcherService.startService(context);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to refresh sync schedules: " + e.getMessage());
            }
        }).start();
    }
}