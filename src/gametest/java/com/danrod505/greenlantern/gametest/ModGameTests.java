package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.aquaman.AquaPower;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.aquaman.AquamanServer;
import com.danrod505.greenlantern.aquaman.SeaCall;
import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.batman.BatmanServer;
import com.danrod505.greenlantern.block.PowerBatteryBlockEntity;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.entity.AmazonShieldEntity;
import com.danrod505.greenlantern.entity.AquaTridentEntity;
import com.danrod505.greenlantern.entity.BatDefenderEntity;
import com.danrod505.greenlantern.entity.BatarangEntity;
import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.danrod505.greenlantern.entity.BatmobileMissileEntity;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.DrillConstructEntity;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.entity.GrappleHookEntity;
import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.danrod505.greenlantern.entity.KrakenEntity;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.entity.SawConstructEntity;
import com.danrod505.greenlantern.entity.SpeedLightningEntity;
import com.danrod505.greenlantern.entity.SpeedTornadoEntity;
import com.danrod505.greenlantern.entity.TrenchCocoonEntity;
import com.danrod505.greenlantern.entity.TrenchCreatureEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.flash.SafeSpot;
import com.danrod505.greenlantern.flash.SpeedFlags;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightFlags;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.item.AquaTridentItem;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModBlocks;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.superman.SuperPower;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.superman.SupermanServer;
import com.danrod505.greenlantern.trench.Trench;
import com.danrod505.greenlantern.trench.TrenchBuilder;
import com.danrod505.greenlantern.trench.TrenchLife;
import com.danrod505.greenlantern.trench.TrenchNest;
import com.danrod505.greenlantern.wonderwoman.AmazonPower;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import com.danrod505.greenlantern.wonderwoman.WonderWomanServer;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * In-world integration tests. Run with {@code ./gradlew runGameTestServer -Pgametests}.
 * Every test uses a real (survival) server player placed in a 15x8x15 stone arena.
 */
