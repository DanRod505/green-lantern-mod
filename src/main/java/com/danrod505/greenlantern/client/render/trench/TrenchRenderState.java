package com.danrod505.greenlantern.client.render.trench;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state of a creature of the Trench. */
public class TrenchRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float swimPhase;
    public float swimAmount;
    /** 0-1 over a claw strike (1 when it begins). */
    public float attack;
    public boolean brute;
    public boolean carrying;
    public boolean hurt;
    public float deathTime;
    public float scale = 1.0F;
}
