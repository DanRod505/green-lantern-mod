package com.danrod505.greenlantern.shazam;

import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.power.Area;
import com.danrod505.greenlantern.hero.power.BlockEffects;
import com.danrod505.greenlantern.hero.power.Bolts;
import com.danrod505.greenlantern.hero.power.Dash;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import com.danrod505.greenlantern.ring.FlightHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The server side of Shazam (see {@code docs/heroes/shazam.md}): each power is built from the blocks
 * in {@code hero/power}, plus the lightning of the magic word (suit on and off, magic back) and the
 * magic coming back on its own, faster in a thunderstorm.
 */
public final class ShazamServer {
    static final int GOLD = 0xF2B705;
    static final int RED = 0xC81E1E;
    static final int GLOW = 0xFFF6B0;
    private static final ColorParticleOption FLASH = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF6B0);
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(GOLD, 1.2F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(RED, 1.4F);
    private static final double BOLT_SPEED = 2.4;
    private static final int DASH_TICKS = 7;

    /** Stamina of Atlas: as heavy as a mountain while it is on. */
    private static final List<SuitModifier> ATLAS_MODIFIERS = List.of(
            SuitModifier.add(Attributes.KNOCKBACK_RESISTANCE, "shazam_atlas_steady", 1.0),
            SuitModifier.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, "shazam_atlas_blast_proof", 1.0));

    private static final class State {
        final Bolts bolts = new Bolts(16, 0.3);
        final Dash dash = new Dash();
        int atlasFood = -1;
        boolean wasFull = true;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    /** When the next "Shazam!" brings magic back (kept when the suit comes off, so turning back and forth is no free recharge). */
    private static final Map<UUID, Long> RECHARGE_READY = new ConcurrentHashMap<>();

    private ShazamServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** The suit comes off or he leaves: Atlas and Solomon switch off and what was in the air ends. */
    public static void remove(ServerPlayer player) {
        STATES.remove(player.getUUID());
        for (SuitModifier modifier : ATLAS_MODIFIERS) modifier.apply(player, false);
        ItemStack item = ShazamHero.INSTANCE.findItem(player);
        if (!item.isEmpty()) {
            item.remove(ShazamContent.ATLAS.get());
            item.remove(ShazamContent.SOLOMON.get());
        }
    }

    // ---- queries (also used by the tests and the client) ------------------------------------------------

    public static boolean isAtlas(ItemStack item) {
        return Boolean.TRUE.equals(item.get(ShazamContent.ATLAS.get()));
    }

    public static boolean isSolomon(ItemStack item) {
        return Boolean.TRUE.equals(item.get(ShazamContent.SOLOMON.get()));
    }

    public static boolean isDashing(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.dash.active();
    }

    public static boolean boltsInFlight(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && !state.bolts.isEmpty();
    }

    // ---- the magic word ----------------------------------------------------------------------------------

    /**
     * "Shazam!": a lightning bolt falls on him (only light and thunder: no fire, no harm to him), hurts
     * and throws back the enemies right next to him and, once every few seconds, brings magic back.
     */
    static void transform(ServerPlayer player, boolean suitingUp) {
        ServerLevel level = player.level();
        strikeVisual(level, player.position());
        Vec3 center = player.position().add(0, 1.0, 0);
        level.sendParticles(FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 60, 0.5, 1.0, 0.5, 0.25);
        level.sendParticles(GOLD_DUST, center.x, center.y, center.z, 40, 0.5, 1.0, 0.5, 0.0);
        Fx.ring(level, player.position().add(0, 0.1, 0), 4.0, ParticleTypes.ELECTRIC_SPARK, 36);
        float damage = ShazamConfig.TRANSFORM_DAMAGE.get().floatValue();
        for (LivingEntity target : Targets.inRadius(level, center, 2.5, e -> Targets.enemy(player, e))) {
            Hits.strike(level, target, magic(player), damage);
            Hits.knockAway(target, player.position(), 0.9, 0.35);
        }
        if (!suitingUp) return;
        ItemStack item = ShazamHero.INSTANCE.findItem(player);
        long now = level.getGameTime();
        if (!item.isEmpty() && now >= RECHARGE_READY.getOrDefault(player.getUUID(), Long.MIN_VALUE)) {
            ShazamHero.ENERGY.add(item, ShazamConfig.TRANSFORM_RECHARGE.get());
            RECHARGE_READY.put(player.getUUID(), now + ShazamConfig.TRANSFORM_RECHARGE_COOLDOWN_SECONDS.get() * 20L);
        }
    }

    /** A lightning bolt that is only light and thunder, falling at {@code at} (it shows through rock, so it works underground). */
    private static void strikeVisual(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(at);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static DamageSource magic(ServerPlayer player) {
        return player.level().damageSources().indirectMagic(player, player);
    }

    // ---- powers ------------------------------------------------------------------------------------------

    /** Uses a power with the Wizard's Bolt; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, ShazamPower power) {
        if (!ShazamHero.INSTANCE.isSuited(player) || player.isSpectator()) return false;
        State state = get(player);
        ServerLevel level = player.level();
        // Switching Atlas and Solomon off is free.
        if (power == ShazamPower.ATLAS_STAMINA && isAtlas(item)) {
            stopAtlas(player, item, state);
            return true;
        }
        if (power == ShazamPower.SOLOMON_WISDOM && isSolomon(item)) {
            stopSolomon(player, item);
            return true;
        }
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !ShazamHero.ENERGY.tryConsume(item, power.cost())) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_shazam_magic").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int cooldown = switch (power) {
            case ZEUS_LIGHTNING -> {
                zeusLightning(player, level, state);
                yield 6;
            }
            case THUNDER_CALL -> {
                thunderCall(player, level);
                yield 30;
            }
            case MERCURY_DASH -> {
                mercuryDash(player, level, state);
                yield 20;
            }
            case HERCULES_SLAM -> {
                herculesSlam(player, level);
                yield 24;
            }
            case ACHILLES_ROAR -> {
                achillesRoar(player, level);
                yield 30;
            }
            case ATLAS_STAMINA -> {
                startAtlas(player, item, state);
                yield 6;
            }
            case SOLOMON_WISDOM -> {
                startSolomon(player, level, item);
                yield 6;
            }
        };
        player.getCooldowns().addCooldown(item, cooldown);
        return true;
    }

    /** Zeus's Lightning: a golden bolt from his hand that homes on the enemy in sight. */
    private static void zeusLightning(ServerPlayer player, ServerLevel level, State state) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(0, -0.35, 0).add(look.scale(0.7));
        LivingEntity target = null;
        HitResult aim = Targets.aim(level, player, 32);
        if (aim instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && Targets.hittable(player, living)) {
            target = living;
        } else {
            List<LivingEntity> ahead = Targets.inCone(level, player.getEyePosition(), look, 32, 12, e -> Targets.enemy(player, e));
            if (!ahead.isEmpty()) target = ahead.get(0);
        }
        state.bolts.fire(hand, look.scale(BOLT_SPEED), target);
        level.sendParticles(FLASH, hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
        Fx.play(level, player, ShazamContent.ZEUS_LIGHTNING.get(), 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    /** Where a bolt of Zeus's Lightning lands: the enemy is hurt and the bolt jumps on to the next ones. */
    private static void zeusImpact(ServerPlayer player, ServerLevel level, Bolts.Impact impact) {
        Vec3 at = impact.at();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.2, 0.2, 0.2, 0.3);
        if (impact.entity() == null) {
            Fx.play(level, player, ShazamContent.LIGHTNING_HIT.get(), 0.5F, 1.3F);
            if (impact.block() != null) BlockEffects.shatterGlass(level, List.of(impact.block().getBlockPos()), player);
            return;
        }
        float damage = ShazamConfig.ZEUS_DAMAGE.get().floatValue();
        LivingEntity current = impact.entity();
        Set<Integer> hit = new HashSet<>();
        for (int jump = 0; current != null; jump++) {
            hit.add(current.getId());
            Hits.strike(level, current, magic(player), damage);
            Vec3 middle = Targets.middle(current);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, middle.x, middle.y, middle.z, 16, 0.3, 0.5, 0.3, 0.2);
            level.playSound(null, middle.x, middle.y, middle.z, ShazamContent.LIGHTNING_HIT.get(), SoundSource.PLAYERS, 0.8F,
                    1.0F + jump * 0.15F);
            if (jump >= ShazamConfig.ZEUS_JUMPS.get()) break;
            LivingEntity from = current;
            current = Targets.inRadius(level, middle, ShazamConfig.ZEUS_JUMP_RANGE.get(), e -> Targets.enemy(player, e) && !hit.contains(e.getId()))
                    .stream().filter(e -> e != from).findFirst().orElse(null);
            if (current != null) arc(level, middle, Targets.middle(current));
            damage *= 0.75F;
        }
    }

    /** A jagged arc of lightning between two points. */
    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 prev = from;
        int segments = Math.max(3, (int) (to.distanceTo(from) / 1.5));
        for (int i = 1; i <= segments; i++) {
            Vec3 next = from.lerp(to, i / (double) segments);
            if (i < segments) next = next.add((level.random.nextDouble() - 0.5) * 0.6, (level.random.nextDouble() - 0.5) * 0.6, (level.random.nextDouble() - 0.5) * 0.6);
            Fx.line(level, prev, next, ParticleTypes.ELECTRIC_SPARK, 5);
            Fx.line(level, prev, next, GOLD_DUST, 3);
            prev = next;
        }
    }

    /** Thunder Call: lightning from the sky where he aims (through rock, too). */
    private static void thunderCall(ServerPlayer player, ServerLevel level) {
        HitResult aim = Targets.aim(level, player, ShazamConfig.THUNDER_RANGE.get());
        Vec3 at = aim.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(ShazamConfig.THUNDER_RANGE.get())) : aim.getLocation();
        if (aim instanceof EntityHitResult hit) at = hit.getEntity().position();
        at = new Vec3(at.x, Fx.groundBelow(level, at), at.z);
        strikeVisual(level, at);
        level.sendParticles(FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 50, 0.6, 0.6, 0.6, 0.4);
        Fx.ring(level, at.add(0, 0.2, 0), ShazamConfig.THUNDER_RADIUS.get(), ParticleTypes.ELECTRIC_SPARK, 32);
        Fx.dust(level, at, ShazamConfig.THUNDER_RADIUS.get(), 24);
        level.playSound(null, at.x, at.y, at.z, ShazamContent.THUNDER_CALL.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        double radius = ShazamConfig.THUNDER_RADIUS.get();
        float damage = ShazamConfig.THUNDER_DAMAGE.get().floatValue();
        Vec3 center = at.add(0, 1, 0);
        for (LivingEntity target : Targets.inRadius(level, center, radius + 0.5, e -> Targets.hittable(player, e))) {
            float falloff = (float) (1.0 - 0.4 * Targets.middle(target).distanceTo(center) / (radius + 0.5));
            Hits.strike(level, target, magic(player), damage * falloff);
            Hits.knockAway(target, at, 1.1 * falloff, 0.55);
        }
        BlockEffects.shatterGlass(level, Area.sphere(center, radius), player);
    }

    /** Speed of Mercury: a red and gold blur in a straight line. */
    private static void mercuryDash(ServerPlayer player, ServerLevel level, State state) {
        state.dash.start(player, player.getLookAngle(), ShazamConfig.DASH_DISTANCE.get(), DASH_TICKS);
        Fx.play(level, player, ShazamContent.MERCURY_DASH.get(), 1.0F, 1.0F);
        Vec3 at = player.position().add(0, 1, 0);
        level.sendParticles(FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private static void tickDash(ServerPlayer player, ServerLevel level, State state) {
        Vec3 from = player.position().add(0, 1, 0);
        float damage = ShazamConfig.DASH_DAMAGE.get().floatValue();
        boolean going = state.dash.tick(player, level, (target, dir) -> {
            Hits.strike(level, target, player.damageSources().playerAttack(player), damage);
            Hits.fling(target, dir, 1.3, 0.45);
            Vec3 m = Targets.middle(target);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, 12, 0.3, 0.4, 0.3, 0.2);
        });
        Vec3 to = player.position().add(0, 1, 0);
        Fx.line(level, from, to, RED_DUST, 3);
        Fx.line(level, from.add(0, -0.4, 0), to.add(0, -0.4, 0), GOLD_DUST, 2);
        Fx.line(level, from, to, ParticleTypes.ELECTRIC_SPARK, 1);
        if (!going) {
            // Keep a little of the speed so the dash doesn't stop dead.
            Vec3 dir = state.dash.direction();
            player.setDeltaMovement(dir.scale(0.4));
            player.hurtMarked = true;
            state.dash.stop();
        }
    }

    /** Strength of Hercules: a punch to the ground that throws everything around away. */
    private static void herculesSlam(ServerPlayer player, ServerLevel level) {
        Vec3 ground = new Vec3(player.getX(), Fx.groundBelow(level, player.position()), player.getZ());
        double radius = ShazamConfig.SLAM_RADIUS.get();
        float damage = ShazamConfig.SLAM_DAMAGE.get().floatValue();
        for (LivingEntity target : Targets.inRadius(level, ground.add(0, 1, 0), radius, e -> Targets.hittable(player, e))) {
            float falloff = (float) (1.0 - 0.5 * target.position().distanceTo(ground) / radius);
            Hits.strike(level, target, player.damageSources().playerAttack(player), damage * falloff);
            Hits.knockAway(target, ground, 1.6 * falloff, 0.6);
        }
        Fx.dust(level, ground, radius, 60);
        Fx.ring(level, ground.add(0, 0.2, 0), radius, ParticleTypes.CLOUD, 40);
        Fx.ring(level, ground.add(0, 0.3, 0), radius * 0.7, GOLD_DUST, 28);
        level.sendParticles(ParticleTypes.EXPLOSION, ground.x, ground.y + 0.3, ground.z, 1, 0, 0, 0, 0);
        Fx.play(level, player, ShazamContent.HERCULES_SLAM.get(), 1.5F, 1.0F);
        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
        player.hurtMarked = true;
    }

    /** Courage of Achilles: a battle cry in a cone that stuns and puts out fire. */
    private static void achillesRoar(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 mouth = player.getEyePosition().add(look.scale(0.4));
        double range = ShazamConfig.ROAR_RANGE.get();
        int stun = Mth.ceil(ShazamConfig.ROAR_STUN_SECONDS.get() * 20.0);
        for (LivingEntity target : Targets.inCone(level, mouth, look, range, 35, e -> Targets.hittable(player, e))) {
            Hits.fling(target, new Vec3(look.x, 0, look.z), 0.5, 0.15);
            if (target instanceof Mob mob && Targets.enemy(player, mob)) {
                Hits.stun(mob, player, stun);
                Vec3 m = Targets.middle(target);
                level.sendParticles(GOLD_DUST, m.x, m.y + target.getBbHeight() * 0.5, m.z, 8, 0.3, 0.1, 0.3, 0.0);
            }
        }
        BlockEffects.snuffFire(level, Area.cone(mouth, look, range, 35), player, true);
        // Rings of golden sound racing down the cone.
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(look).normalize();
        for (double d = 1.0; d <= range; d += 1.5) {
            Vec3 c = mouth.add(look.scale(d));
            double r = 0.3 + d * Math.tan(Math.toRadians(35)) * 0.7;
            int points = 8 + (int) (d * 2);
            for (int i = 0; i < points; i++) {
                double a = Mth.TWO_PI * i / points;
                Vec3 p = c.add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
                level.sendParticles(d % 3 < 1.5 ? GOLD_DUST : ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        Fx.play(level, player, ShazamContent.ACHILLES_ROAR.get(), 1.6F, 1.0F);
    }

    /** Stamina of Atlas on: no knockback, half damage, no hunger. */
    private static void startAtlas(ServerPlayer player, ItemStack item, State state) {
        item.set(ShazamContent.ATLAS.get(), true);
        for (SuitModifier modifier : ATLAS_MODIFIERS) modifier.apply(player, true);
        state.atlasFood = player.getFoodData().getFoodLevel();
        Fx.play(player.level(), player, ShazamContent.ATLAS_ON.get(), 0.9F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.shazam_atlas_on").withStyle(ChatFormatting.GOLD), true);
        Vec3 at = player.position();
        Fx.ring(player.level(), at.add(0, 0.1, 0), 2.0, GOLD_DUST, 24);
    }

    private static void stopAtlas(ServerPlayer player, ItemStack item, State state) {
        item.remove(ShazamContent.ATLAS.get());
        for (SuitModifier modifier : ATLAS_MODIFIERS) modifier.apply(player, false);
        state.atlasFood = -1;
        Fx.play(player.level(), player, ShazamContent.ATLAS_OFF.get(), 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.shazam_atlas_off").withStyle(ChatFormatting.GRAY), true);
    }

    /** Wisdom of Solomon on: hostile creatures nearby glow, even behind walls. */
    private static void startSolomon(ServerPlayer player, ServerLevel level, ItemStack item) {
        item.set(ShazamContent.SOLOMON.get(), true);
        Fx.hear(player, ShazamContent.SOLOMON_ON.get(), 0.9F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.shazam_solomon_on").withStyle(ChatFormatting.GOLD), true);
        revealEnemies(player, level);
    }

    private static void stopSolomon(ServerPlayer player, ItemStack item) {
        item.remove(ShazamContent.SOLOMON.get());
        Fx.hear(player, ShazamContent.SOLOMON_OFF.get(), 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.greenlantern.shazam_solomon_off").withStyle(ChatFormatting.GRAY), true);
    }

    private static void revealEnemies(ServerPlayer player, ServerLevel level) {
        for (LivingEntity target : Targets.inRadius(level, player.position(), ShazamConfig.SOLOMON_RANGE.get(), e -> Targets.enemy(player, e))) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
        }
    }

    // ---- every tick --------------------------------------------------------------------------------------

    /** Lightning hurts everyone else: his magic drinks a natural bolt whole. Returns the share of damage he takes. */
    public static float damageTaken(ServerPlayer player, DamageSource source) {
        if (!ShazamHero.INSTANCE.isSuited(player)) return 1.0F;
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            ItemStack item = ShazamHero.INSTANCE.findItem(player);
            if (!item.isEmpty()) ShazamHero.ENERGY.set(item, ShazamHero.ENERGY.capacity());
            player.clearFire();
            ServerLevel level = player.level();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.9, 0.4, 0.2);
            player.displayClientMessage(Component.translatable("message.greenlantern.shazam_lightning").withStyle(ChatFormatting.GOLD), true);
            return 0.0F;
        }
        ItemStack item = ShazamHero.INSTANCE.findItem(player);
        if (!item.isEmpty() && isAtlas(item) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return ShazamConfig.ATLAS_DAMAGE_TAKEN.get().floatValue();
        }
        return 1.0F;
    }

    public static void tick(ServerPlayer player) {
        boolean suited = ShazamHero.INSTANCE.isSuited(player);
        if (!suited) {
            if (STATES.containsKey(player.getUUID())) remove(player);
            return;
        }
        ItemStack item = ShazamHero.INSTANCE.findItem(player);
        if (item.isEmpty()) return;
        State state = get(player);
        ServerLevel level = player.level();

        if (!state.bolts.isEmpty()) {
            state.bolts.tick(player, level, (from, to) -> arcSegment(level, from, to), impact -> zeusImpact(player, level, impact));
        }
        if (state.dash.active()) tickDash(player, level, state);

        if (player.tickCount % 20 == 0) {
            int recharge = level.isThundering() ? ShazamConfig.STORM_RECHARGE_PER_SECOND.get() : ShazamConfig.RECHARGE_PER_SECOND.get();
            ShazamHero.ENERGY.add(item, recharge);
            boolean full = ShazamHero.ENERGY.get(item).isFull();
            if (full && !state.wasFull) Fx.hear(player, ShazamContent.MAGIC_FULL.get(), 0.6F, 1.0F);
            state.wasFull = full;
            if (isAtlas(item)) tickAtlas(player, item, state);
            if (isSolomon(item)) tickSolomon(player, level, item);
        }
        if (isAtlas(item) && player.tickCount % 4 == 0) {
            level.sendParticles(GOLD_DUST, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.35, 0.05, 0.35, 0.0);
        }
        // Little arcs of lightning in his flight trail.
        if (player.getAbilities().flying && player.getDeltaMovement().lengthSqr() > 0.5 && player.tickCount % 2 == 0) {
            Vec3 back = player.position().add(0, 1, 0).subtract(player.getDeltaMovement().normalize().scale(1.2));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, back.x, back.y, back.z, 3, 0.25, 0.25, 0.25, 0.05);
        }
    }

    /** The trail of a bolt in the air: a crackling line of gold and white. */
    private static void arcSegment(ServerLevel level, Vec3 from, Vec3 to) {
        Fx.line(level, from, to, ParticleTypes.ELECTRIC_SPARK, 3);
        Fx.line(level, from, to, GOLD_DUST, 2);
        Fx.line(level, from, to, ParticleTypes.END_ROD, 0.7);
    }

    private static void tickAtlas(ServerPlayer player, ItemStack item, State state) {
        if (!player.isCreative() && !ShazamHero.ENERGY.tryConsume(item, ShazamPower.ATLAS_STAMINA.cost())) {
            stopAtlas(player, item, state);
            player.displayClientMessage(Component.translatable("message.greenlantern.no_shazam_magic").withStyle(ChatFormatting.RED), true);
            return;
        }
        for (SuitModifier modifier : ATLAS_MODIFIERS) modifier.apply(player, true);
        // He doesn't get hungry.
        if (state.atlasFood < 0) state.atlasFood = player.getFoodData().getFoodLevel();
        if (player.getFoodData().getFoodLevel() < state.atlasFood) player.getFoodData().setFoodLevel(state.atlasFood);
    }

    private static void tickSolomon(ServerPlayer player, ServerLevel level, ItemStack item) {
        if (!player.isCreative() && !ShazamHero.ENERGY.tryConsume(item, ShazamPower.SOLOMON_WISDOM.cost())) {
            stopSolomon(player, item);
            player.displayClientMessage(Component.translatable("message.greenlantern.no_shazam_magic").withStyle(ChatFormatting.RED), true);
            return;
        }
        revealEnemies(player, level);
    }

    /** Magic pays for the flight. */
    static void payFlight(ServerPlayer player, ItemStack item, float multiplier) {
        int cost = Math.round(ShazamConfig.FLIGHT_COST_PER_SECOND.get() * multiplier);
        if (cost <= 0) return;
        ShazamHero.ENERGY.drain(item, cost);
        if (ShazamHero.ENERGY.get(item).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.no_shazam_magic");
    }
}
