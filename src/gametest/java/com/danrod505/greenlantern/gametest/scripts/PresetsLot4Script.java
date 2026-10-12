package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.gametest.heroes.PresetLot4Tests;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets_lote4" (stage 2, batch 3): the test character Protótipo uses each
 * preset of Controle, Invocação, Fortalecimento and Suprema. The abilities are made on the spot with
 * the numbers and settings a character file would give.
 */
public final class PresetsLot4Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets_lote4", PresetsLot4Script::build, PresetsLot4Script::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;
    /** Where the next use aims, on the server only: the camera keeps looking down from behind. */
    private static float aimPitch = 0;

    private PresetsLot4Script() {}

    private static EngineAbility world(String preset, double reach, double radius, double duration, Object... settings) {
        return PresetLot4Tests.world(preset, reach, radius, duration, PresetTests.settings(settings));
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
            if (!base.equals(BlockPos.ZERO)) {
                fill(-5, -4, 1, 5, -2, 13, "minecraft:dirt");
                fill(-5, -1, 1, 5, -1, 13, "minecraft:grass_block");
                fill(-5, 0, 1, 5, 7, 13, "minecraft:air");
            }
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
            EngineHero hero = (EngineHero) HeroRegistry.byId("prototipo").orElseThrow();
            boolean went = EngineServer.useInWorld(sp, hero, sp.getMainHandItem(), ability);
            GreenLantern.LOGGER.info("CLIENTSCRIPT world {} went={}", ability.data().worldPreset().orElse("-"), went);
        });
    }

    private static String label(String name) {
        return String.format("pw%02d_%s", ++number, name);
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
        char suffix = 'b';
        for (int delay : delays) {
            String shot = label + "_" + suffix++ + "_depois";
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

    /** One world use: reset, the stage, a shot before, the use and the shots after. */
    private static void scene(String name, float pitch, Runnable stage, EngineAbility ability, int... delays) {
        String label = label(name);
        reset(pitch);
        step(10, stage);
        step(30, () -> clean(label + "_a_antes"));
        step(2, () -> use(ability));
        shots(label, delays);
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

        // Mining: a stone wall ahead with a 3x3 hole.
        scene("minerar_area", 8, () -> fill(-3, 0, 4, 3, 4, 4, "minecraft:stone"),
                world("minerar_area", 6, 0, 0, "tamanho", 3), 25);
        step(4, () -> fill(-3, 0, 4, 3, 4, 4, "minecraft:air"));

        // Building: energy stairs, a bridge over a ditch and a wall from the inventory.
        scene("construir_escada", 0, () -> {}, world("construir", 0, 0, 20, "forma", "escada", "comprimento", 6), 25);
        scene("construir_ponte", 0, () -> fill(-3, -4, 1, 3, -1, 9, "minecraft:air"),
                world("construir", 0, 0, 20, "forma", "ponte", "comprimento", 8, "largura", 3), 25);
        step(4, () -> {
            fill(-3, -4, 1, 3, -2, 9, "minecraft:dirt");
            fill(-3, -1, 1, 3, -1, 9, "minecraft:grass_block");
        });
        scene("construir_parede", 0, () -> server(sp -> sp.getInventory().add(new ItemStack(Items.STONE_BRICKS, 32))),
                world("construir", 0, 0, 20, "forma", "parede", "material", "inventario", "largura", 5, "altura", 3), 25);

        // Farming: a field of wheat at every age with water in the middle.
        scene("plantar_e_colher", 30, () -> {
            fill(-3, -1, 2, 3, -1, 7, "minecraft:farmland");
            fill(0, -1, 4, 0, -1, 4, "minecraft:water");
            fill(-3, 0, 2, -1, 0, 7, "minecraft:wheat[age=7]");
            fill(1, 0, 2, 3, 0, 7, "minecraft:wheat[age=1]");
        }, world("plantar_e_colher", 0, 6, 0), 25);
        step(4, () -> {
            fill(-3, 0, 2, 3, 0, 7, "minecraft:air");
            fill(-3, -1, 2, 3, -1, 7, "minecraft:grass_block");
        });

        // Ice: a pool of water and one of lava ahead; heat: snow and an unlit campfire.
        scene("gelo", 30, () -> {
            fill(-3, -1, 2, -1, -1, 6, "minecraft:water");
            fill(1, -1, 2, 3, -1, 6, "minecraft:lava");
        }, world("gelo_e_calor", 0, 7, 0, "efeito", "gelo"), 25);
        step(4, () -> fill(-3, -1, 2, 3, -1, 6, "minecraft:grass_block"));
        scene("calor", 30, () -> {
            fill(-3, 0, 2, -1, 1, 5, "minecraft:snow_block");
            fill(1, 0, 3, 1, 0, 3, "minecraft:campfire[lit=false]");
            fill(2, 0, 4, 2, 0, 4, "minecraft:candle[lit=false,candles=4]");
        }, world("gelo_e_calor", 0, 7, 0, "efeito", "calor"), 25);
        step(4, () -> fill(-3, 0, 2, 3, 1, 5, "minecraft:air"));

        // Light: at night.
        scene("luz", 20, () -> command("time set 18000"), world("luz", 0, 10, 20), 25);
        step(4, () -> command("time set 6000"));

        // Moving a block: the gold block floats over and goes down further ahead.
        String move = label("mover_blocos");
        EngineAbility mover = world("mover_blocos", 8, 0, 30);
        reset(30);
        step(4, () -> fill(0, 0, 3, 0, 0, 3, "minecraft:gold_block"));
        step(8, () -> use(mover));
        step(25, () -> clean(move + "_a_carregado"));
        step(4, () -> aimPitch = 12);
        step(2, () -> use(mover));
        step(25, () -> clean(move + "_b_posto"));
        step(4, () -> fill(-2, 0, 2, 2, 2, 12, "minecraft:air"));

        // Opening a way: rough ground, a tree, the water.
        scene("abrir_terreno", 10, () -> {
            fill(-2, 0, 2, -2, 1, 6, "minecraft:dirt");
            fill(1, 0, 4, 1, 0, 8, "minecraft:sand");
            fill(0, 0, 3, 0, 0, 3, "minecraft:poppy");
            fill(-1, 0, 6, 1, 0, 6, "minecraft:tall_grass");
        }, world("abrir_caminho", 0, 0, 0, "remove", "terreno", "comprimento", 10, "largura", 5), 25);
        scene("abrir_arvores", 20, () -> {
            fill(-2, 3, 3, 2, 4, 7, "minecraft:oak_leaves[persistent=true]");
            fill(-1, 5, 4, 1, 5, 6, "minecraft:oak_leaves[persistent=true]");
            fill(0, 0, 5, 0, 4, 5, "minecraft:oak_log");
        }, world("abrir_caminho", 0, 0, 0, "remove", "arvores"), 25);
        scene("abrir_agua", 35, () -> fill(-4, -4, 2, 4, 0, 12, "minecraft:water"),
                world("abrir_caminho", 0, 0, 10, "remove", "agua", "comprimento", 10, "largura", 3, "altura", 4), 25);
        step(4, () -> {
            fill(-4, -4, 2, 4, -2, 12, "minecraft:dirt");
            fill(-4, -1, 2, 4, -1, 12, "minecraft:grass_block");
        });

        // Searching: chests and barrels buried around.
        scene("procurar", 30, () -> {
            fill(-3, -2, 4, -3, -2, 4, "minecraft:chest");
            fill(3, -3, 6, 3, -3, 6, "minecraft:barrel");
            fill(0, -2, 9, 0, -2, 9, "minecraft:chest");
        }, world("procurar", 0, 16, 10, "mostra", "baus"), 25);

        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
