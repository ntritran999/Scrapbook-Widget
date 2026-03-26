package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.ui.scrapbookview.ScrapbookPageData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PageBuilder {
    public static PageResources buildPages(Context context, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        PageResources pageResources = new PageResources();

        if (pagesData != null && !pagesData.isEmpty()) {
            buildBackground(context, pageResources, pagesData.get(0).scrapbookPage);
            buildPagesBitmaps(context, pageResources, pagesData);
        }
        return pageResources;
    }

    private static void buildBackground(Context context, PageResources pageResources, ScrapbookPage page) throws ExecutionException, InterruptedException {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        pageResources.backgroundBitmap = Glide.with(context)
                .asBitmap()
                .load(page.getBackgroundImage())
                .override(displayMetrics.widthPixels, displayMetrics.heightPixels)
                .centerCrop()
                .submit()
                .get();

        pageResources.bitmapWidth = pageResources.backgroundBitmap.getWidth();
        pageResources.bitmapHeight = pageResources.backgroundBitmap.getHeight();
    }
    private static void buildPagesBitmaps(Context context, PageResources pageResources, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        pageResources.pageBitmaps = new ArrayList<>();
        for (var pageData: pagesData) {
            if (pageResources.backgroundBitmap.isRecycled()) {
                buildBackground(context, pageResources, pagesData.get(0).scrapbookPage);
            }
            Bitmap background = pageResources.backgroundBitmap;
            Bitmap result = Bitmap.createBitmap(
                    background.getWidth(),
                    background.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(background, 0, 0, null);
            for (var item: pageData.scrapbookItems) {
                Layout layout = item.getLayout();
                Bitmap photo = getBitMapFromUrl(context, item.getContent().photoUrl, (int) layout.width, (int) layout.height);
                Rect rect = getRectFromLayout(layout);

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

    private static Bitmap getBitMapFromUrl(Context context, String url, int w, int h) throws ExecutionException, InterruptedException {
        return Glide.with(context)
                .asBitmap()
                .load(url)
                .override(w, h)
                .centerCrop()
                .submit()
                .get();
    }
}
