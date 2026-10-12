package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityFunctions.Press;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.network.EnginePacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * The keys of a character of the engine: the power key (V) and the direct keys 1 to 8. An ability
 * with functions only for a tap goes off as soon as V goes down, as before. One with a function for
 * holding the key or for letting it go waits: a short press is a tap, holding it past
 * {@link AbilityFunctions#HOLD_TICKS} sets off the "segurar" function, and letting go after that the
 * "soltar" one. The HUD asks {@link #press()} to show the function that would go off now.
 */
public final class EngineKeys {
    /** Ticks V has been held for an ability that waits, -1 when it isn't. */
    private static int held = -1;
    private static boolean holdSent;

    private EngineKeys() {}

    public static void tick(Minecraft mc, EngineHero hero) {
        boolean free = mc.screen == null;
        while (KeyBindings.HERO_POWER.consumeClick()) {
            if (!free || held >= 0) continue;
            EngineAbility ability = selected(mc, hero);
            if (ability == null) continue;
            if (AbilityFunctions.usesHold(ability.data())) {
                held = 0;
                holdSent = false;
            } else {
                ModNetwork.sendToServer(EnginePacket.use(-1, Press.TAP));
            }
        }
        if (held >= 0) {
            EngineAbility ability = selected(mc, hero);
            if (ability == null || !free) {
                held = -1;
            } else if (KeyBindings.HERO_POWER.isDown()) {
                if (++held == AbilityFunctions.HOLD_TICKS && AbilityFunctions.has(ability.data(), Press.HOLD)) {
                    ModNetwork.sendToServer(EnginePacket.use(-1, Press.HOLD));
                    holdSent = true;
                }
            } else {
                if (held < AbilityFunctions.HOLD_TICKS) ModNetwork.sendToServer(EnginePacket.use(-1, Press.TAP));
                else if (AbilityFunctions.has(ability.data(), Press.RELEASE)) ModNetwork.sendToServer(EnginePacket.use(-1, Press.RELEASE));
                else if (!holdSent) ModNetwork.sendToServer(EnginePacket.use(-1, Press.TAP));
                held = -1;
            }
        }
        for (int i = 0; i < KeyBindings.ABILITIES.length; i++) {
            while (KeyBindings.ABILITIES[i].consumeClick()) {
                if (free && i < hero.powers().set().count()) ModNetwork.sendToServer(EnginePacket.use(i, Press.TAP));
            }
        }
        while (KeyBindings.SKIN.consumeClick()) {
            if (free) ModNetwork.sendToServer(EnginePacket.skin());
        }
    }

    /** How V is pressed right now, for the function the HUD shows. */
    public static Press press(EngineAbility ability) {
        if (held < AbilityFunctions.HOLD_TICKS) return Press.TAP;
        return AbilityFunctions.has(ability.data(), Press.RELEASE) ? Press.RELEASE : Press.HOLD;
    }

    /** Ticks V has been held (for the HUD's charge), -1 when it isn't. */
    public static int held() {
        return held;
    }

    private static EngineAbility selected(Minecraft mc, EngineHero hero) {
        if (mc.player == null) return null;
        ItemStack item = hero.findItem(mc.player);
        if (item.isEmpty()) return null;
        return hero.powers().power(hero.powers().selectedIndex(item)) instanceof EngineAbility a ? a : null;
    }
}
