package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;
import static com.danrod505.greenlantern.gametest.scripts.AquamanScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import java.util.ArrayList;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Screenshot script "atlantis": Atlantis in the deep ocean of the overworld. */
public final class AtlantisScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("atlantis", AtlantisScript::build, null, true);

    private AtlantisScript() {}

    public static com.danrod505.greenlantern.atlantis.Atlantis.Site atlantis;

    /** Teleports to a spot given relative to the center of Atlantis (y relative to the city floor). */
    public static void atTp(double dx, double dy, double dz, float yaw, float pitch) {
        if (atlantis == null) return;
        command(String.format(java.util.Locale.ROOT, "execute in minecraft:overworld run tp @a %.2f %.2f %.2f %.1f %.1f",
                atlantis.x() + 0.5 + dx, atlantis.floor() + dy, atlantis.z() + 0.5 + dz, yaw, pitch));
    }

    /** Teleports next to the nearest Atlantean (citizen or guard), looking at them. */
    public static void nearAtlantean(boolean guard) {
        var people = new java.util.ArrayList<>(mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlanteanEntity.class, mc().player.getBoundingBox().inflate(120)));
        people.removeIf(a -> a.isGuard() != guard);
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT atlanteans guard={} near={}", guard, people.size());
        if (people.isEmpty()) return;
        var a = people.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(mc().player))).get();
        double x = a.getX() + 2.6;
        double z = a.getZ() + 2.6;
        float yaw = (float) Math.toDegrees(Math.atan2(-(a.getX() - x), a.getZ() - z));
        command(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, a.getY() + 0.4, z, yaw, 8.0F));
    }

    /** Atlantis: the gate, the arrival pavilion, the respirator on a hero, the city, its people and the way home. */
    private static void build() {
        step(80, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            command("effect give @a night_vision infinite 0 true");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getInventory().setItem(9, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                LanternHero.INSTANCE.summonSuit(sp);
                sp.getInventory().setItem(10, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ATLANTIS_GATE.get()));
                atlantis = com.danrod505.greenlantern.atlantis.Atlantis.site(sp.level().getServer());
                com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT atlantis site={}", atlantis);
            });
            look(0, 8);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, ClientScript::use);
        step(25, () -> clean("a00_gate_portal"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("a00b_gate_portal_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().options.hideGui = false;
            key(mc().options.keyUp, true);
        });
        step(30, () -> key(mc().options.keyUp, false));
        // If the walk missed the opening (uneven ground), step right into it.
        step(20, () -> server(sp -> {
            if (atlantis == null || sp.position().distanceTo(atlantis.arrival()) < 3) return;
            var portals = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, sp.getBoundingBox().inflate(8));
            if (portals.isEmpty()) return;
            var portal = portals.getFirst();
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT walk missed the portal: player={} portal={}", sp.position(), portal.position());
            sp.teleportTo(portal.getX(), portal.getY(), portal.getZ());
        }));
        // The first trip builds the whole city at once: wait until the player stands in the pavilion.
        waitFor(1200, () -> atlantis != null && mc().player != null && mc().player.position().distanceTo(atlantis.arrival()) < 3);
        step(60, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT arrived dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.FIRST_PERSON);
            look(180, 2);
        });
        step(40, () -> clean("a01_pavilion"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("a01b_arrival_lantern"));
        // Out in the water: the respirator breathes for the Lantern (the mask stays on).
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = false;
            atTp(4.5, 1, 32, 160, 0);
        });
        step(60, () -> {
            mc().gui.getChat().clearMessages(false);
            shot("a02_respirator_hud");
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT underwater={} air={} respirator={}", mc().player.isUnderWater(), mc().player.getAirSupply(),
                    com.danrod505.greenlantern.aquaman.Respirator.air(com.danrod505.greenlantern.aquaman.Respirator.find(mc().player)));
        });
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(6, () -> clean("a03_respirator_face"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            nearAtlantean(false);
        });
        step(40, () -> clean("a04_citizen"));
        step(2, () -> nearAtlantean(true));
        step(20, () -> clean("a05_guard"));
        // Let the eyes get used to the deep (vanilla water vision takes 30 seconds underwater).
        step(2, () -> atTp(0, 2, 8, 180, 4));
        step(80, () -> clean("a06_throne_hall"));
        step(2, () -> atTp(-22, 6, 30, 200, 4));
        step(80, () -> clean("a07_statue"));
        step(2, () -> atTp(23, 4, 42, 180, 6));
        step(80, () -> clean("a08_garden"));
        step(2, () -> atTp(-18, 14, 50, 200, 14));
        step(200, () -> clean("a09_towers_houses"));
        step(2, () -> atTp(0, 10, 54, 180, -6));
        step(120, () -> clean("a10_palace"));
        step(2, () -> atTp(36, 34, 36, 135, 30));
        step(120, () -> clean("a11_city_aerial"));
        step(2, () -> atTp(-64, 30, -20, 290, 18));
        step(120, () -> clean("a12_kelp_slopes"));
        step(2, () -> atTp(24, 60, 44, 150, 30));
        step(120, () -> clean("a13_beacon_from_surface"));
        // Aquaman opens the way home with his own power.
        step(2, () -> {
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.getInventory().setItem(11, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                AquamanHero.INSTANCE.summonSuit(sp);
            });
            atTp(0, 0, 40, 180, 6);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.ATLANTIS_PORTAL);
        });
        step(25, () -> clean("a14_aquaman_portal_home"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                // Swim into the whirlpool.
                var portals = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, sp.getBoundingBox().inflate(8));
                if (!portals.isEmpty()) sp.teleportTo(portals.getFirst().getX(), portals.getFirst().getY() + 0.2, portals.getFirst().getZ());
            });
        });
        step(80, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT back home dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, () -> clean("a15_back_home"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("atlantis"));
        });
        step(10, () -> shot("a16_guide"));
        step(20, () -> mc().stop());
    }
}
