package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightFlags;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.block.PowerBatteryBlockEntity;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.DrillConstructEntity;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.entity.SawConstructEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModBlocks;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.ring.Uniform;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
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
        TESTS.register("sonic_boom_requires_speed", () -> ModGameTests::sonicBoomRequiresSpeed);
        TESTS.register("hero_landing_shockwave", () -> ModGameTests::heroLandingShockwave);
        TESTS.register("flight_cost_scales_with_speed", () -> ModGameTests::flightCostScalesWithSpeed);
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

        Uniform.dismiss(player, false);
        helper.assertFalse(RingHelper.isSuited(player), "uniform should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertFalse(player.getAbilities().mayfly, "survival player should not fly without the uniform");
        remove(player);
        helper.succeed();
    }

    public static void flightDrainsEnergy(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 3, 7.5, 0, 0);
        ItemStack ring = giveRing(player, 1000);
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
        select(player, ConstructRegistry.PORTAL);
        use(player);
        List<com.danrod505.greenlantern.entity.OaPortalEntity> portals = around(player, com.danrod505.greenlantern.entity.OaPortalEntity.class, 6.0);
        helper.assertTrue(portals.size() == 1, "the construct should open one portal, found " + portals.size());
        helper.assertTrue(RingEnergy.get(player.getMainHandItem()).stored() <= 3000 - ConstructRegistry.PORTAL.activationCost(), "the portal should cost energy");
        var portal = portals.getFirst();
        helper.assertTrue(portal.distanceTo(player) > 2.0, "the portal should open in front of the player");
        ServerLevel home = player.level();
        Vec3 start = portal.position();
        helper.startSequence()
                .thenIdle(com.danrod505.greenlantern.entity.OaPortalEntity.OPEN_TICKS + 2)
                .thenExecute(() -> player.teleportTo(portal.getX(), portal.getY(), portal.getZ()))
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(com.danrod505.greenlantern.oa.Oa.is(player.level()), "walking through the portal should take the player to Oa, level=" + player.level().dimension());
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(player);
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
        Uniform.summon(slow);
        Uniform.summon(fast);
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
}
