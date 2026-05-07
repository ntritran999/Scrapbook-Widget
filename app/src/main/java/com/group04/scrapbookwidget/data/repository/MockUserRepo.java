package com.group04.scrapbookwidget.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.AuthService;
import com.group04.scrapbookwidget.data.service.UserService;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockUserRepo implements IUserRepository {

    private final UserService userService;
    private final AuthService authService;

    @Inject
    public MockUserRepo(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    public void login(String email, String password, RepositoryCallback<User> callback) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        userService.login(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Login failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void loginWithGoogle(String idToken, RepositoryCallback<User> callback) {
        User userRequest = new User();
        userRequest.setIdToken(idToken);

        authService.loginWithGoogle(userRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Google login verification failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void register(String email, String password, String name, RepositoryCallback<User> callback) {
        callback.onError(new Exception("Registration now requires OTP verification"));
    }

    @Override
    public void requestRegisterOtp(String email, RepositoryCallback<UserService.RegisterOtpResponse> callback) {
        userService.requestRegisterOtp(new UserService.RegisterOtpRequest(email)).enqueue(new Callback<UserService.RegisterOtpResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.RegisterOtpResponse> call,
                                   @NonNull Response<UserService.RegisterOtpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to send OTP: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.RegisterOtpResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void registerWithOtp(String email, String password, String displayName, String otpCode,
                                RepositoryCallback<UserService.RegisterResponse> callback) {
        UserService.RegisterOtpConfirmRequest request = new UserService.RegisterOtpConfirmRequest();
        request.email = email;
        request.password = password;
        request.otpCode = otpCode;
        request.displayName = displayName;
        request.username = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        request.nickname = displayName;
        request.status = "active";

        userService.registerWithOtp(request).enqueue(new Callback<UserService.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.RegisterResponse> call,
                                   @NonNull Response<UserService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Registration failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.RegisterResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void logout() {
        userService.logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d("MockUserRepo", "Logout success: " + response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("MockUserRepo", "Logout failed", t);
            }
        });
    }

    @Override
    public void getUserById(String userId, RepositoryCallback<User> callback) {
        userService.getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("User not found"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getUserByUsername(String username, RepositoryCallback<User> callback) {
        // Implementation
    }

    @Override
    public void createUser(User user, RepositoryCallback<Void> callback) {
        userService.createUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Create user failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void updateUser(String userId, User updatedUser, RepositoryCallback<Void> callback) {
        userService.updateUser(userId, updatedUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Update user failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void updateUserStatus(String userId, String status, RepositoryCallback<Void> callback) {
        User user = new User();
        user.setStatus(status);
        updateUser(userId, user, callback);
    }

    @Override
    public void updateAvatarUrl(String userId, String avatarUrl, RepositoryCallback<Void> callback) {
        User user = new User();
        user.setAvatarUrl(avatarUrl);
        updateUser(userId, user, callback);
    }

    @Override
    public void deleteUser(String userId, RepositoryCallback<Void> callback) {
        userService.deleteAccount().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Delete account failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getUserGroups(String userId, RepositoryCallback<List<Group>> callback) {
        userService.getUserGroups(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(@NonNull Call<List<Group>> call, @NonNull Response<List<Group>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to fetch groups"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Group>> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void checkUsername(String username, RepositoryCallback<UserService.UsernameCheckResponse> callback) {
        userService.checkUsername(username).enqueue(new Callback<UserService.UsernameCheckResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.UsernameCheckResponse> call, @NonNull Response<UserService.UsernameCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Check username failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.UsernameCheckResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void hasUserEnrolledFace(@NonNull String userId, @NonNull RepositoryCallback<Boolean> callback) {
        // Mock implementation
    }

    @Override
    public void saveFaceEmbedding(@NonNull String userId, @NonNull List<Double> faceEmbedding, @NonNull RepositoryCallback<Void> callback) {
        // Mock implementation
    }

    @Override
    public void uploadAvatar(File imageFile, RepositoryCallback<String> callback) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        userService.uploadAvatar(body).enqueue(new Callback<UserService.AvatarUploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.AvatarUploadResponse> call, @NonNull Response<UserService.AvatarUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().avatarUrl);
                } else {
                    callback.onError(new Exception("Avatar upload failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.AvatarUploadResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }
}
