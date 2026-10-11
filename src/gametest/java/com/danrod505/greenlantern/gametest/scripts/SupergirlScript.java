package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.supergirl.SupergirlContent;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlPower;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Screenshot script "supergirl": Supergirl's suit (front, back, side), the HUD, every power, the power
 * wheel and the guide. Made by the hero kit: add the mobility and the epic moment as they are built.
 */
public final class SupergirlScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("supergirl", SupergirlScript::build, SupergirlScript::log, false);

    private SupergirlScript() {}

    private static void power(SupergirlPower power) {
        server(sp -> {
            ItemStack item = SupergirlHero.INSTANCE.findItem(sp);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = SupergirlHero.INSTANCE.powers().use(sp, item, power.ordinal());
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
                sp.setItemInHand(InteractionHand.MAIN_HAND, SupergirlContent.ITEM.get().charged(new ItemStack(SupergirlContent.ITEM.get())));
                for (int i = -1; i <= 1; i++) {
                    // Husks: targets that don't burn in the sun.
                    var husk = EntityType.HUSK.create(sp.level(), EntitySpawnReason.COMMAND);
                    husk.snapTo(sp.getX() + i * 2.5, sp.getY(), sp.getZ() + 8.5, 180, 0);
                    husk.setNoAi(true);
                    husk.setPersistenceRequired();
                    sp.level().addFreshEntity(husk);
                }
                SupergirlHero.INSTANCE.summonSuit(sp);
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
        step(10, () -> clean("sup00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("sup00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("sup00c_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("sup01_hud"));
        step(20, () -> power(SupergirlPower.HEAT_BOLTS));
        step(6, () -> shot("sup02_heat_bolts"));
        step(20, () -> power(SupergirlPower.METEOR_DASH));
        step(6, () -> shot("sup03_meteor_dash"));
        step(20, () -> power(SupergirlPower.THUNDER_CLAP));
        step(6, () -> shot("sup04_thunder_clap"));
        step(20, () -> power(SupergirlPower.FROST_WALL));
        step(6, () -> shot("sup05_frost_wall"));
        step(20, () -> power(SupergirlPower.SUPER_HEARING));
        step(6, () -> shot("sup06_super_hearing"));
        step(20, () -> power(SupergirlPower.KRYPTONIAN_THROW));
        step(6, () -> shot("sup07_kryptonian_throw"));
        step(20, () -> power(SupergirlPower.SOLAR_FLARE));
        step(6, () -> shot("sup08_solar_flare"));
        // Power wheel.
        step(20, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        });
        step(6, () -> shot("sup09_power_wheel"));
        step(1, () -> {
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("supergirl")));
        step(10, () -> shot("sup10_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("supergirl_powers")));
        step(10, () -> shot("sup10b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        ItemStack item = SupergirlHero.INSTANCE.findItem(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} energy={}", mc.player.tickCount, mc.player.blockPosition(),
                SupergirlHero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : SupergirlHero.ENERGY.get(item).stored());
    }
}
