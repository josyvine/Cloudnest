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
 * UPDATED: Now parses and passes network speed and detailed progress to the adapter.
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
        binding.recyclerViewUploads.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UploadQueueAdapter(requireContext(), new ArrayList<>(), this);
        binding.recyclerViewUploads.setAdapter(adapter);
    }

    private void setupClearAllButton() {
        binding.btnClearCompleted.setOnClickListener(v -> {
            workManager.pruneWork(); // Removes 'SUCCEEDED' or 'FAILED' works from internal DB
            Toast.makeText(requireContext(), "Completed logs cleared.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Observes LiveData from WorkManager.
     * Updates the UI whenever a background task updates its progress or state.
     */
    private void observeUploads() {
        // We observe both Manual Uploads and Auto Backups
        workManager.getWorkInfosByTagLiveData("MANUAL_UPLOAD")
                .observe(getViewLifecycleOwner(), new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(List<WorkInfo> workInfos) {
                        if (workInfos == null || workInfos.isEmpty()) {
                            binding.tvEmptyQueue.setVisibility(View.VISIBLE);
                            binding.recyclerViewUploads.setVisibility(View.GONE);
                            adapter.updateList(new ArrayList<>());
                            return;
                        }

                        binding.tvEmptyQueue.setVisibility(View.GONE);
                        binding.recyclerViewUploads.setVisibility(View.VISIBLE);

                        List<UploadItemModel> uiModels = new ArrayList<>();

                        for (WorkInfo info : workInfos) {
                            String fileName = info.getProgress().getString("CURRENT_FILE");
                            if (fileName == null) fileName = "Preparing Upload...";
                            
                            int progress = info.getProgress().getInt("PROGRESS_PERCENT", 0);
                            String speed = info.getProgress().getString("SPEED");
                            String details = info.getProgress().getString("DETAILS");
                            
                            // Determine display state
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
                                    speed,     // NEW field
                                    details    // NEW field
                            ));
                        }

                        // Update the adapter with new data
                        adapter.updateList(uiModels);
                    }
                });
    }

    // --- Interaction Listener Implementation ---

    @Override
    public void onCancelClicked(UUID workId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Upload?")
                .setMessage("Are you sure you want to stop this upload?")
                .setPositiveButton("Yes, Stop", (dialog, which) -> {
                    workManager.cancelWorkById(workId);
                    Toast.makeText(requireContext(), "Cancellation requested...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onRetryClicked(UUID workId) {
        Toast.makeText(requireContext(), "Please re-select files to retry upload.", Toast.LENGTH_LONG).show();
        workManager.pruneWork(); 
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}