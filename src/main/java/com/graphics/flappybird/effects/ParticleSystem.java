package com.graphics.flappybird.effects;

// ===== IMPORTS: Dependencias necesarias =====
import java.util.ArrayList;    // Lista dinámica para almacenar partículas
import java.util.Iterator;     // Iterador seguro para eliminar elementos durante iteración
import java.util.List;         // Interfaz colección List
import java.util.Random;       // Generador de números aleatorios
import com.graphics.flappybird.services.IParticleService;  // Interfaz del servicio
import com.graphics.flappybird.rendering.Renderer;         // Renderizador OpenGL

/**
 * ============================================================================
 * ParticleSystem: Sistema de partículas para efectos visuales dinámicos
 * ============================================================================
 *
 * ¿QUÉ ES?
 * Un sistema que crea, actualiza y renderiza cientos de partículas pequeñas
 * para simular efectos como explosiones, polvo, lluvia de puntos, etc.
 *
 * RESPONSABILIDADES:
 * 1. Crear partículas en posiciones específicas con velocidad aleatoria
 * 2. Actualizar cada partícula: física (gravedad), vida (desvanecimiento)
 * 3. Renderizar todas las partículas como puntos coloreados
 * 4. Eliminar automáticamente partículas que se "murieron"
 *
 * CARACTERÍSTICAS CLAVE:
 * - Gravedad realista: 0.5 unidades/s² (caída suave)
 * - Desvanecimiento: Alpha disminuye linealmente con la vida
 * - Pool dinámico: Solo usa memoria para partículas vivas (eficiente)
 * - Variabilidad: Cada partícula tiene velocidad/color ligeramente aleatorio
 * - Thread-safe: Iterator permite eliminar durante iteración
 *
 * IMPLEMENTACIÓN DE INTERFAZ:
 * Implementa IParticleService para acceso vía Service Locator Pattern.
 * Esto desacopla ParticleSystem de otros componentes.
 *
 * MÉTODOS PÚBLICOS:
 * - burst(x, y, r, g, b, count): Explosión radial (pájaro muere)
 * - jumpDust(x, y): Polvo hacia arriba (pájaro salta)
 * - scorePopup(x, y): Puntos flotando (anotó)
 * - update(deltaTime): Actualizar todas las partículas
 * - render(renderer): Dibujar todas las partículas
 * - getActiveCount(): Cuántas vivas ahora
 */
public class ParticleSystem implements IParticleService {
    // ===== ATRIBUTOS DE INSTANCIA =====

    // Lista que almacena todas las partículas VIVAS en este momento
    // Se modifica cada frame: se agregan nuevas, se eliminan muertas
    // ArrayList: O(1) lectura, O(1) agregar al final, O(n) eliminar
    private List<Particle> particles;

    // Generador de aleatoriedad para:
    // - Velocidad inicial (dirección y magnitud)
    // - Variación de color (no todas idénticas)
    // - Posición inicial (pequeña dispersión)
    private Random random;

    /**
     * ============================================================================
     * CONSTRUCTOR: ParticleSystem()
     * ============================================================================
     * Se llama UNA SOLA VEZ cuando se registra el servicio en el Service Locator.
     *
     * Línea por línea:
     * 1. new ArrayList<>(): Crea lista vacía para partículas
     * 2. new Random(): Crea generador con semilla diferente cada ejecución
     */
    public ParticleSystem() {
        // Inicializar lista vacía (capacidad inicial ~10, crece automáticamente)
        particles = new ArrayList<>();

        // Inicializar generador aleatorio (semilla = System.nanoTime() por defecto)
        random = new Random();
    }

    /**
     * ============================================================================
     * CLASE INTERNA: Particle (partícula individual)
     * ============================================================================
     *
     * PROPÓSITO:
     * Representa UNA partícula con su propia:
     * - Posición (x, y)
     * - Velocidad (velX, velY)
     * - Ciclo de vida (life, maxLife)
     * - Apariencia (r, g, b, size)
     *
     * POR QUÉ INTERNA?
     * Es un tipo de dato que solo ParticleSystem necesita.
     * Evita que otras clases instancien Particle directamente.
     *
     * POR QUÉ STATIC?
     * No necesita acceso a atributos de ParticleSystem (particles, random).
     * static = una sola copia en memoria, más eficiente.
     */
    private static class Particle {
        // ===== PROPIEDADES FÍSICAS =====

