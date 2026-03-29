package com.group04.scrapbookwidget.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.databinding.ItemGroupBinding;

import java.util.List;

public class GroupSelectionRecyclerAdapter extends RecyclerView.Adapter<GroupSelectionRecyclerAdapter.GroupViewHolder> {
    private List<Group> groups;
    private Context context;
    private OnGroupSelectListener listener;

    public interface OnGroupSelectListener {
        void onGroupSelected(Group group);
    }

    public GroupSelectionRecyclerAdapter(Context context, List<Group> groups, OnGroupSelectListener listener) {
        this.context = context;
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupBinding binding = ItemGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new GroupViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.bind(group, listener);
    }

    @Override
    public int getItemCount() {
        return groups != null ? groups.size() : 0;
    }

    public void updateGroups(List<Group> newGroups) {
        this.groups = newGroups;
        notifyDataSetChanged();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        private ItemGroupBinding binding;

        public GroupViewHolder(@NonNull ItemGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Group group, OnGroupSelectListener listener) {
            binding.tvGroupName.setText(group.getGroupName());
            
            Glide.with(binding.ivGroupAvatar.getContext())
                    .load(group.getAvatarUrl())
                    .circleCrop()
                    .into(binding.ivGroupAvatar);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGroupSelected(group);
                }
            });
        }
    }
}
