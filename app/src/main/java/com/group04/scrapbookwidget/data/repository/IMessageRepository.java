package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.Message;

import java.util.List;
import java.util.Map;

public interface IMessageRepository {
    interface MessageListCallback {
        void onMessages(List<Message> messages);
        void onError(Exception e);
    }
    void getMessage(String groupId, String messageId, RepositoryCallback<Message> callback);
    void getMessages(String groupId, RepositoryCallback<List<Message>> callback);
    void sendMessage(String groupId, Message message, RepositoryCallback<String> callback);
    void deleteMessage(String groupId, String messageId, RepositoryCallback<Void> callback);
    void observeMessages(String groupId, int limit, MessageListCallback callback);

    void markAsSeen(String groupId, String messageId, String userId, RepositoryCallback<Void> callback);
    void getSeenBy(String groupId, String messageId, RepositoryCallback<Map<String, Long>> callback);
    void hasUserSeen(String groupId, String messageId, String userId, RepositoryCallback<Boolean> callback);
}
