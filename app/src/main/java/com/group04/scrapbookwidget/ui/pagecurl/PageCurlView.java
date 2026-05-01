package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.ui.scrapbookview.ScrapbookPageData;

import java.util.ArrayList;
import java.util.List;

public class PageCurlView extends GLSurfaceView {
    class PhotoRect {
        public String pageId, itemId;
        public Rect rect;
        public PhotoRect(String pageId, String itemId, Rect rect) {
            this.pageId = pageId;
            this.itemId = itemId;
            this.rect = rect;
        }
    }

    public interface OnPhotoHitListener {
        void onPhotoHit(String pageId, String itemId);
    }
    public interface OnPageChangedListener {
        void onPageChanged(int newPageIndex);
    }
    public interface OnSwipeToNewPageListener {
        void onSwipeToNewPage();
    }
    private OnPageChangedListener pageChangedListener;
    private OnSwipeToNewPageListener swipeToNewPageListener;

    public void setOnPageChangedListener(OnPageChangedListener listener) {
        this.pageChangedListener = listener;
    }
    public void setOnSwipeToNewPageListener(OnSwipeToNewPageListener listener) {
        this.swipeToNewPageListener = listener;
    }
    private final Context _context;
    private PageRenderer pageRenderer;
    private final float CURL_THRESHOLD = 0.5f;
    private int curPage = 1;
    private boolean isCurling = false;
    private boolean isForward = false;

    private boolean isPressed = false;

    private List<List<PhotoRect>> photoRects;
    private OnPhotoHitListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    public PageCurlView(Context context, boolean isEffectEnabled) {
        super(context);
        _context = context;
        setEGLContextClientVersion(3);
        pageRenderer = new PageRenderer(context, isEffectEnabled);
        setRenderer(pageRenderer);
    }

    public PageRenderer getPageRenderer() {
        return pageRenderer;
    }

    public void togglePageCurlEffect(boolean isEnabled) {
        pageRenderer.setIsEffectEnabled(isEnabled);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float normX = x / getWidth();
        float normY = y / getHeight();

        int numPages = pageRenderer.getPageNums();

        int touchDelayMillis = 1000;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                PhotoRect photoRect = getPhotoHit(x, y);
                if (!isCurling && photoRect != null) {
                    runnable = () -> {
                        if (listener != null) {
                            listener.onPhotoHit(photoRect.pageId, photoRect.itemId);
                        };
                    };
                    handler.postDelayed(runnable, touchDelayMillis);
                    isPressed = true;
                }

                isForward = normX >= CURL_THRESHOLD;
                if (!isForward && curPage == 1) {
                    isCurling = false;
                    return true;
                }

                if (isForward && curPage == numPages) {
                    if (swipeToNewPageListener != null) {
                        swipeToNewPageListener.onSwipeToNewPage();
                    }
                    isCurling = false;
                    return true;
                }

                isCurling = true;
                pageRenderer.setIsForward(isForward);
                pageRenderer.setStartPos(normX, normY);
                pageRenderer.setCurPos(normX, normY);
                requestRender();
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isCurling) return true;

                if (isPressed && runnable != null) {
                    isPressed = false;
                    handler.removeCallbacks(runnable);
                }

                pageRenderer.setCurPos(normX, normY);
                requestRender();
                break;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    handler.removeCallbacks(runnable);
                }

                if (!isCurling) return true;

                int prevPage = curPage;
                if (isForward && normX < CURL_THRESHOLD) {
                    curPage++;
                }
                else if (!isForward && normX >= CURL_THRESHOLD) {
                    curPage--;
                }
                if (prevPage != curPage) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(_context, R.raw.page_turn_sound);
                    mediaPlayer.start();
                    
                    if (pageChangedListener != null) {
                        pageChangedListener.onPageChanged(curPage - 1); 
                    }
                }

                isCurling = false;
                pageRenderer.setStartPos(-1.0f, -1.0f);
                pageRenderer.setCurPos(-1.0f, -1.0f);
                pageRenderer.setCurPage(curPage);
                requestRender();
                break;
        }

        return true;
    }

    public void setOnPhotoHitListener(OnPhotoHitListener listener) {
        this.listener = listener;
    }
    public void setPhotoRects(List<ScrapbookPageData> pages) {
        photoRects = new ArrayList<>();
        if (pages == null) return;
        for (var page: pages) {
            List<PhotoRect> pagePhotoRects = new ArrayList<>();
            if (page.scrapbookItems != null) {
                for (var item: page.scrapbookItems) {
                    Layout layout = item.getLayout();
                    Rect rect = new Rect(
                            Math.round(layout.x),
                            Math.round(layout.y),
                            Math.round(layout.x + layout.width),
                            Math.round(layout.y + layout.height)
                    );
                    pagePhotoRects.add(new PhotoRect(page.scrapbookPage.getId(), item.getId(), rect));
                }
            }
            photoRects.add(pagePhotoRects);
        }
    }

    public void setCurPage(int page) {
        curPage = page + 1;
        pageRenderer.setCurPage(curPage);
    }

    private PhotoRect getPhotoHit(float x, float y) {
        if (photoRects != null && curPage > 0 && curPage <= photoRects.size()) {
            float xScale = x * pageRenderer.getBmpW() / getWidth();
            float yScale = y * pageRenderer.getBmpH() / getHeight();
            List<PhotoRect> photos = photoRects.get(curPage - 1);
            for (var photo: photos) {
                if (photo.rect.contains(Math.round(xScale), Math.round(yScale))) {
                    return photo;
                }
            }
        }
        return null;
    }
}