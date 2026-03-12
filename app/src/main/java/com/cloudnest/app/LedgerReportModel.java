package com.cloudnest.app;

import androidx.room.ColumnInfo;

/**
 * Data Model for the Visual Cloud Ledger report.
 * This class holds the aggregated results from the FileTrackDao query,
 * providing the necessary information to populate the LedgerExpandableAdapter.
 */
public class LedgerReportModel {

    @ColumnInfo(name = "preset_id")
    public long presetId;

    @ColumnInfo(name = "folder_name")
    public String folderName;

    @ColumnInfo(name = "start_sequence")
    public int startSequence;

    @ColumnInfo(name = "end_sequence")
    public int endSequence;

    @ColumnInfo(name = "total_files")
    public int totalFiles;

    @ColumnInfo(name = "total_size")
    public long totalSize;

    // Constructor
    public LedgerReportModel(long presetId, String folderName, int startSequence, int endSequence, int totalFiles, long totalSize) {
        this.presetId = presetId;
        this.folderName = folderName;
        this.startSequence = startSequence;
        this.endSequence = endSequence;
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
    }

    // Getters
    public long getPresetId() {
        return presetId;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getStartSequence() {
        return startSequence;
    }

    public int getEndSequence() {
        return endSequence;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public long getTotalSize() {
        return totalSize;
    }
}