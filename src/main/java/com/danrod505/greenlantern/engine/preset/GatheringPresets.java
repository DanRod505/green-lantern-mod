package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Coleta e Terreno presets (the new model of abilities, 2.6.0): felling a whole tree, digging loose
 * ground, levelling the terrain and fishing. Like the other world presets they only touch blocks the
 * player may change, never blocks that hold things, and a use that changes nothing gives the energy
 * back. Minerar área and abrir caminho, of the same class, are in {@link WorldPresets}.
 */
public final class GatheringPresets {
    private GatheringPresets() {}

    public static void register() {
        Presets.registerWorld("cortar_arvore", GatheringPresets::fellTree);
        Presets.registerWorld("cavar_solto", GatheringPresets::dig);
        Presets.registerWorld("aplanar", GatheringPresets::level);
        Presets.registerWorld("pescar", GatheringPresets::fish);
    }

    // ---- cortar_arvore -----------------------------------------------------------------------------

    /**
     * Cortar árvore: the log aimed at within {@code alcance} (6) brings the whole tree down at once,
     * logs and leaves, with their drops (up to {@code altura_maxima} blocks above the cut, 32). With
     * {@code replanta} (true) a sapling of the same wood is planted where the trunk stood.
     */
    static Outcome fellTree(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        BlockHitResult hit = WorldPresets.aimedBlock(c, c.reach(6.0));
        if (hit == null || !level.getBlockState(hit.getBlockPos()).is(BlockTags.LOGS)) return WorldPresets.noTarget();
        BlockPos cut = hit.getBlockPos();
        // Down to the foot of the trunk.
        BlockPos base = cut;
        while (level.getBlockState(base.below()).is(BlockTags.LOGS) && cut.getY() - base.getY() < 32) base = base.below();
        BlockState log = level.getBlockState(base);
        int top = base.getY() + Mth.clamp(s.whole("altura_maxima", 32), 1, 64);
        List<BlockPos> start = new ArrayList<>();
        for (BlockPos pos = base; pos.getY() <= top && level.getBlockState(pos).is(BlockTags.LOGS); pos = pos.above()) start.add(pos);
        int felled = WorldPresets.fell(level, player, start);
        if (felled == 0) return WorldPresets.nothing();
        if (s.flag("replanta", true)) replant(level, player, base, log);
        Fx.play(level, player, SoundEvents.WOOD_BREAK, 1.0F, 0.6F);
        Fx.play(level, player, SoundEvents.AZALEA_LEAVES_BREAK, 1.0F, 0.8F);
        PresetFx.burst(c, Vec3.atCenterOf(base).add(0, 1, 0), 20, 0.8);
        return Outcome.DONE;
    }

