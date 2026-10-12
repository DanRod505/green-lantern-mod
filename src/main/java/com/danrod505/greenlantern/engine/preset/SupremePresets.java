package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * The two pieces of the supreme abilities: the great blow and the great work. Since the new model
 * of abilities they are ordinary presets: an ability marked supreme (any class) names them like any
 * other, with a bigger cost and cooldown, and so may an ordinary one. The great work is a Construção.
 */
public final class SupremePresets {
    /** The most blocks a great work places. */
    static final int MAX_BLOCKS = 8000;

    private SupremePresets() {}

    public static void register() {
        Presets.registerFight("suprema_de_combate", SupremePresets::greatBlow);
        Presets.registerWorld("grande_obra", SupremePresets::greatWork);
    }

    // ---- suprema_de_combate ------------------------------------------------------------------------

    /**
     * Suprema de combate: the character gathers power for {@code carga} seconds (1) and lets it all
     * out around them: {@code dano} (20) to everyone within {@code raio} (8), less toward the edge,
     * thrown far away, with the camera shaking for whoever is near. Settings: {@code visual}
     * (energia, solar or raio; energia).
     */
    static Outcome greatBlow(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double radius = c.radius(8.0);
        float damage = c.damage() > 0 ? c.damage() : 20.0F;
        int charge = Math.max(1, (int) Math.round(Math.max(0.0, s.number("carga", 1.0)) * 20.0));
        String look = s.text("visual", "energia");
        ParticleOptions particle = switch (look) {
            case "solar" -> ParticleTypes.FLAME;
            case "raio" -> ParticleTypes.ELECTRIC_SPARK;
            default -> new DustParticleOptions(c.color(), 1.6F);
        };
        ParticleOptions light = switch (look) {
            case "solar" -> ParticleTypes.END_ROD;
            case "raio" -> ParticleTypes.ELECTRIC_SPARK;
            default -> new DustParticleOptions(c.lightColor(), 1.2F);
        };
        Fx.play(level, player, SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 0.8F);
        Fx.play(level, player, SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.6F);
        PresetRuntime.start(player, age -> {
            Vec3 center = player.position().add(0, 1.0, 0);
            if (age <= charge) {
                double r = 3.0 * (1.0 - (double) age / charge) + 0.4;
                for (int i = 0; i < 10; i++) {
                    double a = Mth.TWO_PI * i / 10 + age * 0.4;
                    level.sendParticles(i % 2 == 0 ? particle : light, center.x + Math.cos(a) * r, center.y + Math.sin(age * 0.3 + i) * 0.8,
                            center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
                return true;
            }
            int wave = age - charge;
            if (wave == 1) release(c, center, radius, damage, look);
            double r = radius * wave / 8.0;
            for (int i = 0; i < 48; i++) {
                double a = Mth.TWO_PI * i / 48;
                level.sendParticles(i % 3 == 0 ? light : particle, center.x + Math.cos(a) * r, center.y - 0.6, center.z + Math.sin(a) * r, 1, 0, 0.2, 0, 0);
            }
            return wave < 8;
        });
        return Outcome.DONE;
    }

    /** The peak of the great blow: the hit, the flash and the noise. */
    private static void release(PresetContext c, Vec3 center, double radius, float damage, String look) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        for (LivingEntity target : Targets.inRadius(level, center, radius + 0.5, c::hittable)) {
            double near = 1.0 - 0.4 * Targets.middle(target).distanceTo(center) / (radius + 0.5);
            c.hit(target, (float) (damage * near));
            if (look.equals("solar")) target.igniteForSeconds(5.0F);
            Hits.knockAway(target, center, 2.0 * near, 0.6);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z, 60, 0.3, 0.3, 0.3, 0.4);
        Fx.play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
        Fx.play(level, player, SoundEvents.TOTEM_USE, 0.8F, 0.8F);
        if (look.equals("raio")) {
            for (int i = 0; i < 5; i++) {
                double a = Mth.TWO_PI * i / 5;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt == null) continue;
                bolt.setVisualOnly(true);
                bolt.setPos(center.x + Math.cos(a) * radius * 0.6, Fx.groundBelow(level, center), center.z + Math.sin(a) * radius * 0.6);
                level.addFreshEntity(bolt);
            }
        }
        Fx.dust(level, player.position(), radius * 0.6, 40);
        for (ServerPlayer near : level.players()) {
            if (near.distanceTo(player) < radius * 3) PresetFx.shake(near, near == player ? 0.7F : 0.5F, 20);
        }
    }

    // ---- grande_obra -------------------------------------------------------------------------------

    /** One block of a great work: where it goes and what it is. */
    record Piece(BlockPos pos, BlockState state) {}

