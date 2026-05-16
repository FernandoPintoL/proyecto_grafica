package com.graphics.flappybird;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.graphics.flappybird.core.Game;
import com.graphics.flappybird.rendering.Renderer;
import com.graphics.flappybird.services.ServiceLocator;
import com.graphics.flappybird.audio.SoundManager;


/**
 * FlappyBirdGame: Flappy Bird para 2 jugadores con visuales mejorados.
 * Optimizado para mÃ¡ximo rendimiento sin lag.
 */
public class FlappyBirdGame {
    // Identificador Ãºnico de la ventana GLFW (se crea en init())
    private long window;

    // Instancia del motor de juego que maneja la lÃ³gica (colisiones, fÃ­sica, estado)
    private Game game;

    // Instancia del renderizador que dibuja todo en la pantalla usando OpenGL
    private Renderer renderer;

    // Dimensiones de la ventana en pÃ­xeles
    private int windowWidth = 900;
    private int windowHeight = 700;

    // Variables para detectar cambios de estado en el teclado (flanco = cambio de false a true).
    // Se usan para detectar solo la presiÃ³n (no mantener presionado constantemente)
    private boolean prevW = false;      // Estado anterior de la tecla W (Jugador 1)
    private boolean prevSpace = false;  // Estado anterior de la tecla SPACE (Jugador 2)
    private boolean prevR = false;      // Estado anterior de la tecla R (Reiniciar)

    // MÃ©todo principal que ejecuta el ciclo de vida del programa
    public void run() {
        init();      // Paso 1: Inicializar GLFW, OpenGL, ventana y recursos
        loop();      // Paso 2: Bucle principal (procesa input, actualiza lÃ³gica, renderiza)
        cleanup();   // Paso 3: Liberar recursos (ventana, texturas, shaders, memoria)
    }

