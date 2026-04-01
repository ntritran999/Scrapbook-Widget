package com.group04.scrapbookwidget.ui.group;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.databinding.ItemMemberBinding;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private static final String TAG = "MemberAdapter";
    private List<User> members = new ArrayList<>();
    private boolean isAdmin = false;
    private String ownerId;
    private final OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onRemoveMember(User user);
    }

    public MemberAdapter(OnMemberActionListener listener) {
        this.listener = listener;
    }

    public void setMembers(List<User> members, String ownerId, boolean isAdmin) {
        this.members = members != null ? members : new ArrayList<>();
        this.ownerId = ownerId;
        this.isAdmin = isAdmin;
        Log.d(TAG, "setMembers: size=" + this.members.size() + ", ownerId=" + ownerId + ", isAdmin=" + isAdmin);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberBinding binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = members.get(position);
        if (user == null) return;

        Log.d(TAG, "Binding user at " + position + ": name=" + user.getDisplayName() + ", username=" + user.getUsername() + ", avatar=" + user.getAvatarUrl());

        holder.binding.setUser(user);
        holder.binding.setIsAdmin(isAdmin);
        
        // Check owner using both uid and id for compatibility
        String userId = user.getUid();
        if (userId == null) userId = user.getId();
        
        boolean isOwner = userId != null && userId.equals(ownerId);
        holder.binding.setIsOwner(isOwner);

        holder.binding.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveMember(user);
            }
        });

        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMemberBinding binding;

        ViewHolder(ItemMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
