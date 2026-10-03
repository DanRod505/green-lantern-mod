package com.danrod505.greenlantern.client.render.oa;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Render state shared by the inhabitants of Oa (Lanterns and Guardians). */
public class OaNpcRenderState extends HumanoidRenderState {
    public int variant;
    public boolean flying;
    public boolean guardian;
    public float headScale = 1.0F;
    public float sizeScale = 1.0F;
}
