package com.danrod505.greenlantern.client.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.entity.GrappleHookEntity;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.UsePowerPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The grappling hook's pull (local player). Once the hook bites into a block, the cable reels Batman
 * in, faster and faster; at the top he is flung up and over the ledge. Jump lets go of the cable
 * mid-way (keeping the momentum), sneak too (handled by the server).
 */
public final class GrappleController {
    private static boolean pulling;
    private static int hookId = -1;
    private static int ignoredHookId = -1;
    private static double pullSpeed;
    private static int pullTicks;
    private static boolean jumpWasDown;
    private static boolean bumped;

    private GrappleController() {}

    public static boolean isPulling() {
        return pulling;
    }

    public static void preTick(LocalPlayer player) {
        boolean jump = player.input.keyPresses.jump();
        boolean jumpPressed = jump && !jumpWasDown;
        jumpWasDown = jump;
        GrappleHookEntity hook = BatmanHelper.isSuited(player) && !player.isPassenger() ? GrappleHookEntity.find(player) : null;
        if (hook == null || !hook.isAttached() || hook.getId() == ignoredHookId) {
            pulling = false;
            hookId = -1;
            return;
        }
        if (!pulling || hookId != hook.getId()) {
            pulling = true;
            hookId = hook.getId();
            pullSpeed = Math.max(0.4, player.getDeltaMovement().length());
            pullTicks = 0;
        }
        pullTicks++;

        Vec3 target = hook.position().add(0, -1.0, 0);
        Vec3 to = target.subtract(player.position());
        double dist = to.length();
        if (dist < GrappleHookEntity.ARRIVED) {
            // Over the top: a little hop up and forward, onto the ledge.
            Vec3 flat = new Vec3(to.x, 0, to.z);
            Vec3 forward = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.directionFromRotation(0, player.getYRot());
            player.setDeltaMovement(forward.scale(0.3).add(0, 0.75, 0));
            letGo();
            return;
        }
        if (jumpPressed && pullTicks > 5) {
            ModNetwork.sendToServer(new UsePowerPacket(BatPower.GRAPPLE.ordinal()));
            letGo();
            return;
        }
        double top = GLConfig.GRAPPLE_PULL_SPEED.get();
        pullSpeed = Math.min(top, pullSpeed + 0.12);
        Vec3 velocity = to.scale(Math.min(pullSpeed, dist) / dist);
        if (bumped && target.y > player.getY()) {
            // Dragged against a wall below the anchor: climb it instead of sticking to it.
            velocity = velocity.add(0, 0.35, 0);
        }
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
    }

    public static void postTick(LocalPlayer player) {
        bumped = pulling && player.horizontalCollision;
    }

    private static void letGo() {
        ignoredHookId = hookId;
        pulling = false;
        hookId = -1;
        bumped = false;
    }
}
