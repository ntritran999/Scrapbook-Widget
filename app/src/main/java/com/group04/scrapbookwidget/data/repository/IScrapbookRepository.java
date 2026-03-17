package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

public interface IScrapbookRepository {
    Task<ScrapbookPage> getPage(String groupId, String pageId);
    Task<List<ScrapbookPage>> getAllPages(String groupId);
    Task<String> createPage(String groupId, ScrapbookPage page);

    Task<ScrapbookItem> getItem(String groupId, String pageId, String itemId);
    Task<List<ScrapbookItem>> getAllItems(String groupId, String pageId);
    Task<List<ScrapbookItem>> getItemsByType(String groupId, String pageId, String type);
    Task<String> addItem(String groupId, String pageId, ScrapbookItem item);
    Task<Void> updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem);

}