public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TESTS = DeferredRegister.create(Registries.TEST_FUNCTION, GreenLantern.MODID);

    static {
        TESTS.register("uniform_summon_and_restore", () -> ModGameTests::uniformSummonAndRestore);
        TESTS.register("flight_drains_energy", () -> ModGameTests::flightDrainsEnergy);
        TESTS.register("power_out_dismisses_uniform", () -> ModGameTests::powerOutDismissesUniform);
        TESTS.register("energy_blast", () -> ModGameTests::energyBlast);
        TESTS.register("minigun", () -> ModGameTests::minigun);
        TESTS.register("bubble", () -> ModGameTests::bubble);
        TESTS.register("saw", () -> ModGameTests::saw);
        TESTS.register("hammer", () -> ModGameTests::hammer);
        TESTS.register("drill", () -> ModGameTests::drill);
        TESTS.register("mecha", () -> ModGameTests::mecha);
        TESTS.register("oa_portal", () -> ModGameTests::oaPortal);
        TESTS.register("guide_given_on_first_join", () -> ModGameTests::guideGivenOnFirstJoin);
        TESTS.register("lantern_charges_ring", () -> ModGameTests::lanternChargesRing);
        TESTS.register("data_loaded", () -> ModGameTests::dataLoaded);
        TESTS.register("creative_tab_order", () -> ModGameTests::creativeTabOrder);
        TESTS.register("sonic_boom_requires_speed", () -> ModGameTests::sonicBoomRequiresSpeed);
        TESTS.register("hero_landing_shockwave", () -> ModGameTests::heroLandingShockwave);
        TESTS.register("flight_cost_scales_with_speed", () -> ModGameTests::flightCostScalesWithSpeed);
        TESTS.register("flash_suit_summon_and_swap", () -> ModGameTests::flashSuitSummonAndSwap);
        TESTS.register("flash_speed_force_charges", () -> ModGameTests::flashSpeedForceCharges);
        TESTS.register("flash_lightning", () -> ModGameTests::flashLightning);
        TESTS.register("flash_tornado", () -> ModGameTests::flashTornado);
        TESTS.register("flash_phase_safe_exit", () -> ModGameTests::flashPhaseSafeExit);
        TESTS.register("aquaman_suit_summon_and_swap", () -> ModGameTests::aquamanSuitSummonAndSwap);
        TESTS.register("aquaman_breathes_and_regens", () -> ModGameTests::aquamanBreathesAndRegens);
        TESTS.register("aquaman_trident_throw_returns", () -> ModGameTests::aquamanTridentThrowReturns);
        TESTS.register("aquaman_shark_bite", () -> ModGameTests::aquamanSharkBite);
        TESTS.register("aquaman_sea_call", () -> ModGameTests::aquamanSeaCall);
        TESTS.register("atlantean_mounts_hatch_and_ride", () -> ModGameTests::atlanteanMountsHatchAndRide);
        TESTS.register("aquaman_kraken_call", () -> ModGameTests::aquamanKrakenCall);
        TESTS.register("aquaman_kraken_fights_and_falls", () -> ModGameTests::aquamanKrakenFightsAndFalls);
        TESTS.register("respirator_breathes_with_any_suit", () -> ModGameTests::respiratorBreathesWithAnySuit);
        TESTS.register("respirator_refills_out_of_water", () -> ModGameTests::respiratorRefillsOutOfWater);
        TESTS.register("atlantis_build_and_travel", () -> ModGameTests::atlantisBuildAndTravel);
        TESTS.register("atlantis_portal_power", () -> ModGameTests::atlantisPortalPower);
        TESTS.register("batman_suit_summon_and_swap", () -> ModGameTests::batmanSuitSummonAndSwap);
        TESTS.register("batman_belt_recharges", () -> ModGameTests::batmanBeltRecharges);
        TESTS.register("batman_batarang_returns", () -> ModGameTests::batmanBatarangReturns);
        TESTS.register("batman_grapple_hooks_and_releases", () -> ModGameTests::batmanGrappleHooksAndReleases);
        TESTS.register("batman_bat_swarm", () -> ModGameTests::batmanBatSwarm);
        TESTS.register("batman_batmobile", () -> ModGameTests::batmanBatmobile);
        TESTS.register("superman_suit_summon_and_swap", () -> ModGameTests::supermanSuitSummonAndSwap);
        TESTS.register("superman_solar_recharge", () -> ModGameTests::supermanSolarRecharge);
        TESTS.register("superman_heat_vision", () -> ModGameTests::supermanHeatVision);
        TESTS.register("superman_super_punch_area", () -> ModGameTests::supermanSuperPunchArea);
        TESTS.register("superman_super_breath", () -> ModGameTests::supermanSuperBreath);
        TESTS.register("superman_xray_toggle", () -> ModGameTests::supermanXrayToggle);
        TESTS.register("wonder_woman_suit_and_flight", () -> ModGameTests::wonderWomanSuitAndFlight);
        TESTS.register("wonder_woman_lasso_capture", () -> ModGameTests::wonderWomanLassoCapture);
        TESTS.register("wonder_woman_bracelets", () -> ModGameTests::wonderWomanBracelets);
        TESTS.register("wonder_woman_shield_returns", () -> ModGameTests::wonderWomanShieldReturns);
        TESTS.register("wonder_woman_invisible_jet", () -> ModGameTests::wonderWomanInvisibleJet);
        TESTS.register("trench_nest_build", () -> ModGameTests::trenchNestBuild);
        TESTS.register("trench_creature_hunts", () -> ModGameTests::trenchCreatureHunts);
        TESTS.register("trench_creature_grabs_villager", () -> ModGameTests::trenchCreatureGrabsVillager);
        TESTS.register("trench_cocoon_rescue", () -> ModGameTests::trenchCocoonRescue);
        TESTS.register("trench_raid", () -> ModGameTests::trenchRaid);
    }

    private ModGameTests() {}

    public static void register(BusGroup modBus) {
        TESTS.register(modBus);
        if (System.getenv("GL_CLIENT_SCRIPT") != null
                && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            ClientScript.register();
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, double x, double y, double z, float yaw, float pitch) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "lantern-" + helper.getTick());
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 abs = helper.absoluteVec(new Vec3(x, y, z));
        player.snapTo(abs.x, abs.y, abs.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static ItemStack giveRing(ServerPlayer player, int energy) {
        ItemStack ring = new ItemStack(ModItems.POWER_RING.get());
        RingEnergy.set(ring, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);
        return player.getMainHandItem();
    }

    private static void select(ServerPlayer player, Construct construct) {
        ConstructRegistry.select(player.getMainHandItem(), construct);
    }

    private static void use(ServerPlayer player) {
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static void remove(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }

    private static Zombie dummy(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(x, y, z));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        return zombie;
    }

    private static <T extends net.minecraft.world.entity.Entity> List<T> around(ServerPlayer player, Class<T> type, double radius) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(radius));
    }

    // ---- tests ------------------------------------------------------------------------------------

    public static void uniformSummonAndRestore(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        giveRing(player, 1000);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);

        use(player); // not suited: right click summons the uniform
        helper.assertTrue(RingHelper.isSuited(player), "uniform should be summoned");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LANTERN_MASK.get()), "mask should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LANTERN_BOOTS.get()), "boots should be worn");
        helper.assertTrue(player.getAbilities().mayfly, "suited player should be able to fly");

        LanternHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(RingHelper.isSuited(player), "uniform should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertFalse(player.getAbilities().mayfly, "survival player should not fly without the uniform");
        remove(player);
        helper.succeed();
    }

    public static void flightDrainsEnergy(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 3, 7.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        player.getAbilities().flying = true;
        helper.startSequence()
                .thenExecuteFor(61, player::doTick)
                .thenExecute(() -> {
                    int stored = RingEnergy.get(ring).stored();
                    helper.assertTrue(stored < 1000 && stored >= 990, "flight should drain a little energy, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void powerOutDismissesUniform(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        RingEnergy.set(ring, 0);
        helper.startSequence()
                .thenExecuteFor(3, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(RingHelper.isSuited(player), "uniform should fade when the ring is empty");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void energyBlast(GameTestHelper helper) {
        // Player looks south (+Z) at a zombie 6 blocks away.
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.ENERGY_BLAST);
        Zombie zombie = dummy(helper, 7.5, 1, 8.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(!around(player, EnergyBoltEntity.class, 4).isEmpty(), "a bolt should be fired");
        helper.assertTrue(RingEnergy.get(ring).stored() == 1000 - ConstructRegistry.ENERGY_BLAST.activationCost(), "blast should cost energy");
        helper.assertTrue(player.getCooldowns().isOnCooldown(ring), "blast should put the ring on cooldown");
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "zombie should be hit by the blast");
            remove(player);
        });
    }

    public static void minigun(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.MINIGUN);
        Zombie zombie = dummy(helper, 7.5, 1, 9.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(player.isUsingItem(), "minigun is used while holding the button");
        helper.startSequence()
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(!around(player, GunConstructEntity.class, 4).isEmpty(), "gun construct should float next to the player");
                    helper.assertTrue(RingEnergy.get(ring).stored() < 1000, "shots should cost energy");
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "zombie should be shot");
                    player.releaseUsingItem();
                })
                .thenExecuteFor(3, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(around(player, GunConstructEntity.class, 4).isEmpty(), "gun should vanish on release");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void bubble(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.BUBBLE);
        use(player);
        helper.assertTrue(BubbleConstructEntity.find(player) != null, "bubble should be raised");
        // A zombie with AI that tries to reach the player (mobs without AI can't be pushed).
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(8.5, 1, 7.5));
        zombie.setTarget(player);

        helper.startSequence()
                .thenExecuteFor(65, player::doTick) // wait out the spawn invulnerability
                .thenExecute(() -> {
                    helper.assertTrue(zombie.position().distanceTo(player.position()) > 1.5, "bubble should push creatures away");
                    float before = player.getHealth();
                    DamageSource source = player.damageSources().generic();
                    player.hurtServer(player.level(), source, 10.0F);
                    float lost = before - player.getHealth();
                    helper.assertTrue(lost > 0 && lost < 3.0F, "bubble should absorb most damage, lost " + lost);
                    int stored = RingEnergy.get(ring).stored();
                    helper.assertTrue(stored < 1000 - ConstructRegistry.BUBBLE.activationCost(), "bubble should drain energy over time");
                    use(player);
                })
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(BubbleConstructEntity.find(player) == null, "second click should lower the bubble");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void saw(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.SAW);
        use(player);
        helper.assertTrue(player.getVehicle() instanceof SawConstructEntity, "player should ride the saw");
        SawConstructEntity saw = (SawConstructEntity) player.getVehicle();
        Zombie zombie = dummy(helper, 7.5, 1, 3.5 + SawConstructEntity.BLADE_OFFSET);
        BlockPos leaves = helper.absolutePos(new BlockPos(7, 2, 5));
        helper.getLevel().setBlockAndUpdate(leaves, Blocks.OAK_LEAVES.defaultBlockState());
        float health = zombie.getHealth();

        helper.startSequence()
                .thenExecuteFor(25, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "saw blade should hurt the zombie");
                    helper.assertTrue(helper.getLevel().getBlockState(leaves).isAir(), "saw should cut leaves");
                    helper.assertTrue(RingEnergy.get(ring).stored() < 1000 - ConstructRegistry.SAW.activationCost(), "saw should drain energy");
                    player.stopRiding();
                })
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(saw.isRemoved(), "saw should vanish when the rider leaves");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void drill(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.DRILL);
        use(player);
        helper.assertTrue(player.getVehicle() instanceof DrillConstructEntity, "player should ride the drill");
        DrillConstructEntity drill = (DrillConstructEntity) player.getVehicle();
        Zombie zombie = dummy(helper, 7.5, 1, 6.0);
        // Ore and stone right in front of the bit.
        BlockPos ore = helper.absolutePos(new BlockPos(7, 1, 5));
        BlockPos stone = helper.absolutePos(new BlockPos(7, 2, 5));
        helper.getLevel().setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        BlockPos floor = helper.absolutePos(new BlockPos(7, 0, 5));
        float health = zombie.getHealth();
        player.setLastClientInput(new Input(true, false, false, false, false, false, false));

        helper.startSequence()
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "drill bit should hurt the zombie");
                    helper.assertTrue(helper.getLevel().getBlockState(ore).isAir(), "drill should mine the ore");
                    helper.assertTrue(helper.getLevel().getBlockState(stone).isAir(), "drill should mine the stone");
                    helper.assertFalse(helper.getLevel().getBlockState(floor).isAir(), "driving level keeps the floor");
                    helper.assertTrue(player.getInventory().contains(new ItemStack(Items.RAW_IRON)), "mined ore should go to the rider's inventory");
                    helper.assertTrue(RingEnergy.get(ring).stored() < 1000 - ConstructRegistry.DRILL.activationCost(), "drill should drain energy");
                    player.setLastClientInput(Input.EMPTY);
                    player.stopRiding();
                })
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(drill.isRemoved(), "drill should vanish when the rider leaves");
                    remove(player);
                })
                .thenSucceed();
    }

    /** Turns the player to look at the middle of the given entity. */
    private static void lookAt(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI);
        float pitch = (float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }

    public static void oaPortal(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack ring = giveRing(player, 3000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.PORTAL);
        use(player);
        List<com.danrod505.greenlantern.entity.OaPortalEntity> portals = around(player, com.danrod505.greenlantern.entity.OaPortalEntity.class, 6.0);
        helper.assertTrue(portals.size() == 1, "the construct should open one portal, found " + portals.size());
        helper.assertTrue(RingEnergy.get(player.getMainHandItem()).stored() <= 3000 - ConstructRegistry.PORTAL.activationCost(), "the portal should cost energy");
        var portal = portals.getFirst();
        if (com.danrod505.greenlantern.oa.Oa.level(player.level().getServer()) == null) {
            // The game test server doesn't load datapack dimensions: check the city builder here instead
            // (travelling through the portal is covered by the scripted client run, GL_CLIENT_SCRIPT=oa).
            ServerLevel level = player.level();
            com.danrod505.greenlantern.oa.OaCity.ensureBuilt(level);
            helper.assertTrue(com.danrod505.greenlantern.oa.OaCity.isBuilt(level), "the city of Oa should be built");
            helper.assertTrue(level.getBlockState(new BlockPos(0, com.danrod505.greenlantern.oa.OaCity.BATTERY_TOP + 6, 0)).is(Blocks.BEACON), "the Central Power Battery should shine a beacon");
            AABB city = new AABB(-60, 0, -60, 60, 200, 60);
            int guardians = level.getEntitiesOfClass(com.danrod505.greenlantern.entity.OaGuardianEntity.class, city).size();
            int lanterns = level.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, city).size();
            helper.assertTrue(guardians == com.danrod505.greenlantern.oa.OaCity.PILLARS, "a Guardian should stand on every pillar, found " + guardians);
            helper.assertTrue(lanterns >= 10, "Lanterns should live on Oa, found " + lanterns);
            remove(player);
            helper.succeed();
            return;
        }
        helper.assertTrue(portal.distanceTo(player) > 2.0, "the portal should open in front of the player");
        ServerLevel home = player.level();
        Vec3 start = portal.position();
        helper.startSequence()
                .thenIdle(com.danrod505.greenlantern.entity.OaPortalEntity.OPEN_TICKS + 2)
                .thenExecute(() -> {
                    player.teleportTo(portal.getX(), portal.getY(), portal.getZ());
                    helper.assertTrue(portal.insideOpening(player), "the player should stand in the portal; portal=" + portal.position() + " player=" + player.position());
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(com.danrod505.greenlantern.oa.Oa.is(player.level()), "walking through the portal should take the player to Oa, level=" + player.level().dimension()
                            + " portalAge=" + portal.tickCount + " removed=" + portal.isRemoved());
                    ServerLevel oa = player.level();
                    helper.assertTrue(com.danrod505.greenlantern.oa.OaCity.isBuilt(oa), "the city of Oa should be built");
                    helper.assertTrue(oa.getBlockState(new BlockPos(0, com.danrod505.greenlantern.oa.OaCity.BATTERY_TOP + 6, 0)).is(Blocks.BEACON), "the Central Power Battery should shine a beacon");
                    AABB city = new AABB(-60, 0, -60, 60, 200, 60);
                    int guardians = oa.getEntitiesOfClass(com.danrod505.greenlantern.entity.OaGuardianEntity.class, city).size();
                    int lanterns = oa.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, city).size();
                    helper.assertTrue(guardians == com.danrod505.greenlantern.oa.OaCity.PILLARS, "a Guardian should stand on every pillar, found " + guardians);
                    helper.assertTrue(lanterns >= 10, "Lanterns should live on Oa, found " + lanterns);
                    // The Central Power Battery recharges rings nearby.
                    player.teleportTo(oa, 0.5 + 15, com.danrod505.greenlantern.oa.Oa.GROUND_Y, 0.5, java.util.Set.of(), 0, 0, false);
                    RingEnergy.set(RingHelper.findRing(player), 100);
                })
                .thenExecuteFor(20, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(RingEnergy.get(RingHelper.findRing(player)).stored() > 100, "the Central Power Battery should recharge the ring");
                    // On Oa the construct opens the way home.
                    use(player);
                })
                .thenIdle(com.danrod505.greenlantern.entity.OaPortalEntity.OPEN_TICKS + 2)
                .thenExecute(() -> {
                    List<com.danrod505.greenlantern.entity.OaPortalEntity> back = around(player, com.danrod505.greenlantern.entity.OaPortalEntity.class, 6.0);
                    helper.assertTrue(back.size() == 1 && back.getFirst().leadsHome(), "a portal home should open on Oa");
                    var exit = back.getFirst();
                    player.teleportTo(exit.getX(), exit.getY(), exit.getZ());
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(player.level() == home, "the portal home should return the player to their world");
                    helper.assertTrue(player.position().distanceTo(start) < 2.0, "the player should be back where they stepped into the portal, distance=" + player.position().distanceTo(start));
                    remove(player);
                })
                .thenSucceed();
    }

    public static void mecha(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack ring = giveRing(player, 3000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.MECHA);
        use(player);
        if (!(player.getVehicle() instanceof MechaEntity)) {
            // Report what is in the way of the 10 block giant.
            StringBuilder blocking = new StringBuilder();
            AABB room = new AABB(player.getX() - 1.8, player.getY(), player.getZ() - 1.8, player.getX() + 1.8, player.getY() + 10.0, player.getZ() + 1.8);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(room.minX, room.minY, room.minZ), BlockPos.containing(room.maxX, room.maxY, room.maxZ))) {
                var state = helper.getLevel().getBlockState(pos);
                if (!state.isAir() && blocking.length() < 300) blocking.append(state.getBlock()).append('@').append(pos.getY()).append(' ');
            }
            helper.fail(net.minecraft.network.chat.Component.literal("player should pilot the mecha; vehicle=" + player.getVehicle()
                    + " energy=" + RingEnergy.get(ring).stored() + " blocking=" + blocking));
        }
        MechaEntity mecha = (MechaEntity) player.getVehicle();
        helper.assertTrue(RingEnergy.get(ring).stored() <= 3000 - ConstructRegistry.MECHA.activationCost(), "summoning the mecha should cost energy");
        Zombie target = dummy(helper, 7.5, 1, 11.5);
        Zombie second = dummy(helper, 2.5, 1, 13.5);
        float health = target.getHealth();

        helper.startSequence()
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getEyePosition().y - mecha.getY() > 6.0, "the cockpit should be high up in the chest");
                    lookAt(player, target);
                    mecha.setLaserFiring(true);
                })
                .thenExecuteFor(20, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(mecha.isFiringLaser(), "laser should be firing");
                    helper.assertTrue(target.getHealth() < health || target.isDeadOrDying(), "laser should hurt the zombie");
                    mecha.setLaserFiring(false);
                    lookAt(player, second);
                    helper.assertTrue(mecha.fireMissiles(), "a missile salvo should launch");
                    helper.assertFalse(mecha.fireMissiles(), "the pods need to reload between salvos");
                })
                .thenExecuteFor(70, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(second.isDeadOrDying() || second.getHealth() < second.getMaxHealth(), "homing missiles should hit the second zombie");
                    helper.assertFalse(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 0, 13))).isAir(), "missiles don't break blocks by default");
                    // The cockpit shields the pilot, paid for with ring energy.
                    int before = RingEnergy.get(ring).stored();
                    float pilotHealth = player.getHealth();
                    player.hurtServer(player.level(), player.damageSources().generic(), 10.0F);
                    helper.assertTrue(player.getHealth() == pilotHealth, "the mecha should shield its pilot");
                    helper.assertTrue(RingEnergy.get(ring).stored() < before, "absorbing a hit should cost energy");
                    player.stopRiding();
                })
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(mecha.isRemoved(), "mecha should fade when the pilot climbs out");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void guideGivenOnFirstJoin(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.GUIDE_BOOK.get())), "new players should get the Corps Manual");
        remove(player);
        helper.succeed();
    }

    public static void hammer(GameTestHelper helper) {
        // Look down at the floor 4 blocks ahead where two zombies stand.
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 35);
        ItemStack ring = giveRing(player, 1000);
        LanternHero.INSTANCE.summonSuit(player);
        select(player, ConstructRegistry.HAMMER);
        Zombie a = dummy(helper, 7.5, 1, 6.5);
        Zombie b = dummy(helper, 9.5, 1, 7.5);
        float ha = a.getHealth();
        float hb = b.getHealth();
        use(player);
        helper.assertTrue(!around(player, HammerConstructEntity.class, 12).isEmpty(), "hammer should appear");
        helper.assertTrue(RingEnergy.get(ring).stored() == 1000 - ConstructRegistry.HAMMER.activationCost(), "hammer should cost energy");
        helper.startSequence()
                .thenIdle(HammerConstructEntity.IMPACT_TICK + 4)
                .thenExecute(() -> {
                    helper.assertTrue(a.getHealth() < ha || a.isDeadOrDying(), "zombie A should be hit by the shockwave");
                    helper.assertTrue(b.getHealth() < hb || b.isDeadOrDying(), "zombie B should be hit by the shockwave");
                    helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the owner must not be hurt");
                })
                .thenIdle(HammerConstructEntity.LIFETIME)
                .thenExecute(() -> {
                    helper.assertTrue(around(player, HammerConstructEntity.class, 12).isEmpty(), "hammer should disappear");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void lanternChargesRing(GameTestHelper helper) {
        BlockPos lanternPos = new BlockPos(7, 1, 8);
        helper.setBlock(lanternPos, ModBlocks.POWER_BATTERY.get());
        ServerPlayer player = player(helper, 7.5, 1, 6.5, 0, 30);
        ItemStack ring = giveRing(player, 0);
        PowerBatteryBlockEntity battery = (PowerBatteryBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(lanternPos));
        helper.assertTrue(battery != null, "lantern should have a block entity");
        battery.toggleCharging(player);
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(RingEnergy.get(ring).stored() > 0, "ring should be charging"))
                .thenWaitUntil(() -> helper.assertTrue(RingEnergy.get(ring).isFull(), "ring should become full"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
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

    // ---- the Flash ----------------------------------------------------------------------------------

    private static ItemStack giveFlashRing(ServerPlayer player, int speedForce) {
        ItemStack ring = new ItemStack(ModItems.FLASH_RING.get());
        FlashHero.SPEED_FORCE.set(ring, speedForce);
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);
        return player.getMainHandItem();
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

    // ---- Aquaman ----------------------------------------------------------------------------------

    private static ItemStack giveEmblem(ServerPlayer player, int seaForce) {
        ItemStack emblem = new ItemStack(ModItems.AQUAMAN_EMBLEM.get());
        AquamanHero.SEA_FORCE.set(emblem, seaForce);
        player.setItemInHand(InteractionHand.MAIN_HAND, emblem);
        return player.getMainHandItem();
    }

    /** Fills the inside of the arena with water up to (relative) height {@code top}. */
    private static void flood(GameTestHelper helper, int top) {
        for (int x = 1; x <= 13; x++) {
            for (int y = 1; y <= top; y++) {
                for (int z = 1; z <= 13; z++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }
    }

    public static void aquamanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // The Flash first, then Aquaman replaces him (one hero at a time).
        ItemStack ring = new ItemStack(ModItems.FLASH_RING.get());
        player.getInventory().add(ring);
        FlashHero.INSTANCE.summonSuit(player);
        helper.assertTrue(FlashHelper.isSuited(player), "flash suit should be on");
        giveEmblem(player, 0);
        use(player); // not suited as Aquaman: right click summons the suit
        helper.assertTrue(AquamanHelper.isSuited(player), "aquaman suit should be summoned");
        helper.assertFalse(FlashHelper.isSuited(player), "the flash suit should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Aquaman has no mask: the helmet stays on");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.AQUAMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) >= 1.0, "the suit should make swimming easy");
        AquamanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(AquamanHelper.isSuited(player), "aquaman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet still on");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        helper.assertTrue(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) < 1.0, "swimming back to normal");
        remove(player);
        helper.succeed();
    }

    public static void aquamanBreathesAndRegens(GameTestHelper helper) {
        flood(helper, 5);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 0);
        AquamanHero.INSTANCE.summonSuit(player);
        player.setAirSupply(0);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    helper.assertTrue(player.getAirSupply() == player.getMaxAirSupply(), "Aquaman breathes underwater, air " + player.getAirSupply());
                    int stored = AquamanHero.SEA_FORCE.get(emblem).stored();
                    helper.assertTrue(stored >= 30, "the Power of the Seas should refill quickly in water, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanTridentThrowReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.TRIDENT);
        Zombie zombie = dummy(helper, 7.5, 1, 7.5);
        float health = zombie.getHealth();
        use(player);
        ItemStack trident = player.getMainHandItem();
        helper.assertTrue(AquamanHelper.isTrident(trident), "the trident should appear in the main hand");
        helper.assertFalse(AquamanHelper.findEmblem(player).isEmpty(), "the emblem should move to the inventory");
        helper.assertTrue(AquamanHero.SEA_FORCE.get(AquamanHelper.findEmblem(player)).stored() == 1000 - AquaPower.TRIDENT.cost(), "the trident should cost Power of the Seas");
        player.setXRot(10.0F);
        AquaTridentItem.throwTrident(player, trident);
        helper.assertTrue(AquamanHelper.tridentSlot(player) < 0, "the thrown trident leaves the hand");
        helper.assertTrue(AquaTridentEntity.find(player) != null, "the trident should be in flight");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "zombie should be hit"))
                .thenWaitUntil(() -> helper.assertTrue(AquamanHelper.tridentSlot(player) >= 0, "the trident should come back"))
                .thenExecute(() -> {
                    helper.assertTrue(AquaTridentEntity.find(player) == null, "no trident left in flight");
                    remove(player);
                })
                .thenSucceed();
    }

    private static Zombie sharkPrey;
    private static float sharkPreyHealth;

    public static void aquamanSharkBite(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.SHARK);
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    use(player);
                    GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
                    helper.assertTrue(shark != null, "a shark should be summoned");
                    helper.assertTrue(player.getVehicle() == shark, "Aquaman should ride the shark");
                    Vec3 mouth = shark.mouth();
                    Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    zombie.snapTo(mouth.x, mouth.y - 0.8, mouth.z, 180, 0);
                    zombie.setNoAi(true);
                    helper.getLevel().addFreshEntity(zombie);
                    sharkPrey = zombie;
                    sharkPreyHealth = zombie.getHealth();
                    helper.assertTrue(shark.bite(), "the shark should lunge at the zombie in front of its jaws");
                    helper.assertTrue(shark.isLunging(), "the bite starts with a lunge");
                    helper.assertTrue(sharkPrey.getHealth() >= sharkPreyHealth, "the jaws close only after the wind-up");
                    helper.assertFalse(shark.bite(), "no second bite in the middle of a lunge");
                })
                .thenWaitUntil(() -> helper.assertTrue(sharkPrey.getHealth() < sharkPreyHealth || sharkPrey.isDeadOrDying(), "the bite should hurt"))
                .thenWaitUntil(() -> helper.assertFalse(GreatWhiteSharkEntity.find(player).isLunging(), "the lunge ends"))
                .thenExecute(() -> {
                    GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
                    AquamanServer.usePower(player, emblem, AquaPower.SHARK);
                    helper.assertTrue(shark.isRemoved(), "using the power again sends the shark away");
                    sharkPrey.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    /** The eggs hatch the three creatures of Atlantis; anyone can ride them and breathes while riding. */
    public static void atlanteanMountsHatchAndRide(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        var eggs = List.of(com.danrod505.greenlantern.registry.ModItems.MANTA_RAY_EGG.get(),
                com.danrod505.greenlantern.registry.ModItems.GIANT_SEAHORSE_EGG.get(),
                com.danrod505.greenlantern.registry.ModItems.ATLANTEAN_DOLPHIN_EGG.get());
        int[][] spots = {{7, 3}, {3, 11}, {11, 11}};
        for (int i = 0; i < eggs.size(); i++) {
            BlockPos floor = helper.absolutePos(new BlockPos(spots[i][0], 0, spots[i][1]));
            ItemStack egg = new ItemStack(eggs.get(i), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false);
            var result = eggs.get(i).useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertTrue(result.consumesAction(), "the egg should hatch: " + eggs.get(i));
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "hatching uses one egg");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var mounts = around(player, com.danrod505.greenlantern.entity.AtlanteanMountEntity.class, 12);
        helper.assertTrue(mounts.size() == 3, "three creatures should hatch, got " + mounts.size());
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.MantaRayEntity), "a manta ray");
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.GiantSeahorseEntity), "a seahorse");
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.AtlanteanDolphinEntity), "a dolphin");
        for (var mount : mounts) {
            helper.assertTrue(mount.isPersistenceRequired(), "hatched creatures don't despawn");
            helper.assertTrue(mount.variant() >= 0 && mount.variant() < mount.variants(), "valid look");
        }
        var seahorse = mounts.stream().filter(m -> m instanceof com.danrod505.greenlantern.entity.GiantSeahorseEntity).findFirst().orElseThrow();
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    seahorse.interact(player, InteractionHand.MAIN_HAND);
                    helper.assertTrue(player.getVehicle() == seahorse, "anyone can ride a creature of Atlantis");
                    helper.assertTrue(seahorse.getControllingPassenger() == player, "the rider steers it");
                    player.setAirSupply(10);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(player.getAirSupply() > 200, "the rider breathes through the creature, air=" + player.getAirSupply());
                    player.stopRiding();
                    helper.assertTrue(player.getVehicle() == null, "climbed off");
                    helper.assertTrue(seahorse.isAlive(), "the seahorse is fine");
                    for (var mount : mounts) mount.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanKrakenCall(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.KRAKEN);
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    use(player);
                    KrakenEntity kraken = KrakenEntity.find(player);
                    helper.assertTrue(kraken != null, "the Kraken should rise");
                    helper.assertTrue(player.getVehicle() == kraken, "Aquaman should ride the Kraken");
                    helper.assertTrue(kraken.isSwimmingMode(), "called in deep water, the Kraken swims");
                    helper.assertTrue(kraken.getHealth() == KrakenEntity.maxHealth(), "the Kraken starts with full life");
                    int stored = AquamanHero.SEA_FORCE.get(AquamanHelper.findEmblem(player)).stored();
                    helper.assertTrue(stored <= 1000 - AquaPower.KRAKEN.cost(), "the Kraken should cost Power of the Seas, left " + stored);
                })
                .thenExecuteFor(5, player::doTick)
                .thenExecute(() -> {
                    KrakenEntity kraken = KrakenEntity.find(player);
                    helper.assertTrue(kraken != null && player.getVehicle() == kraken, "still riding the Kraken");
                    AquamanServer.usePower(player, AquamanHelper.findEmblem(player), AquaPower.KRAKEN);
                    helper.assertTrue(kraken.isRemoved(), "using the power again sends the Kraken back to the deep");
                    helper.assertTrue(player.getVehicle() == null, "Aquaman is off its head");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanKrakenFightsAndFalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        KrakenEntity kraken = KrakenEntity.create(level, player, false);
        level.addFreshEntity(kraken);
        player.startRiding(kraken);
        // The tentacle comes down about nine blocks in front of the Kraken (it faces south, +Z).
        Zombie slamTarget = dummy(helper, 7.5, 1, 12.0);
        float slamHealth = slamTarget.getHealth();
        Zombie[] jetTarget = new Zombie[1];
        float[] before = new float[2];
        helper.startSequence()
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getVehicle() == kraken, "Aquaman should ride the Kraken");
                    helper.assertTrue(player.getY() - kraken.getY() > 8.0, "Aquaman sits on top of its head, got " + (player.getY() - kraken.getY()));
                    helper.assertFalse(kraken.isSwimmingMode(), "on dry land the Kraken walks");
                    helper.assertTrue(kraken.tentacleSlam(), "the tentacle slam should start");
                    helper.assertFalse(kraken.tentacleSlam(), "the tentacle needs a moment before the next slam");
                })
                .thenExecuteFor(KrakenEntity.SLAM_HIT + 2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(slamTarget.isDeadOrDying() || slamTarget.getHealth() < slamHealth, "the slam should crush the zombie");
                    jetTarget[0] = dummy(helper, 3.5, 1, 12.5);
                    before[0] = jetTarget[0].getHealth();
                    lookAt(player, jetTarget[0]);
                    kraken.setJetFiring(true);
                })
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(kraken.isJetting(), "the water jet should be firing");
                    helper.assertTrue(jetTarget[0].isDeadOrDying() || jetTarget[0].getHealth() < before[0], "the water jet should batter the zombie");
                    helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() < 1000, "the jet drains Power of the Seas");
                    kraken.setJetFiring(false);
                })
                // Past the newcomer's spawn protection, so hits reach the rider.
                .thenExecuteFor(50, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(kraken.isJetting(), "the jet stops when released");
                    Zombie attacker = dummy(helper, 9.5, 1, 4.5);
                    float riderHealth = player.getHealth();
                    before[1] = kraken.getHealth();
                    player.hurtServer(level, level.damageSources().mobAttack(attacker), 6.0F);
                    helper.assertTrue(player.getHealth() == riderHealth, "the Kraken should take the blow aimed at its rider");
                    helper.assertTrue(kraken.getHealth() < before[1], "the Kraken loses life instead, " + kraken.getHealth() + " / " + before[1]);
                    attacker.discard();
                })
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    kraken.hurtServer(level, level.damageSources().generic(), KrakenEntity.maxHealth() * 2);
                    helper.assertTrue(kraken.isDying(), "out of life, the Kraken dies");
                    helper.assertTrue(player.getVehicle() == null, "its rider falls off");
                    helper.assertTrue(KrakenEntity.recoverySeconds(player) > 0, "a fallen Kraken needs time to recover");
                })
                .thenExecuteFor(KrakenEntity.DEATH_TICKS + 5, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(kraken.isRemoved(), "the dead Kraken is gone after its death throes");
                    AquaPower.POWERS.select(emblem, AquaPower.KRAKEN);
                    helper.assertFalse(AquamanServer.usePower(player, emblem, AquaPower.KRAKEN), "it cannot be called while recovering");
                    KrakenEntity.clearRecovery(player);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanSeaCall(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.SEA_CALL);
        helper.spawn(EntityType.COD, new Vec3(5.5, 3, 4.5));
        helper.spawn(EntityType.SQUID, new Vec3(9.5, 3, 4.5));
        Zombie zombie = dummy(helper, 7.5, 2, 9.5);
        float health = zombie.getHealth();
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    use(player);
                    helper.assertTrue(SeaCall.isActive(player), "the call should be active");
                    int allies = SeaCall.allies(player);
                    helper.assertTrue(allies >= GLConfig.SEA_CALL_HELPERS.get(), "dolphins should make up the numbers, got " + allies);
                })
                .thenExecuteFor(120, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the sea creatures should attack the zombie");
                    AquamanServer.usePower(player, emblem, AquaPower.SEA_CALL);
                    helper.assertFalse(SeaCall.isActive(player), "using it again releases the creatures");
                    helper.assertTrue(around(player, net.minecraft.world.entity.animal.dolphin.Dolphin.class, 20).isEmpty(),
                            "the dolphins from the deep go back");
                    remove(player);
                })
                .thenSucceed();
    }

    // ---- Atlantis ---------------------------------------------------------------------------------

    public static void respiratorBreathesWithAnySuit(GameTestHelper helper) {
        flood(helper, 5);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        giveRing(player, 3000);
        LanternHero.INSTANCE.summonSuit(player); // the Lantern mask holds the helmet slot: the respirator works anyway
        player.getInventory().setItem(20, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
        player.setAirSupply(0);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER), "the player's head should be underwater");
                    helper.assertTrue(player.getAirSupply() == player.getMaxAirSupply(), "the respirator breathes for the player, air " + player.getAirSupply());
                    helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LANTERN_MASK.get()), "the mask stays on");
                    ItemStack respirator = player.getInventory().getItem(20);
                    int air = com.danrod505.greenlantern.aquaman.Respirator.air(respirator);
                    helper.assertTrue(air < com.danrod505.greenlantern.aquaman.Respirator.capacity(), "the respirator should spend air, has " + air);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void respiratorRefillsOutOfWater(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack respirator = new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get());
        com.danrod505.greenlantern.aquaman.Respirator.setAir(respirator, 0);
        player.getInventory().setItem(9, respirator);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int air = com.danrod505.greenlantern.aquaman.Respirator.air(player.getInventory().getItem(9));
                    helper.assertTrue(air > 0, "the respirator should refill out of the water, has " + air);
                    remove(player);
                })
                .thenSucceed();
    }

    /** Raises a whole Atlantis far from the tests, checks its landmarks and makes the round trip. */
    public static void atlantisBuildAndTravel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        com.danrod505.greenlantern.atlantis.Atlantis.Site site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(6000, 6000, -30, 20);
        long start = System.nanoTime();
        com.danrod505.greenlantern.atlantis.AtlantisBuilder.buildAllAt(overworld, site);
        GreenLantern.LOGGER.info("GAMETEST atlantis built in {} ms", (System.nanoTime() - start) / 1_000_000);
        BlockPos c = site.center();
        helper.assertTrue(overworld.getBlockState(c.above(24)).is(Blocks.BEACON), "the palace beacon");
        helper.assertTrue(overworld.getBlockState(c.above(23)).is(Blocks.GOLD_BLOCK), "the beacon stands on gold");
        BlockPos pavilion = c.offset(0, 1, com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_Z);
        helper.assertTrue(overworld.getBlockState(pavilion).isAir(), "the pavilion is full of air");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 0, com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_Z - com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_RADIUS)).is(Blocks.WAXED_OXIDIZED_COPPER_DOOR), "the pavilion door");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 1, 15)).is(Blocks.WATER), "the plaza is under water");
        helper.assertTrue(overworld.getBlockState(c.offset(30, 3, 40)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                || !overworld.getBlockState(c.offset(30, 3, 40)).isAir(), "no air pockets in the city");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 20, 70)).is(Blocks.WATER), "the slopes are flooded");
        // The pavilion's chest holds a respirator for visitors.
        boolean found = false;
        for (int dx = -6; dx <= 6 && !found; dx++) {
            for (int dz = -6; dz <= 6 && !found; dz++) {
                if (overworld.getBlockEntity(pavilion.offset(dx, -1, dz)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        if (chest.getItem(i).is(ModItems.ATLANTEAN_RESPIRATOR.get())) found = true;
                    }
                }
            }
        }
        helper.assertTrue(found, "a respirator waits in the pavilion");

        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, site, true);
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        Vec3 home = player.position();
        try {
            helper.assertTrue(com.danrod505.greenlantern.atlantis.AtlantisTravel.sendToAtlantis(player), "the trip to Atlantis");
            helper.assertTrue(player.level() == overworld && player.position().distanceTo(site.arrival()) < 1.0,
                    "arrives in the pavilion, at " + player.position());
            helper.assertTrue(com.danrod505.greenlantern.atlantis.AtlantisTravel.leadsHome(overworld, player.position()), "in Atlantis, portals lead home");
            com.danrod505.greenlantern.atlantis.AtlantisTravel.returnHome(player);
            helper.assertTrue(player.position().distanceTo(home) < 1.0, "back to the exact spot, at " + player.position());
        } finally {
            com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
            remove(player);
        }
        helper.succeed();
    }

    /** Without an Atlantis in the world, the power says so and costs nothing; the gate opens nothing. */
    public static void atlantisPortalPower(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        boolean used = AquamanServer.usePower(player, emblem, AquaPower.ATLANTIS_PORTAL);
        helper.assertFalse(used, "no Atlantis in this world: no portal");
        helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() == 1000, "a failed portal costs nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ATLANTIS_GATE.get()));
        use(player);
        helper.assertTrue(around(player, com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, 8.0).isEmpty(), "the gate opens nothing either");

        // With an Atlantis (pretend), the power opens the whirlpool and pays for it.
        var site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(9000, 9000, -30, 20);
        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, site, true);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, emblem);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(emblem));
            helper.assertTrue(AquamanServer.usePower(player, emblem, AquaPower.ATLANTIS_PORTAL), "the power opens a portal");
            var portals = around(player, com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, 8.0);
            helper.assertTrue(portals.size() == 1, "one whirlpool, found " + portals.size());
            helper.assertFalse(portals.getFirst().leadsHome(), "far from Atlantis, the portal leads there");
            helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() == 1000 - AquaPower.ATLANTIS_PORTAL.cost(), "the portal costs Power of the Seas");
            portals.forEach(p -> p.discard());
        } finally {
            com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
            remove(player);
        }
        helper.succeed();
    }

    // ---- Batman ---------------------------------------------------------------------------------------

    private static ItemStack giveBelt(ServerPlayer player, int charge) {
        ItemStack belt = new ItemStack(ModItems.UTILITY_BELT.get());
        BatmanHero.BAT_CHARGE.set(belt, charge);
        player.setItemInHand(InteractionHand.MAIN_HAND, belt);
        return player.getMainHandItem();
    }

    public static void batmanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // Aquaman first, then Batman replaces him (one hero at a time).
        player.getInventory().add(new ItemStack(ModItems.AQUAMAN_EMBLEM.get()));
        AquamanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(AquamanHelper.isSuited(player), "aquaman suit should be on");
        giveBelt(player, 0);
        use(player); // not suited as Batman: right click summons the suit
        helper.assertTrue(BatmanHelper.isSuited(player), "batman suit should be summoned");
        helper.assertFalse(AquamanHelper.isSuited(player), "the aquaman suit should be gone");
        helper.assertTrue(BatmanHelper.hasCowl(player), "the cowl replaces the helmet");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BATMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BATMAN_BOOTS.get()), "boots should be worn");
        BatmanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(BatmanHelper.isSuited(player), "batman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        remove(player);
        helper.succeed();
    }

    public static void batmanBeltRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack belt = giveBelt(player, 0);
        BatmanHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = BatmanHero.BAT_CHARGE.get(belt).stored();
                    helper.assertTrue(stored >= 10, "the belt should recharge on its own, got " + stored);
                    helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "the cowl gives night vision");
                    BatPower.POWERS.select(belt, BatPower.BAT_SWARM);
                    BatmanHero.BAT_CHARGE.set(belt, 0);
                    helper.assertFalse(BatmanServer.usePower(player, belt, BatPower.BAT_SWARM), "no charge, no gadget");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void batmanBatarangReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BATARANG);
        Zombie zombie = dummy(helper, 7.5, 1, 7.5);
        float health = zombie.getHealth();
        player.setXRot(8.0F);
        use(player);
        helper.assertTrue(BatarangEntity.findAll(player).size() == 1, "a batarang should be in flight");
        helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.BATARANG.cost(), "the batarang should cost charge");
        helper.startSequence()
                // Waits for the batarang's own hit (it slows the target): the zombie may also burn in the sun.
                .thenWaitUntil(() -> helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) || zombie.isDeadOrDying(),
                        "the batarang should hit and slow the zombie"))
                .thenExecute(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the batarang should hurt the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(BatarangEntity.findAll(player).isEmpty(), "the batarang should come back"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void batmanGrappleHooksAndReleases(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        for (int x = 5; x <= 10; x++) {
            for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 11), Blocks.STONE);
        }
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.GRAPPLE);
        use(player);
        helper.assertTrue(GrappleHookEntity.find(player) != null, "the hook should be fired");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    GrappleHookEntity hook = GrappleHookEntity.find(player);
                    helper.assertTrue(hook != null && hook.isAttached(), "the hook should bite into the wall");
                })
                .thenExecute(() -> {
                    GrappleHookEntity hook = GrappleHookEntity.find(player);
                    BatmanServer.usePower(player, belt, BatPower.GRAPPLE);
                    helper.assertTrue(hook.isRemoved(), "using the gadget again lets go of the cable");
                    helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.GRAPPLE.cost(), "letting go is free");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void batmanBatSwarm(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BAT_SWARM);
        Zombie zombie = dummy(helper, 7.5, 1, 11.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(BatDefenderEntity.findAll(player).size() == GLConfig.BAT_SWARM_COUNT.get(), "the whole swarm should come");
        helper.assertTrue(BatmanServer.isSwarmActive(player), "the swarm is active");
        helper.startSequence()
                // A bat's bite both hurts and blinds (a zombie in the sun can lose health on its own, so wait for the blindness).
                .thenWaitUntil(() -> helper.assertTrue(zombie.isDeadOrDying() || zombie.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                        "the bats should attack and blind the zombie"))
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the bats' attack should hurt");
                    BatmanServer.usePower(player, belt, BatPower.BAT_SWARM);
                    helper.assertFalse(BatmanServer.isSwarmActive(player), "using the gadget again scatters the bats");
                })
                .thenWaitUntil(() -> helper.assertTrue(around(player, BatDefenderEntity.class, 64).isEmpty(), "the scattered bats fly away"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void batmanBatmobile(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BATMOBILE);
        Zombie zombie = dummy(helper, 7.5, 1, 12.5);
        float health = zombie.getHealth();
        use(player);
        BatmobileEntity car = BatmobileEntity.find(player);
        helper.assertTrue(car != null, "the Batmobile should arrive");
        helper.assertTrue(player.getVehicle() == car, "Batman should be in the Batmobile");
        helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.BATMOBILE.cost(), "the Batmobile should cost charge");
        helper.startSequence()
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> helper.assertTrue(car.fireMissiles(), "the missiles should fire"))
                .thenWaitUntil(() -> helper.assertTrue(!around(player, BatmobileMissileEntity.class, 32).isEmpty()
                        || zombie.getHealth() < health || zombie.isDeadOrDying(), "missiles should launch"))
                .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the missiles should hit the zombie"))
                .thenExecute(() -> {
                    helper.assertTrue(player.isAlive() && player.getVehicle() == car, "the driver is safe from his own missiles");
                    BatmanServer.usePower(player, BatmanHelper.findBelt(player), BatPower.BATMOBILE);
                    helper.assertTrue(car.isRemoved(), "using the gadget again sends the car away");
                    helper.assertFalse(player.isPassenger(), "Batman gets out");
                    remove(player);
                })
                .thenSucceed();
    }

    // ---- Superman ------------------------------------------------------------------------------------

    private static ItemStack giveCrystal(ServerPlayer player, int energy) {
        ItemStack crystal = new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get());
        SupermanHero.SOLAR_ENERGY.set(crystal, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, crystal);
        return player.getMainHandItem();
    }

    private static net.minecraft.world.entity.animal.pig.Pig dummyPig(GameTestHelper helper, double x, double y, double z) {
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(EntityType.PIG, new Vec3(x, y, z));
        pig.setNoAi(true);
        pig.setPersistenceRequired();
        return pig;
    }

    public static void supermanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // Batman first, then Superman replaces him (one hero at a time).
        player.getInventory().add(new ItemStack(ModItems.UTILITY_BELT.get()));
        BatmanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(BatmanHelper.isSuited(player), "batman suit should be on");
        giveCrystal(player, 1000);
        use(player); // not suited as Superman: right click summons the suit
        helper.assertTrue(SupermanHelper.isSuited(player), "superman suit should be summoned");
        helper.assertFalse(BatmanHelper.isSuited(player), "the batman suit should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SUPERMAN_HAIR.get()), "the hair replaces the helmet");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SUPERMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SUPERMAN_BOOTS.get()), "boots should be worn");
        helper.assertTrue(player.getAbilities().mayfly, "Superman can fly");
        SupermanHero.INSTANCE.updateSuitModifiers(player, true);
        helper.assertTrue(player.getMaxHealth() > 20.0F, "the suit should give extra health");
        SupermanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(SupermanHelper.isSuited(player), "superman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        helper.assertFalse(player.getAbilities().mayfly, "survival player should not fly without the suit");
        remove(player);
        helper.succeed();
    }

    public static void supermanSolarRecharge(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 0);
        SupermanHero.INSTANCE.summonSuit(player);
        // The cells only charge in direct sunlight: make sure it is daytime.
        if (SupermanServer.sunlight(player) <= 0.0F) helper.getLevel().setDayTime(6000);
        helper.assertTrue(SupermanServer.sunlight(player) > 0.0F, "the arena should be under the open sky in daylight");
        helper.assertFalse(SupermanServer.usePower(player, crystal, SuperPower.SUPER_PUNCH), "no energy, no power");
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = SupermanHero.SOLAR_ENERGY.get(crystal).stored();
                    helper.assertTrue(stored >= 10, "the sun should recharge the crystal, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanHeatVision(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.HEAT_VISION);
        net.minecraft.world.entity.animal.pig.Pig pig = dummyPig(helper, 7.5, 1, 7.5);
        float health = pig.getHealth();
        player.setXRot(8.0F);
        use(player);
        helper.assertTrue(SupermanServer.isUsingHeatVision(player), "the beams should be on");
        helper.startSequence()
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the beams should burn the pig");
                    helper.assertTrue(SupermanHero.SOLAR_ENERGY.get(crystal).stored() < 3000, "heat vision should cost solar energy");
                    SupermanServer.usePower(player, crystal, SuperPower.HEAT_VISION);
                    helper.assertFalse(SupermanServer.isUsingHeatVision(player), "using it again stops the beams");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanSuperPunchArea(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 4.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.SUPER_PUNCH);
        // One pig right in front and two more off to the sides: the blast hits them all.
        net.minecraft.world.entity.animal.pig.Pig front = dummyPig(helper, 7.5, 1, 6.5);
        net.minecraft.world.entity.animal.pig.Pig left = dummyPig(helper, 10.5, 1, 7.5);
        net.minecraft.world.entity.animal.pig.Pig right = dummyPig(helper, 4.5, 1, 7.5);
        float health = front.getHealth();
        use(player);
        helper.assertTrue(SupermanHero.SOLAR_ENERGY.get(crystal).stored() == 3000 - SuperPower.SUPER_PUNCH.cost(), "the punch should cost solar energy");
        for (net.minecraft.world.entity.animal.pig.Pig pig : List.of(front, left, right)) {
            helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the punch should hit every pig around");
        }
        remove(player);
        helper.succeed();
    }

    public static void supermanSuperBreath(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.SUPER_BREATH);
        net.minecraft.world.entity.animal.pig.Pig pig = dummyPig(helper, 7.5, 1, 6.5);
        float health = pig.getHealth();
        use(player);
        helper.assertTrue(SupermanServer.isBreathing(player), "the breath should blow");
        helper.startSequence()
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the breath should hurt the pig");
                    helper.assertTrue(pig.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the breath should slow the pig");
                    SupermanServer.usePower(player, crystal, SuperPower.SUPER_BREATH);
                    helper.assertFalse(SupermanServer.isBreathing(player), "using it again stops the breath");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanXrayToggle(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.XRAY_VISION);
        use(player);
        helper.assertTrue(SupermanServer.isXray(player), "X-ray vision should be on");
        helper.startSequence()
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    SupermanServer.usePower(player, crystal, SuperPower.XRAY_VISION);
                    helper.assertFalse(SupermanServer.isXray(player), "using it again turns X-ray vision off");
                    SupermanHero.INSTANCE.dismissSuit(player, false);
                    helper.assertFalse(SupermanServer.isXray(player), "taking the suit off ends every power");
                    remove(player);
                })
                .thenSucceed();
    }

    // ---- Wonder Woman --------------------------------------------------------------------------------

    private static ItemStack giveTiara(ServerPlayer player, int power) {
        ItemStack tiara = new ItemStack(ModItems.AMAZON_TIARA.get());
        WonderWomanHero.DIVINE_POWER.set(tiara, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, tiara);
        return player.getMainHandItem();
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

    // ---- the Trench -------------------------------------------------------------------------------

    /** A nest is dug out: the flooded pit, the brood mound crowned by its catalyst, the chambers, the biome. */
    public static void trenchNestBuild(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        Trench.Nest nest = new Trench.Nest(7, 7000, 7000, -36, -10, 20);
        long start = System.nanoTime();
        TrenchBuilder.buildNestAt(overworld, nest);
        GreenLantern.LOGGER.info("GAMETEST trench nest built in {} ms", (System.nanoTime() - start) / 1_000_000);
        boolean catalyst = false;
        for (int y = nest.floor(); y <= nest.floor() + 8; y++) {
            if (overworld.getBlockState(new BlockPos(nest.x(), y, nest.z())).is(Blocks.SCULK_CATALYST)) catalyst = true;
        }
        helper.assertTrue(catalyst, "the brood mound is crowned by a sculk catalyst");
        int water = 0;
        int total = 0;
        for (int y = nest.floor() + 8; y <= nest.rim(); y++) {
            total++;
            if (overworld.getFluidState(new BlockPos(nest.x() + 13, y, nest.z() + 2)).is(net.minecraft.tags.FluidTags.WATER)) water++;
        }
        helper.assertTrue(water * 10 >= total * 7, "the pit is open and flooded (" + water + "/" + total + ")");
        for (Vec3 spot : TrenchNest.cocoonSpots(nest)) {
            BlockPos pos = BlockPos.containing(spot.x, spot.y + 1, spot.z);
            helper.assertTrue(overworld.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER), "the chamber is hollow and flooded at " + pos);
        }
        helper.assertTrue(overworld.getBiome(nest.center().above(4)).is(Trench.BIOME), "the nest lies in the Trench's own sea");
        helper.assertTrue(overworld.getBiome(new BlockPos(nest.x() + Trench.TERRITORY - 6, nest.rim(), nest.z())).is(Trench.BIOME),
                "the territory is the Trench's own sea too");
        helper.succeed();
    }

    /** A creature of the Trench hunts a player who swims close, and bites. */
    public static void trenchCreatureHunts(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        helper.startSequence()
                .thenExecuteFor(65, player::doTick) // wait out the spawn invulnerability
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    TrenchCreatureEntity creature = TrenchCreatureEntity.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(7.5, 2, 11.5)), false, -1, 0);
                    helper.assertTrue(creature != null, "the creature should spawn");
                })
                .thenWaitUntil(() -> {
                    player.doTick();
                    helper.assertTrue(player.getHealth() < player.getMaxHealth(), "the creature should bite the swimmer");
                })
                .thenExecute(() -> {
                    TrenchCreatureEntity creature = around(player, TrenchCreatureEntity.class, 16).getFirst();
                    helper.assertTrue(creature.getTarget() == player, "it hunts the player");
                    creature.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    private static Villager trenchVillager;

    /** A creature seizes a villager in the water to carry it away. */
    public static void trenchCreatureGrabsVillager(GameTestHelper helper) {
        flood(helper, 6);
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(7.5, 2, 3.5));
        villager.setNoAi(true);
        trenchVillager = villager;
        TrenchCreatureEntity creature = TrenchCreatureEntity.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(7.5, 2, 11.5)), false, -1, 0);
        helper.assertTrue(creature != null, "the creature should spawn");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(trenchVillager.getVehicle() instanceof TrenchCreatureEntity, "the creature should seize the villager"))
                .thenExecute(() -> {
                    helper.assertTrue(trenchVillager.hasEffect(MobEffects.WATER_BREATHING), "the captive breathes through the grip");
                    helper.assertTrue(trenchVillager.getHealth() == trenchVillager.getMaxHealth(), "captives are taken alive");
                    TrenchCreatureEntity taker = (TrenchCreatureEntity) trenchVillager.getVehicle();
                    trenchVillager.stopRiding();
                    taker.discard();
                    trenchVillager.discard();
                })
                .thenSucceed();
    }

    /** Breaking a cocoon frees the villager inside, who swims off breathing water and leaves emeralds. */
    public static void trenchCocoonRescue(GameTestHelper helper) {
        flood(helper, 6);
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(7.5, 2, 9.5));
        villager.setNoAi(true);
        TrenchCocoonEntity cocoon = TrenchCocoonEntity.encase(level, helper.absoluteVec(new Vec3(7.5, 2, 9.5)), villager, -1);
        helper.assertTrue(cocoon != null && villager.getVehicle() == cocoon, "the villager is shut in the cocoon");
        ServerPlayer player = player(helper, 7.5, 2, 7.0, 0, 0);
        int rescued = Trench.rescued(level.getServer());
        helper.startSequence()
                .thenExecuteFor(65, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(cocoon.hurtServer(level, level.damageSources().drown(), 50.0F), "the sea doesn't break a cocoon");
                    helper.assertTrue(cocoon.isAlive(), "the cocoon holds");
                    cocoon.hurtServer(level, level.damageSources().playerAttack(player), 6.0F);
                    helper.assertTrue(cocoon.isAlive() && villager.isPassenger(), "it takes more than one hit");
                })
                .thenIdle(12)
                .thenExecute(() -> cocoon.hurtServer(level, level.damageSources().playerAttack(player), 20.0F))
                .thenWaitUntil(() -> helper.assertTrue(cocoon.isRemoved(), "the cocoon bursts"))
                .thenExecute(() -> {
                    helper.assertFalse(villager.isPassenger(), "the villager is free");
                    helper.assertTrue(villager.isAlive(), "and alive");
                    helper.assertTrue(villager.hasEffect(MobEffects.WATER_BREATHING), "it can breathe on its way up");
                    helper.assertTrue(Trench.rescued(level.getServer()) == rescued + 1, "the rescue is counted");
                    helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(villager.blockPosition()).inflate(4),
                            e -> e.getItem().is(Items.EMERALD)).isEmpty(), "emeralds in thanks");
                    villager.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    /** A war party leaves a nest for the walls of Atlantis, under a boss bar, and goes for the Atlanteans. */
    public static void trenchRaid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        com.danrod505.greenlantern.atlantis.Atlantis.Site site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(-8000, 8000, -30, 20);
        Trench.Nest nest = new Trench.Nest(0, -8000 + Trench.DISTANCE, 8000, -40, -12, 20);
        TrenchLife.stopRaidForTesting();
        List<TrenchCreatureEntity> raiders = TrenchLife.startRaid(level, site, List.of(nest));
        try {
            helper.assertTrue(raiders.size() >= 5, "a whole war party comes, got " + raiders.size());
            helper.assertTrue(TrenchLife.raidActive(), "the raid is on");
            helper.assertTrue(raiders.stream().allMatch(TrenchCreatureEntity::isRaider), "every one of them is a raider");
            helper.assertTrue(raiders.stream().anyMatch(TrenchCreatureEntity::isBrute), "a brute leads them");
            TrenchCreatureEntity first = raiders.getFirst();
            double dx = first.getX() - (site.x() + 0.5);
            double dz = first.getZ() - (site.z() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            helper.assertTrue(d > com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 4 && d < com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 16,
                    "they strike at the city wall, " + d + " blocks from the center");
            helper.assertTrue(first.getX() > site.x(), "from the side of their nest");
            helper.assertTrue(TrenchLife.startRaid(level, site, List.of(nest)).isEmpty(), "one raid at a time");
        } finally {
            TrenchLife.stopRaidForTesting();
            for (TrenchCreatureEntity raider : raiders) raider.discard();
        }
        helper.succeed();
    }
}
