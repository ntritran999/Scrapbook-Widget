package com.group04.scrapbookwidget.data.realtime;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class GroupRealtimeSocketClient {
    public static class SocketPacket {
        private final String groupId;
        private final String eventName;
        private final JsonElement data;
        private final String rawMessage;

        public SocketPacket(String groupId, String eventName, JsonElement data, String rawMessage) {
            this.groupId = groupId;
            this.eventName = eventName;
            this.data = data;
            this.rawMessage = rawMessage;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getEventName() {
            return eventName;
        }

        public JsonElement getData() {
            return data;
        }

        public String getRawMessage() {
            return rawMessage;
        }
    }

    private static final String TAG = "GroupRealtimeSocket";
    private static final long INITIAL_RETRY_DELAY_MS = 1000L;
    private static final long MAX_RETRY_DELAY_MS = 30000L;

    private final OkHttpClient okHttpClient;
    private final FirebaseAuth auth;
    private final String baseUrl;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<SocketPacket> socketPacketsLiveData = new MutableLiveData<>();

    private WebSocket webSocket;
    private String connectedGroupId;
    private boolean isStopped = true;
    private long retryDelayMs = INITIAL_RETRY_DELAY_MS;
    private final Map<String, String> subscribers = new HashMap<>();

    public GroupRealtimeSocketClient(
            @NonNull OkHttpClient okHttpClient,
            @NonNull FirebaseAuth auth,
            @NonNull String baseUrl
    ) {
        this.okHttpClient = okHttpClient;
        this.auth = auth;
        this.baseUrl = baseUrl;
    }

    public LiveData<SocketPacket> getSocketPacketsLiveData() {
        return socketPacketsLiveData;
    }

    public synchronized void subscribe(@NonNull String subscriberId, @NonNull String groupId) {
        if (subscriberId.trim().isEmpty()) {
            return;
        }
        if (groupId.trim().isEmpty()) {
            return;
        }
        subscribers.put(subscriberId, groupId);
        isStopped = false;
        switchConnectionIfNeeded(groupId);
    }

    public synchronized void unsubscribe(@NonNull String subscriberId) {
        subscribers.remove(subscriberId);
        if (subscribers.isEmpty()) {
            stopInternal("No subscribers");
            return;
        }

        if (connectedGroupId == null) {
            return;
        }

        if (!hasSubscriberForGroup(connectedGroupId)) {
            String nextGroupId = subscribers.values().iterator().next();
            reconnectToGroup(nextGroupId);
        }
    }

    public synchronized void stopAll() {
        subscribers.clear();
        stopInternal("Stopped all");
    }

    private synchronized void connect() {
        if (isStopped || connectedGroupId == null || connectedGroupId.trim().isEmpty()) {
            return;
        }

        if (webSocket != null) {
            webSocket.close(1000, "Reconnecting");
            webSocket = null;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "current user is null, skip connect");
            scheduleReconnect();
            return;
        }

        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "token is empty, skip connect");
                scheduleReconnect();
                return;
            }

            String wsBaseUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
            String wsUrl = wsBaseUrl + "groups/" + connectedGroupId + "/messages/ws?token=" + token;

            Request request = new Request.Builder().url(wsUrl).build();
            webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                    retryDelayMs = INITIAL_RETRY_DELAY_MS;
                    Log.d(TAG, "connected to " + wsUrl);
                }

                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                    emitPacket(text);
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                    Log.e(TAG, "socket error: " + t.getMessage());
                    scheduleReconnect();
                }

                @Override
                public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    Log.d(TAG, "socket closed, reason=" + reason);
                    scheduleReconnect();
                }
            });
        }).addOnFailureListener(error -> {
            Log.e(TAG, "failed to fetch token", error);
            scheduleReconnect();
        });
    }

    private synchronized void scheduleReconnect() {
        if (isStopped) {
            return;
        }
        long currentDelay = retryDelayMs;
        mainHandler.postDelayed(this::connect, currentDelay);
        retryDelayMs = Math.min(retryDelayMs * 2L, MAX_RETRY_DELAY_MS);
    }

    private void emitPacket(@NonNull String rawMessage) {
        String eventName = "";
        JsonElement data = null;

        try {
            JsonObject packet = gson.fromJson(rawMessage, JsonObject.class);
            if (packet != null) {
                eventName = packet.has("event") ? packet.get("event").getAsString() : "";
                data = packet.get("data");
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to parse realtime packet", e);
        }

        socketPacketsLiveData.postValue(new SocketPacket(connectedGroupId, eventName, data, rawMessage));
    }

    private void switchConnectionIfNeeded(@NonNull String requestedGroupId) {
        if (connectedGroupId == null) {
            reconnectToGroup(requestedGroupId);
            return;
        }
        if (connectedGroupId.equals(requestedGroupId)) {
            return;
        }
        reconnectToGroup(requestedGroupId);
    }

    private void reconnectToGroup(@NonNull String targetGroupId) {
        connectedGroupId = targetGroupId;
        retryDelayMs = INITIAL_RETRY_DELAY_MS;
        connect();
    }

    private boolean hasSubscriberForGroup(@NonNull String groupId) {
        for (String value : subscribers.values()) {
            if (groupId.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void stopInternal(@NonNull String reason) {
        isStopped = true;
        mainHandler.removeCallbacksAndMessages(null);
        connectedGroupId = null;
        if (webSocket != null) {
            webSocket.close(1000, reason);
            webSocket = null;
        }
    }
}
