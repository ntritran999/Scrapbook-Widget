package com.group04.scrapbookwidget.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.group04.scrapbookwidget.R;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    private final String PREF_NAME = "widget_metadata";
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, SharedPreferences preferences) {
        Intent intent = new Intent(context, MainActivity.class);
        String groupId = preferences.getString("GROUP_ID", null);
        String pageId = preferences.getString("PAGE_ID", null);
        if (groupId != null && pageId != null) {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);
            args.putString("PAGE_ID", pageId);
            intent.putExtras(args);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences widgetPrefs = context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, widgetPrefs);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}