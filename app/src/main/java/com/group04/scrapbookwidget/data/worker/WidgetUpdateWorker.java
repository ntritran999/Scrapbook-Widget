package com.group04.scrapbookwidget.data.worker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.group04.scrapbookwidget.data.model.Widget;
import com.group04.scrapbookwidget.data.repository.IWidgetRepository;
import com.group04.scrapbookwidget.ui.AppWidget;

import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

@HiltWorker
public class WidgetUpdateWorker extends Worker {
    private final String SESSION_PREF_NAME = "TMP_USER_SESSION";
    private final String WIDGET_PREF_NAME =  "widget_metadata";
    private IWidgetRepository widgetRepository;
    @AssistedInject
    public WidgetUpdateWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters workerParams,
                              IWidgetRepository widgetRepository) {
        super(context, workerParams);
        this.widgetRepository = widgetRepository;
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SESSION_PREF_NAME, Activity.MODE_PRIVATE);
        try {
            List<Widget> widgets = widgetRepository.getWidgets(sharedPreferences.getString("USER_ID", ""));
            if (!widgets.isEmpty()) {
                SharedPreferences widgetSharedPreferences = getApplicationContext().getSharedPreferences(WIDGET_PREF_NAME, Activity.MODE_PRIVATE);
                Widget widget = widgets.get(0);
                SharedPreferences.Editor editor = widgetSharedPreferences.edit();
                editor.putString("GROUP_ID", widget.getGroupId());
                editor.putString("PAGE_ID", widget.getPageId());
                editor.putString("PHOTO_URL", widget.getLatestPhotoUrl());
                editor.putString("AVATAR", widget.getSenderAvatar());
                editor.putString("STATUS", widget.getStatus());
                editor.commit();

                AppWidget.updateWidgetNow(getApplicationContext());
            }

        } catch (Exception e) {
            return Result.failure();
        }
        return Result.success();
    }
}
