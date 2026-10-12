package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.engine.CharacterHero;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.network.CycleConstructPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SelectPowerPacket;
import com.danrod505.greenlantern.network.ToggleUniformPacket;
import com.danrod505.greenlantern.network.UsePowerPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;

/** Client tick / input handling: key bindings, scroll selection and the flight aura. */
public final class ClientEvents {
    private ClientEvents() {}

    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CameraShake.tick();
        com.danrod505.greenlantern.client.engine.EngineCamera.tick();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        while (KeyBindings.TOGGLE_UNIFORM.consumeClick()) {
            if (HeroRegistry.context(player).isPresent()) ModNetwork.sendToServer(new ToggleUniformPacket());
        }
        HeroDefinition context = HeroRegistry.context(player).orElse(null);
        for (HeroDefinition hero : HeroRegistry.all()) {
            // The 9 heroes' HUDs draw the icons of their new wheel even when another hero is in hand.
            if (hero instanceof com.danrod505.greenlantern.engine.LegacyHero legacy) com.danrod505.greenlantern.client.engine.EngineIcons.ensure(legacy);
        }
        if (context instanceof CharacterHero character) com.danrod505.greenlantern.client.engine.EngineIcons.ensure(character);
        if (context instanceof CharacterHero character) {
            // Characters in the new model (the engine's and the 9 heroes): the function depends on how V is pressed (see EngineKeys).
            com.danrod505.greenlantern.client.engine.EngineKeys.tick(mc, character);
        } else {
            while (KeyBindings.HERO_POWER.consumeClick()) {
                if (mc.screen != null || context == null || !context.hasPowers()) continue;
                ModNetwork.sendToServer(new UsePowerPacket(-1));
            }
            // The skin and the direct keys only act for characters in the new model.
            while (KeyBindings.SKIN.consumeClick()) {
            }
            for (var key : KeyBindings.ABILITIES) {
                while (key.consumeClick()) {
                }
            }
        }
        tickWheelKey(mc, player);
        for (HeroClient hero : ClientSetup.heroes()) hero.controlsTick(mc);

        if (mc.isPaused()) return;
        // Green aura trail behind every flying Lantern in view.
        for (Player other : mc.level.players()) {
            if (!RingHelper.isSuited(other)) continue;
            boolean flying = other.getAbilities().flying || (!other.onGround() && other.getDeltaMovement().y < -0.1 && other != player);
            if (other == player && mc.options.getCameraType().isFirstPerson() && !flying) continue;
            int count = flying ? 2 : (other.tickCount % 6 == 0 ? 1 : 0);
            for (int i = 0; i < count; i++) {
                double x = other.getX() + (other.getRandom().nextDouble() - 0.5) * 0.5;
                double y = other.getY() + other.getRandom().nextDouble() * (flying ? 0.6 : 1.8);
                double z = other.getZ() + (other.getRandom().nextDouble() - 0.5) * 0.5;
                mc.level.addParticle(ModParticles.GLOW.get(), x, y, z, 0, flying ? -0.02 : 0.01, 0);
            }
        }
    }

    /** Ticks the wheel key has been held (0 when released). */
    private static int wheelKeyTicks;
    /** Hold this long (ticks) to open the wheel; a shorter tap switches to the next construct. */
    private static final int WHEEL_HOLD_TICKS = 4;

    private static void tickWheelKey(Minecraft mc, LocalPlayer player) {
        // Clicks are handled through isDown(); drop queued clicks so they don't pile up.
        while (KeyBindings.CONSTRUCT_WHEEL.consumeClick()) {
        }
        HeroDefinition hero = HeroRegistry.context(player).orElse(null);
        if (mc.screen != null || hero == null) {
            wheelKeyTicks = 0;
            return;
        }
        boolean powers = hero.hasPowers();
        if (KeyBindings.CONSTRUCT_WHEEL.isDown()) {
            if (++wheelKeyTicks == WHEEL_HOLD_TICKS) {
                mc.setScreen(powers ? new PowerWheelScreen(hero) : new ConstructWheelScreen());
            }
        } else {
            if (wheelKeyTicks > 0 && wheelKeyTicks < WHEEL_HOLD_TICKS) {
                ModNetwork.sendToServer(powers ? new SelectPowerPacket(1, true) : new CycleConstructPacket(1));
            }
            wheelKeyTicks = 0;
        }
    }

    /** Sneak + mouse wheel cycles constructs (or powers) while holding a hero item (ring, emblem, belt...). */
    public static boolean onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !player.isShiftKeyDown()) return false;
        boolean powers = false;
        boolean constructs = false;
        for (HeroDefinition hero : HeroRegistry.all()) {
            if (hero.heldItem(player).isEmpty()) continue;
            if (hero.hasPowers()) powers = true;
            else constructs = true;
        }
        if (!powers && !constructs) return false;
        double delta = event.getDeltaY();
        if (delta == 0) return false;
        ModNetwork.sendToServer(powers ? new SelectPowerPacket(delta > 0 ? -1 : 1, true) : new CycleConstructPacket(delta > 0 ? -1 : 1));
        return true;
    }
}
