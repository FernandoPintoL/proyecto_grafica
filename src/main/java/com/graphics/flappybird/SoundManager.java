package com.graphics.flappybird;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SoundManager: genera y reproduce sonidos simples usando síntesis de tonos.
 * No requiere archivos externos.
 */
public class SoundManager {
    private static final int SAMPLE_RATE = 44100;
    private static final float VOLUME = 0.7f;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundManager");
        t.setDaemon(true);
        return t;
    });

    /**
     * Reproduce un sonido simple: tono de salto (beep corto).
     */
    public static void playJumpSound() {
        playTone(800, 100); // 800 Hz por 100 ms.
    }

    /**
     * Reproduce sonido de punto (bip agudo).
     */
    public static void playPointSound() {
        playTone(1200, 150); // 1200 Hz por 150 ms.
    }

    /**
     * Reproduce sonido de game over (descenso).
     */
    public static void playGameOverSound() {
        // Descenso de 600 Hz a 300 Hz en 300 ms.
        playFrequencySweep(600, 300, 300);
    }

    /**
     * Genera y reproduce un tono simple.
     */
    private static void playTone(float frequency, int durationMs) {
        try {
            int samples = (SAMPLE_RATE * durationMs) / 1000;
            byte[] audioData = new byte[samples * 2]; // 16-bit

            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;
                float sample = (float) Math.sin(angle) * VOLUME;

                // Envelope: fade in/out para evitar clicks.
                float envelope = 1.0f;
                if (i < SAMPLE_RATE / 100) { // Fade in 10 ms.
                    envelope = i / (float) (SAMPLE_RATE / 100);
                }
                if (i > samples - (SAMPLE_RATE / 100)) { // Fade out 10 ms.
                    envelope = (samples - i) / (float) (SAMPLE_RATE / 100);
                }

                short sample16 = (short) (sample * envelope * 32767);
                audioData[i * 2] = (byte) (sample16 & 0xFF);
                audioData[i * 2 + 1] = (byte) ((sample16 >> 8) & 0xFF);
            }

            playAudioData(audioData);
        } catch (Exception e) {
            // Silenciar errores de sonido para no romper el juego.
        }
    }

    /**
     * Reproduce un barrido de frecuencias (para descenso).
     */
    private static void playFrequencySweep(float startFreq, float endFreq, int durationMs) {
        try {
            int samples = (SAMPLE_RATE * durationMs) / 1000;
            byte[] audioData = new byte[samples * 2];

            for (int i = 0; i < samples; i++) {
                float progress = (float) i / samples;
                float freq = startFreq + (endFreq - startFreq) * progress;

                // Sumar fases para barrido continuo.
                double phase = 2.0 * Math.PI * i * (startFreq + (endFreq - startFreq) * progress * 0.5f) / SAMPLE_RATE;
                float sample = (float) Math.sin(phase) * VOLUME * (1.0f - progress); // Fade out.

                short sample16 = (short) (sample * 32767);
                audioData[i * 2] = (byte) (sample16 & 0xFF);
                audioData[i * 2 + 1] = (byte) ((sample16 >> 8) & 0xFF);
            }

            playAudioData(audioData);
        } catch (Exception e) {
            // Silenciar.
        }
    }

    /**
     * Reproduce datos de audio en el ejecutor de sonidos para no bloquear.
     */
    private static void playAudioData(byte[] audioData) {
        executor.execute(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                AudioInputStream audioStream = new AudioInputStream(
                    new java.io.ByteArrayInputStream(audioData),
                    format,
                    audioData.length / 2
                );

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();

                int durationMs = (audioData.length / 2) / (SAMPLE_RATE / 1000);
                Thread.sleep(durationMs + 50);
                clip.close();
            } catch (Exception e) {
                // Ignorar errores de sonido.
            }
        });
    }
}
