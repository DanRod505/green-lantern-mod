package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import java.util.ArrayList;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Screenshot script "oa": The portal to Oa and the planet of the Guardians. */
public final class OaScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("oa", OaScript::build, null, false);

    private OaScript() {}

    public static void oaTp(double x, double y, double z, float yaw, float pitch) {
        command(String.format(java.util.Locale.ROOT, "execute in greenlantern:oa run tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    /** Portal construct and the planet Oa: the trip there, the city from several angles and the trip back. */
    private static void build() {
        int g = com.danrod505.greenlantern.oa.Oa.GROUND_Y;
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                LanternHero.INSTANCE.summonSuit(sp);
                ConstructRegistry.select(sp.getMainHandItem(), ConstructRegistry.PORTAL);
            });
            look(0, 8);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, ClientScript::use);
        step(6, () -> clean("o00_portal_opening"));
        step(20, () -> clean("o01_portal"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("o01b_portal_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Walk through the portal.
        step(2, () -> key(mc().options.keyUp, true));
        step(30, () -> key(mc().options.keyUp, false));
        step(100, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT oa dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.FIRST_PERSON);
            look(180, -12);
        });
        step(30, () -> clean("o02_arrival"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(10, () -> clean("o02b_arrival_lantern"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            oaTp(0.5, g + 38, 78.5, 180, 22);
        });
        step(120, () -> clean("o03_city_aerial"));
        step(2, () -> oaTp(0.5, g + 14, 26.5, 180, -8));
        step(80, () -> clean("o04_battery"));
        step(2, () -> oaTp(18.5, g + 4, 18.5, 135, -32));
        step(80, () -> clean("o05_battery_beam"));
        step(2, () -> {
            net.minecraft.core.BlockPos top = com.danrod505.greenlantern.oa.OaCity.pillarTop(0);
            double dx = -top.getX(), dz = -top.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            double x = top.getX() + 0.5 + dx / len * 4.5;
            double z = top.getZ() + 0.5 + dz / len * 4.5;
            float yaw = (float) Math.toDegrees(Math.atan2(-(top.getX() + 0.5 - x), top.getZ() + 0.5 - z));
            oaTp(x, top.getY() - 0.5, z, yaw, 8);
        });
        step(80, () -> clean("o06_guardian"));
        step(2, () -> {
            net.minecraft.core.BlockPos top = com.danrod505.greenlantern.oa.OaCity.pillarTop(2);
            oaTp(top.getX() * 0.55 + 0.5, top.getY() + 3, top.getZ() * 0.55 + 0.5, (float) Math.toDegrees(Math.atan2(-top.getX(), top.getZ())) + 20, 10);
        });
        step(80, () -> clean("o07_guardian_ring"));
        step(2, () -> {
            server(sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            });
            oaTp(30.5, g, 0.5, 90, -4);
        });
        step(80, () -> {
            var lanterns = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, mc().player.getBoundingBox().inflate(80));
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT lanterns near={}", lanterns.size());
            if (!lanterns.isEmpty()) {
                var walker = lanterns.stream().filter(l -> !l.isFlyingLantern()).min(java.util.Comparator.comparingDouble(l -> l.distanceToSqr(mc().player))).orElse(lanterns.getFirst());
                oaTp(walker.getX() + 2.5, g, walker.getZ() + 2.5, 135, 10);
            }
        });
        step(20, () -> clean("o08_lanterns"));
        step(2, () -> {
            server(sp -> {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            var flyers = new java.util.ArrayList<>(mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, mc().player.getBoundingBox().inflate(120)));
            flyers.removeIf(l -> !l.isFlyingLantern());
            if (!flyers.isEmpty()) {
                var f = flyers.getFirst();
                oaTp(f.getX() * 1.35, f.getY() + 1.5, f.getZ() * 1.35, (float) Math.toDegrees(Math.atan2(f.getX(), -f.getZ())), 15);
            }
        });
        step(30, () -> clean("o09_flying_lanterns"));
        step(2, () -> oaTp(70.5, g + 50, -40.5, 120, 20));
        step(100, () -> clean("o10_skyline"));
        step(2, () -> oaTp(0.5, g, 30.5, 180, 5));
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            });
            use();
        });
        step(25, () -> clean("o11_portal_home"));
        step(2, () -> key(mc().options.keyUp, true));
        step(30, () -> key(mc().options.keyUp, false));
        step(80, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT back dimension={} pos={}", mc().level.dimension(), mc().player.position());
            clean("o12_back_home");
        });
        step(20, () -> mc().stop());
    }
}
