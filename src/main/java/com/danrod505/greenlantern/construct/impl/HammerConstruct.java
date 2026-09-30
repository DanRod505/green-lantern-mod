package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Construct 5: a giant hammer that slams the ground in front of the player (area attack). */
public class HammerConstruct extends Construct {
    private static final double REACH = 18.0;
    private static final double DEFAULT_DISTANCE = 5.0;

    public HammerConstruct() {
        super(GreenLantern.id("hammer"), 4);
    }

    @Override
    public UseMode useMode() {
        return UseMode.INSTANT;
    }

    @Override
    public int activationCost() {
        return GLConfig.HAMMER_COST.get();
    }

    @Override
    public int cooldown() {
        return 40;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        ServerLevel level = player.level();
        Vec3 target = findTarget(player, level);
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) return false;

        HammerConstructEntity hammer = new HammerConstructEntity(ModEntities.HAMMER_CONSTRUCT.get(), level);
        hammer.setOwner(player);
        hammer.snapTo(target.x, target.y, target.z, player.getYRot(), 0.0F);
        level.addFreshEntity(hammer);
        level.playSound(null, target.x, target.y, target.z, ModSounds.HAMMER_SUMMON.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    /** Point on the ground the hammer will strike. */
    private static Vec3 findTarget(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 point;
        if (hit.getType() == HitResult.Type.BLOCK) {
            point = hit.getLocation();
        } else {
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
            point = player.position().add(flat.scale(DEFAULT_DISTANCE)).add(0, 1.0, 0);
        }
        // Drop the point onto the ground (up to 8 blocks below).
        BlockHitResult ground = level.clip(new ClipContext(point.add(0, 0.5, 0), point.subtract(0, 8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, (Entity) player));
        return ground.getType() == HitResult.Type.BLOCK ? ground.getLocation() : point.subtract(0, 1.0, 0);
    }
}
