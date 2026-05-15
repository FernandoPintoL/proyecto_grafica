package com.graphics.flappybird;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * TextTexture: Renderiza texto real usando Java2D + OpenGL texture.
 * Versión simplificada compatible con OpenGL 3.3 core profile.
 */
public class TextTexture {
    private Font font;
    private FontMetrics metrics;
    private Renderer renderer;

    public TextTexture(Renderer renderer) {
        this.renderer = renderer;
        this.font = new Font("Arial", Font.BOLD, 96);
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        this.metrics = dummy.getGraphics().getFontMetrics(font);
    }

    /**
     * Dibuja texto en coordenadas NDC.
     */
    public void drawText(String text, float x, float y, float size, float r, float g, float b,
                        int windowWidth, int windowHeight) {
        // Crear imagen con el texto
        BufferedImage img = createTextImage(text, r, g, b);

        // Calcular tamaño en NDC
        float width = (img.getWidth() * 2.0f) / windowWidth;
        float height = (img.getHeight() * 2.0f) / windowHeight;

        // Dibujar como rectángulo texturizado
        drawTextureQuad(img, x, y, width, height, r, g, b);
    }

    /**
     * Crea una imagen BufferedImage con el texto renderizado.
     */
    private BufferedImage createTextImage(String text, float r, float g, float b) {
        int width = metrics.stringWidth(text) + 20;
        int height = metrics.getHeight() + 10;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Fondo transparente
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Renderizar texto con antialiasing
        g2d.setColor(new Color((int)(r * 255), (int)(g * 255), (int)(b * 255)));
        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.drawString(text, 10, metrics.getAscent() + 5);
        g2d.dispose();

        return img;
    }

    /**
     * Dibuja un quad texturizado en NDC.
     */
    private void drawTextureQuad(BufferedImage img, float x, float y, float width, float height, float r, float g, float b) {
        // Convertir BufferedImage a ByteBuffer
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        // Crear textura
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, img.getWidth(), img.getHeight(),
                         0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Dibujar como rectángulo simple en NDC
        renderer.drawRect(x, y, width, height, r, g, b);

        // Limpiar
        GL11.glDeleteTextures(textureID);
    }
}
