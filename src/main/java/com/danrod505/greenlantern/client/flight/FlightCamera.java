package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GLClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;

/** Sense of speed: wider FOV, banking into turns, barrel-roll camera spin and supersonic buffeting. */
public final class FlightCamera {
    private FlightCamera() {}

    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player || !FlightController.isPowerFlying()) return;
        float f = FlightController.speedFraction();
        float multiplier = 1.0F + 0.42F * (float) Math.pow(f, 1.2) + FlightController.boomPunch();
        event.setNewFovModifier(event.getNewFovModifier() * multiplier);
    }

    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!FlightController.isPowerFlying() || !GLClientConfig.CAMERA_EFFECTS.get()) return;
        float partial = (float) event.getPartialTick();
        if (mc.options.getCameraType().isFirstPerson()) {
            event.setRoll(event.getRoll() + FlightController.bank(partial) + FlightController.rollAngle(partial));
        }
        float mach = FlightController.mach();
        if (mach > 0.8F) {
            float amount = Math.min(1.0F, (mach - 0.8F) * 0.8F) * 0.35F;
            float time = (float) (System.nanoTime() / 1.0E7 % 100000.0);
            event.setPitch(event.getPitch() + Mth.sin(time * 1.9F) * amount);
            event.setYaw(event.getYaw() + Mth.cos(time * 2.3F) * amount * 0.7F);
        }
    }
}
