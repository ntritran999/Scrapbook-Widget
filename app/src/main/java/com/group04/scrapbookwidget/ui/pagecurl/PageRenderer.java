package com.group04.scrapbookwidget.ui.pagecurl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.group04.scrapbookwidget.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PageRenderer implements GLSurfaceView.Renderer {
    private Context _context;
    private CurlMesh curlMesh;
    private float startX = -1.0f, startY = -1.0f, curX = -1.0f, curY = -1.0f;
    private int curPage = 2;
    private boolean isForward = false;
    private final int backTexDefault = 0;
    private int[] bitmapIds = {
            R.drawable.test__1_,
            R.drawable.test__2_,
            R.drawable.test__3_,
            R.drawable.test__4_,

    };
    private int[] textures;
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
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);
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

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES32.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        curlMesh = new CurlMesh();

        textures = new int[bitmapIds.length];
        GLES32.glGenTextures(bitmapIds.length, textures, 0);
        for (int i = 0; i < bitmapIds.length; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(_context.getResources(), bitmapIds[i]);
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textures[i]);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);
            GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
            GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);
            bitmap.recycle();
        }
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES32.glCreateShader(type);

        GLES32.glShaderSource(shader, shaderCode);
        GLES32.glCompileShader(shader);

        return shader;
    }
}
