package com.danrod505.greenlantern.client.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ComputeFovModifierEvent;

/**
 * The zoom of the presets of the engine: the view narrows (or widens) for a moment and comes back,
 * easing in and out. A preset sends it with its {@code "zoom"} setting.
 */
public final class EngineCamera {
    private static float target = 1.0F;
    private static int duration;
    private static int remaining;

    private EngineCamera() {}

    /** Zooms to {@code fov} (1 is normal, 0.6 close, 1.3 wide) for {@code ticks}. */
    public static void zoom(float fov, int ticks) {
        target = Mth.clamp(fov, 0.3F, 1.6F);
        duration = Math.max(2, ticks);
        remaining = duration;
    }

    public static void tick() {
        if (remaining > 0) remaining--;
    }

    /** How far into the zoom the view is now, 0 to 1. */
    private static float amount(float partial) {
        if (remaining <= 0) return 0.0F;
        float age = duration - remaining + partial;
        float in = Math.min(1.0F, age / Math.max(1.0F, duration * 0.2F));
        float out = Math.min(1.0F, (remaining - partial) / Math.max(1.0F, duration * 0.3F));
        float t = Math.max(0.0F, Math.min(in, out));
        return t * t * (3 - 2 * t);
    }

    public static void onFov(ComputeFovModifierEvent event) {
        if (remaining <= 0 || event.getPlayer() != Minecraft.getInstance().player) return;
        float t = amount(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        event.setNewFovModifier(event.getNewFovModifier() * Mth.lerp(t, 1.0F, target));
    }
}