    /** Plants the sapling of {@code log}'s wood (oak_log: oak_sapling; crimson_stem: crimson_fungus) where it stood. */
    private static void replant(ServerLevel level, ServerPlayer player, BlockPos base, BlockState log) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String path = id.getPath().replace("stripped_", "");
        String sapling = path.endsWith("_stem") ? path.replace("_stem", "_fungus") : path.replace("_log", "_sapling").replace("_wood", "_sapling");
        if (path.equals("mangrove_log")) sapling = "mangrove_propagule";
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(id.getNamespace(), sapling));
        if (block == Blocks.AIR || !level.getBlockState(base).canBeReplaced() || !level.mayInteract(player, base)) return;
        BlockState state = block.defaultBlockState();
        if (!state.canSurvive(level, base)) return;
        level.setBlockAndUpdate(base, state);
        level.levelEvent(1505, base, 6);
    }

    // ---- cavar_solto -------------------------------------------------------------------------------

    /**
     * Cavar solto: digs the loose ground (earth, sand, gravel, snow, clay, leaves and plants) in a
     * square of {@code tamanho} (3) by {@code tamanho} blocks, {@code profundidade} (3) deep, on the
     * face aimed at within {@code alcance} (6), with their drops. It never breaks stone.
     */
    static Outcome dig(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        BlockHitResult hit = WorldPresets.aimedBlock(c, c.reach(6.0));
        if (hit == null) return WorldPresets.noTarget();
        int size = Mth.clamp(s.whole("tamanho", 3), 1, 9);
        int depth = Mth.clamp(s.whole("profundidade", 3), 1, 9);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        int dug = 0;
        for (BlockPos pos : face(hit, size, depth)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !WorldPresets.soft(state) || !WorldPresets.free(level, player, pos, state)) continue;
            if (WorldPresets.breakBlock(level, player, pos, shovel)) dug++;
        }
        if (dug == 0) return WorldPresets.nothing();
        Fx.play(level, player, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.8F);
        PresetFx.burst(c, hit.getLocation(), 16, size * 0.3);
        return Outcome.DONE;
    }

    /** The blocks of a square of {@code size} by {@code size} on the face hit, {@code depth} deep into it. */
    public static List<BlockPos> face(BlockHitResult hit, int size, int depth) {
        Direction face = hit.getDirection();
        Direction into = face.getOpposite();
        Direction u = face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction v = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : face.getClockWise();
        int half = size / 2;
        List<BlockPos> list = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            for (int a = -half; a < size - half; a++) {
                for (int b = -half; b < size - half; b++) list.add(hit.getBlockPos().relative(into, d).relative(u, a).relative(v, b));
            }
        }
        return list;
    }

    // ---- aplanar -----------------------------------------------------------------------------------

    /**
     * Aplanar: levels the ground around the player within {@code raio} (4, a square): what stands
     * above the feet, up to {@code altura} blocks (4), comes down with its drops (nothing harder than
     * {@code dureza}, 5), and the holes under the feet, up to {@code fundo} blocks deep (3), are filled
     * with {@code material} (minecraft:dirt; inventario takes the blocks from the inventory).
     */
    static Outcome level(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int radius = Mth.clamp((int) Math.round(c.radius(4.0)), 1, 12);
        int height = Mth.clamp(s.whole("altura", 4), 1, 12);
        int depth = Mth.clamp(s.whole("fundo", 3), 0, 8);
        double hardness = s.number("dureza", 5.0);
        String materialName = s.text("material", "minecraft:dirt");
        boolean fromInventory = materialName.equals("inventario");
        Block named = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(materialName.contains(":") ? materialName : "minecraft:dirt"));
        BlockState fill = (named == Blocks.AIR ? Blocks.DIRT : named).defaultBlockState();
        BlockPos feet = player.blockPosition();
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        int changed = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = height - 1; y >= 0; y--) {
                    BlockPos pos = feet.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() || !WorldPresets.free(level, player, pos, state)) continue;
                    if (state.getDestroySpeed(level, pos) > hardness) continue;
                    if (WorldPresets.breakBlock(level, player, pos, tool)) changed++;
                }
                List<BlockPos> hole = new ArrayList<>();
                for (int y = 1; y <= depth; y++) {
                    BlockPos pos = feet.offset(x, -y, z);
                    if (!level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player, pos)) break;
                    hole.add(pos);
                }
                // Filled from the bottom of the hole up.
                for (int k = hole.size() - 1; k >= 0; k--) {
                    BlockPos pos = hole.get(k);
                    if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
                    BlockState put = fill;
                    if (fromInventory) {
                        ItemStack stock = WorldPresets.stock(player);
                        if (stock.isEmpty()) break;
                        put = ((net.minecraft.world.item.BlockItem) stock.getItem()).getBlock().defaultBlockState();
                        if (!player.isCreative()) stock.shrink(1);
                    }
                    level.setBlockAndUpdate(pos, put);
                    changed++;
                }
            }
        }
        if (changed == 0) return WorldPresets.nothing();
        Fx.play(level, player, SoundEvents.ROOTED_DIRT_PLACE, 1.0F, 0.8F);
        PresetFx.burst(c, player.position().add(0, 0.3, 0), 30, radius * 0.4);
        return Outcome.DONE;
    }

    // ---- pescar ------------------------------------------------------------------------------------

    /**
     * Pescar: aiming at water within {@code alcance} (16), pulls out {@code quantidade} (3) catches
     * at once, as a fishing rod would (fish, junk and, with {@code tesouro}, 0.1, the chance of each
     * being a treasure), and drops them at the player's feet.
     */
    static Outcome fish(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(c.reach(16.0))), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return Presets.Outcome.fail(net.minecraft.network.chat.Component.translatable("message.greenlantern.engine_no_water"));
        }
        int count = Mth.clamp(s.whole("quantidade", 3), 1, 16);
        double treasure = Mth.clamp(s.number("tesouro", 0.1), 0.0, 1.0);
        Vec3 water = hit.getLocation();
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        LootTable fishTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH);
        LootTable treasureTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_TREASURE);
        LootTable junkTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_JUNK);
        int caught = 0;
        for (int i = 0; i < count; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, water).withParameter(LootContextParams.TOOL, rod)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.FISHING);
            double roll = level.random.nextDouble();
            LootTable table = roll < treasure ? treasureTable : roll < treasure + 0.1 ? junkTable : fishTable;
            for (ItemStack stack : table.getRandomItems(params)) {
                Block.popResource(level, player.blockPosition(), stack);
                caught++;
            }
        }
        if (caught == 0) return WorldPresets.nothing();
        level.sendParticles(ParticleTypes.SPLASH, water.x, water.y, water.z, 30, 0.6, 0.2, 0.6, 0.2);
        level.sendParticles(ParticleTypes.FISHING, water.x, water.y, water.z, 12, 0.4, 0.1, 0.4, 0.05);
        Fx.play(level, player, SoundEvents.FISHING_BOBBER_SPLASH, 1.0F, 1.0F);
        Fx.play(level, player, SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
        player.awardStat(net.minecraft.stats.Stats.FISH_CAUGHT, caught);
        return Outcome.DONE;
    }
}
