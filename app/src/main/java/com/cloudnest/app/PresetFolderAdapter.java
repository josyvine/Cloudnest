package com.cloudnest.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudnest.app.databinding.ItemPresetFolderBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Preset Folders list.
 * Displays folders configured for Auto-Backup, showing last sync time and
 * providing actions to manually sync, remove, or view the target on Drive.
 */
public class PresetFolderAdapter extends RecyclerView.Adapter<PresetFolderAdapter.PresetViewHolder> {

    private final Context context;
    private List<PresetFolderEntity> presetList;
    private final OnPresetActionListener listener;

    public interface OnPresetActionListener {
        void onSyncNowClicked(PresetFolderEntity folder);
        void onRemoveClicked(PresetFolderEntity folder);
        void onViewInDriveClicked(PresetFolderEntity folder);
    }

    public PresetFolderAdapter(Context context, List<PresetFolderEntity> presetList, OnPresetActionListener listener) {
        this.context = context;
        this.presetList = presetList;
        this.listener = listener;
    }

    public void updateList(List<PresetFolderEntity> newList) {
        this.presetList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPresetFolderBinding binding = ItemPresetFolderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new PresetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        holder.bind(presetList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return presetList.size();
    }

    static class PresetViewHolder extends RecyclerView.ViewHolder {
        private final ItemPresetFolderBinding binding;

        PresetViewHolder(ItemPresetFolderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PresetFolderEntity folder, OnPresetActionListener listener) {
            binding.tvFolderName.setText(folder.folderName);
            
            // Format the timestamp
            if (folder.lastSyncTime > 0) {
                String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date(folder.lastSyncTime));
                binding.tvLastSync.setText("Last synced: " + date);
            } else {
                binding.tvLastSync.setText("Never synced");
            }

            // Buttons
            binding.btnSyncNow.setOnClickListener(v -> listener.onSyncNowClicked(folder));
            binding.btnRemove.setOnClickListener(v -> listener.onRemoveClicked(folder));
            binding.btnViewInDrive.setOnClickListener(v -> listener.onViewInDriveClicked(folder));
        }
    }
}