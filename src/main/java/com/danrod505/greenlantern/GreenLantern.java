package com.danrod505.greenlantern;

import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.registry.ModBlockEntities;
import com.danrod505.greenlantern.registry.ModBlocks;
import com.danrod505.greenlantern.registry.ModCreativeTabs;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.CommonEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Entry point of the Green Lantern Corps mod.
 * <p>
 * Every feature lives in its own package (registry, ring, construct, entity, client...) so new
 * constructs, items or mechanics can be added later without touching unrelated code.
 */
@Mod(GreenLantern.MODID)
public final class GreenLantern {
    public static final String MODID = "greenlantern";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GreenLantern(FMLJavaModLoadingContext context) {
        var modBus = context.getModBusGroup();

        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        context.registerConfig(ModConfig.Type.COMMON, GLConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, GLClientConfig.SPEC);

        ModNetwork.register();
        ConstructRegistry.bootstrap();
        CommonEvents.register();

        net.minecraftforge.event.entity.EntityAttributeCreationEvent.getBus(modBus).addListener(event -> {
            event.put(ModEntities.OA_GUARDIAN.get(), com.danrod505.greenlantern.entity.OaGuardianEntity.createAttributes().build());
            event.put(ModEntities.LANTERN_CORPSMAN.get(), com.danrod505.greenlantern.entity.LanternCorpsmanEntity.createAttributes().build());
        });

        registerGameTests(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.danrod505.greenlantern.client.ClientSetup.init(modBus);
        }
    }

    /** Automated tests (src/gametest) are only present in development builds made with -Pgametests. */
    private static void registerGameTests(BusGroup modBus) {
        try {
            Class.forName("com.danrod505.greenlantern.gametest.ModGameTests").getMethod("register", BusGroup.class).invoke(null, modBus);
            LOGGER.info("Registered Green Lantern game tests");
        } catch (ClassNotFoundException ignored) {
            // Regular build: no tests.
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not register game tests", e);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
