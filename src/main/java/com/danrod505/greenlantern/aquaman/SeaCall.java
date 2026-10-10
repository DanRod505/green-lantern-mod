package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The call of the sea: the sea creatures around Aquaman (fish, squid, dolphins, turtles, axolotls,
 * nautiluses) follow him and fight for him for a while. If few creatures answer and Aquaman is in
 * the water, dolphins come from the deep to help (and swim away when the call ends).
 */
public final class SeaCall {
    /** One creature that answered the call. */
    private static final class Ally {
        final Mob mob;
        final boolean summoned;
        @Nullable LivingEntity target;
        int attackCooldown;

        Ally(Mob mob, boolean summoned) {
            this.mob = mob;
            this.summoned = summoned;
        }
    }

    private static final class Call {
        final List<Ally> allies = new ArrayList<>();
        long endTime;
    }

    private static final Map<UUID, Call> CALLS = new HashMap<>();
    /** Creature id -> owner, so a creature only answers one call at a time. */
    private static final Map<UUID, UUID> CALLED = new HashMap<>();

    private SeaCall() {}

    public static boolean isActive(Player owner) {
        Call call = CALLS.get(owner.getUUID());
        return call != null && !call.allies.isEmpty();
    }

    /** How many creatures follow the player right now. */
    public static int allies(Player owner) {
        Call call = CALLS.get(owner.getUUID());
        return call == null ? 0 : call.allies.size();
    }

    public static boolean isCalledBy(Entity creature, Player owner) {
        return owner.getUUID().equals(CALLED.get(creature.getUUID()));
    }

