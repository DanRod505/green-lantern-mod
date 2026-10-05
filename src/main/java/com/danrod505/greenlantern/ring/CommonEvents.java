package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanServer;
import com.danrod505.greenlantern.aquaman.AquamanSuit;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Game (Forge bus) event handlers shared by both logical sides. */
public final class CommonEvents {
    private static final String GUIDE_GIVEN_TAG = "greenlantern.guide_given";

    private CommonEvents() {}

    public static void register() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(CommonEvents::onPlayerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> com.danrod505.greenlantern.atlantis.AtlantisBuilder.tick(event.server()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> com.danrod505.greenlantern.trench.TrenchBuilder.tick(event.server()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(event -> com.danrod505.greenlantern.trench.TrenchBuilder.onServerStopped(event.getServer()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(event -> com.danrod505.greenlantern.atlantis.AtlantisBuilder.onServerStopped(event.getServer()));
        LivingAttackEvent.BUS.addListener(CommonEvents::onLivingAttack);
        LivingHurtEvent.BUS.addListener(CommonEvents::onLivingHurt);
        LivingFallEvent.BUS.addListener(CommonEvents::onLivingFall);
        LivingDeathEvent.BUS.addListener(CommonEvents::onLivingDeath);
        EntityJoinLevelEvent.BUS.addListener(CommonEvents::onEntityJoin);
        net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent.BUS.addListener(CommonEvents::onLivingTick);
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) giveGuideOnFirstJoin(player);
        });
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                com.danrod505.greenlantern.flight.ServerFlightTracker.remove(player);
                com.danrod505.greenlantern.flash.SpeedsterServer.endPhase(player, false);
                com.danrod505.greenlantern.flash.SpeedsterServer.remove(player);
                if (AquamanHelper.isSuited(player)) AquamanSuit.dismiss(player, false);
                AquamanServer.remove(player);
                if (com.danrod505.greenlantern.batman.BatmanHelper.isSuited(player)) com.danrod505.greenlantern.batman.BatmanSuit.dismiss(player, false);
                com.danrod505.greenlantern.batman.BatmanServer.remove(player);
                if (com.danrod505.greenlantern.superman.SupermanHelper.isSuited(player)) com.danrod505.greenlantern.superman.SupermanSuit.dismiss(player, false);
                com.danrod505.greenlantern.superman.SupermanServer.remove(player);
            }
        });
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        if (RingHelper.isSuited(player)) {
            ItemStack ring = RingHelper.findRing(player);
            if (ring.isEmpty()) {
                Uniform.dismiss(player, true);
                return;
            }
            RingEnergy energy = RingEnergy.get(ring);
            if (energy.stored() <= 0 && !player.isCreative()) {
                player.displayClientMessage(Component.translatable("message.greenlantern.power_out").withStyle(ChatFormatting.RED), true);
                Uniform.dismiss(player, true);
                return;
            }
            if (energy.fraction() <= 0.1F && player.tickCount % 100 == 0) {
                player.displayClientMessage(Component.translatable("message.greenlantern.low_energy").withStyle(ChatFormatting.GOLD), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            }
        }
        if (com.danrod505.greenlantern.flash.FlashHelper.isSuited(player)
                && com.danrod505.greenlantern.flash.FlashHelper.findRing(player).isEmpty()) {
            // The suit lives in the ring: no ring, no suit.
            com.danrod505.greenlantern.flash.FlashSuit.dismiss(player, true);
        }
        if (AquamanHelper.isSuited(player) && AquamanHelper.findEmblem(player).isEmpty()) {
            // The suit answers the emblem: no emblem, no suit.
            AquamanSuit.dismiss(player, true);
        }
        if (com.danrod505.greenlantern.batman.BatmanHelper.isSuited(player)
                && com.danrod505.greenlantern.batman.BatmanHelper.findBelt(player).isEmpty()) {
            // The batsuit folds back into the belt: no belt, no suit.
            com.danrod505.greenlantern.batman.BatmanSuit.dismiss(player, true);
        }
        if (com.danrod505.greenlantern.superman.SupermanHelper.isSuited(player)
                && com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(player).isEmpty()) {
            // The suit belongs to the crystal: no crystal, no suit.
            com.danrod505.greenlantern.superman.SupermanSuit.dismiss(player, true);
        }
        if (player.tickCount % 10 == 0) {
            Uniform.removeStrayPieces(player);
        }
        com.danrod505.greenlantern.flash.SpeedsterServer.tick(player);
        AquamanServer.tick(player);
        com.danrod505.greenlantern.batman.BatmanServer.tick(player);
        com.danrod505.greenlantern.superman.SupermanServer.tick(player);
        com.danrod505.greenlantern.aquaman.Respirator.tick(player);
        if (player.tickCount % 5 == 0) {
            chargeFromCentralBattery(player);
        }
        FlightHandler.tick(player);
    }

    /** On Oa, the Central Power Battery tops up every ring close to it. */
    private static void chargeFromCentralBattery(ServerPlayer player) {
        if (!GLConfig.OA_BATTERY_RECHARGES.get() || !com.danrod505.greenlantern.oa.Oa.is(player.level())
                || !com.danrod505.greenlantern.oa.Oa.nearBattery(player.position())) return;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty() || RingEnergy.get(ring).isFull()) return;
        RingEnergy.add(ring, GLConfig.CHARGE_PER_TICK.get() * 5);
        // A thread of light from the battery to the ring bearer.
        var level = player.level();
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(0.5, com.danrod505.greenlantern.oa.Oa.GROUND_Y + 13, 0.5);
        net.minecraft.world.phys.Vec3 to = player.position().add(0, 1.0, 0);
        for (int i = 1; i <= 6; i++) {
            net.minecraft.world.phys.Vec3 p = from.lerp(to, (i + level.getRandom().nextDouble()) / 7.0);
            level.sendParticles(com.danrod505.greenlantern.registry.ModParticles.GLOW.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (player.tickCount % 60 == 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.oa_battery_charging").withStyle(ChatFormatting.GREEN), true);
        }
        if (RingEnergy.get(ring).isFull()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHARGE_COMPLETE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    /** Phasing players ignore block collisions (both logical sides). Never cancels the tick. */
    private static boolean onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            com.danrod505.greenlantern.flash.PhaseState.apply(player);
        }
        return false;
    }

    /** The mecha shields its pilot from every hit (paid for with ring energy). Returns true to cancel the attack. */
    private static boolean onLivingAttack(LivingAttackEvent event) {
        // Attacks pass right through a phasing speedster.
        if (event.getEntity() instanceof ServerPlayer phasing && com.danrod505.greenlantern.flash.SpeedsterServer.isPhasing(phasing)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return true;
        }
        // The Kraken takes the blows aimed at the Aquaman on its head (monsters, arrows, explosions).
        if (event.getEntity() instanceof ServerPlayer rider && rider.getVehicle() instanceof com.danrod505.greenlantern.entity.KrakenEntity kraken
                && !kraken.isDying() && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (event.getSource().getEntity() != null || event.getSource().getDirectEntity() != null
                        || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))) {
            kraken.shieldRider(rider, event.getSource(), event.getAmount());
            return true;
        }
        // The Batmobile's own missiles never hurt its driver.
        if (event.getEntity() instanceof ServerPlayer driver && driver.getVehicle() instanceof com.danrod505.greenlantern.entity.BatmobileEntity
                && event.getSource().getDirectEntity() instanceof com.danrod505.greenlantern.entity.BatmobileMissileEntity missile
                && missile.getOwner() == driver) {
            return true;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.getVehicle() instanceof MechaEntity mecha)) return false;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        mecha.absorbHit(player, event.getSource(), event.getAmount());
        return true;
    }

    private static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getVehicle() instanceof com.danrod505.greenlantern.entity.BatmobileEntity
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The Batmobile's armor takes most of the blow.
            event.setAmount(event.getAmount() * (1.0F - GLConfig.BATMOBILE_DAMAGE_REDUCTION.get().floatValue()));
        }
        if (com.danrod505.greenlantern.superman.SupermanHelper.isPowered(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The Man of Steel: most blows barely hurt him while the sun charges his cells.
            event.setAmount(event.getAmount() * (1.0F - GLConfig.SUPERMAN_DAMAGE_REDUCTION.get().floatValue()));
        }
        BubbleConstructEntity bubble = BubbleConstructEntity.find(player);
        if (bubble != null && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float reduction = GLConfig.BUBBLE_DAMAGE_REDUCTION.get().floatValue();
            event.setAmount(event.getAmount() * (1.0F - reduction));
            var where = event.getSource().getSourcePosition();
            bubble.onAbsorbHit(where != null ? where : player.position().add(0, 1, 0));
        }
    }

    private static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && (RingHelper.isSuited(player) || com.danrod505.greenlantern.flash.FlashHelper.isSuited(player)
                        || com.danrod505.greenlantern.superman.SupermanHelper.isSuited(player)
                        || (AquamanHelper.isSuited(player) && event.getDistance() < 24.0F))) {
            // The ring cushions every landing; a speedster lands running; an Atlantean, built for the
            // crushing deep, shrugs off any ordinary fall.
            event.setDamageMultiplier(0.0F);
        } else if (event.getEntity() instanceof ServerPlayer batman && com.danrod505.greenlantern.batman.BatmanHelper.isSuited(batman)) {
            // The cape breaks a glide's landing; otherwise years of training soften the fall.
            event.setDamageMultiplier(com.danrod505.greenlantern.batman.BatmanServer.safeLanding(batman) ? 0.0F : 0.5F);
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RingHelper.isSuited(player)) {
            // Give back the stashed armor before the inventory is dropped.
            Uniform.dismiss(player, false);
        }
        if (event.getEntity() instanceof ServerPlayer player && com.danrod505.greenlantern.flash.FlashHelper.isSuited(player)) {
            com.danrod505.greenlantern.flash.FlashSuit.dismiss(player, false);
        }
        if (event.getEntity() instanceof ServerPlayer player && AquamanHelper.isSuited(player)) {
            AquamanSuit.dismiss(player, false);
        }
        if (event.getEntity() instanceof ServerPlayer player && com.danrod505.greenlantern.batman.BatmanHelper.isSuited(player)) {
            com.danrod505.greenlantern.batman.BatmanSuit.dismiss(player, false);
        }
        if (event.getEntity() instanceof ServerPlayer player && com.danrod505.greenlantern.superman.SupermanHelper.isSuited(player)) {
            com.danrod505.greenlantern.superman.SupermanSuit.dismiss(player, false);
        }
    }

    /** New players get the Corps Manual once (remembered in their persistent data, which survives death). */
    private static void giveGuideOnFirstJoin(ServerPlayer player) {
        if (!GLConfig.GIVE_GUIDE_ON_FIRST_JOIN.get()) return;
        CompoundTag persisted = player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr(GUIDE_GIVEN_TAG, false)) return;
        persisted.putBoolean(GUIDE_GIVEN_TAG, true);
        player.getPersistentData().put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
        ItemStack guide = new ItemStack(ModItems.GUIDE_BOOK.get());
        if (!player.getInventory().add(guide)) player.drop(guide, false);
        player.displayClientMessage(Component.translatable("message.greenlantern.guide_given").withStyle(ChatFormatting.GREEN), false);
    }

    private static boolean onEntityJoin(EntityJoinLevelEvent event) {
        // Uniform pieces and Aquaman's trident never exist as dropped items.
        return event.getEntity() instanceof ItemEntity item
                && (item.getItem().getItem() instanceof SuitArmorItem || AquamanHelper.isTrident(item.getItem()));
    }
}
