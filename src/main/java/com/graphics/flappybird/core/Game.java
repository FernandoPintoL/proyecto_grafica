package com.graphics.flappybird.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.graphics.flappybird.effects.ParticleSystem;
import com.graphics.flappybird.services.ServiceLocator;

/**
 * Game: lógica central del Flappy Bird para tres jugadores.
 * Gestiona pájaros, tuberías, colisiones, puntaje y dificultad progresiva.
 */
public class Game {
    // Pájaros (jugadores).
    public Bird bird1;
    public Bird bird2;
    public Bird bird3;

    // Puntuación objetivo para terminar el juego
    public static final int TARGET_SCORE = 7;

    // Tuberías activas.
    public List<Pipe> pipes;

    private Random random;

    // Parámetros de tuberías.
    private static final float TUBERIA_ANCHO = 0.18f;
    private static final float GAP_ALTO_BASE = 0.48f;
    private static final float VELOCIDAD_TUBERIAS_BASE = 0.62f;
    private static final float TIEMPO_ENTRE_TUBERIAS_BASE = 1.5f;
    private static final float GAP_MIN_CENTRO = -0.45f;
    private static final float GAP_MAX_CENTRO = 0.45f;

    // Parámetro de tamaño del pájaro.
    private static final float BIRD_ANCHO = 0.10f;
    private static final float BIRD_ALTO = 0.10f;

    // Spawn positions.
    private static final float BIRD1_X = -0.55f; // Izquierda.
    private static final float BIRD2_X = 0.15f;  // Centro.
    private static final float BIRD3_X = 0.85f;  // Derecha.
    private static final float BIRD_START_Y = 0.0f;

    // Estados del juego.
    public boolean gameStarted;
    public boolean gameOver;
    private float timerSpawn;

    // Dificultad progresiva.
    private float difficultyMultiplier = 1.0f;

    public Game() {
        // Crear pájaros con colores distintos.
        bird1 = new Bird(BIRD1_X, BIRD_START_Y, BIRD_ANCHO, BIRD_ALTO,
                         0.98f, 0.85f, 0.20f); // Naranja
        bird2 = new Bird(BIRD2_X, BIRD_START_Y, BIRD_ANCHO, BIRD_ALTO,
                         0.20f, 0.85f, 0.98f); // Azul
        bird3 = new Bird(BIRD3_X, BIRD_START_Y, BIRD_ANCHO, BIRD_ALTO,
                         0.20f, 0.98f, 0.20f); // Verde

        pipes = new ArrayList<>();
        // Registrar el servicio de partículas en el Service Locator
        ServiceLocator.provideParticles(new ParticleSystem());
        random = new Random();
        gameStarted = false;
        gameOver = false;
        timerSpawn = 0.0f;
    }

    /**
     * Reinicia el juego.
     */
    public void reset() {
        bird1.reset(BIRD1_X, BIRD_START_Y);
        bird2.reset(BIRD2_X, BIRD_START_Y);
        bird3.reset(BIRD3_X, BIRD_START_Y);
        pipes.clear();
        gameStarted = false;
        gameOver = false;
        timerSpawn = 0.0f;
        difficultyMultiplier = 1.0f;
    }

    /**
     * Comienza el juego.
     */
    public void start() {
        gameStarted = true;
        bird1.jump();
        bird2.jump();
        bird3.jump();
    }

