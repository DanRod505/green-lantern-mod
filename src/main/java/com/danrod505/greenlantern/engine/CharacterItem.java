package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.item.HeroItem;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The item of a character of the engine: it calls the suit and holds the class energy, the
 * cooldowns and the skin. Right click with the suit on uses the chosen ability (a tap).
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
}
