package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.group04.scrapbookwidget.R;

public class PageCurlView extends GLSurfaceView {
    private final Context _context;
    private PageRenderer pageRenderer;
    private final float CURL_THRESHOLD = 0.5f;
    private int curPage = 1;
    private boolean isCurling = false;
    private boolean isForward = false;

    private boolean isPressed = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable;
    public PageCurlView(Context context, Runnable runnable) {
        super(context);
        _context = context;
        this.runnable = runnable;
        setEGLContextClientVersion(3);
        pageRenderer = new PageRenderer(context);
        setRenderer(pageRenderer);
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
                if (!isCurling && isPhotoHit(x, y)) {
                    handler.postDelayed(runnable, touchDelayMillis);
                    isPressed = true;
                }

                isForward = normX >= CURL_THRESHOLD;
                if ((isForward && curPage == numPages) || (!isForward && curPage == 1)) {
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

                if (isPressed) {
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
                if (!pageRenderer.getIsDeveloping()) {
                    if (isForward && normX < CURL_THRESHOLD) {
                        curPage++;
                    }
                    else if (!isForward && normX >= CURL_THRESHOLD) {
                        curPage--;
                    }
                }
                if (prevPage != curPage) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(_context, R.raw.page_turn_sound);
                    mediaPlayer.start();
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

    private RectF dummyRect = new RectF(35, 35, 250, 450);
    private boolean isPhotoHit(float x, float y) {
        return dummyRect.contains(x, y);
    }
}