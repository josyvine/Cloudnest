package com.cloudnest.app;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity (Table) for storing Auto-Backup (Preset Folder) configurations.
 * Maps a local directory to a designated Google Drive folder for background syncing.
 */
@Entity(tableName = "preset_folders")
public class PresetFolderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "folder_name")
    public String folderName = "";

    @NonNull
    @ColumnInfo(name = "local_path")
    public String localPath = "";

    @ColumnInfo(name = "drive_folder_id")
    public String driveFolderId;

    @ColumnInfo(name = "last_sync_time")
    public long lastSyncTime;
}