package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Natureza presets of the new model of abilities (2.6.0): taming and leading animals, calling the
 * weather and following a trail. Plantar e colher, also Natureza, is in {@link WorldPresets}.
 */
public final class NaturePresets {
    private NaturePresets() {}

    public static void register() {
        Presets.registerWorld("domar_e_guiar", NaturePresets::tame);
        Presets.registerWorld("clima", NaturePresets::weather);
        Presets.registerWorld("rastro", NaturePresets::trail);
    }

    // ---- domar_e_guiar -----------------------------------------------------------------------------

    /**
     * Domar e guiar: the animal aimed at within {@code alcance} (8), or else the {@code quantos} (4)
     * nearest within {@code raio} (6), are tamed when they can be (wolves, cats, parrots, horses) and
     * the others follow the player on a leash for {@code duracao} seconds (30). Monsters are never
     * touched.
     */
    static Outcome tame(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        List<Mob> animals = new ArrayList<>();
        if (c.aimedTarget(c.reach(8.0), 0.2) instanceof Animal aimed) animals.add(aimed);
        else {
            int many = Mth.clamp(s.whole("quantos", 4), 1, 12);
            for (LivingEntity e : Targets.inRadius(level, player.position(), c.radius(6.0), e -> e instanceof Animal && !Summons.summoned(e))) {
                if (animals.size() >= many) break;
                animals.add((Mob) e);
            }
        }
        List<Mob> led = new ArrayList<>();
        int done = 0;
        for (Mob animal : animals) {
            if (animal instanceof Enemy) continue;
            if (animal instanceof TamableAnimal pet && !pet.isTame()) {
                pet.tame(player);
                level.broadcastEntityEvent(pet, (byte) 7);
                done++;
            } else if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
                horse.tameWithName(player);
                level.broadcastEntityEvent(horse, (byte) 7);
                done++;
            } else if (animal instanceof Leashable leash && !leash.isLeashed() && leash.canBeLeashed()) {
                leash.setLeashedTo(player, true);
                led.add(animal);
                done++;
            }
        }
        if (done == 0) return WorldPresets.nothing();
        Fx.play(level, player, SoundEvents.LEAD_TIED, 1.0F, 1.0F);
        animals.forEach(a -> PresetFx.burst(c, a.position().add(0, a.getBbHeight() * 0.6, 0), 8, 0.4));
        if (!led.isEmpty()) {
            int ticks = c.durationTicks(30.0);
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> age < ticks && led.stream().anyMatch(m -> m.isAlive() && ((Leashable) m).getLeashHolder() == player), () -> {
                for (Mob m : led) {
                    if (m.isAlive() && ((Leashable) m).getLeashHolder() == player) ((Leashable) m).dropLeash();
                }
            }));
        }
        return Outcome.DONE;
    }

    // ---- clima -------------------------------------------------------------------------------------

    /**
     * Clima: calls {@code tipo} (chuva, sol or tempestade) over the world for {@code duracao}
     * seconds (120); then the weather goes on as it would. {@code dia} skips the night: the next
     * morning comes at once and the sky clears.
     */
    static Outcome weather(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return WorldPresets.nothing();
        int ticks = c.durationTicks(120.0);
        switch (c.settings().text("tipo", "chuva")) {
            case "dia" -> {
                long time = level.getDayTime() % 24000L;
                if (time < 12000L && !level.isRaining()) return WorldPresets.nothing();
                if (time >= 12000L) level.setDayTime(level.getDayTime() + 24000L - time);
                level.setWeatherParameters(ticks, 0, false, false);
            }
            case "sol" -> {
                if (!level.isRaining()) return WorldPresets.nothing();
                level.setWeatherParameters(ticks, 0, false, false);
            }
            case "tempestade" -> {
                if (level.isThundering()) return WorldPresets.nothing();
                level.setWeatherParameters(0, ticks, true, true);
            }
            default -> {
                if (level.isRaining() && !level.isThundering()) return WorldPresets.nothing();
                level.setWeatherParameters(0, ticks, true, false);
            }
        }
        Fx.play(level, player, SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.4F);
        PresetFx.burst(c, player.position().add(0, 2.5, 0), 40, 1.5);
        return Outcome.DONE;
    }

    // ---- rastro ------------------------------------------------------------------------------------

    /**
     * Rastro: for {@code duracao} seconds (10) a trail of particles leaves the player toward
     * {@code alvo}: mob (the nearest monster within {@code raio}, 48), estrutura (the nearest
     * {@code estrutura}, a structure tag, minecraft:village) or casa (where the player wakes up).
     */
    static Outcome trail(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String target = s.text("alvo", "mob");
        double radius = Math.min(96.0, c.radius(48.0));
        BlockPos fixed = null;
        if (target.equals("estrutura")) {
            Identifier id = Identifier.tryParse(s.text("estrutura", "minecraft:village").replace("#", ""));
            if (id == null) return WorldPresets.noTarget();
            fixed = level.findNearestMapStructure(TagKey.create(Registries.STRUCTURE, id), player.blockPosition(), 64, false);
        } else if (target.equals("casa")) {
            ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
            fixed = respawn != null && respawn.respawnData().dimension() == level.dimension()
                    ? respawn.respawnData().pos() : level.getRespawnData().pos();
        } else if (nearestMonster(c, radius) == null) {
            return WorldPresets.noTarget();
        }
        if (fixed == null && !target.equals("mob")) return WorldPresets.noTarget();
        BlockPos to = fixed;
        int ticks = c.durationTicks(10.0);
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        PresetRuntime.start(player, age -> {
            if (age % 5 == 1) {
                LivingEntity mob = to == null ? nearestMonster(c, radius) : null;
                Vec3 goal = to != null ? Vec3.atCenterOf(to) : mob != null ? mob.position() : null;
                if (goal != null) {
                    Vec3 from = player.position().add(0, 0.2, 0);
                    Vec3 way = goal.subtract(from);
                    double length = Math.min(12.0, way.length());
                    if (length > 0.5) Fx.line(level, from, from.add(way.normalize().scale(length)), PresetFx.particle(c), 2.0);
                }
            }
            return age < ticks;
        });
        return Outcome.DONE;
    }

    private static LivingEntity nearestMonster(PresetContext c, double radius) {
        ServerPlayer player = c.player();
        LivingEntity best = null;
        for (LivingEntity e : Targets.inRadius(c.level(), player.position(), radius, e -> e instanceof Enemy && !Summons.summoned(e))) {
            if (best == null || e.distanceToSqr(player) < best.distanceToSqr(player)) best = e;
        }
        return best;
    }

}
