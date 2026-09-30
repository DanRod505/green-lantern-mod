package com.danrod505.greenlantern.client;

import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;

/** Short camera shake used by the hammer impact. */
public final class CameraShake {
    private static float intensity;
    private static int duration;
    private static int remaining;

    private CameraShake() {}

    public static void start(float strength, int ticks) {
        if (strength * ticks > intensity * remaining) {
            intensity = Mth.clamp(strength, 0.0F, 1.0F);
            duration = Math.max(1, ticks);
            remaining = duration;
        }
    }

    public static void tick() {
        if (remaining > 0) remaining--;
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remaining <= 0) return;
        float t = (remaining - (float) event.getPartialTick()) / duration;
        float amount = intensity * t * t * 3.0F;
        float time = (float) (System.nanoTime() / 1.0E7 % 10000.0);
        event.setPitch(event.getPitch() + Mth.sin(time * 0.9F) * amount);
        event.setYaw(event.getYaw() + Mth.cos(time * 1.3F) * amount * 0.6F);
        event.setRoll(event.getRoll() + Mth.sin(time * 1.7F) * amount * 0.5F);
    }
}