    /**
     * Inicializa GLFW, OpenGL y recursos.
     * Este mÃ©todo configura todo lo necesario antes de entrar al bucle principal.
     */
    private void init() {
        // Inicializar GLFW (librerÃ­a para gestionar ventanas y entrada)
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        // Resetear todas las opciones de ventana a sus valores por defecto
        GLFW.glfwDefaultWindowHints();

        // Configurar opciones de la ventana ANTES de crearla:
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);         // Ventana oculta al inicio (la mostramos despuÃ©s)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);        // Permitir redimensionar la ventana
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);        // Usar OpenGL versiÃ³n 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);        // (3 = major, 3 = minor)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE); // Usar Core Profile (sin funciones antiguas)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);        // Compatible con versiones futuras

        // Crear la ventana con las dimensiones y tÃ­tulo especificados
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight,
                                        "Flappy Bird - 2 Players (P1: W, P2: SPACE)", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Window creation failed");
        }

        // Configurar callback para ESC: cierra la ventana cuando se presiona ESC
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(w, true);
            }
        });

        // Configurar callback para cuando la ventana se redimensiona:
        // Actualiza el viewport de OpenGL para que renderice en el Ã¡rea correcta
        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            GL11.glViewport(0, 0, width, height);
        });

        // Hacer que esta ventana sea el contexto actual (donde se renderiza)
        GLFW.glfwMakeContextCurrent(window);

        // Habilitar V-Sync (sincronizar con pantalla a 60 FPS)
        GLFW.glfwSwapInterval(1);

        // Mostrar la ventana (ahora que estÃ¡ completamente configurada)
        GLFW.glfwShowWindow(window);

        // Cargar todas las funciones de OpenGL disponibles en esta plataforma
        GL.createCapabilities();

        // Registrar el servicio de audio en el Service Locator (antes de crear Game y Renderer)
        ServiceLocator.provideAudio(new SoundManager());

        // Crear instancia del motor de juego (lÃ³gica, fÃ­sica, colisiones)
        game = new Game();

        // Crear instancia del renderizador (dibuja todo)
        renderer = new Renderer();

        // Actualizar el tÃ­tulo de la ventana con informaciÃ³n inicial del juego
        updateWindowTitle();
    }

    /**
     * Bucle principal: ejecuta continuamente mientras la ventana estÃ© abierta.
     * Es el corazÃ³n del programa: procesa input, actualiza lÃ³gica y renderiza cada frame.
     */
    private void loop() {
        // Obtener el tiempo actual en segundos (para calcular deltaTime)
        float lastTime = (float) GLFW.glfwGetTime();

        // Continuar mientras la ventana NO haya recibido seÃ±al de cierre (ESC o botÃ³n X)
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Obtener el tiempo actual en este frame
            float now = (float) GLFW.glfwGetTime();

            // deltaTime = tiempo transcurrido desde el Ãºltimo frame (en segundos)
            // Ejemplo: si corre a 60 FPS, deltaTime â‰ˆ 0.0167 segundos
            float deltaTime = now - lastTime;

            // Guardar el tiempo actual para el prÃ³ximo frame
            lastTime = now;

            // Limitar deltaTime para evitar saltos grandes (por lag o pauses)
            // MÃ¡ximo 0.033 segundos = mÃ­nimo 30 FPS implÃ­cito
            // Esto previene que la fÃ­sica "salte" demasiado si hay un pico de lag
            if (deltaTime > 0.033f) {
                deltaTime = 0.033f;
            }

            // FASE 1: Procesar entrada del usuario (detectar teclas presionadas)
            processInput();

            // FASE 2: Actualizar la lÃ³gica del juego (movimiento, colisiones, fÃ­sica)
            game.update(deltaTime);

            // FASE 3: Dibujar todo en la pantalla (aves, tuberÃ­as, HUD, partÃ­culas)
            renderer.render(game, windowWidth, windowHeight);

            // Intercambiar buffers: mostrar lo que se dibujÃ³ en esta iteraciÃ³n
            // (OpenGL dibuja en un buffer invisible y lo muestra cuando se llama esto)
            GLFW.glfwSwapBuffers(window);

            // Procesar eventos del sistema (redimensionar ventana, input, etc.)
            GLFW.glfwPollEvents();

            // Actualizar el tÃ­tulo de la ventana con puntuaciones y estado actual
            updateWindowTitle();
        }
    }

    /**
     * Procesa la entrada del usuario usando detecciÃ³n de flanco.
     * "Flanco" = cambio de estado (de no presionado a presionado).
     * Esto permite detectar un solo "click" aunque se mantenga presionada la tecla.
     */
    private void processInput() {
        // ============ JUGADOR 1: Tecla W para saltar ============
        // Obtener estado ACTUAL de la tecla W (true si estÃ¡ presionada, false si no)
        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;

        // Detectar flanco de subida: "wNow" es true Y "prevW" era false
        // Esto solo ocurre en el frame donde la tecla se presiona por primera vez
        if (wNow && !prevW) {
            if (!game.gameStarted) {
                // Si el juego aÃºn no ha comenzado, iniciarlo
                game.start();
            } else if (game.bird1.alive) {
                // Si el juego estÃ¡ corriendo y el jugador 1 estÃ¡ vivo:
                game.bird1.jump();                              // Hacer que el ave salte
                ServiceLocator.particles().jumpDust(game.bird1.x, game.bird1.y); // Crear partÃ­culas de polvo
                ServiceLocator.audio().playJumpSound();                   // Reproducir sonido de salto
            }
        }
        // Guardar el estado actual para comparar en el prÃ³ximo frame
        prevW = wNow;

        // ============ JUGADOR 2: Tecla SPACE para saltar ============
        // Mismo patrÃ³n que jugador 1 pero con tecla diferente
        boolean spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

        if (spaceNow && !prevSpace) {
            if (!game.gameStarted) {
                game.start();
            } else if (game.bird2.alive) {
                game.bird2.jump();
                ServiceLocator.particles().jumpDust(game.bird2.x, game.bird2.y);
                ServiceLocator.audio().playJumpSound();
            }
        }
        prevSpace = spaceNow;

        // ============ REINICIO: Tecla R para reiniciar el juego ============
        // Obtener estado ACTUAL de la tecla R
        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;

        // Detectar flanco: R presionada AHORA, no lo era antes, Y el juego estÃ¡ en estado "GAME OVER"
        if (rNow && !prevR && game.gameOver) {
            game.reset();  // Reiniciar el juego a su estado inicial
        }
        prevR = rNow;
    }

    /**
     * Actualiza el tÃ­tulo de la ventana con informaciÃ³n en tiempo real.
     * Se llama cada frame para mostrar puntuaciones, velocidad y estado actual.
     */
    private void updateWindowTitle() {
        // Construir un string con formato que incluya:
        // - PuntuaciÃ³n del jugador 1 y 2
        // - Multiplicador de velocidad (dificultad progresiva)
        // - Estado del juego (Starting, Playing, o Game Over)
        String title = String.format(
            "Flappy Bird - P1: %d | P2: %d | Speed: %.1fx | %s",
            game.bird1.score,                    // PuntuaciÃ³n jugador 1
            game.bird2.score,                    // PuntuaciÃ³n jugador 2
            game.getDifficultyMultiplier(),      // Velocidad actual (aumenta con el tiempo)
            // Mostrar estado: si estÃ¡ game over, mostrar instrucciÃ³n; si estÃ¡ jugando, mostrar "Playing"; si no ha empezado, mostrar instrucciÃ³n de inicio
            game.gameOver ? "GAME OVER (R to restart)" : (game.gameStarted ? "Playing" : "W/SPACE to start")
        );
        // Cambiar el tÃ­tulo de la ventana GLFW al nuevo string
        GLFW.glfwSetWindowTitle(window, title);
    }

    /**
     * Liberar todos los recursos antes de cerrar el programa.
     * Es importante hacerlo en orden inverso a como se crearon.
     */
    private void cleanup() {
        // Liberar texturas, VAOs, VBOs, shaders (todo lo que estÃ¡ en GPU)
        renderer.cleanup();

        // Destruir la ventana GLFW
        GLFW.glfwDestroyWindow(window);

        // Finalizar GLFW y liberar sus recursos
        GLFW.glfwTerminate();
    }

    /**
     * Punto de entrada del programa.
     * Crea una instancia de FlappyBirdGame y comienza la ejecuciÃ³n.
     */
    public static void main(String[] args) {
        // Crear nuevo juego e iniciar la ejecuciÃ³n (init -> loop -> cleanup)
        new FlappyBirdGame().run();
    }
}

