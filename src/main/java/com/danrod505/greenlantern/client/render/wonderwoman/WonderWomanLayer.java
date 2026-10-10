package com.danrod505.greenlantern.client.render.wonderwoman;

import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.client.wonderwoman.WonderWomanVisuals;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.wonderwoman.AmazonFlags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What the flat armor texture can't do: the silver Bracelets of Submission standing out on her
 * forearms (blazing gold while she guards), the coiled Lasso of Truth at her hip, and the golden rope
 * whirling over her head while she spins it.
 */
public class WonderWomanLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int SILVER = 0xFFC9CED6;
    private static final int SILVER_EDGE = 0xFF8C929C;
    private static final int GOLD_STAR = 0xFFF2C21A;

    public WonderWomanLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.chestEquipment.is(ModItems.WONDER_WOMAN_SUIT.get())) return;
        int flags = WonderWomanVisuals.flags(state.id);
        boolean guarding = AmazonFlags.has(flags, AmazonFlags.GUARD);
        submitBracelets(poseStack, collector, light, state, guarding);
        boolean lassoOut = lassoOut(state.id);
        if (AmazonFlags.has(flags, AmazonFlags.SPIN) && !lassoOut) {
            submitWhirl(poseStack, collector, state);
        } else if (!lassoOut) {
            submitCoil(poseStack, collector);
        }
    }

    private static boolean lassoOut(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Entity entity = mc.level.getEntity(id);
        return entity instanceof Player player && LassoEntity.find(player) != null;
    }

    private void submitBracelets(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, boolean guarding) {
        for (int side = -1; side <= 1; side += 2) {
            boolean right = side < 0;
            poseStack.pushPose();
            getParentModel().root().translateAndRotate(poseStack);
            (right ? getParentModel().rightArm : getParentModel().leftArm).translateAndRotate(poseStack);
            // The arm spans x -3..1 (right) or -1..3 (left), y -2..10, z -2..2 pixels.
            float x0 = right ? -3.45F : -1.45F;
            float x1 = right ? 1.45F : 3.45F;
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
                Geo.box(vc, pose, x0 * P, 6.0F * P, -2.45F * P, x1 * P, 9.6F * P, 2.45F * P, SILVER, SILVER, light);
                Geo.box(vc, pose, (x0 - 0.05F) * P, 6.0F * P, -2.5F * P, (x1 + 0.05F) * P, 6.5F * P, 2.5F * P, SILVER_EDGE, light);
                Geo.box(vc, pose, (x0 - 0.05F) * P, 9.1F * P, -2.5F * P, (x1 + 0.05F) * P, 9.6F * P, 2.5F * P, SILVER_EDGE, light);
                // The gold star on the outer face.
                float cx = right ? x0 - 0.06F : x1 + 0.06F;
                Geo.box(vc, pose, (cx - 0.05F) * P, 7.3F * P, -0.6F * P, (cx + 0.05F) * P, 8.3F * P, 0.6F * P, GOLD_STAR, light);
            });
            if (guarding) {
                float pulse = 0.6F + 0.4F * Mth.sin(state.ageInTicks * 0.8F);
                int glow = (int) (150 * pulse) << 24 | 0xFFD86A;
                collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) ->
                        HardLight.box(vc, pose, (x0 - 0.7F) * P, 5.4F * P, -3.1F * P, (x1 + 0.7F) * P, 10.2F * P, 3.1F * P, glow));
            }
            poseStack.popPose();
        }
    }

    /** The lasso coiled at her right hip. */
    private void submitCoil(PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().body.translateAndRotate(poseStack);
        collector.submitCustomGeometry(poseStack, HardLight.type(Geo.PLAIN), (pose, vc) -> {
            for (int i = 0; i < 3; i++) {
                double r = (2.0 + i * 0.25) * P;
                Vec3 center = new Vec3(-4.55 * P - i * 0.1 * P, 10.3 * P, 0);
                for (int k = 0; k < 10; k++) {
                    double a0 = k * Math.PI * 2 / 10;
                    double a1 = (k + 1) * Math.PI * 2 / 10;
                    Vec3 p0 = center.add(0, Math.sin(a0) * r, Math.cos(a0) * r);
                    Vec3 p1 = center.add(0, Math.sin(a1) * r, Math.cos(a1) * r);
                    Rope.segment(vc, pose, p0, p1, 0.35F * P, Rope.GOLD, 0);
                }
            }
        });
        poseStack.popPose();
    }

    /** The rope whirling in a wide circle over her head. */
    private void submitWhirl(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state) {
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        float angle = state.ageInTicks * 0.9F;
        collector.submitCustomGeometry(poseStack, HardLight.type(Geo.PLAIN), (pose, vc) -> {
            // Model space: y grows downwards; the top of the head is at y = -8 pixels.
            Vec3 center = new Vec3(0, -26 * P, 0);
            Rope.loop(vc, pose, center, 2.6, 28, angle, 0.035F, Rope.GOLD, Rope.GLOW);
            // The rope back down to her raised hand.
            Vec3 loopPoint = center.add(Math.cos(angle) * 2.6, 0, Math.sin(angle) * 2.6);
            Rope.segment(vc, pose, new Vec3(-5 * P, -20 * P, 0), loopPoint, 0.03F, Rope.GOLD, Rope.GLOW);
        });
        poseStack.popPose();
    }
}
