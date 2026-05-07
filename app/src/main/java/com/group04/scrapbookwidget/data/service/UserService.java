package com.group04.scrapbookwidget.data.service;

import com.google.gson.annotations.SerializedName;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {
    @POST("auth/login")
    Call<User> login(@Body User user);

    @POST("auth/register")
    Call<User> register(@Body User user);

    @POST("auth/register/otp")
    Call<RegisterOtpResponse> requestRegisterOtp(@Body RegisterOtpRequest request);

    @POST("auth/register")
    Call<RegisterResponse> registerWithOtp(@Body RegisterOtpConfirmRequest request);

    @POST("auth/session")
    Call<User> verifySession(@Body User user);

    @POST("auth/signout")
    Call<Void> logout();

    @DELETE("auth/account")
    Call<Void> deleteAccount();

    @GET("users")
    Call<List<User>> getUsers();

    @GET("users/{userId}")
    Call<User> getUserById(@Path("userId") String userId);

    @POST("users")
    Call<User> createUser(@Body User user);

    @PATCH("users/{userId}")
    Call<User> updateUser(@Path("userId") String userId, @Body User user);

    @GET("users/{userId}/groups")
    Call<List<Group>> getUserGroups(@Path("userId") String userId);

    @GET("users/check-username")
    Call<UsernameCheckResponse> checkUsername(@Query("q") String username);

    @Multipart
    @POST("users/avatar")
    Call<AvatarUploadResponse> uploadAvatar(@Part MultipartBody.Part file);

    /**
     * Enroll user's face for automatic tagging in group photos.
     * Sends the extracted face embedding to the backend for storage.
     */
    @POST("users/{userId}/enroll-face")
    Call<FaceEnrollmentResponse> enrollUserFace(@Path("userId") String userId, @Body FaceEnrollmentRequest request);

    @POST("users/me/device-token")
    Call<Void> registerDeviceToken(@Header("Authorization") String token, @Body DeviceTokenRequest request);

    @DELETE("users/me/device-token")
    Call<Void> deleteDeviceToken(@Header("Authorization") String token, @Query("token") String deviceToken);

    @PATCH("users/me/device-token/settings")
    Call<Void> updateNotificationSettings(@Header("Authorization") String token, @Body NotificationSettingsRequest request);

    class UsernameCheckResponse {
        public boolean available;
        public boolean valid;
        public String reason;
    }

    class AvatarUploadResponse {
        public String avatarUrl;
    }

    class RegisterOtpRequest {
        public String email;

        public RegisterOtpRequest(String email) {
            this.email = email;
        }
    }

    class RegisterOtpResponse {
        public String email;
        public boolean otpSent;
        public int expiresInMinutes;
        public int retryAfterSeconds;
        public String message;
    }

    class RegisterOtpConfirmRequest {
        public String email;
        public String password;
        public String otpCode;
        public String displayName;
        public String username;
        public String nickname;
        public String avatarUrl;
        public String status;
    }

    class RegisterResponse {
        public String uid;
        public String email;
        public Onboarding onboarding;
        public String message;
    }

    class Onboarding {
        public String defaultGroupId;
        public String defaultGroupName;
        public String defaultPageId;
    }

    /**
     * Request model for face enrollment endpoint.
     * Contains the extracted face embedding vector.
     */
    class FaceEnrollmentRequest {
        @SerializedName("faceVector")
        public List<Double> faceVector;

        public FaceEnrollmentRequest(List<Double> faceVector) {
            this.faceVector = faceVector;
        }
    }

    /**
     * Response model from face enrollment endpoint.
     */
    class FaceEnrollmentResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("message")
        public String message;

        @SerializedName("enrolledAt")
        public long enrolledAt;
    }

    class DeviceTokenRequest {
        public String token;
        public String platform;
        public String deviceId;
        public String deviceName;

        public DeviceTokenRequest(String token, String platform, String deviceId, String deviceName) {
            this.token = token;
            this.platform = platform;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
        }
    }

    class NotificationSettingsRequest {
        public String deviceId;
        public String token;
        public Boolean enabled;
        public Boolean messageEnabled;
        public Boolean photoEnabled;
        public Boolean reactionEnabled;
    }
}
