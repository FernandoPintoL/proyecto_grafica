package com.graphics.flappybird.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;

/**
 * FontGenerator: herramienta offline que genera una imagen PNG con todas las letras y números
 * para usar como bitmap font en OpenGL.
 *
 * CÓMO FUNCIONA:
 * - Crea una imagen PNG con grid de 8x5 celdas (40 caracteres)
 * - Cada celda tiene 32x48 píxeles (imagen final: 256x240 píxeles)
 * - Dibuja cada carácter centrado en su celda
 * - Guarda el resultado en src/main/resources/font.png
 *
 * USO:
 * - Ejecutar: java -cp target/classes com.graphics.flappybird.rendering.FontGenerator
 * - O simplemente: mvn exec:exec con main-class en pom.xml apuntando a esta clase
 */
public class FontGenerator {
    // ===== CONFIGURACIÓN DEL CHARSET =====
    // Caracteres que se incluirán en la fuente: dígitos 0-9, letras A-Z, espacio y dos puntos
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ :";

    // ===== DIMENSIONES DE CADA CARÁCTER =====
    private static final int CHAR_WIDTH = 32;   // Ancho de cada celda en píxeles
    private static final int CHAR_HEIGHT = 48;  // Alto de cada celda en píxeles

    // ===== DISTRIBUCIÓN EN GRID =====
    private static final int COLS = 8;  // 8 columnas (8 caracteres por fila)
    private static final int ROWS = 5;  // 5 filas (40 caracteres total = 8 * 5)

    // ===== PROPIEDADES DE LA FUENTE =====
    // Font(nombre, estilo, tamaño)
    // Arial = fuente sans-serif profesional
    // Font.BOLD = texto en negrita para mejor visibilidad
    // 40 = tamaño en puntos
    private static final Font FONT = new Font("Arial", Font.BOLD, 40);

    /**
     * Genera la imagen PNG de bitmap font y la guarda en disco.
     * Este método es el corazón del generador.
     */
    public static void generateFont() {
        // PASO 1: Calcular tamaño total de la imagen
        // Ancho = 32 píxeles/carácter × 8 columnas = 256 píxeles
        // Alto = 48 píxeles/carácter × 5 filas = 240 píxeles
        int imageWidth = CHAR_WIDTH * COLS;
        int imageHeight = CHAR_HEIGHT * ROWS;

        // PASO 2: Crear imagen vacía en memoria
        // TYPE_INT_ARGB = formato de color con transparencia (Alpha, Red, Green, Blue)
        // Esto permite que el fondo transparente funcione correctamente en OpenGL
        BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                BufferedImage.TYPE_INT_ARGB);

        // PASO 3: Obtener contexto de dibujo 2D
        // Graphics2D es la herramienta que usaremos para dibujar los caracteres
        Graphics2D g2d = image.createGraphics();

        // PASO 4: Activar suavizado anti-aliasing
        // KEY_ANTIALIASING = suaviza las formas geométricas (líneas, bordes)
        // KEY_TEXT_ANTIALIASING = suaviza específicamente el texto
        // VALUE_ANTIALIAS_ON = activa el suavizado
        // Resultado: letras legibles y con bordes suaves, no pixeladas
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // PASO 5: Hacer el fondo completamente transparente
        // AlphaComposite.Clear = borra toda la imagen (todo el alpha se pone a 0)
        // fillRect() llena todo el rectángulo (0,0 a width,height) con transparencia
        // Luego restauramos el modo normal (SrcOver) para dibujar normalmente
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        // PASO 6: Preparar propiedades de dibujo
        // Color.WHITE = dibujar en blanco (en OpenGL se multiplicará por el color del shader)
        // Usar la fuente Arial Bold 40pt definida en las constantes
        // FontMetrics = objeto que contiene información sobre medidas de la fuente
        //              (ancho de caracteres, altura, baseline, etc.)
        g2d.setColor(Color.WHITE);
        g2d.setFont(FONT);
        FontMetrics fm = g2d.getFontMetrics();

        // PASO 7: Bucle para dibujar cada carácter en su celda
        // Recorre cada carácter del CHARSET y lo coloca en su posición
        for (int i = 0; i < CHARSET.length(); i++) {
            // Obtener el carácter en la posición i
            char c = CHARSET.charAt(i);

            // Calcular fila y columna en el grid
            // Ejemplo: si COLS=8 e i=10:
            //   col = 10 % 8 = 2 (tercera columna)
            //   row = 10 / 8 = 1 (segunda fila)
            int col = i % COLS;
            int row = i / COLS;

            // Calcular posición superior-izquierda de la celda
            // Cada celda está desplazada por su ancho/alto
            // Ejemplo con col=2, row=1:
            //   x = 2 * 32 = 64 píxeles desde la izquierda
            //   y = 1 * 48 = 48 píxeles desde la parte superior
            int x = col * CHAR_WIDTH;
            int y = row * CHAR_HEIGHT;

            // Calcular posición X,Y para centrar el carácter dentro de la celda
            // Ancho real del carácter (puede ser menor a CHAR_WIDTH)
            int charWidth = fm.charWidth(c);

            // X centrado: calcular espacio disponible a la izquierda
            // (CHAR_WIDTH - charWidth) / 2 = espacio para alinear a la izquierda
            // Sumamos a la posición x de la celda
            int charX = x + (CHAR_WIDTH - charWidth) / 2;

            // Y centrado: usar la métrica de ascenso + alineación vertical
            // fm.getAscent() = altura desde el baseline hasta la parte superior
            // (CHAR_HEIGHT - fm.getHeight()) / 2 = espacio vertical disponible
            // Esto centra el carácter verticalmente en la celda
            int charY = y + fm.getAscent() + (CHAR_HEIGHT - fm.getHeight()) / 2;

            // Dibujar el carácter en la posición calculada
            g2d.drawString(String.valueOf(c), charX, charY);
        }

        // PASO 8: Liberar recursos gráficos
        // dispose() libera la memoria del contexto Graphics2D
        // Es importante hacerlo para no perder memoria
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
