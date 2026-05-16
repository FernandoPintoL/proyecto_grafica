package com.graphics.flappybird.rendering;

/**
 * Paleta centralizada de colores para todo el juego.
 *
 * ========================================================================================================
 * GUÍA DE VALORES RGB EN OPENGL
 * ========================================================================================================
 *
 * IMPORTANTE: En OpenGL usamos RGB en rango [0.0, 1.0], NO en rango [0, 255].
 *
 * CONVERSIÓN DE COLORES (MÁS IMPORTANTE):
 * ───────────────────────────────────────
 * Si tienes un color en formato [0, 255] (Photoshop, Paint, Color Picker, etc):
 *
 *   Valor en [0, 1] = Valor en [0, 255] / 255.0
 *
 * Ejemplos:
 *   - 255 [0-255]  →  255 / 255 = 1.0f    (máximo, color puro)
 *   - 128 [0-255]  →  128 / 255 = 0.5f    (mitad)
 *   - 64 [0-255]   →  64 / 255  = 0.25f   (un cuarto)
 *   - 0 [0-255]    →  0 / 255   = 0.0f    (mínimo, sin color)
 *
 * ENTENDER CADA COMPONENTE RGB:
 * ────────────────────────────
 * R (Rojo):
 *   0.0f = sin rojo        | 0.5f = rojo medio  | 1.0f = rojo puro/máximo
 *
 * G (Verde):
 *   0.0f = sin verde       | 0.5f = verde medio | 1.0f = verde puro/máximo
 *
 * B (Azul):
 *   0.0f = sin azul        | 0.5f = azul medio  | 1.0f = azul puro/máximo
 *
 * COMBINACIONES ÚTILES:
 * ────────────────────
 *   (1.0, 0.0, 0.0)  →  ROJO puro
 *   (0.0, 1.0, 0.0)  →  VERDE puro
 *   (0.0, 0.0, 1.0)  →  AZUL puro
 *   (1.0, 1.0, 1.0)  →  BLANCO (todos al máximo)
 *   (0.0, 0.0, 0.0)  →  NEGRO (todos al mínimo)
 *   (0.5, 0.5, 0.5)  →  GRIS neutral (todos iguales)
 *   (1.0, 1.0, 0.0)  →  AMARILLO (rojo + verde)
 *   (1.0, 0.0, 1.0)  →  MAGENTA (rojo + azul)
 *   (0.0, 1.0, 1.0)  →  CYAN (verde + azul)
 *   (0.5, 0.25, 0.0) →  MARRÓN oscuro (rojo > verde > sin azul)
 *
 * HERRAMIENTAS RECOMENDADAS:
 * ──────────────────────────
 *
 * OPCIÓN 1: Color Picker Online (MEJOR PARA RÁPIDO):
 *   Sitio: https://www.rapidtables.com/web/color/RGB_Color.html
 *   Pasos:
 *     1) Abre el sitio
 *     2) Haz clic en el cuadro de color grande
 *     3) Selecciona tu color
 *     4) Lee los valores RGB [0, 255] en la columna derecha
 *     5) Usa la TABLA DE CONVERSIÓN abajo
 *
 * OPCIÓN 2: Google Color Picker (RÁPIDO, DISPONIBLE):
 *   Pasos:
 *     1) En Google busca: "color picker"
 *     2) Haz clic en el icono de color que aparece
 *     3) Selecciona tu color
 *     4) Lee RGB [0, 255]
 *     5) Convierte con tabla o calculadora
 *
 * OPCIÓN 3: Plugin VS Code (RECOMENDADO PARA DESARROLLO):
 *   Nombre: \"Color Picker\" por Florian Knobel
 *   ID: anseki.color-picker
 *   Instalación:
 *     1) Ctrl+Shift+X (o Cmd+Shift+X en Mac)
 *     2) Busca \"color picker\"
 *     3) Instala el primero
 *     4) Reinicia VS Code
 *   Uso:
 *     1) Haz clic sobre cualquier color hexadecimal o RGB en el código
 *     2) Se abre selector de color
 *     3) Selecciona tu color
 *     4) Lee RGB [0, 255]
 *     5) Convierte a [0, 1]
 *
 * OPCIÓN 4: Calculadora Online:
 *   Sitio: https://www.colorhexa.com/
 *   Permite ver equivalencias entre RGB, HEX, HSL, etc.
 *
 * TABLA DE CONVERSIÓN RÁPIDA [0, 255] → [0, 1]:
 * ───────────────────────────────────────────────
 *   0   → 0.00f     64  → 0.25f     128 → 0.50f     192 → 0.75f     255 → 1.00f
 *   8   → 0.03f     72  → 0.28f     136 → 0.53f     200 → 0.78f
 *   16  → 0.06f     80  → 0.31f     144 → 0.56f     208 → 0.82f
 *   24  → 0.09f     88  → 0.35f     152 → 0.60f     216 → 0.85f
 *   32  → 0.13f     96  → 0.38f     160 → 0.63f     224 → 0.88f
 *   40  → 0.16f     104 → 0.41f     168 → 0.66f     232 → 0.91f
 *   48  → 0.19f     112 → 0.44f     176 → 0.69f     240 → 0.94f
 *   56  → 0.22f     120 → 0.47f     184 → 0.72f     248 → 0.97f
 *
 * EJEMPLO PRÁCTICO: Cambiar pico del pájaro a ROJO PURO
 * ───────────────────────────────────────────────────
 * 1) Abre: https://www.rapidtables.com/web/color/RGB_Color.html
 * 2) Selecciona rojo puro (o cualquier rojo)
 * 3) Lee valores: R=255, G=0, B=0
 * 4) Convierte:
 *      R: 255 / 255.0 = 1.0f
 *      G: 0 / 255.0 = 0.0f
 *      B: 0 / 255.0 = 0.0f
 * 5) Actualiza en este archivo:
 *      public static final float BIRD_BEAK_R = 1.0f;
 *      public static final float BIRD_BEAK_G = 0.0f;
 *      public static final float BIRD_BEAK_B = 0.0f;
 * 6) ¡Listo! El pico ahora es rojo puro.
 *
 * EJEMPLO 2: Cambiar cielo a NARANJA SUAVE
 * ────────────────────────────────────────
 * 1) Busca color naranja en color picker
 * 2) Lee: R=255, G=165, B=0 (naranja estándar)
 * 3) Convierte cada uno:
 *      R: 255 / 255 = 1.0f
 *      G: 165 / 255 = 0.647f (redondeado a 0.65f)
 *      B: 0 / 255 = 0.0f
 * 4) Actualiza:
 *      public static final float SKY_TOP_R = 1.0f;
 *      public static final float SKY_TOP_G = 0.65f;
 *      public static final float SKY_TOP_B = 0.0f;
 *
 * CONSEJOS FINALES:
 * ────────────────
 * - Siempre divide entre 255.0 (con punto decimal) para obtener float
 * - Los valores deben estar entre 0.0f y 1.0f
 * - Si necesitas valores hexadecimales (#RRGGBB), usa https://www.colorhexa.com/
 * - Guarda los colores que uses frecuentemente en ColorPalette
 * - Prueba los cambios compilando y ejecutando el juego
 * - Los valores muy pequeños (0.1f) hacen colores oscuros
 * - Los valores cercanos a 1.0f hacen colores brillantes
 *
 * ========================================================================================================
 */
