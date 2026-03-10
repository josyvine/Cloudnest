package com.cloudnest.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloudnest.app.databinding.FragmentFileBrowserBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment to browse and manage Google Drive Trash.
 * Allows users to restore files or permanently delete them.
 * FIXES: Glitch 2 (No option to see trash or permanently remove).
 */
public class TrashBrowserFragment extends Fragment implements FileBrowserAdapter.OnFileItemClickListener {

    private static final String TAG = "TrashBrowserFragment";
    private FragmentFileBrowserBinding binding;
    private FileBrowserAdapter adapter;
    private Drive driveService;
    private ExecutorService networkExecutor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        networkExecutor = Executors.newSingleThreadExecutor();
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

        // Update Toolbar
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Trash bin");
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("Google Drive");
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            driveService = DriveApiHelper.getDriveService(requireContext(), account);
            setupRecyclerView();
            loadTrashFiles();
        } else {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        adapter = new FileBrowserAdapter(requireContext(), new ArrayList<>(), this);
        binding.recyclerViewFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewFiles.setAdapter(adapter);
    }

    /**
     * Queries Google Drive for files where trashed = true.
     */
    private void loadTrashFiles() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyFolder.setVisibility(View.GONE);

        networkExecutor.execute(() -> {
            try {
                // Query only trashed items
                String query = "trashed = true";
                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, mimeType, size, modifiedTime, thumbnailLink)")
                        .execute();

                List<File> driveFiles = result.getFiles();
                List<FileItemModel> uiList = new ArrayList<>();

                if (driveFiles != null) {
                    for (File file : driveFiles) {
                        boolean isDir = "application/vnd.google-apps.folder".equals(file.getMimeType());
                        FileItemModel model = new FileItemModel(
                                file.getId(),
                                file.getName(),
                                isDir,
                                file.getModifiedTime() != null ? file.getModifiedTime().getValue() : 0,
                                file.getSize() != null ? file.getSize() : 0,
                                file.getMimeType(),
                                null // webLink not needed for trash
                        );
                        model.setThumbnailUrl(file.getThumbnailLink());
                        uiList.add(model);
                    }
                }

                Collections.sort(uiList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            adapter.updateList(uiList);
                            binding.progressBar.setVisibility(View.GONE);
                            if (uiList.isEmpty()) {
                                binding.tvEmptyFolder.setText("Trash is empty");
                                binding.tvEmptyFolder.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Load trash error", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null) binding.progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    @Override
    public void onFileClicked(FileItemModel file) {
        showTrashOptions(file);
    }

    @Override
    public void onFileLongClicked(FileItemModel file) {
        showTrashOptions(file);
    }

    private void showTrashOptions(FileItemModel file) {
        CharSequence[] options = {"Restore", "Delete Permanently"};
        new AlertDialog.Builder(requireContext())
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) restoreFile(file);
                    else if (which == 1) confirmPermanentDelete(file);
                }).show();
    }

    private void restoreFile(FileItemModel file) {
        networkExecutor.execute(() -> {
            try {
                File meta = new File();
                meta.setTrashed(false);
                driveService.files().update(file.getDriveId(), meta).execute();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Item restored", Toast.LENGTH_SHORT).show();
                        loadTrashFiles();
                    });
                }
            } catch (IOException e) { Log.e(TAG, "Restore error", e); }
        });
    }

    private void confirmPermanentDelete(FileItemModel file) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Permanently?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete Forever", (d, w) -> {
                    networkExecutor.execute(() -> {
                        try {
                            DriveApiHelper.deleteFilePermanently(driveService, file.getDriveId());
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Permanently deleted", Toast.LENGTH_SHORT).show();
                                    loadTrashFiles();
                                });
                            }
                        } catch (IOException e) { Log.e(TAG, "Hard delete error", e); }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(Menu.NONE, 300, Menu.NONE, "Empty Trash");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 300) {
            confirmEmptyTrash();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmEmptyTrash() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Empty Trash?")
                .setMessage("All items will be permanently removed.")
                .setPositiveButton("Empty Now", (d, w) -> {
                    networkExecutor.execute(() -> {
                        try {
                            DriveApiHelper.emptyTrash(driveService);
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Trash Emptied", Toast.LENGTH_SHORT).show();
                                    loadTrashFiles();
                                });
                            }
                        } catch (IOException e) { Log.e(TAG, "Empty trash error", e); }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkExecutor != null) networkExecutor.shutdown();
        binding = null;
    }
}