package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityHero;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityState;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.OwnPowers;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.function.BiPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The row of abilities of a hero in the new model, the same on every HUD: the 5 to 8 abilities with
 * their cost and a ring that fills during the cooldown (the supreme one with a gold border), the icon
 * of the function that goes off now (it changes with the aim, the place and holding the key), and
 * under it the key line. The heroes of before keep the rest of their own HUD around it.
 */
public final class AbilityRow {
    private static final int RING_SEGMENTS = 24;
    private static final int GOLD = 0xFF000000 | AbilityIcons.GOLD;

    private AbilityRow() {}

    /**
     * Draws the icons at {@code x, y} and the key line under them, and returns the height used.
     *
     * @param selectedColor colour (ARGB) behind the chosen ability
     * @param lineColor     colour (ARGB) of the key line and the cooldown rings
     * @param running       which powers of before are running now (a line under their ability), or null
     */
    public static int render(GuiGraphics graphics, AbilityHero hero, ItemStack item, Player player, int x, int y, int selectedColor, int lineColor,
                             BiPredicate<Player, String> running) {
        Minecraft mc = Minecraft.getInstance();
        CharacterData data = hero.data();
        HeroPowers<EngineAbility> powers = hero.powers();
        WheelStyle wheel = powers.wheel();
        AbilityState state = EngineServer.state(item);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        int selected = powers.selectedIndex(item);
        int count = powers.set().count();
        for (int i = 0; i < count; i++) {
            EngineAbility ability = (EngineAbility) powers.power(i);
            int ix = x + i * 20;
            boolean isSelected = i == selected;
            long left = state.remaining(i, now);
            boolean on = running != null && running(ability, player, running);
            if (isSelected) graphics.fill(ix - 1, y - 1, ix + 17, y + 17, selectedColor);
            if (on) graphics.fill(ix - 1, y + 17, ix + 17, y + 19, lineColor);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), ix, y, ability.iconIndex() * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, left > 0 ? 0x70FFFFFF : isSelected || on ? 0xFFFFFFFF : 0xB0FFFFFF);
            if (left > 0) cooldownRing(graphics, ix + 8, y + 8, 1.0F - (float) left / ability.cooldownTicks(), lineColor);
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
        graphics.drawString(mc.font, "›", fx, y + 4, lineColor, true);
        int cell = AbilityIcons.functionCell(data, selected, function.index());
        graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), fx + 7, y, cell * 16, 0, 16, 16, wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT, 0xFFFFFFFF);
        int held = EngineKeys.held();
        if (held > 0 && AbilityFunctions.usesHold(chosen.data())) {
            int w = Math.round(16 * Math.min(1.0F, (float) held / AbilityFunctions.HOLD_TICKS));
            graphics.fill(fx + 7, y + 17, fx + 7 + w, y + 19, held >= AbilityFunctions.HOLD_TICKS ? GOLD : lineColor);
        }
        Component keys = Component.translatable("hud.greenlantern.power_key", chosen.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage());
        graphics.drawString(mc.font, keys, x, y + 21, chosen.supreme() ? GOLD : lineColor, true);
        return 21 + 11;
    }

    /** Whether one of the powers of before the functions of the ability run is going on now. */
    private static boolean running(EngineAbility ability, Player player, BiPredicate<Player, String> running) {
        for (CharacterData.Function function : ability.data().allFunctions()) {
            String power = OwnPowers.power(function, ability.data());
            if (power != null && running.test(player, power)) return true;
        }
        return false;
    }

    /** A ring of dots around the icon that fills clockwise from the top as the cooldown runs out. */
    static void cooldownRing(GuiGraphics graphics, int cx, int cy, float progress, int color) {
        int lit = Math.round(RING_SEGMENTS * Mth.clamp(progress, 0.0F, 1.0F));
        for (int s = 0; s < RING_SEGMENTS; s++) {
            float angle = (float) (s * Math.PI * 2 / RING_SEGMENTS - Math.PI / 2);
            int px = cx + Math.round(Mth.cos(angle) * 7.0F);
            int py = cy + Math.round(Mth.sin(angle) * 7.0F);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, s < lit ? color : 0x80000000);
        }
    }
}
