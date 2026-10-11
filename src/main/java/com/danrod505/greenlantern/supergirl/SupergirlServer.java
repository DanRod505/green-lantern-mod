package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * The server side of Supergirl (see {@code docs/heroes/supergirl.md}): what each power does, the
 * solar energy (the sun, a little in the shade, and every punch that lands), what the powers leave
 * running (bolts in the air, the dash, the held and thrown things, the walls of ice, the Solar Flare
 * and the weakness after it) and Krypto coming and going.
 */
public final class SupergirlServer {
    private static final int GOLD = 0xFFD447;
    private static final int RED = 0xD8202E;
    private static final int ICE = 0xBFEFFF;
    private static final ColorParticleOption FLASH = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFE8A0);
    private static final double BOLT_SPEED = 2.6;
    private static final int DASH_TICKS = 6;
    private static final int FLARE_TICKS = 12;
    /** Ticks a thrown creature stays a projectile. */
    private static final int THROWN_TICKS = 60;
    /** Ticks Krypto takes at most to come down from the sky. */
    static final int KRYPTO_ARRIVAL_TICKS = 80;

    /** A heat bolt in flight (only particles until it hits). */
    private static final class Bolt {
        Vec3 pos;
        final Vec3 velocity;
        int age;

        Bolt(Vec3 pos, Vec3 velocity) {
            this.pos = pos;
            this.velocity = velocity;
        }
    }

    /** A creature she threw: it hurts whatever it crashes into. */
    private static final class Thrown {
        final int id;
        int age;

        Thrown(int id) {
            this.id = id;
        }
    }

    private static final class State {
        final List<Bolt> bolts = new ArrayList<>();
        int dashTicks;
        Vec3 dashStep = Vec3.ZERO;
        final Set<Integer> dashHit = new HashSet<>();
        boolean hearing;
        float pendingHearingCost;
        /** Entity id of what she holds over her head (-1: nothing). */
        int held = -1;
        final List<Thrown> thrown = new ArrayList<>();
        long weakUntil;
        int flareTicks;
        float flarePower;
        long dodgeUntil;
        UUID krypto;
        float pendingSolar;
        boolean wasFull = true;
    }

    /** A wall of ice that melts at {@code meltAt}. */
    private record Wall(ResourceKey<Level> level, List<BlockPos> blocks, long meltAt) {}

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final List<Wall> WALLS = new ArrayList<>();

    private SupergirlServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** The suit comes off or she leaves: what she holds drops, hearing stops and Krypto flies off (remembered on the pendant). */
    public static void remove(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (!item.isEmpty()) item.remove(SupergirlContent.HEARING.get());
        if (state == null) return;
        release(player.level(), state);
        KryptoEntity krypto = krypto(player, state);
        if (krypto != null) {
            if (!item.isEmpty()) remember(item, krypto, 0L);
            krypto.flyAway(false);
        }
    }

    // ---- queries (also used by the tests and the client) ------------------------------------------------

    /** Whether Super Hearing is on (read from the pendant, so the client sees it too). */
    public static boolean isHearing(ItemStack item) {
        return Boolean.TRUE.equals(item.get(SupergirlContent.HEARING.get()));
    }

    /** Whether she is worn out after a Solar Flare (no powers, flight or recharge). */
    public static boolean isWeak(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.weakUntil;
    }

    /** Whether a barrel roll keeps projectiles off her right now. */
    public static boolean isDodging(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.dodgeUntil;
    }

    public static int boltsInFlight(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.bolts.size();
    }

    /** What she holds over her head, or null. */
    public static Entity held(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null || state.held < 0 ? null : player.level().getEntity(state.held);
    }

    /** Her Krypto, if he is with her in this world. */
    public static KryptoEntity krypto(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? null : krypto(player, state);
    }

    private static KryptoEntity krypto(ServerPlayer player, State state) {
        if (state.krypto == null) return null;
        return player.level().getEntity(state.krypto) instanceof KryptoEntity krypto && krypto.isAlive() ? krypto : null;
    }

    /** Whether this Krypto belongs with her right now (otherwise he flies off). */
    static boolean isHerKrypto(ServerPlayer owner, KryptoEntity krypto) {
        if (owner == null || owner.isRemoved() || owner.level() != krypto.level() || !SupergirlHero.INSTANCE.isSuited(owner)) return false;
        State state = STATES.get(owner.getUUID());
        return state != null && krypto.getUUID().equals(state.krypto);
    }

    // ---- powers ------------------------------------------------------------------------------------------

    /** Uses a power with the Argo Pendant; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, SupergirlPower power) {
        if (!SupergirlHero.INSTANCE.isSuited(player) || player.isSpectator()) return false;
        State state = get(player);
        ServerLevel level = player.level();
        // Sneaking, she whistles for Krypto instead (free).
        if (player.isShiftKeyDown()) {
            whistle(player, item, state);
            return true;
        }
        if (level.getGameTime() < state.weakUntil) {
            player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_weak").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        // Switching hearing off and throwing what she holds are free.
        if (power == SupergirlPower.SUPER_HEARING && isHearing(item)) {
            stopHearing(player, item, state);
            return true;
        }
        if (power == SupergirlPower.KRYPTONIAN_THROW && state.held >= 0) {
            hurl(player, level, state);
            return true;
        }
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !SupergirlHero.ENERGY.tryConsume(item, power.cost())) {
            notifyNoEnergy(player);
            return false;
        }
        int cooldown = switch (power) {
            case HEAT_BOLTS -> {
                heatBolt(player, level, state);
                yield 5;
            }
            case METEOR_DASH -> {
                startDash(player, level, state);
                yield 20;
            }
            case THUNDER_CLAP -> {
                thunderClap(player, level);
                yield 24;
            }
            case FROST_WALL -> {
                frostWall(player, level);
                yield 30;
            }
            case SUPER_HEARING -> {
                item.set(SupergirlContent.HEARING.get(), true);
                state.hearing = true;
                state.pendingHearingCost = 0;
                hear(player, SupergirlContent.HEARING_ON.get(), 0.9F, 1.0F);
                player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_hearing_on").withStyle(ChatFormatting.GOLD), true);
                yield 6;
            }
            case KRYPTONIAN_THROW -> {
                if (!grab(player, level, state)) {
                    // Nothing to grab: the energy comes back.
                    if (!player.isCreative()) SupergirlHero.ENERGY.add(item, power.cost());
                    player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_throw_nothing").withStyle(ChatFormatting.GRAY), true);
                }
                yield 8;
            }
            case SOLAR_FLARE -> {
                int rest = SupergirlHero.ENERGY.get(item).stored();
                SupergirlHero.ENERGY.drain(item, rest);
                solarFlare(player, level, state, power.cost() + rest);
                yield 40;
            }
        };
        player.getCooldowns().addCooldown(item, cooldown);
        return true;
    }

    private static void notifyNoEnergy(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_supergirl_solar").withStyle(ChatFormatting.RED), true);
    }

    private static void play(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A sound only she hears. */
    private static void hear(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (player.connection == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }

    private static Vec3 horizontal(Entity player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** Living things her powers treat as enemies (never players, her Krypto or anyone's tamed animals). */
    static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof Player || entity instanceof ArmorStand) return false;
        if (entity instanceof KryptoEntity) return false;
        if (entity instanceof OwnableEntity owned && owned.getOwnerReference() != null) return false;
        return entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
    }

    /** Whatever her powers may hit: enemies and any other creature that isn't a friend. */
    private static boolean hittable(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof Player || entity instanceof ArmorStand) return false;
        if (entity instanceof KryptoEntity) return false;
        return !(entity instanceof OwnableEntity owned && owned.getOwnerReference() != null);
    }

    /** The first creature or block in her line of sight. */
    private static HitResult aim(ServerLevel level, ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != player && !e.isSpectator() && e.isPickable() && !player.isPassengerOfSameVehicle(e), 0.3F);
        return entity != null ? entity : block;
    }

    // ---- heat bolts --------------------------------------------------------------------------------------

    private static void heatBolt(ServerPlayer player, ServerLevel level, State state) {
        Vec3 look = player.getLookAngle();
        Vec3 eyes = player.getEyePosition().add(look.scale(0.4));
        state.bolts.add(new Bolt(eyes, look.scale(BOLT_SPEED)));
        Vec3 right = horizontal(player).cross(new Vec3(0, 1, 0)).normalize().scale(0.1);
        for (Vec3 eye : new Vec3[] {eyes.add(right), eyes.subtract(right)}) {
            level.sendParticles(new DustParticleOptions(GOLD, 0.9F), eye.x, eye.y, eye.z, 3, 0.02, 0.02, 0.02, 0.0);
        }
        play(level, player, SupergirlContent.HEAT_BOLT.get(), 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    private static void tickBolts(ServerPlayer player, ServerLevel level, State state) {
        int life = Mth.ceil(SupergirlConfig.HEAT_BOLT_RANGE.get() / BOLT_SPEED);
        Iterator<Bolt> it = state.bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            bolt.age++;
            Vec3 next = bolt.pos.add(bolt.velocity);
            BlockHitResult block = level.clip(new ClipContext(bolt.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, bolt.pos, end, new AABB(bolt.pos, end).inflate(0.6),
                    e -> e instanceof LivingEntity living && hittable(player, living), 0.3F);
            // The streak: a hot red core with golden edges.
            Vec3 step = end.subtract(bolt.pos);
            for (int i = 0; i < 5; i++) {
                Vec3 p = bolt.pos.add(step.scale(i / 5.0));
                level.sendParticles(new DustParticleOptions(i % 2 == 0 ? RED : GOLD, 1.0F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (entity != null && entity.getEntity() instanceof LivingEntity target) {
                Vec3 at = entity.getLocation();
                target.invulnerableTime = 0;
                target.hurtServer(level, ModDamageTypes.heatVision(level, player), SupergirlConfig.HEAT_BOLT_DAMAGE.get().floatValue());
                target.igniteForSeconds(3.0F);
                burst(level, at);
                it.remove();
                continue;
            }
            if (block.getType() != HitResult.Type.MISS) {
                burst(level, block.getLocation());
                if (SupergirlConfig.HEAT_BOLT_IGNITES.get()) ignite(level, player, block);
                it.remove();
                continue;
            }
            bolt.pos = next;
            if (bolt.age >= life) it.remove();
        }
    }

    /** The small burst where a bolt lands. */
    private static void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(ModParticles.HEAT_SPARK.get(), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.03);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, at.x, at.y, at.z, SupergirlContent.HEAT_HIT.get(), SoundSource.PLAYERS, 0.9F, 0.9F + level.random.nextFloat() * 0.2F);
    }

    /** Fire on the face the bolt hit, if there's room for it (no block is ever broken). */
    private static void ignite(ServerLevel level, ServerPlayer player, BlockHitResult hit) {
        BlockPos at = hit.getBlockPos().relative(hit.getDirection());
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.ICE) && !state.is(Blocks.BLUE_ICE) && !state.is(Blocks.PACKED_ICE)) {
            level.destroyBlock(hit.getBlockPos(), false, player);
            return;
        }
        if (level.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(level, at, hit.getDirection())) {
            level.setBlock(at, BaseFireBlock.getState(level, at), 11);
        }
    }

    // ---- meteor dash -------------------------------------------------------------------------------------

    private static void startDash(ServerPlayer player, ServerLevel level, State state) {
        Vec3 look = player.getLookAngle();
        // On the ground she dashes level, so she never digs her own way down.
        Vec3 dir = player.onGround() && look.y < 0 ? horizontal(player) : look.normalize();
        state.dashStep = dir.scale(SupergirlConfig.DASH_DISTANCE.get() / DASH_TICKS);
        state.dashTicks = DASH_TICKS;
        state.dashHit.clear();
        level.sendParticles(FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        play(level, player, SupergirlContent.METEOR_DASH.get(), 1.4F, 1.0F);
    }

    private static void tickDash(ServerPlayer player, ServerLevel level, State state) {
        state.dashTicks--;
        Vec3 from = player.position();
        Vec3 to = from;
        // As far as she fits, in small steps.
        int steps = Math.max(1, (int) Math.ceil(state.dashStep.length() / 0.4));
        Vec3 small = state.dashStep.scale(1.0 / steps);
        boolean blocked = false;
        for (int i = 0; i < steps; i++) {
            Vec3 next = to.add(small);
            if (!level.noCollision(player, player.getBoundingBox().move(next.subtract(from)))) {
                blocked = true;
                break;
            }
            to = next;
        }
        if (to.distanceToSqr(from) > 1.0E-4) {
            player.teleportTo(to.x, to.y, to.z);
            player.resetFallDistance();
        }
        // Everything in the way is rammed and flung aside.
        AABB swept = player.getBoundingBox().move(to.subtract(player.position())).expandTowards(from.subtract(to)).inflate(0.8);
        Vec3 dir = state.dashStep.normalize();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept, e -> hittable(player, e) && !state.dashHit.contains(e.getId()))) {
            state.dashHit.add(target.getId());
            target.invulnerableTime = 0;
            target.hurtServer(level, ModDamageTypes.superPunch(level, player), SupergirlConfig.DASH_DAMAGE.get().floatValue());
            Vec3 side = target.position().subtract(to);
            side = new Vec3(side.x, 0, side.z);
            side = side.lengthSqr() < 1.0E-4 ? Vec3.ZERO : side.normalize();
            target.push(dir.x * 1.4 + side.x * 0.6, 0.55, dir.z * 1.4 + side.z * 0.6);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
        }
        // The comet's tail.
        Vec3 step = to.subtract(from);
        for (int i = 0; i <= 6; i++) {
            Vec3 p = from.add(step.scale(i / 6.0)).add(0, 1.0, 0);
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? RED : GOLD, 1.6F), p.x, p.y, p.z, 2, 0.15, 0.2, 0.15, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        if (blocked || state.dashTicks <= 0) {
            state.dashTicks = 0;
            level.sendParticles(ModParticles.SUPER_RING.get(), to.x, to.y + 1.0, to.z, 0, dir.x, dir.y, dir.z, 1.0);
        }
    }

    // ---- thunder clap ------------------------------------------------------------------------------------

    private static void thunderClap(ServerPlayer player, ServerLevel level) {
        double range = SupergirlConfig.CLAP_RANGE.get();
        float damage = SupergirlConfig.CLAP_DAMAGE.get().floatValue();
        int stun = Mth.ceil(SupergirlConfig.CLAP_STUN_SECONDS.get() * 20.0);
        Vec3 look = player.getLookAngle();
        Vec3 hands = player.getEyePosition().add(0, -0.4, 0).add(look.scale(0.5));
        double cos = Math.cos(Math.toRadians(28));
        int r = (int) Math.ceil(range);
        BlockPos center = BlockPos.containing(hands);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(hands);
            double dist = to.length();
            if (dist > range || dist < 0.5 || to.normalize().dot(look) < cos) continue;
            BlockState state = level.getBlockState(pos);
            if (isGlass(state)) {
                level.destroyBlock(pos.immutable(), false, player);
            } else if (state.getBlock() instanceof TorchBlock || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
                level.destroyBlock(pos.immutable(), true, player);
            } else if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
            }
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(hands, hands).inflate(range), e -> hittable(player, e))) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(hands);
            double dist = to.length();
            if (dist > range || dist < 1.0E-3 || to.normalize().dot(look) < cos) continue;
            float falloff = (float) (1.0 - 0.5 * dist / range);
            target.invulnerableTime = 0;
            target.hurtServer(level, level.damageSources().sonicBoom(player), damage * falloff);
            target.push(look.x * 0.9 * falloff, 0.25, look.z * 0.9 * falloff);
            target.hurtMarked = true;
            if (target instanceof Mob mob && isEnemy(player, mob)) stun(mob, player, stun);
        }
        // The shockwave: rings of white and gold racing down the cone.
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(look).normalize();
        for (double d = 0.8; d <= range; d += 1.2) {
            Vec3 at = hands.add(look.scale(d));
            double ring = 0.2 + d * Math.tan(Math.toRadians(28)) * 0.8;
            int points = 10 + (int) (d * 2.5);
            for (int k = 0; k < points; k++) {
                double a = k * Mth.TWO_PI / points;
                Vec3 p = at.add(side.scale(Math.cos(a) * ring)).add(up.scale(Math.sin(a) * ring));
                level.sendParticles(new DustParticleOptions(k % 3 == 0 ? GOLD : 0xFFFFFF, 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, hands.x + look.x * 2, hands.y + look.y * 2, hands.z + look.z * 2, 1, 0, 0, 0, 0);
        level.sendParticles(FLASH, hands.x, hands.y, hands.z, 1, 0, 0, 0, 0);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.swing(InteractionHand.OFF_HAND, true);
        play(level, player, SupergirlContent.THUNDER_CLAP.get(), 2.0F, 1.0F);
    }

    static boolean isGlass(BlockState state) {
        return state.is(BlockTags.IMPERMEABLE) || state.is(Blocks.GLASS_PANE)
                || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
    }

    private static void stun(Mob mob, ServerPlayer player, int ticks) {
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, true), player);
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, true), player);
        mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0, false, true), player);
    }

    // ---- frost wall --------------------------------------------------------------------------------------

    private static void frostWall(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 mouth = player.getEyePosition().add(0, -0.15, 0).add(look.scale(0.4));
        double range = 7.0;
        double cos = Math.cos(Math.toRadians(25));
        int r = (int) Math.ceil(range);
        BlockPos center = BlockPos.containing(mouth);
        // The freezing breath: water freezes, fire goes out.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(mouth);
            double dist = to.length();
            if (dist > range || dist < 0.5 || to.normalize().dot(look) < cos) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            } else if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                level.levelEvent(null, 1009, pos, 0);
            } else if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
            }
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(mouth, mouth).inflate(range), e -> e != player && e.isAlive())) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
            if (to.length() > range || to.normalize().dot(look) < cos) continue;
            target.clearFire();
            if (!hittable(player, target)) continue;
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 200));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2), player);
            target.invulnerableTime = 0;
            target.hurtServer(level, ModDamageTypes.superBreath(level, player), 2.0F);
        }
        player.clearFire();
        // The wall: ice rising across her path a few blocks ahead.
        Vec3 forward = horizontal(player);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 base = player.position().add(forward.scale(3.5));
        int width = SupergirlConfig.FROST_WALL_WIDTH.get();
        int height = SupergirlConfig.FROST_WALL_HEIGHT.get();
        List<BlockPos> placed = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (int w = 0; w < width; w++) {
            Vec3 column = base.add(right.scale(w - (width - 1) * 0.5));
            BlockPos foot = groundUnder(level, BlockPos.containing(column.x, player.getY() + 0.5, column.z));
            for (int h = 0; h < height; h++) {
                BlockPos pos = foot.above(h);
                if (!seen.add(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (!state.canBeReplaced() || !state.getFluidState().isEmpty() && !state.getFluidState().isSource()) continue;
                if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
                level.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                placed.add(pos.immutable());
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            }
        }
        if (!placed.isEmpty()) {
            synchronized (WALLS) {
                WALLS.add(new Wall(level.dimension(), placed, level.getGameTime() + Mth.ceil(SupergirlConfig.FROST_WALL_SECONDS.get() * 20.0)));
            }
        }
        for (double d = 0.5; d <= range; d += 0.7) {
            Vec3 at = mouth.add(look.scale(d));
            double spread = d * 0.25;
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 4, spread, spread, spread, 0.02);
            level.sendParticles(new DustParticleOptions(ICE, 1.2F), at.x, at.y, at.z, 2, spread, spread, spread, 0.0);
            if (((int) (d * 10)) % 3 == 0) level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1, spread * 0.5, spread * 0.5, spread * 0.5, 0.01);
        }
        play(level, player, SupergirlContent.FROST_BREATH.get(), 1.4F, 1.0F);
    }

    /** The first spot with ground under it, up to 3 blocks down (or the spot itself in mid air). */
    private static BlockPos groundUnder(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            BlockPos below = pos.below(i + 1);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return pos.below(i);
        }
        return pos;
    }

    /** Every server tick: walls whose time is up melt. */
    public static void tickWalls(MinecraftServer server) {
        synchronized (WALLS) {
            if (WALLS.isEmpty()) return;
            Iterator<Wall> it = WALLS.iterator();
            while (it.hasNext()) {
                Wall wall = it.next();
                ServerLevel level = server.getLevel(wall.level());
                if (level == null) {
                    it.remove();
                    continue;
                }
                if (level.getGameTime() < wall.meltAt()) continue;
                melt(level, wall);
                it.remove();
            }
        }
    }

    /** The server stops: the walls melt now, so no ice is left behind for good. */
    public static void meltAll(MinecraftServer server) {
        synchronized (WALLS) {
            for (Wall wall : WALLS) {
                ServerLevel level = server.getLevel(wall.level());
                if (level != null) melt(level, wall);
            }
            WALLS.clear();
        }
    }

    private static void melt(ServerLevel level, Wall wall) {
        boolean sound = true;
        for (BlockPos pos : wall.blocks()) {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.PACKED_ICE)) continue;
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            if (sound) {
                level.playSound(null, pos, SupergirlContent.FROST_MELT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                sound = false;
            }
        }
    }

    /** Ice blocks of walls that haven't melted yet (for the tests). */
    public static int wallBlocks() {
        synchronized (WALLS) {
            return WALLS.stream().mapToInt(w -> w.blocks().size()).sum();
        }
    }

    // ---- super hearing -----------------------------------------------------------------------------------

    private static void tickHearing(ServerPlayer player, ServerLevel level, ItemStack item, State state) {
        if (!player.isCreative()) {
            state.pendingHearingCost += SupergirlConfig.SUPER_HEARING_COST.get() / 20.0F;
            if (state.pendingHearingCost >= 1.0F) {
                int whole = (int) state.pendingHearingCost;
                state.pendingHearingCost -= whole;
                if (!SupergirlHero.ENERGY.has(item, whole)) {
                    SupergirlHero.ENERGY.drain(item, whole);
                    notifyNoEnergy(player);
                    stopHearing(player, item, state);
                    return;
                }
                SupergirlHero.ENERGY.drain(item, whole);
            }
        }
        if (player.tickCount % 60 != 0) return;
        hear(player, SupergirlContent.HEARING_PULSE.get(), 0.5F, 1.0F);
        // Krypto smells what she hears and barks toward the closest enemy.
        KryptoEntity krypto = krypto(player, state);
        if (krypto == null) return;
        double radius = SupergirlConfig.HEARING_RADIUS.get();
        LivingEntity nearest = null;
        double best = radius * radius;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> isEnemy(player, e))) {
            double d = e.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        if (nearest != null) krypto.alert(nearest);
    }

    private static void stopHearing(ServerPlayer player, ItemStack item, State state) {
        item.remove(SupergirlContent.HEARING.get());
        state.hearing = false;
        hear(player, SupergirlContent.HEARING_OFF.get(), 0.8F, 1.0F);
    }

    // ---- kryptonian throw --------------------------------------------------------------------------------

    /** Grabs the creature or loose block in sight; false when there is nothing she can lift. */
    private static boolean grab(ServerPlayer player, ServerLevel level, State state) {
        HitResult hit = aim(level, player, SupergirlConfig.THROW_RANGE.get());
        Entity grabbed = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && hittable(player, living)
                && !(living instanceof WitherBoss) && !(living instanceof EnderDragon) && living.getBbWidth() <= 3.0F && living.getBbHeight() <= 4.0F) {
            grabbed = living;
            if (living instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        } else if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState block0 = level.getBlockState(pos);
            float hardness = block0.getDestroySpeed(level, pos);
            if (hardness >= 0 && hardness <= 5.0F && !block0.hasBlockEntity() && block0.getFluidState().isEmpty() && !block0.isAir()
                    && player.mayInteract(level, pos)) {
                FallingBlockEntity block = FallingBlockEntity.fall(level, pos, block0);
                block.setNoGravity(true);
                block.time = 1;
                grabbed = block;
            }
        }
        if (grabbed == null) return false;
        state.held = grabbed.getId();
        holdOverhead(player, grabbed);
        level.sendParticles(ParticleTypes.CRIT, grabbed.getX(), grabbed.getY() + 0.5, grabbed.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        play(level, player, SupergirlContent.THROW_GRAB.get(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_throw_ready").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static void holdOverhead(ServerPlayer player, Entity held) {
        Vec3 at = player.position().add(horizontal(player).scale(0.2)).add(0, player.getBbHeight() + 0.3, 0);
        held.teleportTo(at.x, at.y, at.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.resetFallDistance();
        if (held instanceof FallingBlockEntity block) block.time = 1;
    }

    private static void tickHeld(ServerPlayer player, State state) {
        Entity held = player.level().getEntity(state.held);
        if (held == null || !held.isAlive()) {
            state.held = -1;
            return;
        }
        holdOverhead(player, held);
        if (held instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static void hurl(ServerPlayer player, ServerLevel level, State state) {
        Entity held = level.getEntity(state.held);
        state.held = -1;
        if (held == null || !held.isAlive()) return;
        Vec3 velocity = player.getLookAngle().scale(SupergirlConfig.THROW_STRENGTH.get()).add(0, 0.25, 0);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        held.teleportTo(start.x, start.y, start.z);
        if (held instanceof FallingBlockEntity block) {
            block.setNoGravity(false);
            block.setHurtsEntities(2.0F, 40);
        } else {
            state.thrown.add(new Thrown(held.getId()));
        }
        held.setDeltaMovement(velocity);
        held.hurtMarked = true;
        player.swing(InteractionHand.MAIN_HAND, true);
        level.sendParticles(FLASH, start.x, start.y, start.z, 1, 0, 0, 0, 0);
        play(level, player, SupergirlContent.THROW_HURL.get(), 1.3F, 1.0F);
        player.getCooldowns().addCooldown(player.getMainHandItem(), 10);
    }

    private static void tickThrown(ServerPlayer player, ServerLevel level, State state) {
        float damage = SupergirlConfig.THROW_DAMAGE.get().floatValue();
        Iterator<Thrown> it = state.thrown.iterator();
        while (it.hasNext()) {
            Thrown thrown = it.next();
            thrown.age++;
            Entity entity = level.getEntity(thrown.id);
            if (!(entity instanceof LivingEntity flying) || !flying.isAlive() || thrown.age > THROWN_TICKS) {
                it.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.CLOUD, flying.getX(), flying.getY() + flying.getBbHeight() * 0.5, flying.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, flying.getBoundingBox().inflate(0.3),
                    e -> e != flying && hittable(player, e));
            boolean crashed = thrown.age > 2 && (flying.onGround() || flying.horizontalCollision || flying.verticalCollision);
            if (hit.isEmpty() && !crashed) continue;
            for (LivingEntity other : hit) {
                other.invulnerableTime = 0;
                other.hurtServer(level, ModDamageTypes.superPunch(level, player), damage);
                Vec3 push = flying.getDeltaMovement().normalize();
                other.push(push.x * 0.8, 0.4, push.z * 0.8);
                other.hurtMarked = true;
            }
            flying.invulnerableTime = 0;
            flying.hurtServer(level, ModDamageTypes.superPunch(level, player), damage);
            level.sendParticles(ParticleTypes.EXPLOSION, flying.getX(), flying.getY() + 0.5, flying.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, flying.getX(), flying.getY(), flying.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
            it.remove();
        }
    }

    /** Drops what she holds right where it is. */
    private static void release(ServerLevel level, State state) {
        if (state.held < 0) return;
        Entity held = level.getEntity(state.held);
        state.held = -1;
        if (held instanceof FallingBlockEntity block) block.setNoGravity(false);
    }

    // ---- solar flare -------------------------------------------------------------------------------------

    private static void solarFlare(ServerPlayer player, ServerLevel level, State state, int energy) {
        float power = Mth.clamp(energy / (float) SupergirlHero.ENERGY.capacity(), 0.15F, 1.0F);
        double radius = SupergirlConfig.FLARE_RADIUS.get();
        float damage = SupergirlConfig.FLARE_MAX_DAMAGE.get().floatValue() * power;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> hittable(player, e))) {
            double dist = target.distanceTo(player);
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.6 * dist / radius);
            target.invulnerableTime = 0;
            target.hurtServer(level, ModDamageTypes.heatVision(level, player), damage * falloff);
            target.igniteForSeconds(5.0F);
            Vec3 away = target.position().subtract(player.position());
            away = new Vec3(away.x, 0, away.z);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(1.4 * falloff);
            target.push(away.x, 0.5 * falloff + 0.2, away.z);
            target.hurtMarked = true;
        }
        state.flareTicks = FLARE_TICKS;
        state.flarePower = power;
        state.weakUntil = level.getGameTime() + Mth.ceil(SupergirlConfig.FLARE_WEAK_SECONDS.get() * 20.0);
        if (isHearing(player.getMainHandItem())) stopHearing(player, player.getMainHandItem(), state);
        release(level, state);
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        level.sendParticles(FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SUPER_RING.get(), player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 1, 0, 1.0);
        play(level, player, SupergirlContent.SOLAR_FLARE.get(), 3.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_flare").withStyle(ChatFormatting.GOLD), true);
    }

    /** The light of the flare spreading out over a few ticks. */
    private static void tickFlare(ServerPlayer player, ServerLevel level, State state) {
        state.flareTicks--;
        float grow = 1.0F - state.flareTicks / (float) FLARE_TICKS;
        double r = SupergirlConfig.FLARE_RADIUS.get() * grow;
        int points = 24 + (int) (r * 6);
        double y = player.getY() + 1.0;
        for (int i = 0; i < points; i++) {
            double a = i * Mth.TWO_PI / points;
            double tilt = (i % 3 - 1) * 0.35;
            double x = player.getX() + Math.cos(a) * r;
            double z = player.getZ() + Math.sin(a) * r;
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? GOLD : 0xFFF6D0, 2.0F * state.flarePower + 0.6F), x, y + tilt * r * 0.5, z, 1, 0, 0, 0, 0);
            if (i % 4 == 0) level.sendParticles(ModParticles.SOLAR_GLOW.get(), x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }
        if (state.flareTicks % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 20, r * 0.3, 0.6, r * 0.3, 0.2);
    }

    /** While she recovers: worn out, no flight, Krypto standing guard. */
    private static void tickWeak(ServerPlayer player, ServerLevel level, State state) {
        if (player.tickCount % 10 == 0) {
            level.sendParticles(new DustParticleOptions(0x8A7A50, 0.7F), player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.0);
        }
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        KryptoEntity krypto = krypto(player, state);
        if (krypto == null || player.tickCount % 10 != 0) return;
        if (krypto.getTarget() == null || !krypto.getTarget().isAlive()) {
            level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10), m -> isEnemy(player, m))
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))).ifPresent(krypto::setTarget);
        }
        krypto.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 1, false, false), player);
    }

    // ---- barrel roll -------------------------------------------------------------------------------------

    /** Her barrel roll costs energy and keeps projectiles off her for a moment. */
    static void roll(ServerPlayer player) {
        State state = get(player);
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (item.isEmpty()) return;
        if (!player.isCreative() && !SupergirlHero.ENERGY.tryConsume(item, SupergirlConfig.ROLL_COST.get())) return;
        state.dodgeUntil = player.level().getGameTime() + SupergirlConfig.ROLL_DODGE_TICKS.get();
        ServerLevel level = player.level();
        level.sendParticles(new DustParticleOptions(RED, 1.2F), player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(new DustParticleOptions(GOLD, 0.9F), player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.5, 0.5, 0.5, 0.0);
        play(level, player, SupergirlContent.ROLL.get(), 1.0F, 1.0F);
    }

    // ---- krypto ------------------------------------------------------------------------------------------

    private static KryptoData data(ItemStack item) {
        KryptoData data = item.get(SupergirlContent.KRYPTO_DATA.get());
        return data != null ? data : KryptoData.NEW;
    }

    /** The pendant remembers him: his name, his health and when he can come back. */
    private static void remember(ItemStack item, KryptoEntity krypto, long backAt) {
        String name = krypto.hasCustomName() ? krypto.getCustomName().getString() : "";
        float health = backAt > 0 ? 0.0F : krypto.getHealth();
        item.set(SupergirlContent.KRYPTO_DATA.get(), new KryptoData(true, name, health, backAt));
    }

    /** Krypto comes down from the sky to her side. */
    static KryptoEntity callKrypto(ServerPlayer player, ItemStack item, State state) {
        ServerLevel level = player.level();
        KryptoData data = data(item);
        KryptoEntity krypto = SupergirlContent.KRYPTO.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (krypto == null) return null;
        krypto.tame(player);
        krypto.setOrderedToSit(false);
        krypto.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(SupergirlConfig.KRYPTO_HEALTH.get());
        krypto.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(SupergirlConfig.KRYPTO_DAMAGE.get());
        krypto.setHealth(data.health() > 0 ? Math.min(data.health(), krypto.getMaxHealth()) : krypto.getMaxHealth());
        if (!data.name().isEmpty()) krypto.setCustomName(Component.literal(data.name()));
        // As high as the sky above her allows (up to 16 blocks), a little to her side.
        Vec3 side = horizontal(player).cross(new Vec3(0, 1, 0)).normalize().scale(3.0);
        Vec3 start = player.position().add(side);
        double up = 0;
        for (double h = 1; h <= 16; h += 1) {
            if (!level.noCollision(krypto, krypto.getDimensions(krypto.getPose()).makeBoundingBox(start.add(0, h, 0)))) break;
            up = h;
        }
        start = start.add(0, up, 0);
        krypto.snapTo(start.x, start.y, start.z, player.getYRot(), 0.0F);
        krypto.startArrival(KRYPTO_ARRIVAL_TICKS);
        level.addFreshEntity(krypto);
        state.krypto = krypto.getUUID();
        level.sendParticles(ParticleTypes.END_ROD, start.x, start.y + 0.4, start.z, 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, start.x, start.y, start.z, SupergirlContent.KRYPTO_FLY.get(), SoundSource.NEUTRAL, 1.2F, 1.0F);
        if (!data.met()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.krypto_arrives").withStyle(ChatFormatting.GOLD), true);
        }
        item.set(SupergirlContent.KRYPTO_DATA.get(), new KryptoData(true, data.name(), 0.0F, 0L));
        return krypto;
    }

    /** Sneaking and using the pendant: she whistles and he comes, wherever he is. */
    private static void whistle(ServerPlayer player, ItemStack item, State state) {
        ServerLevel level = player.level();
        play(level, player, SupergirlContent.KRYPTO_WHISTLE.get(), 1.0F, 1.0F);
        KryptoEntity krypto = krypto(player, state);
        if (krypto != null) {
            krypto.setOrderedToSit(false);
            krypto.setInSittingPose(false);
            if (krypto.distanceToSqr(player) > 6 * 6) krypto.catchUp(player);
            krypto.bark();
            return;
        }
        long backAt = data(item).backAt();
        if (backAt > level.getGameTime()) {
            int seconds = (int) Math.ceil((backAt - level.getGameTime()) / 20.0);
            player.displayClientMessage(Component.translatable("message.greenlantern.krypto_resting", seconds).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        callKrypto(player, item, state);
    }

    /** His health ran out: he flies off whimpering and comes back later. */
    static void kryptoDowned(KryptoEntity krypto) {
        if (krypto.getOwner() instanceof ServerPlayer owner) {
            State state = STATES.get(owner.getUUID());
            ItemStack item = SupergirlHero.INSTANCE.findItem(owner);
            long backAt = krypto.level().getGameTime() + Math.max(1, Mth.ceil(SupergirlConfig.KRYPTO_RETURN_SECONDS.get() * 20.0));
            if (!item.isEmpty()) remember(item, krypto, backAt);
            if (state != null && krypto.getUUID().equals(state.krypto)) state.krypto = null;
            owner.displayClientMessage(Component.translatable("message.greenlantern.krypto_hurt",
                    Mth.ceil(SupergirlConfig.KRYPTO_RETURN_SECONDS.get())).withStyle(ChatFormatting.GOLD), true);
        }
        krypto.flyAway(true);
    }

    private static void tickKrypto(ServerPlayer player, ServerLevel level, ItemStack item, State state) {
        if (player.tickCount % 10 != 0 || krypto(player, state) != null) return;
        KryptoData data = data(item);
        // Away from her (another world, or she went far): he comes again unless he is recovering.
        if (data.backAt() > level.getGameTime()) return;
        if (data.backAt() > 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.krypto_back").withStyle(ChatFormatting.GOLD), true);
        }
        callKrypto(player, item, state);
    }

    // ---- every server tick -------------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = SupergirlHero.INSTANCE.isSuited(player);
        if (!suited) {
            if (STATES.containsKey(player.getUUID())) remove(player);
            return;
        }
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (item.isEmpty()) return;
        State state = get(player);
        ServerLevel level = player.level();
        boolean weak = level.getGameTime() < state.weakUntil;

        recharge(player, level, item, state, weak);
        if (!state.bolts.isEmpty()) tickBolts(player, level, state);
        if (state.dashTicks > 0) tickDash(player, level, state);
        if (isHearing(item)) tickHearing(player, level, item, state);
        else if (state.hearing) stopHearing(player, item, state);
        if (state.held >= 0) tickHeld(player, state);
        if (!state.thrown.isEmpty()) tickThrown(player, level, state);
        if (state.flareTicks > 0) tickFlare(player, level, state);
        if (weak) tickWeak(player, level, state);
        tickKrypto(player, level, item, state);
    }

    /** The solar energy coming back: from the sun, a little in the shade, nothing while worn out. */
    private static void recharge(ServerPlayer player, ServerLevel level, ItemStack item, State state, boolean weak) {
        if (!weak) {
            state.pendingSolar += rechargeRate(player) / 20.0F;
            if (state.pendingSolar >= 1.0F) {
                int whole = (int) state.pendingSolar;
                state.pendingSolar -= whole;
                SupergirlHero.ENERGY.add(item, whole);
            }
        }
        boolean full = SupergirlHero.ENERGY.get(item).isFull();
        if (full && !state.wasFull) {
            play(level, player, SupergirlContent.SOLAR_FULL.get(), 0.7F, 1.0F);
            player.displayClientMessage(Component.translatable("message.greenlantern.supergirl_solar_full").withStyle(ChatFormatting.GOLD), true);
        }
        state.wasFull = full;
        if (!full && !weak && player.tickCount % 10 == 0 && sunlight(player) > 0.0F) {
            level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.35, 0.8, 0.35, 0.01);
        }
    }

    /** Solar energy per second right now: full in the sun, the shade rate under a roof or at night, none without any sky. */
    public static float rechargeRate(ServerPlayer player) {
        float sun = sunlight(player);
        if (sun > 0.0F) return SupergirlConfig.RECHARGE_PER_SECOND.get() * sun;
        ServerLevel level = player.level();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return 0.0F;
        BlockPos head = BlockPos.containing(player.getEyePosition());
        return level.getBrightness(LightLayer.SKY, head) > 0 ? SupergirlConfig.SHADE_RECHARGE_PER_SECOND.get() : 0.0F;
    }

    /** How much sunlight reaches her (0 in the dark, under a roof or out of the overworld's sky). */
    static float sunlight(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling() || !level.isBrightOutside()) return 0.0F;
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.canSeeSky(head)) return 0.0F;
        return level.isRainingAt(head) || level.isThundering() ? 0.35F : 1.0F;
    }

    /** Every punch she lands gives a little energy back. */
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getEntity() instanceof KryptoEntity) return;
        if (!SupergirlHero.INSTANCE.isSuited(player) || isWeak(player)) return;
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (item.isEmpty()) return;
        int added = SupergirlHero.ENERGY.add(item, SupergirlConfig.PUNCH_ENERGY.get());
        if (added > 0) {
            Entity target = event.getEntity();
            player.level().sendParticles(ModParticles.SOLAR_GLOW.get(), target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    4, 0.2, 0.2, 0.2, 0.05);
        }
    }

    /** Damage multiplier: projectiles miss her during a barrel roll; her skin shrugs off part of every blow while she has energy. */
    static float damageTaken(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
        if (!SupergirlHero.INSTANCE.isSuited(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 1.0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE) && isDodging(player)) return 0.0F;
        if (isWeak(player)) return 1.0F;
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (item.isEmpty() || SupergirlHero.ENERGY.get(item).stored() <= 0) return 1.0F;
        return 1.0F - SupergirlConfig.DAMAGE_REDUCTION.get().floatValue();
    }
}
