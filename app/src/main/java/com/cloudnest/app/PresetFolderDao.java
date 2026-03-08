package com.cloudnest.app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for PresetFolderEntity.
 * Defines the database queries required to manage the list of folders
 * configured for automatic background backup.
 */
@Dao
public interface PresetFolderDao {

    /**
     * Inserts a new folder into the auto-backup configuration.
     * If a folder with the same local path already exists, the record will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PresetFolderEntity presetFolder);

    /**
     * Deletes a folder from the auto-backup configuration.
     * This stops the folder from being synced in the future.
     */
    @Delete
    void delete(PresetFolderEntity presetFolder);

    /**
     * Deletes a preset folder using its unique ID.
     * Useful when the original folder path is no longer valid.
     */
    @Query("DELETE FROM preset_folders WHERE id = :id")
    void deleteById(long id);

    /**
     * Retrieves all configured preset folders from the database.
     * Returns LiveData, allowing the PresetFoldersFragment UI to update automatically
     * when a folder is added or removed.
     */
    @Query("SELECT * FROM preset_folders ORDER BY folderName ASC")
    LiveData<List<PresetFolderEntity>> getAllPresets();

    /**
     * Updates the last synchronization timestamp for a specific preset folder.
     * This is called by the AutoBackupWorker upon successful completion of a sync operation.
     * @param id The unique ID of the preset folder.
     * @param timestamp The current system time in milliseconds.
     */
    @Query("UPDATE preset_folders SET lastSyncTime = :timestamp WHERE id = :id")
    void updateSyncTime(long id, long timestamp);

    /**
     * Checks if a specific local folder path is already configured for auto-backup.
     * @param path The absolute local path of the folder.
     * @return The PresetFolderEntity if it exists, otherwise null.
     */
    @Query("SELECT * FROM preset_folders WHERE localPath = :path LIMIT 1")
    PresetFolderEntity findByPath(String path);
}