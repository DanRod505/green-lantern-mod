package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the shared code needs about a hero's power wheel: the powers, the energy that pays
 * for them, how the selection is announced (message colour, click pitch rising with each slot),
 * how a power is used and how the wheel looks.
 *
 * @param pitchBase pitch of the selection click for the first power
 * @param pitchStep pitch added for each next power
 */
public record HeroPowers<P extends Enum<P> & HeroPower>(PowerSet<P> set, HeroEnergy energy, ChatFormatting selectColor,
                                                       float pitchBase, float pitchStep, PowerAction<P> action, WheelStyle wheel) {
    /** Uses a power with the hero's item; returns whether it went off. */
    @FunctionalInterface
    public interface PowerAction<P> {
        boolean use(ServerPlayer player, ItemStack item, P power);
    }

    /** Picks power {@code value} or, if {@code relative}, steps that many slots (-1 or 1) from the selected one. */
    public void select(ServerPlayer player, ItemStack item, int value, boolean relative) {
        P power;
        if (relative) {
            power = set.cycle(item, Mth.clamp(value, -1, 1));
        } else {
            power = set.byIndex(value);
            set.select(item, power);
        }
        player.displayClientMessage(Component.translatable("message.greenlantern.power_selected", power.displayName().copy().withStyle(selectColor)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POWER_SELECT.get(), SoundSource.PLAYERS,
                0.7F, pitchBase + pitchStep * power.ordinal());
    }

    /** Uses power {@code index}, or the selected one when {@code index} is negative. */
    public boolean use(ServerPlayer player, ItemStack item, int index) {
        return action.use(player, item, index < 0 ? set.selected(item) : set.byIndex(index));
    }

    public HeroPower power(int index) {
        return set.byIndex(index);
    }

    public int selectedIndex(ItemStack item) {
        return set.selected(item).ordinal();
    }
}
