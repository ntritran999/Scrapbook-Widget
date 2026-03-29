package com.group04.scrapbookwidget.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditProfileViewModel extends ViewModel {

    private final IUserRepository userRepository;
    private final FirebaseAuth firebaseAuth;

    public MutableLiveData<String> nickname = new MutableLiveData<>("");
    public MutableLiveData<String> username = new MutableLiveData<>("");
    private final MutableLiveData<String> _avatarUrl = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _usernameError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>(null);

    public LiveData<String> getAvatarUrl() { return _avatarUrl; }
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getUsernameError() { return _usernameError; }
    public LiveData<Boolean> getUpdateSuccess() { return _updateSuccess; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    @Inject
    public EditProfileViewModel(IUserRepository userRepository, FirebaseAuth firebaseAuth) {
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        loadCurrentUserData();
    }

    private void loadCurrentUserData() {
        if (firebaseAuth.getCurrentUser() == null) return;
        _isLoading.setValue(true);
        userRepository.getUserById(firebaseAuth.getCurrentUser().getUid(), new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                _isLoading.setValue(false);
                if (result != null) {
                    nickname.setValue(result.getNickname());
                    username.setValue(result.getUsername());
                    _avatarUrl.setValue(result.getAvatarUrl());
                }
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Failed to load profile");
            }
        });
    }

    public void uploadAvatar(File file) {
        _isLoading.setValue(true);
        userRepository.uploadAvatar(file, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                _isLoading.setValue(false);
                _avatarUrl.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Upload failed");
            }
        });
    }

    public void saveProfile() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String newUsername = username.getValue();
        if (newUsername == null || !newUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
            _usernameError.setValue("Invalid username format");
            return;
        }
        _usernameError.setValue(null);

        _isLoading.setValue(true);
        User updatedUser = new User();
        updatedUser.setNickname(nickname.getValue());
        updatedUser.setUsername(newUsername);
        updatedUser.setAvatarUrl(_avatarUrl.getValue());

        userRepository.updateUser(firebaseAuth.getCurrentUser().getUid(), updatedUser, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _isLoading.setValue(false);
                _updateSuccess.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _errorMessage.setValue(e.getMessage());
            }
        });
    }
}
