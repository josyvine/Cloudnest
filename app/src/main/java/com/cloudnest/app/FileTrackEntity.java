package com.cloudnest.app;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity (Table) for tracking every single uploaded file.
 * This is the core of the continuous numbering system across multiple drives.
 * Each row represents one successfully uploaded file.
 * UPDATED: Added preset_id and file_size for the Visual Cloud Ledger.
 */
@Entity(tableName = "file_tracking",
        // Create an index on the localPath for faster lookups to prevent duplicates.
        // Added index on drive_account_id to prevent full table scans on foreign key modifications.
        // NEW: Added index on preset_id for faster ledger queries.
        indices = {
            @Index(value = "local_path", unique = true),
            @Index("drive_account_id"),
            @Index("preset_id")
        },
        // Establish a foreign key relationship with the drive_accounts table.
        foreignKeys = @ForeignKey(entity = DriveAccountEntity.class,
                                  parentColumns = "email",
                                  childColumns = "drive_account_id",
                                  onDelete = ForeignKey.CASCADE))
public class FileTrackEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "local_path")
    public String localPath;

    @ColumnInfo(name = "sequence_number")
    public int sequenceNumber;

    @NonNull
    @ColumnInfo(name = "drive_account_id") // Foreign key linking to DriveAccountEntity's email
    public String driveAccountId;

    @ColumnInfo(name = "drive_file_id") // The unique ID of the file returned by Google Drive API
    public String driveFileId;

    @ColumnInfo(name = "upload_timestamp")
    public long uploadTimestamp;

    // --- NEW COLUMNS FOR VISUAL LEDGER ---

    /**
     * Foreign key linking to the PresetFolderEntity's ID.
     * This tracks which Auto-Backup preset this file belongs to.
     */
    @ColumnInfo(name = "preset_id")
    public long presetId;

    /**
     * The size of the uploaded file in bytes.
     * Used to calculate total storage used by a preset on a specific drive.
     */
    @ColumnInfo(name = "file_size")
    public long fileSize;
}