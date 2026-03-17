package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.group04.scrapbookwidget.data.model.Message;

import java.util.List;
import java.util.Map;

public interface IMessageRepository {
    interface MessageListCallback {
        void onMessages(List<Message> messages);
        void onError(Exception e);
    }
    Task<Message> getMessage(String groupId, String messageId);
    Task<List<Message>> getMessages(String groupId);
    Task<String> sendMessage(String groupId, Message message);
    Task<Void> deleteMessage(String groupId, String messageId);
    void observeMessages(String groupId, int limit, MessageListCallback callback);

    Task<Void> markAsSeen(String groupId, String messageId, String userId);
    Task<Map<String, Long>> getSeenBy(String groupId, String messageId);
    Task<Boolean> hasUserSeen(String groupId, String messageId, String userId);
}
