package com.group04.scrapbookwidget.data.repository;

import android.util.Log;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.UserService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockUserRepo implements IUserRepository {

    private UserService userService;
    @Inject
    public MockUserRepo(UserService userService) {
        this.userService = userService;
    }
    @Override
    public void getUserById(String userId, RepositoryCallback<User> callback) {

    }
    @Override
    public void getUserByUsername(String username, RepositoryCallback<User> callback) {

    }
    @Override
    public void createUser(User user, RepositoryCallback<Void> callback) {

    }
    @Override
    public void updateUser(String userId, User updatedUser, RepositoryCallback<Void> callback) {

    }
    @Override
    public void updateUserStatus(String userId, String status, RepositoryCallback<Void> callback) {

    }
    @Override
    public void updateAvatarUrl(String userId, String avatarUrl, RepositoryCallback<Void> callback) {

    }
    @Override
    public void deleteUser(String userId, RepositoryCallback<Void> callback) {

    }
    @Override
    public void getUserGroups(String userId, RepositoryCallback<List<Group>> callback) {
        userService.getUserGroups(userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                    Log.d("REPO", "Url: " + call.request().url());
                }
                else {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }
}
