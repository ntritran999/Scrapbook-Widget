package com.group04.scrapbookwidget.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public final class NotificationChannels {
    public static final String SCRAPBOOK_UPDATES = "scrapbook_updates";

    private NotificationChannels() {
    }

    public static void create(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                SCRAPBOOK_UPDATES,
                "Scrapbook Updates",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Messages, photo updates, and reactions");

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
