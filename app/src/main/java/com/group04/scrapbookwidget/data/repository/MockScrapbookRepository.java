package com.group04.scrapbookwidget.data.repository;

import android.util.Log;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockScrapbookRepository implements IScrapbookRepository {
    private GroupService groupService;
    String mockGroupId = "test_group1";

    @Inject
    MockScrapbookRepository(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public void getPage(String groupId, String pageId, RepositoryCallback<ScrapbookPage> callback) {

    }

    @Override
    public void getAllPages(String groupId, RepositoryCallback<List<ScrapbookPage>> callback) {
        groupService.getScrapbookPages(mockGroupId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ScrapbookPage>> call, Response<List<ScrapbookPage>> response) {
                if (response.isSuccessful()) {
                    Log.d("REPO", "Actual URL called: " + call.request().url().toString());
                    callback.onSuccess(response.body());
                }
                else {
                    Log.d("REPO", "unsuccessful");
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailure(Call<List<ScrapbookPage>> call, Throwable t) {
                Log.d("REPO", "Server broken");
                callback.onError((Exception) t);
            }
        });
    }

    @Override
    public void createPage(String groupId, ScrapbookPage page, RepositoryCallback<String> callback) {

    }

    @Override
    public void getItem(String groupId, String pageId, String itemId, RepositoryCallback<ScrapbookItem> callback) {
        groupService.getItem(mockGroupId, pageId, itemId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ScrapbookItem> call, Response<ScrapbookItem> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<ScrapbookItem> call, Throwable t) {
                callback.onError((Exception) t);
            }
        });
    }

    @Override
    public void getAllItems(String groupId, String pageId, RepositoryCallback<List<ScrapbookItem>> callback) {
        groupService.getItems(mockGroupId, pageId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ScrapbookItem>> call, Response<List<ScrapbookItem>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ScrapbookItem>> call, Throwable t) {
                callback.onError((Exception) t);
            }
        });
    }

    @Override
    public void getItemsByType(String groupId, String pageId, String type, RepositoryCallback<List<ScrapbookItem>> callback) {

    }

    @Override
    public void addItem(String groupId, String pageId, ScrapbookItem item, RepositoryCallback<String> callback) {

    }

    @Override
    public void addItemWithFile(String groupId, String pageId, String imagePath, ScrapbookItem itemModel,
                                List<List<Double>> faceEmbeddings,
                                RepositoryCallback<ScrapbookItem> callback) {

    }

    @Override
    public void updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem, RepositoryCallback<Void> callback) {

    }
}
