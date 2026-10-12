package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Area;
import com.danrod505.greenlantern.hero.power.Fx;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Construção and Transmutação presets of the new model of abilities (2.6.0): a building shown as a
 * ghost and built with the blocks of the inventory, changing blocks into others (as a furnace would,
 * from a table, or to another colour) and switching machines on from afar. Construir, luz and the
 * great work, also Construção, and gelo e calor, also Transmutação, are in {@link WorldPresets} and
 * {@link SupremePresets}.
 */
public final class CraftPresets {
    /** The most ghost blocks a project shows. */
    static final int MAX_GHOST = 600;

    private CraftPresets() {}

    public static void register() {
        Presets.registerWorld("projeto_em_fantasma", CraftPresets::project);
        Presets.registerWorld("trocar_bloco", CraftPresets::swap);
        Presets.registerWorld("ligar_maquina", CraftPresets::machine);
    }

    // ---- projeto_em_fantasma -----------------------------------------------------------------------

    /**
     * Projeto em fantasma: shows a structure ({@code estrutura}, minecraft:igloo/top) in front of
     * the player, {@code distancia} blocks away (3), as a ghost of glass in the character's colour,
     * for {@code duracao} seconds (60). Pressing the ability again builds it with the blocks of the
     * inventory (each piece takes its own block; in creative nothing is spent), from the bottom up;
     * the pieces with no block left stay unbuilt and the player reads how many.
     */
    static Outcome project(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        Identifier id = Identifier.tryParse(s.text("estrutura", "minecraft:igloo/top"));
        Optional<StructureTemplate> template = id == null ? Optional.empty() : level.getStructureManager().get(id);
        if (template.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_structure"));
        List<SupremePresets.Piece> plan = SupremePresets.plan(level, template.get(), player, Math.max(1, (int) Math.round(s.number("distancia", 3.0))));
        if (plan.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_space"));
        BlockState ghostLook = DefensePresets.glass(c.color());
        List<Display.BlockDisplay> ghost = new ArrayList<>();
        for (int i = 0; i < plan.size() && i < MAX_GHOST; i++) {
            BlockPos pos = plan.get(i).pos();
            Display.BlockDisplay piece = Displays.block(player, ghostLook, new Vec3(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1),
                    new Vector3f(0.8F, 0.8F, 0.8F), c.color());
            if (piece != null) ghost.add(piece);
        }
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        int ticks = c.durationTicks(60.0);
        PresetRuntime.Task task = PresetRuntime.Task.ending(age -> age < ticks, () -> ghost.forEach(Summons::dismiss));
        PresetRuntime.start(player, task);
        PresetRuntime.undoable(c, () -> {
            PresetRuntime.cancel(player, task);
            ghost.forEach(Summons::dismiss);
            int built = 0;
            int missing = 0;
            for (SupremePresets.Piece piece : plan) {
                if (!level.getBlockState(piece.pos()).canBeReplaced() || !level.mayInteract(player, piece.pos())) continue;
                if (!player.isCreative() && !take(player, piece.state().getBlock())) {
                    missing++;
                    continue;
                }
                level.setBlock(piece.pos(), piece.state(), Block.UPDATE_ALL);
                built++;
            }
            if (built > 0) {
                BlockPos at = plan.getFirst().pos();
                level.playSound(null, at, plan.getFirst().state().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
                Fx.play(level, player, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.3F);
            }
            if (missing > 0) player.displayClientMessage(Component.translatable("message.greenlantern.engine_missing_blocks", missing), true);
        });
        return Outcome.DONE;
    }

    /** Takes one block of {@code block} from the player's inventory; false when there is none. */
    private static boolean take(ServerPlayer player, Block block) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item && item.getBlock() == block) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // ---- trocar_bloco ------------------------------------------------------------------------------

    /**
     * Trocar bloco: changes the block aimed at within {@code alcance} (8), and the ones within
     * {@code raio} of it (0: only that one), into others. {@code modo}: fornalha (as a furnace would:
     * sand into glass, cobblestone into stone, clay into terracotta...), tabela (the table
     * {@code trocas}: {"minecraft:stone": "minecraft:iron_block"}) or cor (wool, glass, concrete,
     * terracotta and the like change to the colour {@code cor_nova}, red by default).
     */
    static Outcome swap(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        BlockHitResult hit = WorldPresets.aimedBlock(c, c.reach(8.0));
        if (hit == null) return WorldPresets.noTarget();
        String mode = s.text("modo", "fornalha");
        double radius = Math.min(6.0, c.radius(0.0));
        List<BlockPos> area = radius <= 0 ? List.of(hit.getBlockPos()) : Area.sphere(Vec3.atCenterOf(hit.getBlockPos()), radius);
        Map<String, JsonElement> table = s.values().get("trocas") instanceof JsonElement t && t.isJsonObject()
                ? t.getAsJsonObject().asMap() : Map.of();
        String dye = s.text("cor_nova", "red");
        int changed = 0;
        ParticleOptions particle = PresetFx.particle(c);
        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !WorldPresets.free(level, player, pos, state)) continue;
            BlockState next = switch (mode) {
                case "tabela" -> fromTable(table, state);
                case "cor" -> recolor(state, dye);
                default -> smelted(level, state);
            };
            if (next == null || next == state) continue;
            level.setBlockAndUpdate(pos, next);
            level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
            changed++;
        }
        if (changed == 0) return WorldPresets.nothing();
        Fx.play(level, player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.4F);
        return Outcome.DONE;
    }

