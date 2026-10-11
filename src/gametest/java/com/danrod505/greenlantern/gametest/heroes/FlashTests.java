package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.SpeedLightningEntity;
import com.danrod505.greenlantern.entity.SpeedTornadoEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.flash.SafeSpot;
import com.danrod505.greenlantern.flash.SpeedFlags;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** The Flash: suit, Speed Force, lightning, tornado and phasing. */
public final class FlashTests {
    private FlashTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("flash_suit_summon_and_swap", () -> FlashTests::flashSuitSummonAndSwap);
        tests.register("flash_speed_force_charges", () -> FlashTests::flashSpeedForceCharges);
        tests.register("flash_lightning", () -> FlashTests::flashLightning);
        tests.register("flash_tornado", () -> FlashTests::flashTornado);
        tests.register("flash_phase_safe_exit", () -> FlashTests::flashPhaseSafeExit);
    }

    public static void flashSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        // Lantern uniform first, then the Flash suit replaces it (one hero at a time).
        ItemStack lanternRing = new ItemStack(ModItems.POWER_RING.get());
        RingEnergy.set(lanternRing, 1000);
        player.getInventory().add(lanternRing);
        LanternHero.INSTANCE.summonSuit(player);
        helper.assertTrue(RingHelper.isSuited(player), "lantern uniform should be on");
        giveFlashRing(player, 0);
        use(player); // not suited as the Flash: right click summons the suit
        helper.assertTrue(FlashHelper.isSuited(player), "flash suit should be summoned");
        helper.assertFalse(RingHelper.isSuited(player), "the lantern uniform should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.FLASH_MASK.get()), "cowl should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.FLASH_BOOTS.get()), "boots should be worn");
        helper.assertTrue(player.getAttributeValue(Attributes.STEP_HEIGHT) > 1.0, "the suit should let the Flash run up steps");
        FlashHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(FlashHelper.isSuited(player), "flash suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertTrue(player.getAttributeValue(Attributes.STEP_HEIGHT) < 1.0, "step height back to normal");
        remove(player);
        helper.succeed();
    }

    public static void flashSpeedForceCharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack ring = giveFlashRing(player, 0);
        FlashHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int idle = FlashHero.SPEED_FORCE.get(ring).stored();
                    helper.assertTrue(idle > 0, "the Speed Force should regenerate while suited, got " + idle);
                    // Reported running at top speed: charges much faster.
                    SpeedsterServer.onState(player, GLConfig.RUN_MAX_SPEED.get().floatValue(), SpeedFlags.RUNNING | SpeedFlags.SUPERSONIC);
                })
                .thenExecuteFor(20, () -> {
                    SpeedsterServer.onState(player, GLConfig.RUN_MAX_SPEED.get().floatValue(), SpeedFlags.RUNNING | SpeedFlags.SUPERSONIC);
                    player.doTick();
                })
                .thenExecute(() -> {
                    int stored = FlashHero.SPEED_FORCE.get(ring).stored();
                    helper.assertTrue(stored >= 20, "running should charge the Speed Force, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void flashLightning(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack ring = giveFlashRing(player, 1000);
        FlashHero.INSTANCE.summonSuit(player);
        SpeedsterPower.POWERS.select(ring, SpeedsterPower.LIGHTNING);
        Zombie zombie = dummy(helper, 7.5, 1, 9.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(!around(player, SpeedLightningEntity.class, 5).isEmpty(), "a bolt of lightning should be thrown");
        helper.assertTrue(FlashHero.SPEED_FORCE.get(ring).stored() == 1000 - SpeedsterPower.LIGHTNING.cost(), "lightning should cost Speed Force");
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "zombie should be struck");
            remove(player);
        });
    }

    public static void flashTornado(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 4.5, 0, 0);
        ItemStack ring = giveFlashRing(player, 1000);
        FlashHero.INSTANCE.summonSuit(player);
        SpeedsterPower.POWERS.select(ring, SpeedsterPower.TORNADO);
        Zombie zombie = dummy(helper, 8.5, 1, 7.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(SpeedTornadoEntity.find(player) != null, "the tornado should form");
        helper.startSequence()
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health, "the vortex should hurt creatures");
                    helper.assertTrue(FlashHero.SPEED_FORCE.get(ring).stored() < 1000 - SpeedsterPower.TORNADO.cost(), "the tornado should drain Speed Force over time");
                    SpeedsterServer.usePower(player, ring, SpeedsterPower.TORNADO); // second use stops it
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(SpeedTornadoEntity.find(player) == null, "the tornado should collapse");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void flashPhaseSafeExit(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack ring = giveFlashRing(player, 1000);
        FlashHero.INSTANCE.summonSuit(player);
        SpeedsterPower.POWERS.select(ring, SpeedsterPower.PHASE);
        use(player);
        helper.assertTrue(SpeedsterServer.isPhasing(player), "should be phasing");
        // Sink into the stone floor, like phasing through the ground.
        Vec3 inside = helper.absoluteVec(new Vec3(7.5, -0.6, 7.5));
        player.teleportTo(inside.x, inside.y, inside.z);
        helper.startSequence()
                .thenExecuteFor(5, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getHealth() == player.getMaxHealth(), "no suffocation while phasing");
                    player.teleportTo(inside.x, inside.y, inside.z);
                    helper.assertTrue(SafeSpot.isStuck(player), "the player should be inside the floor while phasing");
                    use(player); // second use ends the vibration
                    helper.assertFalse(SpeedsterServer.isPhasing(player), "phasing should end");
                    helper.assertFalse(SafeSpot.isStuck(player), "the player should be pushed out of the floor");
                    helper.assertTrue(player.getY() > inside.y + 0.5, "the player should be moved up, out of the floor");
                })
                .thenExecuteFor(10, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getHealth() == player.getMaxHealth(), "no suffocation damage after phasing");
                    remove(player);
                })
                .thenSucceed();
    }
}
