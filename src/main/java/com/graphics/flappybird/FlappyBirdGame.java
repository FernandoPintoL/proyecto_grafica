package com.graphics.flappybird;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.graphics.flappybird.audio.SoundManager;
import com.graphics.flappybird.core.Game;
import com.graphics.flappybird.rendering.Renderer;
import com.graphics.flappybird.services.ServiceLocator;

/**
 * =============================================================================
 * EJEMPLO MÍNIMO DE OPENGL (para quien no sabe nada de gráficos 3D)
 * =============================================================================
 *
 * Este programa hace 3 cosas:
 *   1. Abre una ventana (usando GLFW, una biblioteca que crea ventanas).
 *   2. Define un triángulo y unos "shaders" (programas que corren en la GPU).
 *   3. En un bucle, dibuja el triángulo en la ventana una y otra vez.
 *
 * CONCEPTOS CLAVE:
 *   - OpenGL: conjunto de funciones para dibujar en la pantalla usando la GPU.
 *   - GLFW: biblioteca que crea la ventana y maneja teclado/ratón. OpenGL solo dibuja;
 *     no crea ventanas, por eso usamos GLFW.
 *   - GPU: tarjeta gráfica. Los "shaders" son pequeños programas que se ejecutan ahí.
 *   - Shader: código que dice cómo transformar vértices (vertex shader) y qué color
 *     dar a cada píxel (fragment shader). Se escriben en un lenguaje parecido a C (GLSL).
 *   - Vértice: un punto en 3D (x, y, z). Un triángulo tiene 3 vértices.
 *   - VAO/VBO: formas de guardar en la GPU los datos de los vértices (posiciones, etc.).
 */

/**
 * FlappyBirdGame: Flappy Bird para 2 jugadores con visuales mejorados.
 * Optimizado para máximo rendimiento sin lag.
 */
public class FlappyBirdGame {
    // Identificador único de la ventana GLFW (se crea en init())
    private long window;

    // Instancia del motor de juego que maneja la lógica (colisiones, física, estado)
    private Game game;

    // Instancia del renderizador que dibuja todo en la pantalla usando OpenGL
    private Renderer renderer;

    // Dimensiones de la ventana en píxeles
    private int windowWidth = 900;
    private int windowHeight = 700;

    // Variables para detectar cambios de estado en el teclado (flanco = cambio de false a true).
    // Se usan para detectar solo la presión (no mantener presionado constantemente)
    private boolean prevW = false;      // Estado anterior de la tecla W (Jugador 1)
    private boolean prevSpace = false;  // Estado anterior de la tecla SPACE (Jugador 2)
    private boolean prevUp = false;     // Estado anterior de la tecla UP ARROW (Jugador 3)
    private boolean prevR = false;      // Estado anterior de la tecla R (Reiniciar)
    private boolean musicStopped = false; // Flag para detener la música solo una vez

    // Método principal que ejecuta el ciclo de vida del programa
    public void run() {
        init();      // Paso 1: Inicializar GLFW, OpenGL, ventana y recursos
        loop();      // Paso 2: Bucle principal (procesa input, actualiza logica, renderiza)
        cleanup();   // Paso 3: Liberar recursos (ventana, texturas, shaders, memoria)
    }

