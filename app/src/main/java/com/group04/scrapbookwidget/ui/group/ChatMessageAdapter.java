package com.group04.scrapbookwidget.ui.group;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.databinding.ItemChatMessageBinding;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private final String currentUserId;
    private OnMessageVisibleListener onMessageVisibleListener;
    private OnResendClickListener onResendClickListener;

    public interface OnMessageVisibleListener {
        void onMessageVisible(Message message);
    }

    public interface OnResendClickListener {
        void onResendClick(Message message);
    }

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnMessageVisibleListener(OnMessageVisibleListener listener) {
        this.onMessageVisibleListener = listener;
    }

    public void setOnResendClickListener(OnResendClickListener listener) {
        this.onResendClickListener = listener;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
        
        // Trigger seen logic if it's not our own message and we haven't seen it yet
        String senderId = message.getCreatedBy() != null ? message.getCreatedBy() : message.getSenderId();
        boolean isSentByMe = currentUserId != null && currentUserId.equals(senderId);
        if (onMessageVisibleListener != null && !isSentByMe) {
            boolean alreadySeen = false;
            if (message.getSeenBy() != null) {
                for (Message.SeenBy seen : message.getSeenBy()) {
                    if (seen != null && currentUserId != null && currentUserId.equals(seen.getId())) {
                        alreadySeen = true;
                        break;
                    }
                }
            }
            if (!alreadySeen) {
                onMessageVisibleListener.onMessageVisible(message);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        public MessageViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            String senderId = message.getCreatedBy() != null ? message.getCreatedBy() : message.getSenderId();
            boolean isSentByMe = currentUserId != null && currentUserId.equals(senderId);

            if (isSentByMe) {
                binding.layoutSent.setVisibility(View.VISIBLE);
                binding.layoutReceived.setVisibility(View.GONE);
                binding.tvMessageSent.setText(message.getContent());
                
                // Status Handling
                binding.layoutStatus.setVisibility(View.VISIBLE);
                switch (message.getStatus()) {
                    case SENDING:
                        binding.ivError.setVisibility(View.GONE);
                        binding.tvStatus.setText("Sending...");
                        binding.tvStatus.setTextColor(Color.parseColor("#888888"));
                        binding.layoutStatus.setOnClickListener(null);
                        break;
                    case SENT:
                        binding.ivError.setVisibility(View.GONE);
                        binding.tvStatus.setText("Sent");
                        binding.tvStatus.setTextColor(Color.parseColor("#888888"));
                        binding.layoutStatus.setOnClickListener(null);
                        break;
                    case FAILED:
                        binding.ivError.setVisibility(View.VISIBLE);
                        binding.tvStatus.setText("Error! Resend?");
                        binding.tvStatus.setTextColor(Color.RED);
                        binding.layoutStatus.setOnClickListener(v -> {
                            if (onResendClickListener != null) {
                                onResendClickListener.onResendClick(message);
                            }
                        });
                        break;
                }
            } else {
                binding.layoutSent.setVisibility(View.GONE);
                binding.layoutReceived.setVisibility(View.VISIBLE);
                binding.tvMessageReceived.setText(message.getContent());
                
                Glide.with(itemView.getContext())
                        .load(message.getSenderAvatar())
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(binding.ivUserAvatar);
            }
        }
    }
}
