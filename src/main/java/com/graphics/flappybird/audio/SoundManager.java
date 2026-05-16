package com.graphics.flappybird.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.graphics.flappybird.services.IAudioService;

/**
 * SoundManager: genera y reproduce sonidos simples usando sÃ­ntesis de tonos.
 * No requiere archivos externos.
 * Implementa IAudioService para ser usado a travÃ©s del Service Locator.
 */
public class SoundManager implements IAudioService {
    private static final int SAMPLE_RATE = 44100;
    private static final float VOLUME = 1.0f;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundManager");
        t.setDaemon(true);
        return t;
    });

    // Executor para música de fondo (thread separado para no bloquear efectos)
    private static final ExecutorService musicExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BackgroundMusic");
        t.setDaemon(true);
        return t;
    });

    // Control de música de fondo
    private static volatile boolean backgroundMusicRunning = false;

    /**
     * Reproduce un sonido simple: tono de salto (beep corto).
     */
    @Override
    public void playJumpSound() {
        playTone(800, 100); // 800 Hz por 100 ms.
    }

    /**
     * Reproduce sonido de punto (bip agudo).
     */
    @Override
    public void playPointSound() {
        playTone(1200, 150); // 1200 Hz por 150 ms.
    }

    /**
     * Reproduce sonido de colisión (pájaro golpeado).
     * Dos tonos bajos usando volumen explícito (como playBackgroundMusic).
     * Usa musicExecutor para evitar conflictos de threads.
     */
    @Override
    public void playCollisionSound() {
        musicExecutor.execute(() -> {
            try {
                playToneWithVolume(400, 200, 1.0f); // Tono bajo 400 Hz, volumen máximo
                Thread.sleep(150);
                playToneWithVolume(300, 200, 1.0f); // Tono más bajo 300 Hz, volumen máximo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Reproduce sonido de game over (descenso).
     */
    @Override
    public void playGameOverSound() {
        // Descenso de 800 Hz a 200 Hz en 400 ms.
        playFrequencySweep(800, 200, 400);
    }

    /**
     * Reproduce sonido de tensión/velocidad (barrido ascendente).
     * Hace que parezca que el juego va rápido.
     * Frecuencia: 400 Hz → 800 Hz en 150 ms.
     */
    public void playTensionSound() {
        playFrequencySweep(400, 800, 150);
    }

    /**
     * Alerta rápida de peligro/velocidad máxima.
     * Dos tonos cortos y agudos (1000 Hz).
     */
    public void playSpeedWarning() {
        executor.execute(() -> {
            playTone(1000, 100);
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playTone(1000, 100);
        });
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
                float sample = (float) Math.sin(phase) * VOLUME;

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

    /**
     * Inicia música de fondo sintetizada (melodía en loop continuo).
     * La música se ejecuta en thread separado para no bloquear el juego.
     * Melodía alegre tipo Flappy Bird: Do-Mi-Sol-Mi en loop.
     */
    @Override
    public void playBackgroundMusic() {
        if (backgroundMusicRunning) {
            return; // Ya está tocando
        }

        backgroundMusicRunning = true;
        musicExecutor.execute(() -> {
            try {
                // Frecuencias de las notas (Do mayor pentatónica)
                float[] melody = {
                    261.63f,  // Do (C4)
                    329.63f,  // Mi (E4)
                    392.00f,  // Sol (G4)
                    329.63f,  // Mi (E4)
                    293.66f,  // Re (D4)
                    329.63f,  // Mi (E4)
                    392.00f,  // Sol (G4)
                    440.00f   // La (A4)
                };

                int noteDurationMs = 200; // Duración de cada nota (acelerado)

                while (backgroundMusicRunning) {
                    for (float frequency : melody) {
                        if (!backgroundMusicRunning) {
                            return;
                        }
                        // Tocar nota con volumen más bajo (0.4f para que sea fondo)
                        playToneWithVolume(frequency, noteDurationMs, 0.4f);
                    }

                    // Pausa entre repeticiones de la melodía
                    Thread.sleep(3);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Detiene la música de fondo.
     */
    @Override
    public void stopBackgroundMusic() {
        backgroundMusicRunning = false;
    }

    /**
     * Toca un tono con volumen personalizado.
     * Similar a playTone() pero con volumen configurable.
     */
    private static void playToneWithVolume(float frequency, int durationMs, float volume) {
        try {
            int samples = (SAMPLE_RATE * durationMs) / 1000;
            byte[] audioData = new byte[samples * 2]; // 16-bit

            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;
                float sample = (float) Math.sin(angle) * volume;

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
            // Silenciar errores de sonido
        }
    }
}
