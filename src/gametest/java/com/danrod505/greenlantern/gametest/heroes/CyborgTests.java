package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.cyborg.CyborgConfig;
import com.danrod505.greenlantern.cyborg.CyborgContent;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import com.danrod505.greenlantern.cyborg.CyborgPower;
import com.danrod505.greenlantern.cyborg.CyborgServer;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Cyborg: the battery (recharge, redstone, flight on battery), each power, and that the armored body
 * never takes fall damage. The hero contract tests check the suit, the texts and that every power runs.
 */
public final class CyborgTests {
    private CyborgTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("cyborg_energy_recharges", () -> CyborgTests::energyRecharges);
        tests.register("cyborg_redstone_recharge", () -> CyborgTests::redstoneRecharge);
        tests.register("cyborg_powers_spend_energy", () -> CyborgTests::powersSpendEnergy);
        tests.register("cyborg_flight_needs_battery", () -> CyborgTests::flightNeedsBattery);
        tests.register("cyborg_sonic_cannon", () -> CyborgTests::sonicCannon);
        tests.register("cyborg_shoulder_missiles", () -> CyborgTests::shoulderMissiles);
        tests.register("cyborg_tech_scan", () -> CyborgTests::techScan);
        tests.register("cyborg_machine_hack", () -> CyborgTests::machineHack);
        tests.register("cyborg_emp_burst", () -> CyborgTests::empBurst);
        tests.register("cyborg_self_repair", () -> CyborgTests::selfRepair);
        tests.register("cyborg_boom_tube", () -> CyborgTests::boomTube);
    }

    private static ItemStack giveItem(ServerPlayer player, int energy) {
        ItemStack item = HeroContractTests.chargedItem(CyborgHero.INSTANCE);
        CyborgHero.ENERGY.set(item, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        return player.getMainHandItem();
    }

    /** A suited Cyborg with a full battery. */
    private static ServerPlayer cyborg(GameTestHelper helper, double x, double z, float yaw) {
        ServerPlayer player = player(helper, x, 1, z, yaw, 0);
        giveItem(player, CyborgHero.ENERGY.capacity());
        helper.assertTrue(CyborgHero.INSTANCE.summonSuit(player), "the suit should go on");
        return player;
    }

    private static boolean use(ServerPlayer player, CyborgPower power) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(player.getMainHandItem()));
        return CyborgHero.INSTANCE.powers().use(player, player.getMainHandItem(), power.ordinal());
    }

    private static int energy(ServerPlayer player) {
        return CyborgHero.ENERGY.get(player.getMainHandItem()).stored();
    }

    public static void energyRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        CyborgHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = CyborgHero.ENERGY.get(item).stored();
                    int expected = 2 * CyborgConfig.RECHARGE_PER_SECOND.get();
                    helper.assertTrue(stored >= expected, "the energy should come back every second, got " + stored + " (expected " + expected + ")");
                    helper.assertTrue(stored < 2 * expected, "away from redstone it should come back at the normal rate, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void redstoneRecharge(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, 1, 7), Blocks.REDSTONE_BLOCK);
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        CyborgHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = CyborgHero.ENERGY.get(item).stored();
                    int expected = (int) Math.round(2 * CyborgConfig.RECHARGE_PER_SECOND.get() * CyborgConfig.REDSTONE_RECHARGE_MULTIPLIER.get());
                    helper.assertTrue(stored >= expected, "next to a redstone block the battery should charge faster, got " + stored + " (expected " + expected + ")");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void powersSpendEnergy(GameTestHelper helper) {
        for (CyborgPower power : CyborgPower.values()) {
            ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
            helper.assertTrue(use(player, power), power.id() + " should go off");
            int stored = energy(player);
            helper.assertTrue(stored == CyborgHero.ENERGY.capacity() - power.cost(), power.id() + " should cost " + power.cost() + ", left " + stored);
            CyborgHero.INSTANCE.dismissSuit(player, false);
            remove(player);
        }
        ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
        CyborgHero.ENERGY.set(player.getMainHandItem(), 10);
        helper.assertFalse(use(player, CyborgPower.SONIC_CANNON), "without battery the cannon should not fire");
        helper.assertTrue(energy(player) == 10, "a power that doesn't go off costs nothing");
        CyborgHero.INSTANCE.dismissSuit(player, false);
        remove(player);
        helper.succeed();
    }

    public static void flightNeedsBattery(GameTestHelper helper) {
        ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
        helper.assertTrue(CyborgHero.INSTANCE.flightProfile() != null, "Cyborg should power-fly");
        helper.assertTrue(CyborgHero.INSTANCE.flightProfile().max() < CyborgHero.INSTANCE.flightProfile().barrier(), "by default he flies below the sound barrier");
        helper.assertTrue(CyborgHero.INSTANCE.canFly(player, player.getMainHandItem()), "with battery the thrusters work");
        CyborgHero.ENERGY.set(player.getMainHandItem(), 0);
        helper.assertFalse(CyborgHero.INSTANCE.canFly(player, player.getMainHandItem()), "with an empty battery the thrusters stop");
        helper.assertTrue(CyborgHero.INSTANCE.fallDamageMultiplier(player, 30) == 0.0F, "the suit never takes fall damage");
        CyborgHero.INSTANCE.dismissSuit(player, false);
        remove(player);
        helper.succeed();
    }

    public static void sonicCannon(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.GLASS);
        ServerPlayer player = cyborg(helper, 7.5, 1.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 8.5);
        lookAt(player, zombie);
        float before = zombie.getHealth();
        helper.assertTrue(use(player, CyborgPower.SONIC_CANNON), "the cannon should fire");
        helper.startSequence()
                .thenExecuteFor(4, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < before, "the sonic wave should hurt the zombie");
                    helper.assertBlockNotPresent(Blocks.GLASS, new BlockPos(7, 2, 5));
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void shoulderMissiles(GameTestHelper helper) {
        ServerPlayer player = cyborg(helper, 7.5, 1.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 11.5);
        lookAt(player, zombie);
        float health = player.getHealth();
        helper.assertTrue(use(player, CyborgPower.SHOULDER_MISSILES), "the missiles should launch");
        helper.assertTrue(CyborgServer.missilesInFlight(player) == CyborgConfig.MISSILE_COUNT.get(), "every missile should be in the air");
        float before = zombie.getHealth();
        BlockPos floor = new BlockPos(7, 0, 11);
        var floorBefore = helper.getBlockState(floor);
        helper.startSequence()
                .thenExecuteFor(60, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(CyborgServer.missilesInFlight(player) == 0, "every missile should have exploded");
                    helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < before, "the missiles should find the zombie");
                    helper.assertTrue(player.getHealth() >= health, "Cyborg is never hurt by his own missiles");
                    helper.assertTrue(helper.getBlockState(floor) == floorBefore, "the missiles never break blocks");
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void techScan(GameTestHelper helper) {
        ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
        int cost = CyborgPower.TECH_SCAN.cost();
        helper.assertTrue(use(player, CyborgPower.TECH_SCAN), "the scan should switch on");
        helper.assertTrue(CyborgServer.isScanning(player.getMainHandItem()), "the Mother Box should say the scan is on");
        int[] atStart = new int[1];
        helper.startSequence()
                .thenExecute(() -> atStart[0] = energy(player))
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    // About two seconds of scanning, minus what came back on its own.
                    int spent = atStart[0] - energy(player) + 2 * CyborgConfig.RECHARGE_PER_SECOND.get();
                    helper.assertTrue(spent >= 2 * cost - 2, "the scan should keep costing per second, spent " + spent);
                    int before = energy(player);
                    helper.assertTrue(use(player, CyborgPower.TECH_SCAN), "using it again switches it off");
                    helper.assertFalse(CyborgServer.isScanning(player.getMainHandItem()), "the scan should be off");
                    helper.assertTrue(energy(player) == before, "switching off is free");
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void machineHack(GameTestHelper helper) {
        BlockPos door = new BlockPos(9, 1, 10);
        helper.setBlock(door, Blocks.IRON_DOOR.defaultBlockState());
        helper.setBlock(door.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        ServerPlayer player = cyborg(helper, 4.5, 4.5, 0);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new Vec3(4.5, 1, 9.5));
        golem.setNoAi(true);
        lookAt(player, golem);
        helper.assertTrue(use(player, CyborgPower.MACHINE_HACK), "the hack should go off");
        helper.assertTrue(CyborgServer.isAlly(player, golem), "the iron golem should become an ally");
        helper.assertTrue(golem.isPlayerCreated(), "a hacked golem never turns on Cyborg");
        // Then the iron door: no hand can open it, the hack can.
        Vec3 doorCenter = helper.absoluteVec(new Vec3(9.5, 1.5, 10.5));
        Vec3 eye = player.getEyePosition();
        Vec3 to = doorCenter.subtract(eye);
        player.setYRot((float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI));
        player.setXRot((float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI));
        golem.discard();
        helper.assertTrue(use(player, CyborgPower.MACHINE_HACK), "the hack should go off on the door");
        helper.assertTrue(helper.getBlockState(door).getValue(DoorBlock.OPEN), "the iron door should open");
        CyborgHero.INSTANCE.dismissSuit(player, false);
        remove(player);
        helper.succeed();
    }

    public static void empBurst(GameTestHelper helper) {
        BlockPos lamp = new BlockPos(10, 1, 7);
        helper.setBlock(new BlockPos(11, 1, 7), Blocks.REDSTONE_BLOCK);
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 11.5);
        helper.startSequence()
                .thenExecuteFor(4, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the lamp should be lit by the redstone block");
                    helper.assertTrue(use(player, CyborgPower.EMP_BURST), "the pulse should go off");
                    helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "the zombie should be stunned");
                    helper.assertFalse(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the pulse should switch the lamp off");
                })
                .thenExecuteFor(70, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the lamp should light again after a moment");
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void selfRepair(GameTestHelper helper) {
        ServerPlayer player = cyborg(helper, 7.5, 7.5, 0);
        player.setHealth(6.0F);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isDamageableItem()) chest.setDamageValue(100);
        helper.assertTrue(use(player, CyborgPower.SELF_REPAIR), "the repair should start");
        helper.assertTrue(!chest.isDamageableItem() || player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0, "the suit should be fixed at once");
        helper.startSequence()
                .thenExecuteFor(100, player::doTick)
                .thenExecute(() -> {
                    float healed = player.getHealth() - 6.0F;
                    helper.assertTrue(healed >= CyborgConfig.REPAIR_HEALTH.get().floatValue() - 1.0F, "the nanobots should heal over the repair, healed " + healed);
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void boomTube(GameTestHelper helper) {
        ServerPlayer player = cyborg(helper, 3.5, 3.5, 0);
        // Sneaking marks where the tube goes (free).
        player.setShiftKeyDown(true);
        int before = energy(player);
        helper.assertTrue(use(player, CyborgPower.BOOM_TUBE), "sneaking should mark the destination");
        helper.assertTrue(energy(player) == before, "marking is free");
        GlobalPos mark = player.getMainHandItem().get(CyborgContent.BOOM_MARK.get());
        helper.assertTrue(mark != null && mark.pos().equals(player.blockPosition()), "the mark should be where Cyborg stands");
        player.setShiftKeyDown(false);
        // A friend walks with him, far from the mark.
        Vec3 far = helper.absoluteVec(new Vec3(11.5, 1, 11.5));
        player.teleportTo(far.x, far.y, far.z);
        ServerPlayer friend = player(helper, 12.5, 1, 11.5, 0, 0);
        helper.assertTrue(use(player, CyborgPower.BOOM_TUBE), "the tube should open");
        helper.startSequence()
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.blockPosition().distSqr(mark.pos()) <= 1, "Cyborg should come out at the mark, at " + player.blockPosition());
                    helper.assertTrue(friend.blockPosition().distSqr(mark.pos()) <= 9, "the friend next to him should go through too, at " + friend.blockPosition());
                    CyborgHero.INSTANCE.dismissSuit(player, false);
                    remove(player);
                    remove(friend);
                })
                .thenSucceed();
    }
}
