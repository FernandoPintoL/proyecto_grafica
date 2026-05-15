package com.graphics.flappybird;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ParticleSystem: genera efectos visuales con partículas.
 * Usado para explosiones, polvo, trail de movimiento.
 */
public class ParticleSystem {
    private List<Particle> particles;
    private Random random;

    public ParticleSystem() {
        particles = new ArrayList<>();
        random = new Random();
    }

    /**
     * Partícula individual: posición, velocidad, tiempo de vida, color.
     */
    private static class Particle {
        float x, y;
        float velX, velY;
        float life;      // 0.0 = muerta, 1.0 = nueva
        float maxLife;
        float r, g, b;
        float size;

        Particle(float x, float y, float velX, float velY, float life,
                 float r, float g, float b, float size) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.life = life;
            this.maxLife = life;
            this.r = r;
            this.g = g;
            this.b = b;
            this.size = size;
        }

        void update(float deltaTime) {
            // Gravedad suave.
            velY -= 0.5f * deltaTime;
            x += velX * deltaTime;
            y += velY * deltaTime;

            // Desvanecimiento.
            life -= deltaTime * 2.0f; // Muere rápido.
            if (life < 0.0f) life = 0.0f;
        }

        boolean estaViva() {
            return life > 0.0f;
        }
    }

    /**
     * Crea explosión en una posición (cuando el pájaro muere).
     */
    public void burst(float x, float y, float r, float g, float b, int count) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = 0.5f + random.nextFloat() * 1.5f;
            float velX = (float) Math.cos(angle) * speed;
            float velY = (float) Math.sin(angle) * speed;

            particles.add(new Particle(x, y, velX, velY, 1.0f,
                                      r + random.nextFloat() * 0.2f - 0.1f,
                                      g + random.nextFloat() * 0.2f - 0.1f,
                                      b + random.nextFloat() * 0.2f - 0.1f,
                                      0.03f + random.nextFloat() * 0.02f));
        }
    }

    /**
     * Crea trail de polvo (cuando el pájaro salta).
     */
    public void jumpDust(float x, float y) {
        for (int i = 0; i < 5; i++) {
            float angle = (float) Math.PI * (0.5f + random.nextFloat() * 0.5f); // Hacia arriba.
            float speed = 0.3f + random.nextFloat() * 0.5f;
            float velX = (float) Math.cos(angle) * speed * (random.nextBoolean() ? 1 : -1);
            float velY = (float) Math.sin(angle) * speed;

            particles.add(new Particle(x, y, velX, velY, 0.6f,
                                      0.9f, 0.9f, 0.9f, 0.015f));
        }
    }

    /**
     * Crea puntos flotantes cuando el pájaro pasa una tubería.
     */
    public void scorePopup(float x, float y) {
        // Partículas que flotan hacia arriba.
        for (int i = 0; i < 8; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = 0.2f + random.nextFloat() * 0.3f;
            float velX = (float) Math.cos(angle) * speed;
            float velY = 0.4f + random.nextFloat() * 0.3f; // Hacia arriba.

            particles.add(new Particle(x, y, velX, velY, 1.0f,
                                      0.0f, 1.0f, 0.5f, 0.02f)); // Verde.
        }
    }

    /**
     * Actualiza todas las partículas.
     */
    public void update(float deltaTime) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(deltaTime);
            if (!p.estaViva()) {
                it.remove();
            }
        }
    }

    /**
     * Renderiza todas las partículas. (Llamado por Renderer)
     */
    public void render(Renderer renderer) {
        for (Particle p : particles) {
            // Alfa (transparencia) según vida.
            float alpha = p.life / p.maxLife;
            renderer.drawParticle(p.x, p.y, p.size, p.r, p.g, p.b, alpha);
        }
    }

    /**
     * Retorna cantidad de partículas activas.
     */
    public int getActiveCount() {
        return particles.size();
    }
}
