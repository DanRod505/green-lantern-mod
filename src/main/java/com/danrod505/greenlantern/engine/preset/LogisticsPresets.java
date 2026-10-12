package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Logística presets of the new model of abilities (2.6.0): pulling items to the player, a helper that
 * works beside them and putting a chest in order. Carregar bloco, also Logística, is in
 * {@link WorldPresets}.
 */
public final class LogisticsPresets {
    private LogisticsPresets() {}

    public static void register() {
        Presets.registerWorld("ima_de_itens", LogisticsPresets::magnet);
        Presets.registerWorld("ajudante_de_trabalho", LogisticsPresets::helper);
        Presets.registerWorld("organizar", LogisticsPresets::organize);
    }

    /** The container aimed at within {@code reach}, or null. */
    static Container aimedContainer(PresetContext c, double reach) {
        BlockHitResult hit = WorldPresets.aimedBlock(c, reach);
        if (hit == null || !c.level().mayInteract(c.player(), hit.getBlockPos())) return null;
        return c.level().getBlockEntity(hit.getBlockPos()) instanceof Container container ? container : null;
    }

    // ---- ima_de_itens ------------------------------------------------------------------------------

    /**
     * Ímã de itens: aimed at a chest (or anything that holds items) within {@code alcance} (6) it
     * takes what fits of it into the inventory; aimed anywhere else, for {@code duracao} seconds (5)
     * the items and experience lying within {@code raio} (8) fly to the player.
     */
    static Outcome magnet(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        Container container = aimedContainer(c, c.reach(6.0));
        if (container != null) {
            int moved = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                int before = stack.getCount();
                ItemStack left = stack.copy();
                player.getInventory().add(left);
                if (left.getCount() == before) continue;
                container.setItem(i, left);
                moved += before - left.getCount();
            }
            if (moved == 0) return WorldPresets.nothing();
            container.setChanged();
            Fx.play(level, player, SoundEvents.ITEM_PICKUP, 1.0F, 0.8F);
            return Outcome.DONE;
        }
        double radius = Math.min(24.0, c.radius(8.0));
        AABB around = player.getBoundingBox().inflate(radius);
        if (level.getEntitiesOfClass(ItemEntity.class, around).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, around).isEmpty()) {
            return WorldPresets.nothing();
        }
        int ticks = c.durationTicks(5.0);
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.6F);
        PresetRuntime.start(player, age -> {
            AABB box = player.getBoundingBox().inflate(radius);
            List<net.minecraft.world.entity.Entity> pulled = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, box));
            pulled.addAll(level.getEntitiesOfClass(ExperienceOrb.class, box));
            for (net.minecraft.world.entity.Entity e : pulled) {
                Vec3 way = player.position().add(0, 0.5, 0).subtract(e.position());
                if (way.length() < 1.0) {
                    if (e instanceof ItemEntity item) item.setNoPickUpDelay();
                    e.playerTouch(player);
                } else {
                    e.setDeltaMovement(way.normalize().scale(0.5));
                    e.hurtMarked = true;
                }
            }
            if (age % 4 == 1) Fx.ring(level, player.position().add(0, 0.1, 0), 1.2, PresetFx.particle(c), 10);
            return age < ticks;
        });
        return Outcome.DONE;
    }

    // ---- ajudante_de_trabalho ----------------------------------------------------------------------

    /**
     * Ajudante de trabalho: a little helper (an allay in the character's colour of particles) flies
     * beside the player for {@code duracao} seconds (30) and once a second does {@code tarefa}
     * within {@code raio} (6) of it: colher (picks the ripe crops and plants them again), guardar
     * (brings the items lying around into the inventory) or minerar (breaks an ore).
     */
    static Outcome helper(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String task = s.text("tarefa", "colher");
        double radius = Math.min(12.0, c.radius(6.0));
        int ticks = c.durationTicks(30.0);
        Allay allay = EntityType.ALLAY.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (allay == null) return WorldPresets.nothing();
        allay.setNoAi(true);
        allay.setInvulnerable(true);
        allay.setSilent(true);
        allay.snapTo(player.getX(), player.getY() + 1.5, player.getZ(), player.getYRot(), 0);
        Summons.spawn(c, allay);
        Fx.play(level, player, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 1.0F, 1.0F);
        PresetFx.burst(c, allay.position(), 12, 0.4);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (!allay.isAlive()) return false;
            Vec3 side = player.position().add(player.getLookAngle().yRot(1.2F).multiply(1.2, 0, 1.2)).add(0, 1.4, 0);
            allay.snapTo(side.x, side.y, side.z, player.getYRot(), 0);
            if (age % 20 == 0) work(c, allay, task, radius);
            return age < ticks;
        }, () -> Summons.dismiss(allay)));
    }

    private static void work(PresetContext c, Allay allay, String task, double radius) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        BlockPos center = player.blockPosition();
        int r = (int) Math.ceil(radius);
        switch (task) {
            case "guardar" -> {
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius))) {
                    item.setNoPickUpDelay();
                    item.playerTouch(player);
                }
            }
            case "minerar" -> {
                ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
                for (BlockPos pos : BlockPos.withinManhattan(center, r, r, r)) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES) && WorldPresets.free(level, player, pos, state)) {
                        Fx.line(level, allay.position(), Vec3.atCenterOf(pos), PresetFx.particle(c), 3.0);
                        WorldPresets.breakBlock(level, player, pos, pickaxe);
                        return;
                    }
                }
            }
            default -> {
                int picked = 0;
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || !level.mayInteract(player, pos)) continue;
                    Block.dropResources(state, level, pos.immutable(), null, player, ItemStack.EMPTY, true);
                    level.setBlockAndUpdate(pos, crop.getStateForAge(0));
                    if (++picked >= 4) break;
                }
                if (picked > 0) Fx.play(level, allay, SoundEvents.CROP_BREAK, 0.8F, 1.0F);
            }
        }
    }

    // ---- organizar ---------------------------------------------------------------------------------

    /**
     * Organizar: the chest (or anything that holds items) aimed at within {@code alcance} (6) is put
     * in order: equal items joined into full stacks and sorted by {@code regra} (nome by the item's
     * id, quantidade by the most first, tipo blocks first and then by id).
     */
    static Outcome organize(PresetContext c) {
        ServerPlayer player = c.player();
        Container container = aimedContainer(c, c.reach(6.0));
        if (container == null) return WorldPresets.noTarget();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack left = stack.copy();
            for (ItemStack into : stacks) {
                if (left.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(into, left)) continue;
                int room = Math.min(left.getCount(), into.getMaxStackSize() - into.getCount());
                into.grow(room);
                left.shrink(room);
            }
            if (!left.isEmpty()) stacks.add(left);
        }
        if (stacks.isEmpty()) return WorldPresets.nothing();
        Comparator<ItemStack> byId = Comparator.comparing(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
        Comparator<ItemStack> order = switch (c.settings().text("regra", "nome")) {
            case "quantidade" -> Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed().thenComparing(byId);
            case "tipo" -> Comparator.<ItemStack, Boolean>comparing(st -> !(st.getItem() instanceof net.minecraft.world.item.BlockItem)).thenComparing(byId);
            default -> byId;
        };
        stacks.sort(order);
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        container.setChanged();
        Fx.play(c.level(), player, SoundEvents.BUNDLE_INSERT, 1.0F, 1.0F);
        return Outcome.DONE;
    }

}
