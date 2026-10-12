package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.client.engine.EngineKeys;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityState;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.LegacyHero;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.function.BiPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The row of abilities on the HUD of one of the 9 heroes, in the new model: the icons of the wheel
 * of the catalog (the chosen one framed, the running powers underlined, the ones cooling down
 * dimmed), and under them the name of the chosen one with the power key, and the icon of the
 * function that goes off now when it is not the tap (the crosshair on a tree, holding the key...).
 */
public final class LegacyRow {
    private LegacyRow() {}

    /**
     * Draws the row at ({@code x}, {@code y}), 16 pixels high, and the line under it at {@code lineY}.
     *
     * @param frame   colour (ARGB) of the frame of the chosen ability
     * @param running colour (ARGB) of the line under a power that is on
     * @param active  whether a power of the hero's own code is on now (heat vision, the scan...)
     * @param text    colour (ARGB) of the line
     */
    public static void draw(GuiGraphics graphics, LegacyHero hero, Player player, ItemStack item, int x, int y, int lineY, int frame, int running,
                            BiPredicate<Player, HeroPower> active, int text) {
        Minecraft mc = Minecraft.getInstance();
        HeroPowers<EngineAbility> powers = hero.powers();
        WheelStyle wheel = powers.wheel();
        int selected = powers.selectedIndex(item);
        AbilityState state = item.get(EngineContent.ABILITIES.get());
        long now = player.level().getGameTime();
        for (int i = 0; i < powers.set().count(); i++) {
            EngineAbility ability = powers.set().byIndex(i);
            int ix = x + i * 20;
            boolean isSelected = i == selected;
            HeroPower own = LegacyHero.ownPowerOf(ability.data()).map(hero::ownPowerById).orElse(null);
            boolean on = own != null && active != null && active.test(player, own);
            if (isSelected) graphics.fill(ix - 1, y - 1, ix + 17, y + 17, frame);
            if (on) graphics.fill(ix - 1, y + 17, ix + 17, y + 19, running);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), ix, y, ability.iconIndex() * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, isSelected || on ? 0xFFFFFFFF : 0x90FFFFFF);
            long left = state == null ? 0 : state.remaining(i, now);
            if (left > 0) {
                int shade = Math.round(16 * Math.min(1.0F, (float) left / ability.cooldownTicks()));
                graphics.fill(ix, y + 16 - shade, ix + 16, y + 16, 0xA0000000);
            }
        }
        EngineAbility chosen = powers.set().byIndex(selected);
        Component line = Component.translatable("hud.greenlantern.power_key", chosen.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage());
        graphics.drawString(mc.font, line, x, lineY, text, true);
        AbilityFunctions.Chosen function = AbilityFunctions.choose(chosen.data(), EngineKeys.press(chosen), player);
        if (function.index() > 0) {
            int cell = AbilityIcons.functionCell(hero.data(), selected, function.index());
            int fx = x + mc.font.width(line) + 4;
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), fx, lineY - 4, cell * 16, 0, 16, 16, wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT);
        }
    }

    /** Draws the emblem of the hero (the last cell of its old icon sheet, after its old powers) at ({@code x}, {@code y}). */
    public static void emblem(GuiGraphics graphics, LegacyHero hero, int x, int y, int cell) {
        WheelStyle old = hero.ownWheel();
        graphics.blit(RenderPipelines.GUI_TEXTURED, old.icons(), x, y, cell * 16, 0, 16, 16, old.iconsWidth(), WheelStyle.ICONS_HEIGHT);
    }
}
