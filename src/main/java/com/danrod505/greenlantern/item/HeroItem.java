package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The item that calls a hero's suit and holds its energy, for heroes made with the hero kit
 * ({@code tools/new_hero.py}): right click while not suited puts the suit on, right click while
 * suited uses the selected power (also on the power key). The slot bar shows the energy, going from
 * {@code lowHue} to {@code fullHue} (0 to 1) as it fills, and red when almost empty.
 */
public class HeroItem extends Item {
    private final Supplier<HeroDefinition> hero;
    private final float lowHue;
    private final float fullHue;
    private final ChatFormatting energyColor;

    public HeroItem(Properties properties, Supplier<HeroDefinition> hero, float lowHue, float fullHue, ChatFormatting energyColor) {
        super(properties);
        this.hero = hero;
        this.lowHue = lowHue;
        this.fullHue = fullHue;
        this.energyColor = energyColor;
    }

    private HeroPowers<?> powers() {
        return hero.get().powers();
    }

    /** Returns the given item full of energy (used for the creative tab). */
    public ItemStack charged(ItemStack stack) {
        powers().energy().set(stack, powers().energy().capacity());
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!hero.get().isSuited(player)) {
                hero.get().summonSuit(serverPlayer);
            } else {
                powers().use(serverPlayer, stack, -1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * powers().energy().get(stack).fraction());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = powers().energy().get(stack).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(Mth.lerp(f, lowHue, fullHue), 0.8F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        HeroPowers<?> powers = powers();
        RingEnergy energy = powers.energy().get(stack);
        tooltip.accept(Component.translatable(powers.wheel().energyKey(), energy.stored(), energy.capacity())
                .withStyle(energy.fraction() < 0.2F ? ChatFormatting.RED : energyColor));
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", powers.power(powers.selectedIndex(stack)).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.greenlantern." + hero.get().id() + "_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
