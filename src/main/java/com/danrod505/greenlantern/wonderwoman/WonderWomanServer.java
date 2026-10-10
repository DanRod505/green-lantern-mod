package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.AmazonShieldEntity;
import com.danrod505.greenlantern.entity.ConstructEntity;
import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.WonderWomanSyncPacket;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server side of Wonder Woman: divine power coming back, the suit's gifts and her powers (the Lasso of
 * Truth, the Bracelets of Submission, the Amazon sword and shield and the Invisible Jet). Flight is the
 * shared power flight (see {@code ring.FlightHandler} and {@code flight.FlightProfile}), slower than the
 * Green Lantern's: she never breaks the sound barrier.
 */
public final class WonderWomanServer {
    /** Radius around her in which incoming projectiles are caught by the bracelets. */
    private static final double DEFLECT_RADIUS = 5.0;
    private static final int SPIN_WITH_TARGET_TICKS = 36;
    private static final int SPIN_ALONE_TICKS = 24;
    private static final double SPIN_RADIUS = 4.5;
    private static final double SWING_RADIUS = 3.2;

    private static final class State {
        float pendingRegen;
        float pendingCost;
        int guardTicks;
        int spinTicks;
        int spinAge;
        @Nullable LivingEntity spinTarget;
        @Nullable LassoEntity spinLasso;
        final Set<Integer> spinHit = new HashSet<>();
        /** Blows taken on the bracelets since the last shockwave (0-1): the next one hits harder. */
        float charge;
        int lastSentFlags;
        /** Projectiles already judged by the passive deflection (they roll the chance only once). */
        final Set<Integer> judged = new HashSet<>();
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private WonderWomanServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null && state.lastSentFlags != 0) {
            ModNetwork.sendToTrackingAndSelf(player, new WonderWomanSyncPacket(player.getId(), (byte) 0, AmazonFlags.EVENT_NONE));
        }
    }

    /** The armor is being taken off: the lasso, the shield and the weapons go away and the jet flies off. */
    public static void onSuitRemoved(ServerPlayer player) {
        LassoEntity lasso = LassoEntity.find(player);
        if (lasso != null) lasso.discard();
        AmazonShieldEntity shield = AmazonShieldEntity.find(player);
        if (shield != null) shield.discard();
        InvisibleJetEntity jet = InvisibleJetEntity.find(player);
        if (jet != null) jet.flyAway();
        removeWeapons(player);
        remove(player);
    }

    public static boolean isGuarding(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.guardTicks > 0;
    }

    public static boolean isSpinning(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.spinTicks > 0;
    }

    /** Sends a one-shot event (the shockwave, suiting up...) to her and the players around her. */
    public static void sendEvent(ServerPlayer player, int event) {
        State state = STATES.get(player.getUUID());
        int flags = state == null ? 0 : state.lastSentFlags;
        ModNetwork.sendToTrackingAndSelf(player, new WonderWomanSyncPacket(player.getId(), (byte) flags, event));
    }

    // ---- every server tick -------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = WonderWomanHelper.isSuited(player);
        if (player.tickCount % 20 == 0) WonderWomanSuit.updateModifiers(player, suited);
        if (!suited) {
            if (STATES.containsKey(player.getUUID())) remove(player);
            return;
        }
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty()) return;
        State state = get(player);
        ServerLevel level = player.level();

        // The gift of the gods comes back on its own (not while the bracelets drink from it).
        if (state.guardTicks <= 0) {
            state.pendingRegen += GLConfig.DIVINE_REGEN.get() / 20.0F;
            if (state.pendingRegen >= 1.0F) {
                int whole = (int) state.pendingRegen;
                state.pendingRegen -= whole;
                DivinePower.add(tiara, whole);
            }
        }
        if (player.tickCount % 60 == 0 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);

        if (state.guardTicks > 0) tickGuard(player, tiara, state, level);
        deflectProjectiles(player, state, level);
        if (state.spinTicks > 0) tickSpin(player, state, level);
        if (player.tickCount % 40 == 0) state.judged.clear();

        int flags = (state.guardTicks > 0 ? AmazonFlags.GUARD : 0) | (state.spinTicks > 0 ? AmazonFlags.SPIN : 0);
        if (flags != state.lastSentFlags || (flags != 0 && player.tickCount % 10 == 0)) {
            state.lastSentFlags = flags;
            ModNetwork.sendToTrackingAndSelf(player, new WonderWomanSyncPacket(player.getId(), (byte) flags, AmazonFlags.EVENT_NONE));
        }
    }

    public static void notifyNoPower(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_divine").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 1.1F);
    }

    // ---- powers --------------------------------------------------------------------------------------

    /** Uses a power (power key or right click with the tiara). Returns whether something happened. */
    public static boolean usePower(ServerPlayer player, ItemStack tiara, AmazonPower power) {
        if (!WonderWomanHelper.isSuited(player) || player.isSpectator()) return false;
        State state = get(player);
        // Powers already in use are switched off (or called back) for free.
        switch (power) {
            case LASSO_CAPTURE -> {
                LassoEntity lasso = LassoEntity.find(player);
                if (lasso != null) {
                    lasso.release();
                    return true;
                }
            }
            case LASSO_PULL -> {
                LassoEntity lasso = LassoEntity.find(player);
                if (lasso != null && lasso.boundTarget() == null) {
                    lasso.release();
                    return true;
                }
            }
            case BRACELET_GUARD -> {
                if (state.guardTicks > 0) {
                    endGuard(player, state);
                    return true;
                }
            }
            case SWORD_AND_SHIELD -> {
                if (WonderWomanHelper.swordSlot(player) >= 0 || WonderWomanHelper.shieldSlot(player) >= 0) {
                    removeWeapons(player);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHIELD_RETURN.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
                    return true;
                }
            }
            case SHIELD_THROW -> {
                AmazonShieldEntity shield = AmazonShieldEntity.find(player);
                if (shield != null) {
                    shield.recall();
                    return true;
                }
            }
            case INVISIBLE_JET -> {
                InvisibleJetEntity jet = InvisibleJetEntity.find(player);
                if (jet != null) {
                    if (player.getVehicle() == jet) {
                        jet.flyAway();
                    } else if (!jet.isVehicle()) {
                        // It comes to her and she climbs aboard, wherever it was parked.
                        jet.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
                        player.startRiding(jet);
                        jet.playArrival();
                    }
                    return true;
                }
            }
            default -> {}
        }
        if (state.spinTicks > 0) return false;
        if (player.getCooldowns().isOnCooldown(tiara)) return false;
        int cost = power == AmazonPower.BRACELET_GUARD ? Math.max(1, power.cost() / 4) : power.cost();
        if (!player.isCreative() && !DivinePower.has(tiara, cost)) {
            notifyNoPower(player);
            return false;
        }
        ServerLevel level = player.level();
        boolean used = switch (power) {
            case LASSO_CAPTURE -> throwLasso(player, LassoEntity.MODE_CAPTURE);
            case LASSO_PULL -> pullWithLasso(player, level);
            case LASSO_SPIN -> startSpin(player, state, level);
            case BRACELET_GUARD -> {
                state.guardTicks = Mth.ceil(GLConfig.BRACELET_GUARD_SECONDS.get() * 20.0);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BRACELET_GUARD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                yield true;
            }
            case BRACELET_SHOCKWAVE -> {
                shockwave(player, state, level);
                yield true;
            }
            case SWORD_AND_SHIELD -> summonWeapons(player);
            case SHIELD_THROW -> throwShield(player);
            case INVISIBLE_JET -> summonJet(player, level);
        };
        if (!used) return false;
        // The guard pays per second while it is up (the first drop now).
        if (!player.isCreative()) DivinePower.tryConsume(tiara, power == AmazonPower.BRACELET_GUARD ? 0 : power.cost());
        int cooldown = switch (power) {
            case LASSO_CAPTURE, LASSO_PULL, SHIELD_THROW -> 8;
            case LASSO_SPIN -> 20;
            case BRACELET_SHOCKWAVE -> 30;
            case INVISIBLE_JET -> 40;
            default -> 6;
        };
        player.getCooldowns().addCooldown(tiara, cooldown);
        return true;
    }

    // ---- the Lasso of Truth ----------------------------------------------------------------------------

    private static boolean throwLasso(ServerPlayer player, int mode) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        Vec3 from = player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.3, 0);
        // It leaves from her hand, but flies to whatever is under the crosshair.
        Vec3 dir = aimPoint(player, GLConfig.LASSO_RANGE.get()).subtract(from).normalize();
        LassoEntity lasso = LassoEntity.create(level, player, mode, from, dir.scale(LassoEntity.THROW_SPEED).add(player.getDeltaMovement()));
        level.addFreshEntity(lasso);
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LASSO_THROW.get(), SoundSource.PLAYERS, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        sendEvent(player, AmazonFlags.EVENT_LASSO_THROW);
        return true;
    }

    /** What she is looking at (a creature's middle, or the block under the crosshair), up to {@code range} blocks. */
    private static Vec3 aimPoint(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        HitResult block = player.pick(range, 1.0F, false);
        double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(reach));
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !(e instanceof ConstructEntity) && !player.isPassengerOfSameVehicle(e),
                reach * reach);
        return entity != null ? entity.getEntity().getBoundingBox().getCenter() : end;
    }

    /** Pull: a creature already caught is yanked to her feet; otherwise the lasso is thrown to pull. */
    private static boolean pullWithLasso(ServerPlayer player, ServerLevel level) {
        LassoEntity lasso = LassoEntity.find(player);
        LivingEntity held = lasso == null ? null : lasso.boundTarget();
        if (held == null) return throwLasso(player, LassoEntity.MODE_PULL);
        Vec3 to = player.position().subtract(held.position());
        double dist = to.length();
        if (dist > 1.0E-3) {
            Vec3 pull = to.scale(Math.min(2.0, 0.3 + dist * 0.16) / dist);
            held.setDeltaMovement(pull.x, 0.3 + Math.max(0.0, pull.y * 0.5), pull.z);
            held.hurtMarked = true;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, held.getX(), held.getY(), held.getZ(), ModSounds.LASSO_PULL.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), held.getX(), held.getY() + held.getBbHeight() * 0.5, held.getZ(), 10, 0.3, 0.3, 0.3, 0.08);
        return true;
    }

    /**
     * Spin: with a creature caught, she swings it around on the rope (it batters everything it meets)
     * and hurls it where she looks; with nothing caught, the lasso whirls around her like a golden
     * blade, sweeping creatures and projectiles away.
     */
    private static boolean startSpin(ServerPlayer player, State state, ServerLevel level) {
        LassoEntity lasso = LassoEntity.find(player);
        LivingEntity held = lasso == null ? null : lasso.boundTarget();
        state.spinHit.clear();
        state.spinAge = 0;
        state.spinTarget = held;
        state.spinLasso = held != null ? lasso : null;
        state.spinTicks = held != null ? SPIN_WITH_TARGET_TICKS : SPIN_ALONE_TICKS;
        if (held == null && lasso != null) lasso.discard();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LASSO_SPIN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    private static void tickSpin(ServerPlayer player, State state, ServerLevel level) {
        state.spinTicks--;
        state.spinAge++;
        float damage = GLConfig.LASSO_SPIN_DAMAGE.get().floatValue();
        double angle = state.spinAge * 0.5 + Math.toRadians(player.getYRot());
        if (state.spinTarget != null) {
            LivingEntity held = state.spinTarget;
            if (!held.isAlive() || state.spinLasso == null || state.spinLasso.isRemoved() || state.spinLasso.boundTarget() != held) {
                endSpin(state);
                return;
            }
            // Around and around, higher as she gathers speed.
            double lift = Math.min(1.6, state.spinAge * 0.08);
            Vec3 want = player.position().add(Math.cos(angle) * SWING_RADIUS, 0.4 + lift, Math.sin(angle) * SWING_RADIUS);
            Vec3 move = want.subtract(held.position());
            held.setDeltaMovement(move.length() > 2.5 ? move.normalize().scale(2.5) : move);
            held.hurtMarked = true;
            held.resetFallDistance();
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, held.getBoundingBox().inflate(0.8),
                    e -> e != player && e != held && e.isAlive() && !e.isSpectator() && !player.isPassengerOfSameVehicle(e))) {
                if (!state.spinHit.add(other.getId())) continue;
                other.invulnerableTime = 0;
                other.hurtServer(level, ModDamageTypes.amazon(level, held, player), damage);
                Vec3 away = other.position().subtract(player.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle() : away.normalize();
                other.push(away.x * 1.4, 0.5, away.z * 1.4);
                other.hurtMarked = true;
                level.playSound(null, other.getX(), other.getY(), other.getZ(), ModSounds.SHIELD_HIT.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
            }
            if (state.spinAge % 6 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LASSO_SPIN.get(), SoundSource.PLAYERS, 0.7F, 1.1F + state.spinAge * 0.01F);
            }
            if (state.spinTicks <= 0) {
                // Hurled where she looks.
                LassoEntity lasso = state.spinLasso;
                lasso.letGo();
                Vec3 look = player.getLookAngle();
                held.setDeltaMovement(look.x * 2.4, Math.max(0.5, look.y * 2.0 + 0.5), look.z * 2.4);
                held.hurtMarked = true;
                held.invulnerableTime = 0;
                held.hurtServer(level, ModDamageTypes.amazon(level, player, player), damage);
                player.swing(InteractionHand.MAIN_HAND, true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LASSO_THROW.get(), SoundSource.PLAYERS, 1.3F, 0.7F);
                endSpin(state);
            }
            return;
        }
        // Alone: the golden rope whirls around her.
        if (state.spinAge % 3 == 0) {
            for (Entity target : level.getEntities(player, player.getBoundingBox().inflate(SPIN_RADIUS, 1.5, SPIN_RADIUS),
                    e -> e.isAlive() && !e.isSpectator() && !player.isPassengerOfSameVehicle(e) && !(e instanceof ConstructEntity))) {
                Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
                double dist = away.length();
                if (dist > SPIN_RADIUS) continue;
                away = dist < 1.0E-3 ? player.getLookAngle().multiply(1, 0, 1).normalize() : away.scale(1.0 / dist);
                if (target instanceof Projectile projectile) {
                    if (projectile.getOwner() == player) continue;
                    projectile.setDeltaMovement(away.scale(1.4).add(0, 0.2, 0));
                    projectile.setOwner(player);
                    projectile.hurtMarked = true;
                    continue;
                }
                if (!(target instanceof LivingEntity living)) continue;
                if (state.spinHit.add(living.getId())) {
                    living.invulnerableTime = 0;
                    living.hurtServer(level, ModDamageTypes.amazon(level, player, player), damage);
                }
                living.push(away.x * 0.9, 0.3, away.z * 0.9);
                living.hurtMarked = true;
            }
        }
        if (state.spinAge % 8 == 0) state.spinHit.clear();
        for (int i = 0; i < 3; i++) {
            double a = angle + i * 0.25;
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), player.getX() + Math.cos(a) * (SPIN_RADIUS - 0.8), player.getY() + 1.1,
                    player.getZ() + Math.sin(a) * (SPIN_RADIUS - 0.8), 1, 0, 0, 0, 0);
        }
        if (state.spinAge % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LASSO_SPIN.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        if (state.spinTicks <= 0) endSpin(state);
    }

    private static void endSpin(State state) {
        state.spinTicks = 0;
        state.spinTarget = null;
        state.spinLasso = null;
        state.spinHit.clear();
    }

    // ---- the Bracelets of Submission -------------------------------------------------------------------

    private static void tickGuard(ServerPlayer player, ItemStack tiara, State state, ServerLevel level) {
        if (!player.isCreative()) {
            state.pendingCost += GLConfig.BRACELET_GUARD_COST_PER_SECOND.get() / 20.0F;
            if (state.pendingCost >= 1.0F) {
                int whole = (int) state.pendingCost;
                state.pendingCost -= whole;
                boolean enough = DivinePower.has(tiara, whole);
                DivinePower.drain(tiara, whole);
                if (!enough) {
                    notifyNoPower(player);
                    endGuard(player, state);
                    return;
                }
            }
        }
        if (--state.guardTicks <= 0) {
            endGuard(player, state);
            return;
        }
        if (state.guardTicks % 4 == 0) {
            Vec3 front = braceletPoint(player);
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), front.x, front.y, front.z, 1, 0.2, 0.15, 0.2, 0.01);
        }
    }

    private static void endGuard(ServerPlayer player, State state) {
        state.guardTicks = 0;
        state.pendingCost = 0;
    }

    /** Where the crossed bracelets are, in front of her chest. */
    private static Vec3 braceletPoint(Player player) {
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        return player.position().add(0, player.getBbHeight() * 0.68, 0).add(look.scale(0.5));
    }

    /** Whether a hit coming from {@code from} meets her in front (where the bracelets are). */
    private static boolean fromFront(Player player, Vec3 from, double minDot) {
        Vec3 to = from.subtract(player.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-4) return true;
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        return to.normalize().dot(facing) > minDot;
    }

    /**
     * Arrows, fireballs and tridents flying at her bounce off the bracelets back where they came from:
     * always while she guards, and now and then even when she doesn't.
     */
    private static void deflectProjectiles(ServerPlayer player, State state, ServerLevel level) {
        boolean guarding = state.guardTicks > 0;
        double chance = GLConfig.BRACELET_PASSIVE_DEFLECT_CHANCE.get();
        if (!guarding && chance <= 0.0) return;
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.6, 0);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(DEFLECT_RADIUS), p -> {
            if (p.getOwner() == player || p.isRemoved() || p instanceof LassoEntity || p instanceof AmazonShieldEntity) return false;
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.04) return false;
            Vec3 to = center.subtract(p.position());
            // Heading for her, and close enough to arrive within a couple of ticks.
            return to.normalize().dot(v.normalize()) > 0.75 && to.length() < Math.max(2.0, v.length() * 2.5);
        })) {
            if (!fromFront(player, projectile.position(), guarding ? -0.2 : 0.35)) continue;
            if (!guarding) {
                if (!state.judged.add(projectile.getId())) continue;
                if (player.getRandom().nextFloat() >= chance) continue;
            }
            reflect(player, state, level, projectile);
        }
    }

    private static void reflect(ServerPlayer player, State state, ServerLevel level, Projectile projectile) {
        Entity shooter = projectile.getOwner();
        double speed = Math.max(1.2, projectile.getDeltaMovement().length() * 1.1);
        Vec3 dir;
        if (shooter != null && shooter != player && shooter.isAlive() && shooter.distanceToSqr(player) < 64 * 64) {
            dir = shooter.getEyePosition().add(0, -0.3, 0).subtract(projectile.position()).normalize();
        } else {
            dir = projectile.getDeltaMovement().scale(-1).normalize();
        }
        projectile.setDeltaMovement(dir.scale(speed));
        projectile.setOwner(player);
        projectile.hurtMarked = true;
        if (projectile instanceof net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile hurting) {
            hurting.accelerationPower = Math.max(hurting.accelerationPower, 0.1);
        }
        state.charge = Math.min(1.0F, state.charge + 0.15F);
        Vec3 at = projectile.position();
        level.playSound(null, at.x, at.y, at.z, ModSounds.BRACELET_DEFLECT.get(), SoundSource.PLAYERS, 1.1F, 0.9F + player.getRandom().nextFloat() * 0.25F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), at.x, at.y, at.z, 10, 0.1, 0.1, 0.1, 0.25);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.3);
        player.swing(player.getRandom().nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        sendEvent(player, AmazonFlags.EVENT_DEFLECT);
    }

    /**
     * Called when something tries to hurt her: with the bracelets raised, blows and projectiles from the
     * front are blocked (the attacker is thrown back). Returns true to cancel the attack.
     */
    public static boolean onAttacked(ServerPlayer player, DamageSource source) {
        if (!WonderWomanHelper.isSuited(player)) return false;
        State state = STATES.get(player.getUUID());
        if (state == null || state.guardTicks <= 0) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) return false;
        Vec3 from = source.getSourcePosition();
        if (from == null) return false;
        if (!fromFront(player, from, -0.2)) return false;
        ServerLevel level = player.level();
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            if (!projectile.isRemoved()) reflect(player, state, level, projectile);
            return true;
        }
        // A melee blow rings off the silver: the attacker staggers back.
        if (direct instanceof LivingEntity attacker && attacker != player) {
            Vec3 away = attacker.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, player.getYRot()) : away.normalize();
            attacker.push(away.x * 1.1, 0.3, away.z * 1.1);
            attacker.hurtMarked = true;
        }
        state.charge = Math.min(1.0F, state.charge + 0.2F);
        Vec3 at = braceletPoint(player);
        level.playSound(null, at.x, at.y, at.z, ModSounds.BRACELET_DEFLECT.get(), SoundSource.PLAYERS, 1.0F, 0.75F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.2);
        sendEvent(player, AmazonFlags.EVENT_DEFLECT);
        return true;
    }

    /** The bracelets clash: a ring of force that throws everything around (stronger after blocking hits). */
    private static void shockwave(ServerPlayer player, State state, ServerLevel level) {
        float charge = state.charge;
        state.charge = 0.0F;
        double radius = GLConfig.BRACELET_SHOCKWAVE_RADIUS.get() + charge * 3.0;
        float damage = GLConfig.BRACELET_SHOCKWAVE_DAMAGE.get().floatValue() * (1.0F + charge * 0.5F);
        Vec3 center = player.position().add(0, 1.0, 0);
        for (Entity target : level.getEntities(player, new AABB(center, center).inflate(radius),
                e -> e.isAlive() && !e.isSpectator() && !player.isPassengerOfSameVehicle(e) && !(e instanceof ConstructEntity))) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(center);
            double dist = to.length();
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.6 * dist / radius);
            Vec3 away = new Vec3(to.x, 0, to.z);
            away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1, 0, 1).normalize() : away.normalize();
            if (target instanceof Projectile projectile) {
                if (projectile.getOwner() == player) continue;
                projectile.setDeltaMovement(away.scale(1.5).add(0, 0.2, 0));
                projectile.setOwner(player);
                projectile.hurtMarked = true;
                continue;
            }
            if (target instanceof LivingEntity living) {
                if (living instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                living.invulnerableTime = 0;
                living.hurtServer(level, ModDamageTypes.amazon(level, player, player), damage * falloff);
            }
            double strength = 2.0 * falloff * (1.0 + charge * 0.5);
            target.push(away.x * strength, 0.5 * falloff + 0.3, away.z * strength);
            target.hurtMarked = true;
        }
        if (GLConfig.BRACELET_SHOCKWAVE_BREAKS_GLASS.get()) shatterGlass(player, level, center, radius * 0.6);

        player.swing(InteractionHand.MAIN_HAND, true);
        player.swing(InteractionHand.OFF_HAND, true);
        level.playSound(null, center.x, center.y, center.z, ModSounds.BRACELET_SHOCKWAVE.get(), SoundSource.PLAYERS, 2.5F, 0.95F + level.random.nextFloat() * 0.1F);
        double groundY = groundBelow(level, player.position().add(0, 0.5, 0));
        level.sendParticles(ModParticles.AMAZON_SHOCKWAVE.get(), center.x, groundY + 0.1, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), center.x, center.y + 0.3, center.z, 60, 0.5, 0.4, 0.5, 0.6);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.4, 0.3, 0.4, 0);
        dust(level, new Vec3(center.x, groundY, center.z), radius, 50);
        sendEvent(player, AmazonFlags.EVENT_SHOCKWAVE);
    }

    private static void shatterGlass(ServerPlayer player, ServerLevel level, Vec3 center, double radius) {
        BlockPos c = BlockPos.containing(center);
        int r = Mth.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, r, r))) {
            if (pos.distToCenterSqr(center) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(net.minecraftforge.common.Tags.Blocks.GLASS_BLOCKS) || state.is(net.minecraftforge.common.Tags.Blocks.GLASS_PANES)
                    || state.is(BlockTags.ICE) && !state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE) && !state.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)) {
                level.destroyBlock(pos.immutable(), false, player);
            }
        }
    }

    private static double groundBelow(ServerLevel level, Vec3 at) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return pos.getY();
            pos.move(Direction.DOWN);
        }
        return at.y;
    }

    private static void dust(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double r = level.random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockState state = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 3, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }

    // ---- the Amazon sword and shield ---------------------------------------------------------------------

    /** The sword in her hand and the shield on her arm (what was there goes to the backpack). */
    private static boolean summonWeapons(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        if (WonderWomanHelper.swordSlot(player) < 0) {
            ItemStack sword = new ItemStack(ModItems.AMAZON_SWORD.get());
            int selected = inventory.getSelectedSlot();
            ItemStack inHand = inventory.getItem(selected);
            if (inHand.isEmpty()) {
                inventory.setItem(selected, sword);
            } else if (!WonderWomanHelper.isTiara(inHand) && moveAside(player, inHand)) {
                inventory.setItem(selected, sword);
            } else if (!inventory.add(sword)) {
                player.displayClientMessage(Component.translatable("message.greenlantern.weapons_no_room").withStyle(ChatFormatting.GOLD), true);
                return false;
            }
        }
        if (WonderWomanHelper.shieldSlot(player) < 0 && AmazonShieldEntity.find(player) == null) {
            giveShield(player);
        }
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AMAZON_SWORD_SWING.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.4, 0.5, 0.4, 0.08);
        return true;
    }

    /** The shield goes on her arm (off hand), or into the backpack if the arm is busy. */
    public static void giveShield(ServerPlayer player) {
        ItemStack shield = new ItemStack(ModItems.AMAZON_SHIELD.get());
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, shield);
        } else if (!WonderWomanHelper.isTiara(offhand) && moveAside(player, offhand)) {
            player.setItemInHand(InteractionHand.OFF_HAND, shield);
        } else if (!player.getInventory().add(shield)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.weapons_no_room").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Moves an item from a hand into a free slot of the backpack (false when there is none). */
    private static boolean moveAside(ServerPlayer player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        int free = inventory.getFreeSlot();
        if (free < 0) return false;
        inventory.setItem(free, stack.copy());
        stack.setCount(0);
        return true;
    }

    /** The sword and the shield go back to Themyscira (they never stay without the armor). */
    public static void removeWeapons(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (WonderWomanHelper.isSword(stack) || WonderWomanHelper.isShield(stack)) inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    /** The shield flies from her arm (thrown even when it was put away: it comes when she calls). */
    public static boolean throwShield(ServerPlayer player) {
        if (AmazonShieldEntity.find(player) != null) return false;
        int slot = WonderWomanHelper.shieldSlot(player);
        if (slot >= 0) player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (player.isUsingItem() && WonderWomanHelper.isShield(player.getUseItem())) player.stopUsingItem();
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(0.7)).add(0, -0.35, 0);
        Vec3 dir = aimPoint(player, 48.0).subtract(from).normalize();
        AmazonShieldEntity shield = AmazonShieldEntity.create(level, player, from, dir.scale(AmazonShieldEntity.THROW_SPEED));
        level.addFreshEntity(shield);
        player.swing(InteractionHand.OFF_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHIELD_THROW.get(), SoundSource.PLAYERS, 1.1F, 0.95F + player.getRandom().nextFloat() * 0.1F);
        return true;
    }

    /** Throw from the shield itself (sneak + use): pays like the power. */
    public static void throwShieldFromItem(ServerPlayer player) {
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty() || !WonderWomanHelper.isSuited(player)) return;
        usePower(player, tiara, AmazonPower.SHIELD_THROW);
    }

    // ---- the Invisible Jet -------------------------------------------------------------------------------

    private static boolean summonJet(ServerPlayer player, ServerLevel level) {
        if (player.isPassenger()) player.stopRiding();
        InvisibleJetEntity jet = InvisibleJetEntity.create(level, player);
        if (!InvisibleJetEntity.fits(level, jet)) {
            // Not enough room here: it comes a little higher up.
            jet.setPos(jet.getX(), jet.getY() + 2.0, jet.getZ());
            if (!InvisibleJetEntity.fits(level, jet)) {
                player.displayClientMessage(Component.translatable("message.greenlantern.jet_no_room").withStyle(ChatFormatting.GOLD), true);
                return false;
            }
        }
        level.addFreshEntity(jet);
        player.startRiding(jet);
        jet.playArrival();
        player.displayClientMessage(Component.translatable("message.greenlantern.jet_summoned").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ---- power wheel / key packets -----------------------------------------------------------------------

    public static void selectPower(ServerPlayer player, int value, boolean relative) {
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty()) return;
        AmazonPower power;
        if (relative) {
            power = AmazonPower.cycle(tiara, Mth.clamp(value, -1, 1));
        } else {
            power = AmazonPower.byIndex(value);
            AmazonPower.select(tiara, power);
        }
        player.displayClientMessage(Component.translatable("message.greenlantern.power_selected", power.displayName().copy().withStyle(ChatFormatting.GOLD)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POWER_SELECT.get(), SoundSource.PLAYERS,
                0.7F, 0.85F + 0.06F * power.ordinal());
    }

    public static void usePowerKey(ServerPlayer player, int index) {
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty()) return;
        usePower(player, tiara, index < 0 ? AmazonPower.selected(tiara) : AmazonPower.byIndex(index));
    }
}
