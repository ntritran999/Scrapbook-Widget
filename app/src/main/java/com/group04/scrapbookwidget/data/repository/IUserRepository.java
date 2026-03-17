package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.group04.scrapbookwidget.data.model.User;

public interface IUserRepository {
    Task<User> getUserById(String userId);
    Task<User> getUserByUsername(String username);
    Task<Void> createUser(User user);
    Task<Void> updateUser(String userId, User updatedUser);
    Task<Void> updateUserStatus(String userId, String status);
    Task<Void> updateAvatarUrl(String userId, String avatarUrl);
    Task<Void> deleteUser(String userId);
}
