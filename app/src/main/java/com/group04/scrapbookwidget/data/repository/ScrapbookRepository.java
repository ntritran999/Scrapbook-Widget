package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

import javax.inject.Inject;

public class ScrapbookRepository implements IScrapbookRepository {
    @Inject
    ScrapbookRepository() {}

    @Override
    public void getPage(String groupId, String pageId, RepositoryCallback<ScrapbookPage> callback) {

    }

    @Override
    public void getAllPages(String groupId, RepositoryCallback<List<ScrapbookPage>> callback) {

    }

    @Override
    public void createPage(String groupId, ScrapbookPage page, RepositoryCallback<String> callback) {

    }

    @Override
    public void getItem(String groupId, String pageId, String itemId, RepositoryCallback<ScrapbookItem> callback) {

    }

    @Override
    public void getAllItems(String groupId, String pageId, RepositoryCallback<List<ScrapbookItem>> callback) {

    }

    @Override
    public void getItemsByType(String groupId, String pageId, String type, RepositoryCallback<List<ScrapbookItem>> callback) {

    }

    @Override
    public void addItem(String groupId, String pageId, ScrapbookItem item, RepositoryCallback<String> callback) {

    }

    @Override
    public void updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem, RepositoryCallback<Void> callback) {

    }
}
