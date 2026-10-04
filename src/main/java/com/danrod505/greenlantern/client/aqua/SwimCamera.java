package com.danrod505.greenlantern.client.aqua;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ComputeFovModifierEvent;

/** Sense of speed while swimming: the field of view widens as Aquaman picks up speed. */
public final class SwimCamera {
    private static float fov = 1.0F;

    private SwimCamera() {}

    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        float vanilla = event.getNewFovModifier();
        boolean active = SwimController.isSwimming() && SwimController.speed() > 0.5;
        float target = active ? vanilla + 0.3F * (float) Math.pow(SwimController.speedFraction(), 0.9) : vanilla;
        if (!active && Math.abs(fov - vanilla) < 0.005F) {
            fov = vanilla;
            return;
        }
        fov += (target - fov) * 0.2F;
        event.setNewFovModifier(fov);
    }
}
