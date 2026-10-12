package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityState;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Traits;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The abilities of the wheel of the new model on the HUD, the same for every hero: the 5 to 8
 * abilities with their cost and a ring that fills during the cooldown (the supreme one with a gold
 * border), the icon of the function that goes off now (it changes with the aim, the place and
 * holding the key), the traits (lit while they are on) and the key of the chosen one. The characters
 * of the engine draw it under their energy bar, and so do the first 9 heroes under theirs.
 */
public final class AbilityRow {
    private static final int RING_SEGMENTS = 24;
    private static final int GOLD = 0xFF000000 | AbilityIcons.GOLD;

    private AbilityRow() {}

    /**
     * Draws the row with its first icon at ({@code x}, {@code y}). {@code active} tells which
     * abilities are on now (a beam, a toggle of the hero's own code): a line under them.
     */
    public static void render(GuiGraphics graphics, Minecraft mc, HeroDefinition hero, ItemStack item, int x, int y, int light, int second,
                              boolean suited, IntPredicate active) {
        EngineIcons.ensure(hero);
        Player player = mc.player;
        if (player == null || mc.level == null || !hero.abilityWheel()) return;
        CharacterData data = hero.data();
        HeroPowers<?> powers = hero.powers();
        WheelStyle wheel = powers.wheel();
        AbilityState state = EngineServer.state(item);
        long now = mc.level.getGameTime();
        int selected = powers.selectedIndex(item);
        int count = powers.set().count();
        float ownCooldown = player.getCooldowns().getCooldownPercent(item, 0.0F);

        for (int i = 0; i < count; i++) {
            EngineAbility ability = (EngineAbility) powers.power(i);
            int ix = x + i * 20;
            boolean isSelected = i == selected;
            long left = state.remaining(i, now);
            float progress = left > 0 ? 1.0F - (float) left / ability.cooldownTicks() : -1.0F;
            // The hero's own powers wait on the item's cooldown, shared by all of them.
            if (progress < 0 && ability.own() != null && ownCooldown > 0) progress = 1.0F - ownCooldown;
            if (isSelected) graphics.fill(ix - 1, y - 1, ix + 17, y + 17, second);
            if (active.test(i)) graphics.fill(ix - 1, y + 17, ix + 17, y + 19, light);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), ix, y, ability.iconIndex() * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, progress >= 0 ? 0x70FFFFFF : isSelected ? 0xFFFFFFFF : 0xB0FFFFFF);
            if (progress >= 0) cooldownRing(graphics, ix + 8, y + 8, progress, light);
            String cost = String.valueOf(ability.cost());
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix + 16 - mc.font.width(cost) * 0.5F, y + 12);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.drawString(mc.font, cost, 0, 0, ability.supreme() ? GOLD : 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
        // The function that goes off now: an arrow and its icon, a bar filling while the key is held.
        EngineAbility chosen = (EngineAbility) powers.power(selected);
        AbilityFunctions.Chosen function = AbilityFunctions.choose(chosen.data(), EngineKeys.press(chosen), player);
        int fx = x + count * 20 + 2;
        graphics.drawString(mc.font, "›", fx, y + 4, light, true);
        int cell = AbilityIcons.functionCell(data, selected, function.index());
        graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), fx + 7, y, cell * 16, 0, 16, 16, wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT, 0xFFFFFFFF);
        int held = EngineKeys.held();
        if (held > 0 && AbilityFunctions.usesHold(chosen.data())) {
            int w = Math.round(16 * Math.min(1.0F, (float) held / AbilityFunctions.HOLD_TICKS));
            graphics.fill(fx + 7, y + 17, fx + 7 + w, y + 19, held >= AbilityFunctions.HOLD_TICKS ? GOLD : light);
        }

        // The traits: small icons, lit while they are on.
        int traitsY = y + 20;
        for (int i = 0; i < data.traits().size(); i++) {
            boolean on = suited && Traits.on(data.traits().get(i), player);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + i * 12, traitsY);
            graphics.pose().scale(0.625F, 0.625F);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), 0, 0, AbilityIcons.traitCell(data, i) * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, on ? 0xFFFFFFFF : 0x50FFFFFF);
            graphics.pose().popMatrix();
        }

        int keysY = keysY(hero, y);
        Component keys = Component.translatable("hud.greenlantern.power_key", chosen.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage());
        graphics.drawString(mc.font, keys, x, keysY, chosen.supreme() ? GOLD : light, true);
        if (suited && AbilityFunctions.usesHold(chosen.data())) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_hold_hint", KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                    x, keysY + 11, 0xFFA0A0A0, true);
        }
    }

    /** Which abilities are on, from whether their own power is on (a beam, a toggle of the hero's own code). */
    public static IntPredicate ownActive(HeroDefinition hero, java.util.function.Predicate<com.danrod505.greenlantern.hero.HeroPower> on) {
        return i -> hero.powers().power(i) instanceof EngineAbility ability && ability.own() != null && on.test(ability.own());
    }

    /** The line of the key of the chosen ability, for a row whose icons start at {@code y} (a hint goes 11 below it). */
    public static int keysY(HeroDefinition hero, int y) {
        return y + 20 + (hero.data().traits().isEmpty() ? 0 : 12);
    }

    /** How tall the row is, from its icons to the line under the key (with a hint). */
    public static int height(HeroDefinition hero) {
        return keysY(hero, 0) + 22;
    }

    /** A ring of dots around the icon that fills clockwise from the top as the cooldown runs out. */
    private static void cooldownRing(GuiGraphics graphics, int cx, int cy, float progress, int color) {
        int lit = Math.round(RING_SEGMENTS * Mth.clamp(progress, 0.0F, 1.0F));
        for (int s = 0; s < RING_SEGMENTS; s++) {
            float angle = (float) (s * Math.PI * 2 / RING_SEGMENTS - Math.PI / 2);
            int px = cx + Math.round(Mth.cos(angle) * 7.0F);
            int py = cy + Math.round(Mth.sin(angle) * 7.0F);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, s < lit ? color : 0x80000000);
        }
    }
}