    /**
     * Grande obra: builds a structure from a file ({@code estrutura}, a structure of the game or of a
     * data pack; {@code minecraft:igloo/top}) in front of the character, {@code distancia} blocks away
     * (3), turned to where they look, piece by piece over {@code tempo} seconds (4), from the bottom
     * up. It only fills air, water and plants: it never takes a block away. Pressing the ability again
     * takes the work down (only the pieces still as they were built).
     */
    static Outcome greatWork(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        Identifier id = Identifier.tryParse(s.text("estrutura", "minecraft:igloo/top"));
        Optional<StructureTemplate> template = id == null ? Optional.empty() : level.getStructureManager().get(id);
        if (template.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_structure"));
        List<Piece> plan = plan(level, template.get(), player, Math.max(1, (int) Math.round(s.number("distancia", 3.0))));
        if (plan.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_space"));
        int ticks = Math.max(1, (int) Math.round(Math.max(0.0, s.number("tempo", 4.0)) * 20.0));
        Map<BlockPos, BlockState> built = new LinkedHashMap<>();
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.8F);
        PresetRuntime.Task task = PresetRuntime.Task.ending(age -> {
            int upTo = Math.min(plan.size(), (int) Math.ceil((double) plan.size() * age / ticks));
            for (int i = built.size(); i < upTo; i++) {
                Piece piece = plan.get(i);
                if (!level.getBlockState(piece.pos()).canBeReplaced()) {
                    built.put(piece.pos().immutable(), null);
                    continue;
                }
                level.setBlock(piece.pos(), piece.state(), Block.UPDATE_CLIENTS);
                built.put(piece.pos(), piece.state());
                if (i % 3 == 0) {
                    level.sendParticles(particle, piece.pos().getX() + 0.5, piece.pos().getY() + 0.5, piece.pos().getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
                }
            }
            if (age % 4 == 0 && upTo < plan.size()) {
                BlockPos at = plan.get(upTo).pos();
                level.playSound(null, at, piece(plan.get(upTo).state()), SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return upTo < plan.size();
        }, () -> Fx.play(level, player, SoundEvents.PLAYER_LEVELUP, 0.7F, 1.2F));
        PresetRuntime.start(player, task);
        PresetRuntime.undoable(c, () -> {
            PresetRuntime.cancel(player, task);
            int removed = 0;
            for (Map.Entry<BlockPos, BlockState> e : built.entrySet()) {
                if (e.getValue() == null || level.getBlockState(e.getKey()) != e.getValue()) continue;
                level.setBlock(e.getKey(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (removed++ % 4 == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, e.getValue()), e.getKey().getX() + 0.5, e.getKey().getY() + 0.5,
                            e.getKey().getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
            Fx.play(level, player, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_dismantled"), true);
        });
        return Outcome.DONE;
    }

    private static SoundEvent piece(BlockState state) {
        return state.getSoundType().getPlaceSound();
    }

    /**
     * The pieces of a structure in front of the player, turned to where they look and centred on
     * their line of sight, from the bottom up and from near to far; air, structure voids and the
     * structure's own technical blocks are left out, and so are the places taken by solid blocks.
     */
    static List<Piece> plan(ServerLevel level, StructureTemplate template, ServerPlayer player, int distance) {
        CompoundTag saved = template.save(new CompoundTag());
        ListTag palette = saved.getListOrEmpty("palette");
        if (palette.isEmpty()) palette = saved.getListOrEmpty("palettes").getListOrEmpty(0);
        HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
        Direction facing = player.getDirection();
        Rotation rotation = switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        int half = template.getSize().getX() / 2;
        BlockPos base = player.blockPosition().relative(facing, distance);
        List<Piece> plan = new ArrayList<>();
        ListTag list = saved.getListOrEmpty("blocks");
        for (int i = 0; i < list.size() && plan.size() < MAX_BLOCKS; i++) {
            CompoundTag block = list.getCompoundOrEmpty(i);
            int index = block.getIntOr("state", -1);
            if (index < 0 || index >= states.size()) continue;
            BlockState state = states.get(index);
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW)) continue;
            ListTag p = block.getListOrEmpty("pos");
            BlockPos local = new BlockPos(p.getIntOr(0, 0) - half, p.getIntOr(1, 0), p.getIntOr(2, 0));
            BlockPos pos = base.offset(StructureTemplate.transform(local, Mirror.NONE, rotation, BlockPos.ZERO));
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            plan.add(new Piece(pos, state.rotate(rotation)));
        }
        plan.sort(Comparator.<Piece>comparingInt(piece -> piece.pos().getY()).thenComparingDouble(piece -> piece.pos().distSqr(player.blockPosition())));
        return plan;
    }
}
