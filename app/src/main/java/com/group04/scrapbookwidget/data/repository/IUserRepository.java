package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.UserService;

import java.io.File;
import java.util.List;

public interface IUserRepository {
    void login(String email, String password, RepositoryCallback<User> callback);
    void register(String email, String password, String name, RepositoryCallback<User> callback);
    void logout();

    void getUserById(String userId, RepositoryCallback<User> callback);
    void getUserByUsername(String username, RepositoryCallback<User> callback);
    void createUser(User user, RepositoryCallback<Void> callback);
    void updateUser(String userId, User updatedUser, RepositoryCallback<Void> callback);
    void updateUserStatus(String userId, String status, RepositoryCallback<Void> callback);
    void updateAvatarUrl(String userId, String avatarUrl, RepositoryCallback<Void> callback);
    void deleteUser(String userId, RepositoryCallback<Void> callback);

    void getUserGroups(String userId, RepositoryCallback<List<Group>> callback);
    
    void checkUsername(String username, RepositoryCallback<UserService.UsernameCheckResponse> callback);
    void uploadAvatar(File imageFile, RepositoryCallback<String> callback);
}