        // Posición actual en coordenadas OpenGL normalizadas [-1, 1]
        // x: -1 = izquierda, 0 = centro, 1 = derecha
        // y: -1 = abajo, 0 = centro, 1 = arriba
        float x, y;

        // Velocidad actual: cuántas unidades se mueve por segundo
        // velX: velocidad horizontal (puede ser negativa = izquierda)
        // velY: velocidad vertical (negativa = cae, positiva = sube)
        // Se modifica cada frame por gravedad
        float velX, velY;

        // ===== CICLO DE VIDA =====

        // Vida restante: de 0.0 (muerta) a 1.0 (recién nacida)
        // Disminuye cada frame: life -= deltaTime * 2.0f
        // Cuando life <= 0.0, se elimina de la lista
        float life;

        // Vida inicial: usado para calcular transparencia (alpha)
        // alpha = life / maxLife (1.0 cuando nace, 0.0 cuando muere)
        // Esto crea desvanecimiento suave: opaco → transparente
        float maxLife;

        // ===== PROPIEDADES VISUALES =====

        // Color RGB de la partícula (0.0 a 1.0 por canal)
        // r: rojo, g: verde, b: azul
        // Típicamente: color del pájaro o verde (puntos)
        float r, g, b;

        // Tamaño visual de la partícula (radio en unidades OpenGL)
        // Rango típico: 0.01 a 0.05
        // En renderizado: dibuja un cuadrado de (size*2) x (size*2)
        float size;

        /**
         * ========================================================================
         * CONSTRUCTOR: Particle(parámetros)
         * ========================================================================
         * Crea una partícula CON VALORES INICIALES.
         * Los parámetros vienen de burst(), jumpDust(), scorePopup().
         *
         * @param x Posición X inicial
         * @param y Posición Y inicial
         * @param velX Velocidad X inicial (píxeles/segundo)
         * @param velY Velocidad Y inicial (píxeles/segundo)
         * @param life Duración total (segundos, típico 0.6 a 1.0)
         * @param r Componente rojo (0.0 a 1.0)
         * @param g Componente verde (0.0 a 1.0)
         * @param b Componente azul (0.0 a 1.0)
         * @param size Radio visual (típico 0.015 a 0.03)
         */
        Particle(float x, float y, float velX, float velY, float life,
                 float r, float g, float b, float size) {
            // ===== ASIGNAR POSICIÓN =====
            this.x = x;                    // Posición X (no cambia en constructor)
            this.y = y;                    // Posición Y (no cambia en constructor)

            // ===== ASIGNAR VELOCIDAD =====
            this.velX = velX;              // Velocidad X (cambia por gravedad en update())
            this.velY = velY;              // Velocidad Y (cambia por gravedad en update())

            // ===== ASIGNAR CICLO DE VIDA =====
            this.life = life;              // Vida actual = vida inicial (100%)
            this.maxLife = life;           // Guardar para calcular alpha más tarde

            // ===== ASIGNAR COLOR =====
            this.r = r;                    // Rojo
            this.g = g;                    // Verde
            this.b = b;                    // Azul

            // ===== ASIGNAR TAMAÑO =====
            this.size = size;              // Radio de la partícula
        }

