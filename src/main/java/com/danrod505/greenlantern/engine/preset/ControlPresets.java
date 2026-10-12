package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Control (controle) presets: pull, hold, slow, fear and confusion, dominate and levitate. Most act
 * on the creature the player aims at ({@code alcance}); with nothing aimed they fail and give the
 * energy back. Numbers: {@code alcance}, {@code raio}, {@code duracao}, {@code dano}.
 */
public final class ControlPresets {
    /** The prisons that hold a creature now, by the creature: how much damage they still take. */
    private static final Map<UUID, float[]> PRISONS = new ConcurrentHashMap<>();
    /** The creatures under fear or confusion now: they leave the players alone. */
    private static final Map<UUID, Object> SPELLBOUND = new ConcurrentHashMap<>();

    private ControlPresets() {}

    public static void register() {
        Presets.registerFight("puxar", ControlPresets::pull);
        Presets.registerFight("prender", ControlPresets::hold);
        Presets.registerFight("lentidao", ControlPresets::slow);
        Presets.registerFight("medo_e_confusao", ControlPresets::fear);
        Presets.registerFight("dominar", ControlPresets::dominate);
        Presets.registerFight("levitar", ControlPresets::levitate);
    }

    static void events() {
        LivingHurtEvent.BUS.addListener(event -> {
            float[] prison = PRISONS.get(event.getEntity().getUUID());
            if (prison != null) prison[0] -= event.getAmount();
        });
        LivingChangeTargetEvent.BUS.addListener(event -> event.getNewTarget() instanceof Player
                && SPELLBOUND.containsKey(event.getEntity().getUUID()));
    }

    private static Outcome noTarget() {
        return Outcome.fail(Component.translatable("message.greenlantern.engine_no_target"));
    }

    // ---- puxar -------------------------------------------------------------------------------------

    /**
     * Puxar: a lasso, whip or vine to the creature aimed at within {@code alcance} (12) that pulls it
     * to the character. Settings: {@code forca} (1.2), {@code laco} (the line's element, the
     * ability's by default). With {@code dano} it also hurts.
     */
    static Outcome pull(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        LivingEntity target = c.aimedTarget(c.reach(12.0), 12.0);
        if (target == null) return noTarget();
        Vec3 to = player.position().subtract(target.position());
        double distance = to.length();
        double strength = Math.min(Math.max(0.2, c.settings().number("forca", 1.2)), distance * 0.2 + 0.3);
        if (c.damage() > 0) c.hit(target, c.damage());
        else PresetFx.elementHit(c.element(), target, player);
        Hits.fling(target, to, strength, 0.35);
        ParticleOptions line = PresetFx.particle(c.settings().text("laco", c.element()), c.lightColor());
        Fx.line(level, c.muzzle(), Targets.middle(target), line, 4.0);
        Fx.play(level, player, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.8F);
        Fx.play(level, target, SoundEvents.LEAD_TIED, 1.0F, 1.0F);
        return Outcome.DONE;
    }

    // ---- prender -----------------------------------------------------------------------------------

