package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.flight.ServerFlightTracker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The server side of Cyborg: what each power does (see {@code docs/heroes/cyborg.md}), the Cyborg
 * Battery coming back (faster near powered redstone, all at once from a lightning bolt, never under
 * water) and what the powers leave running: missiles in the air, the scan, hacked golems, stunned
 * enemies, the repair and the Boom Tube opening.
 */
public final class CyborgServer {
    private static final int GLOW = 0xFF5A3C;
    private static final int RED = 0xC81E1E;
    private static final int SILVER = 0xB8BEC8;
    private static final int TUBE_BLUE = 0x9FDCFF;
    /** Ticks the Boom Tube takes to open before it pulls everyone through. */
    static final int BOOM_TUBE_TICKS = 24;
    private static final int REPAIR_TICKS = 100;
    private static final int MISSILE_LIFE = 80;
    private static final ColorParticleOption FLASH = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);
    private static final double MISSILE_SPEED = 0.9;

    /** A micro-rocket in flight (simulated here: it is only particles until it explodes). */
    private static final class Missile {
        Vec3 pos;
        Vec3 velocity;
        final int targetId;
        int age;

        Missile(Vec3 pos, Vec3 velocity, int targetId) {
            this.pos = pos;
            this.velocity = velocity;
            this.targetId = targetId;
        }
    }

    private static final class State {
        final List<Missile> missiles = new ArrayList<>();
        boolean scanning;
        float pendingScanCost;
        final Set<Integer> glowing = new HashSet<>();
        /** Hacked golems: entity id → game time the hack wears off. */
        final Map<Integer, Long> allies = new HashMap<>();
        /** Stunned enemies: entity id → game time they wake up. */
        final Map<Integer, Long> stunned = new HashMap<>();
        /** Redstone lamps switched off by the EMP, lit again at {@link #lampsBack}. */
        final List<BlockPos> lamps = new ArrayList<>();
        long lampsBack;
        int repairTicks;
        int tubeTicks;
        Vec3 tubeAt = Vec3.ZERO;
        boolean wasFull = true;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private CyborgServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** The suit comes off or the player leaves: every running power stops, golems and lamps go back to normal. */
    public static void remove(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state == null) return;
        ServerLevel level = player.level();
        clearGlow(player, state);
        restoreLamps(level, state);
        for (Integer id : state.stunned.keySet()) {
            if (level.getEntity(id) instanceof Mob mob) mob.removeEffect(MobEffects.SLOWNESS);
        }
        ItemStack item = CyborgHero.INSTANCE.findItem(player);
        if (!item.isEmpty()) item.remove(CyborgContent.SCAN.get());
    }

    /** Whether Tech Scan is on (read from the Mother Box, so the client sees it too). */
    public static boolean isScanning(ItemStack item) {
        return Boolean.TRUE.equals(item.get(CyborgContent.SCAN.get()));
    }

    /** Ticks left before an opening Boom Tube pulls everyone through (0 when none is opening). */
    public static int boomTubeTicks(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.tubeTicks;
    }

    /** Missiles of this Cyborg still in the air. */
    public static int missilesInFlight(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.missiles.size();
    }

    /** Whether this Cyborg's hack still holds the golem. */
    public static boolean isAlly(ServerPlayer player, Entity golem) {
        State state = STATES.get(player.getUUID());
        return state != null && state.allies.containsKey(golem.getId());
    }

    // ---- powers ----------------------------------------------------------------------------------------

    /** Uses a power with the Mother Box; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, CyborgPower power) {
        if (!CyborgHero.INSTANCE.isSuited(player) || player.isSpectator()) return false;
        State state = get(player);
        // Switching the scan off is free, and so is marking the Boom Tube's destination.
        if (power == CyborgPower.TECH_SCAN && isScanning(item)) {
            stopScan(player, item, state);
            return true;
        }
        if (power == CyborgPower.BOOM_TUBE && player.isShiftKeyDown()) {
            markBoomTube(player, item);
            return true;
        }
        if (power == CyborgPower.BOOM_TUBE && state.tubeTicks > 0) return false;
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !CyborgHero.ENERGY.tryConsume(item, power.cost())) {
            notifyNoEnergy(player);
            return false;
        }
        ServerLevel level = player.level();
        int cooldown = switch (power) {
            case SONIC_CANNON -> {
                sonicCannon(player, level);
                yield 14;
            }
            case SHOULDER_MISSILES -> {
                launchMissiles(player, level, state);
                yield 30;
            }
            case TECH_SCAN -> {
                item.set(CyborgContent.SCAN.get(), true);
                state.scanning = true;
                state.pendingScanCost = 0;
                play(level, player, CyborgContent.SCAN_ON.get(), 0.9F, 1.0F);
                player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_scan_on").withStyle(ChatFormatting.RED), true);
                yield 6;
            }
            case MACHINE_HACK -> {
                machineHack(player, level, state);
                yield 16;
            }
            case EMP_BURST -> {
                empBurst(player, level, state);
                yield 40;
            }
            case SELF_REPAIR -> {
                state.repairTicks = REPAIR_TICKS;
                repairSuit(player);
                player.clearFire();
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                play(level, player, CyborgContent.REPAIR.get(), 1.0F, 1.0F);
                yield 40;
            }
            case BOOM_TUBE -> {
                state.tubeTicks = BOOM_TUBE_TICKS;
                state.tubeAt = player.position().add(horizontal(player).scale(2.5));
                play(level, player, CyborgContent.BOOM_TUBE.get(), 2.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_boom_tube_open").withStyle(ChatFormatting.AQUA), true);
                yield 40;
            }
        };
        player.getCooldowns().addCooldown(item, cooldown);
        return true;
    }

    private static void notifyNoEnergy(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_cyborg_power").withStyle(ChatFormatting.RED), true);
    }

    private static void play(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 horizontal(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** Living things that count as enemies for Cyborg's powers. */
    static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator()) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerReference() == null ? null : owned.getOwnerReference().getUUID())) return false;
        if (entity instanceof IronGolem golem && isAlly(player, golem)) return false;
        return entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
    }

    /** The first creature or block in Cyborg's line of sight. */
    static HitResult aim(ServerLevel level, ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != player && !e.isSpectator() && e.isPickable() && !player.isPassengerOfSameVehicle(e), 0.3F);
        return entity != null ? entity : block;
    }

    // ---- sonic cannon ------------------------------------------------------------------------------------

    private static void sonicCannon(ServerPlayer player, ServerLevel level) {
        double range = CyborgConfig.SONIC_CANNON_RANGE.get();
        float damage = CyborgConfig.SONIC_CANNON_DAMAGE.get().floatValue();
        Vec3 look = player.getLookAngle();
        Vec3 muzzle = player.getEyePosition().add(0, -0.35, 0).add(look.scale(0.6));
        double cos = Math.cos(Math.toRadians(22));
        // Glass and ice go first, so whatever hides behind a window still gets the blast.
        if (CyborgConfig.SONIC_CANNON_SHATTERS.get()) shatter(player, level, muzzle, look, range);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(muzzle, muzzle).inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player))) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(muzzle);
            double dist = to.length();
            if (dist > range || dist < 1.0E-3 || to.normalize().dot(look) < cos) continue;
            if (!clearPath(level, player, muzzle, target)) continue;
            float falloff = (float) (1.0 - 0.6 * dist / range);
            target.invulnerableTime = 0;
            target.hurtServer(level, level.damageSources().sonicBoom(player), damage * falloff);
            double strength = 1.6 * falloff + 0.4;
            target.push(look.x * strength, 0.25 + 0.2 * falloff, look.z * strength);
            target.hurtMarked = true;
        }
        // The wave: rings widening down the cone.
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(look).normalize();
        for (double d = 1.0; d <= range; d += 1.5) {
            Vec3 at = muzzle.add(look.scale(d));
            if (((int) d) % 3 == 1) level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            double ring = 0.25 + d * 0.3;
            int points = 10 + (int) (d * 2);
            for (int k = 0; k < points; k++) {
                double a = k * Mth.TWO_PI / points;
                Vec3 p = at.add(side.scale(Math.cos(a) * ring)).add(up.scale(Math.sin(a) * ring));
                level.sendParticles(new DustParticleOptions(k % 2 == 0 ? TUBE_BLUE : 0xFFFFFF, 1.1F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(new DustParticleOptions(GLOW, 1.2F), muzzle.x, muzzle.y, muzzle.z, 12, 0.15, 0.15, 0.15, 0.0);
        player.swing(InteractionHand.MAIN_HAND, true);
        play(level, player, CyborgContent.SONIC_CANNON.get(), 1.6F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    private static boolean clearPath(ServerLevel level, ServerPlayer player, Vec3 from, Entity target) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(from) >= to.distanceToSqr(from) - 0.5;
    }

    /** Glass and ice in the cone shatter (no drops, like breaking glass by hand). */
    private static void shatter(ServerPlayer player, ServerLevel level, Vec3 muzzle, Vec3 look, double range) {
        double cos = Math.cos(Math.toRadians(22));
        BlockPos center = BlockPos.containing(muzzle);
        int r = (int) Math.ceil(range);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(muzzle);
            double dist = to.length();
            if (dist > range || dist < 0.5 || to.normalize().dot(look) < cos) continue;
            if (isFragile(level.getBlockState(pos))) level.destroyBlock(pos.immutable(), false, player);
        }
    }

    static boolean isFragile(BlockState state) {
        return state.is(BlockTags.IMPERMEABLE) || state.is(Blocks.GLASS_PANE) || state.is(BlockTags.ICE) && !state.is(Blocks.BLUE_ICE)
                || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
    }

    // ---- shoulder missiles -------------------------------------------------------------------------------

    private static void launchMissiles(ServerPlayer player, ServerLevel level, State state) {
        int count = CyborgConfig.MISSILE_COUNT.get();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24),
                e -> isEnemy(player, e) && e.position().subtract(player.position()).normalize().dot(look) > -0.2));
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        Vec3 right = horizontal(player).cross(new Vec3(0, 1, 0)).normalize();
        Vec3 shoulder = player.position().add(0, player.getBbHeight() * 0.85, 0).add(right.scale(-0.35));
        for (int i = 0; i < count; i++) {
            int targetId = targets.isEmpty() ? -1 : targets.get(i % targets.size()).getId();
            // They fan out and up before turning to their target.
            double spread = (i - (count - 1) * 0.5) * 0.25;
            Vec3 velocity = look.scale(0.4).add(right.scale(spread)).add(0, 0.45, 0);
            state.missiles.add(new Missile(shoulder, velocity, targetId));
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, shoulder.x, shoulder.y, shoulder.z, 8, 0.15, 0.1, 0.15, 0.02);
        play(level, player, CyborgContent.MISSILE_LAUNCH.get(), 1.3F, 1.0F);
    }

    private static void tickMissiles(ServerPlayer player, ServerLevel level, State state) {
        Iterator<Missile> it = state.missiles.iterator();
        while (it.hasNext()) {
            Missile missile = it.next();
            missile.age++;
            Entity target = missile.targetId < 0 ? null : level.getEntity(missile.targetId);
            Vec3 aimAt = target != null && target.isAlive() ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                    : missile.pos.add(player.getLookAngle().scale(4));
            if (missile.age > 4) {
                Vec3 want = aimAt.subtract(missile.pos).normalize().scale(MISSILE_SPEED);
                missile.velocity = missile.velocity.lerp(want, 0.25);
            }
            Vec3 next = missile.pos.add(missile.velocity);
            BlockHitResult hit = level.clip(new ClipContext(missile.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
            Vec3 boom = null;
            if (hit.getType() != HitResult.Type.MISS) boom = hit.getLocation();
            if (boom == null) {
                AABB swept = new AABB(missile.pos, next).inflate(0.4);
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, swept, e -> e != player && e.isAlive() && !(e instanceof Player))) {
                    boom = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                    break;
                }
            }
            if (boom == null && missile.age >= MISSILE_LIFE) boom = next;
            if (boom != null) {
                explode(player, level, boom);
                it.remove();
                continue;
            }
            missile.pos = next;
            level.sendParticles(ParticleTypes.FLAME, next.x, next.y, next.z, 1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, next.x, next.y, next.z, 2, 0.05, 0.05, 0.05, 0.0);
            level.sendParticles(new DustParticleOptions(GLOW, 0.8F), next.x, next.y, next.z, 1, 0, 0, 0, 0);
        }
    }

    private static void explode(ServerPlayer player, ServerLevel level, Vec3 at) {
        // Never breaks blocks; the source (Cyborg) is never hurt by his own rockets.
        level.explode(player, at.x, at.y, at.z, CyborgConfig.MISSILE_POWER.get().floatValue(), false, Level.ExplosionInteraction.NONE);
        level.playSound(null, at.x, at.y, at.z, CyborgContent.MISSILE_EXPLODE.get(), SoundSource.PLAYERS, 1.2F, 0.9F + level.random.nextFloat() * 0.2F);
    }

    // ---- tech scan ---------------------------------------------------------------------------------------

    private static void tickScan(ServerPlayer player, ServerLevel level, ItemStack item, State state) {
        if (!player.isCreative()) {
            state.pendingScanCost += CyborgConfig.TECH_SCAN_COST.get() / 20.0F;
            if (state.pendingScanCost >= 1.0F) {
                int whole = (int) state.pendingScanCost;
                state.pendingScanCost -= whole;
                if (!CyborgHero.ENERGY.has(item, whole)) {
                    CyborgHero.ENERGY.drain(item, whole);
                    notifyNoEnergy(player);
                    stopScan(player, item, state);
                    return;
                }
                CyborgHero.ENERGY.drain(item, whole);
            }
        }
        // The target in sight: its health in the action bar.
        if (player.tickCount % 5 == 0 && aim(level, player, 32) instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_scan_target", living.getDisplayName(),
                    Mth.ceil(living.getHealth()), Mth.ceil(living.getMaxHealth())).withStyle(ChatFormatting.RED), true);
        }
        if (player.tickCount % 40 == 0) play(level, player, CyborgContent.SCAN_PING.get(), 0.4F, 1.0F);
        if (player.tickCount % 10 != 0) return;
        double range = CyborgConfig.SCAN_RADIUS.get();
        Set<Integer> now = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> isEnemy(player, e) && e.distanceToSqr(player) < range * range)) {
            now.add(entity.getId());
            CyborgGlow.send(player, entity, true);
        }
        for (Integer id : state.glowing) {
            if (now.contains(id)) continue;
            Entity entity = level.getEntity(id);
            if (entity != null) CyborgGlow.send(player, entity, false);
        }
        state.glowing.clear();
        state.glowing.addAll(now);
    }

    private static void stopScan(ServerPlayer player, ItemStack item, State state) {
        item.remove(CyborgContent.SCAN.get());
        state.scanning = false;
        clearGlow(player, state);
        play(player.level(), player, CyborgContent.SCAN_OFF.get(), 0.8F, 1.0F);
    }

    private static void clearGlow(ServerPlayer player, State state) {
        for (Integer id : state.glowing) {
            Entity entity = player.level().getEntity(id);
            if (entity != null) CyborgGlow.send(player, entity, false);
        }
        state.glowing.clear();
    }

    // ---- machine hack ------------------------------------------------------------------------------------

    private static void machineHack(ServerPlayer player, ServerLevel level, State state) {
        double range = CyborgConfig.HACK_RANGE.get();
        HitResult hit = aim(level, player, range);
        Vec3 at = hit.getLocation();
        boolean done = false;
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof IronGolem golem) {
                golem.setPlayerCreated(true);
                golem.stopBeingAngry();
                golem.setTarget(null);
                state.allies.put(golem.getId(), level.getGameTime() + Mth.ceil(CyborgConfig.HACK_ALLY_SECONDS.get() * 20.0));
                golem.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Mth.ceil(CyborgConfig.HACK_ALLY_SECONDS.get() * 20.0), 1), player);
                player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_hack_golem").withStyle(ChatFormatting.RED), true);
                done = true;
            } else if (entity instanceof AbstractMinecart cart) {
                Vec3 push = horizontal(player).scale(1.2);
                cart.setDeltaMovement(push.x, 0.1, push.z);
                cart.hurtMarked = true;
                done = true;
            } else if (entity instanceof Mob mob && isEnemy(player, mob)) {
                // Not a machine, but its senses get scrambled for a moment.
                stun(player, level, state, mob, 40);
                done = true;
            }
        } else if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            done = hackBlock(player, level, blockHit.getBlockPos());
        }
        if (!done) {
            player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_hack_nothing").withStyle(ChatFormatting.GRAY), true);
        }
        // The data stream: a line of red sparks from the hand to the target.
        Vec3 from = player.getEyePosition().add(0, -0.4, 0);
        Vec3 step = at.subtract(from);
        int points = Math.max(2, (int) (step.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.add(step.scale(i / (double) points));
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? GLOW : RED, 0.7F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.25, 0.25, 0.25, 0.1);
        play(level, player, CyborgContent.HACK.get(), 1.0F, 1.0F);
    }

    /** Doors, trapdoors and gates swing (iron ones too), levers flip and buttons press. */
    private static boolean hackBlock(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(player, level, state, pos, !door.isOpen(state));
            return true;
        }
        if (state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock) {
            boolean open = !state.getValue(BlockStateProperties.OPEN);
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 10);
            level.levelEvent(null, open ? 1037 : 1036, pos, 0);
            return true;
        }
        if (state.getBlock() instanceof LeverBlock lever) {
            lever.pull(state, level, pos, player);
            return true;
        }
        if (state.getBlock() instanceof ButtonBlock button && !state.getValue(BlockStateProperties.POWERED)) {
            button.press(state, level, pos, player);
            return true;
        }
        return false;
    }

    private static void tickAllies(ServerPlayer player, ServerLevel level, State state) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = state.allies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof IronGolem golem) || !golem.isAlive() || now >= entry.getValue()) {
                it.remove();
                continue;
            }
            if (golem.getTarget() == player) golem.setTarget(null);
            if (player.tickCount % 10 == 0) {
                if (golem.getTarget() == null || !golem.getTarget().isAlive()) {
                    level.getEntitiesOfClass(LivingEntity.class, golem.getBoundingBox().inflate(16), e -> isEnemy(player, e)).stream()
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(golem))).ifPresent(golem::setTarget);
                }
                level.sendParticles(new DustParticleOptions(GLOW, 0.8F), golem.getX(), golem.getY() + golem.getBbHeight() * 0.8, golem.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    // ---- EMP burst ---------------------------------------------------------------------------------------

    private static void empBurst(ServerPlayer player, ServerLevel level, State state) {
        double radius = CyborgConfig.EMP_RADIUS.get();
        int stunTicks = Mth.ceil(CyborgConfig.EMP_STUN_SECONDS.get() * 20.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> isEnemy(player, e) && e.distanceToSqr(player) <= radius * radius)) {
            if (target instanceof Mob mob) stun(player, level, state, mob, stunTicks);
            target.invulnerableTime = 0;
            target.hurtServer(level, level.damageSources().lightningBolt(), 3.0F);
            // Whatever flies falls out of the sky.
            if (!target.onGround() && (target.isNoGravity()
                    || target instanceof Mob mob && mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
                    || target instanceof net.minecraft.world.entity.monster.Phantom)) {
                target.setDeltaMovement(target.getDeltaMovement().x * 0.2, -1.4, target.getDeltaMovement().z * 0.2);
                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 0), player);
                target.hurtMarked = true;
            }
        }
        switchOffLamps(level, player.blockPosition(), radius, state);
        double y = player.getY() + 1.0;
        for (int i = 0; i < 48; i++) {
            double angle = i * Mth.TWO_PI / 48;
            double x = player.getX() + Math.cos(angle) * 1.5;
            double z = player.getZ() + Math.sin(angle) * 1.5;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, Math.cos(angle), 0.05, Math.sin(angle), radius * 0.12);
            level.sendParticles(new DustParticleOptions(TUBE_BLUE, 1.4F), x, y, z, 0, Math.cos(angle), 0, Math.sin(angle), radius * 0.08);
        }
        level.sendParticles(FLASH, player.getX(), y, player.getZ(), 1, 0, 0, 0, 0);
        play(level, player, CyborgContent.EMP.get(), 2.0F, 1.0F);
    }

    private static void stun(ServerPlayer player, ServerLevel level, State state, Mob mob, int ticks) {
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, true), player);
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, true), player);
        state.stunned.merge(mob.getId(), level.getGameTime() + ticks, Math::max);
    }

    private static void tickStunned(ServerPlayer player, ServerLevel level, State state) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = state.stunned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            if (!(level.getEntity(entry.getKey()) instanceof Mob mob) || !mob.isAlive() || now >= entry.getValue()) {
                it.remove();
                continue;
            }
            mob.setTarget(null);
            mob.getNavigation().stop();
            if (player.tickCount % 6 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 2, 0.25, 0.15, 0.25, 0.02);
            }
        }
    }

    /** Lit redstone lamps around go dark for a moment (they light again on their own if still powered). */
    private static void switchOffLamps(ServerLevel level, BlockPos center, double radius, State state) {
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) > radius * radius) continue;
            BlockState lamp = level.getBlockState(pos);
            if (lamp.getBlock() instanceof RedstoneLampBlock && lamp.getValue(RedstoneLampBlock.LIT)) {
                level.setBlock(pos, lamp.setValue(RedstoneLampBlock.LIT, false), 2);
                state.lamps.add(pos.immutable());
            }
        }
        if (!state.lamps.isEmpty()) state.lampsBack = level.getGameTime() + 60;
    }

    private static void restoreLamps(ServerLevel level, State state) {
        for (BlockPos pos : state.lamps) {
            BlockState lamp = level.getBlockState(pos);
            if (lamp.getBlock() instanceof RedstoneLampBlock && !lamp.getValue(RedstoneLampBlock.LIT) && level.hasNeighborSignal(pos)) {
                level.setBlock(pos, lamp.setValue(RedstoneLampBlock.LIT, true), 2);
            }
        }
        state.lamps.clear();
    }

    // ---- self repair -------------------------------------------------------------------------------------

    private static void repairSuit(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.isDamageableItem()) piece.setDamageValue(0);
        }
    }

    private static void tickRepair(ServerPlayer player, ServerLevel level, State state) {
        state.repairTicks--;
        float perTick = CyborgConfig.REPAIR_HEALTH.get().floatValue() / REPAIR_TICKS;
        if (player.getHealth() < player.getMaxHealth()) player.heal(perTick);
        if (state.repairTicks % 3 == 0) {
            level.sendParticles(new DustParticleOptions(state.repairTicks % 2 == 0 ? GLOW : SILVER, 0.6F),
                    player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.4, 0.8, 0.4, 0.0);
        }
        if (state.repairTicks % 20 == 0) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 3, 0.4, 0.5, 0.4, 0.0);
    }

    // ---- boom tube ---------------------------------------------------------------------------------------

    private static void markBoomTube(ServerPlayer player, ItemStack item) {
        BlockPos pos = player.blockPosition();
        item.set(CyborgContent.BOOM_MARK.get(), GlobalPos.of(player.level().dimension(), pos));
        player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_boom_tube_marked", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.AQUA), true);
        play(player.level(), player, CyborgContent.SCAN_ON.get(), 0.8F, 1.4F);
        player.level().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.1, player.getZ(), 20, 0.4, 0.05, 0.4, 0.02);
    }

    private static void tickBoomTube(ServerPlayer player, ServerLevel level, ItemStack item, State state) {
        state.tubeTicks--;
        Vec3 c = state.tubeAt.add(0, 1.0, 0);
        Vec3 forward = horizontal(player);
        Vec3 side = forward.cross(new Vec3(0, 1, 0)).normalize();
        float grow = 1.0F - state.tubeTicks / (float) BOOM_TUBE_TICKS;
        double radius = 0.4 + 1.4 * grow;
        // A spinning ring of white and blue light, opening up, with the tunnel stretching away behind it.
        for (int i = 0; i < 20; i++) {
            double angle = i * Mth.TWO_PI / 20 + state.tubeTicks * 0.35;
            Vec3 p = c.add(side.scale(Math.cos(angle) * radius)).add(0, Math.sin(angle) * radius, 0);
            level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : new DustParticleOptions(TUBE_BLUE, 1.5F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            Vec3 deep = p.add(forward.scale(1.5 + (i % 5)));
            if (i % 2 == 0) level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.0F), deep.x, deep.y, deep.z, 1, 0, 0, 0, 0);
        }
        if (state.tubeTicks > 0) return;
        GlobalPos mark = item.get(CyborgContent.BOOM_MARK.get());
        MinecraftServer server = level.getServer();
        ServerLevel target = null;
        Vec3 arrival = null;
        if (mark != null) {
            target = server.getLevel(mark.dimension());
            if (target != null) arrival = Vec3.atBottomCenterOf(mark.pos());
        }
        if (target == null) {
            // No mark: the bed (or respawn anchor), else the world spawn.
            ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
            if (respawn != null) {
                target = server.getLevel(respawn.respawnData().dimension());
                if (target != null) arrival = safeNear(target, respawn.respawnData().pos());
            }
        }
        if (target == null) {
            var spawn = server.getRespawnData();
            target = server.getLevel(spawn.dimension());
            if (target == null) target = server.overworld();
            arrival = Vec3.atBottomCenterOf(target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.pos()));
        }
        double radius2 = CyborgConfig.BOOM_TUBE_RADIUS.get();
        List<Entity> travellers = new ArrayList<>();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(radius2), e -> e.isAlive() && !e.isSpectator())) {
            boolean friend = entity instanceof Player || (entity instanceof LivingEntity living && !isEnemy(player, living))
                    || entity.getVehicle() == player || player.getVehicle() == entity;
            if (friend && entity.distanceToSqr(player) <= radius2 * radius2 && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
                travellers.add(entity);
            }
        }
        level.sendParticles(FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        player.stopRiding();
        player.teleport(new TeleportTransition(target, arrival, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        int n = 0;
        for (Entity entity : travellers) {
            n++;
            double angle = n * 1.3;
            Vec3 at = arrival.add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            entity.stopRiding();
            entity.teleport(new TeleportTransition(target, at, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING));
        }
        target.sendParticles(FLASH, arrival.x, arrival.y + 1, arrival.z, 1, 0, 0, 0, 0);
        target.sendParticles(ParticleTypes.END_ROD, arrival.x, arrival.y + 1, arrival.z, 60, 0.6, 1.0, 0.6, 0.15);
        target.sendParticles(new DustParticleOptions(TUBE_BLUE, 1.6F), arrival.x, arrival.y + 1, arrival.z, 40, 1.0, 1.0, 1.0, 0.0);
        target.playSound(null, arrival.x, arrival.y, arrival.z, CyborgContent.BOOM_TUBE.get(), SoundSource.PLAYERS, 2.0F, 1.15F);
    }

    /** A spot to stand next to a bed or anchor (the bed itself is not one). */
    private static Vec3 safeNear(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int r = 0; r <= 2; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos feet = pos.offset(dx, dy, dz);
                        if (standable(level, feet)) return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos));
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                && level.getBlockState(feet).getFluidState().isEmpty();
    }

    // ---- every server tick -------------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = CyborgHero.INSTANCE.isSuited(player);
        if (!suited) {
            if (STATES.containsKey(player.getUUID())) remove(player);
            return;
        }
        ItemStack item = CyborgHero.INSTANCE.findItem(player);
        if (item.isEmpty()) return;
        State state = get(player);
        ServerLevel level = player.level();

        if (player.tickCount % 20 == 0) recharge(player, level, item, state);
        if (player.tickCount % 5 == 0 && !level.getEntitiesOfClass(LightningBolt.class, player.getBoundingBox().inflate(10)).isEmpty()
                && !CyborgHero.ENERGY.get(item).isFull()) {
            // A lightning bolt nearby: the battery drinks it all.
            CyborgHero.ENERGY.set(item, CyborgHero.ENERGY.capacity());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.9, 0.4, 0.2);
            player.displayClientMessage(Component.translatable("message.greenlantern.cyborg_lightning").withStyle(ChatFormatting.AQUA), true);
        }
        boolean full = CyborgHero.ENERGY.get(item).isFull();
        if (full && !state.wasFull) play(level, player, CyborgContent.BATTERY_FULL.get(), 0.7F, 1.0F);
        state.wasFull = full;

        if (!state.missiles.isEmpty()) tickMissiles(player, level, state);
        if (isScanning(item)) tickScan(player, level, item, state);
        else if (state.scanning) stopScan(player, item, state);
        if (!state.allies.isEmpty()) tickAllies(player, level, state);
        if (!state.stunned.isEmpty()) tickStunned(player, level, state);
        if (!state.lamps.isEmpty() && level.getGameTime() >= state.lampsBack) restoreLamps(level, state);
        if (state.repairTicks > 0) tickRepair(player, level, state);
        if (state.tubeTicks > 0) tickBoomTube(player, level, item, state);
    }

    /** Once a second: the battery comes back, faster near powered redstone, never under water. */
    private static void recharge(ServerPlayer player, ServerLevel level, ItemStack item, State state) {
        if (player.isUnderWater()) return;
        double amount = CyborgConfig.RECHARGE_PER_SECOND.get();
        boolean redstone = nearPoweredRedstone(level, player.blockPosition());
        if (redstone) amount *= CyborgConfig.REDSTONE_RECHARGE_MULTIPLIER.get();
        int added = CyborgHero.ENERGY.add(item, (int) Math.round(amount));
        if (redstone && added > 0) {
            level.sendParticles(new DustParticleOptions(RED, 0.8F), player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.35, 0.7, 0.35, 0.0);
        }
    }

    /** A redstone block, a lit redstone torch or powered dust within 4 blocks. */
    public static boolean nearPoweredRedstone(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -3, -4), center.offset(4, 4, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.REDSTONE_BLOCK)) return true;
            if (state.getBlock() instanceof RedstoneTorchBlock && state.getValue(RedstoneTorchBlock.LIT)) return true;
            if (state.getBlock() instanceof RedStoneWireBlock && state.getValue(RedStoneWireBlock.POWER) > 0) return true;
        }
        return false;
    }

    /** Whether the thrusters burn battery right now: hovering in place is free. */
    static boolean thrustersBurning(ServerPlayer player) {
        return ServerFlightTracker.speedFraction(player) > 0.04F;
    }
}
