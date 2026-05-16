package com.graphics.flappybird.services;

/**
 * Interfaz para el servicio de audio.
 * Permite reproducir efectos de sonido para el juego.
 */
public interface IAudioService {
    /**
     * Reproduce el sonido de salto (bip corto de 800Hz).
     */
    void playJumpSound();

    /**
     * Reproduce el sonido de puntuación (bip de 1200Hz).
     */
    void playPointSound();

    /**
     * Reproduce el sonido de fin del juego (barrido de frecuencia).
     */
    void playGameOverSound();

    /**
     * Reproduce sonido de tensión/velocidad (barrido ascendente 400→800Hz).
     * Transmite sensación de velocidad aumentada.
     */
    void playTensionSound();

    /**
     * Alerta de peligro: dos tonos agudos (1000Hz).
     * Indica que el juego está en velocidad máxima.
     */
    void playSpeedWarning();
}
