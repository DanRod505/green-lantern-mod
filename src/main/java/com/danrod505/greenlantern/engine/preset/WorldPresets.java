package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Area;
import com.danrod505.greenlantern.hero.power.BlockEffects;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * The world uses of the abilities ({@code "uso_no_mundo"} in a character's file), the same for any
 * character: mining an area, building, planting and harvesting, ice and heat, light, moving a block,
 * opening a way and searching. They are used crouching. They only touch blocks the player may change
 * there, never blocks that hold things (chests, furnaces...) unless they are made to, and a use that
 * changes nothing gives the energy back.
 */
public final class WorldPresets {
    /** The most blocks a tree has (logs and leaves) when the way is opened through it. */
    static final int MAX_TREE = 256;

    private WorldPresets() {}

    public static void register() {
        Presets.registerWorld("minerar_area", WorldPresets::mine);
        Presets.registerWorld("construir", WorldPresets::build);
        Presets.registerWorld("plantar_e_colher", WorldPresets::farm);
        Presets.registerWorld("gelo_e_calor", WorldPresets::iceAndHeat);
        Presets.registerWorld("luz", WorldPresets::light);
        Presets.registerWorld("mover_blocos", WorldPresets::moveBlock);
        Presets.registerWorld("abrir_caminho", WorldPresets::openWay);
        Presets.registerWorld("procurar", WorldPresets::search);
    }

    private static Outcome nothing() {
        return Outcome.fail(Component.translatable("message.greenlantern.engine_nothing_here"));
    }

    private static Outcome noTarget() {
        return Outcome.fail(Component.translatable("message.greenlantern.engine_no_target"));
    }

    /** The block face the player aims at within {@code reach} (creatures and carried blocks do not get in the way), or null. */
    static BlockHitResult aimedBlock(PresetContext c, double reach) {
        ServerPlayer player = c.player();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = c.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    /** Breaks a block as {@code tool} would, dropping what it drops (and the experience of ores). */
    static boolean breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> drops = Block.getDrops(state, level, pos, entity, player, tool);
        if (!level.destroyBlock(pos, false, player)) return false;
        drops.forEach(drop -> Block.popResource(level, pos, drop));
        state.spawnAfterBreak(level, pos, tool, true);
        return true;
    }

    /** Whether the player may change this block and it holds nothing (no chest, no furnace...). */
    static boolean free(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0 && level.mayInteract(player, pos);
    }

    // ---- minerar_area ------------------------------------------------------------------------------

    /**
     * Minerar área: breaks a square of {@code tamanho} (3) by {@code tamanho} blocks, {@code profundidade}
     * (1) deep, on the face the player aims at within {@code alcance} (6), as {@code ferramenta}
     * (minecraft:iron_pickaxe) would: only the blocks it can mine, with their drops, and none harder
     * than {@code dureza} (5; stone is 1.5, obsidian 50).
     */
    static Outcome mine(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        BlockHitResult hit = aimedBlock(c, c.reach(6.0));
        if (hit == null) return noTarget();
        int size = Mth.clamp(s.whole("tamanho", 3), 1, 9);
        int depth = Mth.clamp(s.whole("profundidade", 1), 1, 9);
        double hardness = s.number("dureza", 5.0);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(s.text("ferramenta", "minecraft:iron_pickaxe")));
        ItemStack tool = new ItemStack(item == Items.AIR ? Items.IRON_PICKAXE : item);
        Direction face = hit.getDirection();
        Direction into = face.getOpposite();
        Direction u = face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction v = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : face.getClockWise();
        int half = size / 2;
        int broken = 0;
        for (int d = 0; d < depth; d++) {
            for (int a = -half; a < size - half; a++) {
                for (int b = -half; b < size - half; b++) {
                    BlockPos pos = hit.getBlockPos().relative(into, d).relative(u, a).relative(v, b);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty()) continue;
                    float hard = state.getDestroySpeed(level, pos);
                    if (hard > hardness || !free(level, player, pos, state)) continue;
                    if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) continue;
                    if (breakBlock(level, player, pos, tool)) broken++;
                }
            }
        }
        if (broken == 0) return nothing();
        Fx.play(level, player, SoundEvents.STONE_BREAK, 1.0F, 0.8F);
        PresetFx.burst(c, hit.getLocation(), 16, size * 0.3);
        return Outcome.DONE;
    }

    // ---- construir ---------------------------------------------------------------------------------

    /**
     * Construir: puts blocks in a {@code forma} (ponte ahead at the feet, escada going up ahead or
     * parede ahead) {@code comprimento} long (8), {@code largura} wide (1, the wall 5) and
     * {@code altura} high (the wall, 3). {@code material}: energia (a glass in the character's
     * colour), inventario (the first blocks in the player's inventory, one used per block) or a block
     * id. Energy blocks go away after {@code duracao} seconds (30) unless {@code fica} (false); the
     * others stay. It only fills air, water and plants.
     */
    static Outcome build(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String shape = s.text("forma", "ponte");
        String materialName = s.text("material", "energia");
        boolean fromInventory = materialName.equals("inventario");
        ItemStack stock = fromInventory ? stock(player) : ItemStack.EMPTY;
        if (fromInventory && stock.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_blocks"));
        BlockState material = fromInventory ? ((BlockItem) stock.getItem()).getBlock().defaultBlockState() : DefensePresets.material(materialName, c.color());
        boolean lasts = s.flag("fica", !materialName.equals("energia"));
        int length = Mth.clamp(s.whole("comprimento", 8), 1, 32);
        int width = Mth.clamp(s.whole("largura", shape.equals("parede") ? 5 : 1), 1, 9);
        int height = Mth.clamp(s.whole("altura", 3), 1, 12);
        Direction ahead = player.getDirection();
        Direction right = ahead.getClockWise();
        BlockPos feet = player.blockPosition();
        List<BlockPos> plan = new ArrayList<>();
        int half = width / 2;
        switch (shape) {
            case "escada" -> {
                for (int i = 1; i <= length; i++) {
                    for (int w = -half; w < width - half; w++) plan.add(feet.relative(ahead, i).relative(right, w).above(i - 1));
                }
            }
            case "parede" -> {
                for (int y = 0; y < height; y++) {
                    for (int w = -half; w < width - half; w++) plan.add(feet.relative(ahead, 2).relative(right, w).above(y));
                }
            }
            default -> {
                for (int i = 1; i <= length; i++) {
                    for (int w = -half; w < width - half; w++) plan.add(feet.relative(ahead, i).relative(right, w).below());
                }
            }
        }
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : plan) {
            if (!level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player, pos)) continue;
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
            if (fromInventory) {
                if (stock.isEmpty()) stock = stock(player);
                if (stock.isEmpty()) break;
                material = ((BlockItem) stock.getItem()).getBlock().defaultBlockState();
                if (!player.isCreative()) stock.shrink(1);
            }
            level.setBlockAndUpdate(pos, material);
            placed.add(pos);
        }
        if (placed.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_space"));
        level.playSound(null, placed.getFirst(), material.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        for (BlockPos pos : placed) level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.0);
        if (!lasts) {
            int ticks = c.durationTicks(30.0);
            BlockState built = material;
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> age < ticks, () -> {
                for (BlockPos pos : placed) {
                    if (level.getBlockState(pos) == built) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6F, 1.2F);
            }));
        }
        return Outcome.DONE;
    }

    /** The first stack of placeable full blocks in the player's inventory (the hotbar first). */
    private static ItemStack stock(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem block
                    && block.getBlock().defaultBlockState().isCollisionShapeFullBlock(player.level(), BlockPos.ZERO)
                    && !block.getBlock().defaultBlockState().hasBlockEntity()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ---- plantar_e_colher --------------------------------------------------------------------------

    /**
     * Plantar e colher: within {@code raio} (5) the ripe crops are harvested and planted again
     * ({@code colhe}, true) and the rest grow as with bone meal ({@code cresce}, true; also the
     * saplings), {@code vezes} times (2).
     */
    static Outcome farm(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double radius = c.radius(5.0);
        boolean harvest = s.flag("colhe", true);
        boolean grow = s.flag("cresce", true);
        int times = Mth.clamp(s.whole("vezes", 2), 1, 8);
        int changed = 0;
        for (BlockPos pos : Area.sphere(player.position(), radius)) {
            BlockState state = level.getBlockState(pos);
            boolean plant = state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS);
            if (!plant || !level.mayInteract(player, pos)) continue;
            if (harvest && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Block.getDrops(state, level, pos, null).forEach(drop -> Block.popResource(level, pos, drop));
                level.setBlockAndUpdate(pos, crop.getStateForAge(0));
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.05);
                changed++;
            } else if (grow) {
                boolean grew = false;
                for (int i = 0; i < times; i++) grew |= BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
                if (grew) {
                    level.levelEvent(1505, pos, 15);
                    changed++;
                }
            }
        }
        if (changed == 0) return nothing();
        Fx.play(level, player, SoundEvents.BONE_MEAL_USE, 1.0F, 1.0F);
        return Outcome.DONE;
    }

    // ---- gelo_e_calor ------------------------------------------------------------------------------

    /**
     * Gelo e calor: within {@code raio} (5), {@code efeito} gelo freezes still water (and lava into
     * obsidian) and puts fire out; calor melts ice and snow and lights campfires and candles.
     */
    static Outcome iceAndHeat(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(5.0);
        boolean cold = !c.settings().text("efeito", "gelo").equals("calor");
        List<BlockPos> area = Area.sphere(player.position(), radius).stream().filter(p -> level.mayInteract(player, p)).toList();
        int changed;
        if (cold) {
            changed = BlockEffects.freeze(level, area, true) + BlockEffects.snuffFire(level, area, player, false);
        } else {
            changed = 0;
            for (BlockPos pos : area) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                    level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                } else if (state.getBlock() instanceof CampfireBlock && CampfireBlock.canLight(state)
                        || state.getBlock() instanceof AbstractCandleBlock && state.hasProperty(AbstractCandleBlock.LIT)
                        && !state.getValue(AbstractCandleBlock.LIT) && !state.getFluidState().isSource()) {
                    level.setBlockAndUpdate(pos, state.setValue(AbstractCandleBlock.LIT, true));
                } else {
                    continue;
                }
                changed++;
            }
        }
        if (changed == 0) return nothing();
        Fx.play(level, player, cold ? SoundEvents.POWDER_SNOW_PLACE : SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
        PresetFx.burst(level, player.position().add(0, 0.5, 0), cold ? "gelo" : "fogo", c.color(), 30, radius * 0.4);
        return Outcome.DONE;
    }

    // ---- luz ---------------------------------------------------------------------------------------

    /**
     * Luz: lights the place within {@code raio} (8) with invisible lights every few blocks, for
     * {@code duracao} seconds (60), or for good with {@code de_vez} (false).
     */
    static Outcome light(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(8.0);
        boolean forGood = c.settings().flag("de_vez", false);
        List<BlockPos> lights = new ArrayList<>();
        BlockPos center = player.blockPosition();
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x += 4) {
            for (int z = -r; z <= r; z += 4) {
                for (int y = -2; y <= 4; y += 3) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.distSqr(center) > radius * radius || !level.getBlockState(pos).isAir() || !level.mayInteract(player, pos)) continue;
                    level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState());
                    lights.add(pos);
                }
            }
        }
        if (lights.isEmpty()) return nothing();
        ParticleOptions particle = PresetFx.particle("luz", c.lightColor());
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
        for (BlockPos pos : lights) level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
        if (!forGood) {
            int ticks = c.durationTicks(60.0);
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
                if (age % 40 == 0) {
                    for (BlockPos pos : lights) level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.0);
                }
                return age < ticks;
            }, () -> {
                for (BlockPos pos : lights) {
                    if (level.getBlockState(pos).is(Blocks.LIGHT)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }));
        }
        return Outcome.DONE;
    }

    // ---- mover_blocos ------------------------------------------------------------------------------

    /**
     * Mover blocos: picks up the block aimed at within {@code alcance} (8), if it is no harder than
     * {@code peso} (3; stone is 1.5) and holds nothing, and carries it floating in front. The next
     * use puts it down where the player aims; after {@code duracao} seconds (30) it goes back.
     */
    static Outcome moveBlock(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(8.0);
        BlockHitResult hit = aimedBlock(c, reach);
        if (hit == null) return noTarget();
        BlockPos from = hit.getBlockPos();
        BlockState state = level.getBlockState(from);
        if (state.isAir() || !state.getFluidState().isEmpty() || !free(level, player, from, state)) return noTarget();
        if (state.getDestroySpeed(level, from) > c.settings().number("peso", 3.0)) {
            return Outcome.fail(Component.translatable("message.greenlantern.engine_too_heavy", state.getBlock().getName()));
        }
        FallingBlockEntity carried = FallingBlockEntity.fall(level, from, state);
        carried.setNoGravity(true);
        carried.disableDrop();
        int ticks = c.durationTicks(30.0);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, SoundEvents.SHULKER_BULLET_HIT, 1.0F, 0.6F);
        boolean[] down = {false};
        PresetRuntime.Task task = PresetRuntime.Task.ending(age -> {
            if (carried.isRemoved()) return false;
            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(2.5)).add(0, -0.5, 0);
            carried.time = 1;
            carried.setPos(at.x, at.y, at.z);
            carried.setDeltaMovement(Vec3.ZERO);
            carried.hurtMarked = true;
            if (age % 3 == 0) level.sendParticles(particle, at.x, at.y + 0.5, at.z, 2, 0.3, 0.3, 0.3, 0.0);
            return age < ticks;
        }, () -> {
            if (down[0]) return;
            down[0] = true;
            carried.discard();
            put(level, state, level.getBlockState(from).canBeReplaced() ? from : null, carried.blockPosition());
            PresetRuntime.forget(player, PresetRuntime.key(c));
        });
        PresetRuntime.start(player, task);
        undoMove(c, task, carried, state, from, down);
        return Outcome.DONE;
    }

    /** The next use puts the carried block where the player aims (or tries again if there is no room). */
    private static void undoMove(PresetContext c, PresetRuntime.Task task, FallingBlockEntity carried, BlockState state, BlockPos from, boolean[] down) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        PresetRuntime.undoable(c, () -> {
            BlockHitResult aim = aimedBlock(c, c.reach(8.0));
            BlockPos to = aim == null ? null : aim.getBlockPos().relative(aim.getDirection());
            if (to == null || !level.getBlockState(to).canBeReplaced() || !level.mayInteract(player, to)
                    || !level.getEntitiesOfClass(LivingEntity.class, new AABB(to)).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.greenlantern.engine_no_put"), true);
                undoMove(c, task, carried, state, from, down);
                return;
            }
            down[0] = true;
            carried.discard();
            put(level, state, to, to);
            PresetRuntime.cancel(player, task);
        });
    }

    /** Sets the block at {@code at}, or drops it as an item at {@code fallback} when there is no {@code at}. */
    private static void put(ServerLevel level, BlockState state, BlockPos at, BlockPos fallback) {
        if (at != null) {
            level.setBlockAndUpdate(at, state);
            level.playSound(null, at, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            Block.popResource(level, fallback, new ItemStack(state.getBlock()));
        }
    }

    // ---- abrir_caminho -----------------------------------------------------------------------------

    /**
     * Abrir caminho: clears a way ahead {@code comprimento} long (10), {@code largura} wide (3) and
     * {@code altura} high (3). {@code remove}: terreno (plants, leaves, snow, earth, sand and gravel
     * at and above the feet, with their drops), arvores (whole trees the way touches, logs and
     * leaves) or agua (the water opens, {@code altura} blocks down and up, for {@code duracao}
     * seconds, 20, and comes back).
     */
    static Outcome openWay(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String what = s.text("remove", "terreno");
        int length = Mth.clamp(s.whole("comprimento", 10), 1, 32);
        int width = Mth.clamp(s.whole("largura", 3), 1, 9);
        int height = Mth.clamp(s.whole("altura", 3), 1, 12);
        Direction ahead = player.getDirection();
        Direction right = ahead.getClockWise();
        BlockPos feet = player.blockPosition();
        int half = width / 2;
        List<BlockPos> way = new ArrayList<>();
        int below = what.equals("agua") ? -height : 0;
        for (int i = 1; i <= length; i++) {
            for (int w = -half; w < width - half; w++) {
                for (int y = below; y < height; y++) way.add(feet.relative(ahead, i).relative(right, w).above(y));
            }
        }
        way.removeIf(p -> !level.mayInteract(player, p));
        int changed = switch (what) {
            case "arvores" -> fell(level, player, way);
            case "agua" -> part(c, way);
            default -> clear(level, player, way);
        };
        if (changed == 0) return nothing();
        Fx.play(level, player, what.equals("agua") ? SoundEvents.GENERIC_SPLASH : SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.8F);
        return Outcome.DONE;
    }

    private static boolean soft(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.CLAY)
                || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.isAir();
    }

    private static int clear(ServerLevel level, ServerPlayer player, List<BlockPos> way) {
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        int changed = 0;
        for (BlockPos pos : way) {
            BlockState state = level.getBlockState(pos);
            if (!soft(state) || !free(level, player, pos, state)) continue;
            if (breakBlock(level, player, pos, shovel)) changed++;
        }
        return changed;
    }

    /** Fells the trees the way touches: each log with the logs and leaves joined to it. */
    private static int fell(ServerLevel level, ServerPlayer player, List<BlockPos> way) {
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        Set<BlockPos> tree = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        for (BlockPos pos : way) if (level.getBlockState(pos).is(BlockTags.LOGS)) open.add(pos);
        while (!open.isEmpty() && tree.size() < MAX_TREE) {
            BlockPos pos = open.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) || !tree.add(pos) || !level.mayInteract(player, pos)) continue;
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (!tree.contains(next)) open.add(next.immutable());
            }
        }
        int changed = 0;
        for (BlockPos pos : tree) if (breakBlock(level, player, pos, axe)) changed++;
        return changed;
    }

    /** The water of the way opens and comes back after the duration (and so do the sea plants). */
    private static int part(PresetContext c, List<BlockPos> way) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        Map<BlockPos, BlockState> opened = new LinkedHashMap<>();
        for (BlockPos pos : way) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().isEmpty() || state.hasBlockEntity()) continue;
            if (!state.is(Blocks.WATER) && !state.canBeReplaced()) continue;
            opened.put(pos, state);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (opened.isEmpty()) return 0;
        int ticks = c.durationTicks(20.0);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (age % 10 == 0) {
                for (BlockPos pos : opened.keySet()) {
                    if (!level.getFluidState(pos).isEmpty() && level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (age % 5 == 0) {
                BlockPos any = opened.keySet().stream().skip(level.random.nextInt(opened.size())).findFirst().orElseThrow();
                level.sendParticles(ParticleTypes.SPLASH, any.getX() + 0.5, any.getY() + 0.5, any.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
            }
            return age < ticks;
        }, () -> {
            for (Map.Entry<BlockPos, BlockState> e : opened.entrySet()) {
                if (level.getBlockState(e.getKey()).isAir()) level.setBlockAndUpdate(e.getKey(), e.getValue());
            }
        }));
        return opened.size();
    }

    // ---- procurar ----------------------------------------------------------------------------------

    /**
     * Procurar: for {@code duracao} seconds (10) shows what is hidden within {@code raio} (16):
     * {@code mostra} baus (chests, barrels and anything that holds items), minerios or mobs (they
     * glow). The blocks show their outline through walls (the nearest 32).
     */
    static Outcome search(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        String shows = c.settings().text("mostra", "baus");
        double radius = Math.min(32.0, c.radius(16.0));
        int ticks = c.durationTicks(10.0);
        Map<BlockPos, Display.BlockDisplay> shown = new HashMap<>();
        BiPredicate<ServerLevel, BlockPos> wanted = shows.equals("minerios")
                ? (l, p) -> l.getBlockState(p).is(Tags.Blocks.ORES)
                : (l, p) -> l.getBlockState(p).hasBlockEntity() && l.getBlockEntity(p) instanceof Container;
        boolean mobs = shows.equals("mobs");
        if (!mobs) {
            EmpowerPresets.showBlocks(c, radius, shown, wanted);
            if (shown.isEmpty()) return nothing();
        } else if (Targets.inRadius(level, player.position(), radius, e -> e != player && !Summons.summoned(e)).isEmpty()) {
            return nothing();
        }
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (mobs && age % 20 == 1) {
                for (LivingEntity mob : Targets.inRadius(level, player.position(), radius, e -> e != player && !Summons.summoned(e))) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
                }
            }
            if (!mobs && age % 40 == 0) EmpowerPresets.showBlocks(c, radius, shown, wanted);
            return age < ticks;
        }, () -> {
            shown.values().forEach(Summons::dismiss);
            shown.clear();
        }));
        return Outcome.DONE;
    }
}
