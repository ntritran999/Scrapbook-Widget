package com.group04.scrapbookwidget.ui.group;

import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.InviteLinkResponse;
import com.group04.scrapbookwidget.data.model.LeaveGroupResponse;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class GroupSettingsViewModel extends ViewModel {

    private final GroupService groupService;
    private final FirebaseAuth auth;

    private final MutableLiveData<Group> _group = new MutableLiveData<>();
    public LiveData<Group> getGroup() { return _group; }

    private final MutableLiveData<List<User>> _members = new MutableLiveData<>();
    public LiveData<List<User>> getMembers() { return _members; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _myRole = new MutableLiveData<>("Member");
    public LiveData<String> getMyRole() { return _myRole; }

    private final MutableLiveData<Boolean> _isAdmin = new MutableLiveData<>(false);
    public LiveData<Boolean> isAdmin() { return _isAdmin; }

    private final MutableLiveData<Boolean> _isOwner = new MutableLiveData<>(false);
    public LiveData<Boolean> isOwner() { return _isOwner; }

    private final MutableLiveData<String> _memberCountText = new MutableLiveData<>("0 Members");
    public LiveData<String> getMemberCountText() { return _memberCountText; }

    private final MutableLiveData<List<User>> _searchResults = new MutableLiveData<>();
    public LiveData<List<User>> getSearchResults() { return _searchResults; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    public interface InviteLinkCallback {
        void onSuccess(String inviteLink);
        void onError(String message);
    }

    @Inject
    public GroupSettingsViewModel(GroupService groupService, FirebaseAuth auth) {
        this.groupService = groupService;
        this.auth = auth;
    }

    public void loadGroupDetails(String groupId) {
        _isLoading.setValue(true);
        groupService.getGroupById(groupId).enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Group group = response.body();
                    _group.setValue(group);
                    checkPermissions(group);
                }
                loadMembers(groupId);
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void loadMembers(String groupId) {
        _isLoading.setValue(true);
        groupService.getGroupMembers(groupId).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<User> members = response.body();
                    _members.setValue(members);
                    _memberCountText.setValue(members.size() + " Members");
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    private void checkPermissions(Group group) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

        boolean owner = currentUserId.equals(group.getCreatedBy());
        _isOwner.setValue(owner);
        // Based on API docs, any member can update name and avatar
        _isAdmin.setValue(true);
        _myRole.setValue(owner ? "Owner" : "Member");
    }

    public void leaveGroup(String groupId, Runnable onSuccess) {
        _isLoading.setValue(true);
        groupService.leaveGroup(groupId).enqueue(new Callback<LeaveGroupResponse>() {
            @Override
            public void onResponse(Call<LeaveGroupResponse> call, Response<LeaveGroupResponse> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    onSuccess.run();
                } else {
                    _error.setValue("Failed to leave group");
                }
            }

            @Override
            public void onFailure(Call<LeaveGroupResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void removeMember(String groupId, String userId) {
        _isLoading.setValue(true);
        groupService.removeMember(groupId, userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadMembers(groupId);
                } else {
                    _isLoading.setValue(false);
                    _error.setValue("Failed to remove member");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void updateGroupName(String groupId, String newName) {
        _isLoading.setValue(true);
        Map<String, Object> body = new HashMap<>();
        body.put("groupName", newName);
        groupService.updateGroupName(groupId, body).enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _group.setValue(response.body());
                } else {
                    _error.setValue("Failed to update group name");
                }
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void updateUnreadCount(String groupId, int unreadCount) {
        Group currentGroup = _group.getValue();
        if (currentGroup != null && currentGroup.getId() != null && currentGroup.getId().equals(groupId)) {
            currentGroup.setUnreadCount(unreadCount);
            _group.setValue(currentGroup);
        }
    }

    public void uploadGroupAvatar(String groupId, File file) {
        _isLoading.setValue(true);
        
        String mimeType = getMimeType(file);
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        groupService.uploadGroupAvatar(groupId, body).enqueue(new Callback<GroupService.AvatarUploadResponse>() {
            @Override
            public void onResponse(Call<GroupService.AvatarUploadResponse> call, Response<GroupService.AvatarUploadResponse> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Group currentGroup = _group.getValue();
                    if (currentGroup != null) {
                        currentGroup.setAvatarUrl(response.body().avatarUrl);
                        _group.setValue(currentGroup);
                    }
                } else {
                    String errorMsg = "Failed to upload avatar";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("GroupSettingsViewModel", "Error parsing error body", e);
                    }
                    _error.setValue(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<GroupService.AvatarUploadResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    private String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
        if (extension == null || extension.isEmpty()) {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
        }
        if (extension != null) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (type != null) return type;
        }
        return "image/jpeg"; // Default fallback
    }

    public void searchUsers(String query) {
        if (query.isEmpty()) {
            _searchResults.setValue(new ArrayList<>());
            return;
        }
        _isLoading.setValue(true);
        groupService.searchUsers(query).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _searchResults.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void inviteUser(String groupId, String userId, Runnable onSuccess) {
        _isLoading.setValue(true);
        Map<String, String> body = new HashMap<>();
        body.put("userId", userId);
        
        Log.d("GroupSettingsViewModel", "inviteUser request body: " + body.toString());

        groupService.inviteUser(groupId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful()) {
                    onSuccess.run();
                } else {
                    _error.setValue("Failed to send invitation");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue(t.getMessage());
            }
        });
    }

    public void getInviteLink(String groupId, InviteLinkCallback callback) {
        _isLoading.setValue(true);
        groupService.getInviteLink(groupId).enqueue(new Callback<InviteLinkResponse>() {
            @Override
            public void onResponse(Call<InviteLinkResponse> call, Response<InviteLinkResponse> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getInviteLink() != null
                        && !response.body().getInviteLink().trim().isEmpty()) {
                    callback.onSuccess(response.body().getInviteLink().trim());
                } else {
                    String message = "Failed to get invite link";
                    _error.setValue(message);
                    callback.onError(message);
                }
            }

            @Override
            public void onFailure(Call<InviteLinkResponse> call, Throwable t) {
                _isLoading.setValue(false);
                String message = t.getMessage() != null ? t.getMessage() : "Failed to get invite link";
                _error.setValue(message);
                callback.onError(message);
            }
        });
    }
}
