package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.network.CycleConstructPacket;
import com.danrod505.greenlantern.network.EnginePacket;
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
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        while (KeyBindings.TOGGLE_UNIFORM.consumeClick()) {
            if (HeroRegistry.context(player).isPresent()) ModNetwork.sendToServer(new ToggleUniformPacket());
        }
        HeroDefinition context = HeroRegistry.context(player).orElse(null);
        while (KeyBindings.HERO_POWER.consumeClick()) {
            if (mc.screen != null || context == null || !context.hasPowers()) continue;
            // Engine characters: crouch + V is the ability's use in the world.
            if (context instanceof EngineHero && player.isShiftKeyDown()) ModNetwork.sendToServer(new EnginePacket(EnginePacket.WORLD_USE));
            else ModNetwork.sendToServer(new UsePowerPacket(-1));
        }
        tickEngineKeys(mc, context);
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

    /** B (Supreme), K (skin) and the direct ability keys only act for engine characters. */
    private static void tickEngineKeys(Minecraft mc, HeroDefinition context) {
        boolean engine = mc.screen == null && context instanceof EngineHero;
        while (KeyBindings.SUPREME.consumeClick()) {
            if (engine) ModNetwork.sendToServer(new EnginePacket(EnginePacket.SUPREME));
        }
        while (KeyBindings.SKIN.consumeClick()) {
            if (engine) ModNetwork.sendToServer(new EnginePacket(EnginePacket.SKIN));
        }
        for (int i = 0; i < KeyBindings.ABILITIES.length; i++) {
            while (KeyBindings.ABILITIES[i].consumeClick()) {
                if (engine && i < context.powers().set().count()) ModNetwork.sendToServer(new UsePowerPacket(i));
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
