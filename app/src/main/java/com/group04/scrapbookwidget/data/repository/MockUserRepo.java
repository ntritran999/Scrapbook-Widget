package com.group04.scrapbookwidget.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.UserService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockUserRepo implements IUserRepository {

    private final UserService userService;

    @Inject
    public MockUserRepo(UserService userService) {
        this.userService = userService;
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
    public void register(String email, String password, String name, RepositoryCallback<User> callback) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setDisplayName(name);
        userService.register(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Registration failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
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
}
