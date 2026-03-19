package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.ui.scrapbookview.ScrapbookPageData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PageBuilder {
    public static PageResources buildPages(Context context, List<ScrapbookPageData> pagesData, int pageNum) throws ExecutionException, InterruptedException {
        PageResources pageResources = new PageResources();

        ScrapbookPageData developingPageData = pagesData.get(pageNum);
        buildBackground(context, pageResources, developingPageData.scrapbookPage);
        buildDevelopingItems(context, pageResources, developingPageData.scrapbookItems);

        buildPagesBitmaps(context, pageResources, pagesData);
        return pageResources;
    }

    private static void buildBackground(Context context, PageResources pageResources, ScrapbookPage page) throws ExecutionException, InterruptedException {
        pageResources.backgroundBitmap = Glide.with(context).asBitmap().load(page.getBackgroundImage()).submit().get();

        pageResources.bitmapWidth = pageResources.backgroundBitmap.getWidth();
        pageResources.bitmapHeight = pageResources.backgroundBitmap.getHeight();
    }

    private static void buildDevelopingItems(Context context, PageResources pageResources, List<ScrapbookItem> items) throws ExecutionException, InterruptedException {
        pageResources.developingPhotosBitmaps = new ArrayList<>();
        pageResources.imageRects = new ArrayList<>();
        for (var item: items) {
            pageResources.developingPhotosBitmaps.add(getBitMapFromUrl(context, item.getContent().photoUrl));

            pageResources.imageRects.add(getRectFromLayout(item.getLayout()));
        }
    }

    private static void buildPagesBitmaps(Context context, PageResources pageResources, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        pageResources.pageBitmaps = new ArrayList<>();
        for (var pageData: pagesData) {
            Bitmap background = pageResources.backgroundBitmap;
            Bitmap result = Bitmap.createBitmap(
                    background.getWidth(),
                    background.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(background, 0, 0, null);
            for (var item: pageData.scrapbookItems) {
                Bitmap photo = getBitMapFromUrl(context, item.getContent().photoUrl);
                Rect rect = getRectFromLayout(item.getLayout());

                canvas.drawBitmap(photo, null, rect, null);
            }

            pageResources.pageBitmaps.add(result);
        }
    }

    private static Rect getRectFromLayout(Layout layout) {
        return new Rect(
                Math.round(layout.x),
                Math.round(layout.y),
                Math.round(layout.x + layout.width),
                Math.round(layout.y + layout.height)
        );
    }

    private static Bitmap getBitMapFromUrl(Context context, String url) throws ExecutionException, InterruptedException {
        return Glide.with(context).asBitmap().load(url).submit().get();
    }
}
