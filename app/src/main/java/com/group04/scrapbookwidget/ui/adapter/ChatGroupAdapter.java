package com.group04.scrapbookwidget.ui.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatGroupAdapter extends RecyclerView.Adapter<ChatGroupAdapter.ViewHolder> {

    private List<Group> groups;
    private final String currentUserId;
    private final OnGroupClickListener listener;
    private final SimpleDateFormat isoFormat;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public ChatGroupAdapter(List<Group> groups, String currentUserId, OnGroupClickListener listener) {
        this.groups = groups;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        this.isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.tvGroupName.setText(group.getGroupName());
        
        Message latest = group.getLatestMessage();
        if (latest != null) {
            String senderPrefix;
            if (currentUserId != null && currentUserId.equals(latest.getCreatedBy())) {
                senderPrefix = "You: ";
            } else {
                senderPrefix = latest.getSenderName() != null ? latest.getSenderName() + ": " : "";
            }
            holder.tvLastMessage.setText(senderPrefix + latest.getContent());
            
            String timeStr = latest.getCreatedAt();
            if (timeStr != null) {
                try {
                    Date date = isoFormat.parse(timeStr);
                    if (date != null) {
                        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                                date.getTime(),
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                        );
                        holder.tvTime.setText(relativeTime);
                    } else {
                        holder.tvTime.setText(latest.getTime());
                    }
                } catch (ParseException e) {
                    holder.tvTime.setText(latest.getTime());
                }
            } else {
                holder.tvTime.setText(latest.getTime());
            }
        } else {
            holder.tvLastMessage.setText("No messages yet!");
            holder.tvTime.setText("");
        }

        if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(group.getAvatarUrl())
                    .placeholder(R.drawable.account_circle_24)
                    .circleCrop()
                    .into(holder.ivGroupAvatar);
        } else {
            holder.ivGroupAvatar.setImageResource(R.drawable.account_circle_24);
        }

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivGroupAvatar;
        TextView tvGroupName;
        TextView tvLastMessage;
        TextView tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            ivGroupAvatar = itemView.findViewById(R.id.ivGroupAvatar);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