    /**
     * Actualiza la lógica del juego.
     */
    public void update(float deltaTime) {
        // Actualizar partículas siempre.
        ServiceLocator.particles().update(deltaTime);

        if (!gameStarted || gameOver) {
            return;
        }

        // Actualizar pájaros.
        boolean bird1WasAlive = bird1.alive;
        boolean bird2WasAlive = bird2.alive;
        boolean bird3WasAlive = bird3.alive;

        bird1.update(deltaTime);
        bird2.update(deltaTime);
        bird3.update(deltaTime);

        // Efecto de partículas y sonido cuando un pájaro muere.
        if (bird1WasAlive && !bird1.alive) {
            ServiceLocator.particles().burst(bird1.x, bird1.y, bird1.colorR, bird1.colorG, bird1.colorB, 15);
            ServiceLocator.audio().playCollisionSound();
        }
        if (bird2WasAlive && !bird2.alive) {
            ServiceLocator.particles().burst(bird2.x, bird2.y, bird2.colorR, bird2.colorG, bird2.colorB, 15);
            ServiceLocator.audio().playCollisionSound();
        }
        if (bird3WasAlive && !bird3.alive) {
            ServiceLocator.particles().burst(bird3.x, bird3.y, bird3.colorR, bird3.colorG, bird3.colorB, 15);
            ServiceLocator.audio().playCollisionSound();
        }

        // Actualizar dificultad según puntaje máximo.
        int maxScore = Math.max(bird1.score, Math.max(bird2.score, bird3.score));
        float prevMultiplier = difficultyMultiplier;
        difficultyMultiplier = 1.0f + (maxScore * 0.05f); // +5% por punto.
        if (difficultyMultiplier > 2.0f) {
            difficultyMultiplier = 2.0f; // Límite máximo.
            // Alerta sonora cuando se alcanza velocidad máxima.
            if (prevMultiplier < 2.0f) {
                ServiceLocator.audio().playSpeedWarning();
            }
        }

        // Spawn de tuberías.
        float spawnInterval = TIEMPO_ENTRE_TUBERIAS_BASE / difficultyMultiplier;
        timerSpawn += deltaTime;
        if (timerSpawn >= spawnInterval) {
            timerSpawn = 0.0f;
            spawnPipe();
        }

        // Actualizar tuberías.
        float pipeSpeed = VELOCIDAD_TUBERIAS_BASE * difficultyMultiplier;
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.update(pipeSpeed, deltaTime);

            // Puntaje: cuando la tubería pasa cada pájaro (independiente para cada uno).
            // Se suma SOLO si el pájaro está vivo y cuando la tubería pasa sobre él
            if (bird1.alive && p.x - (TUBERIA_ANCHO * 0.5f) < BIRD1_X && p.x + (TUBERIA_ANCHO * 0.5f) > BIRD1_X && !p.scoredBird1) {
                p.scoredBird1 = true;
                bird1.score++;
                ServiceLocator.particles().scorePopup(bird1.x, bird1.y);
                ServiceLocator.audio().playPointSound();
                ServiceLocator.audio().playTensionSound(); // Sonido de tensión/velocidad.
            }
            if (bird2.alive && p.x - (TUBERIA_ANCHO * 0.5f) < BIRD2_X && p.x + (TUBERIA_ANCHO * 0.5f) > BIRD2_X && !p.scoredBird2) {
                p.scoredBird2 = true;
                bird2.score++;
                ServiceLocator.particles().scorePopup(bird2.x, bird2.y);
                ServiceLocator.audio().playPointSound();
                ServiceLocator.audio().playTensionSound(); // Sonido de tensión/velocidad.
            }
            if (bird3.alive && p.x - (TUBERIA_ANCHO * 0.5f) < BIRD3_X && p.x + (TUBERIA_ANCHO * 0.5f) > BIRD3_X && !p.scoredBird3) {
                p.scoredBird3 = true;
                bird3.score++;
                ServiceLocator.particles().scorePopup(bird3.x, bird3.y);
                ServiceLocator.audio().playPointSound();
                ServiceLocator.audio().playTensionSound(); // Sonido de tensión/velocidad.
            }

            // Colisiones.
            if (bird1.alive && p.colisionaCon(bird1)) {
                bird1.alive = false;
            }
            if (bird2.alive && p.colisionaCon(bird2)) {
                bird2.alive = false;
            }
            if (bird3.alive && p.colisionaCon(bird3)) {
                bird3.alive = false;
            }

            // Remover tuberías fuera de pantalla.
            if (p.estaFueraDePanel()) {
                it.remove();
            }
        }

        // Game over: cuando alguien alcanza TARGET_SCORE o cuando todos los pájaros mueren.
        boolean winCondition = bird1.score >= TARGET_SCORE || bird2.score >= TARGET_SCORE || bird3.score >= TARGET_SCORE;
        boolean loseCondition = !bird1.alive && !bird2.alive && !bird3.alive;

        if ((winCondition || loseCondition) && !gameOver) {
            gameOver = true;
            ServiceLocator.audio().playGameOverSound(); // Sonido de derrota.
        }
    }

    /**
     * Genera una nueva tubería con gap aleatorio.
     */
    private void spawnPipe() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        pipes.add(new Pipe(1.2f, gapCentro, TUBERIA_ANCHO, GAP_ALTO_BASE));
    }

    /**
     * Retorna el multiplicador de dificultad actual (para HUD).
     */
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    /**
     * Retorna el puntaje máximo de los tres jugadores.
     */
    public int getMaxScore() {
        return Math.max(bird1.score, Math.max(bird2.score, bird3.score));
    }
}
