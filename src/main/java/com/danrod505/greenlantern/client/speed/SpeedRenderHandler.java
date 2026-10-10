package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.flash.SpeedFlags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.joml.Quaternionf;

/**
 * Speedster poses: leaning into the run with legs and arms pumping far faster than a sprint,
 * banking around the tornado, perpendicular to the wall while running up it and buzzing in place
 * while the molecules vibrate.
 */
public final class SpeedRenderHandler {
    private static final ThreadLocal<Boolean> PUSHED = ThreadLocal.withInitial(() -> false);

    private SpeedRenderHandler() {}

    public static void register() {
        // Lowest priority: only runs if no other mod cancelled the render, so Pre/Post stay balanced.
        RenderAvatarEvent.Pre.BUS.addListener(Priority.LOWEST, SpeedRenderHandler::onPre);
        RenderAvatarEvent.Post.BUS.addListener(SpeedRenderHandler::onPost);
    }

    private static void onPre(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        SpeedVisuals.Visual visual = SpeedVisuals.get(state.id);
        if (visual == null) return;
        float partial = Mth.frac(state.ageInTicks);
        float lean = visual.lean(partial);
        float wall = visual.wall(partial);
        boolean phasing = visual.has(SpeedFlags.PHASING);
        boolean tornado = visual.has(SpeedFlags.TORNADO);
        if (lean < 0.01F && wall < 0.01F && !phasing && !visual.running()) return;

        Vec3 dir = visual.dir;
        float runYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        if (visual.running()) {
            state.bodyRot = Mth.rotLerp(Mth.clamp(lean * 2.0F, 0.0F, 1.0F), state.bodyRot, runYaw);
            // Legs and arms pump far faster than a normal sprint: a blur at top speed.
            float tempo = 1.3F + 1.5F * Mth.clamp(visual.speed / 2.4F, 0.0F, 1.0F);
            state.walkAnimationPos = state.ageInTicks * tempo;
            state.walkAnimationSpeed = Math.max(state.walkAnimationSpeed, 0.95F + 0.3F * lean);
            state.isCrouching = false;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        PUSHED.set(true);
        if (phasing) {
            // Vibrating molecules: the body buzzes in place.
            float t = state.ageInTicks;
            pose.translate(Mth.sin(t * 91.7F) * 0.05F, Mth.sin(t * 77.3F) * 0.03F, Mth.cos(t * 83.1F) * 0.05F);
        }
        if (wall > 0.01F) {
            // Running up a wall: feet on the wall, body sticking out of it, chest facing up.
            Vec3 into = visual.wallNormal.scale(-1);
            float wallYaw = (float) (Mth.atan2(-into.x, into.z) * Mth.RAD_TO_DEG);
            state.bodyRot = Mth.rotLerp(wall, state.bodyRot, wallYaw);
            state.yRot *= 1.0F - wall;
            pose.translate(into.x * 0.3F * wall, 0.2F * wall, into.z * 0.3F * wall);
            pose.mulPose(new Quaternionf().rotateAxis(-85.0F * wall * Mth.DEG_TO_RAD, (float) into.z, 0.0F, (float) -into.x));
        }
        if (lean > 0.01F) {
            // Lean into the run (pivot at the feet).
            pose.mulPose(new Quaternionf().rotateAxis(lean * 26.0F * Mth.DEG_TO_RAD, (float) dir.z, 0.0F, (float) -dir.x));
            if (tornado) {
                // Banking towards the center of the tornado (always on the runner's right).
                float center = state.boundingBoxHeight * 0.5F;
                pose.translate(0.0F, center, 0.0F);
                pose.mulPose(new Quaternionf().rotateAxis(22.0F * Mth.DEG_TO_RAD, (float) dir.x, 0.0F, (float) dir.z));
                pose.translate(0.0F, -center, 0.0F);
            }
        }
    }

    private static void onPost(RenderAvatarEvent.Post event) {
        if (PUSHED.get()) {
            PUSHED.set(false);
            event.getPoseStack().popPose();
        }
    }
}
