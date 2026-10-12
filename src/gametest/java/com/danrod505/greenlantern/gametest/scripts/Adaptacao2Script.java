package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityHero;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "adaptacao_lote2" (stage 3, lot 2): the 9 heroes with their new skins. For each
 * hero: the transformation of the catalog, the skin from the front and the back, then every
 * alternative skin from the front; at the end the Lantern at night, to show what glows.
 */
public final class Adaptacao2Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("adaptacao_lote2", Adaptacao2Script::build, Adaptacao2Script::log, false);

    private Adaptacao2Script() {}

    /** The 9 heroes, in the order of the registry. */
    static List<HeroDefinition> heroes() {
        return HeroRegistry.all().stream().filter(h -> h instanceof AbilityHero && !(h instanceof EngineHero)).toList();
    }

    private static void suit(HeroDefinition hero) {
        server(sp -> {
            HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false));
            sp.teleportTo(sp.getX(), sp.getY(), sp.getZ());
            sp.setDeltaMovement(Vec3.ZERO);
            sp.setYRot(0);
            sp.setYHeadRot(0);
            sp.setXRot(10);
            sp.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
            boolean on = hero.summonSuit(sp);
            GreenLantern.LOGGER.info("CLIENTSCRIPT suit {} on={}", hero.id(), on);
        });
    }

    /** Shows one skin of the hero (as the Wardrobe would, without going round the others). */
    private static void skin(AbilityHero hero, CharacterData.Skin skin) {
        server(sp -> {
            var item = hero.findItem(sp);
            var state = EngineServer.skin(hero, item).select(skin.id());
            item.set(EngineContent.SKIN.get(), state);
            sp.getItemBySlot(EquipmentSlot.CHEST).set(EngineContent.SKIN.get(), state);
            GreenLantern.LOGGER.info("CLIENTSCRIPT skin {}", skin.id());
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
                sp.setGameMode(GameType.CREATIVE);
                sp.setInvulnerable(true);
            });
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });
        int n = 0;
        for (HeroDefinition definition : heroes()) {
            AbilityHero hero = (AbilityHero) definition;
            String label = String.format("ad%02d_%s", n++, definition.id());
            step(2, () -> {
                camera(CameraType.THIRD_PERSON_FRONT);
                look(0, 10);
            });
            step(2, () -> suit(definition));
            step(10, () -> clean(label + "_a_transformacao"));
            step(40, () -> {
                server(PresetRuntime::stopLasting);
                clean(label + "_b_frente");
            });
            step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
            step(4, () -> clean(label + "_c_costas"));
            step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
            List<CharacterData.Skin> skins = hero.data().skins();
            for (int i = 1; i < skins.size(); i++) {
                CharacterData.Skin skin = skins.get(i);
                String name = String.format("%s_d%d_%s", label, i, skin.id());
                step(2, () -> skin(hero, skin));
                step(6, () -> clean(name));
            }
            step(4, () -> server(sp -> HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false))));
        }
        // What glows: the Lantern at night.
        heroes().stream().filter(h -> h.id().equals("lantern")).findFirst().ifPresent(lantern -> {
            step(2, () -> command("time set 18000"));
            step(2, () -> suit(lantern));
            step(40, () -> clean("ad99_lantern_noite"));
            step(4, () -> server(sp -> HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false))));
        });
        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
