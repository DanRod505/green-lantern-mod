package com.danrod505.greenlantern.client.flight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.joml.Quaternionf;

/**
 * Superhero flight pose: at speed the Lantern lies along the flight direction with one fist
 * forward, rolls during barrel rolls and kneels after a hero landing.
 */
public final class FlightRenderHandler {
    private static final ThreadLocal<Boolean> PUSHED = ThreadLocal.withInitial(() -> false);

    private FlightRenderHandler() {}

    public static void register() {
        // Lowest priority: only runs if no other mod cancelled the render, so Pre/Post stay balanced.
        RenderAvatarEvent.Pre.BUS.addListener(Priority.LOWEST, FlightRenderHandler::onPre);
        RenderAvatarEvent.Post.BUS.addListener(FlightRenderHandler::onPost);
    }

    private static void onPre(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        FlightVisuals.Visual visual = FlightVisuals.get(state.id);
        if (visual == null) return;
        float partial = Mth.frac(state.ageInTicks);

        if (visual.heroLanding > 0) {
            state.isCrouching = true;
        }
        float h = visual.heroPose(partial);
        float roll = visual.rollAngle(partial);
        if (h < 0.01F && roll == 0.0F) return;

        Vec3 dir = visual.dir;
        float flightYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float pitchDown = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        float lean = h * Mth.clamp(90.0F + pitchDown, 0.0F, 180.0F);

        // Body faces the flight direction, head looks ahead.
        state.bodyRot = Mth.rotLerp(Mth.clamp(h * 1.6F, 0.0F, 1.0F), state.bodyRot, flightYaw);
        state.yRot *= 1.0F - h;
        state.xRot = Mth.lerp(h, state.xRot, -65.0F);
        state.walkAnimationSpeed *= 1.0F - h;
        state.isCrouching = false;
        if (h > 0.45F) {
            boolean rightMain = state.mainArm == HumanoidArm.RIGHT;
            state.rightArmPose = rightMain ? FlightArmPoses.fist() : FlightArmPoses.trail();
            state.leftArmPose = rightMain ? FlightArmPoses.trail() : FlightArmPoses.fist();
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        PUSHED.set(true);
        float center = state.boundingBoxHeight * 0.5F;
        pose.translate(0.0F, center, 0.0F);
        if (roll != 0.0F) {
            pose.mulPose(new Quaternionf().rotateAxis(roll * Mth.DEG_TO_RAD, (float) dir.x, (float) dir.y, (float) dir.z));
        }
        double fx = -Math.sin(flightYaw * Mth.DEG_TO_RAD);
        double fz = Math.cos(flightYaw * Mth.DEG_TO_RAD);
        pose.mulPose(new Quaternionf().rotateAxis(lean * Mth.DEG_TO_RAD, (float) fz, 0.0F, (float) -fx));
        pose.translate(0.0F, -center, 0.0F);
    }

    private static void onPost(RenderAvatarEvent.Post event) {
        if (PUSHED.get()) {
            PUSHED.set(false);
            event.getPoseStack().popPose();
        }
    }

    /** Custom arm poses (Forge extensible enum). Created lazily on the client. */
    public static final class FlightArmPoses {
        private static HumanoidModel.ArmPose fist;
        private static HumanoidModel.ArmPose trail;

        private FlightArmPoses() {}

        /** Arm stretched over the head: forward while lying horizontally. */
        public static HumanoidModel.ArmPose fist() {
            if (fist == null) {
                fist = HumanoidModel.ArmPose.create("GREENLANTERN_FLIGHT_FIST", false, false, (model, state, arm) -> {
                    var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    part.xRot = -2.95F;
                    part.yRot = 0.0F;
                    part.zRot = arm == HumanoidArm.RIGHT ? 0.12F : -0.12F;
                });
            }
            return fist;
        }

        /** Arm along the body, slightly back, like streaming in the wind. */
        public static HumanoidModel.ArmPose trail() {
            if (trail == null) {
                trail = HumanoidModel.ArmPose.create("GREENLANTERN_FLIGHT_TRAIL", false, false, (model, state, arm) -> {
                    var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    part.xRot = 0.3F;
                    part.yRot = 0.0F;
                    part.zRot = arm == HumanoidArm.RIGHT ? 0.15F : -0.15F;
                });
            }
            return trail;
        }
    }
}
