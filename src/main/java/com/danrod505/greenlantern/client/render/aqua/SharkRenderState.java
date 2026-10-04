package com.danrod505.greenlantern.client.render.aqua;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SharkRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float tailPhase;
    public float swimAmount;
    public float jaw;
    /** 0-1 progress of the lunge and bite, or -1 when not lunging. */
    public float lunge = -1.0F;
    public boolean outOfWater;
}
