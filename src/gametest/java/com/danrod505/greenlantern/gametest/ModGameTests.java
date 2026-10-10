package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.gametest.heroes.AquamanTests;
import com.danrod505.greenlantern.gametest.heroes.BatmanTests;
import com.danrod505.greenlantern.gametest.heroes.FlashTests;
import com.danrod505.greenlantern.gametest.heroes.LanternTests;
import com.danrod505.greenlantern.gametest.heroes.SharedTests;
import com.danrod505.greenlantern.gametest.heroes.SupermanTests;
import com.danrod505.greenlantern.gametest.heroes.TrenchTests;
import com.danrod505.greenlantern.gametest.heroes.WonderWomanTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * In-world integration tests. Run with {@code ./gradlew runGameTestServer -Pgametests}.
 * Every test uses a real (survival) server player placed in a 15x8x15 stone arena (see
 * {@link GameTestKit}). Each hero keeps its tests in its own class under {@code gametest/heroes},
 * and every test needs a {@code test_instance/<name>.json}. {@link HeroContractTests} checks the
 * basics of every hero in {@code HeroRegistry} without any code of its own.
 */
public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TESTS = DeferredRegister.create(Registries.TEST_FUNCTION, GreenLantern.MODID);

    static {
        SharedTests.register(TESTS);
        HeroContractTests.register(TESTS);
        LanternTests.register(TESTS);
        FlashTests.register(TESTS);
        AquamanTests.register(TESTS);
        BatmanTests.register(TESTS);
        SupermanTests.register(TESTS);
        WonderWomanTests.register(TESTS);
        TrenchTests.register(TESTS);
        // tools/new_hero.py adds new heroes above this line
    }

    private ModGameTests() {}

    public static void register(BusGroup modBus) {
        TESTS.register(modBus);
        if (System.getenv("GL_CLIENT_SCRIPT") != null
                && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            ClientScript.register();
        }
    }
}
