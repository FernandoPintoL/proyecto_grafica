package com.graphics.flappybird.services;

import com.graphics.flappybird.rendering.Renderer;

/**
 * Interfaz para el servicio de partículas.
 * Gestiona efectos visuales de partículas (explosiones, polvo, puntajes).
 */
public interface IParticleService {
    /**
     * Crea una explosión de partículas cuando un ave muere.
     *
     * @param x   Posición horizontal
     * @param y   Posición vertical
     * @param r   Componente rojo del color (0.0 a 1.0)
     * @param g   Componente verde del color (0.0 a 1.0)
     * @param b   Componente azul del color (0.0 a 1.0)
     * @param count Número de partículas
     */
    void burst(float x, float y, float r, float g, float b, int count);

    /**
     * Crea partículas de polvo al saltar.
     *
     * @param x Posición horizontal
     * @param y Posición vertical
     */
    void jumpDust(float x, float y);

    /**
     * Crea partículas cuando se obtiene una puntuación.
     *
     * @param x Posición horizontal
     * @param y Posición vertical
     */
    void scorePopup(float x, float y);

    /**
     * Actualiza todas las partículas activas (física, vida útil, etc.).
     *
     * @param deltaTime Tiempo transcurrido desde el último frame (en segundos)
     */
    void update(float deltaTime);

    /**
     * Renderiza todas las partículas activas en la pantalla.
     *
     * @param renderer El servicio de renderizado para dibujar
     */
    void render(Renderer renderer);

    /**
     * Retorna la cantidad de partículas activas en este momento.
     *
     * @return Número de partículas vivas
     */
    int getActiveCount();
}
