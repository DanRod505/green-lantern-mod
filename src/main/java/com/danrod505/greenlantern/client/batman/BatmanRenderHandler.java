package com.danrod505.greenlantern.client.batman;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.joml.Quaternionf;

/** Gliding pose: Batman leans into the wind, arms spread to hold the cape open like a wing. */
public final class BatmanRenderHandler {
    private static final ThreadLocal<Boolean> PUSHED = ThreadLocal.withInitial(() -> false);
    private static HumanoidModel.ArmPose wing;

    private BatmanRenderHandler() {}

    public static void register() {
        RenderAvatarEvent.Pre.BUS.addListener(Priority.LOWEST, BatmanRenderHandler::onPre);
        RenderAvatarEvent.Post.BUS.addListener(BatmanRenderHandler::onPost);
    }

    private static void onPre(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        float s = BatmanVisuals.spread(state.id, Mth.frac(state.ageInTicks));
        if (s < 0.01F) return;
        float lean = s * 70.0F;
        state.xRot = Mth.lerp(s, state.xRot, -55.0F);
        state.walkAnimationSpeed *= 1.0F - s;
        state.isCrouching = false;
        if (s > 0.35F) {
            state.rightArmPose = wing();
            state.leftArmPose = wing();
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        PUSHED.set(true);
        float center = state.boundingBoxHeight * 0.5F;
        float yaw = state.bodyRot;
        double fx = -Math.sin(yaw * Mth.DEG_TO_RAD);
        double fz = Math.cos(yaw * Mth.DEG_TO_RAD);
        pose.translate(0.0F, center, 0.0F);
        pose.mulPose(new Quaternionf().rotateAxis(lean * Mth.DEG_TO_RAD, (float) fz, 0.0F, (float) -fx));
        pose.translate(0.0F, -center, 0.0F);
    }

    private static void onPost(RenderAvatarEvent.Post event) {
        if (PUSHED.get()) {
            PUSHED.set(false);
            event.getPoseStack().popPose();
        }
    }

    /** Arms out to the sides and a little back, hands holding the cape's edges. */
    private static HumanoidModel.ArmPose wing() {
        if (wing == null) {
            wing = HumanoidModel.ArmPose.create("GREENLANTERN_CAPE_WING", false, false, (model, state, arm) -> {
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                part.xRot = 0.25F;
                part.yRot = 0.0F;
                part.zRot = arm == HumanoidArm.RIGHT ? 1.35F : -1.35F;
            });
        }
        return wing;
    }
}
