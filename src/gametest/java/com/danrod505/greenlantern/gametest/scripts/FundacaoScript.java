package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "fundacao" (version 2.0.0, the character engine): the 9 heroes suited up, to
 * compare with the prints of 1.18.0, then the test characters of the engine on their skeletons
 * (normal, brute, winged), the new HUD with the Supreme bar, the wheel, the K key changing skin,
 * the Heroes' Wardrobe, an ability whose preset comes in a later stage, and the first-person arm.
 */
public final class FundacaoScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("fundacao", FundacaoScript::build, FundacaoScript::log, false);

    private static final List<String> HEROES = List.of("lantern", "flash", "aquaman", "batman", "superman", "wonder_woman", "cyborg",
            "supergirl", "shazam");
    private static final List<String> TEST_CHARACTERS = List.of("prototipo", "prototipo_colosso", "prototipo_alado");

    private static BlockPos base = BlockPos.ZERO;

    private FundacaoScript() {}

    private static HeroDefinition hero(String id) {
        return HeroRegistry.byId(id).orElseThrow();
    }

    /** Takes off any suit and puts on this hero's, with its charged item in hand. */
    private static void suitUp(String id) {
        server(sp -> {
            HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false));
            HeroDefinition hero = hero(id);
            sp.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
            sp.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            sp.setDeltaMovement(Vec3.ZERO);
            boolean on = hero.summonSuit(sp);
            GreenLantern.LOGGER.info("CLIENTSCRIPT suit {} on={}", id, on);
        });
    }

    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            command("kill @e[type=!minecraft:player]");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                ServerLevel level = sp.level();
                base = sp.blockPosition();
                // The Heroes' Wardrobe, 3 blocks north, facing the player.
                level.setBlock(base.offset(0, 0, -3), EngineContent.WARDROBE.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 3);
                level.setBlock(base.offset(1, 0, -3), Blocks.AIR.defaultBlockState(), 3);
            });
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });

        // The 9 heroes, as in 1.18.0.
        int n = 0;
        for (String id : HEROES) {
            String name = String.format("fd%02d_%s", n++, id);
            step(30, () -> suitUp(id));
            step(30, () -> {
                camera(CameraType.THIRD_PERSON_FRONT);
                look(0, 10);
            });
            step(4, () -> clean(name));
        }

        // The test characters of the engine: front and back.
        for (String id : TEST_CHARACTERS) {
            String name = String.format("fd%02d_%s", n++, id);
            step(30, () -> suitUp(id));
            step(30, () -> {
                camera(CameraType.THIRD_PERSON_FRONT);
                look(0, 10);
            });
            step(4, () -> clean(name + "_front"));
            step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
            step(4, () -> clean(name + "_back"));
        }
        // The winged one in the air: the wings open.
        step(2, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
            server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() + 3, base.getZ() + 0.5));
            look(30, 20);
        });
        step(20, () -> clean("fd15_alado_wings_open"));
        step(2, () -> {
            mc().player.getAbilities().flying = false;
            mc().player.onUpdateAbilities();
        });

        // HUD, wheel, skin, wardrobe and an ability with the Protótipo.
        step(30, () -> suitUp("prototipo"));
        step(10, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 10);
        });
        step(4, () -> shot("fd16_hud"));
        step(10, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> shot("fd17_wheel"));
        step(4, () -> key(KeyBindings.CONSTRUCT_WHEEL, false));
        step(10, () -> {
            if (mc().screen != null) mc().setScreen(null);
            // Creative unlocks every skin: K goes to the night suit.
            server(sp -> sp.setGameMode(GameType.CREATIVE));
        });
        step(10, () -> KeyMapping.click(KeyBindings.SKIN.getKey()));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(4, () -> shot("fd18_skin_k"));
        // The wardrobe stands behind the player: the front camera sees both.
        step(4, () -> look(0, 5));
        step(6, () -> server(sp -> {
            // Using the wardrobe with the item: back to the first skin.
            EngineHero hero = (EngineHero) hero("prototipo");
            ItemStack item = hero.findItem(sp);
            EngineServer.cycleSkin(sp, hero, item);
        }));
        step(20, () -> shot("fd19_wardrobe"));
        step(4, () -> {
            server(sp -> sp.setGameMode(GameType.SURVIVAL));
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(10, () -> KeyMapping.click(KeyBindings.HERO_POWER.getKey()));
        step(8, () -> shot("fd20_ability_later_stage"));
        step(30, () -> camera(CameraType.FIRST_PERSON));
        step(6, () -> shot("fd21_first_person_arm"));
        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
