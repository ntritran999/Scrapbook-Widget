package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class PageCurlView extends GLSurfaceView {
    private PageRenderer pageRenderer;
    private final float CURL_THRESHOLD = 0.5f;
    private int curPage = 1;
    private boolean isCurling = false;
    private boolean isForward = false;
    public PageCurlView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        pageRenderer = new PageRenderer(context);
        setRenderer(pageRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float normX = x / getWidth();
        float normY = y / getHeight();

        int numPages = pageRenderer.getPageNums();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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

                pageRenderer.setCurPos(normX, normY);
                requestRender();
                break;

            case MotionEvent.ACTION_UP:
                if (!isCurling) return true;

                if (isForward && normX < CURL_THRESHOLD) {
                    curPage++;
                }
                else if (!isForward && normX >= CURL_THRESHOLD) {
                    curPage--;
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
}