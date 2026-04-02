package com.group04.scrapbookwidget.ui.group;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";
    private static final String MEMORY_TAG = "MemoryDebug";
    private final GroupService groupService;
    private final FirebaseAuth auth;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<Message>> _messages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Message>> getMessages() { return _messages; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<List<TodayMemory>> _todayMemories = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<TodayMemory>> getTodayMemories() { return _todayMemories; }

    private String currentGroupId;
    private Thread streamThread;
    private boolean isStreaming = false;

    @Inject
    public ChatViewModel(GroupService groupService, FirebaseAuth auth) {
        this.groupService = groupService;
        this.auth = auth;
    }

    public void initChat(String groupId) {
        this.currentGroupId = groupId;
        Log.d(MEMORY_TAG, "[INIT_CHAT] groupId=" + groupId + ", authUid=" + auth.getUid());
        startMessageStream();
        loadTodayMemories();
    }

    private void loadTodayMemories() {
        Log.d(MEMORY_TAG, "[LOAD_START] groupId=" + currentGroupId + ", authUid=" + auth.getUid());
        if (currentGroupId == null) {
            Log.w(MEMORY_TAG, "[LOAD_ABORT] currentGroupId is null");
            _todayMemories.setValue(new ArrayList<>());
            return;
        }

        Call<List<TodayMemory>> call = groupService.getTodayMemory(currentGroupId);
        Log.d(MEMORY_TAG, "[API_REQUEST] GET " + call.request().method() + " " + call.request().url());
        call.enqueue(new Callback<List<TodayMemory>>() {
            @Override
            public void onResponse(Call<List<TodayMemory>> call, Response<List<TodayMemory>> response) {
                if (response.isSuccessful()) {
                    List<TodayMemory> memories = response.body();
                    int count = memories != null ? memories.size() : 0;
                    Log.d(MEMORY_TAG, "[API_SUCCESS] code=" + response.code() + ", memoryCount=" + count);
                    if (count == 0) {
                        Log.w(MEMORY_TAG,
                                "[API_EMPTY] Backend returned no memories. Checklist: " +
                                        "1) current user must be a member of groups/" + currentGroupId + "/members/{uid}; " +
                                        "2) scrapbook items must belong to this group; " +
                                        "3) item.type must equal 'photo'; " +
                                        "4) taggedUserIds must be non-empty; " +
                                        "5) joined users for taggedUserIds must still exist and not be deleted."
                        );
                    } else {
                        for (int i = 0; i < memories.size(); i++) {
                            TodayMemory memory = memories.get(i);
                            Log.d(MEMORY_TAG,
                                    "[API_ITEM_" + i + "] photoUrl=" + safe(memory != null ? memory.getPhotoUrl() : null) +
                                            ", taggedUsernames=" + (memory != null ? memory.getTaggedUsernames() : null));
                        }
                    }
                    _todayMemories.setValue(memories != null ? memories : new ArrayList<>());
                } else {
                    String errorBody = null;
                    try {
                        errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                    } catch (Exception e) {
                        errorBody = "[unable to read error body: " + e.getMessage() + "]";
                    }
                    Log.e(MEMORY_TAG,
                            "[API_FAILED] code=" + response.code() +
                                    ", message=" + response.message() +
                                    ", errorBody=" + errorBody);
                    if (response.code() == 403) {
                        Log.w(MEMORY_TAG, "[API_FAILED_HINT] User is likely not a member of this group on backend yet.");
                    } else if (response.code() == 404) {
                        Log.w(MEMORY_TAG, "[API_FAILED_HINT] groupId may not exist on backend: " + currentGroupId);
                    }
                    _todayMemories.setValue(new ArrayList<>());
                    Log.w(TAG, "Failed to load today memories: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TodayMemory>> call, Throwable t) {
                Log.e(MEMORY_TAG,
                        "[API_ERROR] url=" + call.request().url() +
                                ", exception=" + t.getClass().getSimpleName() +
                                ", message=" + t.getMessage(), t);
                _todayMemories.postValue(new ArrayList<>());
                Log.w(TAG, "Failed to load today memories", t);
            }
        });
    }

    public void sendMessage(String content) {
        if (currentGroupId == null || content.trim().isEmpty()) return;

        // Create a temporary message for optimistic UI update
        Message tempMessage = new Message();
        String tempId = "temp_" + UUID.randomUUID().toString();
        tempMessage.setId(tempId);
        tempMessage.setContent(content);
        tempMessage.setCreatedBy(auth.getUid());
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
            
            // Check if the real message is already in the list (e.g. from SSE)
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
                // Real message already exists (from SSE), just remove the temp one
                if (tempIdx != -1) {
                    currentList.remove(tempIdx);
                }
            } else if (tempIdx != -1) {
                // Replace temp with real
                currentList.set(tempIdx, realMessage);
            } else {
                // Neither found, just add real (shouldn't happen)
                currentList.add(realMessage);
            }
            
            _messages.setValue(currentList);
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

    private void startMessageStream() {
        if (isStreaming) stopMessageStream();
        isStreaming = true;

        auth.getCurrentUser().getIdToken(false).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            streamThread = new Thread(() -> {
                try {
                    Response<ResponseBody> response = groupService.streamMessages(currentGroupId, token).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                        String line;
                        String eventType = "";
                        while (isStreaming && (line = reader.readLine()) != null) {
                            if (line.startsWith("event:")) {
                                eventType = line.substring(6).trim();
                            } else if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                handleSseEvent(eventType, data);
                            }
                        }
                    } else {
                        Log.e(TAG, "SSE connection failed: " + response.code());
                        retryStream();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "SSE stream error", e);
                    retryStream();
                }
            });
            streamThread.start();
        });
    }

    private void handleSseEvent(String eventType, String data) {
        switch (eventType) {
            case "messages.initial":
                List<Message> initialMessages = gson.fromJson(data, new com.google.gson.reflect.TypeToken<List<Message>>(){}.getType());
                _messages.postValue(initialMessages);
                break;
            case "message.created":
                Message newMessage = gson.fromJson(data, Message.class);
                updateOrAddMessage(newMessage);
                break;
            case "message.seen":
                Message updatedMessage = gson.fromJson(data, Message.class);
                updateOrAddMessage(updatedMessage);
                break;
        }
    }

    private void updateOrAddMessage(Message message) {
        mainHandler.post(() -> {
            List<Message> currentList = new ArrayList<>(_messages.getValue());
            
            int index = -1;
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(message.getId())) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Already exists, just update it
                currentList.set(index, message);
            } else {
                // Check if this matches a temp message we sent
                boolean replaced = false;
                if (message.getCreatedBy() != null && message.getCreatedBy().equals(auth.getUid())) {
                    // Search backwards for the most recent temp message with matching content
                    for (int i = currentList.size() - 1; i >= 0; i--) {
                        Message m = currentList.get(i);
                        if (m.getId() != null && m.getId().startsWith("temp_") && 
                            m.getContent() != null && m.getContent().equals(message.getContent())) {
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
            _messages.setValue(currentList);
        });
    }

    private void retryStream() {
        if (isStreaming) {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            startMessageStream();
        }
    }

    private void stopMessageStream() {
        isStreaming = false;
        if (streamThread != null) {
            streamThread.interrupt();
            streamThread = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopMessageStream();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "<empty>" : value;
    }
}
