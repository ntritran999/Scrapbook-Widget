package com.group04.scrapbookwidget.ui.scrapbookview;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

public class ScrapbookPageData {
    public ScrapbookPage scrapbookPage;
    public List<ScrapbookItem> scrapbookItems;

    public ScrapbookPageData(ScrapbookPage page, List<ScrapbookItem> items) {
        scrapbookPage = page;
        scrapbookItems = items;
    }
}
