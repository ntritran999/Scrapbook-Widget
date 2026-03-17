package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.group04.scrapbookwidget.data.model.Group;

import java.util.List;
import java.util.Map;

public interface IGroupRepository {
    Task<Group> getGroupById(String groupId);
    Task<List<Group>> getGroupsForUser(String userId);
    Task<Void> updateGroup(String groupId, Group updatedGroup);
    Task<Void> deleteGroup(String groupId);

    Task<Map<String, Object>> getMember(String groupId, String userId);
    Task<Map<String, Map<String, Object>>> getAllMembers(String groupId);

    Task<Void> addMember(String groupId, String userId, String role);
    Task<Void> updateMemberRole(String groupId, String userId, String newRole);
    Task<Void> removeMember(String groupId, String userId);
}
