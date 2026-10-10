package com.danrod505.greenlantern.client.wonderwoman;

import com.danrod505.greenlantern.wonderwoman.AmazonFlags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * Wonder Woman's poses: the bracelets crossed in front of her face while she guards, and the right
 * arm raised over her head, whirling, while the lasso spins.
 */
public final class WonderWomanRenderHandler {
    private static HumanoidModel.ArmPose guard;
    private static HumanoidModel.ArmPose whirl;

    private WonderWomanRenderHandler() {}

    public static void register() {
        RenderAvatarEvent.Pre.BUS.addListener(Priority.LOW, WonderWomanRenderHandler::onPre);
    }

    private static void onPre(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        int flags = WonderWomanVisuals.flags(state.id);
        if (flags == 0) return;
        if (AmazonFlags.has(flags, AmazonFlags.GUARD)) {
            state.rightArmPose = guard();
            state.leftArmPose = guard();
        } else if (AmazonFlags.has(flags, AmazonFlags.SPIN)) {
            state.rightArmPose = whirl();
        }
    }

    /** Forearms raised and crossed in an X in front of the face. */
    private static HumanoidModel.ArmPose guard() {
        if (guard == null) {
            guard = HumanoidModel.ArmPose.create("GREENLANTERN_BRACELET_GUARD", false, true, (model, state, arm) -> {
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                part.xRot = -1.75F;
                part.yRot = arm == HumanoidArm.RIGHT ? -0.62F : 0.62F;
                part.zRot = 0.0F;
            });
        }
        return guard;
    }

    /** The lasso hand up over the head, circling. */
    private static HumanoidModel.ArmPose whirl() {
        if (whirl == null) {
            whirl = HumanoidModel.ArmPose.create("GREENLANTERN_LASSO_WHIRL", false, false, (model, state, arm) -> {
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                float t = state.ageInTicks * 0.9F;
                part.xRot = -2.8F + Mth.sin(t) * 0.15F;
                part.yRot = 0.0F;
                part.zRot = (arm == HumanoidArm.RIGHT ? 1 : -1) * (0.25F + Mth.cos(t) * 0.15F);
            });
        }
        return whirl;
    }
}
