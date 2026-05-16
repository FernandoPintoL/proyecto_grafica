package com.graphics.flappybird.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import com.graphics.flappybird.effects.ParticleSystem;
import com.graphics.flappybird.services.ServiceLocator;

/**
 * Game: lógica central del Flappy Bird para dos jugadores.
 * Gestiona pájaros, tuberías, colisiones, puntaje y dificultad progresiva.
 */
public class Game {
    // Pájaros (jugadores).
    public Bird bird1;
    public Bird bird2;

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
    private static final float BIRD2_X = 0.15f;  // Derecha.
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

        bird1.update(deltaTime);
        bird2.update(deltaTime);

        // Efecto de partículas cuando un pájaro muere.
        if (bird1WasAlive && !bird1.alive) {
            ServiceLocator.particles().burst(bird1.x, bird1.y, bird1.colorR, bird1.colorG, bird1.colorB, 15);
        }
        if (bird2WasAlive && !bird2.alive) {
            ServiceLocator.particles().burst(bird2.x, bird2.y, bird2.colorR, bird2.colorG, bird2.colorB, 15);
        }

        // Actualizar dificultad según puntaje máximo.
        int maxScore = Math.max(bird1.score, bird2.score);
        difficultyMultiplier = 1.0f + (maxScore * 0.05f); // +5% por punto.
        if (difficultyMultiplier > 2.0f) {
            difficultyMultiplier = 2.0f; // Límite máximo.
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

            // Puntaje: cuando la tubería pasa los pájaros.
            if (p.x + (TUBERIA_ANCHO * 0.5f) < BIRD1_X && !p.scored) {
                p.scored = true;
                bird1.score++;
                ServiceLocator.particles().scorePopup(bird1.x, bird1.y);
                ServiceLocator.audio().playPointSound();
            }
            if (p.x + (TUBERIA_ANCHO * 0.5f) < BIRD2_X && !p.scored) {
                p.scored = true;
                bird2.score++;
                ServiceLocator.particles().scorePopup(bird2.x, bird2.y);
                ServiceLocator.audio().playPointSound();
            }

            // Colisiones.
            if (bird1.alive && p.colisionaCon(bird1)) {
                bird1.alive = false;
            }
            if (bird2.alive && p.colisionaCon(bird2)) {
                bird2.alive = false;
            }

            // Remover tuberías fuera de pantalla.
            if (p.estaFueraDePanel()) {
                it.remove();
            }
        }

        // Game over: ambos pájaros mueren.
        if (!bird1.alive && !bird2.alive) {
            gameOver = true;
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
     * Retorna el puntaje máximo de ambos jugadores.
     */
    public int getMaxScore() {
        return Math.max(bird1.score, bird2.score);
    }
}
