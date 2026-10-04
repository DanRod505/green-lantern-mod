package com.danrod505.greenlantern.client.render.aqua;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Vector3f;

public class KrakenRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    /** 0 walking on land, 1 swimming (blends in between). */
    public float swim;
    public float walkPhase;
    public float walkAmount;
    public float swimWave;
    public float swimPower;
    /** Water jet blend (0-1). */
    public float jet;
    /** How far the body slumps after a step (blocks). */
    public float bob;
    public float squash;
    /** Ticks since the last tentacle slam started. */
    public float slamTime = 1000.0F;
    public boolean slamLeft;
    public boolean hurt;
    /** Death animation progress (0-1). */
    public float death;
    /** Rising out of the deep after being called (0-1). */
    public float emerge = 1.0F;
    public boolean jetting;
    public final Vector3f siphon = new Vector3f();
    public final Vector3f jetEnd = new Vector3f();
}
