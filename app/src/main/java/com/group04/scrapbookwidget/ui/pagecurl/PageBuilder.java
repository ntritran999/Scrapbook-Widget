package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.ui.scrapbookview.ScrapbookPageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PageBuilder {
    private static Map<String, Bitmap> bitmapCache;
    public static PageResources buildPages(Context context, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        PageResources pageResources = new PageResources();

        if (pagesData != null && !pagesData.isEmpty()) {
            bitmapCache = new HashMap<>();
            buildBackground(context, pageResources, pagesData.get(0).scrapbookPage);
            buildPagesBitmaps(context, pageResources, pagesData);
        }
        return pageResources;
    }

    private static void buildBackground(Context context, PageResources pageResources, ScrapbookPage page) throws ExecutionException, InterruptedException {
        android.util.Log.d("PageBuilder", "buildBackground: Loading background image: " + page.getBackgroundImage());

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int targetWidth = 1080;
        int targetHeight = (int) (targetWidth * ((float) displayMetrics.heightPixels / displayMetrics.widthPixels));
        
        String bgImage = page.getBackgroundImage();
        if (bgImage != null && !bgImage.isEmpty()) {
            try {
                pageResources.backgroundBitmap = Glide.with(context)
                        .asBitmap()
                        .load(bgImage)
                        .skipMemoryCache(true)
                        .override(targetWidth, targetHeight)
                        .centerCrop()
                        .submit()
                        .get();
            } catch (Exception e) {
                android.util.Log.e("PageBuilder", "Failed to load background image: " + bgImage, e);
                pageResources.backgroundBitmap = createSolidColorBitmap(targetWidth, targetHeight, page.getBackgroundColor());
            }
        } else {
            pageResources.backgroundBitmap = createSolidColorBitmap(targetWidth, targetHeight, page.getBackgroundColor());
        }

        pageResources.bitmapWidth = pageResources.backgroundBitmap.getWidth();
        pageResources.bitmapHeight = pageResources.backgroundBitmap.getHeight();

        android.util.Log.d("PageBuilder", "buildBackground: Loaded, size=" + pageResources.bitmapWidth + "x" + pageResources.bitmapHeight);
    }

    private static Bitmap createSolidColorBitmap(int width, int height, String colorHex) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int color = Color.WHITE;
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                color = Color.parseColor(colorHex);
            } catch (Exception e) {
                android.util.Log.e("PageBuilder", "Failed to parse color: " + colorHex, e);
            }
        }
        bitmap.eraseColor(color);
        return bitmap;
    }

    private static void buildPagesBitmaps(Context context, PageResources pageResources, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        pageResources.pageBitmaps = new ArrayList<>();

        android.util.Log.d("PageBuilder", "buildPagesBitmaps: Processing " + pagesData.size() + " pages");

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

            int itemsDrawn = 0;
            for (var item: pageData.scrapbookItems) {
                Layout layout = item.getLayout();
                Bitmap photo = bitmapCache.get(item.getId());

                if (photo == null) {
                    String photoUrl = item.getContent().photoUrl;
                    android.util.Log.d("PageBuilder", "  Photo not in cache, loading from URL: " + photoUrl);
                    photo = getBitMapFromUrl(context, photoUrl, (int) layout.width, (int) layout.height);
                    if (photo != null) {
                        bitmapCache.put(item.getId(), photo);
                    }
                }

                if (photo != null) {
                    Rect rect = getRectFromLayout(layout);
                    canvas.drawBitmap(photo, null, rect, null);
                    itemsDrawn++;
                    android.util.Log.d("PageBuilder", "  Drew item at rect: " + rect);
                } else {
                    android.util.Log.w("PageBuilder", "  Failed to draw item, photo is null");
                }
            }

            pageResources.pageBitmaps.add(result);
            android.util.Log.d("PageBuilder", "  Page page completed, drew " + itemsDrawn + " items");
        }

        android.util.Log.d("PageBuilder", "buildPagesBitmaps: Completed, created " + pageResources.pageBitmaps.size() + " page bitmaps");
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
        if (url == null || url.isEmpty()) return null;
        int size = Math.max(w, h);
        try {
            return Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .skipMemoryCache(true)
                    .override(size, size)
                    .submit()
                    .get();
        } catch (Exception e) {
            android.util.Log.e("PageBuilder", "Failed to load image from URL: " + url, e);
            return null;
        }
    }
}
