package com.graphics.flappybird;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;

/**
 * FontGenerator: genera una imagen PNG con todas las letras y números
 * para usar como bitmap font en OpenGL.
 */
public class FontGenerator {
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ :";
    private static final int CHAR_WIDTH = 32;
    private static final int CHAR_HEIGHT = 48;
    private static final int COLS = 8;
    private static final int ROWS = 5;
    private static final Font FONT = new Font("Arial", Font.BOLD, 40);

    public static void generateFont() {
        int imageWidth = CHAR_WIDTH * COLS;
        int imageHeight = CHAR_HEIGHT * ROWS;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo transparente completo (RGBA con alpha = 0)
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setColor(Color.WHITE);
        g2d.setFont(FONT);
        FontMetrics fm = g2d.getFontMetrics();

        for (int i = 0; i < CHARSET.length(); i++) {
            char c = CHARSET.charAt(i);
            int col = i % COLS;
            int row = i / COLS;
            int x = col * CHAR_WIDTH;
            int y = row * CHAR_HEIGHT;

            // Dibujar carácter centrado en el cuadro
            int charWidth = fm.charWidth(c);
            int charX = x + (CHAR_WIDTH - charWidth) / 2;
            int charY = y + fm.getAscent() + (CHAR_HEIGHT - fm.getHeight()) / 2;

            g2d.drawString(String.valueOf(c), charX, charY);
        }

        g2d.dispose();

        try {
            String resourcePath = "src/main/resources/font.png";
            new File("src/main/resources").mkdirs();
            ImageIO.write(image, "PNG", new File(resourcePath));
            System.out.println("Fuente bitmap generada: " + resourcePath);
        } catch (Exception e) {
            System.err.println("Error generando fuente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        generateFont();
    }
}
