package com.group04.scrapbookwidget.ui.meshes;

import android.opengl.GLES32;

import com.group04.scrapbookwidget.ui.pagecurl.PageRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class SimpleMesh {
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
                    "in vec2 TexCoord;" +
                    "uniform sampler2D uTexture;\n" +
                    "void main() {\n" +
                    "    FragColor = texture(uTexture, TexCoord);\n" +
                    "}";

    private final int program;
    private final int[] VBO;
    private final int[] VAO;
    private final int[] EBO;

    public SimpleMesh() {
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

    public void draw(int tex) {
        GLES32.glUseProgram(program);

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tex);
        GLES32.glUniform1i(GLES32.glGetUniformLocation(program, "uTexture"), 0);

        GLES32.glBindVertexArray(VAO[0]);

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, drawOrder.length, GLES32.GL_UNSIGNED_SHORT, 0);

        GLES32.glBindVertexArray(0);
    }
}