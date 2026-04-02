package com.group04.scrapbookwidget.ui.group;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.databinding.ItemUserSearchBinding;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private final OnUserInviteListener listener;

    public interface OnUserInviteListener {
        void onInviteUser(User user);
    }

    public UserSearchAdapter(OnUserInviteListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserSearchBinding binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.binding.setUser(user);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.account_circle_24)
                    .circleCrop()
                    .into(holder.binding.ivAvatar);
        } else {
            holder.binding.ivAvatar.setImageResource(R.drawable.account_circle_24);
        }

        holder.binding.btnInvite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInviteUser(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemUserSearchBinding binding;

        ViewHolder(ItemUserSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
