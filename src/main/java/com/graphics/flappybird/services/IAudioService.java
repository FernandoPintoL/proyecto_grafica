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
}
