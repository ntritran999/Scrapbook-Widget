package com.group04.scrapbookwidget.ui.group;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import dagger.hilt.android.qualifiers.ApplicationContext;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.TextMessage;
import com.group04.scrapbookwidget.data.cache.AppCacheStore;
import com.group04.scrapbookwidget.data.cache.NetworkStatusProvider;
import com.group04.scrapbookwidget.data.realtime.GroupRealtimeSocketClient;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";
    private final GroupService groupService;
    private final FirebaseAuth auth;
    private final Context appContext;
    private final Gson gson = new Gson();
    private final SmartReplyGenerator smartReply = SmartReply.getClient();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final GroupRealtimeSocketClient realtimeSocketClient;
    private final AppCacheStore cacheStore;
    private final NetworkStatusProvider networkStatusProvider;
    private final Observer<GroupRealtimeSocketClient.SocketPacket> socketPacketObserver;
    private final String socketSubscriberId = "chat_vm_" + System.identityHashCode(this);

    private final MutableLiveData<List<Message>> _messages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Message>> getMessages() { return _messages; }

    private final MutableLiveData<List<MessageWrapper>> _messageWrappers = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MessageWrapper>> getMessageWrappers() { return _messageWrappers; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<List<TodayMemory>> _todayMemories = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<TodayMemory>> getTodayMemories() { return _todayMemories; }

    private final MutableLiveData<List<String>> suggestedReplies = new MutableLiveData<>(Collections.emptyList());
    public LiveData<List<String>> getSuggestedReplies() { return suggestedReplies; }

    private final MutableLiveData<Message.SeenBy> _markAsSeenResponse = new MutableLiveData<>();
    public LiveData<Message.SeenBy> getMarkAsSeenResponse() { return _markAsSeenResponse; }

    private String currentGroupId;

    private final List<Message> masterMessagesList = new ArrayList<>();

    @Inject
    public ChatViewModel(GroupService groupService, FirebaseAuth auth, @ApplicationContext Context appContext,
                         GroupRealtimeSocketClient realtimeSocketClient, AppCacheStore cacheStore,
                         NetworkStatusProvider networkStatusProvider) {
        this.groupService = groupService;
        this.auth = auth;
        this.appContext = appContext;
        this.realtimeSocketClient = realtimeSocketClient;
        this.cacheStore = cacheStore;
        this.networkStatusProvider = networkStatusProvider;
        this.socketPacketObserver = this::onSocketPacket;
        this.realtimeSocketClient.getSocketPacketsLiveData().observeForever(socketPacketObserver);
    }

    public void initChat(String groupId) {
        this.currentGroupId = groupId;
        Log.d(TAG, "initChat: groupId=" + groupId);

        loadMessages();
        realtimeSocketClient.subscribe(socketSubscriberId, groupId);
        loadTodayMemories();
    }

    private void loadMessages() {
        if (currentGroupId == null) return;
        String targetGroupId = currentGroupId;

        List<Message> cachedMessages = cacheStore.getMessages(targetGroupId);
        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            mergeMessages(cachedMessages);
        }

        _isLoading.setValue(true);
        groupService.getMessages(targetGroupId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(@NonNull Call<List<Message>> call, @NonNull Response<List<Message>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    cacheStore.saveMessages(targetGroupId, response.body());
                    mergeMessages(response.body());
                } else if (cachedMessages == null || cachedMessages.isEmpty()) {
                    _error.postValue("Could not load messages.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Message>> call, @NonNull Throwable t) {
                _isLoading.setValue(false);
                Log.e(TAG, "loadMessages: Failed", t);
                if (cachedMessages == null || cachedMessages.isEmpty()) {
                    _error.postValue(networkStatusProvider.isOnline()
                            ? "Could not load messages."
                            : "Offline mode: no cached messages yet.");
                }
            }
        });
    }

    private void loadTodayMemories() {
        if (currentGroupId == null) return;
        String targetGroupId = currentGroupId;

        List<TodayMemory> cachedMemories = cacheStore.getTodayMemories(targetGroupId);
        if (cachedMemories != null) {
            _todayMemories.setValue(cachedMemories);
        }

        groupService.getTodayMemory(targetGroupId).enqueue(new Callback<List<TodayMemory>>() {
            @Override
            public void onResponse(@NonNull Call<List<TodayMemory>> call, @NonNull Response<List<TodayMemory>> response) {
                if (response.isSuccessful()) {
                    List<TodayMemory> memories = response.body() != null ? response.body() : new ArrayList<>();
                    cacheStore.saveTodayMemories(targetGroupId, memories);
                    _todayMemories.setValue(memories);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TodayMemory>> call, @NonNull Throwable t) {
                if (cachedMemories == null) {
                    _todayMemories.postValue(new ArrayList<>());
                }
            }
        });
    }

    public void sendMessage(String content) {
        if (currentGroupId == null || content.trim().isEmpty()) return;

        Message tempMessage = new Message();
        String tempId = "temp_" + UUID.randomUUID().toString();
        tempMessage.setId(tempId);
        tempMessage.setContent(content);
        tempMessage.setCreatedBy(auth.getUid());
        tempMessage.setCreatedAt(Instant.now().toString());
        tempMessage.setStatus(Message.Status.SENDING);
        tempMessage.setSenderName(auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Me");
        
        backgroundExecutor.execute(() -> {
            masterMessagesList.add(tempMessage);
            processAndPostMessages();
        });

        performSendMessage(tempId, content);
    }

    public void resendMessage(Message message) {
        if (message.getId() == null) return;
        message.setStatus(Message.Status.SENDING);
        updateMessageInList(message);
        performSendMessage(message.getId(), message.getContent());
    }

    private void performSendMessage(String tempId, String content) {
        Map<String, String> body = new HashMap<>();
        body.put("content", content);
        body.put("createdBy", auth.getUid());
        body.put("type", "text");

        groupService.sendMessage(currentGroupId, body).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(@NonNull Call<Message> call, @NonNull Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    replaceTempWithMessage(tempId, response.body());
                } else {
                    failMessageInList(tempId);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Message> call, @NonNull Throwable t) {
                failMessageInList(tempId);
            }
        });
    }

    private void failMessageInList(String id) {
        if (backgroundExecutor.isShutdown()) return;
        backgroundExecutor.execute(() -> {
            for (int i = 0; i < masterMessagesList.size(); i++) {
                if (masterMessagesList.get(i).getId().equals(id)) {
                    masterMessagesList.get(i).setStatus(Message.Status.FAILED);
                    processAndPostMessages();
                    break;
                }
            }
        });
    }

    private void updateMessageInList(Message message) {
        if (backgroundExecutor.isShutdown()) return;
        backgroundExecutor.execute(() -> {
            for (int i = 0; i < masterMessagesList.size(); i++) {
                if (masterMessagesList.get(i).getId().equals(message.getId())) {
                    masterMessagesList.set(i, message);
                    processAndPostMessages();
                    break;
                }
            }
        });
    }

    private void replaceTempWithMessage(String tempId, Message realMessage) {
        if (backgroundExecutor.isShutdown()) return;
        backgroundExecutor.execute(() -> {
            int tempIdx = -1;
            for (int i = 0; i < masterMessagesList.size(); i++) {
                if (masterMessagesList.get(i).getId().equals(tempId)) {
                    tempIdx = i;
                    break;
                }
            }

            int realIdx = -1;
            for (int i = 0; i < masterMessagesList.size(); i++) {
                if (masterMessagesList.get(i).getId().equals(realMessage.getId())) {
                    realIdx = i;
                    break;
                }
            }

            if (tempIdx != -1) {
                if (realIdx != -1) {
                    masterMessagesList.remove(tempIdx);
                    masterMessagesList.set(realIdx > tempIdx ? realIdx - 1 : realIdx, realMessage);
                } else {
                    masterMessagesList.set(tempIdx, realMessage);
                }
            } else if (realIdx != -1) {
                masterMessagesList.set(realIdx, realMessage);
            } else {
                masterMessagesList.add(realMessage);
            }
            
            processAndPostMessages();
        });
    }

    public void markMessageAsSeen(String messageId) {
        if (currentGroupId == null || auth.getUid() == null || messageId.startsWith("temp_")) return;

        groupService.markAsSeen(currentGroupId, messageId, auth.getUid()).enqueue(new Callback<Message.SeenBy>() {
            @Override
            public void onResponse(@NonNull Call<Message.SeenBy> call, @NonNull Response<Message.SeenBy> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _markAsSeenResponse.postValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Message.SeenBy> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to mark as seen", t);
            }
        });
    }

    public void generateReplies(List<Message> recentMessages, String currentUserId) {
        if (recentMessages == null || recentMessages.isEmpty() || currentUserId == null) return;

        // Respect global AI features setting
        try {
            boolean aiEnabled = appContext.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
                    .getBoolean("AI_FEATURES_ENABLED", true);
            if (!aiEnabled) {
                suggestedReplies.setValue(Collections.emptyList());
                return;
            }
        } catch (Exception e) {
            // If unable to read pref, proceed with default behavior
        }

        backgroundExecutor.execute(() -> {
            List<Message> sortedMessages = new ArrayList<>(recentMessages);
            Collections.sort(sortedMessages, Comparator.comparingLong(this::parseTimestamp));

            int startIndex = Math.max(sortedMessages.size() - 10, 0);
            List<TextMessage> conversation = new ArrayList<>();
            for (int i = startIndex; i < sortedMessages.size(); i++) {
                Message message = sortedMessages.get(i);
                if (message == null || message.getContent() == null) continue;

                String senderId = message.getCreatedBy() != null ? message.getCreatedBy() : message.getSenderId();
                if (senderId == null) continue;

                long timestamp = parseTimestamp(message);
                if (currentUserId.equals(senderId)) {
                    conversation.add(TextMessage.createForLocalUser(message.getContent(), timestamp));
                } else {
                    conversation.add(TextMessage.createForRemoteUser(message.getContent(), timestamp, senderId));
                }
            }

            if (conversation.isEmpty()) return;

            smartReply.suggestReplies(conversation)
                    .addOnSuccessListener(result -> {
                        if (result != null && result.getStatus() == 0) {
                            List<String> replies = new ArrayList<>();
                            for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                                replies.add(suggestion.getText());
                            }
                            suggestedReplies.postValue(replies);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "SmartReply failed", e));
        });
    }

    public void clearSuggestedReplies() {
        suggestedReplies.setValue(Collections.emptyList());
    }

    private void handleWebSocketMessage(String text) {
        backgroundExecutor.execute(() -> {
            try {
                JsonObject packet = gson.fromJson(text, JsonObject.class);
                String event = packet.has("event") ? packet.get("event").getAsString() : "";

                if (event.equals("messages.initial")) {
                    List<Message> initialMessages = gson.fromJson(packet.get("data"),
                            new com.google.gson.reflect.TypeToken<List<Message>>(){}.getType());
                    mergeMessagesInternal(initialMessages);
                } else if (event.equals("message.created") || event.equals("message.seen")) {
                    Message message = gson.fromJson(packet.get("data"), Message.class);
                    updateOrAddMessageInternal(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing WebSocket message", e);
            }
        });
    }

    private void mergeMessages(List<Message> newMessages) {
        if (backgroundExecutor.isShutdown()) return;
        backgroundExecutor.execute(() -> mergeMessagesInternal(newMessages));
    }

    private void mergeMessagesInternal(List<Message> newMessages) {
        Map<String, Message> mergedMap = new HashMap<>();
        for (Message m : masterMessagesList) {
            if (m.getId() != null) mergedMap.put(m.getId(), m);
        }
        for (Message m : newMessages) {
            if (m.getId() != null) mergedMap.put(m.getId(), m);
        }

        masterMessagesList.clear();
        masterMessagesList.addAll(mergedMap.values());
        processAndPostMessages();
    }

    private void updateOrAddMessageInternal(Message message) {
        int index = -1;
        for (int i = 0; i < masterMessagesList.size(); i++) {
            if (masterMessagesList.get(i).getId().equals(message.getId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            masterMessagesList.set(index, message);
        } else {
            boolean replaced = false;
            String myUid = auth.getUid();
            if (message.getCreatedBy() != null && message.getCreatedBy().equals(myUid)) {
                for (int i = masterMessagesList.size() - 1; i >= 0; i--) {
                    Message m = masterMessagesList.get(i);
                    if (m != null && m.getId() != null && m.getId().startsWith("temp_") &&
                        m.getContent() != null && m.getContent().equals(message.getContent())) {
                        masterMessagesList.set(i, message);
                        replaced = true;
                        break;
                    }
                }
            }

            if (!replaced) {
                masterMessagesList.add(message);
            }
        }
        processAndPostMessages();
    }

    private void processAndPostMessages() {
        Collections.sort(masterMessagesList, Comparator.comparingLong(this::parseTimestamp));
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            cacheStore.saveMessages(currentGroupId, new ArrayList<>(masterMessagesList));
        }
        _messages.postValue(new ArrayList<>(masterMessagesList));

        String myUid = auth.getUid();
        Map<String, String> userToLatestMessageId = new HashMap<>();
        Map<String, Message.SeenBy> userToLatestDetails = new HashMap<>();

        for (Message m : masterMessagesList) {
            if (m.getSeenBy() != null) {
                for (Message.SeenBy sb : m.getSeenBy()) {
                    String uid = sb.getUserId() != null ? sb.getUserId() : sb.getId();
                    if (uid == null || uid.equals(myUid)) continue;
                    userToLatestMessageId.put(uid, m.getId());
                    userToLatestDetails.put(uid, sb);
                }
            }
        }

        Map<String, List<Message.SeenBy>> messageToUsersAt = new HashMap<>();
        for (Map.Entry<String, String> entry : userToLatestMessageId.entrySet()) {
            String mid = entry.getValue();
            Message.SeenBy details = userToLatestDetails.get(entry.getKey());
            if (details != null) {
                List<Message.SeenBy> list = messageToUsersAt.get(mid);
                if (list == null) {
                    list = new ArrayList<>();
                    messageToUsersAt.put(mid, list);
                }
                list.add(details);
            }
        }

        List<MessageWrapper> wrappers = new ArrayList<>(masterMessagesList.size());
        for (Message m : masterMessagesList) {
            List<Message.SeenBy> usersHere = messageToUsersAt.get(m.getId());
            wrappers.add(new MessageWrapper(m, usersHere != null ? usersHere : new ArrayList<>()));
        }

        _messageWrappers.postValue(wrappers);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeSocketClient.getSocketPacketsLiveData().removeObserver(socketPacketObserver);
        realtimeSocketClient.unsubscribe(socketSubscriberId);
        smartReply.close();
        backgroundExecutor.shutdown();
    }

    private void onSocketPacket(GroupRealtimeSocketClient.SocketPacket packet) {
        if (packet == null) {
            return;
        }
        if (currentGroupId == null || !currentGroupId.equals(packet.getGroupId())) {
            return;
        }
        String event = packet.getEventName();
        if (event == null || event.isEmpty()) {
            return;
        }
        if (!"messages.initial".equals(event) && !"message.created".equals(event) && !"message.seen".equals(event)) {
            return;
        }
        handleWebSocketMessage(packet.getRawMessage());
    }

    private long parseTimestamp(Message message) {
        if (message == null) return System.currentTimeMillis();
        String rawTimestamp = message.getCreatedAt();
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) {
            rawTimestamp = message.getTime();
        }
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) return System.currentTimeMillis();
        try {
            return Instant.parse(rawTimestamp).toEpochMilli();
        } catch (Exception ignored) {}
        try {
            return Long.parseLong(rawTimestamp);
        } catch (NumberFormatException ignored) {}
        return System.currentTimeMillis();
    }
}
