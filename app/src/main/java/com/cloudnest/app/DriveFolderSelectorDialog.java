package com.cloudnest.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Custom Dialog for selecting a target folder on Google Drive.
 * Addresses Glitch 5 by allowing users to choose the destination path.
 */
public class DriveFolderSelectorDialog extends DialogFragment {

    public interface OnFolderSelectedListener {
        void onFolderSelected(String folderId, String folderName);
    }

    private OnFolderSelectedListener listener;
    private Drive driveService;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvPath;
    private String currentFolderId = "root";
    private String currentFolderName = "My Drive";
    private Stack<FolderRef> navStack = new Stack<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private static class FolderRef {
        String id;
        String name;
        FolderRef(String id, String name) { this.id = id; this.name = name; }
    }

    public static DriveFolderSelectorDialog newInstance(OnFolderSelectedListener listener) {
        DriveFolderSelectorDialog dialog = new DriveFolderSelectorDialog();
        dialog.listener = listener;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_drive_selector, container, false);
        
        recyclerView = v.findViewById(R.id.rv_folder_list);
        progressBar = v.findViewById(R.id.pb_loading_folders);
        tvPath = v.findViewById(R.id.tv_current_path);
        Button btnSelect = v.findViewById(R.id.btn_select_this_folder);
        Button btnBack = v.findViewById(R.id.btn_folder_back);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Authenticate Drive
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            driveService = DriveApiHelper.getDriveService(requireContext(), account);
            loadFolders(currentFolderId, currentFolderName);
        }

        btnSelect.setOnClickListener(view -> {
            if (listener != null) listener.onFolderSelected(currentFolderId, currentFolderName);
            dismiss();
        });

        btnBack.setOnClickListener(view -> {
            if (!navStack.isEmpty()) {
                FolderRef prev = navStack.pop();
                loadFolders(prev.id, prev.name);
            } else {
                dismiss();
            }
        });

        return v;
    }

    private void loadFolders(String folderId, String folderName) {
        currentFolderId = folderId;
        currentFolderName = folderName;
        tvPath.setText("Drive > " + folderName);
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String query = "'" + folderId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name)")
                        .execute();

                List<File> folders = result.getFiles();
                if (folders == null) folders = new ArrayList<>();

                final List<File> finalFolders = folders;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        recyclerView.setAdapter(new SimpleFolderAdapter(finalFolders));
                    });
                }

            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private class SimpleFolderAdapter extends RecyclerView.Adapter<SimpleFolderAdapter.ViewHolder> {
        List<File> list;
        SimpleFolderAdapter(List<File> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File folder = list.get(position);
            holder.text.setText(folder.getName());
            holder.itemView.setOnClickListener(v -> {
                navStack.push(new FolderRef(currentFolderId, currentFolderName));
                loadFolders(folder.getId(), folder.getName());
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}