package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
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
                for (HeroDefinition hero : HeroRegistry.all()) hero.onLogout(player);
            }
        });
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        // Each hero keeps its suit honest (no item, no suit); the Lantern may end the tick here.
        for (HeroDefinition hero : HeroRegistry.all()) {
            if (!hero.checkSuit(player)) return;
        }
        if (player.tickCount % 10 == 0) {
            Uniform.removeStrayPieces(player);
        }
        for (HeroDefinition hero : HeroRegistry.all()) hero.tick(player);
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
        // A creature bound by the Lasso of Truth can't fight.
        if (event.getSource().getEntity() != null && com.danrod505.greenlantern.entity.LassoEntity.isBound(event.getSource().getEntity())
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        // Wonder Woman's raised bracelets block blows and send projectiles back.
        if (event.getEntity() instanceof ServerPlayer amazon && com.danrod505.greenlantern.wonderwoman.WonderWomanServer.onAttacked(amazon, event.getSource())) {
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
        if (player.getVehicle() instanceof com.danrod505.greenlantern.entity.InvisibleJetEntity
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The jet's canopy takes most of the blow.
            event.setAmount(event.getAmount() * (1.0F - GLConfig.INVISIBLE_JET_DAMAGE_REDUCTION.get().floatValue()));
        }
        for (HeroDefinition hero : HeroRegistry.all()) {
            // Tough heroes (Superman, Wonder Woman) shrug off part of every blow.
            float multiplier = hero.damageTakenMultiplier(player, event.getSource());
            if (multiplier != 1.0F) event.setAmount(event.getAmount() * multiplier);
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
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        // The ring cushions every landing; a speedster lands running; an Atlantean shrugs off any
        // ordinary fall; Batman's cape and training soften it (see each hero's fallDamageMultiplier).
        HeroRegistry.suited(player).ifPresent(hero -> {
            float multiplier = hero.fallDamageMultiplier(player, event.getDistance());
            if (multiplier >= 0.0F) event.setDamageMultiplier(multiplier);
        });
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (HeroDefinition hero : HeroRegistry.all()) {
            // Give back the stashed armor before the inventory is dropped.
            if (hero.isSuited(player)) hero.dismissSuit(player, false);
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
        // Suit pieces and power items (Aquaman's trident, Wonder Woman's sword and shield) never exist as dropped items.
        if (!(event.getEntity() instanceof ItemEntity item)) return false;
        if (item.getItem().getItem() instanceof SuitArmorItem) return true;
        for (HeroDefinition hero : HeroRegistry.all()) {
            if (hero.neverDropped(item.getItem())) return true;
        }
        return false;
    }
}