        /**
         * ========================================================================
         * MÉTODO: void update(float deltaTime)
         * ========================================================================
         * Actualiza la partícula cada frame.
         *
         * ORDEN DE OPERACIONES:
         * 1. Aplicar gravedad a velocidad
         * 2. Mover según velocidad
         * 3. Reducir vida (desvanecimiento)
         * 4. Clamp vida (no permitir negativo)
         *
         * IMPORTANCIA:
         * Este método se llama 60 veces por segundo.
         * deltaTime ≈ 0.0167 segundos (1/60 FPS)
         *
         * @param deltaTime Tiempo transcurrido desde último frame (segundos)
         */
        void update(float deltaTime) {
            // ===== PASO 1: APLICAR GRAVEDAD A LA VELOCIDAD =====
            // Gravedad simula caída: acelera hacia abajo
            // 0.5 = constante de gravedad (suave)
            // Ecuación: velY_nuevo = velY_anterior - (gravedad × deltaTime)
            // Ejemplo con deltaTime = 0.0167:
            //   Si velY = 0.5 (sube), después: velY = 0.5 - 0.00835 = 0.49165
            //   Si velY = -0.5 (cae), después: velY = -0.5 - 0.00835 = -0.50835
            // Resultado: sube más lentamente, cae más rápidamente
            velY -= 0.5f * deltaTime;

            // ===== PASO 2: MOVER LA PARTÍCULA SEGÚN VELOCIDAD =====
            // Movimiento lineal simple: posición += velocidad × tiempo
            // deltaTime escala el movimiento independientemente del FPS
            // Si velX = 1.0 y dt = 0.0167, se mueve 0.0167 unidades
            x += velX * deltaTime;

            // Lo mismo para Y
            // Si velY = -1.0 (cae) y dt = 0.0167, Y disminuye 0.0167
            y += velY * deltaTime;

            // ===== PASO 3: REDUCIR VIDA (DESVANECIMIENTO) =====
            // Disminuir vida linealmente cada frame
            // Tasa: 2.0 / segundo (life × 2.0 por segundo)
            // Ejemplo: life = 1.0 inicialmente
            //   Después 0.25s: life = 1.0 - (0.25 × 2.0) = 0.5
            //   Después 0.50s: life = 1.0 - (0.50 × 2.0) = 0.0 (muere)
            // Total: cada partícula vive ~0.5 segundos antes de morir
            life -= deltaTime * 2.0f;

            // ===== PASO 4: CLAMP (ASEGURAR NO NEGATIVO) =====
            // life nunca debe ser menor a 0.0
            // Razón: en renderizado usamos alpha = life / maxLife
            // Si life < 0, alpha sería negativo (inválido)
            if (life < 0.0f) life = 0.0f;
        }

        /**
         * ========================================================================
         * MÉTODO: boolean estaViva()
         * ========================================================================
         * Pregunta simple: ¿sigue viva esta partícula?
         *
         * RETORNO:
         * true = life > 0.0 (sigue siendo visible)
         * false = life <= 0.0 (debe eliminarse)
         *
         * USO:
         * Se llama en ParticleSystem.update() para decidir si mantener o eliminar
         *
         * @return true si life > 0.0, false si está muerta
         */
        boolean estaViva() {
            // Lógica simple: cualquier vida restante = partícula viva
            // Una partícula "muere" cuando su vida llega a 0
            return life > 0.0f;
        }
    }

    // ===== MÉTODOS PÚBLICOS: CREAR PARTÍCULAS =====

    /**
     * ========================================================================
     * MÉTODO: void burst(x, y, r, g, b, count)
     * ========================================================================
     * Crea una EXPLOSIÓN radial de partículas.
     *
     * USO:
     * Cuando un pájaro muere: ServiceLocator.particles().burst(...)
     * Efecto visual: Partículas salen en TODAS las direcciones desde el centro
     *
     * PARÁMETROS:
     * @param x Centro X de la explosión
     * @param y Centro Y de la explosión
     * @param r Color rojo del pájaro (0.0-1.0)
     * @param g Color verde del pájaro (0.0-1.0)
     * @param b Color azul del pájaro (0.0-1.0)
     * @param count Cuántas partículas crear (típico: 15)
     */
    @Override
    public void burst(float x, float y, float r, float g, float b, int count) {
        // ===== BUCLE: Crear 'count' partículas =====
        for (int i = 0; i < count; i++) {
            // PASO 1: Calcular ÁNGULO aleatorio (0 a 360°)
            // random.nextFloat() retorna [0.0, 1.0)
            // × 2π = [0, 2π) radianes
            // Resultado: cada partícula va en dirección diferente
            float angle = random.nextFloat() * (float) (2 * Math.PI);

            // PASO 2: Calcular VELOCIDAD aleatoria (0.5 a 2.0)
            // random.nextFloat() = [0.0, 1.0)
            // 0.5 + [0.0, 1.0) × 1.5 = [0.5, 2.0)
            // Resultado: partículas salen con diferentes velocidades
            float speed = 0.5f + random.nextFloat() * 1.5f;

            // PASO 3: Descomponer ángulo + velocidad en velX, velY
            // cos(angle) = componente X, sin(angle) = componente Y
            // speed × cos(angle) = velX escalado
            // speed × sin(angle) = velY escalado
            float velX = (float) Math.cos(angle) * speed;
            float velY = (float) Math.sin(angle) * speed;

            // PASO 4: Crear color LIGERAMENTE VARIABLE
            // Color base ± pequeño offset aleatorio (0.1 rango)
            // Esto evita que todas las partículas sean EXACTAMENTE iguales
            // Random de -0.1 a 0.1: random.nextFloat() × 0.2 - 0.1
            float varR = r + random.nextFloat() * 0.2f - 0.1f;
            float varG = g + random.nextFloat() * 0.2f - 0.1f;
            float varB = b + random.nextFloat() * 0.2f - 0.1f;

            // PASO 5: Crear la PARTÍCULA y agregarla a la lista
            // Parámetros:
            // - x, y: Centro de la explosión
            // - velX, velY: Velocidad calculada
            // - 1.0f: Vida inicial (máxima, durará ~0.5 segundos)
            // - varR, varG, varB: Color variable
            // - 0.03f a 0.05f: Tamaño (rectángulo pequeño)
            particles.add(new Particle(x, y, velX, velY, 1.0f,
                                      varR, varG, varB,
                                      0.03f + random.nextFloat() * 0.02f));
        }
    }

