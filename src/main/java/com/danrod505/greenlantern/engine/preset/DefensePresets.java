package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Defense (defesa) presets: the bubble, the frontal block, hard skin, the wall and the reflection.
 * They put a {@link Guards.Guard} on the player while they last. Numbers: {@code duracao},
 * {@code raio}; the rest are settings, {@code reducao} (the share of the damage taken away) among them.
 */
public final class DefensePresets {
    private DefensePresets() {}

    public static void register() {
        Presets.registerFight("bolha", DefensePresets::bubble);
        Presets.registerFight("bloqueio", DefensePresets::block);
        Presets.registerFight("pele_dura", DefensePresets::hardSkin);
        Presets.registerFight("parede", DefensePresets::wall);
        Presets.registerFight("refletir", DefensePresets::reflect);
    }

    private static double reduction(PresetContext c, double fallback) {
        return Mth.clamp(c.settings().number("reducao", fallback), 0.0, 1.0);
    }

    // ---- bolha -------------------------------------------------------------------------------------

    /**
     * Bolha: a shield all around that takes away {@code reducao} (0.6) of every hit while it lasts,
     * up to {@code duracao} seconds (20), paying {@code gasto_por_segundo} (2) of the class energy;
     * the key turns it off. {@code raio} is its size (1.6). Settings: {@code devolve} (it also sends
     * projectiles back, false).
     */
    static Outcome bubble(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(20.0);
        double radius = c.radius(1.6);
        int drain = (int) Math.ceil(Math.max(0.0, s.number("gasto_por_segundo", 2.0)));
        Guards.Guard guard = new Guards.Guard(reduction(c, 0.6), 180.0, s.flag("devolve", false), 1.0);
        Guards.add(player, guard);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        ParticleOptions edge = PresetFx.particle(c.element(), c.color());
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
        Set<Integer> seen = new HashSet<>();
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (drain > 0 && age % 20 == 0 && !player.isCreative() && !MovementPresets.pay(c, drain)) return false;
            Guards.deflect(player, guard, seen);
            if (age % 2 == 0) sphere(level, player.position().add(0, 0.9, 0), radius, age % 4 == 0 ? particle : edge, 24, age);
            if (age % 40 == 0) Fx.play(level, player, SoundEvents.BEACON_AMBIENT, 0.4F, 1.6F);
            return age < ticks;
        }, () -> {
            Guards.remove(player, guard);
            sphere(level, player.position().add(0, 0.9, 0), radius, particle, 60, 0);
            Fx.play(level, player, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
        }));
    }

    /** Points spread over a sphere (a golden spiral that turns with {@code turn}). */
    static void sphere(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int count, int turn) {
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double r = Math.sqrt(1.0 - y * y);
            double a = golden * i + turn * 0.15;
            level.sendParticles(particle, center.x + Math.cos(a) * r * radius, center.y + y * radius * 1.1, center.z + Math.sin(a) * r * radius,
                    1, 0, 0, 0, 0);
        }
    }

    // ---- bloqueio ----------------------------------------------------------------------------------

    /**
     * Bloqueio: bracelets, a shield or a weapon held up in front for {@code duracao} seconds (2): hits
     * from within {@code angulo} degrees (120, the whole opening) lose {@code reducao} (0.8). With
     * {@code devolve} (false) projectiles from the front go back to who shot them.
     */
    static Outcome block(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(2.0);
        double half = Mth.clamp(s.number("angulo", 120.0) / 2.0, 10.0, 180.0);
        Guards.Guard guard = new Guards.Guard(reduction(c, 0.8), half, s.flag("devolve", false), 1.0);
        Guards.add(player, guard);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.8F);
        Set<Integer> seen = new HashSet<>();
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Guards.deflect(player, guard, seen);
            if (age % 2 == 0) {
                Vec3 eye = player.getEyePosition().add(0, -0.4, 0);
                Vec3 forward = Targets.horizontal(player);
                for (int i = -3; i <= 3; i++) {
                    Vec3 dir = forward.yRot((float) Math.toRadians(half * i / 3.0));
                    Vec3 at = eye.add(dir.scale(0.9));
                    level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0.3, 0, 0);
                }
            }
            return age < ticks;
        }, () -> Guards.remove(player, guard)));
        return Outcome.DONE;
    }

    // ---- pele_dura ---------------------------------------------------------------------------------

    /**
     * Pele dura: for {@code duracao} seconds (8) every hit loses {@code reducao} (0.4), with no shape to
     * see: a flash of the skin hardening and now and then a glint.
     */
    static Outcome hardSkin(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int ticks = c.durationTicks(8.0);
        Guards.Guard guard = new Guards.Guard(reduction(c, 0.4), 180.0, false, 1.0);
        Guards.add(player, guard);
        Vec3 mid = player.position().add(0, 1.0, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), mid.x, mid.y, mid.z, 30, 0.3, 0.6, 0.3, 0.1);
        PresetFx.burst(level, mid, c.element(), c.color(), 12, 0.4);
        Fx.play(level, player, SoundEvents.ANVIL_PLACE, 0.6F, 1.4F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (age % 15 == 0) {
                Vec3 at = player.position().add(0, 1.0, 0);
                level.sendParticles(ParticleTypes.WAX_ON, at.x, at.y, at.z, 4, 0.3, 0.6, 0.3, 0.0);
            }
            return age < ticks;
        }, () -> Guards.remove(player, guard)));
        return Outcome.DONE;
    }

    // ---- parede ------------------------------------------------------------------------------------

    /**
     * Parede: a wall of temporary blocks across the way, {@code distancia} blocks (2) in front,
     * {@code largura} (5) by {@code altura} (3), that goes away after {@code duracao} seconds (8). It only
     * fills empty places (air, grass, water) and puts back what was there. Settings:
     * {@code material} (energia, a glass in the character's colour; gelo, pedra, terra, areia,
     * madeira; or any block id). It fails, giving the energy back, when there is no room.
     */
    static Outcome wall(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int width = Mth.clamp((int) Math.round(s.number("largura", 5.0)), 1, 15);
        int height = Mth.clamp((int) Math.round(s.number("altura", 3.0)), 1, 8);
        int ticks = c.durationTicks(8.0);
        BlockState material = material(s.text("material", "energia"), c.color());
        Vec3 forward = Targets.horizontal(player);
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        Vec3 center = player.position().add(forward.scale(Math.max(1.0, s.number("distancia", 2.0)) + 0.5));
        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        for (int w = 0; w < width; w++) {
            Vec3 column = center.add(side.scale(w - (width - 1) / 2.0));
            BlockPos base = BlockPos.containing(column.x, player.getY() + 0.5, column.z);
            for (int h = 0; h < height; h++) {
                BlockPos pos = base.above(h);
                if (placed.containsKey(pos)) continue;
                BlockState was = level.getBlockState(pos);
                if (!was.canBeReplaced() || level.getBlockEntity(pos) != null || !level.mayInteract(player, pos)) continue;
                if (!level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(pos), e -> e instanceof net.minecraft.world.entity.LivingEntity).isEmpty()) continue;
                placed.put(pos.immutable(), was);
                level.setBlock(pos, material, Block.UPDATE_ALL);
            }
        }
        if (placed.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_room"));
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, material);
        for (BlockPos pos : placed.keySet()) level.sendParticles(dust, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, center.x, center.y, center.z, material.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> age < ticks, () -> {
            List<BlockPos> back = new ArrayList<>();
            placed.forEach((pos, was) -> {
                if (level.getBlockState(pos) == material) {
                    level.setBlock(pos, was, Block.UPDATE_ALL);
                    back.add(pos);
                }
            });
            for (BlockPos pos : back) level.sendParticles(dust, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
            if (!back.isEmpty()) level.playSound(null, center.x, center.y, center.z, material.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }));
        return Outcome.DONE;
    }

    /** The block of a wall: a word, a block id, or a glass in the colour nearest the character's. */
    static BlockState material(String name, int color) {
        return switch (name) {
            case "gelo" -> Blocks.PACKED_ICE.defaultBlockState();
            case "pedra" -> Blocks.COBBLESTONE.defaultBlockState();
            case "terra" -> Blocks.PACKED_MUD.defaultBlockState();
            case "areia" -> Blocks.SANDSTONE.defaultBlockState();
            case "madeira" -> Blocks.OAK_PLANKS.defaultBlockState();
            case "energia" -> glass(color);
            default -> {
                Identifier id = Identifier.tryParse(name);
                Block block = id != null ? BuiltInRegistries.BLOCK.getValue(id) : Blocks.AIR;
                yield block == Blocks.AIR ? glass(color) : block.defaultBlockState();
            }
        };
    }

    private static BlockState glass(int color) {
        DyeColor best = DyeColor.WHITE;
        double nearest = Double.MAX_VALUE;
        for (DyeColor dye : DyeColor.values()) {
            int d = dye.getTextureDiffuseColor();
            double dr = ARGB.red(d) - ARGB.red(color), dg = ARGB.green(d) - ARGB.green(color), db = ARGB.blue(d) - ARGB.blue(color);
            double distance = dr * dr + dg * dg + db * db;
            if (distance < nearest) {
                nearest = distance;
                best = dye;
            }
        }
        Block glass = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(best.getSerializedName() + "_stained_glass"));
        return (glass == Blocks.AIR ? Blocks.GLASS : glass).defaultBlockState();
    }

    // ---- refletir ----------------------------------------------------------------------------------

    /**
     * Refletir: for {@code duracao} seconds (1.5) projectiles that reach the character go back to who
     * shot them, now theirs. Settings: {@code chance} (1, each projectile's chance of going back).
     */
    static Outcome reflect(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int ticks = c.durationTicks(1.5);
        Guards.Guard guard = new Guards.Guard(0.0, 180.0, true, Mth.clamp(c.settings().number("chance", 1.0), 0.0, 1.0));
        Guards.add(player, guard);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
        Set<Integer> seen = new HashSet<>();
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Guards.deflect(player, guard, seen);
            Vec3 at = player.position().add(0, 1.0, 0);
            double a = age * 0.6;
            for (int i = 0; i < 3; i++) {
                double b = a + i * Mth.TWO_PI / 3;
                level.sendParticles(particle, at.x + Math.cos(b) * 1.1, at.y + Math.sin(age * 0.3 + i) * 0.6, at.z + Math.sin(b) * 1.1, 1, 0, 0, 0, 0);
            }
            if (age % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.6, 0.6, 0.6, 0.0);
            return age < ticks;
        }, () -> Guards.remove(player, guard)));
        return Outcome.DONE;
    }
}
