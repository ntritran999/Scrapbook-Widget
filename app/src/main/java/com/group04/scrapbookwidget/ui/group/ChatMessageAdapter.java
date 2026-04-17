package com.group04.scrapbookwidget.ui.group;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.databinding.ItemChatMessageBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatMessageAdapter extends ListAdapter<MessageWrapper, ChatMessageAdapter.MessageViewHolder> {

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
        super(new MessageDiffCallback());
        this.currentUserId = currentUserId;
    }

    public void setOnMessageVisibleListener(OnMessageVisibleListener listener) {
        this.onMessageVisibleListener = listener;
    }

    public void setOnResendClickListener(OnResendClickListener listener) {
        this.onResendClickListener = listener;
    }

    public void setMessages(List<Message> newMessages) {
        setMessages(newMessages, null);
    }

    public void setMessages(@Nullable List<Message> newMessages, @Nullable Runnable onUpdated) {
        if (newMessages == null) {
            submitList(null, onUpdated);
            return;
        }

        // Map userId -> messageId of the latest message they have seen
        Map<String, String> userToLatestMessageId = new HashMap<>();
        // Map userId -> the latest SeenBy details (to get avatar url)
        Map<String, Message.SeenBy> userToLatestDetails = new HashMap<>();

        for (Message m : newMessages) {
            if (m.getSeenBy() != null) {
                for (Message.SeenBy sb : m.getSeenBy()) {
                    String uid = sb.getUserId() != null ? sb.getUserId() : sb.getId();
                    if (uid == null || uid.equals(currentUserId)) continue;
                    
                    userToLatestMessageId.put(uid, m.getId());
                    userToLatestDetails.put(uid, sb);
                }
            }
        }

        // Map messageId -> List of users who last saw it
        Map<String, List<Message.SeenBy>> messageToUsersAt = new HashMap<>();
        for (Map.Entry<String, String> entry : userToLatestMessageId.entrySet()) {
            String uid = entry.getKey();
            String mid = entry.getValue();
            Message.SeenBy details = userToLatestDetails.get(uid);
            if (details != null) {
                List<Message.SeenBy> list = messageToUsersAt.get(mid);
                if (list == null) {
                    list = new ArrayList<>();
                    messageToUsersAt.put(mid, list);
                }
                list.add(details);
            }
        }

        List<MessageWrapper> wrappers = new ArrayList<>(newMessages.size());
        for (Message m : newMessages) {
            List<Message.SeenBy> usersHere = messageToUsersAt.get(m.getId());
            wrappers.add(new MessageWrapper(m, usersHere != null ? usersHere : new ArrayList<>()));
        }

        submitList(wrappers, onUpdated);
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
        MessageWrapper wrapper = getItem(position);
        holder.bind(wrapper);
        
        Message message = wrapper.message;
        String senderId = message.getCreatedBy() != null ? message.getCreatedBy() : message.getSenderId();
        boolean isSentByMe = currentUserId != null && currentUserId.equals(senderId);
        
        if (onMessageVisibleListener != null && !isSentByMe) {
            boolean alreadySeen = false;
            if (message.getSeenBy() != null) {
                for (Message.SeenBy seen : message.getSeenBy()) {
                    String uid = seen.getId() != null ? seen.getId() : seen.getUserId();
                    if (currentUserId != null && currentUserId.equals(uid)) {
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

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        public MessageViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MessageWrapper wrapper) {
            Message message = wrapper.message;
            String senderId = message.getCreatedBy() != null ? message.getCreatedBy() : message.getSenderId();
            boolean isSentByMe = currentUserId != null && currentUserId.equals(senderId);

            if (isSentByMe) {
                binding.layoutSent.setVisibility(View.VISIBLE);
                binding.layoutReceived.setVisibility(View.GONE);
                binding.tvMessageSent.setText(message.getContent());
                
                binding.layoutStatus.setVisibility(View.VISIBLE);
                Message.Status status = message.getStatus();
                if (status == null) status = Message.Status.SENT;

                switch (status) {
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
                binding.tvSenderName.setText(message.getSenderName());
                
                Glide.with(itemView.getContext())
                        .load(message.getSenderAvatar())
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(binding.ivUserAvatar);
            }

            updateSeenByAvatars(wrapper.lastSeenUsers);
        }

        private void updateSeenByAvatars(List<Message.SeenBy> seenByList) {
            int newCount = seenByList != null ? seenByList.size() : 0;
            int currentCount = binding.layoutSeenBy.getChildCount();

            // Hide unused views
            for (int i = newCount; i < currentCount; i++) {
                binding.layoutSeenBy.getChildAt(i).setVisibility(View.GONE);
            }

            if (newCount == 0) return;

            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            int size = (int) (16 * density);

            for (int i = 0; i < newCount; i++) {
                Message.SeenBy seen = seenByList.get(i);
                ShapeableImageView iv;
                if (i < currentCount) {
                    iv = (ShapeableImageView) binding.layoutSeenBy.getChildAt(i);
                    iv.setVisibility(View.VISIBLE);
                } else {
                    iv = new ShapeableImageView(itemView.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.setMargins((int)(4 * density), 0, 0, 0);
                    iv.setLayoutParams(params);
                    iv.setShapeAppearanceModel(iv.getShapeAppearanceModel().toBuilder()
                            .setAllCornerSizes(size / 2f)
                            .build());
                    binding.layoutSeenBy.addView(iv);
                }
                
                String id = seen.getUserId() != null ? seen.getUserId() : seen.getId();
                iv.setTag(id);

                Glide.with(itemView.getContext())
                        .load(seen.getAvatarUrl())
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(iv);
            }
        }
    }
}
