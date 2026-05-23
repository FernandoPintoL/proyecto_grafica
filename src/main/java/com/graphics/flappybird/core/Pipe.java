package com.graphics.flappybird.core;

/**
 * Pipe: representa un obstáculo (tubería superior + inferior).
 * Se mueve horizontalmente de derecha a izquierda.
 */
public class Pipe {
    // Posición horizontal.
    public float x;
    // Centro vertical del hueco.
    public float gapCentroY;
    // Ancho de la tubería.
    public float width;
    // Alto del hueco.
    public float gapHeight;

    // Banderas para evitar contar puntos dos veces por cada jugador.
    public boolean scoredBird1;
    public boolean scoredBird2;
    public boolean scoredBird3;

    public Pipe(float x, float gapCentroY, float width, float gapHeight) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.width = width;
        this.gapHeight = gapHeight;
        this.scoredBird1 = false;
        this.scoredBird2 = false;
        this.scoredBird3 = false;
    }

    /**
     * Mueve la tubería hacia la izquierda.
     */
    public void update(float speed, float deltaTime) {
        x -= speed * deltaTime;
    }

    /**
     * Verifica colisión AABB con un pájaro.
     */
    public boolean colisionaCon(Bird bird) {
        float birdLeft = bird.x - (bird.width * 0.5f);
        float birdRight = bird.x + (bird.width * 0.5f);
        float birdBottom = bird.y - (bird.height * 0.5f);
        float birdTop = bird.y + (bird.height * 0.5f);

        float pipeLeft = x - (width * 0.5f);
        float pipeRight = x + (width * 0.5f);

        // Overlap horizontal.
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) {
            return false;
        }

        // Overlap vertical: fuera del hueco.
        float gapTop = gapCentroY + (gapHeight * 0.5f);
        float gapBottom = gapCentroY - (gapHeight * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    /**
     * Retorna si la tubería está completamente fuera de pantalla (a la izquierda).
     */
    public boolean estaFueraDePanel() {
        return x + (width * 0.5f) < -1.3f;
    }
}
