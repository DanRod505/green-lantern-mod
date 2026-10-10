package com.danrod505.greenlantern.oa;

import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Moves players between their world and Oa, remembering where each traveller came from. */
public final class OaTravel {
    private static final String RETURN_TAG = "greenlantern.oa_return";

    private OaTravel() {}

    /** Sends the player to Oa (from anywhere else) or back home (from Oa). */
    public static void travel(ServerPlayer player) {
        if (Oa.is(player.level())) {
            returnHome(player);
        } else {
            sendToOa(player);
        }
    }

    public static boolean sendToOa(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel oa = Oa.level(server);
        if (oa == null) {
            player.displayClientMessage(Component.translatable("message.greenlantern.oa_missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        CompoundTag back = new CompoundTag();
        back.putString("Dimension", player.level().dimension().identifier().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ());
        back.putFloat("Yaw", player.getYRot());
        player.getPersistentData().put(RETURN_TAG, back);

        OaCity.ensureBuilt(oa);
        departureEffects(player);
        player.stopRiding();
        player.teleport(new TeleportTransition(oa, Oa.ARRIVAL, Vec3.ZERO, Oa.ARRIVAL_YAW, 0.0F, TeleportTransition.DO_NOTHING));
        arrivalEffects(player);
        player.displayClientMessage(Component.translatable("message.greenlantern.oa_welcome").withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    public static boolean returnHome(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        CompoundTag back = player.getPersistentData().getCompoundOrEmpty(RETURN_TAG);
        ServerLevel target = null;
        Vec3 pos = null;
        float yaw = player.getYRot();
        String dimension = back.getStringOr("Dimension", "");
        if (!dimension.isEmpty()) {
            Identifier id = Identifier.tryParse(dimension);
            ResourceKey<Level> key = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            target = key == null || key == Oa.LEVEL ? null : server.getLevel(key);
            pos = new Vec3(back.getDoubleOr("X", 0), back.getDoubleOr("Y", 64), back.getDoubleOr("Z", 0));
            yaw = back.getFloatOr("Yaw", yaw);
        }
        if (target == null) {
            // Never came through a portal (or that world is gone): the world spawn.
            var respawn = server.getRespawnData();
            target = server.getLevel(respawn.dimension());
            if (target == null) target = server.overworld();
            BlockPos top = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, respawn.pos());
            pos = Vec3.atBottomCenterOf(top);
        }
        departureEffects(player);
        player.stopRiding();
        player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        arrivalEffects(player);
        player.getPersistentData().remove(RETURN_TAG);
        return true;
    }

    private static void departureEffects(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void arrivalEffects(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(ModParticles.SHOCKWAVE.get(), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
    }
}
