package com.cloudnest.app;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Background Worker for Preset Folder Auto-Backup.
 * This worker intelligently syncs a local folder with a target folder in Google Drive.
 * It compares local and remote file lists to upload only new or modified files.
 */
public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";
    private Drive driveService;
    private CloudNestDatabase db;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = CloudNestDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        String localFolderPath = getInputData().getString("FOLDER_PATH");
        String presetIdStr = getInputData().getString("PRESET_ID");
        if (localFolderPath == null || presetIdStr == null) return Result.failure();
        
        long presetId = Long.parseLong(presetIdStr);
        java.io.File localFolder = new java.io.File(localFolderPath);

        if (!localFolder.exists() || !localFolder.isDirectory()) {
            // Folder might have been deleted, clean up from DB
            db.presetFolderDao().deleteById(presetId);
            return Result.success();
        }

        try {
            // 1. Authenticate with Google Drive
            authenticateDrive();
            if (driveService == null) return Result.failure();

            // 2. Find or create the root "CloudNest" folder
            String rootFolderId = findOrCreateFolder("CloudNest", "root");

            // 3. Find or create the specific preset subfolder inside "CloudNest"
            String targetFolderId = findOrCreateFolder(localFolder.getName(), rootFolderId);

            // 4. Get list of files already in the Drive folder
            Set<String> remoteFileNames = getRemoteFileNames(targetFolderId);

            // 5. Get list of local files and upload new ones
            java.io.File[] localFiles = localFolder.listFiles();
            if (localFiles != null) {
                for (java.io.File localFile : localFiles) {
                    if (localFile.isFile() && !remoteFileNames.contains(localFile.getName())) {
                        uploadFile(localFile, targetFolderId);
                    }
                }
            }

            // 6. Update the 'lastSyncTime' in the database
            db.presetFolderDao().updateSyncTime(presetId, System.currentTimeMillis());

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Auto-Backup sync failed for " + localFolderPath + ": " + e.getMessage());
            return Result.retry();
        }
    }

    private void authenticateDrive() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account == null) {
            driveService = null;
            return;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("CloudNest Auto-Backup")
                .build();
    }

    /**
     * Finds a folder by name inside a parent. Creates it if it doesn't exist.
     */
    private String findOrCreateFolder(String folderName, String parentId) throws IOException {
        String query = "mimeType = 'application/vnd.google-apps.folder' and " +
                       "name = '" + folderName + "' and '" + parentId + "' in parents and trashed = false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId(); // Folder exists
        } else {
            // Create folder
            File folderMeta = new File();
            folderMeta.setName(folderName);
            folderMeta.setMimeType("application/vnd.google-apps.folder");
            folderMeta.setParents(Collections.singletonList(parentId));
            
            File createdFolder = driveService.files().create(folderMeta).setFields("id").execute();
            return createdFolder.getId();
        }
    }

    /**
     * Retrieves a set of all file names currently inside a Drive folder for comparison.
     */
    private Set<String> getRemoteFileNames(String folderId) throws IOException {
        Set<String> names = new HashSet<>();
        String query = "'" + folderId + "' in parents and trashed = false";
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setFields("nextPageToken, files(name)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                names.add(file.getName());
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return names;
    }

    /**
     * Uploads a single file to the specified Drive folder.
     */
    private void uploadFile(java.io.File localFile, String parentFolderId) throws IOException {
        File fileMeta = new File();
        fileMeta.setName(localFile.getName());
        fileMeta.setParents(Collections.singletonList(parentFolderId));

        com.google.api.client.http.FileContent mediaContent =
                new com.google.api.client.http.FileContent(null, localFile);

        driveService.files().create(fileMeta, mediaContent)
                .setFields("id")
                .execute();
        
        Log.i(TAG, "Auto-synced new file: " + localFile.getName());
    }
}