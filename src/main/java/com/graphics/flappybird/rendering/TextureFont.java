package com.graphics.flappybird.rendering;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import com.graphics.flappybird.services.IFontService;

/**
 * TextureFont: renderiza texto usando una imagen bitmap de fuente.
 * Soporta letras, nÃºmeros, espacios y caracteres especiales.
 * Implementa IFontService para ser usado a travÃ©s del Service Locator.
 */
public class TextureFont implements IFontService {
    private int textureId;
    private int vao;
    private int vbo;
    private int program;

    // Mapeo de caracteres a posiciones en la bitmap
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ :";
    private static final int CHAR_WIDTH = 32;
    private static final int CHAR_HEIGHT = 48;
    private static final int COLS = 8;
    private static final int ROWS = 5;

    public TextureFont() {
        textureId = TextureLoader.loadTexture("font.png");
        setupShaders();
        setupQuad();
    }

    private void setupShaders() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            out vec2 texCoord;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
                texCoord = aTexCoord;
            }
            """;

        String fragmentSrc = """
            #version 330 core
            in vec2 texCoord;
            uniform sampler2D uTexture;
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                vec4 texColor = texture(uTexture, texCoord);
                fragColor = vec4(uColor, 1.0) * texColor;
            }
            """;

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        checkShader(vertexShader, "Vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        checkShader(fragmentShader, "Fragment");

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(program));
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void checkShader(int shader, String type) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(type + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    private void setupQuad() {
        // Quad con coordenadas de textura
        float[] vertices = {
            -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 1.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f, 0.0f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // PosiciÃ³n
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Coordenadas de textura
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    @Override
    public void renderText(String text, float x, float y, float size, float r, float g, float b) {
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        for (char c : text.toCharArray()) {
            int charIndex = CHARSET.indexOf(c);
            if (charIndex < 0) {
                x += size * 0.5f;
                continue;
            }

            int col = charIndex % COLS;
            int row = charIndex / COLS;

            float u = col / (float) COLS;
            float v = row / (float) ROWS;
            float uWidth = 1.0f / COLS;
            float vHeight = 1.0f / ROWS;

            // Actualizar coordenadas UV del quad
            float[] vertices = {
                -0.5f, -0.5f, 0.0f, u, v + vHeight,
                 0.5f, -0.5f, 0.0f, u + uWidth, v + vHeight,
                 0.5f,  0.5f, 0.0f, u + uWidth, v,
                -0.5f, -0.5f, 0.0f, u, v + vHeight,
                 0.5f,  0.5f, 0.0f, u + uWidth, v,
                -0.5f,  0.5f, 0.0f, u, v
            };

            FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
            buffer.put(vertices).flip();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

            int offsetLoc = GL20.glGetUniformLocation(program, "uOffset");
            int scaleLoc = GL20.glGetUniformLocation(program, "uScale");
            int colorLoc = GL20.glGetUniformLocation(program, "uColor");
            int textureLoc = GL20.glGetUniformLocation(program, "uTexture");

            GL20.glUniform2f(offsetLoc, x, y);
            GL20.glUniform2f(scaleLoc, size * 0.6f, size);
            GL20.glUniform3f(colorLoc, r, g, b);
            GL20.glUniform1i(textureLoc, 0);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

            x += size * 0.5f;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL20.glUseProgram(0);
    }

    @Override
    public void renderNumber(int num, float x, float y, float size, float r, float g, float b) {
        renderText(String.valueOf(num), x, y, size, r, g, b);
    }

    @Override
    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(program);
        GL11.glDeleteTextures(textureId);
    }
}
