package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.cyborg.CyborgContent;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import com.danrod505.greenlantern.cyborg.CyborgPower;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Screenshot script "cyborg": Cyborg's suit (front, back, side), the HUD, every power, the power
 * wheel and the guide. Made by the hero kit: add the mobility and the epic moment as they are built.
 */
public final class CyborgScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("cyborg", CyborgScript::build, CyborgScript::log, false);

    private CyborgScript() {}

    private static void power(CyborgPower power) {
        server(sp -> {
            ItemStack item = CyborgHero.INSTANCE.findItem(sp);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = CyborgHero.INSTANCE.powers().use(sp, item, power.ordinal());
            GreenLantern.LOGGER.info("CLIENTSCRIPT power {} used={}", power.id(), used);
        });
    }

    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, CyborgContent.ITEM.get().charged(new ItemStack(CyborgContent.ITEM.get())));
                for (int i = -1; i <= 1; i++) {
                    // Husks: targets that don't burn in the sun.
                    var husk = EntityType.HUSK.create(sp.level(), EntitySpawnReason.COMMAND);
                    husk.snapTo(sp.getX() + i * 2.5, sp.getY(), sp.getZ() + 8.5, 180, 0);
                    husk.setNoAi(true);
                    husk.setPersistenceRequired();
                    sp.level().addFreshEntity(husk);
                }
                CyborgHero.INSTANCE.summonSuit(sp);
            });
            look(0, 5);
        });
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("cyb00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("cyb00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("cyb00c_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("cyb01_hud"));
        step(20, () -> power(CyborgPower.SONIC_CANNON));
        step(6, () -> shot("cyb02_sonic_cannon"));
        step(20, () -> power(CyborgPower.SHOULDER_MISSILES));
        step(6, () -> shot("cyb03_shoulder_missiles"));
        step(20, () -> power(CyborgPower.TECH_SCAN));
        step(6, () -> shot("cyb04_tech_scan"));
        step(20, () -> power(CyborgPower.MACHINE_HACK));
        step(6, () -> shot("cyb05_machine_hack"));
        step(20, () -> power(CyborgPower.EMP_BURST));
        step(6, () -> shot("cyb06_emp_burst"));
        step(20, () -> power(CyborgPower.SELF_REPAIR));
        step(6, () -> shot("cyb07_self_repair"));
        step(20, () -> power(CyborgPower.BOOM_TUBE));
        step(6, () -> shot("cyb08_boom_tube"));
        // Power wheel.
        step(20, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        });
        step(6, () -> shot("cyb09_power_wheel"));
        step(1, () -> {
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("cyborg")));
        step(10, () -> shot("cyb10_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("cyborg_powers")));
        step(10, () -> shot("cyb10b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        ItemStack item = CyborgHero.INSTANCE.findItem(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} energy={}", mc.player.tickCount, mc.player.blockPosition(),
                CyborgHero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : CyborgHero.ENERGY.get(item).stored());
    }
}
