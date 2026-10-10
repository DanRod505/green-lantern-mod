package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.GLClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;

/** Sense of speed while running: the FOV stretches with speed, punches at the sound barrier and the camera buzzes. */
public final class SpeedCamera {
    private static float fov = 1.0F;

    private SpeedCamera() {}

    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        float vanilla = event.getNewFovModifier();
        boolean active = SpeedController.isRunning();
        float target = active ? 1.12F + 0.5F * (float) Math.pow(SpeedController.speedFraction(), 0.9) + SpeedController.boomPunch() : vanilla;
        if (!active && Math.abs(fov - vanilla) < 0.005F) {
            fov = vanilla;
            return;
        }
        fov += (target - fov) * 0.25F;
        event.setNewFovModifier(fov);
    }

    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!SpeedController.isRunning() || !GLClientConfig.CAMERA_EFFECTS.get() || mc.player == null) return;
        float time = (float) (System.nanoTime() / 1.0E7 % 100000.0);
        float f = SpeedController.speedFraction();
        // Footfalls at superhuman tempo: a fine, fast bob that turns into buzzing past the barrier.
        float bob = (SpeedController.isSupersonic() ? 0.45F : 0.22F) * f;
        if (mc.options.getCameraType().isFirstPerson()) {
            event.setPitch(event.getPitch() + Mth.sin(time * 3.1F) * bob);
            event.setYaw(event.getYaw() + Mth.cos(time * 2.7F) * bob * 0.6F);
            if (SpeedController.isOrbiting()) {
                // Leaning into the circle around the tornado.
                event.setRoll(event.getRoll() - 14.0F);
            }
            if (SpeedController.isWallRunning()) {
                event.setRoll(event.getRoll() + Mth.sin(time * 4.3F) * 0.6F);
            }
        }
    }
}
