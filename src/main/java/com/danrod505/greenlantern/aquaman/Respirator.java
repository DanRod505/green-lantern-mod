package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.item.AtlanteanRespiratorItem;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Atlantean Respirator lets anyone breathe underwater. The heroes' suits can't be taken off
 * (their masks hold the helmet slot), so the respirator is not worn as armor: like a totem, it
 * works from anywhere in the inventory and clips onto whatever mask or helmet you have on.
 * <p>
 * It holds a few minutes of air (shown as a bar on the item), used only while your head is
 * underwater, and fills up again out of the water. Aquaman doesn't need it, and doesn't spend it.
 */
public final class Respirator {
    private Respirator() {}

    public static int capacity() {
        return GLConfig.RESPIRATOR_SECONDS.get() * 20;
    }

    public static boolean isRespirator(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AtlanteanRespiratorItem;
    }

    /** Air left, in ticks (a new respirator is full). */
    public static int air(ItemStack respirator) {
        Integer air = respirator.get(ModDataComponents.RESPIRATOR_AIR.get());
        return air == null ? capacity() : Mth.clamp(air, 0, capacity());
    }

    public static void setAir(ItemStack respirator, int air) {
        respirator.set(ModDataComponents.RESPIRATOR_AIR.get(), Mth.clamp(air, 0, capacity()));
    }

    public static float fraction(ItemStack respirator) {
        return air(respirator) / (float) capacity();
    }

    /** The respirator with the most air left anywhere in the inventory, or {@link ItemStack#EMPTY}. */
    public static ItemStack find(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack best = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isRespirator(stack) && (best.isEmpty() || air(stack) > air(best))) best = stack;
        }
        return best;
    }

    /** Whether the player carries a respirator with air in it. */
    public static boolean canBreatheUnderwater(Player player) {
        ItemStack respirator = find(player);
        return !respirator.isEmpty() && air(respirator) > 0;
    }

    /** Server tick of every player. */
    public static void tick(ServerPlayer player) {
        if (player.isSpectator()) return;
        ItemStack respirator = find(player);
        if (respirator.isEmpty()) return;
        boolean underwater = player.isEyeInFluid(FluidTags.WATER);
        if (!underwater) {
            if (player.tickCount % 20 == 0) recharge(player);
            return;
        }
        if (AquamanHelper.isSuited(player) || player.getAbilities().invulnerable) return;
        int air = air(respirator);
        if (air <= 0) return; // empty: the player holds their breath as usual
        player.setAirSupply(player.getMaxAirSupply());
        if (player.tickCount % 20 != 0) return;
        int left = air - 20;
        setAir(respirator, left);
        // A few bubbles from the mouthpiece with every breath.
        Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.35)).add(0, -0.15, 0);
        player.level().sendParticles(ParticleTypes.BUBBLE, mouth.x, mouth.y, mouth.z, 4, 0.05, 0.05, 0.05, 0.03);
        int warn = 30 * 20;
        if (air > warn && left <= warn) {
            player.displayClientMessage(Component.translatable("message.greenlantern.respirator_low").withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.6F, 1.6F);
        } else if (left <= 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.respirator_empty").withStyle(ChatFormatting.RED), true);
        }
    }

    /** Out of the water every respirator fills up again. */
    private static void recharge(ServerPlayer player) {
        int perSecond = Math.max(1, capacity() / GLConfig.RESPIRATOR_RECHARGE_SECONDS.get());
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isRespirator(stack) && air(stack) < capacity()) setAir(stack, air(stack) + perSecond);
        }
    }
}
