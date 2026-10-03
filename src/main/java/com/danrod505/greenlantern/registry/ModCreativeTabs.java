package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
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
                output.accept(PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance()));
                output.accept(ModItems.POWER_RING.get());
                output.accept(ModItems.POWER_BATTERY.get());
                output.accept(ModItems.GUIDE_BOOK.get());
                output.accept(com.danrod505.greenlantern.item.FlashRingItem.charged(ModItems.FLASH_RING.get().getDefaultInstance()));
                output.accept(ModItems.FLASH_RING.get());
            })
            .build());

    private ModCreativeTabs() {}
}
