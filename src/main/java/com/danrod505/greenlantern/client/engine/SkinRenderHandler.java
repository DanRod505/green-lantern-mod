package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.engine.Skeleton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import org.joml.Quaternionf;

/**
 * The suit becomes the skin (catalog, stage 7): with the full suit of a character of the engine on,
 * the player is drawn with the character's skin, on the model of its skeleton (thin arms for the
 * Female and Young ones), at the skeleton's size (only the look: the collision box stays the
 * player's), and in first person the arm is the character's too.
 */
public final class SkinRenderHandler {
    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> PUSHED = ThreadLocal.withInitial(() -> false);

    private SkinRenderHandler() {}

    public static void register() {
        // High: swaps the skin before the flight and glide poses look at the state.
        RenderAvatarEvent.Pre.BUS.addListener(Priority.HIGH, SkinRenderHandler::onPreSkin);
        // Lowest, like the flight pose: only runs if nobody cancelled the render, so Pre and Post stay balanced.
        RenderAvatarEvent.Pre.BUS.addListener(Priority.LOWEST, SkinRenderHandler::onPreSize);
        RenderAvatarEvent.Post.BUS.addListener(SkinRenderHandler::onPost);
        RenderArmEvent.BUS.addListener(SkinRenderHandler::onArm);
    }

    /** Returns true (cancels) when the player must be drawn by the other model's renderer. */
    private static boolean onPreSkin(RenderAvatarEvent.Pre event) {
        if (REDIRECTING.get()) return false;
        AvatarRenderState state = event.getState();
        SkinLook look = SkinLook.of(state.id);
        if (look == null) return false;
        PlayerModelType model = look.skeleton().slim() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        state.skin = PlayerSkin.insecure(new ClientAsset.ResourceTexture(look.skin().texture(), look.skin().texture()), null, null, model);
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        AvatarRenderer<AbstractClientPlayer> wanted = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderers().get(model);
        if (wanted == null || wanted == event.getRenderer()) return false;
        REDIRECTING.set(true);
        try {
            wanted.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
        } finally {
            REDIRECTING.set(false);
        }
        return true;
    }

    private static void onPreSize(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        SkinLook look = SkinLook.of(state.id);
        if (look == null) return;
        Skeleton skeleton = look.skeleton();
        if (skeleton.height() == 1.0F && skeleton.width() == 1.0F && skeleton.lean() == 0.0F) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        PUSHED.set(true);
        if (skeleton.lean() != 0.0F && !state.isFallFlying && state.swimAmount <= 0.0F) {
            // Creatures walk bent over: the body leans forward from the feet.
            double fx = -Math.sin(state.bodyRot * Mth.DEG_TO_RAD);
            double fz = Math.cos(state.bodyRot * Mth.DEG_TO_RAD);
            pose.mulPose(new Quaternionf().rotateAxis(skeleton.lean() * Mth.DEG_TO_RAD, (float) fz, 0.0F, (float) -fx));
        }
        pose.scale(skeleton.width(), skeleton.height(), skeleton.width());
    }

    private static void onPost(RenderAvatarEvent.Post event) {
        if (PUSHED.get()) {
            PUSHED.set(false);
            event.getPoseStack().popPose();
        }
    }

    /** First person: the arm with the character's skin, on its skeleton's model. Returns true to cancel the player's own arm. */
    private static boolean onArm(RenderArmEvent event) {
        if (REDIRECTING.get()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        SkinLook look = SkinLook.of(mc.player);
        if (look == null) return false;
        PlayerModelType model = look.skeleton().slim() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderers().get(model);
        if (renderer == null) return false;
        REDIRECTING.set(true);
        try {
            if (event.getArm() == HumanoidArm.RIGHT) {
                renderer.renderRightHand(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), look.skin().texture(), true);
            } else {
                renderer.renderLeftHand(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), look.skin().texture(), true);
            }
        } finally {
            REDIRECTING.set(false);
        }
        return true;
    }
}
