package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightFlags;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/** Shared by every hero: the guide, data packs, the creative tab and the power flight (sound barrier, landing shockwave, cost). */
public final class SharedTests {
    private SharedTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("guide_given_on_first_join", () -> SharedTests::guideGivenOnFirstJoin);
        tests.register("data_loaded", () -> SharedTests::dataLoaded);
        tests.register("creative_tab_order", () -> SharedTests::creativeTabOrder);
        tests.register("sonic_boom_requires_speed", () -> SharedTests::sonicBoomRequiresSpeed);
        tests.register("hero_landing_shockwave", () -> SharedTests::heroLandingShockwave);
        tests.register("flight_cost_scales_with_speed", () -> SharedTests::flightCostScalesWithSpeed);
    }

    public static void guideGivenOnFirstJoin(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.GUIDE_BOOK.get())), "new players should get the Corps Manual");
        remove(player);
        helper.succeed();
    }

    public static void sonicBoomRequiresSpeed(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 3, 7.5, 0, 0);
        giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        player.getAbilities().flying = true;
        var state = ServerFlightTracker.get(player);
        long before = state.lastBoom();
        ServerFlightTracker.onState(player, 0.8F, FlightFlags.POWER);
        ServerFlightTracker.onAction(player, FlightAction.SONIC_BOOM);
        helper.assertTrue(state.lastBoom() == before, "a slow Lantern must not trigger a sonic boom");
        ServerFlightTracker.onState(player, GLConfig.SOUND_BARRIER_SPEED.get().floatValue() + 0.1F, FlightFlags.POWER | FlightFlags.SUPERSONIC);
        ServerFlightTracker.onAction(player, FlightAction.SONIC_BOOM);
        helper.assertTrue(state.lastBoom() != before, "breaking the sound barrier should be accepted");
        helper.assertTrue(ServerFlightTracker.speedFraction(player) > 0.5F, "speed fraction should follow the reported speed");
        ServerFlightTracker.onState(player, 50.0F, FlightFlags.POWER);
        helper.assertTrue(state.speed <= GLConfig.MAX_FLIGHT_SPEED.get().floatValue(), "reported speed must be clamped");
        remove(player);
        helper.succeed();
    }

    public static void heroLandingShockwave(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        player.getAbilities().flying = true;
        Zombie near = dummy(helper, 9.5, 1, 7.5);
        Zombie far = dummy(helper, 14.5, 1, 14.5);
        float nearHealth = near.getHealth();
        float farHealth = far.getHealth();
        // Without speed the landing is rejected.
        ServerFlightTracker.onAction(player, FlightAction.HERO_LANDING);
        helper.assertTrue(near.getHealth() == nearHealth, "a slow landing must not cause a shockwave");
        ServerFlightTracker.onState(player, 3.0F, FlightFlags.POWER | FlightFlags.SUPERSONIC);
        ServerFlightTracker.onAction(player, FlightAction.HERO_LANDING);
        helper.assertTrue(near.getHealth() < nearHealth, "nearby creatures should be hit by the hero landing");
        helper.assertTrue(far.getHealth() == farHealth, "creatures far away should not be hit");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the Lantern must not hurt themselves");
        remove(player);
        helper.succeed();
    }

    public static void flightCostScalesWithSpeed(GameTestHelper helper) {
        ServerPlayer slow = player(helper, 4.5, 3, 7.5, 0, 0);
        ServerPlayer fast = player(helper, 10.5, 3, 7.5, 0, 0);
        ItemStack slowRing = giveRing(slow, 1000);
        ItemStack fastRing = giveRing(fast, 1000);
        LanternHero.INSTANCE.summonSuit(slow);
        LanternHero.INSTANCE.summonSuit(fast);
        slow.getAbilities().flying = true;
        fast.getAbilities().flying = true;
        helper.startSequence()
                .thenExecuteFor(61, () -> {
                    ServerFlightTracker.onState(slow, 0.0F, 0);
                    ServerFlightTracker.onState(fast, GLConfig.MAX_FLIGHT_SPEED.get().floatValue(), FlightFlags.POWER | FlightFlags.SUPERSONIC);
                    slow.doTick();
                    fast.doTick();
                })
                .thenExecute(() -> {
                    int slowUsed = 1000 - RingEnergy.get(slowRing).stored();
                    int fastUsed = 1000 - RingEnergy.get(fastRing).stored();
                    helper.assertTrue(slowUsed > 0, "hovering flight should cost energy");
                    helper.assertTrue(fastUsed >= slowUsed * 2, "supersonic flight should cost much more: " + slowUsed + " vs " + fastUsed);
                    remove(slow);
                    remove(fast);
                })
                .thenSucceed();
    }

    public static void dataLoaded(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(ModDamageTypes.HARD_LIGHT).isPresent(), "damage type should load");
        for (String recipe : new String[] {"power_ring", "power_battery", "guide_book"}) {
            var key = ResourceKey.create(Registries.RECIPE, GreenLantern.id(recipe));
            helper.assertTrue(server.getRecipeManager().byKey(key).isPresent(), "recipe " + recipe + " should load");
        }
        helper.assertTrue(PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())).getBarWidth() == 13, "charged ring shows a full bar");
        helper.succeed();
    }

    /** Each hero lists its own items in the creative tab: the tab keeps its order (heroes in registry order). */
    public static void creativeTabOrder(GameTestHelper helper) {
        var level = helper.getLevel();
        var tab = com.danrod505.greenlantern.registry.ModCreativeTabs.MAIN.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
        List<net.minecraft.world.item.Item> items = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        List<net.minecraft.world.item.Item> expected = List.of(ModItems.POWER_RING.get(), ModItems.POWER_RING.get(), ModItems.POWER_BATTERY.get(),
                ModItems.GUIDE_BOOK.get(), ModItems.FLASH_RING.get(), ModItems.FLASH_RING.get(), ModItems.AQUAMAN_EMBLEM.get(), ModItems.AQUAMAN_EMBLEM.get(),
                ModItems.ATLANTIS_GATE.get(), ModItems.ATLANTEAN_RESPIRATOR.get(), ModItems.MANTA_RAY_EGG.get(), ModItems.GIANT_SEAHORSE_EGG.get(),
                ModItems.ATLANTEAN_DOLPHIN_EGG.get(), ModItems.TRENCH_CREATURE_EGG.get(), ModItems.TRENCH_BRUTE_EGG.get(), ModItems.UTILITY_BELT.get(),
                ModItems.UTILITY_BELT.get(), ModItems.KRYPTONIAN_CRYSTAL.get(), ModItems.KRYPTONIAN_CRYSTAL.get(), ModItems.AMAZON_TIARA.get(),
                ModItems.AMAZON_TIARA.get());
        helper.assertTrue(items.equals(expected), "creative tab order changed: " + items);
        helper.succeed();
    }
}
