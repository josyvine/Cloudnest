package com.cloudnest.app;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

/**
 * Data Access Object (DAO) for FileTrackEntity.
 * Manages the database operations for the global File Number Tracking System,
 * which ensures upload sequence numbers are continuous across multiple Drive accounts.
 * UPDATED: Added queries for per-preset sequencing and the Visual Cloud Ledger.
 */
@Dao
public interface FileTrackDao {

    // --- Data Holder for the Visual Ledger ---
    // This holds the results of the complex GROUP BY query.
    class LedgerReportModel {
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
    }


    /**
     * Inserts a record of an uploaded file into the tracking database.
     * This logs the file's local path, its assigned sequence number, and which Drive account it was saved to.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FileTrackEntity fileTrack);

    /**
     * Checks if a file from a specific local path has already been uploaded and tracked.
     * Updated SQL query to use 'local_path' to match FileTrackEntity.
     * @param localPath The absolute path of the local file.
     * @return The FileTrackEntity if found, otherwise null.
     */
    @Query("SELECT * FROM file_tracking WHERE local_path = :localPath LIMIT 1")
    FileTrackEntity findByLocalPath(String localPath);

    /**
     * Deletes all records from the file tracking table.
     */
    @Query("DELETE FROM file_tracking")
    void clearAllTrackingData();


    // --- NEW QUERIES FOR VISUAL LEDGER & PER-PRESET SEQUENCING ---

    /**
     * Retrieves the highest (latest) sequence number for a SPECIFIC PRESET.
     * This is the core of the per-preset sequencing logic.
     * @return The maximum sequence number for the given preset, or 0 if none exist.
     */
    @Query("SELECT MAX(sequence_number) FROM file_tracking WHERE preset_id = :presetId")
    int getLatestSequenceNumberForPreset(long presetId);

    /**
     * Performs the complex aggregation query for the Visual Cloud Ledger.
     * For a specific drive, it groups all tracked files by their parent preset folder
     * and calculates the sequence range, total file count, and total size.
     * It joins with the preset_folders table to get the folder_name.
     */
    @Query("SELECT " +
           "    t.preset_id, " +
           "    p.folder_name, " +
           "    MIN(t.sequence_number) as start_sequence, " +
           "    MAX(t.sequence_number) as end_sequence, " +
           "    COUNT(t.id) as total_files, " +
           "    SUM(t.file_size) as total_size " +
           "FROM file_tracking t " +
           "JOIN preset_folders p ON t.preset_id = p.id " +
           "WHERE t.drive_account_id = :driveAccountId " +
           "GROUP BY t.preset_id, p.folder_name " +
           "ORDER BY p.folder_name ASC")
    LiveData<List<LedgerReportModel>> getLedgerReportForDrive(String driveAccountId);

}