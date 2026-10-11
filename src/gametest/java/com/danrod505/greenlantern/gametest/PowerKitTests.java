package com.danrod505.greenlantern.gametest;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.hero.power.Area;
import com.danrod505.greenlantern.hero.power.BlockEffects;
import com.danrod505.greenlantern.hero.power.Bolts;
import com.danrod505.greenlantern.hero.power.Dash;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The power building blocks of hero/power (Targets, Area, BlockEffects, Hits, Dash, Bolts) that new
 * heroes build their powers from. Each test drives a block on its own, with no hero.
 */
public final class PowerKitTests {
    private PowerKitTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("power_kit_targets", () -> PowerKitTests::targets);
        tests.register("power_kit_block_effects", () -> PowerKitTests::blockEffects);
        tests.register("power_kit_hits", () -> PowerKitTests::hits);
        tests.register("power_kit_dash", () -> PowerKitTests::dash);
        tests.register("power_kit_bolts", () -> PowerKitTests::bolts);
    }

    /** Facing +z from the middle of the arena: a zombie and a pig ahead, a tamed wolf ahead, a zombie behind. */
    public static void targets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        Zombie ahead = dummy(helper, 7.5, 1, 11.5);
        Pig pig = dummyPig(helper, 8.5, 1, 11.5);
        Wolf wolf = helper.spawn(EntityType.WOLF, new Vec3(6.5, 1, 11.5));
        wolf.setNoAi(true);
        wolf.tame(player);
        Zombie behind = dummy(helper, 7.5, 1, 3.5);
        ServerPlayer other = player(helper, 7.5, 1, 10.5, 0, 0);

        Vec3 eye = player.getEyePosition();
        List<LivingEntity> cone = Targets.inCone(level, eye, player.getLookAngle(), 6, 35, e -> Targets.hittable(player, e));
        helper.assertTrue(cone.contains(ahead) && cone.contains(pig), "the zombie and the pig ahead should be in the cone");
        helper.assertFalse(cone.contains(wolf), "a tamed wolf is never a target");
        helper.assertFalse(cone.contains(other), "players are never targets");
        helper.assertFalse(cone.contains(behind), "the zombie behind is outside the cone");
        helper.assertTrue(cone.get(0) == ahead, "nearest first: the zombie straight ahead comes first");
        helper.assertTrue(Targets.enemy(player, ahead) && !Targets.enemy(player, pig), "zombies are enemies, pigs are not");
        List<LivingEntity> around = Targets.inRadius(level, player.position(), 5, e -> Targets.enemy(player, e));
        helper.assertTrue(around.contains(ahead) && around.contains(behind) && !around.contains(pig), "enemies all around within 5 blocks");
        Vec3 flat = Targets.horizontal(player);
        helper.assertTrue(flat.y == 0 && flat.z > 0.99, "looking south, flattened: " + flat);
        remove(player);
        remove(other);
        helper.succeed();
    }

    public static void blockEffects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        BlockPos glass = new BlockPos(7, 2, 6);
        BlockPos sideGlass = new BlockPos(3, 2, 3);
        BlockPos torch = new BlockPos(8, 1, 7);
        BlockPos fire = new BlockPos(6, 1, 7);
        BlockPos water = new BlockPos(7, 0, 8);
        helper.setBlock(glass, Blocks.GLASS);
        helper.setBlock(sideGlass, Blocks.GLASS);
        helper.setBlock(torch, Blocks.TORCH);
        helper.setBlock(fire, Blocks.FIRE);
        helper.setBlock(water, Blocks.WATER);

        List<BlockPos> cone = Area.cone(player.getEyePosition(), player.getLookAngle().add(0, -0.25, 0), 7, 35);
        helper.assertTrue(cone.contains(helper.absolutePos(glass)), "the glass ahead is in the cone");
        helper.assertFalse(cone.contains(helper.absolutePos(sideGlass)), "the glass to the side is not");
        helper.assertTrue(Area.sphere(Vec3.atCenterOf(helper.absolutePos(sideGlass)), 0.6).size() == 1, "a tiny sphere is one block");

        helper.assertTrue(BlockEffects.shatterGlass(level, cone, player) >= 1, "the glass should shatter");
        helper.assertTrue(BlockEffects.snuffFire(level, cone, player, true) >= 2, "the fire and the torch should go out");
        helper.assertTrue(BlockEffects.freeze(level, cone, false) >= 1, "the water should freeze");
        helper.assertBlockNotPresent(Blocks.GLASS, glass);
        helper.assertBlockPresent(Blocks.GLASS, sideGlass);
        helper.assertBlockNotPresent(Blocks.TORCH, torch);
        helper.assertBlockNotPresent(Blocks.FIRE, fire);
        helper.assertBlockPresent(Blocks.ICE, water);
        remove(player);
        helper.succeed();
    }

    public static void hits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 9.5);
        zombie.setNoAi(false);
        zombie.setTarget(player);
        float before = zombie.getHealth();
        helper.assertTrue(Hits.strike(level, zombie, level.damageSources().playerAttack(player), 4.0F), "the first strike lands");
        helper.assertTrue(Hits.strike(level, zombie, level.damageSources().playerAttack(player), 4.0F), "a second strike right away lands too");
        helper.assertTrue(zombie.getHealth() < before - 6.0F, "two strikes of 4 both land (a zombie has a little armor): " + zombie.getHealth());
        Hits.stun(zombie, player, 60);
        helper.assertTrue(zombie.getTarget() == null, "a stunned mob forgets its target");
        helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS) && zombie.hasEffect(MobEffects.WEAKNESS), "and is slowed and weakened");
        Hits.knockAway(zombie, player.position(), 1.5, 0.4);
        helper.assertTrue(zombie.getDeltaMovement().z > 1.0 && zombie.getDeltaMovement().y > 0.3, "knocked away from the player: " + zombie.getDeltaMovement());
        remove(player);
        helper.succeed();
    }

    public static void dash(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 5.5);
        List<LivingEntity> rammed = new ArrayList<>();
        Dash dash = new Dash();
        Vec3 start = player.position();
        dash.start(player, player.getLookAngle(), 8, 6);
        helper.assertTrue(dash.active(), "the dash starts");
        helper.startSequence()
                .thenExecuteFor(8, () -> dash.tick(player, level, (target, dir) -> rammed.add(target)))
                .thenExecute(() -> {
                    double went = player.position().distanceTo(start);
                    helper.assertTrue(went > 7.5 && went < 8.5, "8 blocks forward, went " + went);
                    helper.assertTrue(!dash.active(), "and then it ends");
                    helper.assertTrue(rammed.size() == 1 && rammed.get(0) == zombie, "the zombie in the way is rammed once");
                    // A wall stops the next dash.
                    for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(7, y, 12), Blocks.STONE);
                    dash.start(player, player.getLookAngle(), 8, 4);
                })
                .thenExecuteFor(5, () -> dash.tick(player, level, (target, dir) -> {}))
                .thenExecute(() -> {
                    double z = helper.relativeVec(player.position()).z;
                    helper.assertTrue(z < 12.0 && z > 10.5, "the wall stops the dash right in front of it, z " + z);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void bolts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 10.5);
        List<Bolts.Impact> impacts = new ArrayList<>();
        Bolts bolts = new Bolts(20, 0.0);
        Vec3 eye = player.getEyePosition();
        bolts.fire(eye, Targets.middle(zombie).subtract(eye).normalize().scale(1.5), null);
        // Straight up from the side: it hits the barrier ceiling of the arena (a block).
        bolts.fire(eye.add(-3, 0, 0), new Vec3(0, 1.5, 0), null);
        // A rocket fired sideways turns towards its target.
        List<Bolts.Impact> rocketHits = new ArrayList<>();
        Bolts rockets = new Bolts(40, 0.3);
        rockets.fire(eye, new Vec3(1.0, 0.2, 0.2), zombie);
        helper.startSequence()
                .thenExecuteFor(40, () -> {
                    bolts.tick(player, level, (from, to) -> {}, impacts::add);
                    rockets.tick(player, level, (from, to) -> {}, rocketHits::add);
                })
                .thenExecute(() -> {
                    helper.assertTrue(bolts.isEmpty() && rockets.isEmpty(), "every bolt has ended");
                    helper.assertTrue(impacts.stream().filter(i -> i.entity() == zombie).count() == 1, "the straight bolt hits the zombie");
                    helper.assertTrue(impacts.stream().anyMatch(i -> i.block() != null), "the bolt shot up stops at a block");
                    helper.assertTrue(rocketHits.size() == 1 && rocketHits.get(0).entity() == zombie, "the rocket homes in on the zombie");
                    remove(player);
                })
                .thenSucceed();
    }
}
