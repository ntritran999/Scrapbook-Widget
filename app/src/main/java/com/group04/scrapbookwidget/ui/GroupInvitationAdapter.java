package com.group04.scrapbookwidget.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Invitation;
import com.group04.scrapbookwidget.databinding.ItemGroupInvitationBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupInvitationAdapter extends RecyclerView.Adapter<GroupInvitationAdapter.ViewHolder> {

    private List<Invitation> invitations = new ArrayList<>();
    private final OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAccept(Invitation invitation);
        void onDecline(Invitation invitation);
    }

    public GroupInvitationAdapter(OnInvitationActionListener listener) {
        this.listener = listener;
    }

    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations != null ? invitations : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupInvitationBinding binding = ItemGroupInvitationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);
        holder.binding.setInvitation(invitation);
        
        if (invitation.getGroup() != null && invitation.getGroup().getAvatarUrl() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(invitation.getGroup().getAvatarUrl())
                    .placeholder(R.drawable.account_circle_24)
                    .circleCrop()
                    .into(holder.binding.ivGroupAvatar);
        } else {
            holder.binding.ivGroupAvatar.setImageResource(R.drawable.account_circle_24);
        }

        holder.binding.setOnAccept(v -> {
            if (listener != null) listener.onAccept(invitation);
        });
        
        holder.binding.setOnDecline(v -> {
            if (listener != null) listener.onDecline(invitation);
        });
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemGroupInvitationBinding binding;

        ViewHolder(ItemGroupInvitationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
