package com.graphics.flappybird.core;

/**
 * Bird: representa un pájaro individual en el juego.
 * Cada jugador tiene su propia instancia con posición, velocidad y estado.
 */
public class Bird {
    // Posición horizontal fija en NDC (cada pájaro tiene la suya).
    public float x;
    // Posición vertical (puede cambiar).
    public float y;
    // Velocidad vertical (acelera por gravedad).
    public float velY;
    // Ancho y alto del pájaro.
    public float width;
    public float height;

    // Estado del juego.
    public boolean alive;
    public int score;

    // Físicas.
    private static final float GRAVEDAD = -1.9f;
    private static final float IMPULSO_SALTO = 0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Color de cada pájaro (para distinguirlos visualmente).
    public float colorR, colorG, colorB;

    public Bird(float startX, float startY, float width, float height,
                float colorR, float colorG, float colorB) {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;

        this.velY = 0.0f;
        this.alive = true;
        this.score = 0;
    }

    /**
     * Aplica gravedad e integra velocidad.
     */
    public void update(float deltaTime) {
        if (!alive) return;

        // Gravedad.
        velY += GRAVEDAD * deltaTime;
        if (velY < VELOCIDAD_MAX_CAIDA) {
            velY = VELOCIDAD_MAX_CAIDA;
        }
        // Integración.
        y += velY * deltaTime;

        // Colisión contra techo/suelo.
        float top = y + (height * 0.5f);
        float bottom = y - (height * 0.5f);
        if (top >= 1.0f || bottom <= -1.0f) {
            alive = false;
        }
    }

    /**
     * Salta (aplicar impulso).
     */
    public void jump() {
        if (alive) {
            velY = IMPULSO_SALTO;
        }
    }

    /**
     * Reinicia el pájaro para nueva partida.
     */
    public void reset(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.velY = 0.0f;
        this.alive = true;
        this.score = 0;
    }

    /**
     * Retorna ángulo de inclinación basado en velocidad.
     * Útil para animar la rotación del pájaro.
     */
    public float getRotationAngle() {
        // Mapear velY a ángulo: más negativo = más inclinado hacia abajo.
        return (velY / VELOCIDAD_MAX_CAIDA) * 0.5f; // ±0.5 radianes aprox.
    }
}
