package com.danrod505.greenlantern.atlantis;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.Respirator;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Moves players between wherever they are and Atlantis, remembering where each traveller came
 * from so the way back leads to the exact same spot (like the trips to Oa, with its own memory).
 */
public final class AtlantisTravel {
    private static final String RETURN_TAG = "greenlantern.atlantis_return";

    private AtlantisTravel() {}

    /** Whether a portal opened at this spot leads back home (it stands in Atlantis). */
    public static boolean leadsHome(ServerLevel level, Vec3 pos) {
        return Atlantis.containing(level, pos) != null;
    }

    /** Whether Atlantis exists in this world; tells the player when it doesn't. */
    public static boolean available(ServerPlayer player) {
        if (Atlantis.enabled() && Atlantis.site(player.level().getServer()) != null) return true;
        player.displayClientMessage(Component.translatable("message.greenlantern.atlantis_missing").withStyle(ChatFormatting.RED), true);
        return false;
    }

    public static boolean sendToAtlantis(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (!available(player) || !AtlantisBuilder.ensureBuilt(server)) return false;
        Atlantis.Site site = Atlantis.site(server);
        CompoundTag back = new CompoundTag();
        back.putString("Dimension", player.level().dimension().identifier().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ());
        back.putFloat("Yaw", player.getYRot());
        player.getPersistentData().put(RETURN_TAG, back);

        departureEffects(player);
        player.stopRiding();
        player.teleport(new TeleportTransition(server.overworld(), site.arrival(), Vec3.ZERO, site.arrivalYaw(), 0.0F, TeleportTransition.DO_NOTHING));
        arrivalEffects(player);
        player.displayClientMessage(Component.translatable("message.greenlantern.atlantis_welcome").withStyle(ChatFormatting.AQUA), false);
        if (!AquamanHelper.isSuited(player) && !Respirator.canBreatheUnderwater(player)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.atlantis_need_air").withStyle(ChatFormatting.YELLOW), false);
        }
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
            target = key == null ? null : server.getLevel(key);
            pos = new Vec3(back.getDoubleOr("X", 0), back.getDoubleOr("Y", 64), back.getDoubleOr("Z", 0));
            yaw = back.getFloatOr("Yaw", yaw);
            // Coming from the city itself (e.g. a portal opened in Atlantis twice): go to spawn instead.
            if (target != null && leadsHome(target, pos)) target = null;
        }
        if (target == null) {
            // Never came through a portal (or that world is gone): the world spawn.
            var respawn = server.getRespawnData();
            target = server.getLevel(respawn.dimension());
            if (target == null) target = server.overworld();
            // Load the spawn chunk first: the heightmap of an unloaded chunk is the bottom of the world.
            target.getChunkAt(respawn.pos());
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

    /** Through an Atlantean portal: home from the city, to the city from anywhere else. */
    public static void travel(ServerPlayer player, boolean home) {
        if (home) {
            returnHome(player);
        } else {
            sendToAtlantis(player);
        }
    }

    private static void departureEffects(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void arrivalEffects(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 1.0, 0.6, 0.1);
        level.sendParticles(ParticleTypes.DOLPHIN, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
