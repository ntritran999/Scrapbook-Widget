package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.ui.meshes.CurlMesh;
import com.group04.scrapbookwidget.ui.meshes.SimpleMesh;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PageRenderer implements GLSurfaceView.Renderer {
    private Context _context;
    private CurlMesh curlMesh;
    private SimpleMesh flatMesh;
    private float startX = -1.0f, startY = -1.0f, curX = -1.0f, curY = -1.0f;
    private int bmpW, bmpH;
    private int curPage = 1;
    private boolean isForward = false;
    private long startTime;
    private boolean isDeveloping = false;
    private boolean isLoaded = false;
    private final int DEVELOPING_SECONDS = 10;
    private List<float[]> transformMatrices;
    private PageResources pageResources;
    private int[] textures;
    private int[] backTex;
    private int[] developingEffectTextures;
    public PageRenderer(Context context) {
        _context = context;
    }

    public void setStartPos(float x, float y) {
        startX = x; startY = y;
    }
    public void setCurPos(float x, float y) {
        curX = x; curY = y;
    }

    public void setCurPage(int p) {
        curPage = p;
    }

    public void setIsForward(boolean forward) {
        isForward = forward;
    }

    public int getPageNums() {
        if (pageResources == null) {
            return 0;
        }
        return pageResources.pageBitmaps.size();
    }
    public boolean getIsDeveloping() {
        return isDeveloping;
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
        if (!isLoaded) return;;

        if (isDeveloping) {
            float curTime = (System.currentTimeMillis() - startTime) / 1000.0f;
            if (curTime >= DEVELOPING_SECONDS) {
                isDeveloping = false;
                curTime = DEVELOPING_SECONDS;
            }

            float progress = curTime / DEVELOPING_SECONDS;
            for (int i = 0; i < developingEffectTextures.length; i++) {
                if (i == 0) {
                    flatMesh.draw(developingEffectTextures[i], transformMatrices.get(i), 1.0f);
                }
                else {
                    flatMesh.draw(developingEffectTextures[i], transformMatrices.get(i), progress);
                }

            }
        }
        else {
            int front = textures[curPage - 1];
            int next = front;

            if (startX > 0.0f) {
                if (isForward && curPage < pageResources.pageBitmaps.size()) {
                    next = textures[curPage];
                }
                else if (!isForward && curPage >= 2) {
                    front = textures[curPage - 2];
                    next = textures[curPage - 1];
                }
            }
            curlMesh.draw(startX, startY, curX, curY, front, backTex[0], next);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
        if (pageResources != null) {
            prepareTransform();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        startTime = System.currentTimeMillis();
        GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        curlMesh = new CurlMesh();
        flatMesh = new SimpleMesh();
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES32.glCreateShader(type);

        GLES32.glShaderSource(shader, shaderCode);
        GLES32.glCompileShader(shader);

        return shader;
    }

    public void updatePageResources(PageResources pageResources) {
        this.pageResources = pageResources;
        textures = new int[pageResources.pageBitmaps.size()];
        GLES32.glGenTextures(textures.length, textures, 0);
        for (int i = 0; i < textures.length; i++) {
            loadTex(pageResources.pageBitmaps.get(i), textures[i]);
        }

        backTex = new int[1];
        GLES32.glGenTextures(1, backTex, 0);
        loadTex(pageResources.backgroundBitmap, backTex[0]);

        loadPhotoDevelopingEffectTex();

        pageResources.backgroundBitmap.recycle();
        for (var bitmap: pageResources.developingPhotosBitmaps) {
            bitmap.recycle();
        }
        for (var bitmap: pageResources.pageBitmaps) {
            bitmap.recycle();
        }

        isLoaded = true;
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

    private void loadPhotoDevelopingEffectTex() {
        transformMatrices = new ArrayList<>();

        int num_photos_on_cur_page = pageResources.developingPhotosBitmaps.size();

        developingEffectTextures = new int[num_photos_on_cur_page + 1];
        GLES32.glGenTextures(developingEffectTextures.length, developingEffectTextures, 0);

        loadTex(pageResources.backgroundBitmap, developingEffectTextures[0]);
        for (int i = 1; i < developingEffectTextures.length; i++) {
            loadTex(pageResources.developingPhotosBitmaps.get(i - 1), developingEffectTextures[i]);
        }

        prepareTransform();
        isDeveloping = true;
    }

    private void prepareTransform() {
        transformMatrices.clear();
        float[] bg_matrix = new float[16];
        Matrix.setIdentityM(bg_matrix, 0);
        transformMatrices.add(bg_matrix);

        for (var rect: pageResources.imageRects) {
            float[] img_matrix = new float[16];
            prepareTransformMatrix(img_matrix, rect);
            transformMatrices.add(img_matrix);
        }
    }

    private void prepareTransformMatrix(float[] matrix, Rect img) {
        Matrix.setIdentityM(matrix, 0);

        bmpW = pageResources.bitmapWidth;
        bmpH = pageResources.bitmapHeight;

        float cx = (img.left + img.width() * 0.5f) / bmpW;
        float cy = (img.top + img.height() * 0.5f) / bmpH;

        float x = cx * 2f - 1f;
        float y = 1f - cy * 2f;

        float sw = img.width() * 1.0f / bmpW;
        float sh = img.height() * 1.0f / bmpH;

        Matrix.translateM(matrix, 0, x, y, 0.0f);
        Matrix.scaleM(matrix, 0, sw, sh, 1.0f);
    }
}
