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
    private int screen_width, screen_height;
    private float startX = -1.0f, startY = -1.0f, curX = -1.0f, curY = -1.0f;
    private int curPage = 2;
    private boolean isForward = false;
    private long startTime;
    private boolean isDeveloping = false;
    private final int DEVELOPING_SECONDS = 10;
    private List<float[]> transformMatrices;
    private final int backTexDefault = 0;
    private int[] bitmapIds = {
            R.drawable.test__1_,
            R.drawable.test__5_,
            R.drawable.test__3_,
            R.drawable.test__4_,

    };
    private int[] textures;
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
        curPage = p + 1;
    }

    public void setIsForward(boolean forward) {
        isForward = forward;
    }

    public int getPageNums() {
        return bitmapIds.length - 1;
    }
    public boolean getIsDeveloping() {
        return isDeveloping;
    }
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

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
                if (isForward && curPage < bitmapIds.length) {
                    next = textures[curPage];
                }
                else if (!isForward && curPage > 2) {
                    front = textures[curPage - 2];
                    next = textures[curPage - 1];
                }
            }
            curlMesh.draw(startX, startY, curX, curY, front, textures[backTexDefault], next);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
        screen_width = width;
        screen_height = height;


        // Test code
        float[] bg_matrix = new float[16];
        Matrix.setIdentityM(bg_matrix, 0);
        transformMatrices.add(bg_matrix);

        Rect dummyRect = new Rect(35, 35, 250, 450);
        float[] img_matrix = new float[16];
        prepareTransformMatrix(img_matrix, dummyRect);
        transformMatrices.add(img_matrix);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        startTime = System.currentTimeMillis();
        GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        curlMesh = new CurlMesh();
        flatMesh = new SimpleMesh();

        textures = new int[bitmapIds.length];
        GLES32.glGenTextures(bitmapIds.length, textures, 0);
        for (int i = 0; i < bitmapIds.length; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(_context.getResources(), bitmapIds[i]);
            loadTex(bitmap, textures[i]);
        }

        loadPhotoDevelopingEffectTex();
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES32.glCreateShader(type);

        GLES32.glShaderSource(shader, shaderCode);
        GLES32.glCompileShader(shader);

        return shader;
    }

    private void loadTex(Bitmap bitmap, int texId) {
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texId);
        setTexParam();
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);
        bitmap.recycle();
    }
    private void setTexParam() {
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
    }

    private void loadPhotoDevelopingEffectTex() {
        transformMatrices = new ArrayList<>();

        Bitmap dummyBackground = BitmapFactory.decodeResource(_context.getResources(), bitmapIds[1]);
        Rect dummyRect = new Rect(35, 35, 250, 450);
        Bitmap dummyImage = BitmapFactory.decodeResource(_context.getResources(), bitmapIds[3]);

        int num_photos_on_cur_page = 1;

        developingEffectTextures = new int[num_photos_on_cur_page + 1];
        GLES32.glGenTextures(developingEffectTextures.length, developingEffectTextures, 0);

        loadTex(dummyBackground, developingEffectTextures[0]);
        loadTex(dummyImage, developingEffectTextures[1]);

        isDeveloping = true;
    }

    private void prepareTransformMatrix(float[] matrix, Rect img) {
        Matrix.setIdentityM(matrix, 0);

        float cx = (img.left + img.width() * 0.5f) / screen_width;
        float cy = (img.top + img.height() * 0.5f) / screen_height;

        float x = cx * 2f - 1f;
        float y = 1f - cy * 2f;

        float sw = img.width() * 1.0f / screen_width;
        float sh = img.height() * 1.0f / screen_height;

        Matrix.translateM(matrix, 0, x, y, 0.0f);
        Matrix.scaleM(matrix, 0, sw, sh, 1.0f);
    }
}
