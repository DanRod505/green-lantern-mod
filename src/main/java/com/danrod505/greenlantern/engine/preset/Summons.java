package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;

/**
 * The creatures a character calls or takes over (helpers, swarms, copies, turrets, dominated
 * mobs): whose they are, how they fight for their owner, and that they never outlive the preset.
 * They never pick a player (their owner included) or another of the same owner as a target, and a
 * summoned one that comes back after a restart is not let in.
 */
public final class Summons {
    /** The tag of every summoned entity, so one left over after a restart can be told. */
    public static final String TAG = "greenlantern_summon";
    private static final Map<UUID, UUID> OWNERS = new ConcurrentHashMap<>();
    /** The copies that draw the monsters away from a player, by the player. */
    private static final Map<UUID, List<? extends LivingEntity>> DECOYS = new ConcurrentHashMap<>();

    private Summons() {}

    static void register() {
        LivingChangeTargetEvent.BUS.addListener(Summons::onTarget);
        LivingChangeTargetEvent.BUS.addListener(Summons::onDecoy);
        EntityJoinLevelEvent.BUS.addListener(event -> event.getEntity().getTags().contains(TAG) && !OWNERS.containsKey(event.getEntity().getUUID()));
    }

    /** The owner of a summoned or dominated creature, or null. */
    public static UUID owner(Entity entity) {
        return OWNERS.get(entity.getUUID());
    }

    public static boolean summoned(Entity entity) {
        return OWNERS.containsKey(entity.getUUID());
    }

    /** Puts a summoned entity in the world as the character's. */
    static <T extends Entity> T spawn(PresetContext c, T entity) {
        return spawn(c.player(), entity);
    }

    /** Puts an entity in the world as the player's (a summon, or a piece of a transformation). */
    public static <T extends Entity> T spawn(ServerPlayer owner, T entity) {
        entity.addTag(TAG);
        OWNERS.put(entity.getUUID(), owner.getUUID());
        if (entity instanceof Mob mob) mob.setPersistenceRequired();
        owner.level().addFreshEntity(entity);
        return entity;
    }

    /** Ends a summon: a puff and it is gone. */
    public static void dismiss(Entity entity) {
        OWNERS.remove(entity.getUUID());
        if (entity.isRemoved()) return;
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
        entity.discard();
    }

    /**
     * Makes a creature fight for the character for {@code ticks}: it goes for the monsters near its
     * owner and for whatever attacks them, and follows them otherwise. A {@code summoned} one is
     * dismissed at the end; a dominated one is let go.
     */
    static void adopt(PresetContext c, Mob mob, int ticks, boolean summoned, IntConsumer extra) {
        ServerPlayer owner = c.player();
        OWNERS.put(mob.getUUID(), owner.getUUID());
        PresetRuntime.start(owner, PresetRuntime.Task.ending(age -> {
            if (!mob.isAlive()) return false;
            if (age % 10 == 1) drive(owner, mob);
            if (extra != null) extra.accept(age);
            return age < ticks;
        }, () -> {
            if (summoned) dismiss(mob);
            else {
                OWNERS.remove(mob.getUUID());
                if (mob.isAlive()) mob.setTarget(null);
            }
        }));
    }

    /** One step of a helper's mind: pick a foe or follow the owner. */
    static void drive(ServerPlayer owner, Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target instanceof Player || summoned(target)) {
            LivingEntity foe = foe(owner, mob, 16.0);
            mob.setTarget(foe);
            target = foe;
        }
        if (target != null) return;
        double distance = mob.distanceTo(owner);
        if (distance > 24.0 || mob.level() != owner.level()) {
            mob.teleportTo(owner.getX() + 1.0, owner.getY(), owner.getZ() + 1.0);
        } else if (distance > 5.0) {
            mob.getNavigation().moveTo(owner, 1.2);
        }
    }

    /** The nearest creature to {@code from} that is a monster near the owner or that goes for the owner. */
    static LivingEntity foe(ServerPlayer owner, Entity from, double radius) {
        LivingEntity hurt = owner.getLastHurtMob();
        return Targets.inRadius(owner.level(), owner.position(), radius, e -> e != from && Targets.hittable(owner, e) && !summoned(e)
                        && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner) || e == hurt || e == owner.getLastHurtByMob()))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
    }

    /** While they last, the monsters that go for the player go for one of these instead. */
    static void decoys(ServerPlayer player, List<? extends LivingEntity> decoys) {
        if (decoys.isEmpty()) DECOYS.remove(player.getUUID());
        else DECOYS.put(player.getUUID(), decoys);
    }

    private static void onDecoy(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player) || summoned(event.getEntity())) return;
        List<? extends LivingEntity> decoys = DECOYS.get(player.getUUID());
        if (decoys == null) return;
        List<? extends LivingEntity> alive = decoys.stream().filter(LivingEntity::isAlive).toList();
        if (!alive.isEmpty()) event.setNewTarget(alive.get(event.getEntity().getRandom().nextInt(alive.size())));
    }

    /** A summon never turns on a player or on its owner's other summons. */
    private static boolean onTarget(LivingChangeTargetEvent event) {
        UUID owner = OWNERS.get(event.getEntity().getUUID());
        LivingEntity target = event.getNewTarget();
        if (owner == null || target == null) return false;
        return target instanceof Player || owner.equals(OWNERS.get(target.getUUID()));
    }
}
