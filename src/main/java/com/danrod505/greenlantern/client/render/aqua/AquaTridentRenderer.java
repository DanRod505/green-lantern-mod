package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.entity.AquaTridentEntity;
import com.danrod505.greenlantern.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * The thrown trident: the 3D item model (the same one held in the hand), pointed along its flight;
 * flying home it turns around, prongs first towards Aquaman.
 */
public class AquaTridentRenderer extends EntityRenderer<AquaTridentEntity, AquaTridentRenderer.State> {
    /** Direction the prongs point to in the item model (laid diagonally, like a held tool). */
    private static final float MODEL_DIR = Mth.SQRT_OF_TWO / 2.0F;
    private static final float SCALE = 1.7F;

    private final ItemModelResolver itemModelResolver;
    private ItemStack stack;

    public AquaTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AquaTridentEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (stack == null) stack = new ItemStack(ModItems.AQUAMAN_TRIDENT.get());
        itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.NONE, entity);
        // Projectile rotations: yaw around Y from +Z, pitch up from the horizontal.
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) * Mth.DEG_TO_RAD;
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(pitch);
        state.dirX = Mth.sin(yaw) * cos;
        state.dirY = Mth.sin(pitch);
        state.dirZ = Mth.cos(yaw) * cos;
        state.spin = entity.isReturning() ? state.ageInTicks * 0.6F : 0.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationTo(MODEL_DIR, MODEL_DIR, 0.0F, state.dirX, state.dirY, state.dirZ));
        if (state.spin != 0.0F) poseStack.mulPose(new Quaternionf().rotationAxis(state.spin, MODEL_DIR, MODEL_DIR, 0.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float dirX;
        public float dirY = 1.0F;
        public float dirZ;
        public float spin;
    }
}
