package com.danrod505.greenlantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry a hero draws in the world every frame (beams, outlines seen through walls), through the
 * shared client-only trail entity that follows the camera (see {@code client.flight.TrailRenderer}).
 * A hero hands one out from {@link HeroClient#worldLayer}.
 */
public interface WorldLayer {
    /** While the frame's render state is extracted: works out what to draw, relative to {@code origin}. */
    void extract(Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, float partialTick);

    /** Draws what {@link #extract} worked out. */
    void submit(PoseStack poseStack, SubmitNodeCollector collector);
}