    /**
     * ========================================================================
     * MÉTODO: void jumpDust(x, y)
     * ========================================================================
     * Crea polvo HACIA ARRIBA cuando el pájaro salta.
     *
     * USO:
     * Cada vez que presionas W/SPACE: ServiceLocator.particles().jumpDust(...)
     * Efecto visual: Pequeñas partículas salen hacia ARRIBA (principalmente)
     *
     * @param x Posición X del pájaro
     * @param y Posición Y del pájaro
     */
    @Override
    public void jumpDust(float x, float y) {
        // ===== CREAR 5 PARTÍCULAS DE POLVO =====
        for (int i = 0; i < 5; i++) {
            // PASO 1: ÁNGULO entre 90° y 180° (arriba y lados)
            // Math.PI × 0.5 = 90° (recto arriba)
            // + random × 0.5 × Math.PI = extra 0° a 90°
            // Resultado: [90°, 180°] = cuadrante superior
            float angle = (float) Math.PI * (0.5f + random.nextFloat() * 0.5f);

            // PASO 2: VELOCIDAD para polvo (0.3 a 0.8)
            float speed = 0.3f + random.nextFloat() * 0.5f;

            // PASO 3: Descomponer en componentes
            float velX = (float) Math.cos(angle) * speed;
            // Factor aleatorio ±1: hace que algunas partículas vayan izq/der
            velX *= (random.nextBoolean() ? 1 : -1);
            float velY = (float) Math.sin(angle) * speed;

            // PASO 4: Agregar partícula
            // Color blanco (polvo)
            // Vida corta: 0.6 segundos
            particles.add(new Particle(x, y, velX, velY, 0.6f,
                                      0.9f, 0.9f, 0.9f, 0.015f));
        }
    }

    /**
     * ========================================================================
     * MÉTODO: void scorePopup(x, y)
     * ========================================================================
     * Crea PUNTOS FLOTANTES cuando anotas.
     *
     * USO:
     * Cada vez que pasas una tubería: ServiceLocator.particles().scorePopup(...)
     * Efecto visual: Partículas VERDES salen hacia ARRIBA (celebración)
     *
     * @param x Posición X donde anotas (suele ser posición del pájaro)
     * @param y Posición Y donde anotas
     */
    @Override
    public void scorePopup(float x, float y) {
        // ===== CREAR 8 PARTÍCULAS FLOTANTES =====
        for (int i = 0; i < 8; i++) {
            // PASO 1: ÁNGULO aleatorio (todas las direcciones)
            float angle = random.nextFloat() * (float) (2 * Math.PI);

            // PASO 2: VELOCIDAD lenta (0.2 a 0.5)
            float speed = 0.2f + random.nextFloat() * 0.3f;

            // PASO 3: Descomponer
            float velX = (float) Math.cos(angle) * speed;
            // Pero siempre hacia ARRIBA (Y positivo)
            // 0.4 + random × 0.3 = [0.4, 0.7] (sube bastante)
            float velY = 0.4f + random.nextFloat() * 0.3f;

            // PASO 4: Agregar partícula
            // Color VERDE (0.0, 1.0, 0.5)
            // Vida: 1.0 segundos (dura un poco más que otras)
            // Tamaño: 0.02 (mediano, visible como "punto")
            particles.add(new Particle(x, y, velX, velY, 1.0f,
                                      0.0f, 1.0f, 0.5f, 0.02f));
        }
    }

