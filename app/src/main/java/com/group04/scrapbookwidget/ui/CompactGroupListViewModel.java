package com.group04.scrapbookwidget.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CompactGroupListViewModel extends ViewModel {
    private IUserRepository userRepository;
    private static final String TAG = "CompactGroupListViewModel";
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;

    private final MutableLiveData<List<Group>> groupsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    @Inject
    public CompactGroupListViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<List<Group>> getGroupsLiveData() {
        return groupsLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadGroupList(String userId) {
        List<Group> data = groupsLiveData.getValue();
        if (data != null && !data.isEmpty()) {
            return;
        }

        Log.d(TAG, "Loading groups for user: " + userId);
        retryCount = 0;
        loadGroupsWithRetry(userId);
    }

    private void loadGroupsWithRetry(String userId) {
        userRepository.getUserGroups(userId, new RepositoryCallback<List<Group>>() {
            @Override
            public void onSuccess(List<Group> groups) {
                Log.d(TAG, "Groups loaded successfully: " + (groups != null ? groups.size() : 0));
                retryCount = 0; // Reset retry count on success
                if (groups == null || groups.isEmpty()) {
                    groupsLiveData.setValue(new ArrayList<>());
                    return;
                }
                groupsLiveData.setValue(groups);
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error loading groups (attempt " + (retryCount + 1) + "/" + MAX_RETRIES + "): " + exception.getMessage(), exception);
                
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Log.d(TAG, "Retrying group load in 2 seconds...");
                    // Retry after 2 seconds
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        loadGroupsWithRetry(userId);
                    }, 2000);
                } else {
                    errorMessage.setValue(exception.getMessage());
                    Log.e(TAG, "Failed to load groups after " + MAX_RETRIES + " attempts");
                }
            }
        });
    }
}
