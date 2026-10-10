package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SupermanSyncPacket;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of Superman: soaking up sunlight, the suit's passive gifts and his powers (heat
 * vision, the super punch, super breath and X-ray vision). Flight is the shared power flight
 * (see {@code ring.FlightHandler} and {@code flight.FlightProfile}), faster and stronger for him.
 */
public final class SupermanServer {
    /** Duration of the suit's fire resistance, refreshed while it is worn (long enough not to flicker). */
    private static final int FIRE_RESISTANCE_TICKS = 300;

    private static final class State {
        float pendingSolar;
        float pendingCost;
        int heatTicks;
        int heatAge;
        int breathTicks;
        int breathAge;
        boolean xray;
        int lastSentFlags;
        boolean wasFull = true;
        final Set<Integer> glowing = new HashSet<>();
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private SupermanServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null) stopAll(player, state);
    }

    /** The suit is being taken off: every power stops. */
    public static void onSuitRemoved(ServerPlayer player) {
        remove(player);
        MobEffectInstance fire = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (fire != null && fire.getDuration() <= FIRE_RESISTANCE_TICKS && fire.isAmbient()) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
    }

    private static void stopAll(ServerPlayer player, State state) {
        state.heatTicks = 0;
        state.breathTicks = 0;
        state.xray = false;
        clearGlow(player, state);
        if (state.lastSentFlags != 0) {
            state.lastSentFlags = 0;
            ModNetwork.sendToTrackingAndSelf(player, new SupermanSyncPacket(player.getId(), (byte) 0, SuperFlags.EVENT_NONE));
        }
    }

    public static boolean isUsingHeatVision(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.heatTicks > 0;
    }

    public static boolean isBreathing(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.breathTicks > 0;
    }

    public static boolean isXray(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.xray;
    }

    /** Sends a one-shot event (the punch, suiting up) to the Superman and the players around him. */
    static void sendEvent(ServerPlayer player, int event) {
        State state = STATES.get(player.getUUID());
        int flags = state == null ? 0 : state.lastSentFlags;
        ModNetwork.sendToTrackingAndSelf(player, new SupermanSyncPacket(player.getId(), (byte) flags, event));
    }

    // ---- every server tick -------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = SupermanHelper.isSuited(player);
        if (player.tickCount % 20 == 0) SupermanHero.INSTANCE.updateSuitModifiers(player, suited);
        if (!suited) {
            if (STATES.containsKey(player.getUUID())) remove(player);
            return;
        }
        ItemStack crystal = SupermanHelper.findCrystal(player);
        if (crystal.isEmpty()) return;
        State state = get(player);
        ServerLevel level = player.level();

        // Invulnerable to fire and lava (as long as the suit is on).
        if (player.tickCount % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS, 0, true, false, true));
        }

        // The yellow sun recharges his cells (not while he is pouring energy out through his eyes or his breath).
        float sun = state.heatTicks > 0 || state.breathTicks > 0 ? 0.0F : sunlight(player);
        if (sun > 0.0F) {
            state.pendingSolar += GLConfig.SOLAR_REGEN.get() * sun / 20.0F;
            if (state.pendingSolar >= 1.0F) {
                int whole = (int) state.pendingSolar;
                state.pendingSolar -= whole;
                SupermanHero.SOLAR_ENERGY.add(crystal, whole);
            }
            if (player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
            if (player.tickCount % 8 == 0 && !SupermanHero.SOLAR_ENERGY.get(crystal).isFull()) {
                level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.35, 0.8, 0.35, 0.01);
            }
        }
        boolean full = SupermanHero.SOLAR_ENERGY.get(crystal).isFull();
        if (full && !state.wasFull) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SOLAR_CHARGED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.greenlantern.solar_full").withStyle(ChatFormatting.GOLD), true);
        }
        state.wasFull = full;
        float fraction = SupermanHero.SOLAR_ENERGY.get(crystal).fraction();
        if (!player.isCreative() && fraction <= 0.1F && player.tickCount % 100 == 0) {
            player.displayClientMessage(Component.translatable(fraction <= 0.0F ? "message.greenlantern.no_solar" : "message.greenlantern.low_solar")
                    .withStyle(ChatFormatting.GOLD), true);
        }

        if (state.heatTicks > 0) tickHeatVision(player, crystal, state, level);
        if (state.breathTicks > 0) tickBreath(player, crystal, state, level);
        if (state.xray) tickXray(player, crystal, state, level);

        int flags = (state.heatTicks > 0 ? SuperFlags.HEAT_VISION : 0) | (state.breathTicks > 0 ? SuperFlags.SUPER_BREATH : 0)
                | (state.xray ? SuperFlags.XRAY : 0);
        if (flags != state.lastSentFlags || (flags != 0 && player.tickCount % 10 == 0)) {
            state.lastSentFlags = flags;
            ModNetwork.sendToTrackingAndSelf(player, new SupermanSyncPacket(player.getId(), (byte) flags, SuperFlags.EVENT_NONE));
        }
    }

    /** How much sunlight reaches the player (0 in the dark, under a roof or out of the overworld's sky; 1.5 high up). */
    public static float sunlight(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling() || !level.isBrightOutside()) return 0.0F;
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.canSeeSky(head)) return 0.0F;
        float sun = level.isRainingAt(head) || level.isThundering() ? GLConfig.SOLAR_RAIN_FACTOR.get().floatValue() : 1.0F;
        return player.getY() > 150 ? sun * 1.5F : sun;
    }

    /** Pays a continuous power for one tick; false when the cells ran dry. */
    private static boolean payContinuous(ServerPlayer player, ItemStack crystal, State state, int costPerSecond) {
        if (player.isCreative()) return true;
        state.pendingCost += costPerSecond / 20.0F;
        if (state.pendingCost >= 1.0F) {
            int whole = (int) state.pendingCost;
            state.pendingCost -= whole;
            boolean enough = SupermanHero.SOLAR_ENERGY.has(crystal, whole);
            SupermanHero.SOLAR_ENERGY.drain(crystal, whole);
            if (!enough) {
                notifyNoEnergy(player);
                return false;
            }
        }
        return true;
    }

    public static void notifyNoEnergy(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_solar").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    // ---- powers --------------------------------------------------------------------------------------

    /** Uses a power (power key or right click with the crystal). Returns whether something happened. */
    public static boolean usePower(ServerPlayer player, ItemStack crystal, SuperPower power) {
        if (!SupermanHelper.isSuited(player) || player.isSpectator()) return false;
        State state = get(player);
        // Powers already in use are switched off for free.
        switch (power) {
            case HEAT_VISION -> {
                if (state.heatTicks > 0) {
                    endHeatVision(player, state);
                    return true;
                }
            }
            case SUPER_BREATH -> {
                if (state.breathTicks > 0) {
                    state.breathTicks = 0;
                    return true;
                }
            }
            case XRAY_VISION -> {
                if (state.xray) {
                    endXray(player, state);
                    return true;
                }
            }
            case SUPER_PUNCH -> {}
        }
        if (player.getCooldowns().isOnCooldown(crystal)) return false;
        if (!player.isCreative() && !SupermanHero.SOLAR_ENERGY.has(crystal, Math.max(1, power.cost()))) {
            notifyNoEnergy(player);
            return false;
        }
        ServerLevel level = player.level();
        switch (power) {
            case HEAT_VISION -> {
                state.breathTicks = 0;
                state.heatTicks = Mth.ceil(GLConfig.HEAT_VISION_SECONDS.get() * 20.0);
                state.heatAge = 0;
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.HEAT_VISION.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
            }
            case SUPER_PUNCH -> {
                superPunch(player, level);
                if (!player.isCreative()) SupermanHero.SOLAR_ENERGY.tryConsume(crystal, power.cost());
            }
            case SUPER_BREATH -> {
                if (state.heatTicks > 0) endHeatVision(player, state);
                state.breathTicks = Mth.ceil(GLConfig.SUPER_BREATH_SECONDS.get() * 20.0);
                state.breathAge = 0;
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SUPER_BREATH.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
            }
            case XRAY_VISION -> {
                state.xray = true;
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.XRAY_ON.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                player.displayClientMessage(Component.translatable("message.greenlantern.xray_on").withStyle(ChatFormatting.AQUA), true);
            }
        }
        player.getCooldowns().addCooldown(crystal, power == SuperPower.SUPER_PUNCH ? 16 : 6);
        return true;
    }

    // ---- heat vision -----------------------------------------------------------------------------------

    /** Where Superman's eyes are looking: the first creature or block on the line of sight. */
    public static HitResult aim(ServerLevel level, ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != player && !e.isSpectator() && e.isPickable() && !player.isPassengerOfSameVehicle(e), 0.3F);
        return entity != null ? entity : block;
    }

    private static void tickHeatVision(ServerPlayer player, ItemStack crystal, State state, ServerLevel level) {
        if (!payContinuous(player, crystal, state, GLConfig.HEAT_VISION_COST_PER_SECOND.get())) {
            endHeatVision(player, state);
            return;
        }
        state.heatTicks--;
        state.heatAge++;
        HitResult hit = aim(level, player, GLConfig.HEAT_VISION_RANGE.get());
        Vec3 end = hit.getLocation();
        float damage = GLConfig.HEAT_VISION_DAMAGE.get().floatValue();
        if (state.heatAge % 4 == 1) {
            // The target, and anything right next to the point the beams melt.
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(end, end).inflate(1.0),
                    e -> e != player && e.isAlive() && !e.isSpectator())) {
                boolean direct = hit instanceof EntityHitResult eh && eh.getEntity() == target;
                target.invulnerableTime = 0;
                if (target.hurtServer(level, ModDamageTypes.heatVision(level, player), direct ? damage : damage * 0.5F)) {
                    target.igniteForSeconds(4.0F);
                }
            }
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            if (GLConfig.HEAT_VISION_IGNITES.get() && state.heatAge % 8 == 0) scorch(player, level, blockHit);
            if (state.heatAge % 2 == 0) {
                BlockState hitState = level.getBlockState(blockHit.getBlockPos());
                if (hitState.getRenderShape() != RenderShape.INVISIBLE) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, hitState), end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.15);
                }
            }
        }
        if (state.heatAge % 2 == 0) {
            level.sendParticles(ModParticles.HEAT_SPARK.get(), end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.25);
        }
        if (state.heatAge % 5 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.15, 0.1, 0.15, 0.02);
        }
        if (state.heatAge % 30 == 0) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.HEAT_VISION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (state.heatTicks <= 0) endHeatVision(player, state);
    }

    private static void endHeatVision(ServerPlayer player, State state) {
        state.heatTicks = 0;
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.HEAT_VISION_END.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    /** The beams set fire to what they touch and melt ice and snow. */
    private static void scorch(ServerPlayer player, ServerLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.ICE) && !state.is(Blocks.PACKED_ICE) && !state.is(Blocks.BLUE_ICE)) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
            return;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
            return;
        }
        if (state.is(Blocks.TNT)) {
            // Lit, like with a flint and steel.
            if (net.minecraft.world.level.block.TntBlock.prime(level, pos)) level.removeBlock(pos, false);
            return;
        }
        BlockPos firePos = pos.relative(hit.getDirection());
        if (level.getBlockState(firePos).isAir() && BaseFireBlock.canBePlacedAt(level, firePos, hit.getDirection())) {
            level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
        }
    }

    // ---- the super punch ------------------------------------------------------------------------------

    private static void superPunch(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        // The blast centers where the fist lands: on the creature or block right ahead, or a couple of blocks away.
        HitResult hit = aim(level, player, 4.0);
        Vec3 center = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : player.getEyePosition().add(look.scale(2.5));
        double radius = GLConfig.SUPER_PUNCH_RADIUS.get();
        // A punch thrown at full flying speed hits even harder.
        float speedBonus = 1.0F + ServerFlightTracker.speedFraction(player);
        float damage = GLConfig.SUPER_PUNCH_DAMAGE.get().floatValue() * speedBonus;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            double dist = target.position().add(0, target.getBbHeight() * 0.5, 0).distanceTo(center);
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.65 * dist / radius);
            target.invulnerableTime = 0;
            target.hurtServer(level, ModDamageTypes.superPunch(level, player), damage * falloff);
            Vec3 away = target.position().subtract(player.position());
            away = new Vec3(away.x, 0, away.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0, look.z) : away.normalize();
            double strength = 2.4 * falloff * speedBonus;
            target.push(away.x * strength, 0.55 * falloff + 0.25, away.z * strength);
            target.hurtMarked = true;
        }
        // Projectiles in the way are swatted aside.
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(radius))) {
            if (projectile.getOwner() == player) continue;
            Vec3 away = projectile.position().subtract(center).normalize();
            projectile.setDeltaMovement(away.scale(1.5));
            projectile.hurtMarked = true;
        }
        if (GLConfig.SUPER_PUNCH_BREAKS_BLOCKS.get()) smash(player, level, center);

        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, center.x, center.y, center.z, ModSounds.SUPER_PUNCH.get(), SoundSource.PLAYERS, 2.5F, 0.95F + level.random.nextFloat() * 0.1F);
        double groundY = groundBelow(level, center);
        level.sendParticles(ModParticles.SUPER_SHOCKWAVE.get(), center.x, groundY + 0.1, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SUPER_RING.get(), center.x, center.y, center.z, 0, look.x, look.y, look.z, 1.0);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 0.6, 0.4, 0.6, 0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, groundY + 0.3, center.z, 30, radius * 0.35, 0.2, radius * 0.35, 0.12);
        dust(level, new Vec3(center.x, groundY, center.z), radius, 40);
        sendEvent(player, SuperFlags.EVENT_PUNCH);
    }

    /** Smashes the soft blocks (dirt, sand, leaves, glass...) around where the fist lands. */
    private static void smash(ServerPlayer player, ServerLevel level, Vec3 center) {
        BlockPos c = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 2, 2))) {
            if (pos.distToCenterSqr(center) > 6.5) continue;
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0 || hardness > 1.0F || !state.getFluidState().isEmpty()) continue;
            level.destroyBlock(pos.immutable(), true, player);
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
            BlockPos pos = BlockPos.containing(x, center.y - 0.5, z);
            BlockState state = level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 3, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }

    // ---- super breath ----------------------------------------------------------------------------------

    private static void tickBreath(ServerPlayer player, ItemStack crystal, State state, ServerLevel level) {
        if (!payContinuous(player, crystal, state, GLConfig.SUPER_BREATH_COST_PER_SECOND.get())) {
            state.breathTicks = 0;
            return;
        }
        state.breathTicks--;
        state.breathAge++;
        double range = GLConfig.SUPER_BREATH_RANGE.get();
        Vec3 look = player.getLookAngle();
        Vec3 mouth = player.getEyePosition().add(0, -0.2, 0).add(look.scale(0.4));
        double cos = Math.cos(Math.toRadians(28));

        // The gale: shoves, chills and (now and then) hurts everything in the cone.
        for (Entity target : level.getEntities(player, new AABB(mouth, mouth).inflate(range), e -> e.isAlive() && !e.isSpectator())) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
            double dist = to.length();
            if (dist > range || dist < 1.0E-3 || to.normalize().dot(look) < cos) continue;
            float falloff = (float) (1.0 - 0.6 * dist / range);
            if (target instanceof Projectile projectile) {
                projectile.setDeltaMovement(look.scale(1.2));
                projectile.hurtMarked = true;
                continue;
            }
            Vec3 push = look.scale(0.32 * falloff);
            Vec3 v = target.getDeltaMovement().add(push.x, push.y * 0.5 + 0.03, push.z);
            double max = 1.6;
            if (v.lengthSqr() > max * max) v = v.normalize().scale(max);
            target.setDeltaMovement(v);
            target.hurtMarked = true;
            if (target.isOnFire()) target.clearFire();
            if (target instanceof LivingEntity living) {
                living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze() + 120, living.getTicksFrozen() + 6));
                if (state.breathAge % 5 == 1) {
                    living.invulnerableTime = 0;
                    living.hurtServer(level, ModDamageTypes.superBreath(level, player), GLConfig.SUPER_BREATH_DAMAGE.get().floatValue() * falloff);
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), player);
                }
            }
        }

        // Freezing: puts out fires, turns water to ice and lava to obsidian.
        if (GLConfig.SUPER_BREATH_FREEZES.get() && state.breathAge % 3 == 0) freeze(level, mouth, look, range);

        // The icy cloud.
        for (int i = 0; i < 6; i++) {
            Vec3 spread = look.add(level.random.nextGaussian() * 0.16, level.random.nextGaussian() * 0.16, level.random.nextGaussian() * 0.16).normalize();
            double speed = 0.8 + level.random.nextDouble() * 0.6;
            level.sendParticles(ModParticles.FROST_BREATH.get(), mouth.x, mouth.y, mouth.z, 0, spread.x, spread.y, spread.z, speed);
        }
        if (state.breathAge % 2 == 0) {
            Vec3 spread = look.add(level.random.nextGaussian() * 0.2, level.random.nextGaussian() * 0.2, level.random.nextGaussian() * 0.2).normalize();
            level.sendParticles(ParticleTypes.SNOWFLAKE, mouth.x, mouth.y, mouth.z, 0, spread.x, spread.y, spread.z, 0.9);
        }
        if (state.breathAge % 40 == 0) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SUPER_BREATH.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        }
    }

    private static void freeze(ServerLevel level, Vec3 mouth, Vec3 look, double range) {
        for (int i = 0; i < 10; i++) {
            Vec3 dir = look.add(level.random.nextGaussian() * 0.25, level.random.nextGaussian() * 0.25, level.random.nextGaussian() * 0.25).normalize();
            BlockHitResult hit = level.clip(new ClipContext(mouth, mouth.add(dir.scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS) continue;
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
            } else if (state.getFluidState().is(Fluids.WATER) && state.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir()) {
                // Frosted ice, like frost walker's: it melts back on its own.
                level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(level.random, 60, 120));
            } else if (state.getFluidState().is(Fluids.LAVA) && state.is(Blocks.LAVA)) {
                level.setBlockAndUpdate(pos, state.getFluidState().isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
                level.levelEvent(1501, pos, 0);
            }
            BlockPos firePos = pos.relative(hit.getDirection());
            if (level.getBlockState(firePos).is(BlockTags.FIRE)) level.removeBlock(firePos, false);
        }
    }

    // ---- X-ray vision ----------------------------------------------------------------------------------

    private static void tickXray(ServerPlayer player, ItemStack crystal, State state, ServerLevel level) {
        if (!payContinuous(player, crystal, state, GLConfig.XRAY_COST_PER_SECOND.get())) {
            endXray(player, state);
            return;
        }
        if (player.tickCount % 10 != 0) return;
        // Creatures glow for Superman alone: their outline shows through walls (the client draws ores and chests).
        double range = GLConfig.XRAY_RADIUS.get() * 2.0;
        List<LivingEntity> seen = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(player) < range * range);
        Set<Integer> now = new HashSet<>();
        for (LivingEntity entity : seen) {
            now.add(entity.getId());
            sendFlags(player, entity, true);
        }
        for (Integer id : state.glowing) {
            if (now.contains(id)) continue;
            Entity entity = level.getEntity(id);
            if (entity != null) sendFlags(player, entity, false);
        }
        state.glowing.clear();
        state.glowing.addAll(now);
    }

    private static void endXray(ServerPlayer player, State state) {
        state.xray = false;
        clearGlow(player, state);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.XRAY_OFF.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private static void clearGlow(ServerPlayer player, State state) {
        for (Integer id : state.glowing) {
            Entity entity = player.level().getEntity(id);
            if (entity != null) sendFlags(player, entity, false);
        }
        state.glowing.clear();
    }

    /** Tells only this player that the entity glows (or what its real flags are). */
    private static void sendFlags(ServerPlayer player, Entity entity, boolean glow) {
        if (player.connection == null) return;
        byte real = entity.getEntityData().get(GlowAccess.sharedFlags());
        byte value = glow ? (byte) (real | GlowAccess.GLOWING_BIT) : real;
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), List.of(SynchedEntityData.DataValue.create(GlowAccess.sharedFlags(), value))));
    }

    // ---- power wheel / key packets -----------------------------------------------------------------------

}
