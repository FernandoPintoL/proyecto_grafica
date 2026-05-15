# 🎮 Flappy Bird 2P - Masterclass de Arquitectura

## 📋 Índice
1. [Visión General](#visión-general)
2. [Estructura de Carpetas](#estructura-de-carpetas)
3. [Clases Principales](#clases-principales)
4. [Flujo de Renderizado](#flujo-de-renderizado)
5. [Sistema de Entrada](#sistema-de-entrada)
6. [Lógica del Juego](#lógica-del-juego)
7. [Rendering Pipeline](#rendering-pipeline)
8. [Sistemas Especiales](#sistemas-especiales)

---

## 🎯 Visión General

Este es un **juego Flappy Bird para 2 jugadores** escrito en Java con **OpenGL 3.3 Core** y **LWJGL 3** (Lightweight Java Game Library). El juego corre a ~60 FPS con renderizado acelerado por GPU.

### Stack Tecnológico:
- **Lenguaje**: Java 17
- **Gráficos**: OpenGL 3.3 Core
- **Librería de Ventanas**: GLFW (via LWJGL)
- **Build**: Maven
- **Audio**: Java Sound API
- **Fuentes**: Bitmap Font renderizado con texturas

---

## 📁 Estructura de Carpetas

```
proyecto/
├── src/main/java/com/graphics/flappybird/
│   ├── FlappyBirdGame.java          ← PUNTO DE ENTRADA (main)
│   ├── Renderer.java                ← Motor de renderizado (OpenGL)
│   ├── Game.java                    ← Lógica del juego
│   ├── Bird.java                    ← Lógica de cada pájaro
│   ├── Pipe.java                    ← Lógica de tuberías
│   ├── ParticleSystem.java          ← Sistema de partículas
│   ├── TextureFont.java             ← Renderizado de texto con texturas
│   ├── TextureLoader.java           ← Cargador de imágenes PNG → OpenGL
│   ├── FontGenerator.java           ← Generador de bitmap font
│   └── SoundManager.java            ← Síntesis de audio
├── src/main/resources/
│   └── font.png                     ← Imagen bitmap con todas las letras
└── pom.xml                          ← Configuración Maven
```

---

## 🏗️ Clases Principales

### 1. **FlappyBirdGame.java** - El Orquestador

**Responsabilidades:**
- Inicializar GLFW, OpenGL y crear ventana
- Gestionar el loop principal del juego
- Procesar entrada del teclado
- Coordinador entre Game, Renderer y audio

**Flujo principal:**
```
run()
├── init()           ← Inicializa GLFW, OpenGL, crea Game y Renderer
├── loop()           ← Loop infinito (~60 FPS)
│   ├── processInput()      ← Lee teclado (W, SPACE, R)
│   ├── game.update(dt)     ← Actualiza lógica
│   ├── renderer.render()   ← Renderiza a pantalla
│   └── glfwSwapBuffers()   ← Muestra frame
└── cleanup()        ← Libera recursos
```

**Detección de entrada (Edge Detection):**
```java
prevW = false;      // Estado anterior de la tecla W
spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
if (spaceNow && !prevSpace) {  // ← Se detecta CAMBIO, no estado continuo
    bird2.jump();
}
prevSpace = spaceNow;
```

---

### 2. **Game.java** - Lógica del Juego

**Responsabilidades:**
- Gestionar estado del juego (started, gameOver)
- Actualizar pájaros, tuberías y colisiones
- Calcular puntuación y dificultad progresiva
- Generar nuevas tuberías

**Estados:**
```
gameStarted = false  ← Pantalla de inicio
    ↓ (usuario presiona W o SPACE)
gameStarted = true, gameOver = false  ← Jugando
    ↓ (ambos pájaros mueren)
gameStarted = true, gameOver = true   ← Game Over
    ↓ (usuario presiona R)
Reinicia a gameStarted = false
```

**Actualización por frame:**
```java
update(float deltaTime) {
    // 1. Actualizar posiciones de pájaros
    bird1.update(deltaTime);
    bird2.update(deltaTime);
    
    // 2. Generar nuevas tuberías (cada 1.5s / dificultad)
    timerSpawn += deltaTime;
    if (timerSpawn >= spawnInterval) spawnPipe();
    
    // 3. Mover tuberías y detectar colisiones
    for (Pipe p : pipes) {
        p.update(pipeSpeed, deltaTime);
        if (bird1.colisiona(p)) bird1.alive = false;
        if (bird2.colisiona(p)) bird2.alive = false;
    }
    
    // 4. Detectar game over (ambos muertos)
    if (!bird1.alive && !bird2.alive) gameOver = true;
}
```

**Dificultad Progresiva:**
```
difficultyMultiplier = 1.0 + (maxScore * 0.05)
Max: 2.0x (el juego no se vuelve infinitamente rápido)

Afecta:
- Velocidad de tuberías: pipeSpeed * multiplier
- Frecuencia de tuberías: spawnInterval / multiplier
```

---

### 3. **Bird.java** - El Pájaro

**Propiedades:**
- Posición (x, y)
- Velocidad vertical (velocityY)
- Rotación (según velocidad)
- Puntuación
- Color (para diferenciar jugadores)

**Física:**
```java
update(float deltaTime) {
    // Aplicar gravedad
    velocityY -= GRAVITY * deltaTime;  // ~0.8 por frame
    
    // Limitar velocidad máxima de caída
    if (velocityY < -MAX_FALL_SPEED) 
        velocityY = -MAX_FALL_SPEED;
    
    // Actualizar posición
    y += velocityY * deltaTime;
    
    // Detectar colisión con piso
    if (y <= GROUND_LEVEL) alive = false;
}

jump() {
    velocityY = JUMP_STRENGTH;  // ~0.5 (hacia arriba)
}
```

**Rotación Visual:**
```
getRotationAngle() = velocityY / MAX_FALL_SPEED * 90°

Si cae rápido: rota hacia abajo (-90°)
Si sube: rota hacia arriba (+90°)
Efecto: El pájaro "mira" hacia donde va
```

---

### 4. **Renderer.java** - Motor de Renderizado

**Responsabilidades:**
- Compilar shaders de OpenGL
- Crear buffers de geometría
- Renderizar cada elemento del juego
- Gestionar transformaciones y colores

**Pipeline de Renderizado:**
```
render(Game game, Particles, width, height)
├── glClear() ← Limpia pantalla con color cielo
├── glUseProgram(programa) ← Activa shader principal
├── glBindVertexArray(vao) ← Activa geometry buffer
│
├── drawBackgroundGradient()    ← Cielo + Terreno
├── drawAnimatedClouds()        ← Nubes que se mueven
├── drawPipe(p) para cada tubería
├── drawBirdShadow()            ← Sombra debajo del pájaro
├── drawBirdEnhanced()          ← Pájaro detallado
├── particles.render()          ← Efectos de partículas
├── drawHUD()                   ← Puntuación en esquina
│
├── if (!gameStarted)
│   └── drawStartScreen()       ← Pantalla de inicio
└── if (gameStarted && gameOver)
    └── drawGameOverScreen()    ← Pantalla de game over
```

**Sistema de Coordenadas OpenGL:**
```
OpenGL normalized coordinates:
X: -1.0 (izquierda) a 1.0 (derecha)
Y: -1.0 (abajo) a 1.0 (arriba)

Ejemplo: Pájaro en centro-arriba
bird.x = 0.0
bird.y = 0.5
width = 0.1
height = 0.1
```

---

### 5. **TextureFont.java** - Renderizado de Texto

**¿Cómo funciona?**

1. **Bitmap Font**: Una imagen PNG donde cada letra ocupa una posición conocida
   ```
   font.png (256x240):
   Row 0: 0 1 2 3 4 5 6 7
   Row 1: 8 9 A B C D E F
   ...
   ```

2. **UV Mapping**: Cada carácter es un rectángulo con coordenadas de textura
   ```
   Para el carácter 'A' (índice 10):
   col = 10 % 8 = 2
   row = 10 / 8 = 1
   
   uStart = 2/8 = 0.25
   vStart = 1/5 = 0.2
   ```

3. **Renderizado**: Dibuja un quad (rectángulo) por carácter con la textura mapeada

**Problema Que Encontramos:**
```
Después de font.renderText():
GL20.glUseProgram(0);  ← Desactiva programa de OpenGL

Cuando luego drawStartScreen() intenta dibujar paneles:
drawRectAlpha() usa uniforms que no existen
Resultado: Rectángulo blanco (valores default)

SOLUCIÓN:
Al inicio de drawStartScreen() y drawGameOverScreen():
GL20.glUseProgram(programa);
GL30.glBindVertexArray(vao);
```

---

## 🔄 Flujo de Renderizado Detallado

### Inicialización (init):
```
1. GLFW.glfwInit()
   └─ Inicializa biblioteca de ventanas

2. GLFW.glfwCreateWindow()
   └─ Crea ventana 900x700 con título

3. GL.createCapabilities()
   └─ Carga funciones de OpenGL

4. Renderer.crearShaders()
   ├─ Compila vertex shader
   ├─ Compila fragment shader
   └─ Enlaza en programa

5. Renderer.crearQuad()
   ├─ Crea VAO (Vertex Array Object)
   ├─ Crea VBO (Vertex Buffer Object)
   └─ Carga geometría: 2 triángulos = 1 cuadrado
```

### Cada Frame (60 veces por segundo):
```
ANTES:                  DESPUÉS:
[Pantalla anterior]     [Pantalla nueva]
   ↓                         ↓
glClear()          ←─ Limpia con color de cielo
   ↓
processInput()     ←─ Lee entrada del usuario
   ↓
game.update(dt)    ←─ Actualiza física, colisiones
   ↓
renderer.render()  ←─ Dibuja todo en memoria
   ↓
glSwapBuffers()    ←─ Muestra frame en pantalla
```

### Shaders (GPU):

**Vertex Shader** (para cada vértice):
```glsl
#version 330 core
layout (location = 0) in vec3 aPos;
uniform vec2 uOffset;    // Posición en pantalla
uniform vec2 uScale;     // Tamaño (ancho, alto)

void main() {
    vec2 finalPos = aPos.xy * uScale + uOffset;
    gl_Position = vec4(finalPos, aPos.z, 1.0);
}
```

**Fragment Shader** (para cada píxel):
```glsl
#version 330 core
uniform vec3 uColor;     // RGB
uniform float uAlpha;    // Transparencia
out vec4 fragColor;

void main() {
    fragColor = vec4(uColor, uAlpha);  // Color final
}
```

---

## ⌨️ Sistema de Entrada

**Detección de Eventos (Edge Detection):**

```java
// Cada frame se guarda el estado ANTERIOR
private boolean prevW = false;
private boolean prevSpace = false;
private boolean prevR = false;

processInput() {
    // Estado ACTUAL
    boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
    
    // ← Detecta CAMBIO (false → true)
    if (wNow && !prevW) {
        game.bird1.jump();
    }
    
    // Guardar para siguiente frame
    prevW = wNow;
}
```

**¿Por qué edge detection?**
- Sin: Si mantienes W presionado, salta cada frame (incontrolable)
- Con: Salta UNA vez cuando presionas, puedes mantener presionado sin efecto

---

## 🎮 Lógica del Juego

### Estados:
```
INICIO (gameStarted=false, gameOver=false)
  ↓ [Usuario presiona W o SPACE]
JUGANDO (gameStarted=true, gameOver=false)
  ├─ Pájaros caen por gravedad
  ├─ Tuberías se generan cada 1.5s/dificultad
  ├─ Detecta colisiones
  └─ Incrementa puntuación al pasar tubería
  ↓ [Ambos pájaros mueren]
GAME OVER (gameStarted=true, gameOver=true)
  ↓ [Usuario presiona R]
REINICIO (gameStarted=false, gameOver=false)
```

### Generación de Tuberías:
```
spawnPipe() {
    // Gap aleatorio entre -0.45 y 0.45
    float gapCenter = random(-0.45, 0.45);
    
    // Crear tubería en posición x=1.2 (derecha)
    pipes.add(new Pipe(
        x: 1.2,
        gapCenter: gapCenter,
        width: 0.18,
        gapHeight: 0.48
    ));
}

Cada frame: pipe.x -= pipeSpeed * deltaTime
Cuando: pipe.x < -0.5 → Remover de lista
```

### Detección de Colisiones:
```
pipe.colisionaCon(bird) {
    // Primero: ¿Está el pájaro en el rango X de la tubería?
    if (bird.x - bird.width/2 > pipe.x + pipe.width/2) return false;
    if (bird.x + bird.width/2 < pipe.x - pipe.width/2) return false;
    
    // Segundo: ¿Está dentro del gap (Y)?
    float topPipe = pipe.gapCenter + pipe.gapHeight/2;
    float bottomPipe = pipe.gapCenter - pipe.gapHeight/2;
    
    if (bird.y > topPipe || bird.y < bottomPipe) return true;  // ¡COLISIÓN!
    return false;
}
```

---

## 🎨 Sistemas Especiales

### ParticleSystem:
```
jump_dust() {
    // Pequeñas partículas cuando salta
    Para cada partícula:
    - Posición: En el pájaro
    - Velocidad: Hacia abajo (reacción del salto)
    - Vida: 0.5 segundos
    - Color: Amarillo/naranja
}

burst() {
    // Explosión cuando muere
    15 partículas en todas direcciones
    Vida más larga: 1 segundo
    Color: Del pájaro que murió
}
```

### SoundManager:
```
playJumpSound() {
    // 800 Hz por 100ms
    Genera onda seno en tiempo real
    Reproduce en thread separado
    No bloquea el juego
}

playPointSound() {
    // 1200 Hz por 150ms
    Más agudo (puntuación)
}
```

### Dificultad Progresiva:
```
multiplier = 1.0 + (maxScore * 0.05)

Score 0:  1.0x speed  (normal)
Score 10: 1.5x speed
Score 20: 2.0x speed  (máximo)

Afecta:
✓ Velocidad de tuberías
✓ Frecuencia de aparecer tuberías (más juntas)
✗ NO afecta gravedad del pájaro (sigue siendo igual)
```

---

## 🔧 Detalles Técnicos Importantes

### Delta Time (dt):
```
Es el tiempo REAL transcurrido desde el último frame

Sin dt (MALO):
├─ 60 FPS: bird.y += 5
├─ 30 FPS: bird.y += 5  ← ¡Cae más lento!

Con dt (BIEN):
├─ 60 FPS (dt=0.016): bird.y += -9.8 * 0.016 = -0.157
├─ 30 FPS (dt=0.033): bird.y += -9.8 * 0.033 = -0.323  ← Movimiento proporcional
```

### Blending (Transparencia):
```java
GL11.glEnable(GL11.GL_BLEND);
GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

Fórmula:
final = source * source.alpha + dest * (1 - source.alpha)

Ejemplo: Panel semi-transparente negro
source = (0, 0, 0, 0.5)
dest = (0.5, 0.8, 0.9, 1.0) ← Cielo atrás
final = (0, 0, 0) * 0.5 + (0.5, 0.8, 0.9) * 0.5
      = (0.25, 0.4, 0.45) ← Cielo oscurecido
```

### Frame Rate Clamping:
```java
if (deltaTime > 0.033f) {
    deltaTime = 0.033f;  // Max 30 FPS de update
}

¿Por qué? Si el juego se congela (lag), dt sería enorme
Esto evita que los pájaros atraviesen paredes en 1 frame
```

---

## 📊 Diagrama General de Flujo

```
┌─────────────────────────────────────────────┐
│         FlappyBirdGame.main()               │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
    init()              loop() (infinito)
    │                    │
    ├─ GLFW              ├─ processInput()
    ├─ OpenGL            ├─ game.update(dt)
    ├─ Renderer          ├─ renderer.render()
    ├─ Game              └─ glSwapBuffers()
    └─ TextureFont            │
                              └─→ 60 FPS

En cada game.update(dt):
  ├─ bird.update(dt)        ← Física
  ├─ pipe.update(speed)     ← Movimiento
  ├─ Detectar colisiones
  └─ Actualizar puntuación

En cada renderer.render():
  ├─ Enviar geometría a GPU
  ├─ Aplicar transformaciones
  ├─ Renderizar capas en orden
  └─ Mostrar en pantalla
```

---

## 🎓 Conceptos Clave Aprendidos

| Concepto | Uso en este Proyecto |
|----------|----------------------|
| **Edge Detection** | Saltar solo 1 vez al presionar |
| **Delta Time** | Movimiento independiente de FPS |
| **Shaders** | Renderizar rectángulos y colores |
| **VAO/VBO** | Buffers de geometría en GPU |
| **Blending** | Transparencia de paneles |
| **Texture Mapping** | Renderizar texto desde bitmap |
| **Thread Safety** | Reproducir sonido sin bloquear |
| **State Management** | Estados del juego (inicio, jugando, game over) |
| **Collision Detection** | AABB (Axis-Aligned Bounding Box) |
| **Dificultad Progresiva** | Escalar velocidad según puntuación |

---

## 💡 Mejoras Futuras Posibles

```
1. Efectos visuales
   - Screenshake al morir
   - Slow-motion al perder
   - Efectos de luz

2. Gameplay
   - Power-ups (escudo, x2 puntos)
   - Modos especiales (túneles, lluvia)
   - Leaderboard local

3. Audio
   - Música de fondo
   - Efectos sonoros más variados
   - Cambio de tema musical por dificultad

4. Redes
   - Multijugador en red (TCP/UDP)
   - Sincronización de entrada
```

---

**Creado con ❤️ para entender los fundamentos de juegos en Java + OpenGL**
