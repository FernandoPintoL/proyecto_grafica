# Flappy Bird - OpenGL (Java + LWJGL) - Primer Parcial

**Autor:** Fernando Pintolino  
**Fecha:** 2026-05-15  
**Versión:** 1.0

---

## 📋 Descripción del Proyecto

Implementación de **Flappy Bird para dos jugadores simultáneos** usando OpenGL 3.3 Core Profile con LWJGL en Java.

### Características Implementadas

✅ **Refactorización en 6 Clases Especializadas**
- Bird.java - Lógica de pájaros individuales
- Pipe.java - Obstáculos reutilizables
- Game.java - Lógica central (dos pájaros, dificultad progresiva)
- Renderer.java - Renderizado gráfico completo
- SoundManager.java - Sonidos nativos sin archivos externos
- FlappyBirdGame.java - Orquestación principal

✅ **Pájaros Compuestos** - Cada pájaro formado por:
- Cuerpo (rectángulo, color distinto por jugador)
- Pico (triángulo, amarillo)
- Ala (rectángulo que escala con velocidad)
- Cola (rectángulo pequeño)
- Ojo (punto blanco)

✅ **Modo Dos Jugadores**
- P1: Tecla W (naranja)
- P2: Barra ESPACIO (azul)
- Controles independientes, puntajes separados
- Game Over cuando ambos chocan

✅ **Dificultad Progresiva**
- Velocidad +5% por punto (máximo 2.0x)
- Frecuencia de tuberías aumenta
- Mostrado en tiempo real en barra de título

✅ **Interfaz Mejorada**
- Fondo degradado (cielo + suelo)
- Nubes decorativas
- **HUD con tipografía legible** (números 7-segment + letras)
  - P1 Score (panel naranja)
  - P2 Score (panel azul)
  - Multiplicador de dificultad (panel verde)
- Pantallas de inicio y game over con instrucciones claras
- Reinicio con tecla R
- **Sistema BitmapFont:** Renderiza números y texto sin librerías externas

✅ **Sonidos Nativos**
- Síntesis en tiempo real (800Hz, 1200Hz, barrido)
- Sin archivos externos

---

## 🎮 Controles

| Acción | P1 | P2 |
|--------|----|----|
| Saltar | W | ESPACIO |
| Iniciar | W o ESPACIO | W o ESPACIO |
| Reiniciar | R | R |
| Salir | ESC | ESC |

---

## 🔨 Compilación e Instalación

### Requisitos
- Java 17+
- Maven 3.6+
- Windows 10/11 (x64)

### Ejecutar
```bash
cd "d:\PROG GRAFICA\proyecto\opengl-java-class"
mvn clean compile exec:exec
```

---

## 📁 Archivos Creados/Modificados

```
src/main/java/com/graphics/
├── Bird.java               (NUEVO)
├── Pipe.java               (NUEVO)
├── Game.java               (NUEVO)
├── Renderer.java           (NUEVO)
├── SoundManager.java       (NUEVO)
├── FlappyBirdGame.java     (NUEVO)
└── (originales sin cambios)

pom.xml                      (MODIFICADO - mainClass)
README.md                    (NUEVO - este archivo)
```

---

## 🎯 Requisitos Cumplidos

- [x] Pájaro compuesto (cuerpo, pico, ala, cola, ojo)
- [x] Dos jugadores simultáneos (W y ESPACIO)
- [x] Incremento progresivo de velocidad
- [x] Interfaz mejorada (fondo, HUD, pantallas)
- [x] Código organizado en clases separadas
- [x] Sonidos nativos
- [x] Compilable con Maven
- [x] README con instrucciones

---

Última actualización: 2026-05-15
