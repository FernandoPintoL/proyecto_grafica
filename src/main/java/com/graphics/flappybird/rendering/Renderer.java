package com.graphics.flappybird.rendering;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.graphics.flappybird.core.Bird;
import com.graphics.flappybird.core.Game;
import com.graphics.flappybird.core.Pipe;
import com.graphics.flappybird.services.ServiceLocator;

/**
 * Renderer: maneja todo el renderizado grÃ¡fico con mejoras visuales.
 * - Fondo, pÃ¡jaros, tuberÃ­as, HUD, partÃ­culas.
 * - Soporta transparencia y animaciÃ³n.
 */
public class Renderer {
    // programa: ID del shader program (programa OpenGL compilado con vértice y fragmento shaders).
    // Este programa define cómo se rasteriza la geometría en la pantalla.
    private int programa;

    // vao: Vertex Array Object. Almacena la configuración de los atributos de vértices.
    // Describe qué datos de vértices (posiciones) y cómo interpretar el VBO.
    private int vao;

    // vbo: Vertex Buffer Object. Almacena los datos reales de vértices en GPU (QUAD_VERTICES).
    // Contiene un quad reutilizable (6 vértices para 2 triángulos).
    private int vbo;

    // Uniforms: variables de shader que se actualizan cada draw call.
    // uOffsetLocation: ubicación del uniform vec2 uOffset (posición en pantalla).
    // uScaleLocation: ubicación del uniform vec2 uScale (tamaño del objeto).
    // uColorLocation: ubicación del uniform vec3 uColor (color RGB del objeto).
    // uAlphaLocation: ubicación del uniform float uAlpha (transparencia 0.0-1.0).
    private int uOffsetLocation;
    private int uScaleLocation;
    private int uColorLocation;
    private int uAlphaLocation;

    // tiempoGlobal: contador de tiempo global (en segundos aprox).
    // Se incrementa cada frame (~0.016s a 60 FPS) para animar nubes, alas, etc.
    private float tiempoGlobal = 0.0f;

    // QUAD_VERTICES: quad unitario centrado. Se reutiliza para dibujar todos los rectángulos con scale/offset.
    // Estructura: 6 vértices (2 triángulos) = posición X, Y, Z para cada vértice.
    // El quad está en espacio normalizado [-0.5, 0.5], se escala y traslada con uniformes.
    // Triángulo 1: (-0.5,-0.5) (0.5,-0.5) (0.5,0.5)
    // Triángulo 2: (-0.5,-0.5) (0.5,0.5) (-0.5,0.5)
    private static final float[] QUAD_VERTICES = {
        -0.5f, -0.5f, 0.0f,
         0.5f, -0.5f, 0.0f,
         0.5f,  0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
         0.5f,  0.5f, 0.0f,
        -0.5f,  0.5f, 0.0f
    };

    // Constructor: inicializa el sistema de renderizado.
    public Renderer() {
        // crearShaders(): compila y linkea los shader programs (vertex + fragment).
        // También configura los uniforms y el blending (transparencia).
        crearShaders();

        // crearQuad(): genera VAO y VBO con QUAD_VERTICES, listo para reutilizar.
        crearQuad();

        // ServiceLocator.provideFont(): registra TextureFont en el localizador.
        // Permite que otros sistemas accedan al servicio de texto (HUD, pantallas).
        ServiceLocator.provideFont(new TextureFont());
    }

