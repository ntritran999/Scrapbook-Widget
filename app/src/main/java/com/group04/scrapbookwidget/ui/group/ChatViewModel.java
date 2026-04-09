package com.group04.scrapbookwidget.ui.group;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.TextMessage;
import com.google.gson.Gson;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.time.Instant;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.google.android.gms.tasks.Tasks;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";
    private static final String MEMORY_TAG = "MemoryDebug";
    private static final long SSE_RETRY_INITIAL_MS = 1_000L;
    private static final long SSE_RETRY_MAX_MS = 30_000L;
    private static final String SSE_CONTENT_TYPE = "text/event-stream";
    private final GroupService groupService;
    private final FirebaseAuth auth;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SmartReplyGenerator smartReply = SmartReply.getClient();

    private final MutableLiveData<List<Message>> _messages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Message>> getMessages() { return _messages; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<List<TodayMemory>> _todayMemories = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<TodayMemory>> getTodayMemories() { return _todayMemories; }

    private final MutableLiveData<List<String>> suggestedReplies = new MutableLiveData<>(Collections.emptyList());
    public LiveData<List<String>> getSuggestedReplies() { return suggestedReplies; }

    private String currentGroupId;
    private volatile Thread streamThread;
    private volatile boolean isStreaming = false;
    private volatile Call<ResponseBody> activeStreamCall;

    private static class FatalSseException extends Exception {
        FatalSseException(String message) {
            super(message);
        }
    }

    @Inject
    public ChatViewModel(GroupService groupService, FirebaseAuth auth) {
        this.groupService = groupService;
        this.auth = auth;
    }

    public void initChat(String groupId) {
        this.currentGroupId = groupId;
        Log.d(TAG, "initChat: groupId=" + groupId);
        
        loadMessages(); 
        startMessageStream();
        loadTodayMemories();
    }

    private void loadMessages() {
        if (currentGroupId == null) return;
        
        _isLoading.setValue(true);
        groupService.getMessages(currentGroupId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    mergeMessages(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                _isLoading.setValue(false);
                Log.e(TAG, "loadMessages: Failed", t);
            }
        });
    }

    private void loadTodayMemories() {
        if (currentGroupId == null) return;

        groupService.getTodayMemory(currentGroupId).enqueue(new Callback<List<TodayMemory>>() {
            @Override
            public void onResponse(Call<List<TodayMemory>> call, Response<List<TodayMemory>> response) {
                if (response.isSuccessful()) {
                    _todayMemories.setValue(response.body() != null ? response.body() : new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<TodayMemory>> call, Throwable t) {
                _todayMemories.postValue(new ArrayList<>());
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
        tempMessage.setCreatedAt(Instant.now().toString()); // Cần thời gian để sort
        tempMessage.setStatus(Message.Status.SENDING);
        tempMessage.setSenderName(auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Me");
        
        List<Message> currentList = new ArrayList<>(_messages.getValue());
        currentList.add(tempMessage);
        _messages.setValue(currentList);

        performSendMessage(tempId, content);
    }

    public void resendMessage(Message message) {
        if (message.getId() == null) return;
        message.setStatus(Message.Status.SENDING);
        updateMessageStatusInList(message);
        performSendMessage(message.getId(), message.getContent());
    }

    private void performSendMessage(String tempId, String content) {
        Map<String, String> body = new HashMap<>();
        body.put("content", content);
        body.put("createdBy", auth.getUid());
        body.put("type", "text");

        groupService.sendMessage(currentGroupId, body).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    replaceTempWithMessage(tempId, response.body());
                } else {
                    failMessageInList(tempId);
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                failMessageInList(tempId);
            }
        });
    }

    private void failMessageInList(String id) {
        mainHandler.post(() -> {
            List<Message> currentList = new ArrayList<>(_messages.getValue());
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(id)) {
                    currentList.get(i).setStatus(Message.Status.FAILED);
                    _messages.setValue(currentList);
                    break;
                }
            }
        });
    }

    private void updateMessageStatusInList(Message message) {
        mainHandler.post(() -> {
            List<Message> currentList = new ArrayList<>(_messages.getValue());
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(message.getId())) {
                    currentList.set(i, message);
                    _messages.setValue(currentList);
                    break;
                }
            }
        });
    }

    private void replaceTempWithMessage(String tempId, Message realMessage) {
        mainHandler.post(() -> {
            List<Message> currentList = new ArrayList<>(_messages.getValue());
            
            int realIdx = -1;
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(realMessage.getId())) {
                    realIdx = i;
                    break;
                }
            }

            int tempIdx = -1;
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(tempId)) {
                    tempIdx = i;
                    break;
                }
            }

            if (realIdx != -1) {
                if (tempIdx != -1) {
                    currentList.remove(tempIdx);
                }
            } else if (tempIdx != -1) {
                currentList.set(tempIdx, realMessage);
            } else {
                currentList.add(realMessage);
            }
            
            sortAndSetMessages(currentList);
        });
    }

    public void markMessageAsSeen(String messageId) {
        if (currentGroupId == null || auth.getUid() == null || messageId.startsWith("temp_")) return;

        groupService.markAsSeen(currentGroupId, messageId, auth.getUid()).enqueue(new Callback<Message.SeenBy>() {
            @Override
            public void onResponse(Call<Message.SeenBy> call, Response<Message.SeenBy> response) {
            }

            @Override
            public void onFailure(Call<Message.SeenBy> call, Throwable t) {
                Log.e(TAG, "Failed to mark as seen", t);
            }
        });
    }

    public void generateReplies(List<Message> recentMessages, String currentUserId) {
        if (recentMessages == null || recentMessages.isEmpty() || currentUserId == null) return;

        // Đảm bảo chronological order cho ML Kit
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
                        suggestedReplies.setValue(replies);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "SmartReply failed", e));
    }

    public void clearSuggestedReplies() {
        suggestedReplies.setValue(Collections.emptyList());
    }

    private void startMessageStream() {
        if (isStreaming) stopMessageStream();

        if (currentGroupId == null || auth.getCurrentUser() == null) {
            return;
        }

        isStreaming = true;
        streamThread = new Thread(this::runSseLoop, "group-message-sse");
        streamThread.start();
    }

    private void runSseLoop() {
        long retryMs = SSE_RETRY_INITIAL_MS;

        while (isStreaming && !Thread.currentThread().isInterrupted()) {
            try {
                String bearerToken = getBearerToken();
                connectAndConsumeStream(bearerToken);
                retryMs = SSE_RETRY_INITIAL_MS;
            } catch (FatalSseException fatal) {
                Log.e(TAG, "SSE fatal error: " + fatal.getMessage());
                postError(fatal.getMessage());
                stopMessageStream();
                return;
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (!isStreaming) {
                    return;
                }

                Log.w(TAG, "SSE disconnected, retry in " + retryMs + "ms", e);
                postError("SSE reconnecting...");

                try {
                    Thread.sleep(retryMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }

                retryMs = Math.min(retryMs * 2, SSE_RETRY_MAX_MS);
            }
        }
    }

    private String getBearerToken() throws Exception {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new FatalSseException("Unauthorized: user not logged in");
        }

        String idToken = Tasks.await(user.getIdToken(true)).getToken();
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new FatalSseException("Unauthorized: failed to obtain token");
        }

        return "Bearer " + idToken;
    }

    private void connectAndConsumeStream(String bearerToken) throws Exception {
        Call<ResponseBody> call = groupService.streamMessages(currentGroupId, bearerToken);
        activeStreamCall = call;
        try {
            Response<ResponseBody> response = call.execute();

            int statusCode = response.code();
            if (statusCode == 401 || statusCode == 403) {
                throw new FatalSseException("Unauthorized or forbidden (" + statusCode + ")");
            }
            if (statusCode == 404) {
                throw new FatalSseException("Group not found (404)");
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new Exception("SSE HTTP error: " + statusCode);
            }

            String contentType = response.headers().get("content-type");
            if (contentType == null || !contentType.toLowerCase(Locale.US).contains(SSE_CONTENT_TYPE)) {
                throw new Exception("Unexpected SSE content-type: " + contentType);
            }

            postError(null);

            try (ResponseBody body = response.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()))) {

                String line;
                String eventType = "";
                StringBuilder dataBuilder = new StringBuilder();

                while (isStreaming && (line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        dispatchSseFrame(eventType, dataBuilder);
                        eventType = "";
                        dataBuilder.setLength(0);
                        continue;
                    }

                    if (line.startsWith(":")) {
                        continue;
                    }

                    if (line.startsWith("event:")) {
                        eventType = line.substring(6).trim();
                        continue;
                    }

                    if (line.startsWith("data:")) {
                        String chunk = line.substring(5);
                        if (chunk.startsWith(" ")) {
                            chunk = chunk.substring(1);
                        }
                        if (dataBuilder.length() > 0) {
                            dataBuilder.append('\n');
                        }
                        dataBuilder.append(chunk);
                    }
                }

                if (dataBuilder.length() > 0 || !eventType.isEmpty()) {
                    dispatchSseFrame(eventType, dataBuilder);
                }
            }

            if (isStreaming) {
                throw new Exception("SSE closed by server");
            }
        } finally {
            if (activeStreamCall == call) {
                activeStreamCall = null;
            }
        }
    }

    private void dispatchSseFrame(String eventType, StringBuilder dataBuilder) {
        if (dataBuilder == null || dataBuilder.length() == 0) {
            return;
        }
        handleSseEvent(eventType, dataBuilder.toString());
    }

    private void postError(String errorMessage) {
        mainHandler.post(() -> _error.setValue(errorMessage));
    }

    private void handleSseEvent(String eventType, String data) {
        if (eventType == null || eventType.trim().isEmpty() || data == null || data.trim().isEmpty()) {
            return;
        }

        try {
            switch (eventType) {
                case "stream.ready":
                    Log.d(TAG, "SSE stream is ready: " + data);
                    break;
                case "messages.initial":
                    List<Message> initialMessages = gson.fromJson(data, new com.google.gson.reflect.TypeToken<List<Message>>(){}.getType());
                    mergeMessages(initialMessages);
                    break;
                case "message.created":
                case "message.seen":
                    Message newMessage = gson.fromJson(data, Message.class);
                    updateOrAddMessage(newMessage);
                    break;
                default:
                    Log.d(TAG, "Ignoring unsupported SSE event: " + eventType);
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse SSE event: " + eventType, e);
        }
    }

    private void mergeMessages(List<Message> newMessages) {
        List<Message> incoming = newMessages != null ? newMessages : Collections.emptyList();
        mainHandler.post(() -> {
            List<Message> current = _messages.getValue();
            if (current == null) current = new ArrayList<>();

            Map<String, Message> mergedMap = new HashMap<>();
            for (Message m : current) {
                if (m.getId() != null) mergedMap.put(m.getId(), m);
            }
            for (Message m : incoming) {
                if (m.getId() != null) mergedMap.put(m.getId(), m);
            }
            
            sortAndSetMessages(new ArrayList<>(mergedMap.values()));
        });
    }

    private void updateOrAddMessage(Message message) {
        if (message == null || message.getId() == null || message.getId().trim().isEmpty()) {
            return;
        }

        mainHandler.post(() -> {
            List<Message> existingMessages = _messages.getValue();
            if (existingMessages == null) {
                existingMessages = new ArrayList<>();
            }
            List<Message> currentList = new ArrayList<>(existingMessages);
            
            int index = -1;
            for (int i = 0; i < currentList.size(); i++) {
                Message currentMessage = currentList.get(i);
                if (currentMessage != null && message.getId().equals(currentMessage.getId())) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                currentList.set(index, message);
            } else {
                boolean replaced = false;
                if (message.getCreatedBy() != null && message.getCreatedBy().equals(auth.getUid())) {
                    for (int i = currentList.size() - 1; i >= 0; i--) {
                        Message m = currentList.get(i);
                        if (m != null &&
                                m.getId() != null &&
                                m.getId().startsWith("temp_") &&
                                m.getContent() != null &&
                                m.getContent().equals(message.getContent())) {
                            currentList.set(i, message);
                            replaced = true;
                            break;
                        }
                    }
                }
                
                if (!replaced) {
                    currentList.add(message);
                }
            }
            sortAndSetMessages(currentList);
        });
    }

    private void sortAndSetMessages(List<Message> list) {
        Collections.sort(list, Comparator.comparingLong(this::parseTimestamp));
        _messages.postValue(list);
    }

    private void retryStream() {
        // This method is intentionally left as a no-op.
        // Reconnect handling is now managed in runSseLoop() with exponential backoff.
    }

    private void stopMessageStream() {
        isStreaming = false;

        Call<ResponseBody> call = activeStreamCall;
        if (call != null) {
            call.cancel();
            activeStreamCall = null;
        }

        if (streamThread != null) {
            streamThread.interrupt();
            streamThread = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopMessageStream();
        smartReply.close();
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
