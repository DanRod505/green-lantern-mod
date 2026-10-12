package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.PowerWheelScreen;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets_lote6" (stage 2, batch 6, the new model of abilities): the Protótipo
 * uses each new world preset, then the HUD shows the function V would use now (and the outline of
 * the block aimed at), the wheel with the supreme ability's gold border and the traits.
 */
public final class PresetsLot6Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets_lote6", PresetsLot6Script::build, PresetsLot6Script::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;
    /** Where the next use aims, on the server only: the camera keeps looking down from behind. */
    private static float aimPitch = 0;

    private PresetsLot6Script() {}

    private static EngineAbility world(String preset, double reach, double radius, double duration, Object... settings) {
        return PresetLot4Tests.world(preset, reach, radius, duration, PresetTests.settings(settings));
    }

    private static EngineHero hero(String id) {
        return (EngineHero) HeroRegistry.byId(id).orElseThrow();
    }

    /** Creatures of {@code type} at these offsets from the base (x, z), that move. */
    private static void animals(EntityType<? extends Mob> type, double... offsets) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < offsets.length; i += 2) {
                Mob mob = type.create(level, EntitySpawnReason.COMMAND);
                if (mob == null) continue;
                mob.setPersistenceRequired();
                mob.snapTo(base.getX() + 0.5 + offsets[i], base.getY(), base.getZ() + 0.5 + offsets[i + 1], 180.0F, 0.0F);
                level.addFreshEntity(mob);
            }
        });
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
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), ability);
            GreenLantern.LOGGER.info("CLIENTSCRIPT world {} went={}", ability.data().preset(), went);
        });
    }

    private static String label(String name) {
        return String.format("pm%02d_%s", ++number, name);
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

    /** Selects ability {@code index} of the character the player wears. */
    private static void pick(String id, int index) {
        server(sp -> hero(id).powers().select(sp, sp.getMainHandItem(), index, false));
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

        // Coleta e terreno.
        scene("cortar_arvore", 10, () -> {
            fill(0, 0, 4, 0, 4, 4, "minecraft:oak_log");
            fill(-2, 3, 2, 2, 4, 6, "minecraft:oak_leaves[persistent=true]");
            fill(-1, 5, 3, 1, 5, 5, "minecraft:oak_leaves[persistent=true]");
        }, world("cortar_arvore", 6, 0, 0), 25);
        scene("cavar_solto", 8, () -> fill(-3, 0, 4, 3, 3, 6, "minecraft:dirt"),
                world("cavar_solto", 6, 0, 0, "tamanho", 3, "profundidade", 2), 25);
        scene("aplanar", 30, () -> {
            fill(-2, 0, 1, -1, 1, 2, "minecraft:dirt");
            fill(1, 0, 2, 2, 2, 3, "minecraft:stone");
            fill(-2, -1, -2, 2, -2, -1, "minecraft:air");
            fill(0, 0, -2, 0, 0, -2, "minecraft:poppy");
        }, world("aplanar", 0, 3, 0, "altura", 4, "fundo", 2), 25);
        scene("pescar", 22, () -> fill(-2, -2, 3, 2, -1, 7, "minecraft:water"), world("pescar", 12, 0, 0, "quantidade", 4), 20);
        step(4, () -> fill(-2, -2, 3, 2, -1, 7, "minecraft:grass_block"));

        // Construção and Transmutação.
        String plan = label("projeto_em_fantasma");
        EngineAbility ghost = world("projeto_em_fantasma", 0, 0, 60, "estrutura", "minecraft:igloo/top", "distancia", 3);
        reset(0);
        step(6, () -> server(sp -> {
            sp.getInventory().add(new ItemStack(Items.SNOW_BLOCK, 64));
            sp.getInventory().add(new ItemStack(Items.ICE, 8));
        }));
        step(6, () -> use(ghost));
        step(25, () -> clean(plan + "_a_fantasma"));
        step(4, () -> use(ghost));
        step(25, () -> clean(plan + "_b_construido"));
        scene("trocar_fornalha", 10, () -> fill(-2, 0, 4, 2, 2, 4, "minecraft:sand"),
                world("trocar_bloco", 8, 2, 0, "modo", "fornalha"), 25);
        scene("trocar_cor", 10, () -> fill(-2, 0, 4, 2, 2, 4, "minecraft:white_wool"),
                world("trocar_bloco", 8, 2, 0, "modo", "cor", "cor_nova", "purple"), 25);
        scene("ligar_maquina", 8, () -> {
            fill(-1, 0, 6, 1, 2, 6, "minecraft:stone_bricks");
            fill(0, 1, 5, 0, 1, 5, "minecraft:redstone_lamp");
            fill(0, 0, 5, 0, 0, 5, "minecraft:stone_bricks");
        }, world("ligar_maquina", 16, 0, 4), 10);

        // Natureza.
        scene("domar_e_guiar", 25, () -> {
            animals(EntityType.WOLF, -2, 4);
            animals(EntityType.PIG, 2, 3, 1, 5);
        }, world("domar_e_guiar", 0, 8, 30, "quantos", 4), 10, 30);
        scene("clima", 0, () -> {}, world("clima", 0, 0, 30, "tipo", "chuva"), 60);
        step(4, () -> command("weather clear"));
        scene("rastro", 20, () -> server(sp -> {
            Mob husk = EntityType.HUSK.create(sp.level(), EntitySpawnReason.COMMAND);
            husk.setNoAi(true);
            husk.snapTo(base.getX() + 4.5, base.getY(), base.getZ() + 12.5, 180.0F, 0.0F);
            sp.level().addFreshEntity(husk);
        }), world("rastro", 0, 32, 5, "alvo", "mob"), 12);

        // Logística.
        scene("ima_de_itens", 30, () -> server(sp -> {
            Item[] loot = {Items.DIAMOND, Items.IRON_INGOT, Items.APPLE, Items.GOLD_INGOT, Items.EMERALD, Items.BREAD};
            for (int i = 0; i < loot.length; i++) {
                ItemEntity item = new ItemEntity(sp.level(), base.getX() + 0.5 + (i % 3 - 1) * 3, base.getY() + 0.2, base.getZ() + 4.5 + (i / 3) * 3,
                        new ItemStack(loot[i], 4));
                item.setNoPickUpDelay();
                sp.level().addFreshEntity(item);
            }
        }), world("ima_de_itens", 6, 10, 4), 8);
        scene("ajudante_de_trabalho", 30, () -> {
            fill(-3, -1, 2, 3, -1, 6, "minecraft:farmland");
            fill(-3, 0, 2, 3, 0, 6, "minecraft:wheat[age=7]");
        }, world("ajudante_de_trabalho", 0, 5, 30, "tarefa", "colher"), 30, 30);
        scene("levar_junto", 20, () -> animals(EntityType.SHEEP, 1.5, 1, -1.5, 1.5, 0, 2.5), world("levar_junto", 0, 4, 20, "quantos", 3), 4);
        String along = label("levar_junto_no_ar");
        step(1, () -> {
            server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() + 3, base.getZ() + 6.5));
            camera(CameraType.THIRD_PERSON_FRONT);
        });
        step(10, () -> clean(along));

        // The HUD: the function V would use now, the outline of the block aimed at, the traits and the wheel.
        reset(0);
        step(6, () -> {
            fill(0, 0, 3, 0, 4, 3, "minecraft:oak_log");
            fill(-1, 3, 2, 1, 4, 4, "minecraft:oak_leaves[persistent=true]");
            fill(2, 0, 3, 4, 2, 3, "minecraft:dirt");
            pick("prototipo", 0);
            camera(CameraType.FIRST_PERSON);
            look(0, 20);
        });
        String hudTree = label("hud_mira_madeira");
        step(20, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
        });
        step(10, () -> shot(hudTree));
        String hudDig = label("hud_mira_terra");
        step(4, () -> {
            pick("prototipo", 4);
            look(-35, 15);
        });
        step(20, () -> shot(hudDig));
        String wheel = label("roda_com_suprema");
        step(4, () -> mc().setScreen(new PowerWheelScreen(hero("prototipo"))));
        step(25, () -> shot(wheel));
        step(4, () -> mc().setScreen(null));
        step(4, () -> suit("prototipo_alado"));
        reset(0);
        String winged = label("hud_passivas_voo");
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 10);
            server(sp -> {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.teleportTo(base.getX() + 0.5, base.getY() + 3, base.getZ() + 0.5);
            });
            mc().options.hideGui = false;
        });
        step(20, () -> shot(winged));
        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"));
    }
}
