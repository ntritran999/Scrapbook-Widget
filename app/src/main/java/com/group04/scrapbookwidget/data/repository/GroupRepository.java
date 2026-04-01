package com.group04.scrapbookwidget.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.LeaveGroupResponse;
import com.group04.scrapbookwidget.data.service.GroupService;
import com.group04.scrapbookwidget.data.service.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class GroupRepository implements IGroupRepository {

    private final GroupService groupService;
    private final UserService userService;
    private final FirebaseFirestore db;

    @Inject
    public GroupRepository(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getGroupById(String groupId, RepositoryCallback<Group> callback) {
        groupService.getGroupById(groupId).enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to get group: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getGroupsForUser(String userId, RepositoryCallback<List<Group>> callback) {
        userService.getUserGroups(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to get user groups: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void createGroup(String name, String avatarUrl, List<String> memberIds, RepositoryCallback<Group> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("groupName", name);
        body.put("avatarUrl", avatarUrl);
        body.put("memberIds", memberIds != null ? memberIds : new ArrayList<>());

        groupService.createGroup(body).enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to create group: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void updateGroup(String groupId, Group updatedGroup, RepositoryCallback<Void> callback) {
        db.collection("groups").document(groupId).set(updatedGroup)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e));
    }

    @Override
    public void deleteGroup(String groupId, RepositoryCallback<Void> callback) {
        db.collection("groups").document(groupId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e));
    }

    @Override
    public void getMember(String groupId, String userId, RepositoryCallback<Map<String, Object>> callback) {
        // Implementation for Firestore or API
    }

    @Override
    public void getAllMembers(String groupId, RepositoryCallback<Map<String, Map<String, Object>>> callback) {
        // Implementation for Firestore or API
    }

    @Override
    public void addMember(String groupId, String userId, String role, RepositoryCallback<Void> callback) {
        // Implementation for Firestore or API
    }

    @Override
    public void updateMemberRole(String groupId, String userId, String newRole, RepositoryCallback<Void> callback) {
        // Implementation for Firestore or API
    }

    @Override
    public void removeMember(String groupId, String userId, RepositoryCallback<Void> callback) {
        groupService.removeMember(groupId, userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Failed to remove member: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void leaveGroup(String groupId, RepositoryCallback<LeaveGroupResponse> callback) {
        groupService.leaveGroup(groupId).enqueue(new Callback<LeaveGroupResponse>() {
            @Override
            public void onResponse(Call<LeaveGroupResponse> call, Response<LeaveGroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to leave group: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<LeaveGroupResponse> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }
}
