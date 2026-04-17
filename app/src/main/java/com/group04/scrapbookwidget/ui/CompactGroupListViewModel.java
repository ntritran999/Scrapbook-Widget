package com.group04.scrapbookwidget.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CompactGroupListViewModel extends ViewModel {
    private final IUserRepository userRepository;
    private final FirebaseAuth auth;
    private static final String TAG = "CompactGroupListViewModel";

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
                
                // Sort groups by latest message createdAt
                Collections.sort(groups, (g1, g2) -> {
                    long t1 = getTimestamp(g1.getLatestMessage());
                    long t2 = getTimestamp(g2.getLatestMessage());
                    return Long.compare(t2, t1); // Descending order
                });
                
                groupsLiveData.setValue(groups);
            }

            @Override
            public void onError(Exception exception) {
                errorMessage.setValue("Failed to load groups");
            }
        });
    }

    private long getTimestamp(Message message) {
        if (message == null) return 0;
        String createdAt = message.getCreatedAt();
        if (createdAt == null || createdAt.isEmpty()) return 0;
        try {
            return Instant.parse(createdAt).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    public void refresh() {
        if (auth.getCurrentUser() != null) {
            loadGroupList(auth.getCurrentUser().getUid());
        }
    }
}