    /** What a furnace would make of the block, when that is a block. */
    static BlockState smelted(ServerLevel level, BlockState state) {
        ItemStack input = new ItemStack(state.getBlock().asItem());
        if (input.isEmpty()) return null;
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(holder -> holder.value().assemble(recipeInput, level.registryAccess()))
                .filter(out -> out.getItem() instanceof BlockItem)
                .map(out -> ((BlockItem) out.getItem()).getBlock().defaultBlockState())
                .orElse(null);
    }

    private static BlockState fromTable(Map<String, JsonElement> table, BlockState state) {
        JsonElement to = table.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (to == null || !to.isJsonPrimitive()) return null;
        Identifier id = Identifier.tryParse(to.getAsString());
        Block block = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getValue(id);
        return block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** The same block in another colour (red_wool into blue_wool), or null when it has no colour. */
    static BlockState recolor(BlockState state, String dye) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();
            if (!path.startsWith(name + "_")) continue;
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(id.getNamespace(), dye + path.substring(name.length())));
            return block == Blocks.AIR ? null : block.withPropertiesOf(state);
        }
        return null;
    }

    // ---- ligar_maquina -----------------------------------------------------------------------------

    /**
     * Ligar máquina: switches on what the player aims at within {@code alcance} (16): a lever flips,
     * a button is pressed, and anything else (a lamp, a door, a piston, a dispenser, a rail...) gets
     * a redstone signal for {@code duracao} seconds (2) from a block of redstone that appears beside
     * it, on the face aimed at, and goes away.
     */
    static Outcome machine(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        BlockHitResult hit = WorldPresets.aimedBlock(c, c.reach(16.0));
        if (hit == null) return WorldPresets.noTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!level.mayInteract(player, pos)) return WorldPresets.noTarget();
        ParticleOptions particle = PresetFx.particle("raio", c.color(), 1.0F);
        Vec3 from = c.muzzle();
        Fx.line(level, from, hit.getLocation(), particle, 2.0);
        if (state.getBlock() instanceof LeverBlock lever) {
            lever.pull(state, level, pos, player);
        } else if (state.getBlock() instanceof ButtonBlock button) {
            if (state.getValue(ButtonBlock.POWERED)) return WorldPresets.nothing();
            button.press(state, level, pos, player);
        } else {
            BlockPos beside = pos.relative(hit.getDirection());
            if (!level.getBlockState(beside).isAir() || !level.mayInteract(player, beside)) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_space"));
            level.setBlockAndUpdate(beside, Blocks.REDSTONE_BLOCK.defaultBlockState());
            int ticks = c.durationTicks(2.0);
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> age < ticks, () -> {
                if (level.getBlockState(beside).is(Blocks.REDSTONE_BLOCK)) level.setBlockAndUpdate(beside, Blocks.AIR.defaultBlockState());
            }));
        }
        Fx.play(level, player, SoundEvents.COPPER_BULB_TURN_ON, 1.0F, 1.0F);
        level.sendParticles(particle, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 10, 0.2, 0.2, 0.2, 0.05);
        return Outcome.DONE;
    }
}
