package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.count;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.is;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.items;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.look;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.useWorld;
import static com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests.world;
import static com.danrod505.greenlantern.gametest.heroes.PresetTests.*;

import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Traits;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Stage 2, batch 6: the new model of abilities (2.6.0). The 15 ability classes, the check of a
 * character file (5 to 8 abilities, fighting ones first, one supreme last), the functions chosen by
 * how the key is pressed and what is aimed at, the traits (passivas) and the world presets that
 * were missing.
 */
public final class PresetLot6Tests {
    private PresetLot6Tests() {}

    static final List<String> BATCH_6 = List.of("cortar_arvore", "cavar_solto", "aplanar", "pescar", "projeto_em_fantasma", "trocar_bloco",
            "ligar_maquina", "domar_e_guiar", "clima", "rastro", "ima_de_itens", "ajudante_de_trabalho", "organizar", "levar_junto");

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("lot6_classes", () -> PresetLot6Tests::classes);
        tests.register("lot6_check", () -> PresetLot6Tests::check);
        tests.register("lot6_functions", () -> PresetLot6Tests::functions);
        tests.register("lot6_traits", () -> PresetLot6Tests::traits);
        tests.register("lot6_icons", () -> PresetLot6Tests::icons);
        tests.register("lot6_registered", () -> PresetLot6Tests::registered);
        tests.register("preset_cortar_arvore", () -> PresetLot6Tests::fellTree);
        tests.register("preset_cavar_solto", () -> PresetLot6Tests::dig);
        tests.register("preset_pescar", () -> PresetLot6Tests::fish);
        tests.register("preset_projeto_em_fantasma", () -> PresetLot6Tests::project);
        tests.register("preset_trocar_bloco", () -> PresetLot6Tests::swap);
        tests.register("preset_ligar_maquina", () -> PresetLot6Tests::machine);
        tests.register("preset_domar_e_guiar", () -> PresetLot6Tests::tame);
        tests.register("preset_rastro", () -> PresetLot6Tests::trail);
        tests.register("preset_ima_de_itens", () -> PresetLot6Tests::magnet);
        tests.register("preset_ajudante_de_trabalho", () -> PresetLot6Tests::helperTask);
        tests.register("preset_organizar", () -> PresetLot6Tests::organize);
        tests.register("preset_levar_junto", () -> PresetLot6Tests::takeAlong);
    }

    private static EngineHero hero(String id) {
        return (EngineHero) HeroRegistry.byId(id).orElseThrow();
    }

    private static ServerPlayer suitedAs(GameTestHelper helper, String id, double x, double z) {
        ServerPlayer player = player(helper, x, 1, z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero(id)));
        helper.assertTrue(hero(id).summonSuit(player), id + ": the suit should go on");
        return player;
    }

    private static CharacterData.Ability ability(CharacterData data, String id) {
        return data.abilities().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    /** The character with other abilities and traits, for the check. */
    private static CharacterData with(CharacterData d, UnaryOperator<List<CharacterData.Ability>> abilities, List<CharacterData.Trait> traits) {
        return new CharacterData(d.id(), d.name(), d.characterClass(), d.level(), d.skeleton(), d.transformation(), d.theme(), d.legacy(), d.source(),
                d.stats(), d.suit(), abilities.apply(new ArrayList<>(d.abilities())), d.skins(), d.weaknesses(), d.colors(), d.visual(), traits);
    }

    private static boolean refused(CharacterData data, String words) {
        return Characters.check(data, data.id()).stream().anyMatch(p -> p.contains(words));
    }

    // ---- the model ---------------------------------------------------------------------------------

    /** 15 classes, 8 for fighting and 7 for the world; the Supreme is no longer one of them. */
    public static void classes(GameTestHelper helper) {
        helper.assertTrue(AbilityKind.values().length == 15, "15 ability classes");
        helper.assertTrue(Arrays.stream(AbilityKind.values()).filter(AbilityKind::fight).count() == 8, "8 for fighting");
        helper.assertTrue(Arrays.stream(AbilityKind.values()).noneMatch(k -> k.id().equals("suprema")), "no class suprema");
        helper.assertTrue(Arrays.stream(AbilityKind.values()).map(AbilityKind::id).distinct().count() == 15, "each with an id of its own");
        helper.succeed();
    }

    /** The file check: 5 to 8 abilities within the class's arsenal, fighting first, exactly 1 supreme, the last. */
    public static void check(GameTestHelper helper) {
        for (String id : List.of("prototipo", "prototipo_colosso", "prototipo_alado")) {
            helper.assertTrue(Characters.check(hero(id).data(), id).isEmpty(), id + ": " + Characters.check(hero(id).data(), id));
        }
        CharacterData d = hero("prototipo").data();
        helper.assertTrue(refused(with(d, l -> l.subList(0, 4), List.of()), "abilities"), "4 abilities are too few");
        helper.assertTrue(refused(with(d, l -> {
            l.add(0, l.remove(l.size() - 1));
            return l;
        }, List.of()), "supreme ability is the last"), "the supreme goes last");
        helper.assertTrue(refused(with(d, l -> {
            l.add(0, l.remove(4));
            return l;
        }, List.of()), "fighting abilities come before"), "fighting abilities first");
        helper.assertTrue(refused(with(d, l -> {
            CharacterData.Ability a = l.get(0);
            l.set(0, new CharacterData.Ability(a.id(), a.name(), a.kind(), true, a.preset(), a.numbers(), a.settings()));
            return l;
        }, List.of()), "exactly 1 supreme"), "only one supreme");
        helper.assertTrue(refused(with(d, l -> {
            CharacterData.Ability a = l.get(0);
            l.set(0, new CharacterData.Ability(a.id(), a.name(), a.kind(), false, a.preset(), a.numbers(), a.settings(),
                    List.of(new CharacterData.Function("cortar_arvore", new CharacterData.When(Optional.empty(), Optional.of("nuvem"), Optional.empty()),
                            CharacterData.Numbers.NONE, CharacterData.Settings.NONE))));
            return l;
        }, List.of()), "unknown mira"), "an aim the game doesn't know");
        helper.assertTrue(refused(with(d, l -> l, List.of(new CharacterData.Trait("teia", CharacterData.Settings.NONE))), "unknown passiva"),
                "a trait the game doesn't know");
        List<CharacterData.Trait> five = Traits.TYPES.subList(0, 5).stream().map(t -> new CharacterData.Trait(t, CharacterData.Settings.NONE)).toList();
        helper.assertTrue(refused(with(d, l -> l, five), "at most 4"), "5 traits are too many");
        helper.succeed();
    }

    /** V picks the function by how it is pressed and what is aimed at; the first that fits wins, else the default. */
    public static void functions(GameTestHelper helper) {
        CharacterData d = hero("prototipo").data();
        helper.setBlock(new BlockPos(7, 1, 9), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.DIRT);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        CharacterData.Ability strike = ability(d, "teste_golpe");
        CharacterData.Ability shot = ability(d, "teste_disparo");
        CharacterData.Ability gather = ability(d, "teste_coleta");
        look(helper, player, 7.5, 1.5, 9.0);
        helper.assertTrue(AbilityFunctions.choose(strike, AbilityFunctions.Press.TAP, player).function().preset().equals("cortar_arvore"),
                "aiming at wood the strike fells the tree");
        look(helper, player, 9.5, 1.5, 9.0);
        helper.assertTrue(AbilityFunctions.choose(gather, AbilityFunctions.Press.TAP, player).function().preset().equals("cavar_solto"),
                "aiming at earth the gathering digs");
        look(helper, player, 7.5, 6.0, 14.0);
        AbilityFunctions.Chosen plain = AbilityFunctions.choose(strike, AbilityFunctions.Press.TAP, player);
        helper.assertTrue(plain.index() == 0 && plain.function().preset().equals("golpe_forte"), "aiming at nothing, the default");
        helper.assertTrue(AbilityFunctions.choose(shot, AbilityFunctions.Press.HOLD, player).function().preset().equals("leque"), "held, the fan");
        helper.assertTrue(AbilityFunctions.choose(shot, AbilityFunctions.Press.TAP, player).index() == 0, "tapped, the shot");
        helper.assertTrue(AbilityFunctions.usesHold(shot) && !AbilityFunctions.usesHold(strike), "only the shot waits to see if V is held");
        helper.assertTrue(PresetRuntime.key("teste_disparo", 1).equals("teste_disparo#1"), "each function runs under a name of its own");
        remove(player);
        helper.succeed();
    }

    /** The traits work with the suit on and go away with it; voo makes a character fly. */
    public static void traits(GameTestHelper helper) {
        EngineHero colossus = hero("prototipo_colosso");
        ServerPlayer player = suitedAs(helper, "prototipo_colosso", 7.5, 7.5);
        double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH);
        helper.assertTrue(hero("prototipo_alado").flightProfile() != null, "voo: the winged one flies");
        helper.assertTrue(colossus.flightProfile() == null, "the colossus doesn't");
        helper.startSequence()
                .thenExecuteFor(21, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getAttributeValue(Attributes.JUMP_STRENGTH) > jump + 0.2, "pulo_alto jumps higher");
                    helper.assertTrue(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) > 1.5, "superforca breaks blocks faster");
                    colossus.dismissSuit(player, false);
                    helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - jump) < 1e-6, "and they go with the suit");
                    remove(player);
                })
                .thenSucceed();
    }

    /** The icon sheet has a cell for each ability, each extra function and each trait, and the emblem. */
    public static void icons(GameTestHelper helper) {
        CharacterData d = hero("prototipo").data();
        int functions = d.abilities().stream().mapToInt(a -> a.functions().size()).sum();
        helper.assertTrue(AbilityIcons.cells(d) == d.abilities().size() + functions + d.traits().size() + 1,
                "cells " + AbilityIcons.cells(d) + " for " + d.abilities().size() + " abilities, " + functions + " functions, " + d.traits().size() + " traits");
        helper.assertTrue(AbilityIcons.functionCell(d, 0, 0) == 0 && AbilityIcons.functionCell(d, 0, 1) >= d.abilities().size(),
                "the default function shows the ability's icon, the others their own");
        for (AbilityKind kind : AbilityKind.values()) helper.assertTrue(AbilityIcons.hasShape(AbilityIcons.kindShape(kind)), "an icon for " + kind.id());
        for (String trait : Traits.TYPES) helper.assertTrue(AbilityIcons.hasShape(AbilityIcons.traitShape(trait)), "an icon for " + trait);
        for (String preset : BATCH_6) helper.assertTrue(AbilityIcons.hasShape(AbilityIcons.presetShape(preset)), "an icon for " + preset);
        helper.succeed();
    }

    /** The world presets that were missing are there, and the great work, the grapple and the teleport count as world ones. */
    public static void registered(GameTestHelper helper) {
        for (String id : BATCH_6) helper.assertTrue(Presets.hasWorld(id), "missing world preset " + id);
        for (String id : List.of("grande_obra", "gancho", "teleporte")) helper.assertTrue(Presets.hasWorld(id), id + " is a world preset");
        helper.succeed();
    }

    // ---- the new world presets ---------------------------------------------------------------------

    public static void fellTree(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 0, 9), Blocks.DIRT);
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(7, y, 9), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(7, 5, 9), Blocks.OAK_LEAVES);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        look(helper, player, 7.5, 2.5, 9.0);
        helper.assertTrue(useWorld(player, world("cortar_arvore", 6, 0, 0, PresetTests.settings())), "the tree comes down");
        helper.assertTrue(count(helper, Blocks.OAK_LOG) == 0, "all its logs, " + count(helper, Blocks.OAK_LOG) + " left");
        helper.assertTrue(is(helper, 7, 1, 9, Blocks.OAK_SAPLING), "and a sapling stands where it was");
        helper.assertTrue(items(helper).stream().anyMatch(i -> i.getItem().is(Items.OAK_LOG)), "the logs drop");
        remove(player);
        helper.succeed();
    }

    public static void dig(GameTestHelper helper) {
        for (int x = 6; x <= 8; x++) for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 9), Blocks.SAND);
        helper.setBlock(new BlockPos(7, 2, 9), Blocks.STONE);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        look(helper, player, 7.5, 2.5, 9.0);
        helper.assertTrue(useWorld(player, world("cavar_solto", 6, 0, 0, PresetTests.settings("tamanho", 3, "profundidade", 1))), "the digging goes off");
        helper.assertTrue(count(helper, Blocks.SAND) == 0, "the sand is gone, " + count(helper, Blocks.SAND) + " left");
        helper.assertTrue(is(helper, 7, 2, 9, Blocks.STONE), "but never stone");
        remove(player);
        helper.succeed();
    }

    public static void fish(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 5.5);
        look(helper, player, 7.5, 1.0, 9.5);
        helper.assertFalse(useWorld(player, world("pescar", 8, 0, 0, PresetTests.settings())), "no water, no fish");
        for (int x = 6; x <= 8; x++) for (int z = 8; z <= 10; z++) helper.setBlock(new BlockPos(x, 1, z), Fluids.WATER.defaultFluidState().createLegacyBlock());
        int before = items(helper).size();
        helper.assertTrue(useWorld(player, world("pescar", 8, 0, 0, PresetTests.settings("quantidade", 3))), "the fishing goes off");
        helper.assertTrue(items(helper).size() > before, "and the catch lands at the feet");
        remove(player);
        helper.succeed();
    }

    public static void project(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        player.getInventory().add(new ItemStack(Items.STONE_BRICKS, 64));
        EngineAbility plan = world("projeto_em_fantasma", 0, 0, 30, PresetTests.settings("estrutura", "minecraft:igloo/top", "distancia", 2));
        helper.assertTrue(useWorld(player, plan), "the ghost shows");
        helper.assertTrue(PresetRuntime.undoable(player, "teste_projeto_em_fantasma"), "and waits for the key again");
        helper.assertTrue(count(helper, Blocks.SNOW_BLOCK) == 0, "nothing is built yet");
        helper.assertTrue(useWorld(player, plan), "the key again builds it");
        helper.assertTrue(count(helper, Blocks.SNOW_BLOCK) == 0, "with no snow in the inventory, the snow pieces stay unbuilt");
        remove(player);
        helper.succeed();
    }

    public static void swap(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 1, 9), Blocks.SAND);
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.WHITE_WOOL);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        look(helper, player, 7.5, 1.5, 9.0);
        helper.assertTrue(useWorld(player, world("trocar_bloco", 8, 0, 0, PresetTests.settings("modo", "fornalha"))), "the furnace swap");
        helper.assertTrue(is(helper, 7, 1, 9, Blocks.GLASS), "sand into glass");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    look(helper, player, 9.5, 1.5, 9.0);
                    helper.assertTrue(useWorld(player, world("trocar_bloco", 8, 0, 0, PresetTests.settings("modo", "cor", "cor_nova", "blue"))), "the colour swap");
                    helper.assertTrue(is(helper, 9, 1, 9, Blocks.BLUE_WOOL), "white wool into blue");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void machine(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 1, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 1, 9), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(LeverBlock.FACING, net.minecraft.core.Direction.NORTH));
        ServerPlayer player = suited(helper, 7.5, 6.5);
        look(helper, player, 7.5, 1.5, 9.5);
        helper.assertTrue(useWorld(player, world("ligar_maquina", 16, 0, 0, PresetTests.settings())), "the lever flips from afar");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 1, 9))).getValue(LeverBlock.POWERED), "it is on");
        remove(player);
        helper.succeed();
    }

    public static void tame(GameTestHelper helper) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        wolf.snapTo(helper.absoluteVec(new Vec3(7.5, 1, 9.5)));
        helper.getLevel().addFreshEntity(wolf);
        Pig pig = dummyPig(helper, 9.5, 1, 7.5);
        pig.setNoAi(false);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("domar_e_guiar", 0, 6, 10, PresetTests.settings())), "the animals answer");
        helper.assertTrue(wolf.isTame() && player.getUUID().equals(wolf.getOwnerReference().getUUID()), "the wolf is the player's");
        helper.assertTrue(pig.isLeashed(), "the pig follows on a leash");
        remove(player);
        wolf.discard();
        pig.discard();
        helper.succeed();
    }

    public static void trail(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertFalse(useWorld(player, world("rastro", 0, 16, 2, PresetTests.settings("alvo", "mob"))), "no monster, no trail");
        dummy(helper, 7.5, 1, 13.5);
        helper.assertTrue(useWorld(player, world("rastro", 0, 16, 2, PresetTests.settings("alvo", "mob"))), "the trail leads to the monster");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(useWorld(player, world("rastro", 0, 16, 2, PresetTests.settings("alvo", "casa"))), "and home");
                    remove(player);
                })
                .thenSucceed();
    }

    private static Container chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (Container) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    public static void magnet(GameTestHelper helper) {
        Container chest = chest(helper, new BlockPos(7, 1, 9));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        ServerPlayer player = suited(helper, 7.5, 7.5);
        look(helper, player, 7.5, 1.5, 9.0);
        helper.assertTrue(useWorld(player, world("ima_de_itens", 6, 8, 5, PresetTests.settings())), "the chest empties into the inventory");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3 && chest.isEmpty(), "all three diamonds");
        remove(player);
        helper.succeed();
    }

    public static void helperTask(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, 0, 7), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(9, 1, 7), ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(7));
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("ajudante_de_trabalho", 0, 4, 3, PresetTests.settings("tarefa", "colher"))), "the helper comes");
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    var wheat = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, 1, 7)));
                    helper.assertTrue(wheat.is(Blocks.WHEAT) && !((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).isMaxAge(wheat),
                            "it picked the wheat and planted it again");
                    helper.assertTrue(items(helper).stream().anyMatch(i -> i.getItem().is(Items.WHEAT)), "the wheat drops");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void organize(GameTestHelper helper) {
        Container chest = chest(helper, new BlockPos(7, 1, 9));
        chest.setItem(5, new ItemStack(Items.STONE, 10));
        chest.setItem(2, new ItemStack(Items.APPLE, 3));
        chest.setItem(9, new ItemStack(Items.STONE, 20));
        ServerPlayer player = suited(helper, 7.5, 7.5);
        look(helper, player, 7.5, 1.5, 9.0);
        helper.assertTrue(useWorld(player, world("organizar", 6, 0, 0, PresetTests.settings("regra", "nome"))), "the chest is put in order");
        helper.assertTrue(chest.getItem(0).is(Items.APPLE) && chest.getItem(1).is(Items.STONE) && chest.getItem(1).getCount() == 30,
                "apples first, then the stone joined into one stack");
        helper.assertTrue(chest.getItem(2).isEmpty(), "and the rest is empty");
        remove(player);
        helper.succeed();
    }

    public static void takeAlong(GameTestHelper helper) {
        Pig pig = dummyPig(helper, 8.5, 1, 7.5);
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(useWorld(player, world("levar_junto", 0, 4, 10, PresetTests.settings())), "the pig comes along");
        player.teleportTo(player.getX(), player.getY() + 4, player.getZ() - 4);
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(pig.distanceTo(player) < 3.5, "it stays close behind, " + pig.distanceTo(player) + " away");
                    helper.assertTrue(PresetRuntime.stop(player, "teste_levar_junto"), "the key again lets it down");
                    helper.assertFalse(pig.isNoGravity(), "and it falls softly again");
                    remove(player);
                    pig.discard();
                })
                .thenSucceed();
    }
}
