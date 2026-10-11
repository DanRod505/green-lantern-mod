package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.entity.AmazonShieldEntity;
import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.wonderwoman.AmazonPower;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import com.danrod505.greenlantern.wonderwoman.WonderWomanServer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** Wonder Woman: armor and flight, Lasso of Truth, bracelets, shield and the Invisible Jet. */
public final class WonderWomanTests {
    private WonderWomanTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("wonder_woman_suit_and_flight", () -> WonderWomanTests::wonderWomanSuitAndFlight);
        tests.register("wonder_woman_lasso_capture", () -> WonderWomanTests::wonderWomanLassoCapture);
        tests.register("wonder_woman_bracelets", () -> WonderWomanTests::wonderWomanBracelets);
        tests.register("wonder_woman_shield_returns", () -> WonderWomanTests::wonderWomanShieldReturns);
        tests.register("wonder_woman_invisible_jet", () -> WonderWomanTests::wonderWomanInvisibleJet);
    }

    public static void wonderWomanSuitAndFlight(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        // Superman first, then Wonder Woman replaces him (one hero at a time).
        player.getInventory().add(new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get()));
        SupermanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(SupermanHelper.isSuited(player), "superman suit should be on");
        giveTiara(player, 1000);
        use(player); // not suited as Wonder Woman: right click summons the armor
        helper.assertTrue(WonderWomanHelper.isSuited(player), "wonder woman armor should be summoned");
        helper.assertFalse(SupermanHelper.isSuited(player), "the superman suit should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WONDER_WOMAN_HAIR.get()), "the hair replaces the helmet");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WONDER_WOMAN_BOOTS.get()), "boots should be worn");
        helper.assertTrue(player.getAbilities().mayfly, "Wonder Woman can fly");
        // Her flight is weaker than the Lantern's: lower top speed, and she never breaks the sound barrier.
        FlightProfile amazon = com.danrod505.greenlantern.wonderwoman.WonderWomanHero.INSTANCE.flightProfile();
        FlightProfile lantern = FlightProfile.lantern();
        helper.assertTrue(amazon.max() < lantern.max(), "her top speed should be below the Lantern's");
        helper.assertTrue(amazon.max() < amazon.barrier(), "she should stay below the sound barrier");
        helper.assertTrue(FlightProfile.of(player) == amazon || FlightProfile.of(player).max() == amazon.max(), "her own flight profile is used");
        WonderWomanHero.INSTANCE.updateSuitModifiers(player, true);
        helper.assertTrue(player.getMaxHealth() > 20.0F, "the armor should give extra health");
        WonderWomanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(WonderWomanHelper.isSuited(player), "the armor should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertFalse(player.getAbilities().mayfly, "survival player should not fly without the armor");
        remove(player);
        helper.succeed();
    }

    public static void wonderWomanLassoCapture(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack tiara = giveTiara(player, 1000);
        WonderWomanHero.INSTANCE.summonSuit(player);
        AmazonPower.POWERS.select(tiara, AmazonPower.LASSO_CAPTURE);
        net.minecraft.world.entity.animal.pig.Pig pig = dummyPig(helper, 7.5, 1, 7.5);
        // The loop leaves from her right hand, a little below the eyes: aim it at the middle of the pig.
        Vec3 eye = player.getEyePosition();
        Vec3 to = pig.getBoundingBox().getCenter().subtract(eye.add(0, -0.3, 0));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, to.horizontalDistance()));
        player.setXRot(pitch);
        player.xRotO = pitch;
        use(player);
        helper.assertTrue(LassoEntity.find(player) != null, "the lasso should be thrown");
        helper.assertTrue(WonderWomanHero.DIVINE_POWER.get(tiara).stored() < 1000, "the lasso should cost divine power");
        helper.startSequence()
                .thenExecuteFor(15, player::doTick)
                .thenExecute(() -> {
                    LassoEntity lasso = LassoEntity.find(player);
                    helper.assertTrue(LassoEntity.isBound(pig), "the lasso should catch the pig (lasso "
                            + (lasso == null ? "gone" : "state " + lasso.state() + " at " + lasso.position()) + ", pig at " + pig.position() + ", her at " + player.position() + ")");
                    double before = pig.distanceTo(player);
                    helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.LASSO_PULL), "pulling a bound creature should work");
                    helper.assertTrue(pig.getDeltaMovement().z < 0, "the pull should yank the pig toward her, distance " + before);
                    WonderWomanServer.usePower(player, tiara, AmazonPower.LASSO_CAPTURE);
                })
                .thenExecuteFor(3, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(LassoEntity.isBound(pig), "using capture again lets the pig go");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void wonderWomanBracelets(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack tiara = giveTiara(player, 1000);
        WonderWomanHero.INSTANCE.summonSuit(player);
        net.minecraft.world.entity.animal.pig.Pig front = dummyPig(helper, 7.5, 1, 9.5);
        net.minecraft.world.entity.animal.pig.Pig behind = dummyPig(helper, 7.5, 1, 5.0);
        helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.BRACELET_GUARD), "the bracelets should be raised");
        helper.assertTrue(WonderWomanServer.isGuarding(player), "she should be guarding");
        helper.assertTrue(WonderWomanServer.onAttacked(player, player.damageSources().mobAttack(front)), "a blow from the front is blocked");
        helper.assertFalse(WonderWomanServer.onAttacked(player, player.damageSources().mobAttack(behind)), "a blow from behind is not");
        WonderWomanServer.usePower(player, tiara, AmazonPower.BRACELET_GUARD);
        helper.assertFalse(WonderWomanServer.isGuarding(player), "using it again lowers the bracelets");
        float health = front.getHealth();
        helper.startSequence()
                .thenExecuteFor(8, player::doTick)
                .thenExecute(() -> {
                    int before = WonderWomanHero.DIVINE_POWER.get(tiara).stored();
                    helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.BRACELET_SHOCKWAVE), "the bracelets should clash");
                    helper.assertTrue(WonderWomanHero.DIVINE_POWER.get(tiara).stored() < before, "the shockwave should cost divine power");
                    for (net.minecraft.world.entity.animal.pig.Pig pig : List.of(front, behind)) {
                        helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the shockwave should hit every pig around");
                    }
                    remove(player);
                })
                .thenSucceed();
    }

    public static void wonderWomanShieldReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack tiara = giveTiara(player, 1000);
        WonderWomanHero.INSTANCE.summonSuit(player);
        player.getInventory().setSelectedSlot(1);
        helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.SWORD_AND_SHIELD), "the weapons should come");
        helper.assertTrue(player.getMainHandItem().is(ModItems.AMAZON_SWORD.get()), "the sword should be in her hand");
        helper.assertTrue(player.getOffhandItem().is(ModItems.AMAZON_SHIELD.get()), "the shield should be on her arm");
        helper.startSequence()
                .thenExecuteFor(8, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.SHIELD_THROW), "the shield should be thrown");
                    helper.assertTrue(AmazonShieldEntity.find(player) != null, "the shield should fly");
                    helper.assertTrue(player.getOffhandItem().isEmpty(), "the arm is empty while the shield flies");
                    AmazonShieldEntity.find(player).recall();
                })
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(AmazonShieldEntity.find(player) == null, "the shield should be back");
                    helper.assertTrue(WonderWomanHelper.shieldSlot(player) >= 0, "the shield should be on her arm again");
                    WonderWomanHero.INSTANCE.dismissSuit(player, false);
                    helper.assertTrue(WonderWomanHelper.swordSlot(player) < 0 && WonderWomanHelper.shieldSlot(player) < 0, "the weapons go away with the armor");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void wonderWomanInvisibleJet(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack tiara = giveTiara(player, 1000);
        WonderWomanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.INVISIBLE_JET), "the jet should come");
        helper.assertTrue(player.getVehicle() instanceof InvisibleJetEntity, "she should be aboard the jet");
        InvisibleJetEntity jet = (InvisibleJetEntity) player.getVehicle();
        helper.assertTrue(WonderWomanServer.usePower(player, tiara, AmazonPower.INVISIBLE_JET), "using it again sends the jet away");
        helper.assertTrue(jet.isRemoved(), "the jet should fly away");
        helper.assertFalse(player.isPassenger(), "she should be out of the jet");
        remove(player);
        helper.succeed();
    }
}
