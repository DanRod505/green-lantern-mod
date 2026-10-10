package com.danrod505.greenlantern.flash;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * Molecular vibration on both logical sides. The server decides who is phasing (see
 * {@link SpeedsterServer}) and syncs it to the clients; every tick, phasing players ignore block
 * collisions, can't suffocate and keep standing (instead of crawling inside walls).
 */
public final class PhaseState {
    /** Entity ids of the players phasing, as known by this client. */
    private static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();

    private PhaseState() {}

    public static void setClient(int entityId, boolean phasing) {
        if (phasing) CLIENT.add(entityId);
        else CLIENT.remove(entityId);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static boolean isPhasing(Player player) {
        if (player.level().isClientSide()) return CLIENT.contains(player.getId());
        return player instanceof ServerPlayer serverPlayer && SpeedsterServer.isPhasing(serverPlayer);
    }

    /** Called at the start of every living tick, after vanilla reset {@code noPhysics}. */
    public static void apply(Player player) {
        if (!isPhasing(player)) return;
        player.noPhysics = true;
        player.resetFallDistance();
        player.setForcedPose(Pose.STANDING);
    }
}
