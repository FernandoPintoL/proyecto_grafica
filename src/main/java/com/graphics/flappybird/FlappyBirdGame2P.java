package com.graphics.flappybird;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * FlappyBirdGame2P: Flappy Bird para 2 jugadores, optimizado y fluido.
 * Basado en AppFlappyBird pero con dos pájaros simultáneos.
 */
public class FlappyBirdGame2P {
    private static final int ANCHO = 900;
    private static final int ALTO = 700;

    // Posiciones de pájaros.
    private static final float BIRD1_X = -0.55f;
    private static final float BIRD2_X = 0.15f;
    private static final float BIRD_ANCHO = 0.10f;
    private static final float BIRD_ALTO = 0.10f;

    // Física.
    private static final float GRAVEDAD = -1.9f;
    private static final float IMPULSO = 0.85f;
    private static final float VELOCIDAD_MAX = -1.8f;

    // Tuberías.
    private static final float TUBERIA_ANCHO = 0.18f;
    private static final float GAP_ALTO = 0.48f;
    private static final float VELOCIDAD_TUBERIAS = 0.62f;
    private static final float TIEMPO_SPAWN = 1.5f;
    private static final float GAP_MIN = -0.45f;
    private static final float GAP_MAX = 0.45f;

    private long window;
    private int programa;
    private int vao;
    private int vbo;
    private int uOffsetLocation;
    private int uScaleLocation;
    private int uColorLocation;

    // Estado de pájaros.
    private float bird1Y, bird1VelY;
    private float bird2Y, bird2VelY;
    private int score1, score2;
    private boolean bird1Alive, bird2Alive;

    // Estado de juego.
    private boolean started, gameOver;
    private float timerSpawn;
    private List<Tuberia> tuberias = new ArrayList<>();
    private Random random = new Random();

    // Input detection.
    private boolean prevW, prevSpace, prevR;

    private static class Tuberia {
        float x, gapCentroY;
        boolean scored;
        Tuberia(float x, float gapCentroY) {
            this.x = x;
            this.gapCentroY = gapCentroY;
        }
    }

    public void run() {
        init();
        reset();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW init failed");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird 2P", 0, 0);
        if (window == 0) throw new RuntimeException("Window creation failed");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        setupShaders();
        setupQuad();