public class ColorPalette {
    // ===== CIELO: DEGRADADO DE AZULES =====
    // Capa superior (más clara, horizonte lejano)
    public static final float SKY_TOP_R = 0.68f, SKY_TOP_G = 0.88f, SKY_TOP_B = 0.99f;
    // Capa media (transición)
    public static final float SKY_MID_R = 0.58f, SKY_MID_G = 0.82f, SKY_MID_B = 0.95f;
    // Capa inferior (más cálido, atardecer suave)
    public static final float SKY_BOT_R = 0.62f, SKY_BOT_G = 0.80f, SKY_BOT_B = 0.88f;

    // ===== MONTAÑAS: VERDES OSCUROS CON PARALAJE =====
    // Montañas lejanas (muy transparentes, alpha 0.3f)
    public static final float MOUNT_DISTANT_R = 0.45f, MOUNT_DISTANT_G = 0.60f, MOUNT_DISTANT_B = 0.35f;
    // Montañas cercanas (menos transparentes, alpha 0.4f)
    public static final float MOUNT_NEAR_R = 0.40f, MOUNT_NEAR_G = 0.55f, MOUNT_NEAR_B = 0.30f;

    // ===== SUELO: GRADIENTE VERDE =====
    // Línea superior de hierba (verde claro)
    public static final float GRASS_TOP_R = 0.40f, GRASS_TOP_G = 0.65f, GRASS_TOP_B = 0.25f;
    // Suelo principal (verde oscuro)
    public static final float GRASS_MAIN_R = 0.30f, GRASS_MAIN_G = 0.50f, GRASS_MAIN_B = 0.18f;
    // Línea de sombra (verde muy oscuro)
    public static final float GRASS_SHADOW_R = 0.18f, GRASS_SHADOW_G = 0.30f, GRASS_SHADOW_B = 0.08f;
    // Detalles de hierba (semi-transparentes)
    public static final float GRASS_DETAIL_R = 0.35f, GRASS_DETAIL_G = 0.58f, GRASS_DETAIL_B = 0.20f;

