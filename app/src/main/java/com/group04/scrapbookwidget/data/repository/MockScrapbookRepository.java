package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.group04.scrapbookwidget.data.model.ItemContent;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MockScrapbookRepository implements IScrapbookRepository{
    @Inject
    MockScrapbookRepository() {

    }
    @Override
    public Task<List<ScrapbookPage>> getAllPages(String groupId) {
        List<ScrapbookPage> pages = new ArrayList<>();

        ScrapbookPage page1 = new ScrapbookPage();
        page1.setBackgroundImage("http://10.0.2.2:8080/test__2_.jpg");

        ScrapbookPage page2 = new ScrapbookPage();
        page2.setBackgroundImage("http://10.0.2.2:8080/test__2_.jpg");

        ScrapbookPage page3 = new ScrapbookPage();
        page3.setBackgroundImage("http://10.0.2.2:8080/test__2_.jpg");

        pages.add(page1);
        pages.add(page2);
        pages.add(page3);
        return Tasks.forResult(pages);
    }

    @Override
    public Task<List<ScrapbookItem>> getAllItems(String groupId, String pageId) {
        List<ScrapbookItem> items = new ArrayList<>();

        items.add(createMockPhotoItem("item_1", "http://10.0.2.2:8080/test__1_.jpg", 100f, 200f, 100f, 200f, -10f));
        items.add(createMockPhotoItem("item_2", "http://10.0.2.2:8080/test__4_.jpg", 0f, 0f, 100f, 200f, 15f));
        items.add(createMockPhotoItem("item_3", "http://10.0.2.2:8080/test__3_.jpg", 200f, 300f, 100f, 200f, 5f));

        return Tasks.forResult(items);
    }

    private ScrapbookItem createMockPhotoItem(String id, String url, float x, float y, float w, float h, float rot) {
        ScrapbookItem item = new ScrapbookItem();
        item.setId(id);
        item.setType("photo");

        Layout layout = new Layout(x, y, w, h, rot, 1.0f, 1);
        item.setLayout(layout);

        ItemContent content = new ItemContent(url, "caption");
        item.setContent(content);

        return item;
    }

    @Override public Task<ScrapbookPage> getPage(String groupId, String pageId) { return Tasks.forResult(null); }
    @Override public Task<String> createPage(String groupId, ScrapbookPage page) { return Tasks.forResult("new_id"); }
    @Override public Task<ScrapbookItem> getItem(String groupId, String pageId, String itemId) {
        String caption = "Hello world";
        String url = "";
        if (itemId.equals("item_1")) {
            url = "http://10.0.2.2:8080/test__1_.jpg";
        }
        else if (itemId.equals("item_2")) {
            url = "http://10.0.2.2:8080/test__4_.jpg";
        }
        else if (itemId.equals("item_3")) {
            url = "http://10.0.2.2:8080/test__3_.jpg";
        }
        ScrapbookItem item = new ScrapbookItem();
        item.setContent(new ItemContent(url, caption));
        return Tasks.forResult(item);
    }
    @Override public Task<List<ScrapbookItem>> getItemsByType(String groupId, String pageId, String type) { return Tasks.forResult(new ArrayList<>()); }
    @Override public Task<String> addItem(String groupId, String pageId, ScrapbookItem item) { return Tasks.forResult("new_item_id"); }
    @Override public Task<Void> updateItem(String groupId, String pageId, String itemId, ScrapbookItem updatedItem) { return Tasks.forResult(null); }
}