    /**
     * Crea shaders con soporte para transparencia (alpha).
     * Compila GLSL 3.30 shaders y configura el programa para renderizado.
     */
    private void crearShaders() {
        // VERTEX SHADER: transforma vértices de espacio modelo a espacio pantalla.
        // Recibe:
        // - aPos: posición del vértice (x, y, z) desde el VBO.
        // - uOffset: traslación (x, y) del objeto (uniforme).
        // - uScale: escala (width, height) del objeto (uniforme).
        // Calcula:
        // - finalPos = aPos.xy * uScale + uOffset: aplica escala y traslación.
        // - gl_Position: posición final en rango [-1, 1] normalizado de OpenGL.
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

        // FRAGMENT SHADER: calcula el color final de cada píxel (fragmento).
        // Recibe:
        // - uColor: color RGB (uniforme, rango [0, 1]).
        // - uAlpha: valor de transparencia (uniforme, rango [0, 1]).
        // Asigna:
        // - fragColor: salida = color RGB con alpha para blending.
        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            uniform float uAlpha;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, uAlpha);
            }
            """;

        // Crear y compilar VERTEX SHADER.
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        checkShader(vertexShader, "Vertex"); // Verifica si compiló correctamente.

        // Crear y compilar FRAGMENT SHADER.
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        checkShader(fragmentShader, "Fragment"); // Verifica si compiló correctamente.

        // Crear programa y linkar shaders.
        programa = GL20.glCreateProgram(); // Crea programa vacío.
        GL20.glAttachShader(programa, vertexShader); // Adjunta vertex shader.
        GL20.glAttachShader(programa, fragmentShader); // Adjunta fragment shader.
        GL20.glLinkProgram(programa); // Linka los shaders en un programa ejecutable.

        // Verifica si linkear fue exitoso.
        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar: " + GL20.glGetProgramInfoLog(programa));
        }

        // Obtener ubicación de cada uniform para poder actualizarlo en draw calls.
        // Estas ubicaciones se cachejan para uso rápido en drawRectAlpha, etc.
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");
        uAlphaLocation = GL20.glGetUniformLocation(programa, "uAlpha");

        // Eliminar shaders individuales (ya están vinculados al programa).
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        // Activar blending (mezcla de colores) para transparencia.
        // GL_BLEND: permite que píxeles nuevos se mezclen con los existentes.
        GL11.glEnable(GL11.GL_BLEND);
        // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA: fórmula estándar de alpha blending.
        // Resultado = nuevoColor * alpha + colorExistente * (1 - alpha).
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    // Verifica si un shader compiló correctamente. Si falla, lanza excepción con el error.
    // Parámetro shader: ID del shader compilado.
    // Parámetro tipo: nombre del shader ("Vertex" o "Fragment") para mensaje de error.
    private void checkShader(int shader, String tipo) {
        // GL20.glGetShaderi(): obtiene el estado de compilación del shader.
        // GL20.GL_COMPILE_STATUS: retorna GL11.GL_FALSE si hay error de compilación.
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            // GL20.glGetShaderInfoLog(): obtiene el mensaje de error detallado.
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    /**
     * Crea el quad base reutilizable (VAO + VBO).
     * Este quad se usa para todos los rectángulos, círculos (con custom geometry), etc.
     * El quad está en espacio local [-0.5, 0.5], se transforma con uniformes.
     */
    private void crearQuad() {
        // GL30.glGenVertexArrays(): genera un ID único para el VAO.
        // El VAO almacena la configuración de atributos de vértices.
        vao = GL30.glGenVertexArrays();
        // GL30.glBindVertexArray(vao): activa el VAO para configuración subsecuente.
        GL30.glBindVertexArray(vao);

        // GL15.glGenBuffers(): genera un ID único para el VBO.
        // El VBO almacena datos de vértices en la GPU.
        vbo = GL15.glGenBuffers();
        // GL15.glBindBuffer(): activa el VBO para que subidas de datos vayan aquí.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // BufferUtils.createFloatBuffer(): crea buffer Java (FloatBuffer).
        // Necesario para copiar datos a GPU con glBufferData.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
        // buffer.put(QUAD_VERTICES).flip(): copia datos y ajusta posición para lectura.
        buffer.put(QUAD_VERTICES).flip();
        // GL15.glBufferData(): copia datos a GPU. GL_STATIC_DRAW: datos no cambian frecuentemente.
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // GL20.glVertexAttribPointer(): define estructura de datos de vértices.
        // Parámetros: (índice=0, tamaño=3 floats, tipo=GL_FLOAT, normalizado=false, stride=12 bytes, offset=0)
        // Índice 0: coincide con "layout (location = 0) in vec3 aPos" en vertex shader.
        // Stride 12: (3 floats × 4 bytes/float) = 12 bytes entre vértices.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        // GL20.glEnableVertexAttribArray(0): activa el atributo 0 (aPos).
        // Sin esto, los shaders no recibirían datos de vértices.
        GL20.glEnableVertexAttribArray(0);

        // Desvincular buffers (buena práctica para evitar modificaciones accidentales).
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Renderiza el frame completo del juego.
     * Orquesta todas las llamadas de dibujo en el orden correcto (layering).
     * Parámetros:
     * - game: objeto Game con lógica (pájaros, tuberías, etc).
     * - windowWidth, windowHeight: dimensiones de la ventana (no usadas en este caso).
     */
    public void render(Game game, int windowWidth, int windowHeight) {
        // Incrementar tiempo global (~60 FPS = 0.016s por frame).
        // Se usa para animar nubes, alas de pájaros, colas, etc.
        tiempoGlobal += 0.016f;

        // Limpiar el buffer de color. Establecer color de fondo = cielo azul claro.
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f); // RGB claro + Alpha opaco.
        // GL11.GL_COLOR_BUFFER_BIT: limpia el buffer de color (descarta frame anterior).
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Activar el shader program para todas las operaciones posteriores.
        GL20.glUseProgram(programa);
        // Activar el VAO quad para que glDrawArrays() use estos vértices.
        GL30.glBindVertexArray(vao);

        // LAYER 1: Fondo (cielo con degradado, montañas, suelo).
        // Dibujado primero (más atrás en profundidad).
        drawBackgroundGradient();

        // LAYER 2: Nubes (paralaje, animadas).
        // Se dibuja sobre el fondo.
        drawAnimatedClouds();

        // LAYER 3: Tuberías (obstáculos).
        // Iteración sobre lista de tuberías activas.
        for (Pipe p : game.pipes) {
            drawPipe(p);
        }

        // LAYER 4: Sombras de pájaros (efecto de profundidad, debajo del pájaro).
        drawBirdShadow(game.bird1);
        drawBirdShadow(game.bird2);

        // LAYER 5: Pájaros geométricos (cuerpo, alas, pico, ojos).
        // Dibujado sobre todo lo anterior (más al frente).
        drawBirdGeometric(game.bird1);
        drawBirdGeometric(game.bird2);

        // LAYER 6: Partículas (explosiones, polvo de salto, popup de score).
        // Se accede al servicio de partículas registrado en ServiceLocator.
        // Partículas renderean sobre pájaros.
        ServiceLocator.particles().render(this);

        // LAYER 7: HUD (paneles de puntuación, multiplicador de dificultad).
        drawHUD(game, windowWidth, windowHeight);

        // LAYER 8: Pantalla de inicio.
        // Solo se dibuja si el juego aún no ha comenzado (gameStarted == false).
        if (!game.gameStarted) {
            drawStartScreen(windowWidth, windowHeight);
        }

        // LAYER 9: Pantalla de game over.
        // Solo se dibuja después de que el juego empezó AND terminó (gameOver == true).
        if (game.gameStarted && game.gameOver) {
            drawGameOverScreen(game, windowWidth, windowHeight);
        }
    }

    /**
     * Dibuja un rectángulo simple (opaco, alpha = 1.0).
     * Conveniencia: llama a drawRectAlpha con alpha fijo = 1.0f.
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b) {
        drawRectAlpha(x, y, width, height, r, g, b, 1.0f);
    }

    /**
     * Dibuja un círculo usando triangle fan (GL_TRIANGLE_FAN).
     * El círculo se construye como múltiples triángulos desde un centro.
     *
     * Parámetros:
     * - centerX, centerY: posición del centro del círculo.
     * - radiusX, radiusY: radios en X e Y (radiusX == radiusY para círculo perfecto).
     * - segments: número de subdivisiones (más = más suave, 12-20 usualmente suficiente).
     * - r, g, b: color RGB del círculo.
     */
    public void drawCircle(float centerX, float centerY, float radiusX, float radiusY,
                          int segments, float r, float g, float b) {
        drawCircleAlpha(centerX, centerY, radiusX, radiusY, segments, r, g, b, 1.0f);
    }

    /**
     * Dibuja un círculo con transparencia variable.
     * Genera vértices dinámicamente usando trigonometría.
     */
    public void drawCircleAlpha(float centerX, float centerY, float radiusX, float radiusY,
                               int segments, float r, float g, float b, float alpha) {
        // Crear array de vértices: 1 centro + segments puntos en circunferencia.
        // Cada vértice = (x, y, z), así que total = (segments + 2) * 3 floats.
        float[] vertices = new float[(segments + 2) * 3];

        // Vértice 0 = centro del triángulo fan.
        vertices[0] = centerX;
        vertices[1] = centerY;
        vertices[2] = 0.0f;

        // Generar puntos en la circunferencia usando senos y cosenos.
        // Ángulo va de 0 a 2π radianes (círculo completo).
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0f * Math.PI * i / segments); // Ángulo en radianes.
            float x = centerX + (float) Math.cos(angle) * radiusX; // X en circunferencia.
            float y = centerY + (float) Math.sin(angle) * radiusY; // Y en circunferencia.
            vertices[(i + 1) * 3] = x;
            vertices[(i + 1) * 3 + 1] = y;
            vertices[(i + 1) * 3 + 2] = 0.0f;
        }

        // Renderizar usando triangle fan (centro + circunferencia).
        drawCustomGeometry(vertices, GL11.GL_TRIANGLE_FAN, r, g, b, alpha);
    }

    /**
     * Dibuja un triángulo (opaco).
     * Conveniencia: llama a drawTriangleAlpha con alpha = 1.0f.
     */
    public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3,
                            float r, float g, float b) {
        drawTriangleAlpha(x1, y1, x2, y2, x3, y3, r, g, b, 1.0f);
    }

    /**
     * Dibuja un triángulo con transparencia.
     * Parámetros: tres puntos (x1,y1), (x2,y2), (x3,y3) y color RGBA.
     */
    public void drawTriangleAlpha(float x1, float y1, float x2, float y2, float x3, float y3,
                                 float r, float g, float b, float alpha) {
        // Crear array de 3 vértices (triángulo).
        float[] vertices = {
            x1, y1, 0.0f,
            x2, y2, 0.0f,
            x3, y3, 0.0f
        };
        // Renderizar como GL_TRIANGLES (1 triángulo por cada 3 vértices).
        drawCustomGeometry(vertices, GL11.GL_TRIANGLES, r, g, b, alpha);
    }

    /**
     * Dibuja geometría personalizada usando vértices dinámicos.
     * Se usa internamente para círculos y triángulos.
     * Genera temporalmente un VAO/VBO, renderiza y limpia recursos.
     *
     * Parámetros:
     * - vertices: array de floats [x1, y1, z1, x2, y2, z2, ...].
     * - mode: tipo de primitiva (GL_TRIANGLES, GL_TRIANGLE_FAN, etc).
     * - r, g, b, alpha: color RGBA del objeto.
     */
    private void drawCustomGeometry(float[] vertices, int mode, float r, float g, float b, float alpha) {
        // Crear VAO temporal para esta geometría.
        int customVAO = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(customVAO);

        // Crear VBO temporal y cargar vértices.
        int customVBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, customVBO);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Configurar atributo de vértices (3 floats por vértice = posición).
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Configurar uniforms para color y transparencia.
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL20.glUniform1f(uAlphaLocation, alpha);
        // Para geometría personalizada, offset=0 y scale=1 (sin transformación).
        GL20.glUniform2f(uOffsetLocation, 0, 0);
        GL20.glUniform2f(uScaleLocation, 1, 1);

        // Renderizar: dibuja "vertices.length / 3" vértices usando el modo especificado.
        GL11.glDrawArrays(mode, 0, vertices.length / 3);

        // Limpiar recursos (liberar VBO y VAO temporales).
        GL15.glDeleteBuffers(customVBO);
        GL30.glDeleteVertexArrays(customVAO);

        // Reactivar el VAO quad principal para operaciones posteriores.
        GL30.glBindVertexArray(vao);
    }

    /**
     * Dibuja un rectángulo con transparencia.
     * Método de bajo nivel que actualiza uniforms y dibuja el quad base.
     *
     * Parámetros:
     * - x, y: posición del centro.
     * - width, height: tamaño (se aplican como escala).
     * - r, g, b, alpha: color RGBA.
     */
    private void drawRectAlpha(float x, float y, float width, float height, float r, float g, float b, float alpha) {
        // Actualizar uniform de offset (posición en pantalla).
        GL20.glUniform2f(uOffsetLocation, x, y);
        // Actualizar uniform de scale (tamaño del rectángulo).
        GL20.glUniform2f(uScaleLocation, width, height);
        // Actualizar uniform de color RGB.
        GL20.glUniform3f(uColorLocation, r, g, b);
        // Actualizar uniform de transparencia.
        GL20.glUniform1f(uAlphaLocation, alpha);
        // Dibujar: 6 vértices = 2 triángulos = 1 quad.
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /**
     * Dibuja una partícula individual.
     * Las partículas son pequeños cuadrados de color con transparencia.
     *
     * Parámetros:
     * - x, y: posición del centro.
     * - size: radio (width/height = size*2).
     * - r, g, b, alpha: color RGBA de la partícula.
     */
    public void drawParticle(float x, float y, float size, float r, float g, float b, float alpha) {
        // Renderizar como rectángulo de ancho y alto = size*2.
        drawRectAlpha(x, y, size * 2, size * 2, r, g, b, alpha);
    }

    /**
     * Fondo mejorado con degradado profesional, montañas y paralaje.
     * Crea sensación de profundidad mediante múltiples capas que se mueven a velocidades diferentes.
     */
    private void drawBackgroundGradient() {
        // ===== CIELO CON DEGRADADO SUAVE =====
        // Se dibuja el cielo en 3 capas horizontales de colores progresivos.
        // Esto crea un efecto degradado visual sin usar texturas.

        // Capa superior del cielo: Azul más claro (parte superior/horizonte).
        // Posición Y=0.75f (arriba), altura=0.5f, ancho=2.0f (pantalla completa).
        drawRect(0.0f, 0.75f, 2.0f, 0.5f, ColorPalette.SKY_TOP_R, ColorPalette.SKY_TOP_G, ColorPalette.SKY_TOP_B);

        // Capa media del cielo: Azul intermedio (transición).
        drawRect(0.0f, 0.45f, 2.0f, 0.3f, ColorPalette.SKY_MID_R, ColorPalette.SKY_MID_G, ColorPalette.SKY_MID_B);

        // Capa inferior del cielo: Azul más cálido (atardecer suave).
        // Más cerca del horizonte, tono más cálido.
        drawRect(0.0f, 0.15f, 2.0f, 0.3f, ColorPalette.SKY_BOT_R, ColorPalette.SKY_BOT_G, ColorPalette.SKY_BOT_B);

        // ===== MONTAÑAS EN EL FONDO (Parallax capa 1 - MÁS LENTA) =====
        // Las montañas lejanas se mueven lentamente para efecto de profundidad.
        // Multiplicador 0.02f: muy lenta (lejana).
        float mountOffset1 = (tiempoGlobal * 0.02f) % 2.0f;
        // Modulo 2.0f: cuando offset llega a 2.0, vuelve a 0 (wrapping para animación infinita).

        // Montaña lejana izquierda: triángulo con vértices base en Y=0.15f, pico en Y=0.5f.
        // Color verde oscuro + semi-transparente (alpha=0.3f) para efecto de distancia.
        drawTriangleAlpha(-0.8f + mountOffset1, 0.15f,
                         -0.3f + mountOffset1, 0.5f,
                         0.2f + mountOffset1, 0.15f,
                         ColorPalette.MOUNT_DISTANT_R, ColorPalette.MOUNT_DISTANT_G, ColorPalette.MOUNT_DISTANT_B, 0.3f);

        // Montaña lejana derecha: similar a la izquierda pero desplazada.
        drawTriangleAlpha(0.7f + mountOffset1, 0.15f,
                         1.2f + mountOffset1, 0.45f,
                         1.7f + mountOffset1, 0.15f,
                         ColorPalette.MOUNT_DISTANT_R, ColorPalette.MOUNT_DISTANT_G, ColorPalette.MOUNT_DISTANT_B, 0.3f);

        // ===== MONTAÑAS MÁS CERCANAS (Parallax capa 2 - MÁS RÁPIDA) =====
        // Estas montañas se mueven más rápido (multiplicador 0.035f).
        // Aparecen más grande y oscuro (alpha=0.4f, más opaco que las lejanas).
        float mountOffset2 = (tiempoGlobal * 0.035f) % 2.5f;

        // Primera montaña cercana (izquierda).
        drawTriangleAlpha(-1.0f + mountOffset2, 0.05f,
                         -0.4f + mountOffset2, 0.35f,
                         0.2f + mountOffset2, 0.05f,
                         ColorPalette.MOUNT_NEAR_R, ColorPalette.MOUNT_NEAR_G, ColorPalette.MOUNT_NEAR_B, 0.4f);

        // Segunda montaña cercana (derecha).
        drawTriangleAlpha(0.5f + mountOffset2, 0.05f,
                         1.1f + mountOffset2, 0.40f,
                         1.7f + mountOffset2, 0.05f,
                         ColorPalette.MOUNT_NEAR_R, ColorPalette.MOUNT_NEAR_G, ColorPalette.MOUNT_NEAR_B, 0.4f);

        // ===== SUELO HERBOSO (FOREGROUND) =====
        // El suelo es el elemento más cercano (sin paralaje, no se mueve).
        // Se compone de 3 capas para efecto de profundidad.

        // Línea de hierba superior (verde más claro, borde de suelo).
        // Y = -0.58f (casi en la parte inferior), altura pequeña para efecto de bordo.
        drawRect(0.0f, -0.58f, 2.0f, 0.08f, ColorPalette.GRASS_TOP_R, ColorPalette.GRASS_TOP_G, ColorPalette.GRASS_TOP_B);

        // Capa principal de suelo (verde oscuro, el cuerpo del terreno).
        // Altura mayor (0.17f) para ocupar la mayor parte del espacio inferior.
        drawRect(0.0f, -0.75f, 2.0f, 0.17f, ColorPalette.GRASS_MAIN_R, ColorPalette.GRASS_MAIN_G, ColorPalette.GRASS_MAIN_B);

        // Línea de suelo oscura (sombra/profundidad en el borde inferior).
        // Muy delgada (0.03f) en Y = -0.73f para efecto de sombra.
        drawRect(0.0f, -0.73f, 2.0f, 0.03f, ColorPalette.GRASS_SHADOW_R, ColorPalette.GRASS_SHADOW_G, ColorPalette.GRASS_SHADOW_B);

        // ===== DETALLES DE HIERBA (TEXTURAS PEQUEÑAS) =====
        // Pequeñas líneas/detalles de hierba distribuidas en el suelo.
        // Semi-transparentes (alpha=0.5f) para efecto de textura sutil.
        // Estas líneas varían en tamaño y posición para romper monotonía.

        // Línea de hierba izquierda-lejana.
        drawRectAlpha(-0.8f, -0.58f, 0.15f, 0.02f, ColorPalette.GRASS_DETAIL_R, ColorPalette.GRASS_DETAIL_G, ColorPalette.GRASS_DETAIL_B, 0.5f);

        // Línea de hierba centro-izquierda.
        drawRectAlpha(-0.3f, -0.58f, 0.12f, 0.02f, ColorPalette.GRASS_DETAIL_R, ColorPalette.GRASS_DETAIL_G, ColorPalette.GRASS_DETAIL_B, 0.5f);

        // Línea de hierba centro-derecha.
        drawRectAlpha(0.2f, -0.58f, 0.14f, 0.02f, ColorPalette.GRASS_DETAIL_R, ColorPalette.GRASS_DETAIL_G, ColorPalette.GRASS_DETAIL_B, 0.5f);

        // Línea de hierba derecha.
        drawRectAlpha(0.7f, -0.58f, 0.13f, 0.02f, ColorPalette.GRASS_DETAIL_R, ColorPalette.GRASS_DETAIL_G, ColorPalette.GRASS_DETAIL_B, 0.5f);
    }

    /**
     * Nubes mejoradas con formas más realistas y animación suave.
     * Cada nube está compuesta de múltiples círculos (óvalos) para efecto esponjoso.
     * Las nubes se mueven a diferentes velocidades (parallax) para efecto de profundidad.
     */
    private void drawAnimatedClouds() {
        // cloudOffset: desplazamiento base que anima todas las nubes.
        // Multiplicador 0.03f: velocidad de movimiento (lenta para efecto suave).
        // Modulo 3.0f: reinicia cuando llega a 3.0 (wrapping para animación continua).
        float cloudOffset = (tiempoGlobal * 0.03f) % 3.0f;

        // ===== NUBE 1: Grande y esponjosa (posición arriba) =====
        // Posición X inicial = -1.2f (izquierda), se suma cloudOffset para mover hacia derecha.
        float cloud1X = -1.2f + cloudOffset;
        // La nube se dibuja como 3 círculos (óvalos) conectados, creando efecto esponjoso.

        // Círculo izquierdo de la nube (más pequeño, más oscuro: alpha=0.85f).
        drawCircleAlpha(cloud1X - 0.15f, 0.72f, 0.12f, 0.08f, 12, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, 0.85f);
        // Círculo central (más grande, más brillante: alpha=0.9f).
        drawCircleAlpha(cloud1X + 0.0f, 0.75f, 0.14f, 0.09f, 12, ColorPalette.CLOUD_BRIGHT, ColorPalette.CLOUD_BRIGHT, ColorPalette.CLOUD_BRIGHT, 0.9f);
        // Círculo derecho (similar al izquierdo).
        drawCircleAlpha(cloud1X + 0.15f, 0.72f, 0.12f, 0.08f, 12, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, 0.85f);

        // ===== NUBE 2: Mediana (paralaje intermedio) =====
        // Multiplicador 0.65f: se mueve más lentamente que nube 1 (efecto de distancia).
        // Posición Y más baja (0.55f aprox), más transparente (alpha ~0.7f) para efecto lejano.
        float cloud2X = -0.5f + cloudOffset * 0.65f;

        // Tres círculos formando nube mediana.
        drawCircleAlpha(cloud2X - 0.1f, 0.55f, 0.09f, 0.06f, 10, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, 0.7f);
        drawCircleAlpha(cloud2X + 0.05f, 0.58f, 0.10f, 0.07f, 10, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, ColorPalette.CLOUD_LIGHT, 0.75f);
        drawCircleAlpha(cloud2X + 0.18f, 0.55f, 0.08f, 0.06f, 10, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, 0.68f);

        // ===== NUBE 3: Pequeña y ligera (paralaje rápido) =====
        // Multiplicador 0.8f: se mueve rápido (aparenta estar más cerca).
        // Restar cloudOffset: se mueve en dirección opuesta (variación visual).
        // Posición Y más alta (0.78f), menos transparente (alpha ~0.6f).
        float cloud3X = 0.6f - cloudOffset * 0.8f;

        // Dos círculos formando nube pequeña.
        drawCircleAlpha(cloud3X, 0.78f, 0.07f, 0.05f, 10, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, 0.6f);
        drawCircleAlpha(cloud3X + 0.1f, 0.80f, 0.08f, 0.05f, 10, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, ColorPalette.CLOUD_MEDIUM, 0.65f);

        // ===== NUBE 4: Lejana y semi-transparente =====
        // Multiplicador 0.4f: movimiento muy lento (muy lejana).
        // Posición Y central (0.68f), muy transparente (alpha ~0.4f) para efecto de lejanía.
        float cloud4X = 1.0f + cloudOffset * 0.4f;

        // Dos círculos formando nube lejana y difuminada.
        drawCircleAlpha(cloud4X - 0.08f, 0.68f, 0.10f, 0.06f, 10, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, 0.4f);
        drawCircleAlpha(cloud4X + 0.08f, 0.68f, 0.10f, 0.06f, 10, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, ColorPalette.CLOUD_SOFT, 0.35f);
    }

    /**
     * Dibuja sombra debajo del pájaro (efecto de profundidad).
     * La sombra añade sensación de que el pájaro está flotando sobre el suelo.
     */
    private void drawBirdShadow(Bird bird) {
        // No dibujar sombra si el pájaro está muerto.
        if (!bird.alive) return;

        // shadowX: posición horizontal = misma que el pájaro (directamente debajo).
        float shadowX = bird.x;

        // shadowY: posición vertical debajo del pájaro.
        // bird.height * 0.6f: distancia hacia abajo (más bajo = sombra más lejana).
        float shadowY = bird.y - bird.height * 0.6f;

        // shadowWidth: 60% del ancho del pájaro (sombra más estrecha que el cuerpo).
        float shadowWidth = bird.width * 0.6f;

        // shadowHeight: 15% de la altura del pájaro (sombra muy plana).
        float shadowHeight = bird.height * 0.15f;

        // Dibujar sombra como rectángulo negro semi-transparente.
        // Negro con alpha=0.15f (muy transparente, apenas visible).
        drawRectAlpha(shadowX, shadowY, shadowWidth, shadowHeight,
                      ColorPalette.SHADOW_R, ColorPalette.SHADOW_G, ColorPalette.SHADOW_B, 0.15f);
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
                 ColorPalette.EYE_BLACK_R, ColorPalette.EYE_BLACK_G, ColorPalette.EYE_BLACK_B);

        // Brillo en ojo (blanco).
        drawRect(eyeX + bird.width * 0.02f, eyeY + bird.height * 0.03f,
                 bird.width * 0.04f, bird.height * 0.05f,
                 ColorPalette.EYE_WHITE_R, ColorPalette.EYE_WHITE_G, ColorPalette.EYE_WHITE_B);

        // Contorno del ojo (blanco).
        drawRectAlpha(eyeX, eyeY, bird.width * 0.11f, bird.height * 0.16f,
                      ColorPalette.EYE_WHITE_R, ColorPalette.EYE_WHITE_G, ColorPalette.EYE_WHITE_B, 0.5f);
    }

    /**
     * Pájaro geométrico: compuesto por figuras elementales (círculos, triángulos, rectángulos).
     * Cumple requerimiento de examen: pájaro con pico, alas, cola y ojo.
     * El pájaro se anima mediante senos/cosenos para movimientos suaves.
     *
     * Componentes:
     * - Cuerpo: óvalo (círculo con radiusX != radiusY) principal + sombreado secundario.
     * - Pico: triángulo naranja apuntando hacia adelante.
     * - Alas: dos triángulos (superior e inferior) que animan con sin(tiempoGlobal).
     * - Cola: rectángulo + triángulo de pluma distintiva (mueve con tailWag).
     * - Ojo: círculo blanco (esclerótica) + iris negro + pupila brillante + párpado inferior.
     */
    public void drawBirdGeometric(Bird bird) {
        // No dibujar si el pájaro está muerto.
        if (!bird.alive) return;

        // Animación de alas: ondulación sinusoidal.
        // sin(tiempoGlobal * 8.0f): frecuencia alta = movimiento rápido (batir de alas).
        // Rango [-1, 1] se usa para desplazar vértices de las alas.
        float wingFlap = (float) Math.sin(tiempoGlobal * 8.0f);

        // Animación de cola: movimiento sinusoidal más lento.
        // Multiplicador 0.15f: amplitud de movimiento (cuánto se mueve la cola).
        float tailWag = (float) Math.sin(tiempoGlobal * 6.0f) * 0.15f;

        // ===== CUERPO: ÓVALO PRINCIPAL =====
        // Dimensiones del cuerpo.
        float bodyRadiusX = bird.width * 0.35f;
        float bodyRadiusY = bird.height * 0.4f;
        // Dibujar óvalo principal (color del pájaro, 20 segmentos para suavidad).
        drawCircleAlpha(bird.x, bird.y, bodyRadiusX, bodyRadiusY, 20,
                       bird.colorR, bird.colorG, bird.colorB, 1.0f);

        // ===== SOMBREADO DEL CUERPO =====
        // Círculo pequeño oscuro en la parte superior-derecha del cuerpo.
        // Efecto: profundidad/relieve (luz viniendo de arriba-derecha).
        drawCircleAlpha(bird.x + bodyRadiusX * 0.3f, bird.y - bodyRadiusY * 0.1f,
                       bodyRadiusX * 0.2f, bodyRadiusY * 0.2f, 12,
                       bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.4f);

        // ===== PICO: TRIÁNGULO NARANJA =====
        // El pico es un triángulo que apunta hacia la derecha (adelante del pájaro).
        // Vértices: punta (derecha), base superior, base inferior.
        float peakTipX = bird.x + bird.width * 0.38f;  // Punta del pico (hacia adelante).
        float peakTipY = bird.y + bird.height * 0.08f; // Punta ligeramente hacia arriba.
        float peakBaseX1 = bird.x + bird.width * 0.15f;
        float peakBaseY1 = bird.y + bird.height * 0.15f; // Base superior del pico.
        float peakBaseX2 = bird.x + bird.width * 0.15f;
        float peakBaseY2 = bird.y - bird.height * 0.08f; // Base inferior del pico.
        // Color naranja (1.0, 0.85, 0.1).
        drawTriangleAlpha(peakTipX, peakTipY, peakBaseX1, peakBaseY1, peakBaseX2, peakBaseY2,
                         ColorPalette.BIRD_BEAK_R, ColorPalette.BIRD_BEAK_G, ColorPalette.BIRD_BEAK_B, 1.0f);

        // ===== ALA SUPERIOR: TRIÁNGULO ANIMADO =====
        // El ala superior se mueve con wingFlap (0.1f amplitud).
        float wingX1 = bird.x - bird.width * 0.28f;
        float wingY1 = bird.y + bird.height * 0.3f + wingFlap * 0.1f; // Arriba.
        float wingX2 = bird.x - bird.width * 0.1f;
        float wingY2 = bird.y + bird.height * 0.5f; // Abajo.
        float wingX3 = bird.x - bird.width * 0.05f;
        float wingY3 = bird.y + bird.height * 0.25f; // Atrás.
        // Color: 60% más oscuro que el cuerpo (efecto de sombra).
        drawTriangleAlpha(wingX1, wingY1, wingX2, wingY2, wingX3, wingY3,
                         bird.colorR * 0.6f, bird.colorG * 0.6f, bird.colorB * 0.6f, 0.85f);

        // ===== ALA INFERIOR: TRIÁNGULO ANIMADO (OPUESTA) =====
        // Se mueve en dirección opuesta a la ala superior (efecto de batir sincronizado).
        float wing2X1 = bird.x - bird.width * 0.25f;
        float wing2Y1 = bird.y - bird.height * 0.25f - wingFlap * 0.08f; // Abajo cuando ala superior sube.
        float wing2X2 = bird.x - bird.width * 0.08f;
        float wing2Y2 = bird.y - bird.height * 0.4f; // Más abajo.
        float wing2X3 = bird.x - bird.width * 0.02f;
        float wing2Y3 = bird.y - bird.height * 0.15f; // Atrás.
        // Color: 50% más oscuro (más sombreado que ala superior).
        drawTriangleAlpha(wing2X1, wing2Y1, wing2X2, wing2Y2, wing2X3, wing2Y3,
                         bird.colorR * 0.5f, bird.colorG * 0.5f, bird.colorB * 0.5f, 0.75f);

        // ===== COLA: RECTÁNGULO ANIMADO =====
        // Cola rectangular que se mueve horizontalmente (tailWag).
        float tailX = bird.x - bird.width * 0.45f + tailWag; // Hacia atrás (izquierda).
        float tailY = bird.y - bird.height * 0.15f;
        // Tamaño: rectángulo delgado y largo.
        drawRectAlpha(tailX, tailY, bird.width * 0.12f, bird.height * 0.5f,
                     bird.colorR * 0.4f, bird.colorG * 0.4f, bird.colorB * 0.4f, 0.6f);

        // ===== PLUMA: TRIÁNGULO DISTINTIVO EN LA COLA =====
        // Triángulo pequeño que sobresale de la cola, animado con tailWag.
        float featherX1 = bird.x - bird.width * 0.5f;
        float featherY1 = bird.y - bird.height * 0.35f + tailWag;
        float featherX2 = bird.x - bird.width * 0.58f; // Más atrás.
        float featherY2 = bird.y - bird.height * 0.28f;
        float featherX3 = bird.x - bird.width * 0.45f;
        float featherY3 = bird.y - bird.height * 0.48f + tailWag;
        // Color muy oscuro (30% del color base).
        drawTriangleAlpha(featherX1, featherY1, featherX2, featherY2, featherX3, featherY3,
                         bird.colorR * 0.3f, bird.colorG * 0.3f, bird.colorB * 0.3f, 0.7f);

        // ===== OJO: ESCLERA (BLANCO) =====
        float eyeX = bird.x + bird.width * 0.15f;
        float eyeY = bird.y + bird.height * 0.15f;

        // Parte blanca del ojo (esclera): círculo blanco grande.
        drawCircleAlpha(eyeX, eyeY, bird.width * 0.065f, bird.height * 0.1f, 16,
                       ColorPalette.EYE_WHITE_R, ColorPalette.EYE_WHITE_G, ColorPalette.EYE_WHITE_B, 1.0f);

        // ===== OJO: IRIS (NEGRO) =====
        // Iris: círculo negro más pequeño, desplazado hacia la derecha (dirección de visión).
        drawCircleAlpha(eyeX + bird.width * 0.015f, eyeY, bird.width * 0.035f, bird.height * 0.065f, 14,
                       ColorPalette.EYE_BLACK_R, ColorPalette.EYE_BLACK_G, ColorPalette.EYE_BLACK_B, 1.0f);

        // ===== OJO: PUPILA (BRILLO BLANCO) =====
        // Punto brillante blanco en la pupila (efecto de luz/vida).
        drawCircleAlpha(eyeX + bird.width * 0.025f, eyeY + bird.height * 0.025f,
                       bird.width * 0.015f, bird.height * 0.025f, 8,
                       ColorPalette.EYE_WHITE_R, ColorPalette.EYE_WHITE_G, ColorPalette.EYE_WHITE_B, 0.9f);

        // ===== OJO: PÁRPADO INFERIOR =====
        // Triángulo pequeño debajo del ojo (forma natural de párpado).
        drawTriangleAlpha(eyeX - bird.width * 0.065f, eyeY + bird.height * 0.105f,
                         eyeX + bird.width * 0.065f, eyeY + bird.height * 0.105f,
                         eyeX, eyeY + bird.height * 0.115f,
                         bird.colorR * 0.3f, bird.colorG * 0.3f, bird.colorB * 0.3f, 0.5f);
    }

    /**
     * Dibuja una tubería con efecto 3D: bordes, sombreado y profundidad.
     * Cada tubería se compone de dos tubos (superior e inferior) con un espacio (gap) entre ellos.
     * Los tubos tienen bordes oscuros y claros para efecto de iluminación.
     */
    private void drawPipe(Pipe p) {
        // Calcular límites del gap (espacio por donde pasa el pájaro).
        // gapCentroY: posición Y del centro del espacio.
        // gapHeight: altura del espacio.
        float gapTop = p.gapCentroY + (p.gapHeight * 0.5f);    // Límite superior del gap.
        float gapBottom = p.gapCentroY - (p.gapHeight * 0.5f); // Límite inferior del gap.

        // Colores de tubería desde ColorPalette (verde estilo Mario con efecto 3D).
        float colorR = ColorPalette.PIPE_R;
        float colorG = ColorPalette.PIPE_G;
        float colorB = ColorPalette.PIPE_B;

        // Colores derivados para efecto 3D.
        float darkR = ColorPalette.PIPE_DARK_R;
        float darkG = ColorPalette.PIPE_DARK_G;
        float darkB = ColorPalette.PIPE_DARK_B;

        // Colores claros para efecto de luz.
        float lightR = ColorPalette.PIPE_LIGHT_R;
        float lightG = ColorPalette.PIPE_LIGHT_G;
        float lightB = ColorPalette.PIPE_LIGHT_B;

        // ===== TUBO SUPERIOR (arriba del gap) =====
        // Este tubo va desde el gap hasta la parte superior de la pantalla (Y=1.0).
        float altoSup = 1.0f - gapTop;
        if (altoSup > 0.0f) {
            // Posición Y del centro del tubo superior.
            float yCentroSup = gapTop + (altoSup * 0.5f);

            // ===== TUBO PRINCIPAL (verde sólido) =====
            drawRect(p.x, yCentroSup, p.width, altoSup, colorR, colorG, colorB);

            // ===== BORDES LATERALES (efecto 3D: luz y sombra) =====
            float borderWidth = p.width * 0.08f; // 8% del ancho del tubo.

            // Borde izquierdo (oscuro): simula la sombra del lado izquierdo.
            // Posición: borde izquierdo del tubo (-0.5 * width) desplazado un poco adentro.
            drawRectAlpha(p.x - p.width * 0.5f + borderWidth * 0.5f, yCentroSup,
                         borderWidth, altoSup, darkR, darkG, darkB, 0.6f);

            // Borde derecho (claro): simula la luz reflejada del lado derecho.
            // Posición: borde derecho del tubo (+0.5 * width) desplazado un poco adentro.
            drawRectAlpha(p.x + p.width * 0.5f - borderWidth * 0.5f, yCentroSup,
                         borderWidth, altoSup, lightR, lightG, lightB, 0.4f);

            // ===== BORDE SUPERIOR (entrada del tubo) =====
            // Línea horizontal oscura en el tope del tubo (donde comienza el espacio).
            float topBorderHeight = altoSup * 0.05f;
            float topY = gapTop + topBorderHeight * 0.5f;
            drawRectAlpha(p.x, topY, p.width + borderWidth * 0.5f, topBorderHeight,
                         darkR, darkG, darkB, 0.7f);

            // ===== EFECTO DE PROFUNDIDAD: LÍNEA INTERIOR OSCURA =====
            // Pequeña línea horizontal oscura cerca de gapTop para efecto de profundidad.
            // Simula la boca de entrada del tubo.
            drawRectAlpha(p.x, yCentroSup - altoSup * 0.48f, p.width * 0.7f, altoSup * 0.02f,
                         darkR * 0.8f, darkG * 0.8f, darkB * 0.8f, 0.5f);
        }

        // ===== TUBO INFERIOR (debajo del gap) =====
        // Este tubo va desde el gap hasta la parte inferior de la pantalla (Y=-1.0).
        float altoInf = gapBottom + 1.0f; // gapBottom es negativo, así que suma 1.0 para obtener altura.
        if (altoInf > 0.0f) {
            // Posición Y del centro del tubo inferior.
            // Tubo inferior está en Y negativo, su centro es más bajo.
            float yCentroInf = -1.0f + (altoInf * 0.5f);

            // ===== TUBO PRINCIPAL (verde sólido) =====
            drawRect(p.x, yCentroInf, p.width, altoInf, colorR, colorG, colorB);

            // ===== BORDES LATERALES (efecto 3D) =====
            float borderWidth = p.width * 0.08f;

            // Borde izquierdo (oscuro).
            drawRectAlpha(p.x - p.width * 0.5f + borderWidth * 0.5f, yCentroInf,
                         borderWidth, altoInf, darkR, darkG, darkB, 0.6f);

            // Borde derecho (claro).
            drawRectAlpha(p.x + p.width * 0.5f - borderWidth * 0.5f, yCentroInf,
                         borderWidth, altoInf, lightR, lightG, lightB, 0.4f);

            // ===== BORDE INFERIOR (salida del tubo) =====
            // Línea horizontal oscura en el tope del tubo inferior (donde comienza el espacio).
            float bottomBorderHeight = altoInf * 0.05f;
            float bottomY = gapBottom - bottomBorderHeight * 0.5f;
            drawRectAlpha(p.x, bottomY, p.width + borderWidth * 0.5f, bottomBorderHeight,
                         darkR, darkG, darkB, 0.7f);

            // ===== EFECTO DE PROFUNDIDAD: LÍNEA INTERIOR OSCURA =====
            // Pequeña línea horizontal oscura cerca de gapBottom para efecto de profundidad.
            drawRectAlpha(p.x, yCentroInf + altoInf * 0.48f, p.width * 0.7f, altoInf * 0.02f,
                         darkR * 0.8f, darkG * 0.8f, darkB * 0.8f, 0.5f);
        }

        // ===== GAP (Espacio entre tubos) =====
        // El espacio es invisible; esta línea es solo para debugging.
        // Se puede descomentar para visualizar la línea central del gap (útil para testing).
        float gapCenterLineOpacity = 0.0f; // Invisible, solo para debugging si quieres.
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

        // Pico: triángulo (aprox con pequeños rectángulos).
        float peakX = bird.x + bird.width * 0.3f;
        float peakY = bird.y;
        drawRect(peakX, peakY, bird.width * 0.2f, bird.height * 0.3f,
                 ColorPalette.BIRD_BEAK_R, ColorPalette.BIRD_BEAK_G, ColorPalette.BIRD_BEAK_B);

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
                 ColorPalette.EYE_WHITE_R, ColorPalette.EYE_WHITE_G, ColorPalette.EYE_WHITE_B);
    }

    /**
     * HUD: dibuja paneles con puntuación y multiplicador de dificultad.
     * Se dibuja siempre (incluso durante pantalla de inicio/game over).
     * Orden importante: primero fondos (paneles), luego números (texto) para que aparezcan al frente.
     */
    private void drawHUD(Game game, int windowWidth, int windowHeight) {
        // ===== PRIMERO: DIBUJAR FONDOS DE LOS PANELES =====
        // Esto garantiza que los números aparezcan al frente (z-order correcto).

        // Panel para PLAYER 1 (esquina superior izquierda).
        // Posición (-0.95, 0.88), tamaño (0.18, 0.08), alpha=0.85 (semi-transparente).
        drawRectAlpha(-0.95f, 0.88f, 0.18f, 0.08f, ColorPalette.HUD_P1_R, ColorPalette.HUD_P1_G, ColorPalette.HUD_P1_B, 0.85f);

        // Panel para PLAYER 2 (esquina superior derecha).
        // Similar al panel P1 pero en X=0.77f (derecha).
        drawRectAlpha(0.77f, 0.88f, 0.18f, 0.08f, ColorPalette.HUD_P2_R, ColorPalette.HUD_P2_G, ColorPalette.HUD_P2_B, 0.85f);

        // Panel para DIFICULTAD (centro superior).
        // Ancho más grande (0.24f) porque el número es más complejo.
        drawRectAlpha(-0.12f, 0.88f, 0.24f, 0.08f, ColorPalette.HUD_DIFF_R, ColorPalette.HUD_DIFF_G, ColorPalette.HUD_DIFF_B, 0.75f);

        // ===== DESPUÉS: RENDERIZAR LOS NÚMEROS ENCIMA DE LOS PANELES =====
        // Usar el servicio de fuente registrado en ServiceLocator.

        // Puntuación P1: renderizar en el panel izquierdo.
        // Parámetros: (valor, posX, posY, tamaño, colorR, colorG, colorB).
        ServiceLocator.font().renderNumber(game.bird1.score, -0.94f, 0.85f, 0.25f,
                                           ColorPalette.HUD_TEXT_R, ColorPalette.HUD_TEXT_G, ColorPalette.HUD_TEXT_B);

        // Puntuación P2: renderizar en el panel derecho.
        ServiceLocator.font().renderNumber(game.bird2.score, 0.78f, 0.85f, 0.25f,
                                           ColorPalette.HUD_TEXT_R, ColorPalette.HUD_TEXT_G, ColorPalette.HUD_TEXT_B);

        // Multiplicador de dificultad: redondeado a 1 decimal.
        // Ejemplo: 1.53 → redondea a 1.5, 2.0 → 2.0.
        // Fórmula: multiplicar por 10, redondear, dividir entre 10.
        int diffLevel = Math.round(game.getDifficultyMultiplier() * 10) / 10;
        // Renderizar en el panel central.
        ServiceLocator.font().renderNumber(diffLevel, -0.09f, 0.85f, 0.25f,
                                           ColorPalette.HUD_TEXT_R, ColorPalette.HUD_TEXT_G, ColorPalette.HUD_TEXT_B);
    }

    /**
     * Pantalla de inicio (LOBBY): muestra instrucciones, controles y objetivo del juego.
     * Se dibuja solo cuando gameStarted == false (antes de presionar W o SPACE).
     * Modalidad: modal (bloquea interacción con el juego, solo muestra instrucciones).
     */
    private void drawStartScreen(int windowWidth, int windowHeight) {
        // ===== REACTIVAR SHADER PROGRAM Y VAO =====
        // IMPORTANTE: TextureFont.renderText() puede desactivar el programa y VAO.
        // Reactivamos aquí para asegurar que drawRectAlpha funcione correctamente.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // ===== FONDO MODAL: RECTANGULO OSCURO SEMI-TRANSPARENTE =====
        // Cubre toda la pantalla (2.0 x 2.0) con color oscuro.
        // Alpha = 0.8 (80% opaco, permite ver el juego atrás de manera borrosa).
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, ColorPalette.MODAL_START_R, ColorPalette.MODAL_START_G, ColorPalette.MODAL_START_B, 0.8f);

        // TÃTULO
        // drawRectAlpha(-0.5f, 0.65f, 1.0f, 0.15f, 0.2f, 0.2f, 0.3f, 0.9f);
        ServiceLocator.font().renderText("FLAPPY BIRD 2P", -0.42f, 0.66f, 0.09f, 1.0f, 1.0f, 1.0f);

        // CONTROLES - P1
        // drawRectAlpha(-0.75f, 0.40f, 0.45f, 0.15f, 0.98f, 0.85f, 0.20f, 0.85f);
        ServiceLocator.font().renderText("PLAYER 1", -0.73f, 0.43f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("PRESIONE W", -0.73f, 0.35f, 0.06f, 0.95f, 0.95f, 0.95f);

        // CONTROLES - P2
        // drawRectAlpha(0.30f, 0.40f, 0.45f, 0.15f, 0.20f, 0.85f, 0.98f, 0.85f);
        ServiceLocator.font().renderText("PLAYER 2", 0.32f, 0.43f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("PRESIONE SPACE", 0.32f, 0.35f, 0.06f, 0.95f, 0.95f, 0.95f);

        // INSTRUCCIÃ“N PARA COMENZAR
        // drawRectAlpha(-0.5f, 0.18f, 1.0f, 0.12f, 0.7f, 0.6f, 0.2f, 0.8f);
        ServiceLocator.font().renderText("TOCA PARA INICIAR EL JUEGO", -0.45f, 0.20f, 0.075f, 0.2f, 0.2f, 0.2f);

        // OBJETIVO DEL JUEGO
        // drawRectAlpha(-0.5f, -0.05f, 1.0f, 0.15f, 1.2f, 1.5f, 1.8f, 0.75f);
        ServiceLocator.font().renderText("OBJETIVO: EVITAR TUBOS", -0.48f, 0.02f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("SOBREVIVE TODO EL TIEMPO QUE PUEDAS", -0.48f, -0.04f, 0.06f, 0.9f, 0.9f, 0.9f);

        // DIFICULTAD PROGRESIVA
        // drawRectAlpha(-0.5f, -0.35f, 1.0f, 0.15f, 0.8f, 0.4f, 0.2f, 0.75f);
        ServiceLocator.font().renderText("LA DIFICULTAD AUMENTA", -0.48f, -0.28f, 0.07f, 1.0f, 1.0f, 1.0f);
        ServiceLocator.font().renderText("GANA PUNTOS PASANDO HUECOS", -0.48f, -0.34f, 0.06f, 0.9f, 0.9f, 0.9f);
    }

    /**
     * Pantalla de game over: muestra puntuaciones de ambos jugadores.
     * Se dibuja solo cuando gameStarted == true AND gameOver == true.
     * Modalidad: modal (bloquea interacción, permite ver las puntuaciones y reiniciar).
     */
    private void drawGameOverScreen(Game game, int windowWidth, int windowHeight) {
        // ===== REACTIVAR SHADER PROGRAM Y VAO =====
        // IMPORTANTE: TextureFont.renderText() puede desactivar el programa y VAO.
        // Reactivamos aquí para asegurar que drawRectAlpha funcione correctamente.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // ===== FONDO MODAL: RECTÁNGULO ROJO SEMI-TRANSPARENTE =====
        // Cubre toda la pantalla (2.0 x 2.0) con color rojo.
        // Alpha = 0.85 (85% opaco, da sensación de urgencia/derrota).
        drawRectAlpha(0.0f, 0.0f, 2.0f, 2.0f, ColorPalette.MODAL_GAMEOVER_R, ColorPalette.MODAL_GAMEOVER_G, ColorPalette.MODAL_GAMEOVER_B, 0.85f);

        // ===== TÍTULO: GAME OVER =====
        // Texto grande en blanco, tamaño=0.08.
        // (Fondo comentado: drawRectAlpha(-0.6f, 0.55f, ...))
        ServiceLocator.font().renderText("JUEGO TERMINADO", -0.48f, 0.565f, 0.08f, 1.0f, 1.0f, 1.0f);

        // ===== PUNTUACIÓN PLAYER 1 =====
        // Muestra la puntuación final del pájaro 1.
        // Concatenación de string: "P1: " + puntuación.
        // (Fondo comentado: drawRectAlpha(-0.6f, 0.30f, ...))
        ServiceLocator.font().renderText("P1: " + game.bird1.score, -0.55f, 0.355f, 0.07f, 1.0f, 1.0f, 1.0f);

        // ===== PUNTUACIÓN PLAYER 2 =====
        // Muestra la puntuación final del pájaro 2.
        // Concatenación de string: "P2: " + puntuación.
        // (Fondo comentado: drawRectAlpha(0.05f, 0.30f, ...))
        ServiceLocator.font().renderText("P2: " + game.bird2.score, 0.12f, 0.355f, 0.07f, 1.0f, 1.0f, 1.0f);

        // ===== INSTRUCCIÓN: PRESIONAR R PARA REINICIAR =====
        // Texto en la parte inferior indicando cómo reiniciar el juego.
        // (Fondo comentado: drawRectAlpha(-0.6f, 0.05f, ...))
        ServiceLocator.font().renderText("PRESIONA R PARA REINICIAR", -0.48f, 0.075f, 0.06f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Limpia recursos de GPU: VAO, VBO, programa y servicio de fuente.
     * Se llama cuando la aplicación se cierra o reinicia.
     * IMPORTANTE: Llamar esto es necesario para evitar memory leaks en GPU.
     */
    public void cleanup() {
        // Eliminar Vertex Array Object de la GPU.
        GL30.glDeleteVertexArrays(vao);

        // Eliminar Vertex Buffer Object de la GPU.
        GL15.glDeleteBuffers(vbo);

        // Eliminar shader program de la GPU.
        GL20.glDeleteProgram(programa);

        // Limpiar recursos del servicio de fuente (texturas de caracteres, etc).
        ServiceLocator.font().cleanup();
    }
}
