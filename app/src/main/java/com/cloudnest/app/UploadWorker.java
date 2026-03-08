package com.cloudnest.app;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * Background Upload Worker.
 * Handles recursive file scanning and uploading to Google Drive.
 * Updates Progress (0-100%) for the UploadManagerFragment to display.
 */
public class UploadWorker extends Worker {

    private static final String TAG = "UploadWorker";
    private Drive driveService;
    private int totalFiles = 0;
    private int uploadedFiles = 0;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] filePaths = getInputData().getStringArray("FILE_PATHS");
        if (filePaths == null) return Result.failure();

        // 1. Authenticate
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account == null) return Result.failure();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("CloudNest")
                .build();

        // 2. Start Process
        try {
            // First pass: count total files for progress calculation
            for (String path : filePaths) {
                countFilesRecursive(new java.io.File(path));
            }

            // Second pass: Perform uploads
            for (String path : filePaths) {
                processAndUpload(new java.io.File(path), "root");
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Upload Error: " + e.getMessage());
            return Result.retry();
        }
    }

    private void countFilesRecursive(java.io.File file) {
        if (file.isDirectory()) {
            java.io.File[] files = file.listFiles();
            if (files != null) {
                for (java.io.File f : files) countFilesRecursive(f);
            }
        } else {
            totalFiles++;
        }
    }

    private void processAndUpload(java.io.File localFile, String parentFolderId) throws IOException {
        if (localFile.isDirectory()) {
            // Create folder in Drive
            File folderMeta = new File();
            folderMeta.setName(localFile.getName());
            folderMeta.setMimeType("application/vnd.google-apps.folder");
            folderMeta.setParents(Collections.singletonList(parentFolderId));

            File driveFolder = driveService.files().create(folderMeta).setFields("id").execute();

            // Recurse
            java.io.File[] files = localFile.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    processAndUpload(f, driveFolder.getId());
                }
            }
        } else {
            // Upload file
            File fileMeta = new File();
            fileMeta.setName(localFile.getName());
            fileMeta.setParents(Collections.singletonList(parentFolderId));

            com.google.api.client.http.FileContent content = new com.google.api.client.http.FileContent(
                    "application/octet-stream", localFile);

            driveService.files().create(fileMeta, content).execute();

            uploadedFiles++;
            updateProgress(localFile.getName());
        }
    }

    private void updateProgress(String currentFileName) {
        int progress = (totalFiles > 0) ? (uploadedFiles * 100 / totalFiles) : 100;
        
        Data progressData = new Data.Builder()
                .putString("CURRENT_FILE", currentFileName)
                .putInt("PROGRESS_PERCENT", progress)
                .build();
        
        setProgressAsync(progressData);
    }
}