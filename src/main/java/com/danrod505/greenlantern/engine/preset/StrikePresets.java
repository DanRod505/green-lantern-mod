package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Dash;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Strike (golpe) presets, close combat: strong punch, charge, weapon swing, stomp, grab and throw,
 * and the 3-blow combo. Numbers: {@code dano}, {@code alcance}, {@code raio}; the rest are settings.
 */
public final class StrikePresets {
    private StrikePresets() {}

    public static void register() {
        Presets.registerFight("soco_forte", StrikePresets::strongPunch);
        Presets.registerFight("investida", StrikePresets::charge);
        Presets.registerFight("golpe_de_arma", StrikePresets::weaponSwing);
        Presets.registerFight("pisao", StrikePresets::stomp);
        Presets.registerFight("agarrar_e_arremessar", StrikePresets::grabAndThrow);
        Presets.registerFight("sequencia", StrikePresets::combo);
    }

    /**
     * Soco forte: one blow at the creature in front, which flies back. Settings: {@code empurrao}
     * (push, 1.5), {@code altura} (lift, 0.35), {@code tremor} (camera shake, 0.35).
     */
    static Outcome strongPunch(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        LivingEntity target = c.aimedTarget(c.reach(4.0), 25.0);
        Vec3 fist = player.getEyePosition().add(0, -0.3, 0).add(player.getLookAngle().scale(1.1));
        if (target == null) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, fist.x, fist.y, fist.z, 1, 0, 0, 0, 0);
            Fx.play(level, player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.8F);
            return Outcome.DONE;
        }
        Vec3 at = Targets.middle(target);
        c.hit(target, c.damage());
        Hits.fling(target, Targets.horizontal(player), s.number("empurrao", 1.5), s.number("altura", 0.35));
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.4);
        PresetFx.burst(c, at, 16, 0.35);
        Fx.play(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        Fx.play(level, target, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.6F);
        PresetFx.shake(player, (float) s.number("tremor", 0.35), 8);
        return Outcome.DONE;
    }

    /**
     * Investida: a run in a straight line that rams everyone in the way. {@code alcance} is the
     * distance (8). Settings: {@code velocidade} (blocks per tick, 1.2), {@code empurrao} (1.0).
     */
    static Outcome charge(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double distance = c.reach(8.0);
        double speed = Math.max(0.2, c.settings().number("velocidade", 1.2));
        double push = c.settings().number("empurrao", 1.0);
        int ticks = Math.max(2, Math.min(60, (int) Math.ceil(distance / speed)));
        Dash dash = new Dash();
        dash.start(player, player.getLookAngle(), distance, ticks);
        Fx.play(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        PresetRuntime.start(player, age -> {
            Vec3 before = player.position();
            boolean going = dash.tick(player, level, (target, direction) -> {
                c.hit(target, c.damage());
                Hits.fling(target, direction, push, 0.3);
                Fx.play(level, target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
                Vec3 at = Targets.middle(target);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.3);
            });
            Fx.line(level, before.add(0, 1.0, 0), player.position().add(0, 1.0, 0), PresetFx.particle(c), 2.0);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            return going;
        });
        return Outcome.DONE;
    }

    /**
     * Golpe de arma: an arc in front that cuts everyone in it. Settings: {@code arma} (espada,
     * machado, tridente, maca: the weapon shown and its sound), {@code item} (any item id to show
     * instead), {@code angulo} (the whole arc in degrees, 120). {@code alcance} is the reach (3.5).
     */
    static Outcome weaponSwing(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String weapon = s.text("arma", "espada");
        double reach = c.reach(3.5);
        double half = Math.max(10.0, s.number("angulo", 120.0) / 2.0);
        ItemStack shown = new ItemStack(item(s.text("item", ""), switch (weapon) {
            case "machado" -> Items.IRON_AXE;
            case "tridente" -> Items.TRIDENT;
            case "maca" -> Items.MACE;
            default -> Items.IRON_SWORD;
        }));
        Vec3 eye = player.getEyePosition();
        Vec3 forward = Targets.horizontal(player);
        List<LivingEntity> hit = Targets.inCone(level, eye.add(0, -0.6, 0), forward, reach + 0.5, half, c::hittable);
        for (LivingEntity target : hit) {
            c.hit(target, c.damage());
            switch (weapon) {
                case "maca" -> Hits.knockAway(target, player.position(), 0.6, 0.55);
                case "machado" -> Hits.knockAway(target, player.position(), 0.8, 0.2);
                default -> Hits.knockAway(target, player.position(), 0.45, 0.15);
            }
        }
        SoundEvent sound = switch (weapon) {
            case "machado" -> SoundEvents.PLAYER_ATTACK_CRIT;
            case "tridente" -> SoundEvents.TRIDENT_THROW.value();
            case "maca" -> SoundEvents.MACE_SMASH_GROUND;
            default -> SoundEvents.PLAYER_ATTACK_SWEEP;
        };
        Fx.play(level, player, sound, 1.0F, 1.0F);
        ItemParticleOption bits = new ItemParticleOption(ParticleTypes.ITEM, shown);
        // The arc sweeps from one side to the other over 4 ticks.
        PresetRuntime.start(player, age -> {
            for (int i = 0; i < 5; i++) {
                double t = ((age - 1) * 5 + i) / 19.0;
                double angle = Math.toRadians(-half + 2 * half * t);
                Vec3 dir = forward.yRot((float) -angle);
                Vec3 at = player.getEyePosition().add(0, -0.5, 0).add(dir.scale(Math.min(reach, 2.4)));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                level.sendParticles(bits, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.05);
                PresetFx.burst(c, at, 2, 0.1);
            }
            return age < 4;
        });
        return Outcome.DONE;
    }

    /**
     * Pisão: a blow on the ground that throws everyone around up. In the air the character first
     * dives and stomps on landing. {@code raio} is the reach (4). Settings: {@code altura} (lift, 0.9).
     */
    static Outcome stomp(PresetContext c) {
        ServerPlayer player = c.player();
        if (player.onGround()) {
            stompNow(c);
            return Outcome.DONE;
        }
        player.setDeltaMovement(player.getDeltaMovement().x * 0.3, -2.2, player.getDeltaMovement().z * 0.3);
        player.hurtMarked = true;
        Fx.play(c.level(), player, SoundEvents.PHANTOM_FLAP, 1.0F, 0.6F);
        PresetRuntime.start(player, age -> {
            player.resetFallDistance();
            if (player.onGround() || player.isInWater()) {
                stompNow(c);
                return false;
            }
            if (age % 2 == 0) PresetFx.burst(c, player.position().add(0, 1, 0), 4, 0.3);
            return age < 80;
        });
        return Outcome.DONE;
    }

    private static void stompNow(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(4.0);
        double lift = c.settings().number("altura", 0.9);
        Vec3 center = player.position();
        for (LivingEntity target : Targets.inRadius(level, center.add(0, 0.5, 0), radius + 0.5, c::hittable)) {
            c.hit(target, c.damage());
            Hits.knockAway(target, center, 0.4, lift);
        }
        player.resetFallDistance();
        Fx.dust(level, center, radius, 50);
        Fx.ring(level, center.add(0, 0.15, 0), radius, ParticleTypes.CLOUD, 32);
        Fx.ring(level, center.add(0, 0.3, 0), radius, PresetFx.particle(c), 24);
        Fx.play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.6F);
        Fx.play(level, player, SoundEvents.ANVIL_LAND, 0.5F, 0.5F);
        PresetFx.shake(player, 0.5F, 12);
    }

    /**
     * Agarrar e arremessar: picks up the creature in front (or a loose block) over the head and
     * throws it far; it gets the damage when it lands. Settings: {@code peso_maximo} (the most health
     * a creature can have to be lifted, 60), {@code distancia} (how far, 12), {@code blocos} (also
     * blocks, true). {@code alcance} is the reach (5).
     */
    static Outcome grabAndThrow(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double reach = c.reach(5.0);
        double strength = Math.max(0.3, s.number("distancia", 12.0) * 0.12);
        LivingEntity target = c.aimedTarget(reach, 20.0);
        if (target != null) {
            if (target.getMaxHealth() > s.number("peso_maximo", 60.0)) {
                return Outcome.fail(Component.translatable("message.greenlantern.engine_too_heavy", target.getDisplayName()));
            }
            grabCreature(c, target, strength);
            return Outcome.DONE;
        }
        HitResult aim = c.aim(reach);
        if (s.flag("blocos", true) && aim instanceof BlockHitResult block) {
            BlockPos pos = block.getBlockPos();
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness >= 0 && hardness <= 5.0F && !state.hasBlockEntity() && !state.isAir() && state.getFluidState().isEmpty()
                    && level.mayInteract(player, pos)) {
                FallingBlockEntity thrown = FallingBlockEntity.fall(level, pos, state);
                thrown.setHurtsEntities(2.0F, Math.max(4, (int) c.damage()));
                Vec3 look = player.getLookAngle();
                thrown.setDeltaMovement(look.x * strength, Math.max(0.3, look.y * strength + 0.3), look.z * strength);
                thrown.hurtMarked = true;
                Fx.play(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
                return Outcome.DONE;
            }
        }
        return Outcome.fail(Component.translatable("message.greenlantern.engine_nothing_to_grab"));
    }

    private static void grabCreature(PresetContext c, LivingEntity target, double strength) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        Fx.play(level, player, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.6F);
        int hold = 12;
        boolean[] thrown = {false};
        PresetRuntime.start(player, age -> {
            if (!target.isAlive()) return false;
            if (age <= hold) {
                Vec3 above = player.position().add(0, player.getBbHeight() + 0.3, 0).add(Targets.horizontal(player).scale(0.3));
                target.teleportTo(above.x, above.y, above.z);
                target.setDeltaMovement(Vec3.ZERO);
                target.resetFallDistance();
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
                return true;
            }
            if (!thrown[0]) {
                thrown[0] = true;
                Hits.fling(target, player.getLookAngle().add(0, 0.25, 0), strength, 0.2);
                Fx.play(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
                return true;
            }
            PresetFx.burst(c, Targets.middle(target), 2, 0.2);
            boolean landed = age > hold + 3 && (target.onGround() || target.horizontalCollision || target.isInWater());
            if (landed || age > hold + 60) {
                c.hit(target, c.damage());
                Fx.dust(level, target.position(), 1.5, 20);
                Fx.play(level, target, SoundEvents.GENERIC_SMALL_FALL, 1.0F, 0.6F);
                return false;
            }
            return true;
        });
    }

    /** Where each player is in their combo: the blow they gave last and when. */
    private record ComboStep(int blow, long at) {}

    private static final Map<UUID, ComboStep> COMBOS = new ConcurrentHashMap<>();

    /**
     * Sequência: a combo of blows; pressing again in rhythm gives the next one, the last one is the
     * strongest and only then the ability cools down. Settings: {@code golpes} (3), {@code intervalo}
     * (seconds to press again, 1.0), {@code danos} (the damage of each blow; the last one is 1.5 times
     * {@code dano} when left out). {@code alcance} is the reach (3.5).
     */
    static Outcome combo(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int blows = Math.max(1, s.whole("golpes", 3));
        long now = level.getGameTime();
        ComboStep last = COMBOS.get(player.getUUID());
        int window = (int) Math.round(s.number("intervalo", 1.0) * 20.0);
        int blow = last != null && now - last.at() <= window && last.blow() < blows ? last.blow() + 1 : 1;
        List<Double> damages = s.numbers("danos");
        float damage = blow <= damages.size() ? damages.get(blow - 1).floatValue() : blow == blows && blows > 1 ? c.damage() * 1.5F : c.damage();
        boolean finisher = blow == blows;

        LivingEntity target = c.aimedTarget(c.reach(3.5), 35.0);
        Vec3 at = target != null ? Targets.middle(target) : player.getEyePosition().add(player.getLookAngle().scale(1.5));
        if (target != null) {
            c.hit(target, damage);
            if (finisher) {
                Hits.fling(target, Targets.horizontal(player), 1.2, 0.4);
            } else {
                Hits.knockAway(target, player.position(), 0.15, 0.05);
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6 * blow, 0.25, 0.25, 0.25, 0.3);
        PresetFx.burst(c, at, 4 * blow, 0.25);
        Fx.play(level, player, finisher ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 0.9F + 0.15F * blow);
        if (finisher) {
            COMBOS.remove(player.getUUID());
            if (target != null) level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            PresetFx.shake(player, 0.3F, 6);
            return Outcome.DONE;
        }
        COMBOS.put(player.getUUID(), new ComboStep(blow, now));
        player.displayClientMessage(Component.translatable("message.greenlantern.engine_combo", blow, blows), true);
        return Outcome.cooldown(Math.min(6, Math.max(2, window / 2)));
    }

    /** The item of an id ({@code "minecraft:iron_sword"}), or {@code fallback} when it is empty or unknown. */
    static Item item(String id, Item fallback) {
        if (id.isEmpty()) return fallback;
        Identifier key = Identifier.tryParse(id);
        if (key == null) return fallback;
        return BuiltInRegistries.ITEM.getOptional(key).orElse(fallback);
    }
}
