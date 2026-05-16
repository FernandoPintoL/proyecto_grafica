package com.graphics.flappybird.services;

/**
 * Service Locator: registro central de servicios globales.
 *
 * Este patrón permite que las clases accedan a servicios sin tener
 * que conocerse directamente entre sí, reduciendo el acoplamiento.
 *
 * Uso:
 *   ServiceLocator.provideAudio(new SoundManager());
 *   IAudioService audio = ServiceLocator.audio();
 *   audio.playJumpSound();
 */
public class ServiceLocator {
    // Servicios registrados (estáticos, singleton)
    private static IAudioService audioService;
    private static IParticleService particleService;
    private static IFontService fontService;

    // ===== REGISTRO DE SERVICIOS =====

    /**
     * Registra la implementación del servicio de audio.
     */
    public static void provideAudio(IAudioService service) {
        audioService = service;
    }

    /**
     * Registra la implementación del servicio de partículas.
     */
    public static void provideParticles(IParticleService service) {
        particleService = service;
    }

    /**
     * Registra la implementación del servicio de fuente de texto.
     */
    public static void provideFont(IFontService service) {
        fontService = service;
    }

    // ===== ACCESO A SERVICIOS =====

    /**
     * Obtiene el servicio de audio.
     */
    public static IAudioService audio() {
        return audioService;
    }

    /**
     * Obtiene el servicio de partículas.
     */
    public static IParticleService particles() {
        return particleService;
    }

    /**
     * Obtiene el servicio de fuente de texto.
     */
    public static IFontService font() {
        return fontService;
    }
}
