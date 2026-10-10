package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.BatDefenderEntity;
import com.danrod505.greenlantern.entity.BatarangEntity;
import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.danrod505.greenlantern.entity.GrappleHookEntity;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of Batman: the night lenses of the cowl, the belt charge, gliding with the cape (the
 * glide itself is simulated by the client, see {@code client.batman.GlideController}) and the
 * gadgets (the batarang, the grapnel gun, the swarm of bats and the Batmobile).
 */
public final class BatmanServer {
    /** Duration of the night vision of the cowl, refreshed while it is worn (long enough not to flicker). */
    private static final int NIGHT_VISION_TICKS = 300;
    /** After a glide or a grapple, falls stay harmless this long (ticks): time to land. */
    private static final int LANDING_GRACE = 60;

    private static final class State {
        float pendingCharge;
        boolean gliding;
        int safeLandingUntil;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private BatmanServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        releaseSwarm(player, false);
        STATES.remove(player.getUUID());
    }

    // ---- every server tick -------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = BatmanHelper.isSuited(player);
        if (player.tickCount % 20 == 0) BatmanSuit.updateModifiers(player, suited);
        if (!suited) {
            STATES.remove(player.getUUID());
            return;
        }
        ItemStack belt = BatmanHelper.findBelt(player);
        if (belt.isEmpty()) return;
        State state = get(player);

