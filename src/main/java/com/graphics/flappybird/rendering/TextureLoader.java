package com.graphics.flappybird.rendering;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * TextureLoader: carga imágenes PNG y las convierte en texturas OpenGL.
 */
public class TextureLoader {

    public static int loadTexture(String resourcePath) {
        try (InputStream stream = TextureLoader.class.getResourceAsStream("/" + resourcePath)) {
            if (stream == null) {
                throw new RuntimeException("Recurso no encontrado: " + resourcePath);
            }

            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }

            buffer.flip();

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            return textureId;
        } catch (Exception e) {
            throw new RuntimeException("Error cargando textura: " + resourcePath, e);
        }
    }
}
