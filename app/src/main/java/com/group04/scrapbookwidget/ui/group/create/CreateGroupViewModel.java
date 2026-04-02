package com.group04.scrapbookwidget.ui.group.create;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.repository.IGroupRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CreateGroupViewModel extends ViewModel {

    private final IGroupRepository groupRepository;
    private final FirebaseAuth auth;

    public final MutableLiveData<String> groupName = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Group> createdGroup = new MutableLiveData<>(null);

    @Inject
    public CreateGroupViewModel(IGroupRepository groupRepository, FirebaseAuth auth) {
        this.groupRepository = groupRepository;
        this.auth = auth;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Group> getCreatedGroup() {
        return createdGroup;
    }

    public void createGroup() {
        String name = groupName.getValue();
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("Group name cannot be empty");
            return;
        }

        if (auth.getCurrentUser() == null) {
            errorMessage.setValue("User not authenticated");
            return;
        }

        isLoading.setValue(true);
        String currentUserId = auth.getCurrentUser().getUid();

        groupRepository.createGroup(name, null, Collections.singletonList(currentUserId), new RepositoryCallback<Group>() {
            @Override
            public void onSuccess(Group data) {
                isLoading.setValue(false);
                createdGroup.setValue(data);
            }

            @Override
            public void onError(Exception exception) {
                isLoading.setValue(false);
                errorMessage.setValue(exception.getMessage());
            }
        });
    }
}
