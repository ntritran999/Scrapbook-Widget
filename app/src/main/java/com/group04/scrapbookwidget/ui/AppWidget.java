package com.group04.scrapbookwidget.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.group04.scrapbookwidget.R;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    private final String PREF_NAME = "widget_metadata";
    private static final String USER_SESSION_PREF = "TMP_USER_SESSION";
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, int[] appWidgetIds, SharedPreferences preferences) {
        SharedPreferences userSessionPref = context.getSharedPreferences(USER_SESSION_PREF, Activity.MODE_PRIVATE);
        int layout = R.layout.app_widget_empty;
        String status = preferences.getString("STATUS", "");
        String photoLatest = preferences.getString("PHOTO_URL", "");
        String avatar = preferences.getString("AVATAR", "");
        Intent intent = new Intent(context, MainActivity.class);

        String groupId = null, pageId = null;
        if (!userSessionPref.getString("USER_ID", "").isEmpty()) {
            groupId = preferences.getString("GROUP_ID", null);
            pageId = preferences.getString("PAGE_ID", null);
        }

        Boolean isWidgetEmpty = true;
        if (groupId != null && pageId != null) {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);
            args.putString("PAGE_ID", pageId);
            args.putString("PASTED_IMAGE_PATH", "");
            args.putString("CAPTION", "");
            intent.putExtras(args);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (!photoLatest.isEmpty()) {
                isWidgetEmpty = false;
            }
        }
        if (!isWidgetEmpty) {
            layout = R.layout.app_widget;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        if (!isWidgetEmpty) {
            views.setTextViewText(R.id.widget_sender_status, status);

            AppWidgetTarget backgroundImage = new AppWidgetTarget(context, R.id.widget_background_image, views, appWidgetIds);
            AppWidgetTarget avatarImage = new AppWidgetTarget(context, R.id.widget_sender_avatar_image, views, appWidgetIds);

            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(photoLatest)
                    .centerCrop()
                    .into(backgroundImage);

            if (avatar.isEmpty()) {
                views.setImageViewResource(R.id.widget_sender_avatar_image, R.drawable.account_circle_24);
            }
            else {
                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(avatar)
                        .circleCrop()
                        .into(avatarImage);
            }
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences widgetPrefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, appWidgetIds, widgetPrefs);
        }
    }

    public static void updateWidgetNow(Context ctx) {
        int[]  ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(new ComponentName(ctx, AppWidget.class));
        Intent intent = new Intent(ctx, AppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        ctx.sendBroadcast(intent);
    }
}