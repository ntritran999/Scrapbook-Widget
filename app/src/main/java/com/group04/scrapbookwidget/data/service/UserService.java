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

    class UsernameCheckResponse {
        public boolean available;
        public boolean valid;
        public String reason;
    }

    class AvatarUploadResponse {
        public String avatarUrl;
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
}
