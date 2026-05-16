package com.graphics.flappybird.services;

/**
 * Interfaz para el servicio de renderizado de texto.
 * Permite dibujar texto en la pantalla usando una fuente bitmap.
 */
public interface IFontService {
    /**
     * Renderiza un texto en la pantalla.
     *
     * @param text     String a dibujar
     * @param x        Posición horizontal en coordenadas NDC (-1.0 a 1.0)
     * @param y        Posición vertical en coordenadas NDC (-1.0 a 1.0)
     * @param size     Tamaño del texto (en unidades NDC)
     * @param r        Componente rojo del color (0.0 a 1.0)
     * @param g        Componente verde del color (0.0 a 1.0)
     * @param b        Componente azul del color (0.0 a 1.0)
     */
    void renderText(String text, float x, float y, float size, float r, float g, float b);

    /**
     * Renderiza un número en la pantalla.
     * Equivalente a renderText(String.valueOf(num), x, y, size, r, g, b).
     *
     * @param num      Número a dibujar
     * @param x        Posición horizontal
     * @param y        Posición vertical
     * @param size     Tamaño del texto
     * @param r        Componente rojo del color
     * @param g        Componente verde del color
     * @param b        Componente azul del color
     */
    void renderNumber(int num, float x, float y, float size, float r, float g, float b);

    /**
     * Libera todos los recursos GPU asociados con esta fuente.
     * Se debe llamar antes de cerrar la aplicación.
     */
    void cleanup();
}