    // ===== MÉTODOS PÚBLICOS: ACTUALIZAR Y RENDERIZAR =====

    /**
     * ========================================================================
     * MÉTODO: void update(float deltaTime)
     * ========================================================================
     * Actualiza TODAS las partículas vivas y elimina las muertas.
     *
     * LLAMADAS POR:
     * Game.update() cada frame
     *
     * LÓGICA:
     * 1. Recorrer lista de partículas
     * 2. Llamar update() en cada una (gravedad, movimiento, vida)
     * 3. Si está muerta, eliminarla de la lista
     *
     * POR QUÉ USAR ITERATOR?
     * No puedes hacer list.remove() en un for normal.
     * Iterator permite eliminar durante iteración sin errores.
     *
     * @param deltaTime Tiempo desde último frame (segundos)
     */
    @Override
    public void update(float deltaTime) {
        // ===== USAR ITERATOR PARA ITERACIÓN SEGURA =====
        // Iterator permite modificar la lista durante el bucle
        Iterator<Particle> it = particles.iterator();

        // ===== BUCLE: PROCESAR CADA PARTÍCULA =====
        while (it.hasNext()) {
            // Obtener siguiente partícula
            Particle p = it.next();

            // Actualizar posición, velocidad y vida
            p.update(deltaTime);

            // Si murió, eliminarla de la lista
            if (!p.estaViva()) {
                it.remove();  // Iterator.remove() es seguro
            }
        }
    }

    /**
     * ========================================================================
     * MÉTODO: void render(Renderer renderer)
     * ========================================================================
     * Dibuja TODAS las partículas en pantalla.
     *
     * LLAMADAS POR:
     * Renderer.render() cada frame
     *
     * PARA CADA PARTÍCULA:
     * 1. Calcular alpha (transparencia) = life / maxLife
     * 2. Llamar renderer.drawParticle() con posición, color, alpha
     *
     * TRANSPARENCIA:
     * alpha = 1.0 cuando nace (opaco)
     * alpha = 0.0 cuando muere (invisible)
     * Esto crea desvanecimiento suave: la partícula "se desvanece"
     *
     * @param renderer Renderizador OpenGL para dibujar
     */
    @Override
    public void render(Renderer renderer) {
        // ===== BUCLE SIMPLE: DIBUJAR CADA PARTÍCULA VIVA =====
        for (Particle p : particles) {
            // Calcular TRANSPARENCIA (alpha)
            // Alpha = vida actual / vida máxima
            // Si life = maxLife (recién creada): alpha = 1.0 (opaco)
            // Si life = maxLife/2: alpha = 0.5 (semi-transparente)
            // Si life → 0: alpha → 0.0 (casi invisible)
            float alpha = p.life / p.maxLife;

            // Llamar al renderizador para dibujar esta partícula
            // Parámetros:
            // - p.x, p.y: Posición
            // - p.size: Radio visual
            // - p.r, p.g, p.b: Color RGB
            // - alpha: Transparencia (0.0 a 1.0)
            renderer.drawParticle(p.x, p.y, p.size, p.r, p.g, p.b, alpha);
        }
    }

    /**
     * ========================================================================
     * MÉTODO: int getActiveCount()
     * ========================================================================
     * Retorna cuántas partículas están vivas AHORA MISMO.
     *
     * PROPÓSITO:
     * Útil para DEBUG: ver cuántas partículas hay en memoria
     * También para limitar máximo (evitar lag si hay demasiadas)
     *
     * IMPLEMENTACIÓN:
     * Solo retorna el tamaño de la lista de partículas
     * Una línea simple
     *
     * @return Número de partículas vivas actualmente
     */
    @Override
    public int getActiveCount() {
        // size() = cantidad de elementos en la lista
        // Solo partículas vivas están en la lista (muertas se eliminan)
        return particles.size();
    }
}
