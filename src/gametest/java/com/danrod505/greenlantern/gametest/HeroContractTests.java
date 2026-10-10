package com.danrod505.greenlantern.gametest;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The hero contract: checks every hero in {@link HeroRegistry} the same way, so a new hero is tested
 * on the basics the moment it is listed there, before it has a single test of its own.
 * <ul>
 *     <li>{@code hero_contracts}: unique id, the charged item first in the creative tab, names in
 *     English and Portuguese for the item, the suit and every power, the wheel icon sheet wide enough
 *     for every power, and the suit sounds declared in {@code sounds.json}.</li>
 *     <li>{@code hero_suits_contract}: the suit goes on with the hero's item (taking off the
 *     previous hero's), the shared keys talk to this hero, and taking it off gives the old armor back.</li>
 *     <li>{@code hero_powers_contract}: every power can be selected and used without errors, and
 *     using it never adds energy.</li>
 * </ul>
 */
public final class HeroContractTests {
    private static final String[] LOCALES = {"en_us", "pt_br"};

    private HeroContractTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("hero_contracts", () -> HeroContractTests::heroContracts);
        tests.register("hero_suits_contract", () -> HeroContractTests::heroSuitsContract);
        tests.register("hero_powers_contract", () -> HeroContractTests::heroPowersContract);
    }

    // ---- helpers ----------------------------------------------------------------------------------

    /** The hero's item as the creative tab hands it out first (charged), or EMPTY if the tab doesn't have it. */
    public static ItemStack chargedItem(HeroDefinition hero) {
        List<ItemStack> items = new ArrayList<>();
        hero.creativeTabItems((stack, visibility) -> items.add(stack));
        for (ItemStack stack : items) {
            if (hero.isActivator(stack)) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private static InputStream resource(String path) {
        InputStream in = HeroContractTests.class.getResourceAsStream("/" + path);
        if (in == null) in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        return in;
    }

    private static JsonObject json(GameTestHelper helper, String path) {
        try (InputStream in = resource(path)) {
            helper.assertTrue(in != null, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    /** Width of a PNG in the mod's assets (read from its header), or -1 if it is missing. */
    private static int pngWidth(String path) {
        try (InputStream in = resource(path)) {
            if (in == null) return -1;
            byte[] header = in.readNBytes(24);
            if (header.length < 24) return -1;
            return ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        } catch (IOException e) {
            return -1;
        }
    }

    private static List<Item> suitPieces(SuitSet suit) {
        List<Item> pieces = new ArrayList<>();
        for (Supplier<? extends Item> piece : List.of(nonNull(suit.head()), suit.chest(), nonNull(suit.legs()), nonNull(suit.feet()))) {
            Item item = piece.get();
            if (item != Items.AIR) pieces.add(item);
        }
        return pieces;
    }

    private static Supplier<? extends Item> nonNull(Supplier<? extends Item> piece) {
        return piece == null ? () -> Items.AIR : piece;
    }

    /** Removes what the powers left in the arena (creatures, vehicles, projectiles), keeping the players. Only inside this test: the others run next to it. */
    private static void sweep(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        for (Entity entity : helper.getLevel().getEntities((Entity) null, bounds, e -> !(e instanceof Player))) {
            entity.discard();
        }
    }

    // ---- tests ------------------------------------------------------------------------------------

    public static void heroContracts(GameTestHelper helper) {
        JsonObject sounds = json(helper, "assets/" + GreenLantern.MODID + "/sounds.json");
        List<JsonObject> langs = new ArrayList<>();
        for (String locale : LOCALES) langs.add(json(helper, "assets/" + GreenLantern.MODID + "/lang/" + locale + ".json"));
        Set<String> ids = new HashSet<>();
        for (HeroDefinition hero : HeroRegistry.all()) {
            String who = "hero " + hero.id() + ": ";
            helper.assertTrue(hero.id().matches("[a-z][a-z0-9_]*"), who + "id must be lowercase letters, digits and _");
            helper.assertTrue(ids.add(hero.id()), who + "two heroes with the same id");

            ItemStack item = chargedItem(hero);
            helper.assertTrue(!item.isEmpty(), who + "the creative tab must hand out the hero's item");
            List<String> keys = new ArrayList<>();
            keys.add(item.getItem().getDescriptionId());
            keys.add(hero.suit().missingItemKey());
            for (Item piece : suitPieces(hero.suit())) keys.add(piece.getDescriptionId());

            for (var sound : List.of(hero.suit().upSound(), hero.suit().downSound())) {
                var id = BuiltInRegistries.SOUND_EVENT.getKey(sound.get());
                helper.assertTrue(id != null && (!id.getNamespace().equals(GreenLantern.MODID) || sounds.has(id.getPath())),
                        who + "suit sound " + id + " is not in sounds.json");
            }

            HeroPowers<?> powers = hero.powers();
            if (powers != null) {
                WheelStyle wheel = powers.wheel();
                keys.add(wheel.costKey());
                keys.add(wheel.energyKey());
                int count = powers.set().count();
                helper.assertTrue(count > 0, who + "a power wheel needs at least one power");
                String icons = "assets/" + wheel.icons().getNamespace() + "/" + wheel.icons().getPath();
                int width = pngWidth(icons);
                helper.assertTrue(width == wheel.iconsWidth(), who + icons + " is " + width + " pixels wide, the wheel says " + wheel.iconsWidth());
                Set<String> powerIds = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    HeroPower power = powers.power(i);
                    helper.assertTrue(powerIds.add(power.id()), who + "two powers with the id " + power.id());
                    helper.assertTrue(power.cost() >= 0, who + "power " + power.id() + " has a negative cost");
                    helper.assertTrue((power.iconIndex() + 1) * 16 <= wheel.iconsWidth(), who + "no icon for power " + power.id());
                    keys.add("power." + GreenLantern.MODID + "." + power.id());
                    keys.add("power." + GreenLantern.MODID + "." + power.id() + ".desc");
                }
            }

            for (int l = 0; l < LOCALES.length; l++) {
                for (String key : keys) {
                    helper.assertTrue(langs.get(l).has(key), who + "missing " + LOCALES[l] + " text " + key);
                }
            }
        }
        helper.succeed();
    }

    public static void heroSuitsContract(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        HeroDefinition previous = null;
        for (HeroDefinition hero : HeroRegistry.all()) {
            String who = "hero " + hero.id() + ": ";
            ItemStack item = chargedItem(hero);
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            helper.assertTrue(HeroRegistry.context(player).orElse(null) == (previous != null ? previous : hero),
                    who + "a worn suit keeps the keys until it comes off");
            helper.assertTrue(hero.summonSuit(player), who + "the suit should go on with the hero's item in hand");
            helper.assertTrue(hero.isSuited(player), who + "should be suited");
            helper.assertTrue(HeroRegistry.suited(player).orElse(null) == hero, who + "the registry should see this hero's suit");
            helper.assertTrue(HeroRegistry.context(player).orElse(null) == hero, who + "the shared keys should talk to this hero");
            for (HeroDefinition other : HeroRegistry.all()) {
                helper.assertFalse(other != hero && other.isSuited(player), who + "the suit of " + other.id() + " should come off");
            }
            previous = hero;
        }
        // The last suit comes off and the armor worn before the first one comes back.
        previous.dismissSuit(player, false);
        helper.assertTrue(HeroRegistry.suited(player).isEmpty(), "no suit should be left on");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the iron helmet should come back");
        remove(player);
        helper.succeed();
    }

    public static void heroPowersContract(GameTestHelper helper) {
        GameTestSequence sequence = helper.startSequence();
        for (HeroDefinition hero : HeroRegistry.all()) {
            HeroPowers<?> powers = hero.powers();
            if (powers == null) continue;
            for (int i = 0; i < powers.set().count(); i++) {
                int index = i;
                String who = "hero " + hero.id() + ", power " + powers.power(index).id() + ": ";
                ServerPlayer[] player = new ServerPlayer[1];
                sequence.thenExecute(() -> {
                    player[0] = player(helper, 7.5, 1, 4.5, 0, 0);
                    dummy(helper, 7.5, 1, 10.5);
                    ItemStack item = chargedItem(hero);
                    player[0].setItemInHand(InteractionHand.MAIN_HAND, item);
                    helper.assertTrue(hero.summonSuit(player[0]), who + "the suit should go on");
                    ItemStack held = player[0].getMainHandItem();
                    powers.select(player[0], held, index, false);
                    helper.assertTrue(powers.selectedIndex(held) == index, who + "the wheel should select it");
                    int before = powers.energy().get(held).stored();
                    try {
                        powers.use(player[0], held, -1);
                    } catch (RuntimeException e) {
                        GreenLantern.LOGGER.error("{}failed", who, e);
                        helper.fail(who + "threw " + e);
                    }
                    int after = powers.energy().get(player[0].getMainHandItem()).stored();
                    helper.assertTrue(after <= before, who + "using a power must not add energy (" + before + " -> " + after + ")");
                })
                        .thenExecuteFor(10, () -> player[0].doTick())
                        .thenExecute(() -> {
                            if (hero.isSuited(player[0])) hero.dismissSuit(player[0], false);
                            remove(player[0]);
                            sweep(helper);
                        });
            }
        }
        sequence.thenSucceed();
    }
}
