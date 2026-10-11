package com.danrod505.greenlantern.shazam;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.item.HeroItem;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Wizard's Bolt: like every hero item, right click calls the suit (here with the lightning of
 * "Shazam!") and then uses the selected power; sneaking with the suit on, the lightning comes again
 * and he turns back into Billy.
 */
public class WizardBoltItem extends HeroItem {
    public WizardBoltItem(Properties properties, Supplier<HeroDefinition> hero, float lowHue, float fullHue, ChatFormatting energyColor) {
        super(properties, hero, lowHue, fullHue, energyColor);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && ShazamHero.INSTANCE.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) ShazamHero.INSTANCE.dismissSuit(serverPlayer, true);
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
}
