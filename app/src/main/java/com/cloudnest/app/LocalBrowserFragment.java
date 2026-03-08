package com.cloudnest.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.cloudnest.app.databinding.FragmentFileBrowserBinding;
import com.cloudnest.app.FileBrowserAdapter; // To be generated next
import com.cloudnest.app.FileItemModel; // To be generated
import com.cloudnest.app.UploadWorker; // To be generated

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Local File Browser (Phone & SD Card).
 * Handles directory navigation, file counting, and selection mode.
 * Triggers the recursive upload logic to Google Drive.
 */
public class LocalBrowserFragment extends Fragment implements FileBrowserAdapter.OnFileItemClickListener {

    private FragmentFileBrowserBinding binding;
    private FileBrowserAdapter adapter;
    private File currentDirectory;
    private File rootDirectory;
    private boolean isGridMode = false;

    // Selection Mode
    private ActionMode actionMode;
    private final List<FileItemModel> selectedFiles = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable top menu for Sort/Grid toggle

        // Handle Back Button to navigate up folder hierarchy instead of exiting
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentDirectory != null && !currentDirectory.equals(rootDirectory)) {
                    loadDirectory(currentDirectory.getParentFile());
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileBrowserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Determine Root Storage based on Arguments
        String storageType = getArguments() != null ? getArguments().getString("STORAGE_TYPE", "PHONE") : "PHONE";
        rootDirectory = getRootPath(storageType);
        currentDirectory = rootDirectory;

        // 2. Setup RecyclerView
        setupRecyclerView();

        // 3. Load initial files
        loadDirectory(currentDirectory);
    }

    /**
     * Determines the root path for Internal Storage or SD Card.
     */
    private File getRootPath(String type) {
        if ("SD_CARD".equals(type)) {
            File[] fs = requireContext().getExternalFilesDirs(null);
            if (fs.length > 1 && fs[1] != null) {
                // Return the root of the SD card, not the app-specific folder
                File sdRoot = fs[1];
                while (sdRoot.getParentFile() != null && sdRoot.getParentFile().canRead()) {
                    File parent = sdRoot.getParentFile();
                    if (parent.getAbsolutePath().equals("/storage")) break;
                    sdRoot = parent;
                }
                return sdRoot;
            }
        }
        return Environment.getExternalStorageDirectory(); // Default to Internal Storage
    }

    private void setupRecyclerView() {
        adapter = new FileBrowserAdapter(requireContext(), new ArrayList<>(), this);
        binding.recyclerViewFiles.setAdapter(adapter);
        setLayoutManager();
    }

    private void setLayoutManager() {
        if (isGridMode) {
            binding.recyclerViewFiles.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        } else {
            binding.recyclerViewFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    /**
     * Loads files from the specified directory, counts them, and updates the UI.
     */
    private void loadDirectory(File directory) {
        currentDirectory = directory;
        
        // Update Title
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(directory.getName());
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(directory.getAbsolutePath());
        }

        File[] files = directory.listFiles();
        List<FileItemModel> fileList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isHidden()) continue; // Skip hidden files

                int childCount = 0;
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    childCount = (subFiles != null) ? subFiles.length : 0;
                }

                fileList.add(new FileItemModel(
                        file.getName(),
                        file.getAbsolutePath(),
                        file.isDirectory(),
                        file.lastModified(),
                        file.length(),
                        childCount
                ));
            }
        }

        // Sort: Folders first, then Files. Alphabetical.
        Collections.sort(fileList, new Comparator<FileItemModel>() {
            @Override
            public int compare(FileItemModel o1, FileItemModel o2) {
                if (o1.isDirectory() && !o2.isDirectory()) return -1;
                if (!o1.isDirectory() && o2.isDirectory()) return 1;
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        adapter.updateList(fileList);
        
        if (fileList.isEmpty()) {
            binding.tvEmptyFolder.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmptyFolder.setVisibility(View.GONE);
        }
    }

    // --- Adapter Interfaces ---

    @Override
    public void onFileClicked(FileItemModel fileModel) {
        if (actionMode != null) {
            toggleSelection(fileModel);
            return;
        }

        File file = new File(fileModel.getPath());
        if (file.isDirectory()) {
            loadDirectory(file);
        } else {
            openFile(file);
        }
    }

    @Override
    public void onFileLongClicked(FileItemModel fileModel) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
        }
        toggleSelection(fileModel);
    }

    /**
     * Toggles selection state of a file in the adapter.
     */
    private void toggleSelection(FileItemModel file) {
        adapter.toggleSelection(file);
        
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }

        if (selectedFiles.isEmpty()) {
            actionMode.finish();
        } else {
            actionMode.setTitle(selectedFiles.size() + " Selected");
        }
    }

    /**
     * Opens a file using the system default viewer.
     */
    private void openFile(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "*/*"); // Let system decide type
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app found to open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Options Menu (Sort & View Toggle) ---

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.browser_top_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_view) {
            isGridMode = !isGridMode;
            setLayoutManager();
            adapter.setGridMode(isGridMode);
            binding.recyclerViewFiles.setAdapter(adapter); // Re-bind to refresh view types
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Action Mode Callback (Selection Context Menu) ---

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selection_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_upload) {
                confirmUpload();
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete();
                return true;
            } else if (id == R.id.action_share) {
                shareSelectedFiles();
                return true;
            } else if (id == R.id.action_select_all) {
                adapter.selectAll();
                selectedFiles.clear();
                selectedFiles.addAll(adapter.getAllItems());
                mode.setTitle(selectedFiles.size() + " Selected");
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            selectedFiles.clear();
            actionMode = null;
        }
    };

    /**
     * Logic: Triggers background upload for selected files.
     * Uses WorkManager to handle large queues and folder recursion.
     */
    private void confirmUpload() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Upload to CloudNest?")
                .setMessage("Upload " + selectedFiles.size() + " items to Google Drive? Folders will be preserved recursively.")
                .setPositiveButton("Upload", (dialog, which) -> {
                    
                    // Convert list of paths to array for WorkManager
                    String[] paths = new String[selectedFiles.size()];
                    for (int i = 0; i < selectedFiles.size(); i++) {
                        paths[i] = selectedFiles.get(i).getPath();
                    }

                    // Pass data to UploadWorker
                    Data inputData = new Data.Builder()
                            .putStringArray("FILE_PATHS", paths)
                            .build();

                    OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                            .setInputData(inputData)
                            .addTag("MANUAL_UPLOAD")
                            .build();

                    WorkManager.getInstance(requireContext()).enqueue(uploadRequest);

                    Toast.makeText(requireContext(), "Upload started in background.", Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Files?")
                .setMessage("Are you sure you want to permanently delete " + selectedFiles.size() + " items from your device?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (FileItemModel item : selectedFiles) {
                        File file = new File(item.getPath());
                        deleteRecursive(file);
                    }
                    loadDirectory(currentDirectory); // Refresh list
                    actionMode.finish();
                    Toast.makeText(requireContext(), "Items deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void shareSelectedFiles() {
        ArrayList<Uri> uris = new ArrayList<>();
        for (FileItemModel item : selectedFiles) {
            if (!item.isDirectory()) { // Can only share files, not folders directly via intent
                File file = new File(item.getPath());
                Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
                uris.add(uri);
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "Cannot share empty folders directly.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType("*/*");
        startActivity(Intent.createChooser(shareIntent, "Share files via"));
        actionMode.finish();
    }
}