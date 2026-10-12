package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.engine.OwnPowers;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.item.PowerRingItem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The constructs of the ring as powers of before of the Lantern (stage 3, lot 1): the functions of
 * his abilities make a construct exactly as right click with it selected did (the same energy, the
 * same recharge). A construct that fired while the use key was held (the gatling gun) now fires on
 * its own for {@value #AUTO_FIRE_TICKS} ticks; pressing it again stops it.
 */
public final class RingConstructs {
    /** How long a construct that fired while held fires on its own. */
    public static final int AUTO_FIRE_TICKS = 60;

    private static final Map<UUID, PresetRuntime.Task> FIRING = new ConcurrentHashMap<>();

    private RingConstructs() {}

    /** The powers of before of the Lantern: every construct, by its id ({@code hammer}, {@code minigun}...). */
    public static OwnPowers powers() {
        OwnPowers own = new OwnPowers();
        for (Construct construct : ConstructRegistry.all()) {
            own.add(construct.id().getPath(), construct::activationCost, (player, ring) -> use(player, ring, construct));
        }
        return own;
    }

    /** Whether a construct of the player is firing on its own now (server side). */
    public static boolean autoFiring(net.minecraft.world.entity.player.Player player) {
        return FIRING.containsKey(player.getUUID());
    }

    /** Makes a construct with the ring, as right click with it selected did. */
    public static boolean use(ServerPlayer player, ItemStack ring, Construct construct) {
        if (!RingHelper.isSuited(player)) return false;
        PresetRuntime.Task firing = FIRING.remove(player.getUUID());
        if (firing != null) {
            PresetRuntime.cancel(player, firing);
            if (construct.useMode() == Construct.UseMode.HOLD) return true;
        }
        if (player.getCooldowns().isOnCooldown(ring)) return false;
        boolean toggle = construct.useMode() == Construct.UseMode.TOGGLE;
        // Toggle constructs can always be dismissed; only new constructs need energy.
        if (!toggle && !player.isCreative() && !RingEnergy.has(ring, Math.max(1, construct.activationCost()))) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        if (construct.useMode() == Construct.UseMode.HOLD) return autoFire(player, ring, construct);
        boolean made = construct.activate(player, ring);
        if (made && construct.cooldown() > 0) player.getCooldowns().addCooldown(ring, construct.cooldown());
        return made;
    }

    /** A construct that worked while the key was held: it fires on its own for a while. */
    private static boolean autoFire(ServerPlayer player, ItemStack ring, Construct construct) {
        int[] held = {0};
        PresetRuntime.Task task = PresetRuntime.Task.ending(age -> {
            held[0] = age;
            ItemStack now = LanternHero.INSTANCE.findItem(player);
            if (now.isEmpty() || !RingHelper.isSuited(player)) return false;
            construct.holdTick(player, now, age);
            return age < AUTO_FIRE_TICKS && (player.isCreative() || RingEnergy.get(now).stored() > 0);
        }, () -> {
            FIRING.remove(player.getUUID());
            ItemStack now = LanternHero.INSTANCE.findItem(player);
            if (now.isEmpty()) return;
            construct.release(player, now, held[0]);
            if (construct.cooldown() > 0) player.getCooldowns().addCooldown(now, construct.cooldown());
        });
        FIRING.put(player.getUUID(), task);
        if (!construct.activate(player, ring)) {
            FIRING.remove(player.getUUID());
            return false;
        }
        PresetRuntime.start(player, task);
        return true;
    }
}
