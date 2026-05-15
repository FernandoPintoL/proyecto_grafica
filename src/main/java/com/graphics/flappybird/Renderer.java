package com.graphics.flappybird;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Renderer: maneja todo el renderizado gráfico con mejoras visuales.
 * - Fondo, pájaros, tuberías, HUD, partículas.
 * - Soporta transparencia y animación.
 */
public class Renderer {
    private int programa;
    private int vao;
    private int vbo;

    // Uniforms.
    private int uOffsetLocation;
    private int uScaleLocation;
    private int uColorLocation;
    private int uAlphaLocation;

    // Para animación de tiempo.
    private float tiempoGlobal = 0.0f;

    // Sistema de texto.
    private BitmapFont font;

    // Quad unitario centrado en [-0.5, 0.5] x [-0.5, 0.5].
    private static final float[] QUAD_VERTICES = {
        -0.5f, -0.5f, 0.0f,
         0.5f, -0.5f, 0.0f,
         0.5f,  0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
         0.5f,  0.5f, 0.0f,
        -0.5f,  0.5f, 0.0f
    };

    public Renderer() {
        crearShaders();
        crearQuad();
        font = new BitmapFont(this, 1.0f);
    }

    /**
     * Crea shaders con soporte para transparencia (alpha).
     */
    private void crearShaders() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            uniform float uAlpha;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, uAlpha);
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

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar: " + GL20.glGetProgramInfoLog(programa));
        }

        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");
        uAlphaLocation = GL20.glGetUniformLocation(programa, "uAlpha");

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        // Activar blending para transparencia.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void checkShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    /**
     * Crea el quad base reutilizable.
     */
    private void crearQuad() {
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
        buffer.put(QUAD_VERTICES).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Renderiza el frame completo del juego.
     */
    public void render(Game game, ParticleSystem particles) {
        tiempoGlobal += 0.016f; // ~60 FPS.

        // Limpiar con color de cielo.
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Fondo con degradado suave.
        drawBackgroundGradient();

        // Nubes animadas.
        drawAnimatedClouds();

        // Tuberías.
        for (Pipe p : game.pipes) {
            drawPipe(p);
        }

        // Sombras de pájaros (debajo).
        drawBirdShadow(game.bird1);
        drawBirdShadow(game.bird2);

        // Pájaros mejorados.
        drawBirdEnhanced(game.bird1);
        drawBirdEnhanced(game.bird2);

        // Partículas (explosiones, polvo, efectos).
        particles.render(this);

        // HUD.
        drawHUD(game);

        // Pantalla de inicio.
        if (!game.gameStarted) {
            drawStartScreen();
        }

        // Pantalla de game over.
        if (game.gameOver) {
            drawGameOverScreen(game);
        }
    }

    /**
     * Dibuja un rectángulo simple con alpha (transparencia).
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b) {
        drawRectAlpha(x, y, width, height, r, g, b, 1.0f);
    }

    /**
     * Dibuja un rectángulo con transparencia.
     */
    private void drawRectAlpha(float x, float y, float width, float height, float r, float g, float b, float alpha) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, width, height);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL20.glUniform1f(uAlphaLocation, alpha);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /**
     * Dibuja una partícula (punto coloreado pequeño).
     */
    public void drawParticle(float x, float y, float size, float r, float g, float b, float alpha) {
        drawRectAlpha(x, y, size * 2, size * 2, r, g, b, alpha);
    }

    /**
     * Fondo con degradado suave (gradientes).
     */
    private void drawBackgroundGradient() {
        // Cielo superior (azul claro).
        drawRect(0.0f, 0.5f, 2.0f, 1.0f, 0.60f, 0.85f, 0.98f);
        // Cielo inferior (más oscuro).
        drawRect(0.0f, 0.0f, 2.0f, 0.5f, 0.52f, 0.80f, 0.92f);
        // Suelo (verde oscuro).
        drawRect(0.0f, -0.65f, 2.0f, 0.65f, 0.34f, 0.52f, 0.20f);
        // Línea de suelo.
        drawRect(0.0f, -0.7f, 2.0f, 0.05f, 0.2f, 0.35f, 0.1f);
    }

    /**
     * Nubes animadas que se mueven lentamente.
     */
    private void drawAnimatedClouds() {
        float cloudOffset = (tiempoGlobal * 0.05f) % 2.0f; // Se repite cada 2 unidades.

        // Nube 1.
        float cloud1X = -0.6f + cloudOffset;
        drawRect(cloud1X, 0.7f, 0.3f, 0.15f, 0.95f, 0.95f, 0.95f);
        drawRect(cloud1X - 0.1f, 0.75f, 0.15f, 0.1f, 0.98f, 0.98f, 0.98f);
        drawRect(cloud1X + 0.2f, 0.73f, 0.12f, 0.08f, 0.93f, 0.93f, 0.93f);

        // Nube 2 (más lejana).
        float cloud2X = 0.3f + cloudOffset * 0.6f;
        drawRectAlpha(cloud2X, 0.6f, 0.35f, 0.12f, 0.9f, 0.9f, 0.9f, 0.6f);

        // Nube 3 (pequeña).
        float cloud3X = 0.8f - cloudOffset * 0.8f;
        drawRectAlpha(cloud3X, 0.8f, 0.2f, 0.08f, 0.92f, 0.92f, 0.92f, 0.5f);
    }

    /**
     * Sombra debajo del pájaro (efecto de profundidad).
     */
    private void drawBirdShadow(Bird bird) {
        if (!bird.alive) return;
        float shadowX = bird.x;
        float shadowY = bird.y - bird.height * 0.6f; // Abajo del pájaro.
        float shadowWidth = bird.width * 0.6f;
        float shadowHeight = bird.height * 0.15f;
        drawRectAlpha(shadowX, shadowY, shadowWidth, shadowHeight,
                      0.0f, 0.0f, 0.0f, 0.15f); // Negro semi-transparente.
    }

    /**
     * Pájaro mejorado con más detalles y animación.
     */
    private void drawBirdEnhanced(Bird bird) {
        if (!bird.alive) {
            return;
        }

        float rotation = bird.getRotationAngle();
        float wingFlap = (float) Math.sin(tiempoGlobal * 8.0f) * 0.3f; // Animación de alas.

        // CUERPO: rectángulo principal con sombreado.
        drawRect(bird.x, bird.y, bird.width, bird.height,
                 bird.colorR, bird.colorG, bird.colorB);

        // Sombreado del cuerpo (lado oscuro).
        drawRectAlpha(bird.x + bird.width * 0.15f, bird.y - bird.height * 0.15f,
                      bird.width * 0.3f, bird.height * 0.3f,
                      bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.3f);

        // PICO: triángulo (rectángulo aproximado).
        float peakX = bird.x + bird.width * 0.35f;
        float peakY = bird.y + bird.height * 0.05f;
        drawRect(peakX, peakY, bird.width * 0.22f, bird.height * 0.35f,
                 1.0f, 0.88f, 0.1f); // Naranja.

        // ALA SUPERIOR: animada con batir.
        float wingX = bird.x - bird.width * 0.2f;
        float wingY = bird.y + bird.height * 0.25f + wingFlap;
        float wingScale = 0.9f + Math.abs(wingFlap) * 0.4f;
        drawRect(wingX, wingY, bird.width * 0.45f * wingScale, bird.height * 0.28f,
                 bird.colorR * 0.75f, bird.colorG * 0.75f, bird.colorB * 0.75f);

        // ALA INFERIOR: opuesta.
        float wing2Y = bird.y - bird.height * 0.2f - wingFlap;
        drawRect(wingX, wing2Y, bird.width * 0.4f * (1.0f - Math.abs(wingFlap) * 0.3f),
                 bird.height * 0.22f,
                 bird.colorR * 0.6f, bird.colorG * 0.6f, bird.colorB * 0.6f);

        // COLA: rectángulo trasero con fade.
        float tailX = bird.x - bird.width * 0.4f;
        float tailY = bird.y - bird.height * 0.25f;
        drawRectAlpha(tailX, tailY, bird.width * 0.15f, bird.height * 0.4f,
                      bird.colorR * 0.4f, bird.colorG * 0.4f, bird.colorB * 0.4f, 0.7f);

        // OJO: círculo blanco con pupila.
        float eyeX = bird.x + bird.width * 0.18f;
        float eyeY = bird.y + bird.height * 0.12f;

        // Iris (negro).
        drawRect(eyeX, eyeY, bird.width * 0.09f, bird.height * 0.14f,
                 0.1f, 0.1f, 0.1f);

        // Brillo en ojo (blanco).
        drawRect(eyeX + bird.width * 0.02f, eyeY + bird.height * 0.03f,
                 bird.width * 0.04f, bird.height * 0.05f,
                 1.0f, 1.0f, 1.0f);

        // Contorno del ojo (blanco).
        drawRectAlpha(eyeX, eyeY, bird.width * 0.11f, bird.height * 0.16f,
                      1.0f, 1.0f, 1.0f, 0.5f);
    }

    /**
     * Dibuja una tubería (superior + inferior).
     */
    private void drawPipe(Pipe p) {
        float gapTop = p.gapCentroY + (p.gapHeight * 0.5f);
        float gapBottom = p.gapCentroY - (p.gapHeight * 0.5f);

        // Parte superior.
        float altoSup = 1.0f - gapTop;
        if (altoSup > 0.0f) {
            float yCentroSup = gapTop + (altoSup * 0.5f);
            drawRect(p.x, yCentroSup, p.width, altoSup, 0.18f, 0.70f, 0.25f);
        }

        // Parte inferior.
        float altoInf = gapBottom + 1.0f;
        if (altoInf > 0.0f) {
            float yCentroInf = -1.0f + (altoInf * 0.5f);
            drawRect(p.x, yCentroInf, p.width, altoInf, 0.18f, 0.70f, 0.25f);
        }
    }

    /**
     * Dibuja un pájaro compuesto:
     * - Cuerpo (rectángulo)
     * - Pico (triángulo)
     * - Ala (rectángulo pequeño)
     * - Cola (rectángulo)
     * - Ojo (punto).
     */
    private void drawBird(Bird bird) {
        if (!bird.alive) {
            return; // No dibujar pájaros muertos.
        }

        float rotation = bird.getRotationAngle(); // Para animar inclinación.

        // Cuerpo: rectángulo principal.
        drawRect(bird.x, bird.y, bird.width, bird.height,
                 bird.colorR, bird.colorG, bird.colorB);

        // Pico: triángulo (aprox con pequeños rectángulos).
        float peakX = bird.x + bird.width * 0.3f;
        float peakY = bird.y;
        drawRect(peakX, peakY, bird.width * 0.2f, bird.height * 0.3f,
                 1.0f, 0.85f, 0.0f); // Amarillo.

        // Ala: rectángulo que rota según velocidad.
        float wingX = bird.x - bird.width * 0.15f;
        float wingY = bird.y + bird.height * 0.15f;
        float wingScale = 0.8f + (Math.abs(rotation) * 0.5f); // Escala dinámica.
        drawRect(wingX, wingY, bird.width * 0.4f * wingScale, bird.height * 0.25f,
                 bird.colorR * 0.7f, bird.colorG * 0.7f, bird.colorB * 0.7f);

        // Cola: rectángulo pequeño atrás.
        float tailX = bird.x - bird.width * 0.35f;
        float tailY = bird.y - bird.height * 0.2f;
        drawRect(tailX, tailY, bird.width * 0.15f, bird.height * 0.35f,
                 bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f);

        // Ojo: punto blanco muy pequeño.
        float eyeX = bird.x + bird.width * 0.15f;
        float eyeY = bird.y + bird.height * 0.1f;
        drawRect(eyeX, eyeY, bird.width * 0.08f, bird.height * 0.12f,
                 1.0f, 1.0f, 1.0f); // Blanco.
    }

    /**
     * HUD: paneles de información con números MUY GRANDES y legibles.
     * IMPORTANTE: dibujar paneles PRIMERO, números DESPUÉS para que queden enfrente.
     */
    private void drawHUD(Game game) {
        // PRIMERO: Todos los paneles (fondos)
        drawRectAlpha(-0.95f, 0.88f, 0.18f, 0.08f, 0.98f, 0.85f, 0.20f, 0.85f);
        drawRectAlpha(0.77f, 0.88f, 0.18f, 0.08f, 0.20f, 0.85f, 0.98f, 0.85f);
        drawRectAlpha(-0.12f, 0.88f, 0.24f, 0.08f, 0.6f, 0.6f, 0.2f, 0.75f);

        // DESPUÉS: Todos los números (enfrente de los paneles) - MUCHO MÁS GRANDES
        font.renderNumber(game.bird1.score, -0.94f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
        font.renderNumber(game.bird2.score, 0.78f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
        int diffLevel = Math.round(game.getDifficultyMultiplier() * 10) / 10;
        font.renderNumber(diffLevel, -0.09f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Pantalla de inicio con instrucciones claras y letras GRANDES.
     */
    private void drawStartScreen() {
        // Panel semi-transparente (oscuro con transparencia).
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.5f);

        // Título: rectángulo grande (gris)
        drawRectAlpha(-0.6f, 0.55f, 1.2f, 0.15f, 0.2f, 0.2f, 0.2f, 0.95f);
        font.renderText("FLAPPY BIRD 2P", -0.55f, 0.57f, 0.08f, 1.0f, 1.0f, 1.0f);

        // Panel P1 (naranja) - Control W
        drawRectAlpha(-0.7f, 0.30f, 0.55f, 0.2f, 0.98f, 0.85f, 0.20f, 0.85f);
        font.renderText("PLAYER 1: W", -0.65f, 0.37f, 0.07f, 1.0f, 1.0f, 1.0f);

        // Panel P2 (azul) - Control SPACE
        drawRectAlpha(0.15f, 0.30f, 0.55f, 0.2f, 0.20f, 0.85f, 0.98f, 0.85f);
        font.renderText("PLAYER 2: SP", 0.20f, 0.37f, 0.07f, 1.0f, 1.0f, 1.0f);

        // Instrucción para comenzar (amarillo/verde)
        drawRectAlpha(-0.6f, 0.05f, 1.2f, 0.12f, 0.6f, 0.6f, 0.2f, 0.8f);
        font.renderText("PRESS W or SPACE", -0.55f, 0.08f, 0.065f, 0.2f, 0.2f, 0.2f);

        // Información: evitar tuberías (gris oscuro)
        drawRectAlpha(-0.6f, -0.20f, 1.2f, 0.1f, 0.3f, 0.3f, 0.3f, 0.6f);
        font.renderText("AVOID PIPES", -0.55f, -0.18f, 0.06f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Pantalla de game over con scores y ganador visible - LETRAS GRANDES.
     */
    private void drawGameOverScreen(Game game) {
        // Panel oscuro semi-transparente.
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.65f);

        // Título "GAME OVER" (rojo).
        drawRectAlpha(-0.6f, 0.55f, 1.2f, 0.15f, 0.9f, 0.2f, 0.2f, 0.95f);
        font.renderText("GAME OVER", -0.55f, 0.57f, 0.08f, 1.0f, 1.0f, 1.0f);

        // Panel P1 Score (naranja)
        drawRectAlpha(-0.6f, 0.30f, 0.55f, 0.2f, 0.98f, 0.85f, 0.20f, 0.85f);
        font.renderNumber(game.bird1.score, -0.48f, 0.30f, 0.20f, 1.0f, 1.0f, 1.0f);

        // Panel P2 Score (azul)
        drawRectAlpha(0.05f, 0.30f, 0.55f, 0.2f, 0.20f, 0.85f, 0.98f, 0.85f);
        font.renderNumber(game.bird2.score, 0.17f, 0.30f, 0.20f, 1.0f, 1.0f, 1.0f);

        // Indicador de ganador - por color.
        if (game.bird1.score > game.bird2.score) {
            drawRectAlpha(-0.6f, 0.05f, 1.2f, 0.15f, 0.98f, 0.85f, 0.20f, 0.9f);
            font.renderText("P1 WINS", -0.5f, 0.08f, 0.065f, 0.2f, 0.2f, 0.2f);
        } else if (game.bird2.score > game.bird1.score) {
            drawRectAlpha(-0.6f, 0.05f, 1.2f, 0.15f, 0.20f, 0.85f, 0.98f, 0.9f);
            font.renderText("P2 WINS", -0.5f, 0.08f, 0.065f, 1.0f, 1.0f, 1.0f);
        } else {
            drawRectAlpha(-0.6f, 0.05f, 1.2f, 0.15f, 0.8f, 0.8f, 0.1f, 0.9f);
            font.renderText("TIE", -0.5f, 0.08f, 0.065f, 0.2f, 0.2f, 0.2f);
        }

        // Instrucción para reiniciar (verde/amarillo).
        drawRectAlpha(-0.6f, -0.20f, 1.2f, 0.12f, 0.6f, 0.6f, 0.2f, 0.8f);
        font.renderText("PRESS R TO RESTART", -0.55f, -0.18f, 0.06f, 0.2f, 0.2f, 0.2f);
    }

    /**
     * Limpia recursos.
     */
    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
    }
}
