package com.group04.scrapbookwidget;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.group04.scrapbookwidget.data.worker.WidgetUpdateWorker;
import com.group04.scrapbookwidget.notifications.NotificationChannels;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class ScrapbookWidgetApplication extends Application implements Configuration.Provider {
    @Inject
    HiltWorkerFactory hiltWorkerFactory;
    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannels.create(this);

        PeriodicWorkRequest widgetUpdateWorkRequest =
                new PeriodicWorkRequest.Builder(WidgetUpdateWorker.class, 30, TimeUnit.MINUTES)
                        .build();
        WorkManager.getInstance(this).cancelUniqueWork("updateWidget");
        WorkManager
                .getInstance(this)
                .enqueueUniquePeriodicWork(
                        "updateWidget",
                        ExistingPeriodicWorkPolicy.KEEP,
                        widgetUpdateWorkRequest
                );
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build();
    }
}
