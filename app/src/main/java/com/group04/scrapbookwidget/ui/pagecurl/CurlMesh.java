package com.group04.scrapbookwidget.ui.pagecurl;

import android.opengl.GLES32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CurlMesh {
    private final short COORDS_PER_VERTEX = 5;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private float meshCoords[] = {
            -1.0f, 1.0f, 0.0f, 0.0f, 0.0f, // x,y,z,u,v
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
    };
    private FloatBuffer vertexBuffer;
    private final short[] drawOrder = {0, 2, 3, 0, 3, 1};
    private ShortBuffer drawListBuffer;

    private final String vertexShaderCode =
            "#version 300 es\n" +
                    "layout (location = 0) in vec3 aPos;\n" +
                    "layout (location = 1) in vec2 aTexCoord;\n" +
                    "\n" +
                    "out vec2 TexCoord;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = vec4(aPos, 1.0);\n" +
                    "    TexCoord = aTexCoord;\n" +
                    "}";
    private final String fragmentShaderCode =
            "#version 300 es\n" +
                    "precision highp float;\n" +
                    "out vec4 FragColor;\n" +
                    "in vec2 TexCoord;\n" +
                    "uniform sampler2D frontTexture;\n" +
                    "uniform sampler2D backTexture;\n" +
                    "uniform sampler2D nextTexture;\n" +
                    "uniform vec2 uStart;\n" +
                    "uniform vec2 uCur;\n" +
                    "\n" +
                    "#define pi 3.14159265359\n" +
                    "#define radius 0.1\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = TexCoord;\n" +
                    "    float threshold = 0.5;" +
                    "    \n" +
                    "    if (uStart.x < 0.0 || distance(uStart, uCur) < 0.01) {\n" +
                    "        FragColor = (uStart.x >= 0.0 && uStart.x < threshold) ? texture(nextTexture, uv) : texture(frontTexture, uv);\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    \n" +
                    "    vec2 dir = normalize(uStart - uCur);\n" +
                    "    if (uStart.x < threshold) dir = -dir;\n" +
                    "    \n" +
                    "    vec2 origin = clamp(uCur - dir * (uCur.x / dir.x), 0.0, 1.0);\n" +
                    "    \n" +
                    "    float dragDist = distance(uCur, origin);\n" +
                    "    float proj = dot(uv - origin, dir);\n" +
                    "    float dist = proj - dragDist;\n" +
                    "    vec2 linePoint = uv - dist * dir;\n" +
                    "    \n" +
                    "    if (dist > radius) {\n" +
                    "        FragColor = texture(nextTexture, uv);\n" +
                    "    } else if (dist >= 0.0) {\n" +
                    "        float theta = asin(dist / radius);\n" +
                    "        vec2 p2 = linePoint + dir * (pi - theta) * radius;\n" +
                    "        vec2 p1 = linePoint + dir * theta * radius;\n" +
                    "        uv = (p2.x <= 1.0 && p2.y <= 1.0 && p2.x >= 0.0 && p2.y >= 0.0) ? p2 : p1;\n" +
                    "        if (uv == p2) {\n" +
                    "           FragColor = texture(backTexture, uv);\n" +
                    "        }\n" +
                    "        else {\n" +
                    "           FragColor = texture(frontTexture, uv);\n" +
                    "        }\n" +
                    "    } else {\n" +
                    "        vec2 p = linePoint + dir * (abs(dist) + pi * radius);\n" +
                    "        uv = (p.x <= 1.0 && p.y <= 1.0 && p.x >= 0.0 && p.y >= 0.0) ? p : uv;\n" +
                    "        if (uv == p) {\n" +
                    "           FragColor = texture(backTexture, uv);\n" +
                    "        }\n" +
                    "        else {\n" +
                    "           FragColor = texture(frontTexture, uv);\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

    private final int program;
    private final int[] VBO;
    private final int[] VAO;
    private final int[] EBO;

    public CurlMesh() {
        ByteBuffer bb = ByteBuffer.allocateDirect(meshCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(meshCoords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = PageRenderer.loadShader(GLES32.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = PageRenderer.loadShader(GLES32.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        program = GLES32.glCreateProgram();
        GLES32.glAttachShader(program, vertexShader);
        GLES32.glAttachShader(program, fragmentShader);
        GLES32.glLinkProgram(program);

        VBO = new int[1];
        VAO = new int[1];
        EBO = new int[1];

        GLES32.glGenBuffers(1, VBO, 0);
        GLES32.glGenVertexArrays(1, VAO, 0);
        GLES32.glGenBuffers(1, EBO, 0);

        GLES32.glBindVertexArray(VAO[0]);

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, VBO[0]);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, meshCoords.length * 4, vertexBuffer, GLES32.GL_STATIC_DRAW);

        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, vertexStride, 0);
        GLES32.glEnableVertexAttribArray(0);

        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, EBO[0]);
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, drawOrder.length * 2, drawListBuffer, GLES32.GL_STATIC_DRAW);

        GLES32.glVertexAttribPointer(1, 2, GLES32.GL_FLOAT, false, vertexStride, 3 * 4);
        GLES32.glEnableVertexAttribArray(1);

        GLES32.glBindVertexArray(0);
    }

    public void draw(float startX, float startY, float curX, float curY, int frontTex, int backTex, int nextTex) {
        GLES32.glUseProgram(program);

        int startPosHandle = GLES32.glGetUniformLocation(program, "uStart");
        int curPosHandle = GLES32.glGetUniformLocation(program, "uCur");

        GLES32.glUniform2f(startPosHandle, startX, startY);
        GLES32.glUniform2f(curPosHandle, curX, curY);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, frontTex);
        GLES32.glUniform1i(GLES32.glGetUniformLocation(program, "frontTexture"), 0);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, backTex);
        GLES32.glUniform1i(GLES32.glGetUniformLocation(program, "backTexture"), 1);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE2);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, nextTex);
        GLES32.glUniform1i(GLES32.glGetUniformLocation(program, "nextTexture"), 2);

        GLES32.glBindVertexArray(VAO[0]);

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.length, GLES32.GL_UNSIGNED_SHORT, 0);

        GLES32.glBindVertexArray(0);
    }
}
