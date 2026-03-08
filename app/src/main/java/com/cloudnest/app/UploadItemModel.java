package com.cloudnest.app;

import java.util.UUID;

/**
 * Data Model for Upload Queue Items.
 * Represents a single background upload task managed by WorkManager.
 * Used by the UploadManagerFragment to display real-time progress.
 */
public class UploadItemModel {

    // Enum representing the current state of the upload task
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private UUID workId;
    private String fileName;
    private int progress;
    private Status status;

    public UploadItemModel(UUID workId, String fileName, int progress, Status status) {
        this.workId = workId;
        this.fileName = fileName;
        this.progress = progress;
        this.status = status;
    }

    // Getters
    public UUID getWorkId() {
        return workId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getProgress() {
        return progress;
    }

    public Status getStatus() {
        return status;
    }

    // Setters
    public void setWorkId(UUID workId) {
        this.workId = workId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}