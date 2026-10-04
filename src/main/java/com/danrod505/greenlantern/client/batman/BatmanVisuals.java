package com.danrod505.greenlantern.client.batman;

import com.danrod505.greenlantern.batman.BatmanHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side visual state of every Batman in view: how far the cape is spread (0 hanging, 1 a full
 * wing). The local player knows for sure; for the others it is read from how they move (falling
 * slowly while moving fast through the air means a glide).
 */
public final class BatmanVisuals {
    public static final class Visual {
        float spread;
        float spreadO;
        Vec3 lastPos;
        boolean active;

        public float spread(float partial) {
            return Mth.lerp(partial, spreadO, spread);
        }
    }

    private static final Map<Integer, Visual> VISUALS = new HashMap<>();

    private BatmanVisuals() {}

    /** Cape spread of a player (0-1), smoothed. */
    public static float spread(int entityId, float partial) {
        Visual visual = VISUALS.get(entityId);
        return visual == null ? 0.0F : visual.spread(partial);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            VISUALS.clear();
            return;
        }
        if (mc.isPaused()) return;
        for (Visual visual : VISUALS.values()) visual.active = false;
        for (Player player : level.players()) {
            if (!BatmanHelper.isSuited(player)) continue;
            Visual visual = VISUALS.computeIfAbsent(player.getId(), id -> new Visual());
            visual.active = true;
            Vec3 pos = player.position();
            Vec3 delta = visual.lastPos == null ? Vec3.ZERO : pos.subtract(visual.lastPos);
            visual.lastPos = pos;
            boolean gliding;
            if (player == mc.player) {
                gliding = GlideController.isGliding();
            } else {
                gliding = !player.onGround() && !player.isPassenger() && !player.isInWater() && delta.y < -0.01 && delta.y > -0.45
                        && delta.horizontalDistance() > 0.25;
            }
            visual.spreadO = visual.spread;
            float target = gliding ? 1.0F : 0.0F;
            visual.spread += (target - visual.spread) * (gliding ? 0.3F : 0.2F);
        }
        Iterator<Visual> it = VISUALS.values().iterator();
        while (it.hasNext()) {
            if (!it.next().active) it.remove();
        }
    }
}
