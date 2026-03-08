package com.cloudnest.app;

import android.content.Context;
import java.io.File;

/**
 * Utility for managing the local Offline Cache for Google Drive files.
 * Features:
 * - Checks if a Drive file exists in local storage.
 * - Resolves the local path for cached files.
 * - Clears the cache directory.
 */
public class OfflineCacheHelper {

    private static final String CACHE_DIR_NAME = "OfflineCache";

    /**
     * Checks if a file from Google Drive has already been cached locally.
     * @param context The app context.
     * @param fileName The name of the file (usually the same as the Drive file name).
     * @return true if the file exists in the cache.
     */
    public static boolean isFileCached(Context context, String fileName) {
        File cacheDir = new File(context.getExternalFilesDir(null), CACHE_DIR_NAME);
        File cachedFile = new File(cacheDir, fileName);
        return cachedFile.exists();
    }

    /**
     * Gets the path to a cached file.
     * @param context The app context.
     * @param fileName The name of the file.
     * @return The local File object.
     */
    public static File getCachedFile(Context context, String fileName) {
        File cacheDir = new File(context.getExternalFilesDir(null), CACHE_DIR_NAME);
        return new File(cacheDir, fileName);
    }

    /**
     * Clears all cached Drive files to free up space.
     * @param context The app context.
     */
    public static void clearCache(Context context) {
        File cacheDir = new File(context.getExternalFilesDir(null), CACHE_DIR_NAME);
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Calculates the total size of the offline cache in bytes.
     * Useful for displaying "Cache Size" in the Settings page.
     */
    public static long getCacheSize(Context context) {
        File cacheDir = new File(context.getExternalFilesDir(null), CACHE_DIR_NAME);
        long size = 0;
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += file.length();
                }
            }
        }
        return size;
    }
}