        // Callback para redimensionamiento.
        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            GL11.glViewport(0, 0, width, height);
        });
    }

    private void setupShaders() {
        String vert = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        String frag = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vert);
        GL20.glCompileShader(vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, frag);
        GL20.glCompileShader(fs);

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vs);
        GL20.glAttachShader(programa, fs);
        GL20.glLinkProgram(programa);

        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
    }

    private void setupQuad() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
        GL20.glEnableVertexAttribArray(0);
    }

    private void reset() {
        bird1Y = bird2Y = 0.0f;
        bird1VelY = bird2VelY = 0.0f;
        score1 = score2 = 0;
        bird1Alive = bird2Alive = true;
        started = gameOver = false;
        timerSpawn = 0.0f;
        tuberias.clear();
        updateTitle();
    }

    private void input() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        if (wNow && !prevW) {
            if (!started) {
                started = true;
                bird1VelY = IMPULSO;
                bird2VelY = IMPULSO;
            } else if (!gameOver && bird1Alive) {
                bird1VelY = IMPULSO;
            }
        }
        prevW = wNow;

        boolean spacNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spacNow && !prevSpace) {
            if (!started) {
                started = true;
                bird1VelY = IMPULSO;
                bird2VelY = IMPULSO;
            } else if (!gameOver && bird2Alive) {
                bird2VelY = IMPULSO;
            }
        }
        prevSpace = spacNow;

        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rNow && !prevR && gameOver) {
            reset();
        }
        prevR = rNow;
    }

    private void update(float dt) {
        if (!started || gameOver) return;

        // Bird 1 physics
        bird1VelY += GRAVEDAD * dt;
        if (bird1VelY < VELOCIDAD_MAX) bird1VelY = VELOCIDAD_MAX;
        bird1Y += bird1VelY * dt;

        // Bird 2 physics
        bird2VelY += GRAVEDAD * dt;
        if (bird2VelY < VELOCIDAD_MAX) bird2VelY = VELOCIDAD_MAX;
        bird2Y += bird2VelY * dt;

        // Collision with ceiling/floor
        if (bird1Alive) {
            if (bird1Y + BIRD_ALTO * 0.5f >= 1.0f || bird1Y - BIRD_ALTO * 0.5f <= -1.0f) {
                bird1Alive = false;
            }
        }
        if (bird2Alive) {
            if (bird2Y + BIRD_ALTO * 0.5f >= 1.0f || bird2Y - BIRD_ALTO * 0.5f <= -1.0f) {
                bird2Alive = false;
            }
        }

        if (!bird1Alive && !bird2Alive) {
            gameOver = true;
            updateTitle();
            return;
        }

        // Pipe spawning
        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_SPAWN) {
            timerSpawn = 0.0f;
            float gapCentro = GAP_MIN + random.nextFloat() * (GAP_MAX - GAP_MIN);
            tuberias.add(new Tuberia(1.2f, gapCentro));
        }

        // Pipe update and collision
        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= VELOCIDAD_TUBERIAS * dt;

            // Scoring
            if (t.x + TUBERIA_ANCHO * 0.5f < BIRD1_X && !t.scored) {
                score1++;
                t.scored = true;
            }
            if (t.x + TUBERIA_ANCHO * 0.5f < BIRD2_X && !t.scored) {
                score2++;
            }

            // Collisions
            if (bird1Alive && collides(BIRD1_X, bird1Y, t)) bird1Alive = false;
            if (bird2Alive && collides(BIRD2_X, bird2Y, t)) bird2Alive = false;

            if (t.x + TUBERIA_ANCHO * 0.5f < -1.3f) it.remove();
        }

        if (!bird1Alive && !bird2Alive) {
            gameOver = true;
            updateTitle();
        }
    }

    private boolean collides(float birdX, float birdY, Tuberia t) {
        float bL = birdX - BIRD_ANCHO * 0.5f;
        float bR = birdX + BIRD_ANCHO * 0.5f;
        float bB = birdY - BIRD_ALTO * 0.5f;
        float bT = birdY + BIRD_ALTO * 0.5f;

        float pL = t.x - TUBERIA_ANCHO * 0.5f;
        float pR = t.x + TUBERIA_ANCHO * 0.5f;

        if (!(bR > pL && bL < pR)) return false;

        float gT = t.gapCentroY + GAP_ALTO * 0.5f;
        float gB = t.gapCentroY - GAP_ALTO * 0.5f;
        return bT > gT || bB < gB;
    }

    private void render() {
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Draw pipes
        for (Tuberia t : tuberias) {
            float gT = t.gapCentroY + GAP_ALTO * 0.5f;
            float gB = t.gapCentroY - GAP_ALTO * 0.5f;

            float hT = 1.0f - gT;
            if (hT > 0) {
                float yT = gT + hT * 0.5f;
                drawRect(t.x, yT, TUBERIA_ANCHO, hT, 0.18f, 0.70f, 0.25f);
            }

            float hB = gB + 1.0f;
            if (hB > 0) {
                float yB = -1.0f + hB * 0.5f;
                drawRect(t.x, yB, TUBERIA_ANCHO, hB, 0.18f, 0.70f, 0.25f);
            }
        }

        // Draw birds
        if (bird1Alive) drawRect(BIRD1_X, bird1Y, BIRD_ANCHO, BIRD_ALTO, 0.98f, 0.85f, 0.20f);
        if (bird2Alive) drawRect(BIRD2_X, bird2Y, BIRD_ANCHO, BIRD_ALTO, 0.20f, 0.85f, 0.98f);

        // Draw score panels
        drawRect(-0.95f, 0.88f, 0.18f, 0.08f, 0.98f, 0.85f, 0.20f);
        drawRect(0.77f, 0.88f, 0.18f, 0.08f, 0.20f, 0.85f, 0.98f);

        if (gameOver) {
            drawRect(0.0f, 0.0f, 2.0f, 0.22f, 0.15f, 0.18f, 0.22f);
        }
    }

    private void drawRect(float x, float y, float w, float h, float r, float g, float b) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, w, h);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private void updateTitle() {
        String base = "Flappy Bird 2P - P1: " + score1 + " | P2: " + score2;
        if (!started) {
            GLFW.glfwSetWindowTitle(window, base + " | W or SPACE to start");
        } else if (gameOver) {
            GLFW.glfwSetWindowTitle(window, base + " | GAME OVER - R to restart");
        } else {
            GLFW.glfwSetWindowTitle(window, base);
        }
    }

    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt = now - lastTime;
            lastTime = now;
            if (dt > 0.033f) dt = 0.033f;

            input();
            update(dt);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new FlappyBirdGame2P().run();
    }
}
