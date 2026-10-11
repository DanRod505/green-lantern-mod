package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Helpers shared by the game tests of every hero: a real survival server player in the arena, the
 * hero items (charged), dummies and entity lookups.
 */
public final class GameTestKit {
    private GameTestKit() {}

    @SuppressWarnings("removal")
    public static ServerPlayer player(GameTestHelper helper, double x, double y, double z, float yaw, float pitch) {
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

    public static ItemStack giveRing(ServerPlayer player, int energy) {
        ItemStack ring = new ItemStack(ModItems.POWER_RING.get());
        RingEnergy.set(ring, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);
        return player.getMainHandItem();
    }

    public static void select(ServerPlayer player, Construct construct) {
        ConstructRegistry.select(player.getMainHandItem(), construct);
    }

    public static void use(ServerPlayer player) {
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    public static void remove(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }

    public static Zombie dummy(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(x, y, z));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        return zombie;
    }

    public static <T extends net.minecraft.world.entity.Entity> List<T> around(ServerPlayer player, Class<T> type, double radius) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(radius));
    }

    /** Turns the player to look at the middle of the given entity. */
    public static void lookAt(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI);
        float pitch = (float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }

    public static ItemStack giveFlashRing(ServerPlayer player, int speedForce) {
        ItemStack ring = new ItemStack(ModItems.FLASH_RING.get());
        FlashHero.SPEED_FORCE.set(ring, speedForce);
        player.setItemInHand(InteractionHand.MAIN_HAND, ring);
        return player.getMainHandItem();
    }

    public static ItemStack giveEmblem(ServerPlayer player, int seaForce) {
        ItemStack emblem = new ItemStack(ModItems.AQUAMAN_EMBLEM.get());
        AquamanHero.SEA_FORCE.set(emblem, seaForce);
        player.setItemInHand(InteractionHand.MAIN_HAND, emblem);
        return player.getMainHandItem();
    }

    /** Fills the inside of the arena with water up to (relative) height {@code top}. */
    public static void flood(GameTestHelper helper, int top) {
        for (int x = 1; x <= 13; x++) {
            for (int y = 1; y <= top; y++) {
                for (int z = 1; z <= 13; z++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }
    }

    public static ItemStack giveBelt(ServerPlayer player, int charge) {
        ItemStack belt = new ItemStack(ModItems.UTILITY_BELT.get());
        BatmanHero.BAT_CHARGE.set(belt, charge);
        player.setItemInHand(InteractionHand.MAIN_HAND, belt);
        return player.getMainHandItem();
    }

    public static ItemStack giveCrystal(ServerPlayer player, int energy) {
        ItemStack crystal = new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get());
        SupermanHero.SOLAR_ENERGY.set(crystal, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, crystal);
        return player.getMainHandItem();
    }

    public static net.minecraft.world.entity.animal.pig.Pig dummyPig(GameTestHelper helper, double x, double y, double z) {
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(EntityType.PIG, new Vec3(x, y, z));
        pig.setNoAi(true);
        pig.setPersistenceRequired();
        return pig;
    }

    public static ItemStack giveTiara(ServerPlayer player, int power) {
        ItemStack tiara = new ItemStack(ModItems.AMAZON_TIARA.get());
        WonderWomanHero.DIVINE_POWER.set(tiara, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, tiara);
        return player.getMainHandItem();
    }
}
