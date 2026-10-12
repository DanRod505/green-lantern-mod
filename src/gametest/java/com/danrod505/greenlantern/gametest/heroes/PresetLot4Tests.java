package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;
import static com.danrod505.greenlantern.gametest.heroes.PresetTests.*;

import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.AbilitySlot;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.EmpowerPresets;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The ability presets of stage 2, batch 4 (the 8 uses in the world), on the test character
 * Protótipo with abilities made on the spot, used crouching (the world use).
 */
public final class PresetLot4Tests {
    private PresetLot4Tests() {}

    static final List<String> BATCH_4 = List.of(
            "minerar_area", "construir", "plantar_e_colher", "gelo_e_calor", "luz", "mover_blocos", "abrir_caminho", "procurar");

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("preset_batch4_registered", () -> PresetLot4Tests::registered);
        tests.register("preset_minerar_area", () -> PresetLot4Tests::mine);
        tests.register("preset_construir", () -> PresetLot4Tests::build);
        tests.register("preset_plantar_e_colher", () -> PresetLot4Tests::farm);
        tests.register("preset_gelo_e_calor", () -> PresetLot4Tests::iceAndHeat);
        tests.register("preset_luz", () -> PresetLot4Tests::light);
        tests.register("preset_mover_blocos", () -> PresetLot4Tests::moveBlock);
        tests.register("preset_abrir_caminho", () -> PresetLot4Tests::openWay);
        tests.register("preset_procurar", () -> PresetLot4Tests::search);
    }

    /** An ability whose world use ({@code "uso_no_mundo"}) is {@code preset}: the shortest cooldown (half a second), cost 5. */
    public static EngineAbility world(String preset, double reach, double radius, double duration, CharacterData.Settings settings) {
        CharacterData.Numbers numbers = new CharacterData.Numbers(0, 5, 0.05, reach, radius, duration);
        CharacterData.Ability data = new CharacterData.Ability("teste_" + preset, Optional.of(preset), Optional.of(AbilityKind.STRIKE),
                Optional.of(AbilitySlot.MAIN), "soco_forte", Optional.of(preset), numbers, settings);
        return new EngineAbility(data, 0, AbilityKind.STRIKE);
    }

    static boolean useWorld(ServerPlayer player, EngineAbility ability) {
        return EngineServer.useInWorld(player, prototype(), player.getMainHandItem(), ability);
    }

    /** Turns the player to look at a point of the arena. */
    static void look(GameTestHelper helper, ServerPlayer player, double x, double y, double z) {
        Vec3 to = helper.absoluteVec(new Vec3(x, y, z)).subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, to.horizontalDistance()));
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
    }

    static boolean is(GameTestHelper helper, int x, int y, int z, Block block) {
        return helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z))).is(block);
    }

    static int count(GameTestHelper helper, Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(13, 7, 13)))) {
            if (helper.getLevel().getBlockState(pos).is(block)) n++;
        }
        return n;
    }

    static List<ItemEntity> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(15, 8, 15))));
    }

    // ---- tests ------------------------------------------------------------------------------------

    /** The 8 world uses of batch 4 are there for the character files to name. */
    public static void registered(GameTestHelper helper) {
        for (String id : BATCH_4) helper.assertTrue(Presets.hasWorld(id), "missing world preset " + id);
        helper.succeed();
    }

    public static void mine(GameTestHelper helper) {
        for (int x = 6; x <= 8; x++) for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.OBSIDIAN);
        ServerPlayer player = suited(helper, 7.5, 2.5);
        look(helper, player, 7.5, 2.5, 5.0);
        helper.assertTrue(useWorld(player, world("minerar_area", 6, 0, 0, settings("tamanho", 3))), "the mining goes off");
        helper.assertTrue(count(helper, Blocks.STONE) == 0, "the 3x3 of stone is gone, " + count(helper, Blocks.STONE) + " left");
        helper.assertTrue(is(helper, 7, 2, 5, Blocks.OBSIDIAN), "but not the obsidian, too hard");
        helper.assertTrue(items(helper).stream().anyMatch(i -> i.getItem().is(Items.COBBLESTONE)), "and the stone drops cobblestone");
        remove(player);
        helper.succeed();
    }

    public static void build(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        helper.assertTrue(useWorld(player, world("construir", 0, 0, 1, settings("forma", "escada", "comprimento", 4))), "the stairs go up");
        helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 1, 3))).isAir()
                && !helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 4, 6))).isAir(), "four steps of energy");
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 1, 3))).isAir(), "the energy goes away");
                    // From the inventory: a wall of the player's stone, which stays.
                    player.getInventory().add(new ItemStack(Items.STONE_BRICKS, 10));
                    helper.assertTrue(useWorld(player, world("construir", 0, 0, 1, settings("forma", "parede", "material", "inventario", "largura", 3, "altura", 2))),
                            "the wall goes up");
                    helper.assertTrue(count(helper, Blocks.STONE_BRICKS) == 6, "six bricks, got " + count(helper, Blocks.STONE_BRICKS));
                    helper.assertTrue(player.getInventory().countItem(Items.STONE_BRICKS) == 4, "taken from the inventory");
                })
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(count(helper, Blocks.STONE_BRICKS) == 6, "and they stay");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void farm(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, 0, 7), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(5, 0, 7), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(9, 1, 7), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        helper.setBlock(new BlockPos(5, 1, 7), ((CropBlock) Blocks.WHEAT).getStateForAge(0));
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("plantar_e_colher", 0, 5, 0, settings())), "the farming goes off");
        var ripe = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, 1, 7)));
        var young = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(5, 1, 7)));
        helper.assertTrue(ripe.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(ripe) == 0, "the ripe wheat is harvested and planted again");
        helper.assertTrue(items(helper).stream().anyMatch(i -> i.getItem().is(Items.WHEAT)), "dropping wheat");
        helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(young) > 0, "the young wheat grows");
        remove(player);
        helper.succeed();
    }

    public static void iceAndHeat(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.WATER);
        helper.setBlock(new BlockPos(5, 1, 9), Blocks.LAVA);
        helper.setBlock(new BlockPos(9, 1, 5), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("gelo_e_calor", 0, 5, 0, settings("efeito", "gelo"))), "the cold goes off");
        helper.assertTrue(is(helper, 9, 1, 9, Blocks.ICE), "the water freezes");
        helper.assertTrue(is(helper, 5, 1, 9, Blocks.OBSIDIAN), "the lava turns to obsidian");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(useWorld(player, world("gelo_e_calor", 0, 5, 0, settings("efeito", "calor"))), "the heat goes off");
                    helper.assertTrue(is(helper, 9, 1, 9, Blocks.WATER), "the ice melts");
                    helper.assertTrue(is(helper, 9, 1, 5, Blocks.AIR), "and the snow");
                    helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(5, 1, 5))).getValue(CampfireBlock.LIT), "the campfire is lit");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void light(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("luz", 0, 6, 1, settings())), "the light goes on");
        int lights = count(helper, Blocks.LIGHT);
        helper.assertTrue(lights > 0, "invisible lights around, got " + lights);
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(count(helper, Blocks.LIGHT) == 0, "they go away after the time");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void moveBlock(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 1, 5), Blocks.DIRT);
        helper.setBlock(new BlockPos(9, 1, 5), Blocks.OBSIDIAN);
        ServerPlayer player = suited(helper, 7.5, 2.5);
        EngineAbility move = world("mover_blocos", 8, 0, 30, settings());
        look(helper, player, 9.5, 1.5, 5.0);
        helper.assertFalse(useWorld(player, move), "the obsidian is too heavy");
        look(helper, player, 7.5, 1.5, 5.0);
        helper.assertTrue(useWorld(player, move), "the dirt is picked up");
        helper.assertTrue(is(helper, 7, 1, 5, Blocks.AIR), "it leaves its place");
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(!helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, player.getBoundingBox().inflate(6)).isEmpty(), "and floats in front");
                    look(helper, player, 7.5, 1.0, 9.5);
                    helper.assertTrue(useWorld(player, move), "the key again puts it down");
                    helper.assertTrue(is(helper, 7, 1, 9, Blocks.DIRT), "where the player aims");
                    helper.assertFalse(PresetRuntime.undoable(player, PresetRuntime.worldKey("teste_mover_blocos")), "and it is done");
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, player.getBoundingBox().inflate(6)).isEmpty(), "nothing floats any more");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void openWay(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 1, 4), Blocks.DIRT);
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.SAND);
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(7, y, 9), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(7, 5, 9), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(8, 5, 9), Blocks.OAK_LEAVES);
        ServerPlayer player = suited(helper, 7.5, 2.5);
        helper.assertTrue(useWorld(player, world("abrir_caminho", 0, 0, 0, settings("remove", "terreno"))), "the ground is cleared");
        helper.assertTrue(is(helper, 7, 1, 4, Blocks.AIR) && is(helper, 6, 2, 6, Blocks.AIR), "the dirt and the sand are gone");
        helper.assertTrue(is(helper, 7, 1, 9, Blocks.OAK_LOG), "but not the tree");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(useWorld(player, world("abrir_caminho", 0, 0, 0, settings("remove", "arvores"))), "the trees are felled");
                    helper.assertTrue(count(helper, Blocks.OAK_LOG) == 0 && count(helper, Blocks.OAK_LEAVES) == 0, "the whole tree, logs and leaves");
                })
                .thenExecuteAfter(12, () -> {
                    flood(helper, 3);
                    helper.assertTrue(useWorld(player, world("abrir_caminho", 0, 0, 1, settings("remove", "agua"))), "the water opens");
                    helper.assertTrue(is(helper, 7, 2, 6, Blocks.AIR) && is(helper, 8, 3, 10, Blocks.AIR), "a dry way ahead");
                    helper.assertTrue(is(helper, 2, 2, 6, Blocks.WATER), "with water on the sides");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(is(helper, 7, 2, 6, Blocks.WATER), "then the water comes back");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void search(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.CHEST);
        helper.setBlock(new BlockPos(11, 1, 11), Blocks.BARREL);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertFalse(useWorld(player, world("procurar", 0, 16, 1, settings("mostra", "minerios"))), "no ores: nothing to show");
        helper.assertTrue(useWorld(player, world("procurar", 0, 16, 1, settings("mostra", "baus"))), "the search goes off");
        helper.assertTrue(EmpowerPresets.shownOres(helper.getLevel(), player) == 2, "the chest and the barrel show, got "
                + EmpowerPresets.shownOres(helper.getLevel(), player));
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(EmpowerPresets.shownOres(helper.getLevel(), player) == 0, "and stop showing after the time");
                    remove(player);
                })
                .thenSucceed();
    }
}
