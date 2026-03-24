package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.Group;

import java.util.List;
import java.util.Map;

public interface IGroupRepository {
    void getGroupById(String groupId, RepositoryCallback<Group> callback);
    void getGroupsForUser(String userId, RepositoryCallback<List<Group>> callback);
    void updateGroup(String groupId, Group updatedGroup, RepositoryCallback<Void> callback);
    void deleteGroup(String groupId, RepositoryCallback<Void> callback);

    void getMember(String groupId, String userId, RepositoryCallback<Map<String, Object>> callback);
    void getAllMembers(String groupId, RepositoryCallback<Map<String, Map<String, Object>>> callback);

    void addMember(String groupId, String userId, String role, RepositoryCallback<Void> callback);
    void updateMemberRole(String groupId, String userId, String newRole, RepositoryCallback<Void> callback);
    void removeMember(String groupId, String userId, RepositoryCallback<Void> callback);
}
