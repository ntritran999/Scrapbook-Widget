package com.group04.scrapbookwidget.ui;

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
}
