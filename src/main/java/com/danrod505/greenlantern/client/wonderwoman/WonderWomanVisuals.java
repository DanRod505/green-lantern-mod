package com.danrod505.greenlantern.client.wonderwoman;

import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.network.WonderWomanSyncPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.wonderwoman.AmazonFlags;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Client side of Wonder Woman's powers: which Wonder Woman around is doing what (synced by the
 * server) for the poses, the glowing bracelets and the whirling lasso, and the one-shot events
 * (the shockwave shakes the camera of everyone close).
 */
public final class WonderWomanVisuals {
    private record Entry(int flags, long time) {}

    private static final Map<Integer, Entry> STATES = new HashMap<>();

    private WonderWomanVisuals() {}

    public static int flags(int entityId) {
        Entry entry = STATES.get(entityId);
        return entry == null ? 0 : entry.flags();
    }

    public static boolean has(int entityId, int flag) {
        return AmazonFlags.has(flags(entityId), flag);
    }

    public static void onSync(WonderWomanSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (packet.flags() == 0) {
            STATES.remove(packet.entityId());
        } else {
            STATES.put(packet.entityId(), new Entry(packet.flags(), mc.level.getGameTime()));
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity == null || mc.player == null) return;
        double dist = mc.player.distanceTo(entity);
        switch (packet.event()) {
            case AmazonFlags.EVENT_SHOCKWAVE -> {
                if (dist < 28) CameraShake.start((float) Math.min(1.0, 1.0 * (1 - dist / 28)), 18);
            }
            case AmazonFlags.EVENT_DEFLECT -> {
                if (entity == mc.player) CameraShake.start(0.12F, 4);
            }
            case AmazonFlags.EVENT_SUIT_UP -> {
                // A column of golden light, like a bolt from Olympus.
                for (int i = 0; i < 30; i++) {
                    double y = entity.getY() + mc.level.random.nextDouble() * 6.0;
                    mc.level.addParticle(ModParticles.AMAZON_SPARK.get(), entity.getX() + mc.level.random.nextGaussian() * 0.15, y,
                            entity.getZ() + mc.level.random.nextGaussian() * 0.15, 0, -0.05, 0);
                }
            }
            default -> {}
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STATES.clear();
            return;
        }
        long now = mc.level.getGameTime();
        // A Wonder Woman out of view stops sending: forget her after a while.
        Iterator<Map.Entry<Integer, Entry>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().time() > 40) it.remove();
        }
    }
}
