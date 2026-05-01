package com.group04.scrapbookwidget.ui;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.group04.scrapbookwidget.notifications.DeviceTokenRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class NotificationSettingViewModel extends ViewModel {
    private static final String TAG = "NotifSettingVM";

    private final MutableLiveData<Boolean> isSystemEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> enabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> messageEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> photoEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> reactionEnabled = new MutableLiveData<>(true);

    private final DeviceTokenRepository repository;
    private final Context context;

    @Inject
    public NotificationSettingViewModel(DeviceTokenRepository repository, @ApplicationContext Context context) {
        this.repository = repository;
        this.context = context;
        checkSystemStatus();
    }

    public void checkSystemStatus() {
        isSystemEnabled.setValue(NotificationManagerCompat.from(context).areNotificationsEnabled());
    }

    public LiveData<Boolean> getIsSystemEnabled() { return isSystemEnabled; }
    public LiveData<Boolean> getEnabled() { return enabled; }
    public LiveData<Boolean> getMessageEnabled() { return messageEnabled; }
    public LiveData<Boolean> getPhotoEnabled() { return photoEnabled; }
    public LiveData<Boolean> getReactionEnabled() { return reactionEnabled; }

    public void setEnabled(boolean value) {
        enabled.setValue(value);
        sync();
    }

    public void setMessageEnabled(boolean value) {
        messageEnabled.setValue(value);
        sync();
    }

    public void setPhotoEnabled(boolean value) {
        photoEnabled.setValue(value);
        sync();
    }

    public void setReactionEnabled(boolean value) {
        reactionEnabled.setValue(value);
        sync();
    }

    private void sync() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            repository.updateSettings(
                    context,
                    token,
                    enabled.getValue(),
                    messageEnabled.getValue(),
                    photoEnabled.getValue(),
                    reactionEnabled.getValue()
            );
        });
    }
}
