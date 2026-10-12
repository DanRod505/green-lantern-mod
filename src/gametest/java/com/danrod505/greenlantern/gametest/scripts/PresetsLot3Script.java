package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets_lote3" (stage 2, batch 3): the test character Protótipo uses each
 * preset of Controle, Invocação, Fortalecimento and Suprema. The abilities are made on the spot with
 * the numbers and settings a character file would give.
 */
public final class PresetsLot3Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets_lote3", PresetsLot3Script::build, PresetsLot3Script::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;
    /** Where the next use aims, on the server only: the camera keeps looking down from behind. */
    private static float aimPitch = 0;

    private PresetsLot3Script() {}

    private static EngineAbility ability(String preset, AbilityKind kind, double damage, double reach, double radius, double duration, Object... settings) {
        return PresetTests.ability(preset, kind, damage, 0.5, reach, radius, duration, PresetTests.settings(settings));
    }

    /** Creatures at these offsets from the base (x, z): still husks, or with {@code alive} zombies that move. */
    private static void targets(boolean alive, double... offsets) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < offsets.length; i += 2) {
                Mob mob = alive ? EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND) : EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                if (mob == null) continue;
                mob.setNoAi(!alive);
                mob.setPersistenceRequired();
                mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
                mob.snapTo(base.getX() + 0.5 + offsets[i], base.getY(), base.getZ() + 0.5 + offsets[i + 1], 180.0F, 0.0F);
                mob.setYHeadRot(180.0F);
                level.addFreshEntity(mob);
            }
        });
    }

    private static void targets(double... offsets) {
        targets(false, offsets);
    }

    /** A fill command relative to the base. */
    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        command(String.format("fill %d %d %d %d %d %d %s", base.getX() + x1, base.getY() + y1, base.getZ() + z1,
                base.getX() + x2, base.getY() + y2, base.getZ() + z2, block));
    }

    /** Ends what is still on, clears the stage and puts the player back at the base, looking south. */
    private static void reset(float pitch) {
        step(4, () -> {
            command("kill @e[type=!minecraft:player]");
            server(sp -> {
                PresetRuntime.stopLasting(sp);
                sp.stopRiding();
                sp.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                sp.setDeltaMovement(Vec3.ZERO);
                sp.hurtMarked = true;
                sp.setHealth(sp.getMaxHealth());
            });
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 35);
            aimPitch = pitch;
        });
    }

    private static void use(EngineAbility ability) {
        server(sp -> {
            sp.setYRot(0);
            sp.setXRot(aimPitch);
            sp.setYHeadRot(0);
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), ability);
            GreenLantern.LOGGER.info("CLIENTSCRIPT preset {} went={}", ability.data().preset(), went);
        });
    }

    private static String label(String name) {
        return String.format("pr%02d_%s", ++number, name);
    }

    /** One preset: reset, targets, use, and a screenshot after each of {@code delays} (ticks after the previous one). */
    private static void demo(String name, EngineAbility ability, float pitch, boolean alive, double[] offsets, int... delays) {
        String label = label(name);
        reset(pitch);
        step(6, () -> targets(alive, offsets));
        step(12, () -> use(ability));
        shots(label, delays);
    }

    private static void demo(String name, EngineAbility ability, float pitch, double[] offsets, int... delays) {
        demo(name, ability, pitch, false, offsets, delays);
    }

    private static void shots(String label, int... delays) {
        char suffix = 'a';
        for (int delay : delays) {
            String shot = delays.length == 1 ? label : label + "_" + suffix++;
            step(delay, () -> clean(shot));
        }
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

        // ---- Controle ----
        demo("puxar", ability("puxar", AbilityKind.CONTROL, 0, 12, 0, 0, "forca", 1.4), 4, new double[]{0, 8}, 2, 5);
        demo("prender_energia", ability("prender", AbilityKind.CONTROL, 0, 12, 0, 4), 10, new double[]{0, 5}, 8);
        demo("prender_gelo", ability("prender", AbilityKind.CONTROL, 0, 12, 0, 4, "material", "gelo"), 10, new double[]{0, 5}, 8);
        demo("lentidao", ability("lentidao", AbilityKind.CONTROL, 0, 0, 6, 4, "elemento", "gelo"), 20,
                new double[]{-2.5, 3, 2.5, 3, 0, 5}, 25);
        demo("confusao", ability("medo_e_confusao", AbilityKind.CONTROL, 0, 0, 10, 5, "tipo", "confusao", "elemento", "magia"), 25, true,
                new double[]{-2, 5, 2, 5, 0, 8}, 12, 20);
        demo("medo", ability("medo_e_confusao", AbilityKind.CONTROL, 0, 0, 10, 5, "tipo", "medo", "elemento", "sombra"), 25, true,
                new double[]{-2, 4, 2, 4, 0, 6}, 12, 20);
        // Domination: the near zombie turns on the husk behind it.
        String dominate = label("dominar");
        reset(10);
        step(6, () -> {
            targets(true, 0, 4);
            targets(0, 9);
        });
        step(12, () -> use(ability("dominar", AbilityKind.CONTROL, 0, 12, 0, 15, "elemento", "magia")));
        shots(dominate, 4, 40);
        demo("levitar_criatura", ability("levitar", AbilityKind.CONTROL, 0, 16, 0, 4, "altura", 3), 12, new double[]{0, 5}, 20);
        // Lifting a block: a gold block ahead.
        String lift = label("levitar_bloco");
        reset(30);
        step(4, () -> fill(0, 0, 4, 0, 0, 4, "minecraft:gold_block"));
        step(10, () -> use(ability("levitar", AbilityKind.CONTROL, 0, 16, 0, 2, "altura", 3)));
        shots(lift, 20);
        step(50, () -> fill(-1, 0, 3, 1, 4, 5, "minecraft:air"));

        // ---- Invocação ----
        demo("ajudante", ability("ajudante", AbilityKind.SUMMON, 4, 0, 0, 20, "quantidade", 2), 20, new double[]{-1, 8, 1, 9}, 6, 40);
        demo("construto_martelo", ability("construto", AbilityKind.SUMMON, 3, 10, 0, 6, "forma", "martelo", "intervalo", 1), 25,
                new double[]{-2, 5, 2, 6}, 8, 22);
        demo("construto_serra", ability("construto", AbilityKind.SUMMON, 2, 10, 0, 6, "forma", "serra"), 25, new double[]{-2, 2, 2, 3}, 10);
        demo("construto_broca", ability("construto", AbilityKind.SUMMON, 3, 10, 0, 6, "forma", "broca"), 25, new double[]{0, 6}, 8, 12);
        demo("veiculo_cavalo", ability("veiculo", AbilityKind.SUMMON, 0, 0, 0, 20), 20, new double[]{}, 30);
        demo("veiculo_ghast", ability("veiculo", AbilityKind.SUMMON, 0, 0, 0, 20, "entidade", "minecraft:happy_ghast"), 20, new double[]{}, 14);
        demo("enxame", ability("enxame", AbilityKind.SUMMON, 1, 12, 0, 6, "quantidade", 8), 20, new double[]{0, 7}, 10, 20);
        demo("copia", ability("copia", AbilityKind.SUMMON, 1, 0, 0, 8, "quantidade", 2), 20, true, new double[]{-1, 9, 1, 10}, 10, 30);
        demo("torre", ability("torre", AbilityKind.SUMMON, 2, 16, 0, 10, "cadencia", 0.5), 20, new double[]{-3, 10, 3, 12}, 12, 14);

        // ---- Fortalecimento ----
        demo("aura", ability("aura", AbilityKind.EMPOWER, 0, 0, 0, 8, "atributo", "todos"), 20, new double[]{}, 12);
        String heal = label("cura");
        reset(20);
        step(4, () -> server(sp -> sp.setHealth(6.0F)));
        step(6, () -> use(ability("cura", AbilityKind.EMPOWER, 0, 0, 0, 0, "quanto", 8, "tempo", 2)));
        shots(heal, 12);
        String grow = label("crescer");
        reset(20);
        step(6, () -> targets(2, 4, -2, 4));
        step(12, () -> use(ability("crescer", AbilityKind.EMPOWER, 0, 0, 0, 10, "tamanho", 2.0)));
        shots(grow + "_a", 20);
        step(4, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean(grow + "_b_front"));
        demo("furia", ability("furia", AbilityKind.EMPOWER, 0, 0, 0, 8), 20, new double[]{}, 14);
        // Senses: ores under the grass ahead and husks behind a wall.
        String senses = label("sentidos");
        reset(30);
        step(4, () -> {
            fill(-3, -2, 4, -3, -2, 4, "minecraft:diamond_ore");
            fill(2, -3, 6, 2, -3, 6, "minecraft:gold_ore");
            fill(0, -2, 9, 1, -2, 9, "minecraft:iron_ore");
            fill(-3, 0, 7, 3, 2, 7, "minecraft:stone_bricks");
        });
        step(6, () -> targets(-1, 10, 2, 11));
        step(10, () -> use(ability("sentidos", AbilityKind.EMPOWER, 0, 16, 0, 8)));
        shots(senses, 6);
        step(4, () -> {
            fill(-3, -3, 4, 3, -2, 9, "minecraft:dirt");
            fill(-3, 0, 7, 3, 2, 7, "minecraft:air");
        });

        // ---- Suprema ----
        demo("suprema_energia", PresetTests.supreme(ability("suprema_de_combate", AbilityKind.BLAST, 20, 0, 8, 0, "carga", 1.5)), 25,
                new double[]{-3, 3, 3, 3, 0, 6, -5, 6, 5, 6}, 15, 18, 6);
        demo("suprema_raio", PresetTests.supreme(ability("suprema_de_combate", AbilityKind.BLAST, 20, 0, 8, 0, "carga", 1.0, "visual", "raio")), 25,
                new double[]{-3, 3, 3, 3, 0, 6}, 21, 4);
        demo("suprema_solar", PresetTests.supreme(ability("suprema_de_combate", AbilityKind.BLAST, 20, 0, 8, 0, "carga", 1.0, "visual", "solar")), 25,
                new double[]{-3, 3, 3, 3, 0, 6}, 21, 4);
        // The great work: an igloo goes up piece by piece; the key again takes it down.
        String work = label("grande_obra");
        EngineAbility igloo = PresetTests.supreme(ability("grande_obra", AbilityKind.BUILDING, 0, 0, 0, 0, "tempo", 4, "distancia", 3));
        reset(10);
        step(6, () -> look(0, 25));
        step(4, () -> use(igloo));
        shots(work, 30, 60);
        step(10, () -> server(sp -> {
            sp.setYRot(0);
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), igloo);
            GreenLantern.LOGGER.info("CLIENTSCRIPT preset grande_obra dismantled={}", went);
        }));
        step(10, () -> clean(work + "_c_down"));

        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
