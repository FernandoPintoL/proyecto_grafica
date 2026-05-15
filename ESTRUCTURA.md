# Estructura del Proyecto - Flappy Bird Refactorizado

## 📦 Organización de Directorios

```
opengl-java-class/
├── src/main/java/com/graphics/
│   ├── flappybird/                    ← NUEVO PAQUETE (Refactorización)
│   │   ├── FlappyBirdGame.java        (Main - Orquestación)
│   │   ├── Game.java                  (Lógica central: dos pájaros, dificultad)
│   │   ├── Bird.java                  (Pájaro individual)
│   │   ├── Pipe.java                  (Tubería / obstáculo)
│   │   ├── Renderer.java              (Gráficos: pájaros, HUD, pantallas)
│   │   └── SoundManager.java          (Sonidos nativos)
│   │
│   ├── App.java                       (Triángulo - Ejemplo básico)
│   ├── App3D.java                     (Cubo 3D - Ejemplo)
│   ├── AppCamara.java                 (Cámara first-person - Ejemplo)
│   ├── AppFlappyBird.java             (Original sin refactorizar - Referencia)
│   ├── AppLaberinto.java              (Laberinto 3D - Ejemplo)
│   ├── AppMovimientoTeclado.java      (Triángulo movible - Ejemplo)
│   ├── AppMovimientoZoom.java         (Movimiento + zoom - Ejemplo)
│   └── AppZoom.java                   (Zoom - Ejemplo)
│
├── pom.xml                            (Maven - mainClass → com.graphics.flappybird.FlappyBirdGame)
├── README.md                          (Documentación principal)
├── ESTRUCTURA.md                      (Este archivo)
└── target/                            (Compilados - .class)
```

---

## 🎯 Paquete Principal: `com.graphics.flappybird`

### Clases y Responsabilidades

| Clase | LOC | Responsabilidad |
|-------|-----|-----------------|
| **FlappyBirdGame.java** | 180 | Main, GLFW/OpenGL init, loop principal, input procesamiento |
| **Game.java** | 180 | Lógica del juego: dos pájaros, tuberías, dificultad progresiva, colisiones |
| **Bird.java** | 95 | Pájaro individual: posición, velocidad, gravedad, salto, reset |
| **Pipe.java** | 60 | Tubería: movimiento, colisión AABB, validaciones |
| **Renderer.java** | 320 | Renderizado: pájaros compuestos (5 componentes), tuberías, HUD, pantallas |
| **SoundManager.java** | 130 | Sonidos nativos: síntesis de tonos (salto, punto, game over) |
| **TOTAL** | **960** | **Arquitectura limpia y modular** |

---

## 🔗 Dependencias Entre Clases

```
FlappyBirdGame
    ├── usa → Game
    ├── usa → Renderer
    └── usa → SoundManager

Game
    ├── usa → Bird (x2: bird1, bird2)
    ├── usa → Pipe (list)
    └── Random

Renderer
    ├── usa → Game (referencia de datos)
    ├── usa → Bird
    └── usa → Pipe

SoundManager
    └── usa → javax.sound.sampled (nativo de Java)
```

---

## 📁 Archivos de Referencia (Ejemplos)

Los archivos en `com.graphics` (sin paquete adicional) son **ejemplos educativos**:
- `App.java` - Triángulo básico (Hello World de OpenGL)
- `App3D.java` - Cubo con rotación y zoom
- `AppCamara.java` - Cámara first-person
- `AppFlappyBird.java` - Versión original monolítica (referencia)
- `AppLaberinto.java` - Laberinto navegable
- etc.

**No interfieren** con el Flappy Bird refactorizado.

---

## 🚀 Comandos de Ejecución

### Compilar
```bash
cd "d:\PROG GRAFICA\proyecto\opengl-java-class"
mvn clean compile
```

### Ejecutar Flappy Bird Refactorizado
```bash
mvn exec:exec
```
(Automáticamente ejecuta `com.graphics.flappybird.FlappyBirdGame`)

### Cambiar Main (si necesario)
```bash
mvn exec:exec -DmainClass=com.graphics.App
```

---

## ✅ Ventajas de la Separación

✔️ **Claridad:** Código refactorizado separado de ejemplos originales  
✔️ **Mantenibilidad:** Cada clase tiene una responsabilidad clara  
✔️ **Extensibilidad:** Fácil agregar nuevas features sin afectar ejemplos  
✔️ **Compilación limpia:** 14 archivos, 0 errores, 0 warnings de código  

---

## 📊 Estadísticas

| Métrica | Valor |
|---------|-------|
| Archivos en paquete flappybird | 6 |
| Líneas de código (flappybird) | ~960 |
| Archivos ejemplos (com.graphics) | 8 |
| Total archivos compilables | 14 |
| Dependencias externas | LWJGL 3.3.3 + Java stdlib |
| Versión Java requerida | 17+ |

---

**Última actualización:** 2026-05-15
