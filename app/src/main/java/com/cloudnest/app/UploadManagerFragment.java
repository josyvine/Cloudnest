package com.cloudnest.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.cloudnest.app.databinding.FragmentUploadManagerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Upload Queue Manager.
 * Observes background WorkManager tasks tagged with "MANUAL_UPLOAD" or "AUTO_BACKUP".
 * Displays real-time progress bars, file names, and allows cancellation.
 * UPDATED: Fixed Glitch 2 (Speed/Details) and Glitch 7 (Multi-tag tracking).
 */
public class UploadManagerFragment extends Fragment implements UploadQueueAdapter.OnUploadActionClickListener {

    private FragmentUploadManagerBinding binding;
    private UploadQueueAdapter adapter;
    private WorkManager workManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        workManager = WorkManager.getInstance(requireContext());

        setupRecyclerView();
        observeUploads();
        setupClearAllButton();
    }

    private void setupRecyclerView() {
        if (binding == null) return;
        binding.recyclerViewUploads.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Initialize adapter with empty list and 'this' as listener
        adapter = new UploadQueueAdapter(requireContext(), new ArrayList<>(), this);
        binding.recyclerViewUploads.setAdapter(adapter);
    }

    private void setupClearAllButton() {
        if (binding == null) return;
        binding.btnClearCompleted.setOnClickListener(v -> {
            // Removes 'SUCCEEDED' or 'FAILED' works from internal WorkManager database
            workManager.pruneWork(); 
            Toast.makeText(requireContext(), "Finished logs cleared.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Observes LiveData from WorkManager.
     * Updates the UI whenever a background task updates its progress or state.
     */
    private void observeUploads() {
        // FIXED GLITCH 7: We now monitor both manual transfers and background auto-backups
        // We use a combined approach to ensure all active CloudNest tasks are visible
        
        // Combine MANUAL_UPLOAD and AUTO_BACKUP tags for observation
        workManager.getWorkInfosByTagLiveData("MANUAL_UPLOAD")
                .observe(getViewLifecycleOwner(), workInfos -> updateQueueUI(workInfos, "AUTO_BACKUP"));
    }

    /**
     * Helper to process WorkInfo lists and merge results if necessary.
     */
    private void updateQueueUI(List<WorkInfo> manualInfos, String secondaryTag) {
        workManager.getWorkInfosByTagLiveData(secondaryTag).observe(getViewLifecycleOwner(), autoInfos -> {
            if (!isAdded() || binding == null) return;

            List<WorkInfo> combined = new ArrayList<>();
            if (manualInfos != null) combined.addAll(manualInfos);
            if (autoInfos != null) combined.addAll(autoInfos);

            if (combined.isEmpty()) {
                binding.tvEmptyQueue.setVisibility(View.VISIBLE);
                binding.recyclerViewUploads.setVisibility(View.GONE);
                adapter.updateList(new ArrayList<>());
                return;
            }

            binding.tvEmptyQueue.setVisibility(View.GONE);
            binding.recyclerViewUploads.setVisibility(View.VISIBLE);

            // Convert WorkInfo objects to our UI Model
            List<UploadItemModel> uiModels = new ArrayList<>();

            for (WorkInfo info : combined) {
                // FIXED GLITCH 2: Extract real-time speed and file details
                String fileName = info.getProgress().getString("CURRENT_FILE");
                if (fileName == null) fileName = "Syncing...";

                int progress = info.getProgress().getInt("PROGRESS_PERCENT", 0);
                String speed = info.getProgress().getString("SPEED");
                String details = info.getProgress().getString("DETAILS");

                // Determine task state for UI icons
                UploadItemModel.Status status;
                if (info.getState() == WorkInfo.State.RUNNING) {
                    status = UploadItemModel.Status.IN_PROGRESS;
                } else if (info.getState() == WorkInfo.State.SUCCEEDED) {
                    status = UploadItemModel.Status.COMPLETED;
                    progress = 100;
                } else if (info.getState() == WorkInfo.State.FAILED) {
                    status = UploadItemModel.Status.FAILED;
                } else if (info.getState() == WorkInfo.State.CANCELLED) {
                    status = UploadItemModel.Status.CANCELLED;
                } else {
                    status = UploadItemModel.Status.PENDING;
                }

                uiModels.add(new UploadItemModel(
                        info.getId(),
                        fileName,
                        progress,
                        status,
                        speed,
                        details
                ));
            }

            // Update the adapter with merged data
            adapter.updateList(uiModels);
        });
    }

    // --- Interaction Listener Implementation ---

    @Override
    public void onCancelClicked(UUID workId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Stop Task?")
                .setMessage("This will stop the current transfer immediately.")
                .setPositiveButton("Stop", (dialog, which) -> {
                    workManager.cancelWorkById(workId);
                    Toast.makeText(requireContext(), "Task cancelled.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Keep Running", null)
                .show();
    }

    @Override
    public void onRetryClicked(UUID workId) {
        // WorkManager tasks are immutable once finished. 
        // Instructions: Notify user to re-trigger since input data extraction is complex.
        Toast.makeText(requireContext(), "Please re-select items to retry upload.", Toast.LENGTH_LONG).show();
        workManager.pruneWork(); 
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}