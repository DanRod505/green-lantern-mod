package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GreenLantern.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.greenlantern"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance()))
            .displayItems((params, output) -> {
                // Each hero adds its own items, in hero order.
                for (HeroDefinition hero : HeroRegistry.all()) hero.creativeTabItems(output);
            })
            .build());

    private ModCreativeTabs() {}
}
