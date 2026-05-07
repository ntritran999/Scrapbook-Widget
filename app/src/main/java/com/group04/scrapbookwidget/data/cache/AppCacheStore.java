package com.group04.scrapbookwidget.data.cache;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.data.model.User;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AppCacheStore {
    private static final String ESSENTIAL_PREF = "offline_cache_essential";
    private static final String CONTENT_PREF = "offline_cache_content";

    private static final String USER_KEY_PREFIX = "user:";
    private static final String USER_GROUPS_KEY_PREFIX = "user_groups:";
    private static final String GROUP_KEY_PREFIX = "group:";
    private static final String GROUP_MESSAGES_KEY_PREFIX = "group_messages:";
    private static final String GROUP_TODAY_MEMORIES_KEY_PREFIX = "group_today_memories:";

    private final Gson gson = new Gson();
    private final SharedPreferences essentialPreferences;
    private final SharedPreferences contentPreferences;

    @Inject
    public AppCacheStore(@ApplicationContext Context context) {
        essentialPreferences = context.getSharedPreferences(ESSENTIAL_PREF, Context.MODE_PRIVATE);
        contentPreferences = context.getSharedPreferences(CONTENT_PREF, Context.MODE_PRIVATE);
    }

    public void saveUser(@NonNull User user) {
        if ((user.getId() == null || user.getId().isEmpty())
                && (user.getUid() == null || user.getUid().isEmpty())) {
            return;
        }
        if (user.getId() != null && !user.getId().isEmpty()) {
            put(essentialPreferences, USER_KEY_PREFIX + user.getId(), user);
        }
        if (user.getUid() != null && !user.getUid().isEmpty()) {
            put(essentialPreferences, USER_KEY_PREFIX + user.getUid(), user);
        }
    }

    @Nullable
    public User getUser(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return get(essentialPreferences, USER_KEY_PREFIX + userId, User.class);
    }

    public void saveUserGroups(@NonNull String userId, @NonNull List<Group> groups) {
        if (userId.isEmpty()) {
            return;
        }
        put(contentPreferences, USER_GROUPS_KEY_PREFIX + userId, groups);
    }

    @Nullable
    public List<Group> getUserGroups(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        Type type = new TypeToken<List<Group>>() {}.getType();
        return get(contentPreferences, USER_GROUPS_KEY_PREFIX + userId, type);
    }

    public void saveGroup(@NonNull Group group) {
        if (group.getId() == null || group.getId().isEmpty()) {
            return;
        }
        put(contentPreferences, GROUP_KEY_PREFIX + group.getId(), group);
    }

    @Nullable
    public Group getGroup(@Nullable String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return null;
        }
        return get(contentPreferences, GROUP_KEY_PREFIX + groupId, Group.class);
    }

    public void saveMessages(@NonNull String groupId, @NonNull List<Message> messages) {
        if (groupId.isEmpty()) {
            return;
        }
        put(contentPreferences, GROUP_MESSAGES_KEY_PREFIX + groupId, messages);
    }

    @Nullable
    public List<Message> getMessages(@Nullable String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return null;
        }
        Type type = new TypeToken<List<Message>>() {}.getType();
        return get(contentPreferences, GROUP_MESSAGES_KEY_PREFIX + groupId, type);
    }

    public void saveTodayMemories(@NonNull String groupId, @NonNull List<TodayMemory> memories) {
        if (groupId.isEmpty()) {
            return;
        }
        put(contentPreferences, GROUP_TODAY_MEMORIES_KEY_PREFIX + groupId, memories);
    }

    @Nullable
    public List<TodayMemory> getTodayMemories(@Nullable String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return null;
        }
        Type type = new TypeToken<List<TodayMemory>>() {}.getType();
        return get(contentPreferences, GROUP_TODAY_MEMORIES_KEY_PREFIX + groupId, type);
    }

    private void put(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull Object value) {
        preferences.edit().putString(key, gson.toJson(value)).apply();
    }

    @Nullable
    private <T> T get(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull Class<T> clazz) {
        String json = preferences.getString(key, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private <T> T get(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull Type type) {
        String json = preferences.getString(key, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }
}
