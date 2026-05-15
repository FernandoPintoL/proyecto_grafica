package com.graphics.flappybird;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * FlappyBirdGame: Flappy Bird para 2 jugadores con visuales mejorados.
 * Optimizado para máximo rendimiento sin lag.
 */
public class FlappyBirdGame {
    private long window;
    private Game game;
    private Renderer renderer;
    private int windowWidth = 900;
    private int windowHeight = 700;

    // Input detection (flanco).
    private boolean prevW = false;
    private boolean prevSpace = false;
    private boolean prevR = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    /**
     * Inicializa GLFW, OpenGL y recursos.
     */
    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(windowWidth, windowHeight,
                                        "Flappy Bird - 2 Players (P1: W, P2: SPACE)", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Window creation failed");
        }

        // ESC to close.
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(w, true);
            }
        });

        // Window resize callback.
        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            GL11.glViewport(0, 0, width, height);
        });

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);

        GL.createCapabilities();

        game = new Game();
        renderer = new Renderer();

        updateWindowTitle();
    }

    /**
     * Main loop: optimized for performance.
     */
    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float deltaTime = now - lastTime;
            lastTime = now;

            // Clamp dt to avoid large jumps.
            if (deltaTime > 0.033f) {
                deltaTime = 0.033f;
            }

            processInput();
            game.update(deltaTime);
            renderer.render(game, game.particles);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            updateWindowTitle();
        }
    }

    /**
     * Process input for both players with edge detection.
     */
    private void processInput() {
        // Player 1: W to jump
        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        if (wNow && !prevW) {
            if (!game.gameStarted) {
                game.start();
            } else if (game.bird1.alive) {
                game.bird1.jump();
                game.particles.jumpDust(game.bird1.x, game.bird1.y);
                SoundManager.playJumpSound();
            }
        }
        prevW = wNow;

        // Player 2: SPACE to jump
        boolean spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceNow && !prevSpace) {
            if (!game.gameStarted) {
                game.start();
            } else if (game.bird2.alive) {
                game.bird2.jump();
                game.particles.jumpDust(game.bird2.x, game.bird2.y);
                SoundManager.playJumpSound();
            }
        }
        prevSpace = spaceNow;

        // R to restart
        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rNow && !prevR && game.gameOver) {
            game.reset();
        }
        prevR = rNow;
    }

    /**
     * Update window title with live info.
     */
    private void updateWindowTitle() {
        String title = String.format(
            "Flappy Bird - P1: %d | P2: %d | Speed: %.1fx | %s",
            game.bird1.score,
            game.bird2.score,
            game.getDifficultyMultiplier(),
            game.gameOver ? "GAME OVER (R to restart)" : (game.gameStarted ? "Playing" : "W/SPACE to start")
        );
        GLFW.glfwSetWindowTitle(window, title);
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new FlappyBirdGame().run();
    }
}
