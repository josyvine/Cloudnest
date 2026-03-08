package com.cloudnest.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudnest.app.databinding.ItemDriveAccountBinding;

import java.util.List;

/**
 * Adapter for the Drive Accounts management screen.
 * Lists connected Google Drive accounts, shows which is active, and provides
 * controls to switch the active account or remove one from the app.
 */
public class DriveAccountAdapter extends RecyclerView.Adapter<DriveAccountAdapter.AccountViewHolder> {

    private final Context context;
    private List<DriveAccountEntity> accountList;
    private final OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(DriveAccountEntity account);
        void onRemoveClick(DriveAccountEntity account);
    }

    public DriveAccountAdapter(Context context, List<DriveAccountEntity> accountList, OnAccountClickListener listener) {
        this.context = context;
        this.accountList = accountList;
        this.listener = listener;
    }

    public void updateList(List<DriveAccountEntity> newList) {
        this.accountList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDriveAccountBinding binding = ItemDriveAccountBinding.inflate(LayoutInflater.from(context), parent, false);
        return new AccountViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        holder.bind(accountList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        private final ItemDriveAccountBinding binding;

        AccountViewHolder(ItemDriveAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DriveAccountEntity account, OnAccountClickListener listener) {
            binding.tvAccountEmail.setText(account.email);
            binding.tvDisplayName.setText(account.displayName);

            // Display "Active" status indicator
            if (account.isActive) {
                binding.tvStatus.setText("Active Upload Destination");
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.cloudnest_status_green));
                binding.getRoot().setBackgroundColor(Color.parseColor("#F1F8E9")); // Light green tint
            } else {
                binding.tvStatus.setText("Inactive");
                binding.tvStatus.setTextColor(Color.GRAY);
                binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
            }

            // Display "Full" warning if quota is reached
            if (account.isFull) {
                binding.tvStatus.setText("Storage Full!");
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.cloudnest_status_red));
            }

            // Actions
            binding.btnSwitchAccount.setOnClickListener(v -> listener.onAccountClick(account));
            binding.btnRemoveAccount.setOnClickListener(v -> listener.onRemoveClick(account));
        }
    }
}