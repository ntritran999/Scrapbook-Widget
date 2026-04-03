package com.group04.scrapbookwidget.data.repository;

import androidx.annotation.Nullable;

import com.group04.scrapbookwidget.data.model.Reaction;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

public interface IScrapbookRepository {
    void getPage(String groupId, String pageId, RepositoryCallback<ScrapbookPage> callback);
    void getAllPages(String groupId, RepositoryCallback<List<ScrapbookPage>> callback);
    void createPage(String groupId, RepositoryCallback<ScrapbookPage> callback);

    void getItem(String groupId, String pageId, String itemId, RepositoryCallback<ScrapbookItem> callback);
    void getAllItems(String groupId, String pageId, RepositoryCallback<List<ScrapbookItem>> callback);
    void getItemsByType(String groupId, String pageId, String type, RepositoryCallback<List<ScrapbookItem>> callback);
    void addItem(String groupId, String pageId, ScrapbookItem item, RepositoryCallback<String> callback);
    void addItemWithFile(String groupId, String pageId, String imagePath, ScrapbookItem itemModel,
                         @Nullable List<List<Double>> faceEmbeddings,
                         RepositoryCallback<ScrapbookItem> callback);
    void updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem, RepositoryCallback<Void> callback);

    void getReactions(String groupId, String pageId, String itemId, RepositoryCallback<List<Reaction>> callback);
    void addReaction(String groupId, String pageId, String itemId, Reaction reaction, RepositoryCallback<Reaction> callback);
    void removeReaction(String groupId, String pageId, String itemId, String userId, RepositoryCallback<Boolean> callback);
}
