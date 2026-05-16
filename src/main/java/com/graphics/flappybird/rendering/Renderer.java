package com.graphics.flappybird.rendering;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import com.graphics.flappybird.core.Game;
import com.graphics.flappybird.core.Bird;
import com.graphics.flappybird.core.Pipe;
import com.graphics.flappybird.services.ServiceLocator;

/**
 * Renderer: maneja todo el renderizado grÃ¡fico con mejoras visuales.
 * - Fondo, pÃ¡jaros, tuberÃ­as, HUD, partÃ­culas.
 * - Soporta transparencia y animaciÃ³n.
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

    // Para animaciÃ³n de tiempo.
    private float tiempoGlobal = 0.0f;

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
        // Registrar el servicio de fuente de texto en el Service Locator
        ServiceLocator.provideFont(new TextureFont());
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
    public void render(Game game, int windowWidth, int windowHeight) {
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

        // TuberÃ­as.
        for (Pipe p : game.pipes) {
            drawPipe(p);
        }

        // Sombras de pÃ¡jaros (debajo).
        drawBirdShadow(game.bird1);
        drawBirdShadow(game.bird2);

        // PÃ¡jaros geomÃ©tricos (figuras complejas).
        drawBirdGeometric(game.bird1);
        drawBirdGeometric(game.bird2);

        // PartÃ­culas (explosiones, polvo, efectos).
        ServiceLocator.particles().render(this);

        // HUD.
        drawHUD(game, windowWidth, windowHeight);

        
        // Pantalla de inicio (SOLO si el juego no ha empezado).
        if (!game.gameStarted) {
            drawStartScreen(windowWidth, windowHeight);
        }

        // Pantalla de game over (SOLO despuÃ©s de que el juego empezÃ³ y terminÃ³).
        if (game.gameStarted && game.gameOver) {
            drawGameOverScreen(game, windowWidth, windowHeight);
        }
    }

    /**
     * Dibuja un rectÃ¡ngulo simple con alpha (transparencia).
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b) {
        drawRectAlpha(x, y, width, height, r, g, b, 1.0f);
    }

    /**
     * Dibuja un círculo usando triangle fan.
     * @param centerX Centro X
     * @param centerY Centro Y
     * @param radiusX Radio en eje X
     * @param radiusY Radio en eje Y (para óvalos)
     * @param segments Número de segmentos (más = más suave)
     * @param r, g, b Color RGB
     */
    public void drawCircle(float centerX, float centerY, float radiusX, float radiusY,
                          int segments, float r, float g, float b) {
        drawCircleAlpha(centerX, centerY, radiusX, radiusY, segments, r, g, b, 1.0f);
    }

    /**
     * Dibuja un círculo con transparencia.
     */
    public void drawCircleAlpha(float centerX, float centerY, float radiusX, float radiusY,
                               int segments, float r, float g, float b, float alpha) {
        float[] vertices = new float[(segments + 2) * 3];
        vertices[0] = centerX;
        vertices[1] = centerY;
        vertices[2] = 0.0f;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0f * Math.PI * i / segments);
            float x = centerX + (float) Math.cos(angle) * radiusX;
            float y = centerY + (float) Math.sin(angle) * radiusY;
            vertices[(i + 1) * 3] = x;
            vertices[(i + 1) * 3 + 1] = y;
            vertices[(i + 1) * 3 + 2] = 0.0f;
        }

        drawCustomGeometry(vertices, GL11.GL_TRIANGLE_FAN, r, g, b, alpha);
    }

    /**
     * Dibuja un triángulo dado tres puntos.
     */
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3,
                            float r, float g, float b) {
        drawTriangleAlpha(x1, y1, x2, y2, x3, y3, r, g, b, 1.0f);
    }

    /**
     * Dibuja un triángulo con transparencia.
     */
    public void drawTriangleAlpha(float x1, float y1, float x2, float y2, float x3, float y3,
                                 float r, float g, float b, float alpha) {
        float[] vertices = {
            x1, y1, 0.0f,
            x2, y2, 0.0f,
            x3, y3, 0.0f
        };
        drawCustomGeometry(vertices, GL11.GL_TRIANGLES, r, g, b, alpha);
    }

    /**
     * Dibuja geometría personalizada usando vértices personalizados.
     */
    private void drawCustomGeometry(float[] vertices, int mode, float r, float g, float b, float alpha) {
        int customVAO = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(customVAO);

        int customVBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, customVBO);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glUniform3f(uColorLocation, r, g, b);
        GL20.glUniform1f(uAlphaLocation, alpha);
        GL20.glUniform2f(uOffsetLocation, 0, 0);
        GL20.glUniform2f(uScaleLocation, 1, 1);

        GL11.glDrawArrays(mode, 0, vertices.length / 3);

        GL15.glDeleteBuffers(customVBO);
        GL30.glDeleteVertexArrays(customVAO);

        GL30.glBindVertexArray(vao);
    }

    /**
     * Dibuja un rectÃ¡ngulo con transparencia.
     */
    private void drawRectAlpha(float x, float y, float width, float height, float r, float g, float b, float alpha) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, width, height);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL20.glUniform1f(uAlphaLocation, alpha);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /**
     * Dibuja una partÃ­cula (punto coloreado pequeÃ±o).
     */
    public void drawParticle(float x, float y, float size, float r, float g, float b, float alpha) {
        drawRectAlpha(x, y, size * 2, size * 2, r, g, b, alpha);
    }

    /**
     * Fondo mejorado con degradado profesional, montañas y paralaje.
     */
    private void drawBackgroundGradient() {
        // ===== CIELO CON DEGRADADO SUAVE =====
        // Capa superior: Azul más claro (horizonte)
        drawRect(0.0f, 0.75f, 2.0f, 0.5f, 0.68f, 0.88f, 0.99f);
        // Capa media: Azul degradado
        drawRect(0.0f, 0.45f, 2.0f, 0.3f, 0.58f, 0.82f, 0.95f);
        // Capa inferior: Azul más cálido (atardecer suave)
        drawRect(0.0f, 0.15f, 2.0f, 0.3f, 0.62f, 0.80f, 0.88f);

        // ===== MONTAÑAS EN EL FONDO (Parallax capa 1) =====
        // Montaña lejana izquierda (muy translúcida, ve el cielo atrás)
        float mountOffset1 = (tiempoGlobal * 0.02f) % 2.0f;
        drawTriangleAlpha(-0.8f + mountOffset1, 0.15f,
                         -0.3f + mountOffset1, 0.5f,
                         0.2f + mountOffset1, 0.15f,
                         0.45f, 0.60f, 0.35f, 0.3f);

        // Montaña lejana derecha
        drawTriangleAlpha(0.7f + mountOffset1, 0.15f,
                         1.2f + mountOffset1, 0.45f,
                         1.7f + mountOffset1, 0.15f,
                         0.50f, 0.62f, 0.38f, 0.3f);

        // ===== MONTAÑAS MÁS CERCANAS (Parallax capa 2) =====
        float mountOffset2 = (tiempoGlobal * 0.035f) % 2.5f;
        drawTriangleAlpha(-1.0f + mountOffset2, 0.05f,
                         -0.4f + mountOffset2, 0.35f,
                         0.2f + mountOffset2, 0.05f,
                         0.40f, 0.55f, 0.30f, 0.4f);

        drawTriangleAlpha(0.5f + mountOffset2, 0.05f,
                         1.1f + mountOffset2, 0.40f,
                         1.7f + mountOffset2, 0.05f,
                         0.42f, 0.57f, 0.32f, 0.4f);

        // ===== SUELO HERBOSO =====
        // Capa de hierba superior (verde claro)
        drawRect(0.0f, -0.58f, 2.0f, 0.08f, 0.40f, 0.65f, 0.25f);
        // Suelo principal (verde oscuro)
        drawRect(0.0f, -0.75f, 2.0f, 0.17f, 0.30f, 0.50f, 0.18f);
        // Línea de suelo oscura (sombra)
        drawRect(0.0f, -0.73f, 2.0f, 0.03f, 0.18f, 0.30f, 0.08f);

        // ===== DETALLES DE HIERBA =====
        // Líneas de hierba (detalles)
        drawRectAlpha(-0.8f, -0.58f, 0.15f, 0.02f, 0.35f, 0.58f, 0.20f, 0.5f);
        drawRectAlpha(-0.3f, -0.58f, 0.12f, 0.02f, 0.35f, 0.58f, 0.20f, 0.5f);
        drawRectAlpha(0.2f, -0.58f, 0.14f, 0.02f, 0.35f, 0.58f, 0.20f, 0.5f);
        drawRectAlpha(0.7f, -0.58f, 0.13f, 0.02f, 0.35f, 0.58f, 0.20f, 0.5f);
    }

    /**
     * Nubes mejoradas con formas más realistas y animación suave.
     */
    private void drawAnimatedClouds() {
        float cloudOffset = (tiempoGlobal * 0.03f) % 3.0f; // Movimiento lento y continuo

        // ===== NUBE 1: Grande y esponjosa (arriba) =====
        float cloud1X = -1.2f + cloudOffset;
        // Partes de la nube (círculos conectados)
        drawCircleAlpha(cloud1X - 0.15f, 0.72f, 0.12f, 0.08f, 12, 0.96f, 0.96f, 0.96f, 0.85f);
        drawCircleAlpha(cloud1X + 0.0f, 0.75f, 0.14f, 0.09f, 12, 0.98f, 0.98f, 0.98f, 0.9f);
        drawCircleAlpha(cloud1X + 0.15f, 0.72f, 0.12f, 0.08f, 12, 0.96f, 0.96f, 0.96f, 0.85f);

        // ===== NUBE 2: Mediana (parallax medio) =====
        float cloud2X = -0.5f + cloudOffset * 0.65f;
        drawCircleAlpha(cloud2X - 0.1f, 0.55f, 0.09f, 0.06f, 10, 0.94f, 0.94f, 0.94f, 0.7f);
        drawCircleAlpha(cloud2X + 0.05f, 0.58f, 0.10f, 0.07f, 10, 0.96f, 0.96f, 0.96f, 0.75f);
        drawCircleAlpha(cloud2X + 0.18f, 0.55f, 0.08f, 0.06f, 10, 0.93f, 0.93f, 0.93f, 0.68f);

        // ===== NUBE 3: Pequeña y ligera (paralax rápido) =====
        float cloud3X = 0.6f - cloudOffset * 0.8f;
        drawCircleAlpha(cloud3X, 0.78f, 0.07f, 0.05f, 10, 0.92f, 0.92f, 0.92f, 0.6f);
        drawCircleAlpha(cloud3X + 0.1f, 0.80f, 0.08f, 0.05f, 10, 0.94f, 0.94f, 0.94f, 0.65f);

        // ===== NUBE 4: Lejana y semi-transparente =====
        float cloud4X = 1.0f + cloudOffset * 0.4f;
        drawCircleAlpha(cloud4X - 0.08f, 0.68f, 0.10f, 0.06f, 10, 0.90f, 0.90f, 0.90f, 0.4f);
        drawCircleAlpha(cloud4X + 0.08f, 0.68f, 0.10f, 0.06f, 10, 0.90f, 0.90f, 0.90f, 0.35f);
    }

    /**
     * Sombra debajo del pÃ¡jaro (efecto de profundidad).
     */
    private void drawBirdShadow(Bird bird) {
        if (!bird.alive) return;
        float shadowX = bird.x;
        float shadowY = bird.y - bird.height * 0.6f; // Abajo del pÃ¡jaro.
        float shadowWidth = bird.width * 0.6f;
        float shadowHeight = bird.height * 0.15f;
        drawRectAlpha(shadowX, shadowY, shadowWidth, shadowHeight,
                      0.0f, 0.0f, 0.0f, 0.15f); // Negro semi-transparente.
    }

    /**
     * PÃ¡jaro mejorado con mÃ¡s detalles y animaciÃ³n.
     */
    private void drawBirdEnhanced(Bird bird) {
        if (!bird.alive) {
            return;
        }

        float rotation = bird.getRotationAngle();
        float wingFlap = (float) Math.sin(tiempoGlobal * 8.0f) * 0.3f; // AnimaciÃ³n de alas.

        // CUERPO: rectÃ¡ngulo principal con sombreado.
        drawRect(bird.x, bird.y, bird.width, bird.height,
                 bird.colorR, bird.colorG, bird.colorB);

        // Sombreado del cuerpo (lado oscuro).
        drawRectAlpha(bird.x + bird.width * 0.15f, bird.y - bird.height * 0.15f,
                      bird.width * 0.3f, bird.height * 0.3f,
                      bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.3f);

        // PICO: triÃ¡ngulo (rectÃ¡ngulo aproximado).
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

        // COLA: rectÃ¡ngulo trasero con fade.
        float tailX = bird.x - bird.width * 0.4f;
        float tailY = bird.y - bird.height * 0.25f;
        drawRectAlpha(tailX, tailY, bird.width * 0.15f, bird.height * 0.4f,
                      bird.colorR * 0.4f, bird.colorG * 0.4f, bird.colorB * 0.4f, 0.7f);

        // OJO: cÃ­rculo blanco con pupila.
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
     * PÃ¡jaro geomÃ©trico: compuesto por figuras (cÃ­rculo, triÃ¡ngulos, etc.)
     * Cumple requerimiento de examen: pÃ¡jaro con pico, alas, cola y ojo.
     * Componentes:
     * - Cuerpo: óvalo/círculo
     * - Pico: triángulo naranja
     * - Alas: triángulos animados
     * - Cola: rectángulo + pluma distintiva
     * - Ojo: círculo blanco con iris negro y pupila brillante
     */
    public void drawBirdGeometric(Bird bird) {
        if (!bird.alive) return;

        float wingFlap = (float) Math.sin(tiempoGlobal * 8.0f);
        float tailWag = (float) Math.sin(tiempoGlobal * 6.0f) * 0.15f;

        float bodyRadiusX = bird.width * 0.35f;
        float bodyRadiusY = bird.height * 0.4f;
        drawCircleAlpha(bird.x, bird.y, bodyRadiusX, bodyRadiusY, 20,
                       bird.colorR, bird.colorG, bird.colorB, 1.0f);

        drawCircleAlpha(bird.x + bodyRadiusX * 0.3f, bird.y - bodyRadiusY * 0.1f,
                       bodyRadiusX * 0.2f, bodyRadiusY * 0.2f, 12,
                       bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.4f);

        float peakTipX = bird.x + bird.width * 0.38f;
        float peakTipY = bird.y + bird.height * 0.08f;
        float peakBaseX1 = bird.x + bird.width * 0.15f;
        float peakBaseY1 = bird.y + bird.height * 0.15f;
        float peakBaseX2 = bird.x + bird.width * 0.15f;
        float peakBaseY2 = bird.y - bird.height * 0.08f;
        drawTriangleAlpha(peakTipX, peakTipY, peakBaseX1, peakBaseY1, peakBaseX2, peakBaseY2,
                         1.0f, 0.85f, 0.1f, 1.0f);

        float wingX1 = bird.x - bird.width * 0.28f;
        float wingY1 = bird.y + bird.height * 0.3f + wingFlap * 0.1f;
        float wingX2 = bird.x - bird.width * 0.1f;
        float wingY2 = bird.y + bird.height * 0.5f;
        float wingX3 = bird.x - bird.width * 0.05f;
        float wingY3 = bird.y + bird.height * 0.25f;
        drawTriangleAlpha(wingX1, wingY1, wingX2, wingY2, wingX3, wingY3,
                         bird.colorR * 0.6f, bird.colorG * 0.6f, bird.colorB * 0.6f, 0.85f);

        float wing2X1 = bird.x - bird.width * 0.25f;
        float wing2Y1 = bird.y - bird.height * 0.25f - wingFlap * 0.08f;
        float wing2X2 = bird.x - bird.width * 0.08f;
        float wing2Y2 = bird.y - bird.height * 0.4f;
        float wing2X3 = bird.x - bird.width * 0.02f;
        float wing2Y3 = bird.y - bird.height * 0.15f;
        drawTriangleAlpha(wing2X1, wing2Y1, wing2X2, wing2Y2, wing2X3, wing2Y3,
                         bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.75f);

        float tailX = bird.x - bird.width * 0.45f + tailWag;
        float tailY = bird.y - bird.height * 0.15f;
        drawRectAlpha(tailX, tailY, bird.width * 0.12f, bird.height * 0.5f,
                     bird.colorR * 0.4f, bird.colorG * 0.4f, bird.colorB * 0.4f, 0.6f);

        float featherX1 = bird.x - bird.width * 0.5f;
        float featherY1 = bird.y - bird.height * 0.35f + tailWag;
        float featherX2 = bird.x - bird.width * 0.58f;
        float featherY2 = bird.y - bird.height * 0.28f;
        float featherX3 = bird.x - bird.width * 0.45f;
        float featherY3 = bird.y - bird.height * 0.48f + tailWag;
        drawTriangleAlpha(featherX1, featherY1, featherX2, featherY2, featherX3, featherY3,
                         bird.colorR * 0.3f, bird.colorG * 0.3f, bird.colorB * 0.3f, 0.7f);

        float eyeX = bird.x + bird.width * 0.15f;
        float eyeY = bird.y + bird.height * 0.15f;

        drawCircleAlpha(eyeX, eyeY, bird.width * 0.065f, bird.height * 0.1f, 16,
                       1.0f, 1.0f, 1.0f, 1.0f);

        drawCircleAlpha(eyeX + bird.width * 0.015f, eyeY, bird.width * 0.035f, bird.height * 0.065f, 14,
                       0.05f, 0.05f, 0.05f, 1.0f);

        drawCircleAlpha(eyeX + bird.width * 0.025f, eyeY + bird.height * 0.025f,
                       bird.width * 0.015f, bird.height * 0.025f, 8,
                       1.0f, 1.0f, 1.0f, 0.9f);

        drawTriangleAlpha(eyeX - bird.width * 0.065f, eyeY + bird.height * 0.105f,
                         eyeX + bird.width * 0.065f, eyeY + bird.height * 0.105f,
                         eyeX, eyeY + bird.height * 0.115f,
                         bird.colorR * 0.3f, bird.colorG * 0.3f, bird.colorB * 0.3f, 0.5f);
    }

    /**
     * Dibuja una tubería con efecto 3D: bordes, sombreado y profundidad.
     */
    private void drawPipe(Pipe p) {
        float gapTop = p.gapCentroY + (p.gapHeight * 0.5f);
        float gapBottom = p.gapCentroY - (p.gapHeight * 0.5f);

        // Color base verde oscuro (tuberías)
        float colorR = 0.18f;
        float colorG = 0.70f;
        float colorB = 0.25f;

        // Bordes más oscuros y sombreado más claro
        float darkR = colorR * 0.4f;
        float darkG = colorG * 0.4f;
        float darkB = colorB * 0.4f;

        float lightR = colorR + 0.15f;
        float lightG = colorG + 0.15f;
        float lightB = colorB + 0.15f;

        // ===== TUBO SUPERIOR =====
        float altoSup = 1.0f - gapTop;
        if (altoSup > 0.0f) {
            float yCentroSup = gapTop + (altoSup * 0.5f);

            // Tubo principal (verde)
            drawRect(p.x, yCentroSup, p.width, altoSup, colorR, colorG, colorB);

            // Borde izquierdo (sombreado oscuro para profundidad)
            float borderWidth = p.width * 0.08f;
            drawRectAlpha(p.x - p.width * 0.5f + borderWidth * 0.5f, yCentroSup,
                         borderWidth, altoSup, darkR, darkG, darkB, 0.6f);

            // Borde derecho (más claro, efecto de luz)
            drawRectAlpha(p.x + p.width * 0.5f - borderWidth * 0.5f, yCentroSup,
                         borderWidth, altoSup, lightR, lightG, lightB, 0.4f);

            // Borde superior (entrada del tubo, oscuro)
            float topBorderHeight = altoSup * 0.05f;
            float topY = gapTop + topBorderHeight * 0.5f;
            drawRectAlpha(p.x, topY, p.width + borderWidth * 0.5f, topBorderHeight,
                         darkR, darkG, darkB, 0.7f);

            // Efecto de profundidad: línea interior oscura
            drawRectAlpha(p.x, yCentroSup - altoSup * 0.48f, p.width * 0.7f, altoSup * 0.02f,
                         darkR * 0.8f, darkG * 0.8f, darkB * 0.8f, 0.5f);
        }

        // ===== TUBO INFERIOR =====
        float altoInf = gapBottom + 1.0f;
        if (altoInf > 0.0f) {
            float yCentroInf = -1.0f + (altoInf * 0.5f);

            // Tubo principal (verde)
            drawRect(p.x, yCentroInf, p.width, altoInf, colorR, colorG, colorB);

            // Borde izquierdo (sombreado oscuro)
            float borderWidth = p.width * 0.08f;
            drawRectAlpha(p.x - p.width * 0.5f + borderWidth * 0.5f, yCentroInf,
                         borderWidth, altoInf, darkR, darkG, darkB, 0.6f);

            // Borde derecho (más claro)
            drawRectAlpha(p.x + p.width * 0.5f - borderWidth * 0.5f, yCentroInf,
                         borderWidth, altoInf, lightR, lightG, lightB, 0.4f);

            // Borde inferior (salida del tubo, oscuro)
            float bottomBorderHeight = altoInf * 0.05f;
            float bottomY = gapBottom - bottomBorderHeight * 0.5f;
            drawRectAlpha(p.x, bottomY, p.width + borderWidth * 0.5f, bottomBorderHeight,
                         darkR, darkG, darkB, 0.7f);

            // Efecto de profundidad: línea interior oscura
            drawRectAlpha(p.x, yCentroInf + altoInf * 0.48f, p.width * 0.7f, altoInf * 0.02f,
                         darkR * 0.8f, darkG * 0.8f, darkB * 0.8f, 0.5f);
        }

        // ===== GAP (Espacio entre tubos) =====
        // Línea central invisible para marcar el límite de seguridad
        float gapCenterLineOpacity = 0.0f; // Invisible, solo para debugging si quieres
        // drawRectAlpha(p.x, p.gapCentroY, p.width, 0.01f, 1.0f, 0.0f, 0.0f, gapCenterLineOpacity);
    }

    /**
     * Dibuja un pÃ¡jaro compuesto:
     * - Cuerpo (rectÃ¡ngulo)
     * - Pico (triÃ¡ngulo)
     * - Ala (rectÃ¡ngulo pequeÃ±o)
     * - Cola (rectÃ¡ngulo)
     * - Ojo (punto).
     */
    private void drawBird(Bird bird) {
        if (!bird.alive) {
            return; // No dibujar pÃ¡jaros muertos.
        }

        float rotation = bird.getRotationAngle(); // Para animar inclinaciÃ³n.

        // Cuerpo: rectÃ¡ngulo principal.
        drawRect(bird.x, bird.y, bird.width, bird.height,
                 bird.colorR, bird.colorG, bird.colorB);

        // Pico: triÃ¡ngulo (aprox con pequeÃ±os rectÃ¡ngulos).
        float peakX = bird.x + bird.width * 0.3f;
        float peakY = bird.y;
        drawRect(peakX, peakY, bird.width * 0.2f, bird.height * 0.3f,
                 1.0f, 0.85f, 0.0f); // Amarillo.

        // Ala: rectÃ¡ngulo que rota segÃºn velocidad.
        float wingX = bird.x - bird.width * 0.15f;
        float wingY = bird.y + bird.height * 0.15f;
        float wingScale = 0.8f + (Math.abs(rotation) * 0.5f); // Escala dinÃ¡mica.
        drawRect(wingX, wingY, bird.width * 0.4f * wingScale, bird.height * 0.25f,
                 bird.colorR * 0.7f, bird.colorG * 0.7f, bird.colorB * 0.7f);

        // Cola: rectÃ¡ngulo pequeÃ±o atrÃ¡s.
        float tailX = bird.x - bird.width * 0.35f;
        float tailY = bird.y - bird.height * 0.2f;
        drawRect(tailX, tailY, bird.width * 0.15f, bird.height * 0.35f,
                 bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f);

        // Ojo: punto blanco muy pequeÃ±o.
        float eyeX = bird.x + bird.width * 0.15f;
        float eyeY = bird.y + bird.height * 0.1f;
        drawRect(eyeX, eyeY, bird.width * 0.08f, bird.height * 0.12f,
                 1.0f, 1.0f, 1.0f); // Blanco.
    }

    /**
     * HUD: paneles con nÃºmeros grandes legibles.
     */
    private void drawHUD(Game game, int windowWidth, int windowHeight) {
        // PRIMERO: Todos los paneles (fondos)
        drawRectAlpha(-0.95f, 0.88f, 0.18f, 0.08f, 0.98f, 0.85f, 0.20f, 0.85f);
        drawRectAlpha(0.77f, 0.88f, 0.18f, 0.08f, 0.20f, 0.85f, 0.98f, 0.85f);
        drawRectAlpha(-0.12f, 0.88f, 0.24f, 0.08f, 0.6f, 0.6f, 0.2f, 0.75f);

        // DESPUÃ‰S: Todos los nÃºmeros (enfrente de los paneles)
        ServiceLocator.font().renderNumber(game.bird1.score, -0.94f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderNumber(game.bird2.score, 0.78f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
        int diffLevel = Math.round(game.getDifficultyMultiplier() * 10) / 10;
        ServiceLocator.font().renderNumber(diffLevel, -0.09f, 0.85f, 0.25f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Pantalla de inicio con instrucciones claras.
     */
    private void drawStartScreen(int windowWidth, int windowHeight) {
        // Reactivar el programa y VAO (puede estar desactivado despuÃ©s de font.renderText)
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Fondo oscuro semi-transparente
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, 0.1f, 0.1f, 0.15f, 0.8f);

        // TÃTULO
        // drawRectAlpha(-0.5f, 0.65f, 1.0f, 0.15f, 0.2f, 0.2f, 0.3f, 0.9f);
        ServiceLocator.font().renderText("FLAPPY BIRD 2P", -0.42f, 0.66f, 0.09f, 1.0f, 1.0f, 1.0f);

        // CONTROLES - P1
        // drawRectAlpha(-0.75f, 0.40f, 0.45f, 0.15f, 0.98f, 0.85f, 0.20f, 0.85f);
        ServiceLocator.font().renderText("PLAYER 1", -0.73f, 0.43f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("PRESS W", -0.73f, 0.35f, 0.06f, 0.95f, 0.95f, 0.95f);

        // CONTROLES - P2
        // drawRectAlpha(0.30f, 0.40f, 0.45f, 0.15f, 0.20f, 0.85f, 0.98f, 0.85f);
        ServiceLocator.font().renderText("PLAYER 2", 0.32f, 0.43f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("PRESS SPACE", 0.32f, 0.35f, 0.06f, 0.95f, 0.95f, 0.95f);

        // INSTRUCCIÃ“N PARA COMENZAR
        // drawRectAlpha(-0.5f, 0.18f, 1.0f, 0.12f, 0.7f, 0.6f, 0.2f, 0.8f);
        ServiceLocator.font().renderText("TAP TO START THE GAME", -0.45f, 0.20f, 0.075f, 0.2f, 0.2f, 0.2f);

        // OBJETIVO DEL JUEGO
        // drawRectAlpha(-0.5f, -0.05f, 1.0f, 0.15f, 1.2f, 1.5f, 1.8f, 0.75f);
        ServiceLocator.font().renderText("GOAL: AVOID PIPES", -0.48f, 0.02f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("SURVIVE AS LONG AS YOU CAN", -0.48f, -0.04f, 0.06f, 0.9f, 0.9f, 0.9f);

        // DIFICULTAD PROGRESIVA
        // drawRectAlpha(-0.5f, -0.35f, 1.0f, 0.15f, 0.8f, 0.4f, 0.2f, 0.75f);
        ServiceLocator.font().renderText("DIFFICULTY INCREASES", -0.48f, -0.28f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("GAIN POINTS BY PASSING GAPS", -0.48f, -0.34f, 0.06f, 0.9f, 0.9f, 0.9f);
    }

    /**
     * Pantalla de game over con scores y ganador.
     */
    private void drawGameOverScreen(Game game, int windowWidth, int windowHeight) {
        // Reactivar el programa y VAO (puede estar desactivado despuÃ©s de font.renderText)
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Panel ROJO para debug
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, 1.0f, 0.0f, 0.0f, 0.85f);

        // TÃ­tulo ROJO
        // drawRectAlpha(-0.6f, 0.55f, 1.2f, 0.15f, 1.0f, 0.0f, 0.0f, 0.95f);
        ServiceLocator.font().renderText("GAME OVER", -0.48f, 0.565f, 0.08f, 1.0f, 1.0f, 1.0f);

        // Panel P1 ROJO
        // drawRectAlpha(-0.6f, 0.30f, 0.55f, 0.2f, 1.0f, 0.0f, 0.0f, 0.85f);
        ServiceLocator.font().renderText("P1: " + game.bird1.score, -0.55f, 0.355f, 0.07f, 1.0f, 1.0f, 1.0f);

        // Panel P2 ROJO
        // drawRectAlpha(0.05f, 0.30f, 0.55f, 0.2f, 1.0f, 0.0f, 0.0f, 0.85f);
        ServiceLocator.font().renderText("P2: " + game.bird2.score, 0.12f, 0.355f, 0.07f, 1.0f, 1.0f, 1.0f);

        // Indicador de ganador ROJO
        // drawRectAlpha(-0.6f, 0.05f, 1.2f, 0.15f, 1.0f, 0.0f, 0.0f, 0.9f);
        ServiceLocator.font().renderText("PRESS R TO RESTART", -0.48f, 0.075f, 0.06f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Limpia recursos.
     */
    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        ServiceLocator.font().cleanup();
    }
}
