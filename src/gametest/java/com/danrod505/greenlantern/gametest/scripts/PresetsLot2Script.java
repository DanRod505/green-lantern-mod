package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "presets_lote2" (stage 2, batch 2): the test character Protótipo uses each
 * preset of Movimento, Defesa and Evasão (the Protótipo Alado flies at the end). The abilities are
 * made on the spot with the numbers and settings a character file would give.
 */
public final class PresetsLot2Script {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("presets_lote2", PresetsLot2Script::build, PresetsLot2Script::log, false);

    private static BlockPos base = BlockPos.ZERO;
    private static int number = 0;
    /** Where the next use aims, on the server only: the camera keeps looking down from behind. */
    private static float aimPitch = 0;

    private PresetsLot2Script() {}

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

    /** A fill command relative to the base. */
    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        command(String.format("fill %d %d %d %d %d %d %s", base.getX() + x1, base.getY() + y1, base.getZ() + z1,
                base.getX() + x2, base.getY() + y2, base.getZ() + z2, block));
    }

    /** Ends what is still on, clears the stage and puts the player back at the base, looking south. */
    private static void reset(float pitch) {
        step(4, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            command("kill @e[type=!minecraft:player]");
            server(sp -> {
                PresetRuntime.stopLasting(sp);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
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
        return String.format("pq%02d_%s", ++number, name);
    }

    /** One preset: reset, targets, use, and a screenshot after each of {@code delays} (ticks after the previous one). */
    private static void demo(String name, EngineAbility ability, float pitch, double[] offsets, int... delays) {
        String label = label(name);
        reset(pitch);
        step(6, () -> targets(offsets));
        step(12, () -> use(ability));
        shots(label, delays);
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
                base = sp.blockPosition();
            });
            suit("prototipo");
            mc().options.hideGui = true;
            mc().gui.getChat().clearMessages(false);
        });

        // ---- Movimento ----
        // Super speed: the run with its trail, holding forward and sprint.
        String run = label("supervelocidade");
        reset(0);
        step(6, () -> use(ability("supervelocidade", AbilityKind.MOVEMENT, 0, 0, 0, 6, "velocidade", 2.5)));
        // The script can't hold keys: the server carries the runner forward, as running would.
        for (int i = 0; i < 18; i++) {
            step(1, () -> server(sp -> com.danrod505.greenlantern.engine.preset.PresetBody.push(sp, new Vec3(0, sp.getDeltaMovement().y, 1.1))));
            if (i == 9) step(0, () -> clean(run + "_a"));
        }
        step(1, () -> clean(run + "_b"));
        // Glide: from high up, the cape carries the character forward.
        String glide = label("planar");
        reset(0);
        step(6, () -> server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() + 14, base.getZ() + 0.5)));
        step(4, () -> use(ability("planar", AbilityKind.MOVEMENT, 0, 0, 0, 10)));
        shots(glide, 12, 16);
        // Giant leap: up high, then the landing among the husks.
        String leap = label("salto_gigante");
        reset(0);
        step(6, () -> targets(-2, 10, 2, 10, 0, 12));
        step(10, () -> use(ability("salto_gigante", AbilityKind.MOVEMENT, 3, 0, 3.5, 0, "altura", 7, "distancia", 10)));
        step(12, () -> clean(leap + "_a_up"));
        watch(40, () -> mc().player.onGround(), leap + "_b_land");
        step(3, () -> clean(leap + "_c_after"));
        // Grapple: to a pillar ahead.
        String hook = label("gancho");
        reset(-8);
        step(4, () -> fill(-1, 0, 12, 1, 6, 12, "minecraft:stone_bricks"));
        step(10, () -> use(ability("gancho", AbilityKind.MOVEMENT, 0, 24, 0, 0, "velocidade", 1.0)));
        shots(hook, 4, 8);
        step(10, () -> fill(-1, 0, 12, 1, 6, 12, "minecraft:air"));
        // Teleport: a boom tube, then a shadow.
        demo("teleporte_tubo", ability("teleporte", AbilityKind.MOVEMENT, 0, 16, 0, 0, "visual", "tubo", "tempo", 0.5), 12,
                new double[]{}, 3, 9);
        demo("teleporte_sombra", ability("teleporte", AbilityKind.MOVEMENT, 0, 16, 0, 0, "visual", "sombra", "tempo", 0.5), 12,
                new double[]{}, 3, 9);
        // Pass through a wall 2 blocks thick.
        String phase = label("atravessar");
        reset(0);
        step(4, () -> fill(-2, 0, 2, 2, 2, 3, "minecraft:stone_bricks"));
        step(10, () -> use(ability("atravessar", AbilityKind.MOVEMENT, 0, 0, 0, 0.6, "espessura", 3)));
        shots(phase + "_a", 5);
        // From the front: the character on the other side of the wall.
        step(8, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(3, () -> clean(phase + "_b_behind"));
        step(2, () -> fill(-2, 0, 2, 2, 2, 3, "minecraft:air"));
        // Fast swim: in a pool dug ahead.
        String swim = label("nado_rapido");
        reset(10);
        step(4, () -> fill(-3, -3, 1, 3, -1, 14, "minecraft:water"));
        step(10, () -> server(sp -> sp.teleportTo(base.getX() + 0.5, base.getY() - 2, base.getZ() + 2.5)));
        step(6, () -> use(ability("nado_rapido", AbilityKind.MOVEMENT, 0, 0, 0, 5, "velocidade", 1.5)));
        shots(swim, 3, 6);
        step(10, () -> {
            fill(-3, -3, 1, 3, -2, 14, "minecraft:dirt");
            fill(-3, -1, 1, 3, -1, 14, "minecraft:grass_block");
        });

        // ---- Defesa ----
        demo("bolha", ability("bolha", AbilityKind.DEFENSE, 0, 0, 1.8, 10), 30, new double[]{-2.5, 1.5, 2.5, 1.5}, 8, 6);
        demo("bloqueio", ability("bloqueio", AbilityKind.DEFENSE, 0, 0, 0, 2, "angulo", 120), 20, new double[]{0, 2.5}, 4);
        demo("pele_dura", ability("pele_dura", AbilityKind.DEFENSE, 0, 0, 0, 4), 25, new double[]{}, 2, 16);
        demo("parede_energia", ability("parede", AbilityKind.DEFENSE, 0, 0, 0, 1, "largura", 5, "altura", 3), 15, new double[]{0, 7}, 4);
        demo("parede_gelo", ability("parede", AbilityKind.DEFENSE, 0, 0, 0, 1, "material", "gelo", "largura", 5, "altura", 3), 15,
                new double[]{0, 7}, 4);
        // Reflection: an arrow from a skeleton ahead comes back to it.
        String mirror = label("refletir");
        reset(10);
        step(6, () -> server(sp -> {
            Skeleton skeleton = EntityType.SKELETON.create(sp.level(), EntitySpawnReason.COMMAND);
            if (skeleton == null) return;
            skeleton.setNoAi(true);
            skeleton.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 10.5, 180.0F, 0.0F);
            skeleton.setYHeadRot(180.0F);
            skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            sp.level().addFreshEntity(skeleton);
        }));
        step(10, () -> use(ability("refletir", AbilityKind.DEFENSE, 0, 0, 0, 3)));
        step(2, () -> server(sp -> {
            Skeleton skeleton = sp.level().getEntitiesOfClass(Skeleton.class, sp.getBoundingBox().inflate(16)).stream().findFirst().orElse(null);
            Arrow arrow = new Arrow(sp.level(), base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 9.0, new ItemStack(Items.ARROW), null);
            arrow.setOwner(skeleton);
            arrow.shoot(0, 0.02, -1, 1.2F, 0.0F);
            sp.level().addFreshEntity(arrow);
        }));
        shots(mirror, 5, 3, 4);

        // ---- Evasão ----
        demo("esquiva", ability("esquiva", AbilityKind.EVASION, 0, 4, 0, 0), 20, new double[]{0.5, 2.5}, 2, 6);
        demo("invisivel", ability("invisivel", AbilityKind.EVASION, 0, 0, 0, 5), 20, new double[]{0, 4}, 3, 10);
        demo("fumaca", ability("fumaca", AbilityKind.EVASION, 0, 0, 4, 4), 30, new double[]{-2, 2, 2, 2.5}, 8, 20);
        step(60, () -> {});
        demo("encolher", ability("encolher", AbilityKind.EVASION, 0, 0, 0, 10), 30, new double[]{1.5, 3}, 22);
        demo("fuga_sombra", ability("fuga_de_forma", AbilityKind.EVASION, 0, 0, 0, 3, "forma", "sombra"), 30, new double[]{}, 10);
        step(40, () -> {});
        demo("fuga_animal", ability("fuga_de_forma", AbilityKind.EVASION, 0, 0, 0, 3, "forma", "animal"), 30, new double[]{1.5, 3}, 24);
        step(40, () -> {});
        demo("fuga_poca", ability("fuga_de_forma", AbilityKind.EVASION, 0, 0, 0, 3, "forma", "poca"), 30, new double[]{1.5, 3}, 24);

        // ---- Voo, on the Protótipo Alado, whose file names it ----
        String fly = label("voo");
        reset(0);
        step(4, () -> suit("prototipo_alado"));
        step(20, () -> server(sp -> {
            EngineHero winged = (EngineHero) HeroRegistry.byId("prototipo_alado").orElseThrow();
            EngineAbility flight = (EngineAbility) winged.powers().power(3);
            boolean went = EngineServer.useAbility(sp, sp.getMainHandItem(), flight);
            GreenLantern.LOGGER.info("CLIENTSCRIPT preset voo went={} flying={}", went, sp.getAbilities().flying);
        }));
        shots(fly, 6, 14);
        step(10, () -> key(mc().options.keyUp, true));
        step(25, () -> clean(fly + "_c_forward"));
        step(4, () -> key(mc().options.keyUp, false));

        step(20, () -> mc().stop());
    }

    private static void log(Minecraft mc) {
        var suited = HeroRegistry.suited(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} flying={}", mc.player.tickCount, mc.player.blockPosition(),
                suited.map(HeroDefinition::id).orElse("-"), mc.player.getAbilities().flying);
    }
}
