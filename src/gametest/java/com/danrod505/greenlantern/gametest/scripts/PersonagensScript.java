package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "personagens" (stage 2, lot 7): the prints of a lot of characters. For each
 * character: the suit going on, front and back, the wheel, and every ability used on a husk in front.
 * <p>
 * Which characters: the ids in {@code GL_PERSONAGENS} (comma separated); without it, every character
 * of the engine that is neither a test one nor one of the 9 old heroes, so after
 * {@code python3 tools/catalogo.py --lote N} it shows that lot; with none of those, the test characters.
 */
public final class PersonagensScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("personagens", PersonagensScript::build, PersonagensScript::log, false);

    private static final List<String> TEST_CHARACTERS = List.of("prototipo", "prototipo_colosso", "prototipo_alado");

    private static BlockPos base = BlockPos.ZERO;

    private PersonagensScript() {}

    /** The characters this run shows, in the order of the registry. */
    static List<EngineHero> characters() {
        List<EngineHero> engine = HeroRegistry.all().stream().filter(h -> h instanceof EngineHero).map(h -> (EngineHero) h).toList();
        String wanted = System.getenv("GL_PERSONAGENS");
        if (wanted != null && !wanted.isBlank()) {
            List<String> ids = Arrays.stream(wanted.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            return engine.stream().filter(h -> ids.contains(h.data().id())).toList();
        }
        List<EngineHero> made = engine.stream().filter(h -> !TEST_CHARACTERS.contains(h.data().id())).toList();
        return made.isEmpty() ? engine : made;
    }

    private static void suit(EngineHero hero) {
        server(sp -> {
            HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false));
            sp.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
            boolean on = hero.summonSuit(sp);
            GreenLantern.LOGGER.info("CLIENTSCRIPT suit {} on={}", hero.data().id(), on);
        });
    }

    /** A fill command relative to the base. */
    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        command(String.format("fill %d %d %d %d %d %d %s", base.getX() + x1, base.getY() + y1, base.getZ() + z1,
                base.getX() + x2, base.getY() + y2, base.getZ() + z2, block));
    }

    /**
     * Ends what is still on, clears the stage, then (a step later, once the commands ran) puts the
     * player back looking south with a husk 6 blocks in front, a little to the side so the camera sees it.
     */
    private static void reset() {
        step(4, () -> {
            server(PresetRuntime::stopLasting);
            command("kill @e[type=!minecraft:player]");
            command("weather clear");
            if (!base.equals(BlockPos.ZERO)) {
                fill(-6, -4, -3, 6, -2, 14, "minecraft:dirt");
                fill(-6, -1, -3, 6, -1, 14, "minecraft:grass_block");
                fill(-6, 0, -3, 6, 8, 14, "minecraft:air");
            }
        });
        step(4, () -> server(sp -> {
            sp.stopRiding();
            sp.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            sp.setYRot(0);
            sp.setXRot(10);
            sp.setYHeadRot(0);
            sp.setDeltaMovement(Vec3.ZERO);
            sp.hurtMarked = true;
            sp.setHealth(sp.getMaxHealth());
            Mob husk = EntityType.HUSK.create(sp.level(), EntitySpawnReason.COMMAND);
            if (husk != null) {
                husk.setNoAi(true);
                husk.setPersistenceRequired();
                husk.snapTo(base.getX() + 2.5, base.getY(), base.getZ() + 6.5, 180.0F, 0.0F);
                sp.level().addFreshEntity(husk);
            }
        }));
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
                base = sp.blockPosition();
            });
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });
        int n = 0;
        for (EngineHero hero : characters()) {
            String label = String.format("pc%02d_%s", n++, hero.data().id());
            reset();
            step(2, () -> {
                camera(CameraType.THIRD_PERSON_FRONT);
                look(0, 10);
            });
            step(2, () -> suit(hero));
            step(8, () -> clean(label + "_a_transformacao"));
            step(20, () -> clean(label + "_b_frente"));
            step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
            step(4, () -> clean(label + "_c_costas"));
            step(2, () -> {
                mc().options.hideGui = false;
                key(KeyBindings.CONSTRUCT_WHEEL, true);
            });
            step(10, () -> shot(label + "_d_roda"));
            step(2, () -> {
                key(KeyBindings.CONSTRUCT_WHEEL, false);
                if (mc().screen != null) mc().setScreen(null);
                mc().options.hideGui = true;
            });
            for (int i = 0; i < hero.powers().set().count(); i++) {
                EngineAbility ability = hero.powers().set().byIndex(i);
                String name = String.format("%s_e%d_%s", label, i + 1, ability.data().id());
                reset();
                step(2, () -> {
                    camera(CameraType.THIRD_PERSON_BACK);
                    look(0, 10);
                });
                step(2, () -> server(sp -> {
                    boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), ability);
                    GreenLantern.LOGGER.info("CLIENTSCRIPT {} {} ({}) went={}", hero.data().id(), ability.data().id(), ability.data().preset(), went);
                }));
                step(10, () -> clean(name));
            }
            step(4, () -> server(sp -> HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false))));
        }
        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
