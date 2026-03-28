package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
    public static PageResources buildPages(Context context, List<ScrapbookPageData> pagesData, int pageNum) throws ExecutionException, InterruptedException {
        android.util.Log.d("PageBuilder", "buildPages: Starting build for pageNum=" + pageNum + ", totalPages=" + pagesData.size());
        
        PageResources pageResources = new PageResources();
        bitmapCache = new HashMap<>();

        ScrapbookPageData developingPageData = pagesData.get(pageNum);
        
        android.util.Log.d("PageBuilder", "buildPages: Building background");
        buildBackground(context, pageResources, developingPageData.scrapbookPage);
        
        android.util.Log.d("PageBuilder", "buildPages: Building developing items, count=" + (developingPageData.scrapbookItems != null ? developingPageData.scrapbookItems.size() : 0));
        buildDevelopingItems(context, pageResources, developingPageData.scrapbookItems);

        android.util.Log.d("PageBuilder", "buildPages: Building all pages bitmaps");
        buildPagesBitmaps(context, pageResources, pagesData);
        
        android.util.Log.d("PageBuilder", "buildPages: Completed successfully");
        return pageResources;
    }

    private static void buildBackground(Context context, PageResources pageResources, ScrapbookPage page) throws ExecutionException, InterruptedException {
        android.util.Log.d("PageBuilder", "buildBackground: Loading background image: " + page.getBackgroundImage());
        
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int targetWidth = 1080;
        int targetHeight = (int) (targetWidth * ((float) displayMetrics.heightPixels / displayMetrics.widthPixels));
        pageResources.backgroundBitmap = Glide.with(context)
                .asBitmap()
                .load(page.getBackgroundImage())
                .skipMemoryCache(true)
                .override(targetWidth, targetHeight)
                .centerCrop()
                .submit()
                .get();

        pageResources.bitmapWidth = pageResources.backgroundBitmap.getWidth();
        pageResources.bitmapHeight = pageResources.backgroundBitmap.getHeight();
        
        android.util.Log.d("PageBuilder", "buildBackground: Loaded, size=" + pageResources.bitmapWidth + "x" + pageResources.bitmapHeight);
    }

    private static void buildDevelopingItems(Context context, PageResources pageResources, List<ScrapbookItem> items) throws ExecutionException, InterruptedException {
        pageResources.developingPhotosBitmaps = new ArrayList<>();
        pageResources.imageRects = new ArrayList<>();
        
        if (items == null || items.isEmpty()) {
            android.util.Log.d("PageBuilder", "buildDevelopingItems: No items to build");
            return;
        }
        
        android.util.Log.d("PageBuilder", "buildDevelopingItems: Processing " + items.size() + " items");
        for (var item: items) {
            Layout layout = item.getLayout();
            String photoUrl = item.getContent().photoUrl;
            
            android.util.Log.d("PageBuilder", "  Processing item: id=" + item.getId() + ", photoUrl=" + photoUrl);
            
            if (photoUrl == null || photoUrl.isEmpty()) {
                android.util.Log.w("PageBuilder", "  WARNING: photoUrl is null or empty for item " + item.getId());
                continue;
            }
            
            Bitmap bm = getBitMapFromUrl(context, photoUrl, (int) layout.width, (int) layout.height);
            if (bm == null) {
                android.util.Log.e("PageBuilder", "  ERROR: Failed to load bitmap for item " + item.getId());
                continue;
            }
            
            pageResources.developingPhotosBitmaps.add(bm);
            bitmapCache.put(item.getId(), bm);

            pageResources.imageRects.add(getRectFromLayout(layout));
            android.util.Log.d("PageBuilder", "  Added item to cache, bitmap size=" + bm.getWidth() + "x" + bm.getHeight());
        }
        
        android.util.Log.d("PageBuilder", "buildDevelopingItems: Completed, loaded " + pageResources.developingPhotosBitmaps.size() + " photos");
    }

    private static void buildPagesBitmaps(Context context, PageResources pageResources, List<ScrapbookPageData> pagesData) throws ExecutionException, InterruptedException {
        pageResources.pageBitmaps = new ArrayList<>();
        
        android.util.Log.d("PageBuilder", "buildPagesBitmaps: Processing " + pagesData.size() + " pages");
        
        for (var pageData: pagesData) {
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
        return Glide.with(context)
                .asBitmap()
                .load(url)
                .skipMemoryCache(true)
                .override(w, h)
                .centerCrop()
                .submit()
                .get();
    }
}
