package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.group04.scrapbookwidget.ui.meshes.CurlMesh;
import com.group04.scrapbookwidget.ui.meshes.SimpleMesh;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PageRenderer implements GLSurfaceView.Renderer {
    private Context _context;
    private boolean isEffectEnabled;
    private CurlMesh curlMesh;
    private SimpleMesh simpleMesh;
    private float startX = -1.0f, startY = -1.0f, curX = -1.0f, curY = -1.0f;
    private int bmpW, bmpH;
    private int curPage = 1;
    private boolean isForward = false;
    private boolean isLoaded = false;
    private PageResources pageResources;
    private int[] textures;
    private int[] backTex;
    public PageRenderer(Context context, boolean isEffectEnabled) {
        _context = context;
        this.isEffectEnabled = isEffectEnabled;
    }

    public void setStartPos(float x, float y) {
        startX = x; startY = y;
    }
    public void setCurPos(float x, float y) {
        curX = x; curY = y;
    }

    public void setCurPage(int p) {
        curPage = Math.max(1, p);
    }

    public void setIsForward(boolean forward) {
        isForward = forward;
    }

    public void setIsEffectEnabled(boolean isEffectEnabled) {
        this.isEffectEnabled = isEffectEnabled;
    }

    public int getPageNums() {
        if (pageResources == null) {
            return 0;
        }
        return pageResources.pageBitmaps.size();
    }
    public int getBmpW() {
        return bmpW;
    }

    public int getBmpH() {
        return bmpH;
    }
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);
        if (!isLoaded || pageResources == null || textures == null || textures.length == 0 || backTex == null || backTex.length == 0) {
            return;
        }

        int pageCount = pageResources.pageBitmaps != null ? pageResources.pageBitmaps.size() : 0;
        if (pageCount <= 0) {
            return;
        }

        curPage = Math.max(1, Math.min(curPage, pageCount));

        int front = textures[curPage - 1];
        int next = front;

        if (isEffectEnabled) {
            if (startX > 0.0f) {
                if (isForward && curPage < pageCount && curPage < textures.length) {
                    next = textures[curPage];
                }
                else if (!isForward && curPage >= 2) {
                    front = textures[curPage - 2];
                    next = textures[curPage - 1];
                }
            }
            curlMesh.draw(startX, startY, curX, curY, front, backTex[0], next);
        }
        else {
            simpleMesh.draw(front);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA);

        curlMesh = new CurlMesh();
        simpleMesh = new SimpleMesh();
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES32.glCreateShader(type);

        GLES32.glShaderSource(shader, shaderCode);
        GLES32.glCompileShader(shader);

        return shader;
    }

    public void updatePageResources(PageResources pageResources) {
        if (pageResources == null || pageResources.pageBitmaps == null || pageResources.pageBitmaps.isEmpty()
                || pageResources.backgroundBitmap == null) {
            isLoaded = false;
            this.pageResources = pageResources;
            textures = null;
            backTex = null;
            android.util.Log.w("PageRenderer", "updatePageResources: Skipped because page resources are incomplete");
            return;
        }

        this.pageResources = pageResources;
        textures = new int[pageResources.pageBitmaps.size()];
        GLES32.glGenTextures(textures.length, textures, 0);
        for (int i = 0; i < textures.length; i++) {
            loadTex(pageResources.pageBitmaps.get(i), textures[i]);
        }

        backTex = new int[1];
        GLES32.glGenTextures(1, backTex, 0);
        loadTex(pageResources.backgroundBitmap, backTex[0]);

        bmpW = pageResources.bitmapWidth;
        bmpH = pageResources.bitmapHeight;
        curPage = Math.max(1, Math.min(curPage, textures.length));

        if (!pageResources.backgroundBitmap.isRecycled()) {
            pageResources.backgroundBitmap.recycle();
        }
        for (var bitmap: pageResources.pageBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }

        isLoaded = true;
        android.util.Log.d("PageRenderer", "updatePageResources: Completed, isLoaded=true");
    }

    private void loadTex(Bitmap bitmap, int texId) {
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texId);
        setTexParam();
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);
    }
    private void setTexParam() {
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
    }
}
