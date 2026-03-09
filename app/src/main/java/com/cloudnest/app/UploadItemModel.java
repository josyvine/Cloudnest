package com.cloudnest.app;

import java.util.UUID;

/**
 * Data Model for Upload Queue Items.
 * Represents a single background upload task managed by WorkManager.
 * UPDATED: Added fields for network speed and detailed progress strings.
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
    private String speed;    // NEW: To show "1.5 MB/s"
    private String details;  // NEW: To show "File 5 of 50"

    public UploadItemModel(UUID workId, String fileName, int progress, Status status, String speed, String details) {
        this.workId = workId;
        this.fileName = fileName;
        this.progress = progress;
        this.status = status;
        this.speed = speed;
        this.details = details;
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

    public String getSpeed() {
        return speed;
    }

    public String getDetails() {
        return details;
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

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}