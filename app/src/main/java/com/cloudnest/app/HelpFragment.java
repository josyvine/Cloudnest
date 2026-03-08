package com.cloudnest.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudnest.app.databinding.FragmentHelpBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * User Guide & Help Page.
 * Displays step-by-step instructions on how to use CloudNest features:
 * 1. Login & Connect Drive
 * 2. Browse & Upload Files
 * 3. Auto-Backup (Preset Folders)
 * 4. Multi-Account Switching
 */
public class HelpFragment extends Fragment {

    private FragmentHelpBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHelpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupInstructionsList();
    }

    private void setupInstructionsList() {
        // We use a simple RecyclerView to display the help steps cleanly
        List<HelpItem> helpItems = new ArrayList<>();

        helpItems.add(new HelpItem("1. Connect Google Drive", 
                "Tap 'Continue with Google' on the home screen. You can add multiple accounts in the 'Drive Accounts' menu if one gets full."));

        helpItems.add(new HelpItem("2. Browse Files", 
                "Use the Dashboard to access 'Phone Storage' or 'SD Card'. Tap folders to open them. Use the back button to go up."));

        helpItems.add(new HelpItem("3. Select & Upload", 
                "Long press any file or folder to enter Selection Mode. Select multiple items, then tap the Upload icon in the top menu."));

        helpItems.add(new HelpItem("4. Auto-Backup (Preset Folders)", 
                "To automatically sync a folder: Long press it in the file browser -> Select 'Add to Preset'. New files will upload in the background automatically."));

        helpItems.add(new HelpItem("5. Offline Access", 
                "In the Google Drive browser, long press a file and select 'Make Available Offline' to save it to your local cache."));

        helpItems.add(new HelpItem("6. Drive Full?", 
                "If your Drive is full, add a second account in 'Drive Accounts'. CloudNest will automatically switch to the new account for future uploads."));

        HelpAdapter adapter = new HelpAdapter(helpItems);
        binding.recyclerViewHelp.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewHelp.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Simple Internal Adapter for Help List ---

    private static class HelpItem {
        String title;
        String description;

        HelpItem(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private static class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.HelpViewHolder> {

        private final List<HelpItem> items;

        HelpAdapter(List<HelpItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using standard android list item layout or a custom one defined in XML later
            // For now, we inflate a simple custom view holder
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_help_step, parent, false);
            return new HelpViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
            HelpItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDesc.setText(item.description);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HelpViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvDesc;

            HelpViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_help_title);
                tvDesc = itemView.findViewById(R.id.tv_help_desc);
            }
        }
    }
}