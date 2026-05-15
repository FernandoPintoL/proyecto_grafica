package com.graphics.flappybird;

/**
 * BitmapFont: renderiza números y letras con alineación consistente.
 * Todas las letras usan la misma línea de base para perfecta alineación.
 */
public class BitmapFont {
    private Renderer renderer;

    public BitmapFont(Renderer renderer, float scale) {
        this.renderer = renderer;
    }

    /**
     * Renderiza un número grande.
     */
    public void renderNumber(int num, float x, float y, float size, float r, float g, float b) {
        String str = String.valueOf(num);

        for (char c : str.toCharArray()) {
            renderDigitAsShape(c, x, y, size, r, g, b);
            x += size * 0.7f;
        }
    }

    /**
     * Renderiza texto (números y letras).
     */
    public void renderText(String text, float x, float y, float size, float r, float g, float b) {
        for (char c : text.toCharArray()) {
            if (c >= '0' && c <= '9') {
                renderDigitAsShape(c, x, y, size, r, g, b);
                x += size * 0.7f;
            } else if (c >= 'A' && c <= 'Z') {
                renderLetterAsShape(c, x, y, size, r, g, b);
                x += size * 0.6f;
            } else if (c == ' ') {
                x += size * 0.3f;
            } else if (c == ':') {
                float dotSize = size * 0.08f;
                renderer.drawRect(x + size * 0.05f, y + size * 0.15f, dotSize, dotSize, r, g, b);
                renderer.drawRect(x + size * 0.05f, y - size * 0.15f, dotSize, dotSize, r, g, b);
                x += size * 0.25f;
            }
        }
    }

    /**
     * Renderiza un dígito con alineación estándar.
     */
    private void renderDigitAsShape(char digit, float x, float y, float size, float r, float g, float b) {
        float barW = size * 0.10f;
        float barH = size * 0.20f;
        float w = size * 0.45f;
        float h = size * 0.75f;
        float cx = x + w * 0.5f;
        float cy = y;

        float top = cy + h * 0.35f;
        float middle = cy;
        float bottom = cy - h * 0.35f;

        switch (digit) {
            case '0':
                renderer.drawRect(cx - w * 0.2f, cy, barW, h * 0.7f, r, g, b);
                renderer.drawRect(cx + w * 0.2f, cy, barW, h * 0.7f, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '1':
                renderer.drawRect(cx + w * 0.1f, cy, barW, h * 0.7f, r, g, b);
                break;
            case '2':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle + h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, middle - h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '3':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle + h * 0.1f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle - h * 0.1f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '4':
                renderer.drawRect(cx - w * 0.15f, middle + h * 0.15f, barW, h * 0.35f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, h * 0.7f, r, g, b);
                break;
            case '5':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, middle + h * 0.1f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle - h * 0.1f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '6':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, cy, barW, h * 0.7f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle - h * 0.15f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '7':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, cy, barW, h * 0.7f, r, g, b);
                break;
            case '8':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, middle + h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle + h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, middle - h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle - h * 0.1f, barW, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case '9':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, middle + h * 0.15f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, h * 0.7f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
        }
    }

    /**
     * Renderiza una letra (A-Z) con alineación estándar consistente.
     * TODAS las letras usan la MISMA línea de base vertical.
     */
    private void renderLetterAsShape(char letter, float x, float y, float size, float r, float g, float b) {
        float barW = size * 0.10f;
        float barH = size * 0.18f;
        float w = size * 0.70f;
        float h = size * 0.75f;
        float cx = x + w * 0.5f;
        float cy = y;  // MISMA LÍNEA DE BASE PARA TODAS

        float top = cy + h * 0.35f;
        float middle = cy;
        float bottom = cy - h * 0.35f;
        float vHeight = h * 0.7f;

        switch (letter) {
            case 'A':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                break;
            case 'B':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle + h * 0.15f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle - h * 0.15f, barW, barH * 0.6f, r, g, b);
                break;
            case 'C':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'D':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle + h * 0.1f, barW, barH * 0.5f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle - h * 0.1f, barW, barH * 0.5f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'E':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'F':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                break;
            case 'G':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, middle, w * 0.3f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle - h * 0.05f, barW, barH * 0.5f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'I':
                renderer.drawRect(cx, cy, barW, vHeight, r, g, b);
                break;
            case 'L':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'M':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, middle + h * 0.2f, barW, barH * 0.7f, r, g, b);
                break;
            case 'N':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, middle - h * 0.1f, w * 0.25f, barH * 0.6f, r, g, b);
                break;
            case 'O':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx + w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'P':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle + h * 0.15f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                break;
            case 'R':
                renderer.drawRect(cx - w * 0.15f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle + h * 0.15f, barW, barH * 0.6f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle - h * 0.15f, w * 0.3f, barH * 0.5f, r, g, b);
                break;
            case 'S':
                renderer.drawRect(cx, top, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx - w * 0.1f, middle + h * 0.1f, barW, barH * 0.5f, r, g, b);
                renderer.drawRect(cx, middle, w * 0.5f, barH, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle - h * 0.1f, barW, barH * 0.5f, r, g, b);
                renderer.drawRect(cx, bottom, w * 0.5f, barH, r, g, b);
                break;
            case 'T':
                renderer.drawRect(cx, top, w * 0.6f, barH, r, g, b);
                renderer.drawRect(cx, cy, barW, vHeight, r, g, b);
                break;
            case 'V':
                renderer.drawRect(cx - w * 0.15f, middle + h * 0.2f, barW, h * 0.5f, r, g, b);
                renderer.drawRect(cx + w * 0.15f, middle + h * 0.2f, barW, h * 0.5f, r, g, b);
                break;
            case 'W':
                renderer.drawRect(cx - w * 0.2f, cy, barW, vHeight, r, g, b);
                renderer.drawRect(cx, middle - h * 0.1f, barW, h * 0.6f, r, g, b);
                renderer.drawRect(cx + w * 0.2f, cy, barW, vHeight, r, g, b);
                break;
            case 'Y':
                renderer.drawRect(cx - w * 0.1f, middle + h * 0.2f, barW, h * 0.35f, r, g, b);
                renderer.drawRect(cx + w * 0.1f, middle + h * 0.2f, barW, h * 0.35f, r, g, b);
                renderer.drawRect(cx, cy, barW, h * 0.45f, r, g, b);
                break;
        }
    }
}
