package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.SkinState;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.hero.SuitModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The character engine (2.0.0). The first 9 heroes moved their suit to data files: the snapshot
 * below holds the numbers of version 1.18.0, copied from the code before the move, so any change
 * in the files shows here. The other tests use the test characters (src/gametest/resources), which
 * never reach the released jar.
 */
public final class EngineTests {
    private EngineTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("engine_legacy_suits_unchanged", () -> EngineTests::legacySuitsUnchanged);
        tests.register("engine_characters_load", () -> EngineTests::charactersLoad);
        tests.register("engine_class_stats", () -> EngineTests::classStats);
        tests.register("engine_energy_and_cooldown", () -> EngineTests::energyAndCooldown);
        tests.register("engine_supreme", () -> EngineTests::supreme);
        tests.register("engine_skins_and_wardrobe", () -> EngineTests::skinsAndWardrobe);
        tests.register("engine_world_use", () -> EngineTests::worldUse);
    }

    /** A suit of version 1.18.0: armor of each piece, toughness and knockback resistance per piece, sounds, repair tag, look and bonuses. */
    private record Snapshot(int head, int chest, int legs, int feet, double toughness, double knockback, String sound, String repair,
                            String look, List<String> bonuses) {}

    private static final Map<String, Snapshot> V1_18 = Map.of(
            "lantern", new Snapshot(3, 8, 6, 3, 2.0, 0.1, "item.armor.equip_generic", "repairs_netherite_armor", "green_lantern", List.of()),
            "flash", new Snapshot(2, 7, 5, 3, 1.0, 0.0, "item.armor.equip_leather", "repairs_leather_armor", "flash", List.of(
                    "movement_speed flash_speed 0.35 add_multiplied_base", "step_height flash_step 0.65 add_value")),
            "aquaman", new Snapshot(3, 8, 6, 3, 2.0, 0.0, "item.armor.equip_turtle", "repairs_turtle_helmet", "aquaman", List.of(
                    "water_movement_efficiency aquaman_water 1.0 add_value", "submerged_mining_speed aquaman_mining 4.0 add_value",
                    "attack_damage aquaman_strength 2.0 add_value", "knockback_resistance aquaman_knockback 0.4 add_value")),
            "batman", new Snapshot(3, 8, 6, 3, 2.5, 0.1, "item.armor.equip_leather", "repairs_leather_armor", "batman", List.of(
                    "attack_damage batman_strength 3.0 add_value", "knockback_resistance batman_knockback 0.3 add_value",
                    "movement_speed batman_speed 0.1 add_multiplied_base")),
            "superman", new Snapshot(4, 9, 7, 4, 4.0, 0.2, "item.armor.equip_netherite", "repairs_netherite_armor", "superman", List.of(
                    "attack_damage superman_strength 10.0 add_value", "attack_knockback superman_punch_knockback 2.0 add_value",
                    "max_health superman_health 20.0 add_value", "movement_speed superman_speed 0.35 add_multiplied_base",
                    "knockback_resistance superman_steady 1.0 add_value", "explosion_knockback_resistance superman_blast_proof 1.0 add_value",
                    "step_height superman_step 0.65 add_value", "jump_strength superman_jump 0.25 add_value",
                    "block_break_speed superman_mining 1.5 add_multiplied_base", "entity_interaction_range superman_reach 1.5 add_value",
                    "block_interaction_range superman_block_reach 1.5 add_value", "oxygen_bonus superman_breath 8.0 add_value")),
            "wonder_woman", new Snapshot(3, 8, 6, 3, 3.0, 0.1, "item.armor.equip_gold", "repairs_gold_armor", "wonder_woman", List.of(
                    "attack_damage wonder_woman_strength 7.0 add_value", "max_health wonder_woman_health 16.0 add_value",
                    "movement_speed wonder_woman_speed 0.25 add_multiplied_base", "attack_speed wonder_woman_attack_speed 0.3 add_multiplied_base",
                    "knockback_resistance wonder_woman_steady 0.8 add_value", "step_height wonder_woman_step 0.5 add_value",
                    "jump_strength wonder_woman_jump 0.2 add_value", "entity_interaction_range wonder_woman_reach 1.0 add_value",
                    "oxygen_bonus wonder_woman_breath 4.0 add_value")),
            "cyborg", new Snapshot(3, 8, 6, 3, 2.0, 0.0, "item.armor.equip_generic", "repairs_diamond_armor", "cyborg", List.of(
                    "attack_damage cyborg_strength 3.0 add_value", "max_health cyborg_health 8.0 add_value",
                    "movement_speed cyborg_speed 0.15 add_multiplied_base")),
            "supergirl", new Snapshot(3, 8, 6, 3, 2.0, 0.0, "item.armor.equip_generic", "repairs_diamond_armor", "supergirl", List.of(
                    "attack_damage supergirl_strength 3.0 add_value", "max_health supergirl_health 8.0 add_value",
                    "movement_speed supergirl_speed 0.15 add_multiplied_base")),
            "shazam", new Snapshot(3, 8, 6, 3, 2.0, 0.0, "item.armor.equip_generic", "repairs_diamond_armor", "shazam", List.of(
                    "attack_damage shazam_strength 4.0 add_value", "max_health shazam_health 8.0 add_value",
                    "movement_speed shazam_speed 0.15 add_multiplied_base")));

    private static String describe(SuitModifier modifier) {
        String attribute = modifier.attribute().unwrapKey().orElseThrow().identifier().getPath();
        return attribute + " " + modifier.id().getPath() + " " + modifier.amount() + " " + modifier.operation().getSerializedName();
    }

    private static EngineHero engineHero(String id) {
        return (EngineHero) HeroRegistry.byId(id).orElseThrow(() -> new IllegalStateException("no character " + id));
    }

    /** A suited test character with a full energy bar. */
    private static ServerPlayer suited(GameTestHelper helper, String id, double x, double z) {
        EngineHero hero = engineHero(id);
        ServerPlayer player = player(helper, x, 1, z, 0, 0);
        ItemStack item = HeroContractTests.chargedItem(hero);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        helper.assertTrue(hero.summonSuit(player), id + ": the suit should go on");
        return player;
    }

    // ---- tests ------------------------------------------------------------------------------------

    /** The 9 heroes wear exactly the suit of version 1.18.0. */
    public static void legacySuitsUnchanged(GameTestHelper helper) {
        helper.assertTrue(V1_18.size() == 9, "the snapshot has the 9 heroes");
        for (Map.Entry<String, Snapshot> entry : V1_18.entrySet()) {
            String who = entry.getKey() + ": ";
            Snapshot want = entry.getValue();
            HeroDefinition hero = HeroRegistry.byId(entry.getKey()).orElseThrow();
            helper.assertFalse(hero instanceof EngineHero, who + "keeps its own code");
            List<String> bonuses = hero.suit().modifiers().stream().map(EngineTests::describe).toList();
            helper.assertTrue(bonuses.equals(want.bonuses()), who + "bonuses changed: " + bonuses);

            ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
            helper.assertTrue(hero.summonSuit(player), who + "the suit should go on");
            int[] armor = {want.head(), want.chest(), want.legs(), want.feet()};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
            for (int i = 0; i < 4; i++) {
                ItemStack piece = player.getItemBySlot(slots[i]);
                if (piece.isEmpty() && i != 1) continue; // some heroes have no helmet or boots
                Equippable equippable = piece.get(DataComponents.EQUIPPABLE);
                helper.assertTrue(equippable != null, who + slots[i] + " is not armor");
                helper.assertTrue(piece.getMaxDamage() == types[i].getDurability(1000), who + slots[i] + " durability " + piece.getMaxDamage());
                helper.assertTrue(equippable.equipSound().unwrapKey().orElseThrow().identifier().getPath().equals(want.sound()),
                        who + "equip sound " + equippable.equipSound());
                helper.assertTrue(equippable.assetId().map(k -> k.identifier().getPath()).orElse("").equals(want.look()),
                        who + "look " + equippable.assetId());
                var repair = piece.get(DataComponents.REPAIRABLE);
                helper.assertTrue(repair != null && repair.items().unwrapKey().map(t -> t.location().getPath()).orElse("").equals(want.repair()),
                        who + "repair tag " + (repair == null ? null : repair.items().unwrapKey()));
                double[] values = {0, 0, 0};
                piece.forEachModifier(slots[i], (attribute, modifier) -> {
                    if (attribute.is(Attributes.ARMOR)) values[0] += modifier.amount();
                    if (attribute.is(Attributes.ARMOR_TOUGHNESS)) values[1] += modifier.amount();
                    if (attribute.is(Attributes.KNOCKBACK_RESISTANCE)) values[2] += modifier.amount();
                });
                helper.assertTrue(values[0] == armor[i], who + slots[i] + " armor " + values[0] + ", was " + armor[i]);
                helper.assertTrue(Math.abs(values[1] - want.toughness()) < 1e-6, who + slots[i] + " toughness " + values[1]);
                helper.assertTrue(Math.abs(values[2] - want.knockback()) < 1e-6, who + slots[i] + " knockback " + values[2]);
            }
            hero.dismissSuit(player, false);
            remove(player);
        }
        helper.succeed();
    }

    /** Every character file loads; the 9 heroes list their powers in wheel order; the test characters are on the list. */
    public static void charactersLoad(GameTestHelper helper) {
        Map<String, CharacterData> all = Characters.all();
        for (String id : V1_18.keySet()) {
            CharacterData data = all.get(id);
            helper.assertTrue(data != null && data.legacy(), id + ": needs a legacy character file");
            HeroPowers<?> powers = HeroRegistry.byId(id).orElseThrow().powers();
            List<String> wheel = new ArrayList<>();
            if (powers != null) {
                for (int i = 0; i < powers.set().count(); i++) wheel.add(powers.power(i).id());
            }
            List<String> file = data.abilities().stream().map(CharacterData.Ability::id).toList();
            helper.assertTrue(wheel.equals(file), id + ": the file lists " + file + ", the wheel has " + wheel);
        }
        for (String id : List.of("prototipo", "prototipo_colosso", "prototipo_alado")) {
            EngineHero hero = engineHero(id);
            int count = hero.powers().set().count();
            helper.assertTrue(count >= 5 && count <= 8, id + ": 5 to 8 abilities, has " + count);
            helper.assertTrue(hero.powers().power(count - 1) instanceof EngineAbility a && a.supreme(), id + ": the last one is the supreme");
            helper.assertTrue(Characters.check(hero.data(), id).isEmpty(), id + ": " + Characters.check(hero.data(), id));
        }
        helper.succeed();
    }

    /** The class rule: hearts, damage, defense and speed from class and level (level 4 is Legendary). */
    public static void classStats(GameTestHelper helper) {
        for (String id : List.of("prototipo", "prototipo_colosso", "prototipo_alado")) {
            CharacterData data = engineHero(id).data();
            ServerPlayer player = suited(helper, id, 7.5, 7.5);
            helper.assertTrue(Math.abs(player.getMaxHealth() - data.hearts() * 2) < 1e-4, id + ": max health " + player.getMaxHealth() + " for " + data.hearts() + " hearts");
            double[] armor = {0};
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                player.getItemBySlot(slot).forEachModifier(slot, (attribute, modifier) -> {
                    if (attribute.is(Attributes.ARMOR)) armor[0] += modifier.amount();
                });
            }
            helper.assertTrue(Math.abs(armor[0] - data.defense()) < 1e-4, id + ": armor " + armor[0] + " for defense " + data.defense());
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0 - data.damage()) < 1e-4, id + ": damage");
            engineHero(id).dismissSuit(player, false);
            helper.assertTrue(player.getMaxHealth() == 20.0F, id + ": the bonus hearts go away with the suit");
            remove(player);
        }
        CharacterData colossus = engineHero("prototipo_colosso").data();
        helper.assertTrue(colossus.defense() == 24 && colossus.speed() == 35 && colossus.firmness() == 1.0, "Legendary: defense 24, speed 35, firmness 1");
        helper.succeed();
    }

    /** An ability spends its cost and waits for its cooldown; without energy it is refused. */
    public static void energyAndCooldown(GameTestHelper helper) {
        EngineHero hero = engineHero("prototipo");
        ServerPlayer player = suited(helper, "prototipo", 7.5, 7.5);
        ItemStack item = player.getMainHandItem();
        EngineAbility strike = (EngineAbility) hero.powers().power(0);
        int before = hero.energy().get(item).stored();
        helper.assertTrue(before == hero.energy().capacity(), "the creative tab hands out a full bar");
        helper.assertTrue(EngineServer.useAbility(player, item, strike), "the first use goes off");
        helper.assertTrue(hero.energy().get(item).stored() == before - strike.cost(), "it costs " + strike.cost());
        helper.assertTrue(EngineServer.state(item).remaining(0, player.level().getGameTime()) > 0, "then it cools down");
        helper.assertFalse(EngineServer.useAbility(player, item, strike), "a second use waits for the cooldown");
        helper.assertTrue(hero.energy().get(item).stored() == before - strike.cost(), "a refused use costs nothing");
        hero.energy().set(item, 0);
        EngineAbility shot = (EngineAbility) hero.powers().power(1);
        helper.assertFalse(EngineServer.useAbility(player, item, shot), "no energy, no ability");
        hero.dismissSuit(player, false);
        helper.assertFalse(EngineServer.useAbility(player, item, shot), "no suit, no ability");
        remove(player);
        helper.succeed();
    }

    /** The supreme ability has no bar: it goes off like the others and waits for its long cooldown. */
    public static void supreme(GameTestHelper helper) {
        EngineHero hero = engineHero("prototipo");
        ServerPlayer player = suited(helper, "prototipo", 7.5, 7.5);
        ItemStack item = player.getMainHandItem();
        int last = hero.powers().set().count() - 1;
        EngineAbility supreme = (EngineAbility) hero.powers().power(last);
        helper.assertTrue(supreme.supreme(), "the last ability is the supreme one");
        helper.assertTrue(EngineServer.useAbility(player, item, supreme), "it goes off with no bar to fill");
        long now = player.level().getGameTime();
        helper.assertTrue(EngineServer.state(item).remaining(last, now) > 20 * 50, "then waits for its long cooldown");
        helper.assertFalse(EngineServer.useAbility(player, item, supreme), "a second use is refused");
        hero.dismissSuit(player, false);
        remove(player);
        helper.succeed();
    }

    /** K and the Heroes' Wardrobe change to the next unlocked skin, and the chest piece carries it for the other players. */
    public static void skinsAndWardrobe(GameTestHelper helper) {
        EngineHero hero = engineHero("prototipo");
        ServerPlayer player = suited(helper, "prototipo", 7.5, 7.5);
        ItemStack item = player.getMainHandItem();
        helper.assertTrue(EngineServer.skin(hero, item).selected().equals("prototipo"), "the first skin is the default");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).get(EngineContent.SKIN.get()) != null, "the chest piece shows the skin");

        EngineServer.cycleSkin(player, hero, item);
        helper.assertTrue(EngineServer.skin(hero, item).selected().equals("prototipo"), "a locked skin is skipped in survival");

        item.set(EngineContent.SKIN.get(), EngineServer.skin(hero, item).unlock("prototipo_noite"));
        BlockPos pos = new BlockPos(7, 1, 9);
        helper.setBlock(pos, EngineContent.WARDROBE.get());
        BlockPos abs = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(abs);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false);
        state.useItemOn(item, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        SkinState after = EngineServer.skin(hero, item);
        helper.assertTrue(after.selected().equals("prototipo_noite"), "the wardrobe puts on the unlocked skin, has " + after.selected());
        SkinState chest = player.getItemBySlot(EquipmentSlot.CHEST).get(EngineContent.SKIN.get());
        helper.assertTrue(chest != null && chest.selected().equals("prototipo_noite"), "the chest piece follows the change");

        player.setGameMode(GameType.CREATIVE);
        EngineServer.cycleSkin(player, hero, item);
        helper.assertTrue(EngineServer.skin(hero, item).selected().equals("prototipo"), "the skins go round");
        hero.dismissSuit(player, false);
        helper.setBlock(pos, Blocks.AIR);
        remove(player);
        helper.succeed();
    }

    /** A world ability goes through the same checks as the others: energy and cooldown. */
    public static void worldUse(GameTestHelper helper) {
        EngineHero hero = engineHero("prototipo_colosso");
        ServerPlayer player = suited(helper, "prototipo_colosso", 7.5, 7.5);
        ItemStack item = player.getMainHandItem();
        EngineAbility level = null;
        for (int i = 0; i < hero.powers().set().count(); i++) {
            if (hero.powers().power(i) instanceof EngineAbility a && a.id().equals("teste_coleta")) level = a;
        }
        helper.assertTrue(level != null && !level.kind().fight(), "the colossus has a world ability");
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.DIRT);
        int before = hero.energy().get(item).stored();
        helper.assertTrue(EngineServer.useAbility(player, item, level), "the world ability goes off");
        helper.assertTrue(hero.energy().get(item).stored() < before, "and spends energy");
        helper.assertFalse(EngineServer.useAbility(player, item, level), "then waits for the cooldown");
        hero.dismissSuit(player, false);
        remove(player);
        helper.succeed();
    }
}
