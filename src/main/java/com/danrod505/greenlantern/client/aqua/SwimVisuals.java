package com.danrod505.greenlantern.client.aqua;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side visual state of every Aquaman swimming in view (the local player included): the
 * smoothed speed and the points of the water trail (a sea-green wake wrapped in spiralling
 * streams of bubbles, drawn by {@code TrailRenderer}).
 */
public final class SwimVisuals {
    public static final int TRAIL_POINTS = 48;
    /** Speed (blocks/tick) above which the trail is drawn. */
    public static final float TRAIL_SPEED = 0.55F;

    /** A point of the trail (middle of the body) and the swimming direction there. */
    public record TrailPoint(double x, double y, double z, long time) {}

    public static final class Visual {
        public final int entityId;
        public float speed;
        public final ArrayDeque<TrailPoint> trail = new ArrayDeque<>();
        Vec3 lastPos;
        boolean active;

        Visual(int entityId) {
            this.entityId = entityId;
        }

        public boolean swimming() {
            return speed > TRAIL_SPEED;
        }

        public int trailLife() {
            return speed > 1.6F ? 26 : 18;
        }
    }

    private static final Map<Integer, Visual> VISUALS = new HashMap<>();
    private static long gameTime;

    private SwimVisuals() {}

    public static Visual get(int entityId) {
        return VISUALS.get(entityId);
    }

    public static Iterable<Visual> all() {
        return VISUALS.values();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            VISUALS.clear();
            return;
        }
        if (mc.isPaused()) return;
        gameTime = level.getGameTime();
        for (Visual visual : VISUALS.values()) visual.active = false;
        for (Player player : level.players()) {
            if (!AquamanHelper.isSuited(player) && !VISUALS.containsKey(player.getId())) continue;
            Visual visual = VISUALS.computeIfAbsent(player.getId(), Visual::new);
            visual.active = true;
            update(level, player, visual, player == mc.player);
        }
        Iterator<Visual> it = VISUALS.values().iterator();
        while (it.hasNext()) {
            Visual visual = it.next();
            if (!visual.active) {
                prune(visual, 0);
                if (visual.trail.isEmpty()) it.remove();
            }
        }
    }

    private static void update(ClientLevel level, Player player, Visual visual, boolean isLocal) {
        Vec3 pos = player.position();
        Vec3 delta = visual.lastPos == null ? Vec3.ZERO : pos.subtract(visual.lastPos);
        visual.lastPos = pos;
        boolean inWater = player.isInWater() && AquamanHelper.isSuited(player) && !player.isPassenger();
        float target = inWater ? (float) (isLocal ? SwimController.speed() : Math.min(delta.length(), 6.0)) : 0.0F;
        visual.speed += (target - visual.speed) * 0.4F;

        if (inWater && visual.swimming()) {
            Vec3 c = pos.add(0, player.getBbHeight() * (player.isVisuallySwimming() ? 0.3 : 0.5), 0);
            visual.trail.addFirst(new TrailPoint(c.x, c.y, c.z, gameTime));
        }
        prune(visual, visual.trailLife());

        RandomSource random = player.getRandom();
        if (!isLocal && inWater && visual.speed > 0.5F && random.nextFloat() < 0.6F) {
            level.addParticle(ParticleTypes.BUBBLE, player.getX() + random.nextGaussian() * 0.3, player.getY() + 0.4 + random.nextGaussian() * 0.3,
                    player.getZ() + random.nextGaussian() * 0.3, 0, 0.05, 0);
        }
    }

    private static void prune(Visual visual, int life) {
        while (visual.trail.size() > TRAIL_POINTS) visual.trail.removeLast();
        while (!visual.trail.isEmpty() && gameTime - visual.trail.peekLast().time() > life) visual.trail.removeLast();
    }
}
