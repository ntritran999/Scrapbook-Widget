package com.group04.scrapbookwidget.data.repository;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.group04.scrapbookwidget.data.model.Reaction;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.data.service.GroupService;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScrapbookRepository implements IScrapbookRepository {
    private GroupService groupService;
    private Gson gson;
    @Inject
    ScrapbookRepository(GroupService groupService) {
        this.gson = new Gson(); // for parsing json
        this.groupService = groupService;
    }

    @Override
    public void getPage(String groupId, String pageId, RepositoryCallback<ScrapbookPage> callback) {

    }

    @Override
    public void getAllPages(String groupId, RepositoryCallback<List<ScrapbookPage>> callback) {
        groupService.getScrapbookPages(groupId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ScrapbookPage>> call, Response<List<ScrapbookPage>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to load pages: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<ScrapbookPage>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void createPage(String groupId, ScrapbookPage page, RepositoryCallback<ScrapbookPage> callback) {
        groupService.createScrapbookPage(groupId, page).enqueue(new Callback<ScrapbookPage>() {
            @Override
            public void onResponse(Call<ScrapbookPage> call, Response<ScrapbookPage> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to create page: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ScrapbookPage> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void removePage(String groupId, String pageId, RepositoryCallback<Void> callback) {
        groupService.removeScrapbookPage(groupId, pageId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to remove page: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getItem(String groupId, String pageId, String itemId, RepositoryCallback<ScrapbookItem> callback) {
        groupService.getItem(groupId, pageId, itemId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ScrapbookItem> call, Response<ScrapbookItem> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<ScrapbookItem> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getAllItems(String groupId, String pageId, RepositoryCallback<List<ScrapbookItem>> callback) {
        groupService.getItems(groupId, pageId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ScrapbookItem>> call, Response<List<ScrapbookItem>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ScrapbookItem>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getItemsByType(String groupId, String pageId, String type, RepositoryCallback<List<ScrapbookItem>> callback) {

    }

    @Override
    public void addItem(String groupId, String pageId, ScrapbookItem item, RepositoryCallback<String> callback) {
        groupService.createItem(groupId, pageId, item).enqueue(new Callback<ScrapbookItem>() {
            @Override
            public void onResponse(Call<ScrapbookItem> call, Response<ScrapbookItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getId());
                } else {
                    callback.onError(new Exception("Failed to create item: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ScrapbookItem> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void addItemWithFile(String groupId, String pageId, String imagePath, ScrapbookItem itemModel,
                                @Nullable List<List<Double>> faceEmbeddings,
                                RepositoryCallback<ScrapbookItem> callback) {
        long startTime = System.currentTimeMillis();
        itemModel.setFaceEmbeddings(faceEmbeddings);

        String jsonItem = gson.toJson(itemModel);
        RequestBody payloadPart = RequestBody.create(MediaType.parse("application/json"), jsonItem);
        // logcat
        android.util.Log.d("ScrapbookRepository", "addItemWithFile: " + jsonItem);

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            callback.onError(new Exception("Image file not found"));
            return;
        }

        android.util.Log.d("ScrapbookRepo", "addItemWithFile: File size = " + (imageFile.length() / 1024) + " KB");

        String mimeType = "image/jpeg";
        if (imageFile.getName().toLowerCase().endsWith(".png")) {
            mimeType = "image/png";
        } else if (imageFile.getName().toLowerCase().endsWith(".webp")) {
            mimeType = "image/webp";
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);
        // logcat
        android.util.Log.d("ScrapbookRepository", "addItemWithFile: " + imageFile.getName());

        groupService.createItemWithFile(groupId, pageId, filePart, payloadPart).enqueue(new retrofit2.Callback<ScrapbookItem>() {
            @Override
            public void onResponse(Call<ScrapbookItem> call, retrofit2.Response<ScrapbookItem> response) {
                android.util.Log.d("ScrapbookRepo", "addItemWithFile: Response received in " + (System.currentTimeMillis() - startTime) + " ms");
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ScrapbookItem> call, Throwable t) {
                android.util.Log.d("ScrapbookRepo", "addItemWithFile: Failed after " + (System.currentTimeMillis() - startTime) + " ms");
                callback.onError(new Exception(t.getMessage()));
            }
        });

        // logcat
        android.util.Log.d("ScrapbookRepository", "addItemWithFile: done");
    }

    @Override
    public void updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem, RepositoryCallback<Void> callback) {

    }

    @Override
    public void getReactions(String groupId, String pageId, String itemId, RepositoryCallback<List<Reaction>> callback) {
        groupService.getReactions(groupId, pageId, itemId).enqueue(new Callback<List<Reaction>>() {
            @Override
            public void onResponse(Call<List<Reaction>> call, Response<List<Reaction>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
                else {
                    callback.onError(new Exception("Failed to load reactions."));
                }
            }

            @Override
            public void onFailure(Call<List<Reaction>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void addReaction(String groupId, String pageId, String itemId, Reaction reaction, RepositoryCallback<Reaction> callback) {
        groupService.addReaction(groupId, pageId, itemId, reaction).enqueue(new Callback<Reaction>() {
            @Override
            public void onResponse(Call<Reaction> call, Response<Reaction> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
                else {
                    callback.onError(new Exception("Failed to add reaction."));
                }
            }

            @Override
            public void onFailure(Call<Reaction> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void removeReaction(String groupId, String pageId, String itemId, String userId, RepositoryCallback<Boolean> callback) {
        groupService.removeReaction(groupId, pageId, itemId, userId).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
                else {
                    callback.onError(new Exception("Failed to remove reaction."));
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }
}
