package com.cloudnest.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.cloudnest.app.databinding.FragmentSettingsBinding;

/**
 * App Settings Configuration Page.
 * Manages user preferences:
 * - Auto Backup (Enable/Disable)
 * - Upload Only on WiFi
 * - Notifications
 * - Dark Mode
 * - Background Sync
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;

    // Preference Keys
    public static final String PREF_NAME = "cloudnest_prefs";
    public static final String KEY_AUTO_BACKUP = "auto_backup_enabled";
    public static final String KEY_WIFI_ONLY = "wifi_only_enabled";
    public static final String KEY_NOTIFICATIONS = "notifications_enabled";
    public static final String KEY_DARK_MODE = "dark_mode_enabled";
    public static final String KEY_BACKGROUND_SYNC = "background_sync_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        loadSavedPreferences();
        setupListeners();
    }

    /**
     * Loads the current state of switches from SharedPreferences.
     */
    private void loadSavedPreferences() {
        binding.switchAutoBackup.setChecked(prefs.getBoolean(KEY_AUTO_BACKUP, true));
        binding.switchWifiOnly.setChecked(prefs.getBoolean(KEY_WIFI_ONLY, true));
        binding.switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        binding.switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        binding.switchBackgroundSync.setChecked(prefs.getBoolean(KEY_BACKGROUND_SYNC, true));
    }

    /**
     * Attaches listeners to handle toggle changes.
     */
    private void setupListeners() {
        // Auto Backup Toggle
        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_AUTO_BACKUP, isChecked).apply();
            String status = isChecked ? "Enabled" : "Disabled";
            Toast.makeText(getContext(), "Auto Backup " + status, Toast.LENGTH_SHORT).show();
        });

        // WiFi Only Toggle
        binding.switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_WIFI_ONLY, isChecked).apply();
        });

        // Notifications Toggle
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
        });

        // Dark Mode Toggle (Requires App Restart or Recreate)
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            applyTheme(isChecked);
        });

        // Background Sync Toggle
        binding.switchBackgroundSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BACKGROUND_SYNC, isChecked).apply();
            if (!isChecked) {
                Toast.makeText(getContext(), "Background sync paused.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Applies the Night Mode setting immediately.
     */
    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // Recreate activity to apply theme change
        requireActivity().recreate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}