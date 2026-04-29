package com.group04.scrapbookwidget.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.repository.IGroupRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;
import com.group04.scrapbookwidget.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AppFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";

    @Inject
    DeviceTokenRepository deviceTokenRepository;

    @Inject
    IGroupRepository groupRepository;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        deviceTokenRepository.enqueueRegister(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        String type = value(data, "type");
        String title = remoteMessage.getNotification() != null
                ? value(remoteMessage.getNotification().getTitle())
                : defaultTitle(type);
        String body = remoteMessage.getNotification() != null
                ? value(remoteMessage.getNotification().getBody())
                : defaultBody(type);
        String groupId = value(data, "groupId");
        String groupAvatarUrl = value(data, "groupAvatarUrl");

        if ((groupAvatarUrl == null || groupAvatarUrl.isEmpty()) && groupId != null && !groupId.isEmpty()) {
            // Fetch group avatar if missing in payload
            groupRepository.getGroupById(groupId, new RepositoryCallback<Group>() {
                @Override
                public void onSuccess(Group group) {
                    showNotification(type, title, body, data, group.getAvatarUrl());
                }

                @Override
                public void onError(Exception e) {
                    showNotification(type, title, body, data, null);
                }
            });
        } else {
            showNotification(type, title, body, data, groupAvatarUrl);
        }
    }

    private void showNotification(String type, String title, String body, Map<String, String> data, String groupAvatarUrl) {
        Intent intent = buildTargetIntent(this, type, data);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationChannels.SCRAPBOOK_UPDATES)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (groupAvatarUrl != null && !groupAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(groupAvatarUrl)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap largeIcon = overlayBadge(resource);
                            builder.setLargeIcon(largeIcon);
                            notifyInternal(builder);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            notifyInternal(builder);
                        }
                    });
        } else {
            notifyInternal(builder);
        }
    }

    private void notifyInternal(NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    private Bitmap overlayBadge(Bitmap avatar) {
        int width = avatar.getWidth();
        int height = avatar.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // Draw Avatar
        canvas.drawBitmap(avatar, 0, 0, null);

        // Draw Badge (Scrapbook Icon)
        int badgeSize = width / 3;
        Drawable badgeDrawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher_round);
        if (badgeDrawable != null) {
            badgeDrawable.setBounds(width - badgeSize, height - badgeSize, width, height);
            badgeDrawable.draw(canvas);
        }

        return result;
    }

    private static Intent buildTargetIntent(Context context, String type, Map<String, String> data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra("notification_type", type);
        intent.putExtra("groupId", value(data, "groupId"));
        intent.putExtra("pageId", value(data, "pageId"));
        intent.putExtra("itemId", value(data, "itemId"));
        intent.putExtra("messageId", value(data, "messageId"));
        intent.putExtra("senderId", value(data, "senderId"));
        intent.putExtra("reactorId", value(data, "reactorId"));
        intent.putExtra("reactionType", value(data, "reactionType"));

        return intent;
    }

    private static String defaultTitle(String type) {
        switch (type) {
            case "message_created":
                return "New message";
            case "photo_created":
                return "New photo";
            case "photo_reacted":
                return "Photo reaction";
            default:
                return "Scrapbook update";
        }
    }

    private static String defaultBody(String type) {
        switch (type) {
            case "message_created":
                return "You received a new message";
            case "photo_created":
                return "A new photo was added";
            case "photo_reacted":
                return "Someone reacted to a photo";
            default:
                return "Open the app to view details";
        }
    }

    private static String value(Map<String, String> data, String key) {
        return data != null && data.containsKey(key) ? value(data.get(key)) : "";
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
