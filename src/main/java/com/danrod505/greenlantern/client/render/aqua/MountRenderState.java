package com.danrod505.greenlantern.client.render.aqua;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state of the sea creatures of Atlantis (manta ray, seahorse, dolphin). */
public class MountRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float swimPhase;
    public float swimAmount;
    public boolean outOfWater;
    public boolean ridden;
    public boolean hurt;
    public float deathTime;
    public int variant;
}
