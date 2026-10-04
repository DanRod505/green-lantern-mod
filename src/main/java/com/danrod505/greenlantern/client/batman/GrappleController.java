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
    private static double lastDist = Double.MAX_VALUE;
    private static Vec3 lastTo = Vec3.ZERO;
    /** Ticks of the final hop left, and its direction (towards the ledge). */
    private static int hopTicks;
    private static Vec3 hopDir = Vec3.ZERO;

    private GrappleController() {}

    public static boolean isPulling() {
        return pulling;
    }

    public static void preTick(LocalPlayer player) {
        boolean jump = player.input.keyPresses.jump();
        boolean jumpPressed = jump && !jumpWasDown;
        jumpWasDown = jump;
        if (hopTicks > 0) {
            // Keep pushing towards the ledge while rising: scraping up the wall would stop the hop dead.
            hopTicks--;
            if (player.onGround()) {
                hopTicks = 0;
            } else {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(hopDir.x * 0.3, v.y, hopDir.z * 0.3);
            }
        }
        GrappleHookEntity hook = BatmanHelper.isSuited(player) && !player.isPassenger() ? GrappleHookEntity.find(player) : null;
        if (hook == null || !hook.isAttached() || hook.getId() == ignoredHookId) {
            if (pulling && hook == null && lastDist < 3.5) {
                // The server let go of the cable just before the top: still fling Batman over the ledge.
                hop(player, lastTo);
            }
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
        lastDist = dist;
        lastTo = to;
        if (dist < GrappleHookEntity.ARRIVED) {
            hop(player, to);
            ModNetwork.sendToServer(new UsePowerPacket(BatPower.GRAPPLE.ordinal()));
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

    /** Over the top: a hop up and forward, onto the ledge. */
    private static void hop(LocalPlayer player, Vec3 to) {
        Vec3 flat = new Vec3(to.x, 0, to.z);
        Vec3 forward = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.directionFromRotation(0, player.getYRot());
        player.setDeltaMovement(forward.scale(0.3).add(0, 0.8, 0));
        hopDir = forward;
        hopTicks = 14;
        player.resetFallDistance();
    }

    public static void postTick(LocalPlayer player) {
        bumped = pulling && player.horizontalCollision;
    }

    private static void letGo() {
        ignoredHookId = hookId;
        lastDist = Double.MAX_VALUE;
        pulling = false;
        hookId = -1;
        bumped = false;
    }
}