    /**
     * Prender: a cage, ice, vines or sand that holds the aimed creature (within {@code alcance}, 12)
     * still for {@code duracao} seconds (4), unless it takes {@code vida} damage (10) first, which
     * breaks it. Settings: {@code material} (energia, gelo, vinha, areia), {@code vida}.
     */
    static Outcome hold(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        LivingEntity target = c.aimedTarget(c.reach(12.0), 12.0);
        if (target == null) return noTarget();
        int ticks = c.durationTicks(4.0);
        String material = c.settings().text("material", "energia");
        float[] prison = {(float) Math.max(1.0, c.settings().number("vida", 10.0))};
        PRISONS.put(target.getUUID(), prison);
        Vec3 anchor = target.position();
        if (target instanceof Mob mob) Hits.stun(mob, player, ticks);
        if (c.damage() > 0) c.hit(target, c.damage());
        ParticleOptions particle = prisonParticle(material, c.color());
        Fx.play(level, target, switch (material) {
            case "gelo" -> SoundEvents.GLASS_PLACE;
            case "vinha" -> SoundEvents.CAVE_VINES_PLACE;
            case "areia" -> SoundEvents.SAND_PLACE;
            default -> SoundEvents.BEACON_POWER_SELECT;
        }, 1.0F, 1.2F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (!target.isAlive() || prison[0] <= 0) return false;
            target.teleportTo(anchor.x, anchor.y, anchor.z);
            target.setDeltaMovement(Vec3.ZERO);
            if (age % 3 == 0) cage(level, target, particle, age);
            return age < ticks;
        }, () -> {
            PRISONS.remove(target.getUUID(), prison);
            if (target.isAlive()) {
                target.removeEffect(MobEffects.SLOWNESS);
                level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                Fx.play(level, target, prison[0] <= 0 ? SoundEvents.GLASS_BREAK : SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.2F);
            }
        }));
        return Outcome.DONE;
    }

    private static ParticleOptions prisonParticle(String material, int color) {
        return switch (material) {
            case "gelo" -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
            case "vinha" -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState());
            case "areia" -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
            default -> PresetFx.particle(PresetFx.ENERGY, color);
        };
    }

    /** Bars of particles around a creature. */
    private static void cage(ServerLevel level, LivingEntity target, ParticleOptions particle, int age) {
        double r = target.getBbWidth() * 0.8 + 0.3;
        double h = target.getBbHeight() + 0.2;
        for (int i = 0; i < 8; i++) {
            double a = Mth.TWO_PI * i / 8 + age * 0.02;
            for (double y = 0; y <= h; y += 0.35) {
                level.sendParticles(particle, target.getX() + Math.cos(a) * r, target.getY() + y, target.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- lentidao ----------------------------------------------------------------------------------

    /**
     * Lentidão: the aimed creature (within {@code alcance}, 16), or everyone within {@code raio} of the
     * character when it has one, slows down more and more for {@code duracao} seconds (5), up to
     * {@code forca} (3). With the element gelo it freezes as well.
     */
    static Outcome slow(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        List<LivingEntity> targets;
        if (c.numbers().radius() > 0) targets = Targets.inRadius(level, player.position(), c.numbers().radius(), c::hittable);
        else {
            LivingEntity one = c.aimedTarget(c.reach(16.0), 12.0);
            targets = one == null ? List.of() : List.of(one);
        }
        if (targets.isEmpty()) return noTarget();
        int ticks = c.durationTicks(5.0);
        int strength = Mth.clamp(c.settings().whole("forca", 3), 0, 6);
        boolean ice = c.element().equals("gelo");
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        if (c.damage() > 0) targets.forEach(t -> c.hit(t, c.damage()));
        Fx.play(level, player, ice ? SoundEvents.POWDER_SNOW_STEP : SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.8F);
        PresetRuntime.start(player, age -> {
            int level0 = Math.min(strength, strength * age / Math.max(1, ticks / 2));
            for (LivingEntity target : targets) {
                if (!target.isAlive()) continue;
                if (age % 10 == 1) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, level0, false, true), player);
                if (ice) target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 20, target.getTicksFrozen() + 6));
                if (age % 4 == 0) level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 3, 0.3, 0.4, 0.3, 0.0);
            }
            return age < ticks;
        });
        return Outcome.DONE;
    }

    // ---- medo_e_confusao ---------------------------------------------------------------------------

    /**
     * Medo e confusão: the creatures within {@code raio} (8) run away from the character
     * ({@code tipo} medo) or attack each other at random ({@code tipo} confusao) for {@code duracao}
     * seconds (5). The element gives the look (gas, illusion...).
     */
    static Outcome fear(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(8.0);
        int ticks = c.durationTicks(5.0);
        boolean confusion = c.settings().text("tipo", "medo").equals("confusao");
        List<Mob> mobs = Targets.inRadius(level, player.position(), radius, c::hittable).stream()
                .filter(e -> e instanceof Mob).map(e -> (Mob) e).toList();
        if (mobs.isEmpty()) return noTarget();
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, confusion ? SoundEvents.ILLUSIONER_MIRROR_MOVE : SoundEvents.WARDEN_ROAR, 0.8F, 1.4F);
        if (c.damage() > 0) mobs.forEach(m -> c.hit(m, c.damage()));
        Object spell = new Object();
        mobs.forEach(m -> SPELLBOUND.put(m.getUUID(), spell));
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            for (Mob mob : mobs) {
                if (!mob.isAlive()) continue;
                if (age % 10 == 1) {
                    if (confusion) {
                        Mob other = mobs.stream().filter(m -> m != mob && m.isAlive())
                                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
                        mob.setTarget(other);
                    } else {
                        mob.setTarget(null);
                        Vec3 away = mob.position().subtract(player.position());
                        Vec3 to = mob.position().add(new Vec3(away.x, 0, away.z).normalize().scale(8.0));
                        mob.getNavigation().moveTo(to.x, to.y, to.z, 1.5);
                    }
                }
                if (age % 5 == 0) level.sendParticles(confusion ? ParticleTypes.WITCH : ParticleTypes.ANGRY_VILLAGER,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                if (age % 6 == 0) level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
            }
            return age < ticks;
        }, () -> {
            for (Mob mob : mobs) {
                SPELLBOUND.remove(mob.getUUID(), spell);
                if (mob.isAlive() && confusion) mob.setTarget(null);
            }
        }));
        return Outcome.DONE;
    }

    // ---- dominar -----------------------------------------------------------------------------------

    /**
     * Dominar: the aimed creature (within {@code alcance}, 12) fights for the character for
     * {@code duracao} seconds (15), if its health is at most {@code vida_maxima} (40).
     */
    static Outcome dominate(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        LivingEntity aimed = c.aimedTarget(c.reach(12.0), 12.0);
        if (!(aimed instanceof Mob mob)) return noTarget();
        if (mob.getMaxHealth() > c.settings().number("vida_maxima", 40.0)) {
            return Outcome.fail(Component.translatable("message.greenlantern.engine_too_strong", mob.getDisplayName()));
        }
        int ticks = c.durationTicks(15.0);
        mob.setTarget(null);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.line(level, player.getEyePosition(), mob.getEyePosition(), particle, 4.0);
        Fx.play(level, mob, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.0F, 1.0F);
        Summons.adopt(c, mob, ticks, false, age -> {
            if (age % 4 == 0) level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() + 0.4, mob.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        });
        return Outcome.DONE;
    }

    // ---- levitar -----------------------------------------------------------------------------------

    /**
     * Levitar: lifts the aimed creature (health at most {@code peso}, 40) or block {@code altura}
     * blocks (3) into the air and holds it there for {@code duracao} seconds (4); then it falls.
     * Within {@code alcance} (16).
     */
    static Outcome levitate(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(16.0);
        double height = Mth.clamp(c.settings().number("altura", 3.0), 0.5, 12.0);
        int ticks = c.durationTicks(4.0);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        LivingEntity creature = c.aimedTarget(reach, 8.0);
        if (creature != null) {
            if (creature.getMaxHealth() > c.settings().number("peso", 40.0)) {
                return Outcome.fail(Component.translatable("message.greenlantern.engine_too_heavy", creature.getDisplayName()));
            }
            if (creature instanceof Mob mob) Hits.stun(mob, player, ticks);
            Vec3 start = creature.position();
            creature.setNoGravity(true);
            Fx.play(level, creature, SoundEvents.SHULKER_BULLET_HIT, 1.0F, 0.6F);
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
                if (!creature.isAlive()) return false;
                double up = height * Math.min(1.0, age / 15.0);
                creature.teleportTo(start.x, start.y + up + Math.sin(age * 0.2) * 0.1, start.z);
                creature.setDeltaMovement(Vec3.ZERO);
                creature.resetFallDistance();
                if (age % 2 == 0) level.sendParticles(particle, creature.getX(), creature.getY() - 0.1, creature.getZ(), 3, 0.3, 0.1, 0.3, 0.0);
                return age < ticks;
            }, () -> creature.setNoGravity(false)));
            return Outcome.DONE;
        }
        HitResult aim = c.aim(reach);
        if (!(aim instanceof BlockHitResult hit) || aim.getType() != HitResult.Type.BLOCK) return noTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null || !level.mayInteract(player, pos)) {
            return noTarget();
        }
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.setNoGravity(true);
        block.disableDrop();
        Vec3 start = block.position();
        Fx.play(level, player, SoundEvents.SHULKER_BULLET_HIT, 1.0F, 0.6F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (block.isRemoved()) return false;
            block.time = 1;
            double up = height * Math.min(1.0, age / 15.0);
            block.setPos(start.x, start.y + up, start.z);
            block.setDeltaMovement(Vec3.ZERO);
            block.hurtMarked = true;
            if (age % 2 == 0) level.sendParticles(particle, block.getX(), block.getY() - 0.1, block.getZ(), 3, 0.3, 0.1, 0.3, 0.0);
            return age < ticks;
        }, () -> block.setNoGravity(false)));
        return Outcome.DONE;
    }

    /** Whether a prison holds this creature now (for the tests). */
    public static boolean held(LivingEntity entity) {
        return PRISONS.containsKey(entity.getUUID());
    }
}