    // ===== NUBES: BLANCO GRISÁCEO =====
    public static final float CLOUD_BRIGHT = 0.98f;      // Centro brillante
    public static final float CLOUD_LIGHT = 0.96f;       // Bordes claros
    public static final float CLOUD_MEDIUM = 0.94f;      // Tonos medios
    public static final float CLOUD_SOFT = 0.90f;        // Muy lejanas

    // ===== TUBERÍAS: VERDES ESTILO MARIO CON EFECTO 3D =====
    public static final float PIPE_R = 0.18f, PIPE_G = 0.70f, PIPE_B = 0.25f;      // Verde base
    // Bordes oscuros (sombra): 40% del color base
    public static final float PIPE_DARK_R = 0.072f, PIPE_DARK_G = 0.28f, PIPE_DARK_B = 0.10f;
    // Bordes claros (luz): color base + 15%
    public static final float PIPE_LIGHT_R = 0.33f, PIPE_LIGHT_G = 0.85f, PIPE_LIGHT_B = 0.40f;

    // ===== HUD: PANELES CON COLORES DE JUGADORES =====
    // Panel P1 (naranja)
    public static final float HUD_P1_R = 0.98f, HUD_P1_G = 0.85f, HUD_P1_B = 0.20f;
    // Panel P2 (azul)
    public static final float HUD_P2_R = 0.20f, HUD_P2_G = 0.85f, HUD_P2_B = 0.98f;
    // Panel Dificultad (verde)
    public static final float HUD_DIFF_R = 0.6f, HUD_DIFF_G = 0.6f, HUD_DIFF_B = 0.2f;
    // Texto HUD (blanco)
    public static final float HUD_TEXT_R = 1.0f, HUD_TEXT_G = 1.0f, HUD_TEXT_B = 1.0f;

    // ===== PÁJAROS: COLORES POR JUGADOR =====
    // Pájaro 1 (naranja)
    public static final float BIRD_P1_R = 0.98f, BIRD_P1_G = 0.85f, BIRD_P1_B = 0.20f;
    // Pájaro 2 (azul)
    public static final float BIRD_P2_R = 0.20f, BIRD_P2_G = 0.85f, BIRD_P2_B = 0.98f;
    // Pico (naranja puro: RGB 255, 165, 0 → 1.0f, 0.65f, 0.0f)
    public static final float BIRD_BEAK_R = 1.0f, BIRD_BEAK_G = 0.65f, BIRD_BEAK_B = 0.0f;

    // ===== PANTALLAS MODALES =====
    // Fondo start screen (gris muy oscuro semi-transparente)
    public static final float MODAL_START_R = 0.1f, MODAL_START_G = 0.1f, MODAL_START_B = 0.15f;
    // Fondo game over screen (rojo semi-transparente)
    public static final float MODAL_GAMEOVER_R = 1.0f, MODAL_GAMEOVER_G = 0.0f, MODAL_GAMEOVER_B = 0.0f;

    // ===== SOMBRA Y EFECTOS =====
    // Sombra de pájaro (negro semi-transparente)
    public static final float SHADOW_R = 0.0f, SHADOW_G = 0.0f, SHADOW_B = 0.0f;
    // Ojo: esclera (blanco)
    public static final float EYE_WHITE_R = 1.0f, EYE_WHITE_G = 1.0f, EYE_WHITE_B = 1.0f;
    // Ojo: iris (negro)
    public static final float EYE_BLACK_R = 0.05f, EYE_BLACK_G = 0.05f, EYE_BLACK_B = 0.05f;
}
