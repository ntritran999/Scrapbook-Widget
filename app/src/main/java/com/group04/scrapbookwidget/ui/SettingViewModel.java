package com.group04.scrapbookwidget.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.Invitation;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class SettingViewModel extends ViewModel {

    private static final String TAG = "SettingViewModel";
    private final IUserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final GroupService groupService;

    private final MutableLiveData<Boolean> _loggedOut = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _accountDeleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _streakEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> _isUploading = new MutableLiveData<>(false);
    
    private final MutableLiveData<List<Invitation>> _invitations = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Invitation>> getInvitations() { return _invitations; }
    public LiveData<User> getCurrentUser() { return _currentUser; }
    public LiveData<Boolean> getLoggedOut() { return _loggedOut; }
    public LiveData<Boolean> getAccountDeleted() { return _accountDeleted; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Boolean> getStreakEnabled() { return _streakEnabled; }
    public LiveData<Boolean> isUploading() { return _isUploading; }

    @Inject
    public SettingViewModel(IUserRepository userRepository, FirebaseAuth firebaseAuth, GroupService groupService) {
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        this.groupService = groupService;
        loadCurrentUser();
        loadInvitations();
    }

    public void refresh() {
        loadCurrentUser();
        loadInvitations();
    }

    private void loadCurrentUser() {
        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();
            Log.d(TAG, "Loading user info for UID: " + uid);
            userRepository.getUserById(uid, new RepositoryCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    Log.d(TAG, "Successfully loaded user: " + (result != null ? result.getDisplayName() : "null"));
                    if (result != null) {
                        _currentUser.setValue(result);
                    } else {
                        setDummyUser();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading user info", e);
                    setDummyUser();
                }
            });
        } else {
            Log.w(TAG, "No Firebase user found");
        }
    }

    public void loadInvitations() {
        groupService.getMyInvitations().enqueue(new Callback<List<Invitation>>() {
            @Override
            public void onResponse(Call<List<Invitation>> call, Response<List<Invitation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _invitations.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Invitation>> call, Throwable t) {
                Log.e(TAG, "Failed to load invitations", t);
            }
        });
    }

    public void acceptInvitation(String groupId) {
        groupService.acceptInvitation(groupId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadInvitations(); // Refresh invitations list
                    // You might want to refresh groups list here too if it was displayed
                } else {
                    _errorMessage.setValue("Failed to accept invitation");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                _errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void declineInvitation(String groupId) {
        groupService.declineInvitation(groupId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadInvitations();
                } else {
                    _errorMessage.setValue("Failed to decline invitation");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                _errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void uploadAvatar(File file) {
        if (firebaseAuth.getCurrentUser() == null) return;
        _isUploading.setValue(true);
        userRepository.uploadAvatar(file, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String avatarUrl) {
                userRepository.updateAvatarUrl(firebaseAuth.getCurrentUser().getUid(), avatarUrl, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        _isUploading.setValue(false);
                        loadCurrentUser(); // Reload to update UI
                    }

                    @Override
                    public void onError(Exception e) {
                        _isUploading.setValue(false);
                        _errorMessage.setValue("Failed to update avatar URL");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                _isUploading.setValue(false);
                _errorMessage.setValue("Upload failed");
            }
        });
    }

    private void setDummyUser() {
        User dummy = new User();
        dummy.setDisplayName("Trâm Võ");
        dummy.setUsername("tremolitee");
        dummy.setEmail(firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getEmail() : "user@example.com");
        _currentUser.setValue(dummy);
    }

    public void setStreakEnabled(boolean enabled) {
        _streakEnabled.setValue(enabled);
    }

    public void signOut() {
        userRepository.logout();
        _loggedOut.setValue(true);
    }

    public void deleteAccount() {
        if (firebaseAuth.getCurrentUser() == null) return;
        userRepository.deleteUser(firebaseAuth.getCurrentUser().getUid(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _accountDeleted.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.setValue(e.getMessage());
            }
        });
    }
}
