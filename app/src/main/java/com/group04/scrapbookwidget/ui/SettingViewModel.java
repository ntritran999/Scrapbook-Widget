package com.group04.scrapbookwidget.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingViewModel extends ViewModel {

    private static final String TAG = "SettingViewModel";
    private final IUserRepository userRepository;
    private final FirebaseAuth firebaseAuth;

    private final MutableLiveData<Boolean> _loggedOut = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _accountDeleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _streakEnabled = new MutableLiveData<>(true);

    public LiveData<Boolean> getLoggedOut() { return _loggedOut; }
    public LiveData<Boolean> getAccountDeleted() { return _accountDeleted; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<User> getCurrentUser() { return _currentUser; }
    public LiveData<Boolean> getStreakEnabled() { return _streakEnabled; }

    @Inject
    public SettingViewModel(IUserRepository userRepository, FirebaseAuth firebaseAuth) {
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        loadCurrentUser();
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
