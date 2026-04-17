package com.group04.scrapbookwidget.ui.group;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;

public class MessageDiffCallback extends DiffUtil.ItemCallback<MessageWrapper> {

    @Override
    public boolean areItemsTheSame(@NonNull MessageWrapper oldItem, @NonNull MessageWrapper newItem) {
        if (oldItem.message == null || newItem.message == null) return false;
        return Objects.equals(oldItem.message.getId(), newItem.message.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull MessageWrapper oldItem, @NonNull MessageWrapper newItem) {
        // MessageWrapper.equals compares both the message content/status and the lastSeenUsers list
        return Objects.equals(oldItem, newItem);
    }
}
