package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.PowerWheelScreen;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.preset.PresetFx;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.engine.preset.Transformations;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.gametest.heroes.PresetTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets_lote5" (stage 2, batch 5): the 15 transformations on the test character
 * Protótipo, the beam forms, a translucent construct, the element families, the trail and aura of
 * the Protótipo Alado and the wheel with the icons drawn from the presets.
 */
public final class PresetsLot5Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets_lote5", PresetsLot5Script::build, PresetsLot5Script::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;

    private PresetsLot5Script() {}

    private static String label(String name) {
        return String.format("pv%02d_%s", ++number, name);
    }

    private static EngineHero hero(String id) {
        return (EngineHero) HeroRegistry.byId(id).orElseThrow();
    }

    private static void suit(String id) {
        server(sp -> {
            HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false));
            HeroDefinition hero = HeroRegistry.byId(id).orElseThrow();
            sp.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
            boolean on = hero.summonSuit(sp);
            GreenLantern.LOGGER.info("CLIENTSCRIPT suit {} on={}", id, on);
        });
    }

    /** Puts the player back at the base, looking south, the camera behind (or in front). */
    private static void reset(CameraType camera, float pitch) {
        step(4, () -> {
            command("kill @e[type=!minecraft:player]");
            server(sp -> {
                PresetRuntime.stopLasting(sp);
                sp.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                sp.setYRot(0);
                sp.setYHeadRot(0);
                sp.setXRot(0);
                sp.setDeltaMovement(Vec3.ZERO);
                sp.hurtMarked = true;
            });
            camera(camera);
            look(0, pitch);
        });
    }

    private static void husk(double x, double z) {
        server(sp -> {
            ServerLevel level = sp.level();
            Mob mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
            if (mob == null) return;
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
            mob.snapTo(base.getX() + 0.5 + x, base.getY(), base.getZ() + 0.5 + z, 180.0F, 0.0F);
            level.addFreshEntity(mob);
        });
    }

    /** Uses a fight ability turned 25 degrees to the side, so the camera behind sees the beam. */
    private static void fight(EngineAbility ability) {
        server(sp -> {
            sp.setYRot(-25);
            sp.setYHeadRot(-25);
            sp.setXRot(0);
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), ability);
            GreenLantern.LOGGER.info("CLIENTSCRIPT fight {} went={}", ability.data().preset(), went);
        });
    }

    /** One transformation: two shots while it plays. */
    private static void transformation(String id) {
        String label = label("transformacao_" + id);
        boolean front = List.of("sol", "armadura", "arsenal", "anel").contains(id);
        reset(front ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK, 15);
        step(20, () -> server(sp -> {
            boolean played = Transformations.play(sp, hero("prototipo"), id);
            GreenLantern.LOGGER.info("CLIENTSCRIPT transformation {} played={}", id, played);
        }));
        step(18, () -> clean(label + "_a"));
        step(14, () -> clean(label + "_b"));
        step(30, () -> {});
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
                sp.setInvulnerable(true);
                base = sp.blockPosition();
            });
            suit("prototipo");
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });

        for (String id : Transformations.IDS) transformation(id);

        // Beams: straight and thick, spiral, lightning.
        for (String form : List.of("reto", "espiral", "raio")) {
            String label = label("feixe_" + form);
            reset(CameraType.THIRD_PERSON_BACK, 25);
            step(6, () -> husk(3.4, 7.3));
            step(10, () -> fight(PresetTests.ability("feixe_continuo", AbilityKind.SHOT, 1, 1, 16, 0, 3,
                    PresetTests.settings("forma", form, "espessura", form.equals("reto") ? 2.5 : 1.2))));
            step(25, () -> clean(label));
            step(40, () -> {});
        }

        // A translucent hammer of energy over a husk.
        String hammer = label("construto_martelo");
        reset(CameraType.THIRD_PERSON_BACK, 25);
        step(6, () -> husk(2.1, 4.5));
        step(10, () -> fight(PresetTests.ability("construto", AbilityKind.SUMMON, 1, 1, 10, 0, 4,
                PresetTests.settings("forma", "martelo", "intervalo", 3, "escala", 1.4))));
        step(25, () -> clean(hammer));
        step(60, () -> {});

        // The element families, side by side.
        String families = label("familias_de_particulas");
        reset(CameraType.THIRD_PERSON_BACK, 20);
        for (int t = 0; t < 6; t++) {
            step(4, () -> server(sp -> {
                List<String> elements = List.of("energia", "raio", "fogo", "gelo", "agua", "sombra", "magia", "luz", "fumaca", "terra", "plantas", "vento");
                for (int i = 0; i < elements.size(); i++) {
                    Vec3 at = new Vec3(base.getX() + 0.5 - 5.5 + i, base.getY() + 1.2, base.getZ() + 6.5);
                    PresetFx.burst(sp.level(), at, elements.get(i), 0x40C0FF, 1.2F, 12, 0.25);
                }
            }));
        }
        step(20, () -> clean(families));

        // The trail and aura of the Protótipo Alado, running past.
        step(4, () -> suit("prototipo_alado"));
        String aura = label("aura");
        reset(CameraType.THIRD_PERSON_FRONT, 10);
        step(40, () -> clean(aura));
        String trail = label("rastro");
        reset(CameraType.THIRD_PERSON_BACK, 10);
        for (int t = 0; t < 24; t++) {
            int i = t;
            step(1, () -> server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 0.5 + i * 0.6)));
        }
        step(4, () -> clean(trail));

        // The wheel and the HUD with the icons drawn from the presets.
        step(4, () -> suit("prototipo"));
        reset(CameraType.THIRD_PERSON_BACK, 10);
        String wheel = label("roda_de_icones");
        step(20, () -> {
            mc().options.hideGui = false;
            mc().setScreen(new PowerWheelScreen(hero("prototipo")));
        });
        step(25, () -> shot(wheel));
        step(4, () -> mc().setScreen(null));
        String hud = label("hud_de_icones");
        step(25, () -> shot(hud));
        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
