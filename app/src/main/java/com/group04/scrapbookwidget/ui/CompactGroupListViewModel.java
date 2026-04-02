package com.group04.scrapbookwidget.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CompactGroupListViewModel extends ViewModel {
    private final IUserRepository userRepository;
    private final FirebaseAuth auth;
    private static final String TAG = "CompactGroupListViewModel";
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;

    private final MutableLiveData<List<Group>> groupsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public CompactGroupListViewModel(IUserRepository userRepository, FirebaseAuth auth) {
        this.userRepository = userRepository;
        this.auth = auth;
    }

    public LiveData<List<Group>> getGroupsLiveData() {
        return groupsLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadGroupList(String userId) {
        userRepository.getUserGroups(userId, new RepositoryCallback<List<Group>>() {
            @Override
            public void onSuccess(List<Group> groups) {
                if (groups == null || groups.isEmpty()) {
                    groupsLiveData.setValue(new ArrayList<>());
                    return;
                }
                groupsLiveData.setValue(groups);
            }

            @Override
            public void onError(Exception exception) {
                errorMessage.setValue("Failed to load groups");
            }
        });
    }

    public void refresh() {
        if (auth.getCurrentUser() != null) {
            loadGroupList(auth.getCurrentUser().getUid());
        }
    }
}
