package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.AquaTridentEntity;
import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.danrod505.greenlantern.entity.KrakenEntity;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server side of Aquaman: breathing underwater, the Power of the Seas, and the powers (the trident,
 * the great white shark, the call of the sea, the way to Atlantis and the Kraken). Fast swimming itself is simulated by the client
 * (see {@code client.aqua.SwimController}), like the Flash's running.
 */
public final class AquamanServer {
    /** Duration of the underwater vision / breathing effect, refreshed while in water. */
    private static final int CONDUIT_TICKS = 400;

    private static final class State {
        float pendingCharge;
        @Nullable Vec3 lastPos;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private AquamanServer() {}

    private static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        SeaCall.release(player, false);
        STATES.remove(player.getUUID());
    }

    // ---- every server tick -------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = AquamanHelper.isSuited(player);
        if (player.tickCount % 20 == 0) AquamanSuit.updateModifiers(player, suited);
        SeaCall.tick(player);
        if (!suited) {
            STATES.remove(player.getUUID());
            return;
        }
        ItemStack emblem = AquamanHelper.findEmblem(player);
        if (emblem.isEmpty()) return;
        State state = get(player);

        // An Atlantean breathes underwater, and sees clearly in the deep.
        player.setAirSupply(player.getMaxAirSupply());
        if (player.isEyeInFluid(FluidTags.WATER) && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, CONDUIT_TICKS, 0, true, false, true));
        }

        // The Power of the Seas flows back quickly in the water, slowly on land.
        boolean inWater = player.isInWater();
        float regen = inWater ? GLConfig.SEA_FORCE_REGEN_WATER.get() : (player.isInWaterOrRain() ? GLConfig.SEA_FORCE_REGEN_WATER.get() * 0.5F : GLConfig.SEA_FORCE_REGEN_LAND.get());
        state.pendingCharge += regen / 20.0F;
        if (state.pendingCharge >= 1.0F) {
            int whole = (int) state.pendingCharge;
            state.pendingCharge -= whole;
            AquamanHero.SEA_FORCE.add(emblem, whole);
        }

        // Swimming at Atlantean speed shouldn't starve you: give back most of the food vanilla
        // charges per block swum (0.01 exhaustion per block).
        Vec3 pos = player.position();
        if (inWater && state.lastPos != null && !player.isPassenger()) {
            double dist = Math.min(pos.distanceTo(state.lastPos), 8.0);
            if (dist > 0.3) player.getFoodData().addExhaustion((float) (-0.009 * dist));
        }
        state.lastPos = pos;
    }

    /** The suit is being taken off: the trident returns to the sea, the shark swims away, the call ends. */
    public static void onSuitRemoved(ServerPlayer player) {
        removeTrident(player);
        GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
        if (shark != null) shark.swimAway();
        KrakenEntity kraken = KrakenEntity.find(player);
        if (kraken != null) kraken.retreat();
        SeaCall.release(player, false);
        MobEffectInstance conduit = player.getEffect(MobEffects.CONDUIT_POWER);
        if (conduit != null && conduit.getDuration() <= CONDUIT_TICKS && conduit.isAmbient()) {
            player.removeEffect(MobEffects.CONDUIT_POWER);
        }
    }

    // ---- powers --------------------------------------------------------------------------------------

    /** Uses a power (power key or right click with the emblem). Returns whether something happened. */
    public static boolean usePower(ServerPlayer player, ItemStack emblem, AquaPower power) {
        if (!AquamanHelper.isSuited(player) || player.isSpectator()) return false;
        // Powers already in use are toggled without cost.
        switch (power) {
            case TRIDENT -> {
                if (handleExistingTrident(player)) return true;
            }
            case SHARK -> {
                GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
                if (shark != null) {
                    if (player.getVehicle() == shark) {
                        shark.swimAway();
                    } else if (!shark.isVehicle() && player.distanceToSqr(shark) < 48 * 48) {
                        // Call it back and climb on.
                        shark.teleportTo(player.getX(), player.getY(), player.getZ());
                        player.startRiding(shark);
                    } else {
                        shark.swimAway();
                    }
                    return true;
                }
            }
            case SEA_CALL -> {
                if (SeaCall.isActive(player)) {
                    SeaCall.release(player, true);
                    return true;
                }
            }
            case ATLANTIS_PORTAL -> {}
            case KRAKEN -> {
                KrakenEntity kraken = KrakenEntity.find(player);
                if (kraken != null && !kraken.isDying()) {
                    if (player.getVehicle() == kraken) {
                        kraken.retreat();
                    } else if (!kraken.isVehicle() && player.distanceToSqr(kraken) < 64 * 64) {
                        // Climb back onto its head, wherever it is.
                        player.startRiding(kraken);
                    } else {
                        kraken.retreat();
                    }
                    return true;
                }
                if (kraken != null) return false;
            }
        }
        if (player.getCooldowns().isOnCooldown(emblem)) return false;
        if (!player.isCreative() && !AquamanHero.SEA_FORCE.has(emblem, power.cost())) {
            notifyNoSeaForce(player);
            return false;
        }
        boolean used = switch (power) {
            case TRIDENT -> summonTrident(player);
            case SHARK -> summonShark(player);
            case SEA_CALL -> callTheSea(player);
            case ATLANTIS_PORTAL -> openAtlantisPortal(player);
            case KRAKEN -> summonKraken(player);
        };
        if (used) {
            if (!player.isCreative()) AquamanHero.SEA_FORCE.tryConsume(emblem, power.cost());
            player.getCooldowns().addCooldown(emblem, power == AquaPower.ATLANTIS_PORTAL ? 60 : 20);
        }
        return used;
    }

    public static void notifyNoSeaForce(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_sea_force").withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    // ---- the trident -------------------------------------------------------------------------------

    /**
     * Using the trident power while it already exists: a thrown trident flies back, one in the
     * backpack jumps into your hand, and the one in your hand returns to the sea.
     */
    private static boolean handleExistingTrident(ServerPlayer player) {
        AquaTridentEntity thrown = AquaTridentEntity.find(player);
        if (thrown != null) {
            thrown.recall();
            return true;
        }
        int slot = AquamanHelper.tridentSlot(player);
        if (slot < 0) return false;
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (slot == selected) {
            removeTrident(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRIDENT_RETURN.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
            tridentSplash(player.level(), player);
        } else {
            ItemStack trident = inventory.getItem(slot);
            ItemStack held = inventory.getItem(selected);
            inventory.setItem(selected, trident);
            inventory.setItem(slot, held);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRIDENT_SUMMON.get(), SoundSource.PLAYERS, 0.6F, 1.3F);
        }
        return true;
    }

    /** The trident of Atlantis forms in a swirl of water in the main hand. */
    public static boolean summonTrident(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        ItemStack held = inventory.getItem(selected);
        if (!held.isEmpty()) {
            int free = inventory.getFreeSlot();
            if (free < 0) {
                player.displayClientMessage(Component.translatable("message.greenlantern.trident_no_room").withStyle(ChatFormatting.AQUA), true);
                return false;
            }
            inventory.setItem(free, held);
        }
        inventory.setItem(selected, new ItemStack(ModItems.AQUAMAN_TRIDENT.get()));
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRIDENT_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        tridentSplash(level, player);
        return true;
    }

    /** Takes the trident away, wherever it is (hand, inventory, in flight). */
    public static void removeTrident(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (AquamanHelper.isTrident(inventory.getItem(i))) inventory.setItem(i, ItemStack.EMPTY);
        }
        if (AquamanHelper.isTrident(player.containerMenu.getCarried())) player.containerMenu.setCarried(ItemStack.EMPTY);
        AquaTridentEntity thrown = AquaTridentEntity.find(player);
        if (thrown != null) thrown.discard();
    }

    private static void tridentSplash(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(look.scale(0.5)).add(new Vec3(-look.z, 0, look.x).normalize().scale(-0.4)).add(0, -0.4, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Mth.TWO_PI / 12;
            double y = i / 24.0 * 1.6 - 0.8;
            level.sendParticles(ParticleTypes.SPLASH, hand.x + Math.cos(a) * 0.35, hand.y + y, hand.z + Math.sin(a) * 0.35, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.DOLPHIN, hand.x, hand.y, hand.z, 12, 0.2, 0.5, 0.2, 0.02);
        level.sendParticles(ParticleTypes.BUBBLE_POP, hand.x, hand.y, hand.z, 10, 0.2, 0.5, 0.2, 0.02);
    }

    // ---- the shark ------------------------------------------------------------------------------------

    private static boolean summonShark(ServerPlayer player) {
        if (!player.isInWater()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.shark_needs_water").withStyle(ChatFormatting.AQUA), true);
            return false;
        }
        ServerLevel level = player.level();
        GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.create(level, player);
        level.addFreshEntity(shark);
        player.startRiding(shark);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHARK_SUMMON.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, player.getX(), player.getY(), player.getZ(), 60, 1.2, 0.6, 1.2, 0.2);
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.2, 0.6, 1.2, 0.3);
        return true;
    }

    // ---- the Kraken ------------------------------------------------------------------------------------

    /** The Kraken rises from the deep (or out of the ground) under Aquaman, who ends up on its head. */
    private static boolean summonKraken(ServerPlayer player) {
        int recovering = KrakenEntity.recoverySeconds(player);
        if (recovering > 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.kraken_recovering", recovering).withStyle(ChatFormatting.DARK_AQUA), true);
            return false;
        }
        ServerLevel level = player.level();
        boolean swimming = player.isInWater() && deepWater(level, player);
        if (!KrakenEntity.fits(level, player, swimming)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.kraken_no_room").withStyle(ChatFormatting.DARK_AQUA), true);
            return false;
        }
        if (player.isPassenger()) player.stopRiding();
        KrakenEntity kraken = KrakenEntity.create(level, player, swimming);
        level.addFreshEntity(kraken);
        player.startRiding(kraken);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KRAKEN_SUMMON.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        level.sendParticles(swimming ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.SPLASH, player.getX(), player.getY() + 0.5, player.getZ(),
                120, 3.0, 0.8, 3.0, 0.3);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 2.0, player.getZ(), 40, 2.5, 1.5, 2.5, 0.05);
        player.displayClientMessage(Component.translatable("message.greenlantern.kraken_summoned").withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    /** Enough water around the player for the Kraken to swim in (at least about six blocks). */
    private static boolean deepWater(ServerLevel level, ServerPlayer player) {
        net.minecraft.core.BlockPos feet = player.blockPosition();
        int depth = 0;
        for (int dy = -6; dy <= 6; dy++) {
            if (level.getFluidState(feet.above(dy)).is(FluidTags.WATER)) depth++;
        }
        return depth >= 6;
    }

    // ---- the call of the sea ----------------------------------------------------------------------------

    private static boolean callTheSea(ServerPlayer player) {
        if (!SeaCall.start(player)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.sea_call_nobody").withStyle(ChatFormatting.AQUA), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.greenlantern.sea_call", SeaCall.allies(player)).withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    // ---- the way to Atlantis ----------------------------------------------------------------------------

    /** The sea opens before Aquaman: a whirlpool to Atlantis, or back home from the city. */
    private static boolean openAtlantisPortal(ServerPlayer player) {
        if (!com.danrod505.greenlantern.atlantis.AtlantisTravel.available(player)) return false;
        com.danrod505.greenlantern.entity.AtlantisPortalEntity portal = com.danrod505.greenlantern.entity.AtlantisPortalEntity.open(player.level(), player);
        player.displayClientMessage(Component.translatable(portal.leadsHome()
                ? "message.greenlantern.atlantis_portal_home" : "message.greenlantern.atlantis_portal").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    // ---- power wheel / key packets -----------------------------------------------------------------------

}
