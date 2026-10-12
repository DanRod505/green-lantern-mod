package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.gametest.heroes.PresetTests;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets" (stage 2, batch 1): the test character Protótipo uses each preset of
 * Golpe, Disparo and Explosão on husks that stand still (husks don't burn in the sun). The abilities
 * are made on the spot with the numbers and settings a character file would give.
 */
public final class PresetsScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets", PresetsScript::build, PresetsScript::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;

    private PresetsScript() {}

    private static EngineAbility ability(String preset, AbilityKind kind, double damage, double reach, double radius, double duration, Object... settings) {
        return PresetTests.ability(preset, kind, damage, 0.5, reach, radius, duration, PresetTests.settings(settings));
    }

    /** Husks that stand still at these offsets from the base (x, z). */
    private static void targets(double... offsets) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < offsets.length; i += 2) {
                Husk husk = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                if (husk == null) continue;
                husk.setNoAi(true);
                husk.setPersistenceRequired();
                husk.snapTo(base.getX() + 0.5 + offsets[i], base.getY(), base.getZ() + 0.5 + offsets[i + 1], 180.0F, 0.0F);
                husk.setYHeadRot(180.0F);
                level.addFreshEntity(husk);
            }
        });
    }

    /** Clears the stage and puts the player back at the base, looking south. */
    private static void reset(float pitch) {
        step(4, () -> {
            command("kill @e[type=!minecraft:player,tag=!gl_camera]");
            server(sp -> {
                sp.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                sp.setDeltaMovement(Vec3.ZERO);
                sp.setHealth(sp.getMaxHealth());
            });
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 35);
            aimYaw = 0;
            aimPitch = pitch;
        });
    }

    /** Where the next use aims, on the server only: the camera keeps looking down from behind and above. */
    private static float aimYaw = 0, aimPitch = 0;

    /** Aims the next use at the first target (eye to the middle of a husk); the camera turns the same way but stays high. */
    private static void aimAt(double x, double z) {
        aimYaw = (float) Math.toDegrees(Math.atan2(-x, z));
        aimPitch = (float) Math.toDegrees(Math.atan2(0.6, Math.max(1.0, Math.hypot(x, z))));
        mc().player.setYRot(aimYaw);
        mc().player.yRotO = aimYaw;
        mc().player.setYHeadRot(aimYaw);
    }


    private static void use(EngineAbility ability) {
        server(sp -> {
            sp.setYRot(aimYaw);
            sp.setXRot(aimPitch);
            sp.setYHeadRot(aimYaw);
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), ability);
            GreenLantern.LOGGER.info("CLIENTSCRIPT preset {} went={}", ability.data().preset(), went);
        });
    }

    /** One preset: reset, targets, use, and a screenshot after each of {@code delays} (ticks after the previous one). */
    private static void demo(String name, EngineAbility ability, float pitch, double[] offsets, int... delays) {
        String label = String.format("pr%02d_%s", ++number, name);
        reset(pitch);
        step(6, () -> targets(offsets));
        if (offsets.length >= 2) step(2, () -> aimAt(offsets[0], offsets[1]));
        step(12, () -> use(ability));
        char suffix = 'a';
        for (int delay : delays) {
            String shot = delays.length == 1 ? label : label + "_" + suffix++;
            step(delay, () -> clean(shot));
        }
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
                base = sp.blockPosition();
                HeroRegistry.suited(sp).ifPresent(h -> h.dismissSuit(sp, false));
                HeroDefinition hero = HeroRegistry.byId("prototipo").orElseThrow();
                sp.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
                sp.getInventory().add(new ItemStack(Items.ARROW, 16));
                boolean on = hero.summonSuit(sp);
                GreenLantern.LOGGER.info("CLIENTSCRIPT suit prototipo on={}", on);
            });
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });

        // ---- Golpe ----
        demo("soco_forte", ability("soco_forte", AbilityKind.STRIKE, 4, 0, 0, 0, "empurrao", 2.0), 8, new double[]{1.0, 3}, 2, 6);
        demo("investida", ability("investida", AbilityKind.STRIKE, 3, 10, 0, 0, "velocidade", 0.8), 4, new double[]{1.0, 6}, 4, 6);
        demo("golpe_de_arma", ability("golpe_de_arma", AbilityKind.STRIKE, 3, 3.5, 0, 0, "arma", "espada"), 10,
                new double[]{-1.5, 2, 1.5, 2}, 2, 2);
        // The stomp from the air: the character dives and lands among them.
        reset(30);
        step(6, () -> {
            targets(-2.5, 1.5, 2.5, 1.5, 0, 3.5);
            server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() + 5, base.getZ() + 0.5));
        });
        step(4, () -> use(ability("pisao", AbilityKind.STRIKE, 3, 0, 4.5, 0)));
        String stomp = String.format("pr%02d_pisao", ++number);
        step(2, () -> clean(stomp + "_a_dive"));
        watch(30, () -> mc().player.onGround(), stomp + "_b_land");
        step(3, () -> clean(stomp + "_c_after"));
        demo("agarrar_e_arremessar", ability("agarrar_e_arremessar", AbilityKind.STRIKE, 4, 5, 0, 0, "distancia", 14), 12,
                new double[]{1.0, 3}, 6, 10);
        // The combo: three presses in rhythm, with the HUD to show the count.
        String combo = String.format("pr%02d_sequencia", ++number);
        EngineAbility sequence = ability("sequencia", AbilityKind.STRIKE, 2, 3.5, 0, 0, "golpes", 3, "intervalo", 1.0);
        reset(10);
        step(6, () -> targets(1.0, 3));
        step(2, () -> aimAt(1.0, 3));
        step(12, () -> use(sequence));
        step(2, () -> {
            mc().options.hideGui = false;
            shot(combo + "_a_first");
        });
        step(8, () -> use(sequence));
        step(2, () -> shot(combo + "_b_second"));
        step(8, () -> use(sequence));
        step(2, () -> clean(combo + "_c_finisher"));

        // ---- Disparo ----
        demo("projetil", ability("projetil", AbilityKind.SHOT, 4, 24, 0, 0, "velocidade", 0.9, "perseguicao", 0.3, "elemento", "fogo"), 3,
                new double[]{1.5, 10}, 4, 8);
        demo("feixe_continuo", ability("feixe_continuo", AbilityKind.SHOT, 4, 20, 0, 2.0), 4, new double[]{1.5, 8}, 10, 20);
        demo("leque", ability("leque", AbilityKind.SHOT, 2, 20, 0, 0, "quantidade", 7, "abertura", 70, "elemento", "gelo", "velocidade", 0.8), 3,
                new double[]{-4, 9, 0, 10, 4, 9}, 4, 6);
        demo("arremesso_que_volta", ability("arremesso_que_volta", AbilityKind.SHOT, 3, 12, 0, 0, "quicadas", 2, "item", "minecraft:shield",
                "velocidade", 0.7), 6, new double[]{0, 7, 4, 9, -4, 9}, 6, 10, 14);
        demo("balistico", ability("balistico", AbilityKind.SHOT, 3, 40, 2.5, 0, "ponta", "explosiva", "municao", "minecraft:arrow",
                "velocidade", 1.4, "gravidade", 0.03), 2, new double[]{0, 11, 1.5, 11.5}, 5, 8);
        demo("raio_do_ceu", ability("raio_do_ceu", AbilityKind.SHOT, 4, 32, 2.5, 0, "atraso", 0.6), 8, new double[]{0, 8}, 6, 10);
        demo("raio_do_ceu_feixe", ability("raio_do_ceu", AbilityKind.SHOT, 4, 32, 2.5, 0, "atraso", 0.6, "visual", "feixe"), 8,
                new double[]{0, 8}, 13);

        // ---- Explosão ----
        demo("onda_em_volta", ability("onda_em_volta", AbilityKind.BLAST, 3, 0, 5, 0), 25, new double[]{-3, 1, 3, 1, 0, 3.5}, 2, 4);
        demo("explosao_no_ponto", ability("explosao_no_ponto", AbilityKind.BLAST, 5, 32, 3, 0, "quebra_blocos", true), 12,
                new double[]{0, 9, 2, 10}, 2, 30);
        demo("cone_gelo", ability("cone", AbilityKind.BLAST, 4, 6, 0, 1.5, "elemento", "gelo"), 8, new double[]{0, 4, 1.5, 5}, 10, 20);
        demo("cone_fogo", ability("cone", AbilityKind.BLAST, 4, 6, 0, 1.5, "elemento", "fogo"), 8, new double[]{0, 4}, 12);
        demo("pulso", ability("pulso", AbilityKind.BLAST, 1, 0, 6, 3), 25, new double[]{-3, 2, 3, 2, 0, 4}, 2, 4);
        demo("chuva", ability("chuva", AbilityKind.BLAST, 1, 24, 4, 3, "elemento", "fogo", "golpes_por_segundo", 8), 15,
                new double[]{0, 10, 2, 11, -2, 11}, 20, 20);

        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