    /**
     * Inicializa GLFW, OpenGL y recursos.
     * Este método configura todo lo necesario antes de entrar al bucle principal.
     */
    private void init() {
        // Inicializar GLFW (librería para gestionar ventanas y entrada)
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        // Resetear todas las opciones de ventana a sus valores por defecto
        GLFW.glfwDefaultWindowHints();

        // Configurar opciones de la ventana ANTES de crearla:
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);         // Ventana oculta al inicio (la mostramos después)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);        // Permitir redimensionar la ventana
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);        // Usar OpenGL versión 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);        // (3 = major, 3 = minor)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE); // Usar Core Profile (sin funciones antiguas)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);        // Compatible con versiones futuras

        // Crear la ventana con las dimensiones y título especificados
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight,
                                        "Flappy Bird - 3 Players (P1: W, P2: SPACE, P3: UP ARROW)", 0, 0);
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
        // Actualiza el viewport de OpenGL para que renderice en el área correcta
        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            GL11.glViewport(0, 0, width, height);
        });

        // Hacer que esta ventana sea el contexto actual (donde se renderiza)
        GLFW.glfwMakeContextCurrent(window);

        // Habilitar V-Sync (sincronizar con pantalla a 60 FPS)
        GLFW.glfwSwapInterval(1);

        // Mostrar la ventana (ahora que está completamente configurada)
        GLFW.glfwShowWindow(window);

        // Cargar todas las funciones de OpenGL disponibles en esta plataforma
        GL.createCapabilities();

        // Registrar el servicio de audio en el Service Locator (antes de crear Game y Renderer)
        ServiceLocator.provideAudio(new SoundManager());

        // Crear instancia del motor de juego (lógica, física, colisiones)
        game = new Game();

        // Crear instancia del renderizador (dibuja todo)
        renderer = new Renderer();

        // Actualizar el título de la ventana con información inicial del juego
        updateWindowTitle();
    }

    /**
    * Bucle principal: ejecuta continuamente mientras la ventana esté abierta.
    * Es el corazón del programa: procesa input, actualiza lógica y renderiza cada frame.
     */
    private void loop() {
        // Obtener el tiempo actual en segundos (para calcular deltaTime)
        float lastTime = (float) GLFW.glfwGetTime();
        // musicStopped es ahora una variable de clase (field) para acceso desde processInput()

        // Continuar mientras la ventana NO haya recibido señal de cierre (ESC o botón X)
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Obtener el tiempo actual en este frame
            float now = (float) GLFW.glfwGetTime();

            // deltaTime = tiempo transcurrido desde el último frame (en segundos)
            // Ejemplo: si corre a 60 FPS, deltaTime ≈ 0.0167 segundos
            float deltaTime = now - lastTime;

            // Guardar el tiempo actual para el próximo frame
            lastTime = now;

            // Limitar deltaTime para evitar saltos grandes (por lag o pauses)
            // Máximo 0.033 segundos = mínimo 30 FPS implícito
            // Esto previene que la física "salte" demasiado si hay un pico de lag
            if (deltaTime > 0.033f) {
                deltaTime = 0.033f;
            }

            // FASE 1: Procesar entrada del usuario (detectar teclas presionadas)
            processInput();

            // FASE 2: Actualizar la lógica del juego (movimiento, colisiones, física)
            game.update(deltaTime);

            // Detener la música cuando el juego termina (solo una vez)
            if (game.gameOver && !musicStopped) {
                ServiceLocator.audio().stopBackgroundMusic();
                musicStopped = true;
            }

            // Reset para el próximo juego
            if (!game.gameStarted && musicStopped) {
                musicStopped = false;
            }

            // FASE 3: Dibujar todo en la pantalla (aves, tuberías, HUD, partículas)
            renderer.render(game, windowWidth, windowHeight);

            // Intercambiar buffers: mostrar lo que se dibujó en esta iteración
            // (OpenGL dibuja en un buffer invisible y lo muestra cuando se llama esto)
            GLFW.glfwSwapBuffers(window);

            // Procesar eventos del sistema (redimensionar ventana, input, etc.)
            GLFW.glfwPollEvents();

            // Actualizar el título de la ventana con puntuaciones y estado actual
            updateWindowTitle();
        }
    }

    /**
      * Procesa la entrada del usuario usando detección de flanco.
     * "Flanco" = cambio de estado (de no presionado a presionado).
     * Esto permite detectar un solo "click" aunque se mantenga presionada la tecla.
     */
    private void processInput() {
        // ============ JUGADOR 1: Tecla W para saltar ============
          // Obtener estado ACTUAL de la tecla W (true si está presionada, false si no)
        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;

        // Detectar flanco de subida: "wNow" es true Y "prevW" era false
        // Esto solo ocurre en el frame donde la tecla se presiona por primera vez
        if (wNow && !prevW) {
            if (!game.gameStarted) {
                // Si el juego aún no ha comenzado, iniciarlo
                game.start();
                // Iniciar música de fondo cuando comienza el juego
                ServiceLocator.audio().playBackgroundMusic();
            } else if (game.bird1.alive) {
                // Si el juego está corriendo y el jugador 1 está vivo:
                game.bird1.jump();                              // Hacer que el ave salte
                ServiceLocator.particles().jumpDust(game.bird1.x, game.bird1.y); // Crear partículas de polvo
                ServiceLocator.audio().playJumpSound();                   // Reproducir sonido de salto
            }
        }
        // Guardar el estado actual para comparar en el próximo frame
        prevW = wNow;

        // ============ JUGADOR 2: Tecla SPACE para saltar ============
        // Mismo patrón que jugador 1 pero con tecla diferente
        boolean spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

        if (spaceNow && !prevSpace) {
            if (!game.gameStarted) {
                game.start();
                // Iniciar música de fondo cuando comienza el juego
                ServiceLocator.audio().playBackgroundMusic();
            } else if (game.bird2.alive) {
                game.bird2.jump();
                // ServiceLocator.particles().jumpDust(game.bird2.x, game.bird2.y);
                ServiceLocator.audio().playJumpSound();
            }
        }
        prevSpace = spaceNow;

        // ============ JUGADOR 3: Tecla UP ARROW para saltar ============
        // Mismo patrón que jugadores anteriores con tecla diferente
        boolean upNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;

        if (upNow && !prevUp) {
            if (!game.gameStarted) {
                game.start();
                // Iniciar música de fondo cuando comienza el juego
                ServiceLocator.audio().playBackgroundMusic();
            } else if (game.bird3.alive) {
                game.bird3.jump();
                // ServiceLocator.particles().jumpDust(game.bird3.x, game.bird3.y);
                ServiceLocator.audio().playJumpSound();
            }
        }
        prevUp = upNow;

        // ============ REINICIO: Tecla R para reiniciar el juego ============
        // Obtener estado ACTUAL de la tecla R
        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;

        // Detectar flanco: R presionada AHORA, no lo era antes, Y el juego está en estado "GAME OVER"
        if (rNow && !prevR && game.gameOver) {
            // Detener música de fondo cuando se reinicia el juego
            ServiceLocator.audio().stopBackgroundMusic();
            game.reset();  // Reiniciar el juego a su estado inicial
            // Reiniciar el flag de música para permitir que vuelva a sonar
            musicStopped = false;
        }
        prevR = rNow;
    }

    /**
     * Actualiza el título de la ventana con información en tiempo real.
     * Se llama cada frame para mostrar puntuaciones, velocidad y estado actual.
     */
    private void updateWindowTitle() {
        // Construir un string con formato que incluya:
        // - Puntuación de los tres jugadores
        // - Multiplicador de velocidad (dificultad progresiva)
        // - Estado del juego (Starting, Playing, o Game Over)
        String title = String.format(
            "Flappy Bird - P1: %d | P2: %d | P3: %d | Speed: %.1fx | %s",
            game.bird1.score,                    // Puntuación jugador 1
            game.bird2.score,                    // Puntuación jugador 2
            game.bird3.score,                    // Puntuación jugador 3
            game.getDifficultyMultiplier(),      // Velocidad actual (aumenta con el tiempo)
            // Mostrar estado: si está game over, mostrar instrucción; si está jugando, mostrar "Playing"; si no ha empezado, mostrar instrucción de inicio
            game.gameOver ? "GAME OVER (R to restart)" : (game.gameStarted ? "Playing" : "W/SPACE/UP to start")
        );
        // Cambiar el título de la ventana GLFW al nuevo string
        GLFW.glfwSetWindowTitle(window, title);
    }

    /**
     * Liberar todos los recursos antes de cerrar el programa.
     * Es importante hacerlo en orden inverso a como se crearon.
     */
    private void cleanup() {
        // Liberar texturas, VAOs, VBOs, shaders (todo lo que está en GPU)
        renderer.cleanup();

        // Destruir la ventana GLFW
        GLFW.glfwDestroyWindow(window);

        // Finalizar GLFW y liberar sus recursos
        GLFW.glfwTerminate();
    }

    /**
     * Punto de entrada del programa.
    * Crea una instancia de FlappyBirdGame y comienza la ejecución.
     */
    public static void main(String[] args) {
        // Crear nuevo juego e iniciar la ejecución (init -> loop -> cleanup)
        new FlappyBirdGame().run();
    }
}