        // The cowl's lenses: the dark is no place to hide from Batman.
        if (BatmanHelper.hasCowl(player) && player.tickCount % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, true));
        }

        // The belt's cells recharge, faster in the dark.
        boolean dark = player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        float regen = GLConfig.BAT_CHARGE_REGEN.get() * (dark ? 1.5F : 1.0F);
        state.pendingCharge += regen / 20.0F;
        if (state.pendingCharge >= 1.0F) {
            int whole = (int) state.pendingCharge;
            state.pendingCharge -= whole;
            BatmanHero.BAT_CHARGE.add(belt, whole);
        }

        // Gliding (reported by the client): no fall damage, ever.
        if (state.gliding) {
            if (player.onGround() || player.isInWater() || player.isPassenger() || player.getAbilities().flying) {
                state.gliding = false;
            } else {
                state.safeLandingUntil = player.tickCount + LANDING_GRACE;
            }
        }
        if (GrappleHookEntity.find(player) != null) state.safeLandingUntil = player.tickCount + LANDING_GRACE;
        if (player.tickCount < state.safeLandingUntil) player.resetFallDistance();

        if (player.tickCount % 2 == 0) defendFromProjectiles(player);
    }

    /** The suit is being taken off: the gadgets go back in the belt, the bats fly away, the car drives off. */
    public static void onSuitRemoved(ServerPlayer player) {
        GrappleHookEntity hook = GrappleHookEntity.find(player);
        if (hook != null) hook.discard();
        for (BatarangEntity batarang : BatarangEntity.findAll(player)) batarang.discard();
        releaseSwarm(player, false);
        BatmobileEntity car = BatmobileEntity.find(player);
        if (car != null) car.driveAway();
        MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
        if (vision != null && vision.getDuration() <= NIGHT_VISION_TICKS && vision.isAmbient()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        STATES.remove(player.getUUID());
    }

    // ---- gliding --------------------------------------------------------------------------------------

    /** The client started or stopped gliding with the cape. */
    public static void setGliding(ServerPlayer player, boolean gliding) {
        if (gliding && (!BatmanHelper.isSuited(player) || player.onGround())) return;
        State state = get(player);
        if (gliding && !state.gliding) {
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.CAPE_GLIDE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        state.gliding = gliding;
        if (gliding) state.safeLandingUntil = player.tickCount + LANDING_GRACE;
    }

    public static boolean isGliding(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.gliding;
    }

    /** Whether a fall right now ends harmlessly (gliding, or just after a glide or a grapple). */
    public static boolean safeLanding(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && (state.gliding || player.tickCount < state.safeLandingUntil);
    }

    // ---- powers --------------------------------------------------------------------------------------

    /** Uses a gadget (power key or right click with the belt). Returns whether something happened. */
    public static boolean usePower(ServerPlayer player, ItemStack belt, BatPower power) {
        if (!BatmanHelper.isSuited(player) || player.isSpectator()) return false;
        // Gadgets already in use are toggled without cost.
        switch (power) {
            case BATARANG -> {}
            case GRAPPLE -> {
                GrappleHookEntity hook = GrappleHookEntity.find(player);
                if (hook != null) {
                    hook.release();
                    return true;
                }
            }
            case BAT_SWARM -> {
                if (isSwarmActive(player)) {
                    releaseSwarm(player, true);
                    return true;
                }
            }
            case BATMOBILE -> {
                BatmobileEntity car = BatmobileEntity.find(player);
                if (car != null) {
                    if (player.getVehicle() == car) {
                        car.driveAway();
                    } else if (!car.isVehicle()) {
                        // Call it back and climb in, wherever it is parked.
                        car.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
                        player.startRiding(car);
                        car.playEngineStart();
                    }
                    return true;
                }
            }
        }
        if (player.getCooldowns().isOnCooldown(belt)) return false;
        if (!player.isCreative() && !BatmanHero.BAT_CHARGE.has(belt, power.cost())) {
            notifyNoCharge(player);
            return false;
        }
        boolean used = switch (power) {
            case BATARANG -> throwBatarang(player);
            case GRAPPLE -> fireGrapple(player);
            case BAT_SWARM -> summonSwarm(player);
            case BATMOBILE -> summonBatmobile(player);
        };
        if (used) {
            if (!player.isCreative()) BatmanHero.BAT_CHARGE.tryConsume(belt, power.cost());
            int cooldown = switch (power) {
                case BATARANG -> 6;
                case GRAPPLE -> 8;
                case BAT_SWARM, BATMOBILE -> 40;
            };
            player.getCooldowns().addCooldown(belt, cooldown);
        }
        return used;
    }

    public static void notifyNoCharge(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_bat_charge").withStyle(ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 0.7F);
    }

    // ---- the batarang ----------------------------------------------------------------------------------

    public static boolean throwBatarang(ServerPlayer player) {
        if (BatarangEntity.findAll(player).size() >= 3) return false;
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        Vec3 from = player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.3)).add(0, -0.25, 0);
        BatarangEntity batarang = BatarangEntity.create(level, player, from, look.scale(BatarangEntity.THROW_SPEED));
        level.addFreshEntity(batarang);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BATARANG_THROW.get(), SoundSource.PLAYERS, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        return true;
    }

    // ---- the grapnel gun -------------------------------------------------------------------------------

    public static boolean fireGrapple(ServerPlayer player) {
        if (player.isPassenger()) return false;
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(0.5)).add(0, -0.2, 0);
        GrappleHookEntity hook = GrappleHookEntity.create(level, player, from, look.scale(GrappleHookEntity.SPEED));
        level.addFreshEntity(hook);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRAPPLE_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // ---- the swarm of bats -----------------------------------------------------------------------------

    public static boolean isSwarmActive(ServerPlayer player) {
        return !BatDefenderEntity.findAll(player).isEmpty();
    }

    private static boolean summonSwarm(ServerPlayer player) {
        ServerLevel level = player.level();
        int count = GLConfig.BAT_SWARM_COUNT.get();
        int life = Mth.ceil(GLConfig.BAT_SWARM_SECONDS.get() * 20.0);
        for (int i = 0; i < count; i++) {
            double a = i * Mth.TWO_PI / count;
            double r = 1.5 + player.getRandom().nextDouble() * 2.0;
            Vec3 at = player.position().add(Math.cos(a) * r, 0.5 + player.getRandom().nextDouble() * 2.5, Math.sin(a) * r);
            level.addFreshEntity(BatDefenderEntity.create(level, player, at, life + player.getRandom().nextInt(20), i));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BAT_SWARM.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.2, 1.0, 1.2, 0.04);
        player.displayClientMessage(Component.translatable("message.greenlantern.bat_swarm", count).withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    /** The bats scatter into the night. */
    public static void releaseSwarm(ServerPlayer player, boolean message) {
        List<BatDefenderEntity> bats = BatDefenderEntity.findAll(player);
        if (bats.isEmpty()) return;
        for (BatDefenderEntity bat : bats) bat.scatter();
        if (message) {
            player.displayClientMessage(Component.translatable("message.greenlantern.bat_swarm_end").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** Arrows, fireballs and tridents flying at Batman meet a bat on the way. */
    private static void defendFromProjectiles(ServerPlayer player) {
        List<BatDefenderEntity> bats = BatDefenderEntity.findAll(player);
        if (bats.isEmpty()) return;
        Vec3 center = player.position().add(0, 1.0, 0);
        List<Projectile> incoming = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(6.0), p -> {
            if (p.getOwner() == player || p.isRemoved() || p instanceof BatarangEntity || p instanceof GrappleHookEntity) return false;
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.04) return false;
            return center.subtract(p.position()).normalize().dot(v.normalize()) > 0.6;
        });
        for (Projectile projectile : incoming) {
            BatDefenderEntity bat = bats.stream().filter(b -> !b.isScattering())
                    .min(Comparator.comparingDouble(b -> b.distanceToSqr(projectile))).orElse(null);
            if (bat == null) return;
            bat.intercept(projectile);
        }
    }

    // ---- the Batmobile ---------------------------------------------------------------------------------

    private static boolean summonBatmobile(ServerPlayer player) {
        ServerLevel level = player.level();
        if (player.isPassenger()) player.stopRiding();
        BatmobileEntity car = BatmobileEntity.create(level, player);
        if (!BatmobileEntity.fits(level, car)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.batmobile_no_room").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        level.addFreshEntity(car);
        player.startRiding(car);
        car.playEngineStart();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, car.getX(), car.getY() + 0.5, car.getZ(), 50, 1.5, 0.4, 1.5, 0.05);
        player.displayClientMessage(Component.translatable("message.greenlantern.batmobile_summoned").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    // ---- power wheel / key packets -----------------------------------------------------------------------

}
