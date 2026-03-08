package com.cloudnest.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudnest.app.databinding.ItemFileListViewBinding;
import com.cloudnest.app.databinding.ItemFileGridViewBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Universal Adapter for File Browsing.
 * Supports both Local (Phone/SD) and Cloud (Drive) file items.
 * Switches between List and Grid layouts dynamically.
 */
public class FileBrowserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<FileItemModel> fileList;
    private final OnFileItemClickListener listener;
    private boolean isGridMode = false;
    private final List<FileItemModel> selectedItems = new ArrayList<>();

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    public interface OnFileItemClickListener {
        void onFileClicked(FileItemModel file);
        void onFileLongClicked(FileItemModel file);
    }

    public FileBrowserAdapter(Context context, List<FileItemModel> fileList, OnFileItemClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.listener = listener;
    }

    public void setGridMode(boolean isGrid) {
        this.isGridMode = isGrid;
        notifyDataSetChanged();
    }

    public void updateList(List<FileItemModel> newList) {
        this.fileList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? TYPE_GRID : TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GRID) {
            ItemFileGridViewBinding binding = ItemFileGridViewBinding.inflate(LayoutInflater.from(context), parent, false);
            return new GridViewHolder(binding);
        } else {
            ItemFileListViewBinding binding = ItemFileListViewBinding.inflate(LayoutInflater.from(context), parent, false);
            return new ListViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileItemModel file = fileList.get(position);
        boolean isSelected = selectedItems.contains(file);

        if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).bind(file, listener, isSelected);
        } else if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(file, listener, isSelected);
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    // --- Selection Logic ---

    public void toggleSelection(FileItemModel file) {
        if (selectedItems.contains(file)) {
            selectedItems.remove(file);
        } else {
            selectedItems.add(file);
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedItems.clear();
        selectedItems.addAll(fileList);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<FileItemModel> getAllItems() {
        return fileList;
    }

    // --- ViewHolders ---

    static class ListViewHolder extends RecyclerView.ViewHolder {
        private final ItemFileListViewBinding binding;

        ListViewHolder(ItemFileListViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FileItemModel file, OnFileItemClickListener listener, boolean isSelected) {
            binding.tvFileName.setText(file.getName());
            
            // Show file count if directory
            if (file.isDirectory()) {
                binding.tvFileDetails.setText(file.getChildCount() + " items");
                binding.ivFileIcon.setImageResource(R.drawable.ic_folder);
            } else {
                binding.tvFileDetails.setText(String.format("%.2f MB", file.getSize() / 1024.0 / 1024.0));
                binding.ivFileIcon.setImageResource(R.drawable.ic_file_generic);
            }

            itemView.setBackgroundColor(isSelected ? Color.LTGRAY : Color.TRANSPARENT);
            itemView.setOnClickListener(v -> listener.onFileClicked(file));
            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClicked(file);
                return true;
            });
        }
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        private final ItemFileGridViewBinding binding;

        GridViewHolder(ItemFileGridViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FileItemModel file, OnFileItemClickListener listener, boolean isSelected) {
            binding.tvGridFileName.setText(file.getName());
            
            if (file.isDirectory()) {
                binding.ivGridIcon.setImageResource(R.drawable.ic_folder);
            } else {
                binding.ivGridIcon.setImageResource(R.drawable.ic_file_generic);
            }

            itemView.setBackgroundColor(isSelected ? Color.LTGRAY : Color.TRANSPARENT);
            itemView.setOnClickListener(v -> listener.onFileClicked(file));
            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClicked(file);
                return true;
            });
        }
    }
}