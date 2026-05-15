package com.graphics.flappybird;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * TextRenderer: Renderiza texto usando Java2D + OpenGL texture.
 * Mucho más simple y legible que intentar dibujar con geometría.
 */
public class TextRenderer {
    private Font font;
    private FontMetrics metrics;

    public TextRenderer() {
        this.font = new Font("Arial", Font.BOLD, 80);
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        this.metrics = dummy.getGraphics().getFontMetrics(font);
    }

    /**
     * Dibuja un número en 2D en la posición dada.
     * x, y están en coordenadas de pantalla (píxeles).
     * windowWidth, windowHeight son el tamaño de la ventana.
     */
    public void drawNumberAtPixel(int num, int pixelX, int pixelY, int windowWidth, int windowHeight, float r, float g, float b) {
        String text = String.valueOf(num);

        // Crear una imagen con el número renderizado
        int width = metrics.stringWidth(text) + 10;
        int height = metrics.getHeight() + 10;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Fondo transparente
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Texto blanco
        g2d.setColor(new Color((int)(r * 255), (int)(g * 255), (int)(b * 255)));
        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawString(text, 5, metrics.getAscent() + 5);
        g2d.dispose();

        // Convertir a OpenGL y renderizar
        drawImageAsQuad(img, pixelX, pixelY, windowWidth, windowHeight);
    }

    /**
     * Dibuja una imagen BufferedImage como un quad en OpenGL.
     */
    private void drawImageAsQuad(BufferedImage img, int pixelX, int pixelY, int windowWidth, int windowHeight) {
        // Convertir BufferedImage a ByteBuffer
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        // Crear textura OpenGL
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, img.getWidth(), img.getHeight(),
                         0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Dibujar como quad en posición de pantalla
        drawTextureQuad(pixelX, pixelY, img.getWidth(), img.getHeight(), windowWidth, windowHeight);

        GL11.glDeleteTextures(textureID);
    }

    /**
     * Dibuja un quad de textura en coordenadas de pantalla.
     */
    private void drawTextureQuad(int x, int y, int width, int height, int windowWidth, int windowHeight) {
        // Convertir píxeles a NDC (Normalized Device Coordinates)
        float left = (2.0f * x / windowWidth) - 1.0f;
        float right = (2.0f * (x + width) / windowWidth) - 1.0f;
        float top = 1.0f - (2.0f * y / windowHeight);
        float bottom = 1.0f - (2.0f * (y + height) / windowHeight);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
