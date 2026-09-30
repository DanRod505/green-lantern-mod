package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Game (Forge bus) event handlers shared by both logical sides. */
public final class CommonEvents {
    private CommonEvents() {}

    public static void register() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(CommonEvents::onPlayerTick);
        LivingHurtEvent.BUS.addListener(CommonEvents::onLivingHurt);
        LivingFallEvent.BUS.addListener(CommonEvents::onLivingFall);
        LivingDeathEvent.BUS.addListener(CommonEvents::onLivingDeath);
        EntityJoinLevelEvent.BUS.addListener(CommonEvents::onEntityJoin);
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
        if (player.tickCount % 10 == 0) {
            Uniform.removeStrayPieces(player);
        }
        FlightHandler.tick(player);
    }

    private static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BubbleConstructEntity bubble = BubbleConstructEntity.find(player);
        if (bubble != null && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float reduction = GLConfig.BUBBLE_DAMAGE_REDUCTION.get().floatValue();
            event.setAmount(event.getAmount() * (1.0F - reduction));
            var where = event.getSource().getSourcePosition();
            bubble.onAbsorbHit(where != null ? where : player.position().add(0, 1, 0));
        }
    }

    private static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && RingHelper.isSuited(player)) {
            // The ring cushions every landing.
            event.setDamageMultiplier(0.0F);
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RingHelper.isSuited(player)) {
            // Give back the stashed armor before the inventory is dropped.
            Uniform.dismiss(player, false);
        }
    }

    private static boolean onEntityJoin(EntityJoinLevelEvent event) {
        // Uniform pieces never exist as dropped items.
        return event.getEntity() instanceof ItemEntity item && item.getItem().getItem() instanceof SuitArmorItem;
    }
}