    /** Whether the creature lives in the sea and can answer the call (never hostile monsters). */
    public static boolean isSeaCreature(Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.isAlive() || entity instanceof Enemy) return false;
        return entity instanceof WaterAnimal || entity instanceof AgeableWaterCreature || entity instanceof Axolotl
                || entity instanceof Turtle || entity instanceof AbstractNautilus;
    }

    /** Starts the call. Returns false (and nothing happens) when no creature can answer. */
    public static boolean start(ServerPlayer owner) {
        ServerLevel level = owner.level();
        double radius = GLConfig.SEA_CALL_RADIUS.get();
        List<Mob> found = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(radius),
                e -> isSeaCreature(e) && !CALLED.containsKey(e.getUUID()));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(owner)));
        int max = GLConfig.SEA_CALL_MAX_CREATURES.get();
        if (found.size() > max) found = found.subList(0, max);

        Call call = new Call();
        for (Mob mob : found) call.allies.add(new Ally(mob, false));
        // Too few answered: dolphins come from the deep (only in the water, of course).
        int helpers = GLConfig.SEA_CALL_HELPERS.get() - found.size();
        if (helpers > 0 && owner.isInWater()) {
            for (int i = 0; i < helpers; i++) {
                Dolphin dolphin = EntityType.DOLPHIN.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (dolphin == null) continue;
                double a = Mth.TWO_PI * i / helpers;
                Vec3 at = owner.position().add(Math.cos(a) * 3.0, 0.2, Math.sin(a) * 3.0);
                if (!level.getFluidState(net.minecraft.core.BlockPos.containing(at)).is(net.minecraft.tags.FluidTags.WATER)) {
                    at = owner.position();
                }
                dolphin.snapTo(at.x, at.y, at.z, owner.getYRot(), 0);
                dolphin.setAirSupply(dolphin.getMaxAirSupply());
                level.addFreshEntity(dolphin);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.1);
                call.allies.add(new Ally(dolphin, true));
            }
        }
        if (call.allies.isEmpty()) return false;

        release(owner, false);
        call.endTime = level.getGameTime() + Math.round(GLConfig.SEA_CALL_SECONDS.get() * 20);
        CALLS.put(owner.getUUID(), call);
        for (Ally ally : call.allies) {
            CALLED.put(ally.mob.getUUID(), owner.getUUID());
            ally.mob.getNavigation().stop();
            level.sendParticles(ParticleTypes.DOLPHIN, ally.mob.getX(), ally.mob.getY() + ally.mob.getBbHeight(), ally.mob.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
        // A ring of bubbles spreading out from Aquaman: the call travels through the water.
        for (int i = 0; i < 48; i++) {
            double a = Mth.TWO_PI * i / 48;
            level.sendParticles(ParticleTypes.NAUTILUS, owner.getX(), owner.getY() + 1.0, owner.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 1.2);
            level.sendParticles(ParticleTypes.BUBBLE, owner.getX() + Math.cos(a) * 1.5, owner.getY() + 1.0, owner.getZ() + Math.sin(a) * 1.5, 1, 0, 0, 0, 0);
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SEA_CALL.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        return true;
    }

    /** Ends the call: creatures go back to their lives, the dolphins from the deep swim away. */
    public static void release(ServerPlayer owner, boolean effects) {
        Call call = CALLS.remove(owner.getUUID());
        if (call == null) return;
        for (Ally ally : call.allies) {
            CALLED.remove(ally.mob.getUUID());
            if (ally.mob.isRemoved()) continue;
            if (ally.summoned) {
                if (ally.mob.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, ally.mob.getX(), ally.mob.getY(), ally.mob.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
                }
                ally.mob.discard();
            } else {
                ally.mob.getNavigation().stop();
            }
        }
        if (effects) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }

    /** Every server tick, for every player. */
    public static void tick(ServerPlayer owner) {
        Call call = CALLS.get(owner.getUUID());
        if (call == null) return;
        ServerLevel level = owner.level();
        if (!owner.isAlive() || !AquamanHelper.isSuited(owner) || level.getGameTime() > call.endTime) {
            if (owner.isAlive() && level.getGameTime() > call.endTime) {
                owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.greenlantern.sea_call_end")
                        .withStyle(net.minecraft.ChatFormatting.AQUA), true);
            }
            release(owner, true);
            return;
        }
        Iterator<Ally> it = call.allies.iterator();
        while (it.hasNext()) {
            Ally ally = it.next();
            Mob mob = ally.mob;
            if (mob.isRemoved() || !mob.isAlive() || mob.level() != level || mob.distanceToSqr(owner) > 96 * 96) {
                CALLED.remove(mob.getUUID());
                if (ally.summoned && !mob.isRemoved()) mob.discard();
                it.remove();
                continue;
            }
            tickAlly(level, owner, ally);
        }
        if (call.allies.isEmpty()) CALLS.remove(owner.getUUID());
    }

    private static void tickAlly(ServerLevel level, ServerPlayer owner, Ally ally) {
        Mob mob = ally.mob;
        if (ally.attackCooldown > 0) ally.attackCooldown--;
        // The deep dolphins never run out of air while helping.
        if (ally.summoned) mob.setAirSupply(mob.getMaxAirSupply());

        if (ally.target == null || !ally.target.isAlive() || ally.target.distanceToSqr(owner) > 24 * 24) {
            ally.target = findTarget(level, owner);
        }
        LivingEntity target = ally.target;
        if (target != null) {
            moveTowards(mob, target.position().add(0, target.getBbHeight() * 0.5, 0), speed(mob));
            double reach = 1.0 + (mob.getBbWidth() + target.getBbWidth()) * 0.5;
            if (ally.attackCooldown == 0 && mob.distanceToSqr(target) < reach * reach) {
                attack(level, mob, target);
                ally.attackCooldown = 20;
            }
        } else if (mob.distanceToSqr(owner) > 16) {
            // Follow Aquaman, a little behind and around him.
            double a = (mob.getId() * 1.7) % Mth.TWO_PI;
            Vec3 spot = owner.position().add(Math.cos(a) * 2.5, 0.6 + (mob.getId() % 3) * 0.4, Math.sin(a) * 2.5);
            moveTowards(mob, spot, speed(mob));
        }
        if (mob.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.DOLPHIN, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Who to fight: whoever hurt Aquaman recently, else the closest monster around him. */
    private static @Nullable LivingEntity findTarget(ServerLevel level, ServerPlayer owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker != owner && attacker.distanceToSqr(owner) < 20 * 20
                && !CALLED.containsKey(attacker.getUUID()) && !(attacker instanceof Player p && p.isCreative())) {
            return attacker;
        }
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(14),
                e -> e instanceof Enemy && e.isAlive() && !e.isInvisible()
                        && !(e instanceof OwnableEntity ownable && ownable.getOwner() == owner));
        enemies.sort(Comparator.comparingDouble(e -> e.distanceToSqr(owner)));
        return enemies.isEmpty() ? null : enemies.getFirst();
    }

    private static double speed(Mob mob) {
        if (mob instanceof Dolphin || mob instanceof AbstractNautilus) return 0.85;
        if (mob instanceof Squid) return 0.45;
        if (mob instanceof Turtle) return 0.35;
        return 0.5;
    }

    /** Swims straight at the point (sea creatures in water) or walks there (on land). */
    private static void moveTowards(Mob mob, Vec3 to, double speed) {
        Vec3 delta = to.subtract(mob.position());
        if (mob.isInWater()) {
            if (delta.lengthSqr() < 0.01) return;
            Vec3 wish = delta.normalize().scale(Math.min(speed, delta.length() * 0.5));
            Vec3 motion = mob.getDeltaMovement().lerp(wish, 0.3);
            mob.setDeltaMovement(motion);
            mob.getNavigation().stop();
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
            mob.hurtMarked = true;
        } else if (mob instanceof Axolotl || mob instanceof Turtle) {
            mob.getNavigation().moveTo(to.x, to.y, to.z, 1.4);
        }
    }

    private static void attack(ServerLevel level, Mob mob, LivingEntity target) {
        float damage;
        if (mob instanceof Dolphin || mob instanceof AbstractNautilus) {
            damage = 5.0F;
        } else if (mob instanceof Axolotl || mob instanceof Turtle || mob instanceof Squid) {
            damage = 3.0F;
        } else {
            damage = 2.0F;
        }
        damage *= GLConfig.SEA_CALL_DAMAGE_MULTIPLIER.get().floatValue();
        if (target.hurtServer(level, level.damageSources().mobAttack(mob), damage)) {
            Vec3 push = target.position().subtract(mob.position()).normalize();
            target.push(push.x * 0.4, 0.15, push.z * 0.4);
            target.hurtMarked = true;
            if (mob instanceof GlowSquid) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200), mob);
            } else if (mob instanceof Squid) {
                // A cloud of ink in the face.
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60), mob);
                level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getEyeY(), target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            } else if (mob instanceof Pufferfish) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100), mob);
            }
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.DOLPHIN_ATTACK, SoundSource.NEUTRAL, 0.8F, 1.0F + level.random.nextFloat() * 0.3F);
    }
}
