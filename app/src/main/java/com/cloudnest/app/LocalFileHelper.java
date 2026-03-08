package com.cloudnest.app;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for local file system operations.
 * Handles:
 * 1. Recursive directory scanning.
 * 2. Counting total files within sub-directories for UI labels.
 * 3. Sorting and filtering logic for the file browser.
 */
public class LocalFileHelper {

    /**
     * Recursively counts all files inside a directory.
     * Used by the Browser to show "Folder (120)" labels.
     */
    public static int getFileCountRecursive(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += getFileCountRecursive(file);
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Retrieves the storage root paths (Internal Storage and SD Card).
     */
    public static List<File> getStorageRoots(Context context) {
        List<File> roots = new ArrayList<>();
        
        // Internal Storage
        roots.add(Environment.getExternalStorageDirectory());

        // External SD Card (If available)
        File[] externalFiles = context.getExternalFilesDirs(null);
        for (File file : externalFiles) {
            if (file != null && Environment.isExternalStorageRemovable(file)) {
                // Navigate up to find the root of the SD card
                File root = file;
                while (root.getParentFile() != null && root.getParentFile().canRead()) {
                    File parent = root.getParentFile();
                    if (parent.getAbsolutePath().equals("/storage")) break;
                    root = parent;
                }
                if (!roots.contains(root)) {
                    roots.add(root);
                }
            }
        }
        return roots;
    }

    /**
     * Returns true if a file is an image (for specific backup rules).
     */
    public static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
    }

    /**
     * Helper to get a human-readable size for the UI.
     */
    public static String getReadableSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#")
                .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}