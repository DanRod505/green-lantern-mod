package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.item.HeroItem;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The item of a character of the engine: it calls the suit and holds the class energy, the
 * cooldowns, the Supreme charge and the skin. Right click with the suit on uses the chosen ability;
 * crouching, its use in the world.
 */
public class CharacterItem extends HeroItem {
    private final Supplier<EngineHero> hero;

    public CharacterItem(Properties properties, Supplier<EngineHero> hero) {
        super(properties, () -> (HeroDefinition) hero.get(), 0.55F, 0.15F, ChatFormatting.AQUA);
        this.hero = hero;
    }

    public EngineHero hero() {
        return hero.get();
    }

    @Override
    public Component getName(ItemStack stack) {
        CharacterData data = hero.get().data();
        return Component.translatableWithFallback(getDescriptionId(), data.name());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && hero.get().isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) EngineServer.useInWorld(serverPlayer, hero.get(), player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
}
