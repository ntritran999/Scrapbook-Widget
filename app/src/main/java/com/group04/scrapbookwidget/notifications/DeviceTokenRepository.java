package com.group04.scrapbookwidget.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group04.scrapbookwidget.data.service.UserService;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public final class DeviceTokenRepository {
    private static final String TAG = "DeviceTokenRepo";
    private static final String PREFS_NAME = "device_token_prefs";
    private static final String KEY_IS_REGISTERED = "is_registered";
    private static final String KEY_REGISTERED_DEVICE_ID = "registered_device_id";

    private final UserService userService;
    private final FirebaseAuth firebaseAuth;
    private final SharedPreferences prefs;
    private boolean isRegistering = false;

    @Inject
    public DeviceTokenRepository(UserService userService, FirebaseAuth firebaseAuth, @ApplicationContext Context context) {
        this.userService = userService;
        this.firebaseAuth = firebaseAuth;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isRegistered() {
        return prefs.getBoolean(KEY_IS_REGISTERED, false);
    }

    public String getRegisteredDeviceId(Context context) {
        String savedId = prefs.getString(KEY_REGISTERED_DEVICE_ID, null);
        if (savedId == null) {
            savedId = getDeviceId(context);
        }
        return savedId;
    }

    public void enqueueRegister(Context context, String token) {
        registerNow(context, token, null);
    }

    public void registerNow(Context context, String token, Runnable onSuccess) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || token == null || token.isEmpty()) {
            return;
        }

        if (isRegistering) return;
        isRegistering = true;

        String deviceId = getDeviceId(context);

        user.getIdToken(false).addOnSuccessListener(result -> {
            String firebaseIdToken = result.getToken();
            UserService.DeviceTokenRequest body = new UserService.DeviceTokenRequest(
                    token,
                    "android",
                    deviceId,
                    getDeviceName()
            );

            userService.registerDeviceToken("Bearer " + firebaseIdToken, body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    isRegistering = false;
                    if (response.isSuccessful()) {
                        Log.d(TAG, "registerDeviceToken success");
                        prefs.edit()
                                .putBoolean(KEY_IS_REGISTERED, true)
                                .putString(KEY_REGISTERED_DEVICE_ID, deviceId)
                                .apply();
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        Log.e(TAG, "registerDeviceToken error: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    isRegistering = false;
                    Log.e(TAG, "registerDeviceToken failure", t);
                }
            });
        }).addOnFailureListener(e -> {
            isRegistering = false;
            Log.e(TAG, "Failed to get ID token", e);
        });
    }

    public void unregister(Context context, String token) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || token == null || token.isEmpty()) {
            return;
        }

        user.getIdToken(false).addOnSuccessListener(result -> {
            String firebaseIdToken = result.getToken();
            userService.deleteDeviceToken("Bearer " + firebaseIdToken, token).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "deleteDeviceToken success");
                        prefs.edit().remove(KEY_IS_REGISTERED).remove(KEY_REGISTERED_DEVICE_ID).apply();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "deleteDeviceToken failure", t);
                }
            });
        });
    }

    public void updateSettings(Context context, String token, Boolean enabled, Boolean messageEnabled, Boolean photoEnabled, Boolean reactionEnabled) {
        if (!isRegistered()) {
            Log.w(TAG, "updateSettings called but device not registered yet. Skipping.");
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnSuccessListener(result -> {
            String firebaseIdToken = result.getToken();
            UserService.NotificationSettingsRequest body = new UserService.NotificationSettingsRequest();
            body.deviceId = getRegisteredDeviceId(context);
            body.token = token;
            body.enabled = enabled;
            body.messageEnabled = messageEnabled;
            body.photoEnabled = photoEnabled;
            body.reactionEnabled = reactionEnabled;

            userService.updateNotificationSettings("Bearer " + firebaseIdToken, body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    Log.d(TAG, "updateNotificationSettings success: " + response.isSuccessful());
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "updateNotificationSettings failure", t);
                }
            });
        });
    }

    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getDeviceName() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }
}
