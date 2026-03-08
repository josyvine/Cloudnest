package com.cloudnest.app;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.cloudnest.app.databinding.FragmentStorageGraphBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Visual Storage Analytics Page.
 * Displays dedicated progress bars (Green/Yellow/Red) for:
 * 1. Google Drive (Cloud)
 * 2. Internal Phone Storage
 * 3. SD Card Storage (if available)
 */
public class StorageGraphFragment extends Fragment {

    private FragmentStorageGraphBinding binding;
    private ExecutorService networkExecutor;

    private static final long GIGABYTE = 1073741824L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStorageGraphBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        networkExecutor = Executors.newSingleThreadExecutor();

        setupDetailButtons();
        refreshStorageStats();
    }

    private void setupDetailButtons() {
        binding.btnDriveDetails.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_storageGraph_to_driveBrowser)
        );

        binding.btnPhoneDetails.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("STORAGE_TYPE", "PHONE");
            Navigation.findNavController(v).navigate(R.id.action_storageGraph_to_localBrowser, bundle);
        });

        binding.btnSdDetails.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("STORAGE_TYPE", "SD_CARD");
            Navigation.findNavController(v).navigate(R.id.action_storageGraph_to_localBrowser, bundle);
        });
    }

    private void refreshStorageStats() {
        updatePhoneGraph();
        updateSdCardGraph();
        fetchDriveQuota();
    }

    /**
     * Updates Internal Storage Progress Bar & Text.
     */
    private void updatePhoneGraph() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());

        long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
        long freeBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        long usedBytes = totalBytes - freeBytes;

        int progress = (int) (((double) usedBytes / totalBytes) * 100);
        
        binding.progressPhone.setProgress(progress);
        binding.tvPhoneUsage.setText(String.format("%.1f GB used of %.1f GB", 
                (double) usedBytes / GIGABYTE, (double) totalBytes / GIGABYTE));
        
        applyColorRule(binding.progressPhone, progress);
    }

    /**
     * Updates SD Card Progress Bar & Text.
     */
    private void updateSdCardGraph() {
        File[] externalDirs = ContextCompat.getExternalFilesDirs(requireContext(), null);
        File sdCard = (externalDirs.length > 1) ? externalDirs[1] : null;

        if (sdCard != null && Environment.isExternalStorageRemovable(sdCard)) {
            // Find root of SD card
            while (sdCard.getParentFile() != null && sdCard.getParentFile().canRead()) {
                File parent = sdCard.getParentFile();
                if (parent.getAbsolutePath().equals("/storage")) break;
                sdCard = parent;
            }

            StatFs stat = new StatFs(sdCard.getPath());
            long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
            long freeBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            long usedBytes = totalBytes - freeBytes;

            int progress = (int) (((double) usedBytes / totalBytes) * 100);

            binding.layoutSdCard.setVisibility(View.VISIBLE);
            binding.progressSd.setProgress(progress);
            binding.tvSdUsage.setText(String.format("%.1f GB used of %.1f GB", 
                    (double) usedBytes / GIGABYTE, (double) totalBytes / GIGABYTE));
            
            applyColorRule(binding.progressSd, progress);
        } else {
            binding.layoutSdCard.setVisibility(View.GONE);
        }
    }

    /**
     * Fetches Google Drive Quota and updates Cloud Progress Bar.
     */
    private void fetchDriveQuota() {
        networkExecutor.execute(() -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                if (account == null) {
                    requireActivity().runOnUiThread(() -> 
                        binding.tvDriveUsage.setText("Not connected to Google Drive")
                    );
                    return;
                }

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        requireContext(), Collections.singleton(DriveScopes.DRIVE_READONLY));
                credential.setSelectedAccount(account.getAccount());

                Drive driveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(getString(R.string.app_name))
                        .build();

                About about = driveService.about().get().setFields("storageQuota").execute();
                About.StorageQuota quota = about.getStorageQuota();

                long totalBytes = quota.getLimit();
                long usedBytes = quota.getUsage();

                int progress = (int) (((double) usedBytes / totalBytes) * 100);

                requireActivity().runOnUiThread(() -> {
                    binding.progressDrive.setProgress(progress);
                    binding.tvDriveUsage.setText(String.format("%.1f GB used of %.1f GB", 
                            (double) usedBytes / GIGABYTE, (double) totalBytes / GIGABYTE));
                    
                    applyColorRule(binding.progressDrive, progress);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> 
                    binding.tvDriveUsage.setText("Error syncing Drive quota")
                );
            }
        });
    }

    /**
     * Applies color to ProgressBar based on percentage.
     * < 70% Green | 70-90% Yellow | > 90% Red
     */
    private void applyColorRule(ProgressBar progressBar, int progress) {
        if (progress >= 90) {
            progressBar.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.cloudnest_status_red));
        } else if (progress >= 70) {
            progressBar.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.cloudnest_status_yellow));
        } else {
            progressBar.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.cloudnest_status_green));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkExecutor != null) networkExecutor.shutdown();
        binding = null;
    }
}