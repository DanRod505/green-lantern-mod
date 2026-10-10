package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.aquaman.AquaPower;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.SeaForce;
import com.danrod505.greenlantern.batman.BatCharge;
import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.SpeedForce;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.hero.Hero;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SelectPowerPacket;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * A hero's power wheel (same controls as the Lantern's construct wheel, in the hero's colours: Speed
 * Force red and gold for the Flash, sea teal and gold for Aquaman, night black and
 * utility-belt yellow for Batman, Kryptonian blue, red and gold for Superman): hold the wheel key, point at a
 * power and release the key (or click) to select it.
 */
public class PowerWheelScreen extends Screen {
    public static final Identifier ICONS = GreenLantern.id("textures/gui/flash_powers.png");
    public static final int ICONS_W = 64;
    public static final int ICONS_H = 16;
    public static final Identifier AQUA_ICONS = GreenLantern.id("textures/gui/aquaman_powers.png");
    public static final int AQUA_ICONS_W = 96;
    public static final Identifier BAT_ICONS = GreenLantern.id("textures/gui/batman_powers.png");
    public static final int BAT_ICONS_W = 80;
    public static final Identifier SUPER_ICONS = GreenLantern.id("textures/gui/superman_powers.png");
    public static final int SUPER_ICONS_W = 80;

    /** Everything that differs between the heroes' wheels. */
    private record Powers(WheelTextures.Theme theme, Identifier icons, int iconsW, Function<Player, ItemStack> item,
                          Function<ItemStack, RingEnergy> energy, ToIntFunction<ItemStack> selected, int count,
                          IntFunction<Component> name, IntFunction<Component> description, IntFunction<Integer> cost,
                          String costKey, String energyKey, int background, int nameColor, int costColor, int textColor,
                          int dimIcon, int numberColor, int hintColor) {
        static final Powers FLASH = new Powers(WheelTextures.Theme.FLASH, ICONS, ICONS_W, FlashHelper::findRing, SpeedForce::get,
                ring -> SpeedsterPower.selected(ring).ordinal(), SpeedsterPower.count(),
                i -> SpeedsterPower.byIndex(i).displayName(), i -> SpeedsterPower.byIndex(i).description(), i -> SpeedsterPower.byIndex(i).cost(),
                "wheel.greenlantern.speed_cost", "tooltip.greenlantern.speed_force", 0x66100400, 0xFFD24A, 0xFFE6A0, 0xF0E0D0,
                0xD8B8A8, 0xFFD27A, 0xC8B8A8);
        static final Powers AQUAMAN = new Powers(WheelTextures.Theme.AQUAMAN, AQUA_ICONS, AQUA_ICONS_W, AquamanHelper::findEmblem, SeaForce::get,
                emblem -> AquaPower.selected(emblem).ordinal(), AquaPower.count(),
                i -> AquaPower.byIndex(i).displayName(), i -> AquaPower.byIndex(i).description(), i -> AquaPower.byIndex(i).cost(),
                "wheel.greenlantern.sea_cost", "tooltip.greenlantern.sea_force", 0x66001014, 0xF2C94A, 0xA8F0E0, 0xD8F0EC,
                0xA8C8C4, 0xF2D27A, 0xA8C4C0);
        static final Powers BATMAN = new Powers(WheelTextures.Theme.BATMAN, BAT_ICONS, BAT_ICONS_W, BatmanHelper::findBelt, BatCharge::get,
                belt -> BatPower.selected(belt).ordinal(), BatPower.count(),
                i -> BatPower.byIndex(i).displayName(), i -> BatPower.byIndex(i).description(), i -> BatPower.byIndex(i).cost(),
                "wheel.greenlantern.bat_cost", "tooltip.greenlantern.bat_charge", 0x88050506, 0xF2D03A, 0xE8D890, 0xE0E0E4,
                0x9A9AA4, 0xF2D03A, 0xA8A8B0);
        static final Powers SUPERMAN = new Powers(WheelTextures.Theme.SUPERMAN, SUPER_ICONS, SUPER_ICONS_W,
                com.danrod505.greenlantern.superman.SupermanHelper::findCrystal, com.danrod505.greenlantern.superman.SolarEnergy::get,
                crystal -> com.danrod505.greenlantern.superman.SuperPower.selected(crystal).ordinal(), com.danrod505.greenlantern.superman.SuperPower.count(),
                i -> com.danrod505.greenlantern.superman.SuperPower.byIndex(i).displayName(),
                i -> com.danrod505.greenlantern.superman.SuperPower.byIndex(i).description(),
                i -> com.danrod505.greenlantern.superman.SuperPower.byIndex(i).cost(),
                "wheel.greenlantern.solar_cost", "tooltip.greenlantern.solar_energy", 0x66020A24, 0xFFD84A, 0xFFE8A0, 0xE6ECFF,
                0xA8B4D8, 0xFFD84A, 0xA8B4D0);
    }

    private final Powers powers;
    private final int count;
    private final int current;
    private int hovered = -1;
    private final long openedAt = System.nanoTime();
    private final float[] pop;
    private boolean confirmed;

    public PowerWheelScreen() {
        this(Hero.FLASH);
    }

    public PowerWheelScreen(Hero hero) {
        super(Component.translatable("wheel.greenlantern.powers"));
        this.powers = switch (hero) {
            case AQUAMAN -> Powers.AQUAMAN;
            case BATMAN -> Powers.BATMAN;
            case SUPERMAN -> Powers.SUPERMAN;
            default -> Powers.FLASH;
        };
        this.count = powers.count();
        this.pop = new float[count];
        ItemStack item = powers.item().apply(net.minecraft.client.Minecraft.getInstance().player);
        this.current = item.isEmpty() ? 0 : powers.selected().applyAsInt(item);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (minecraft.player == null || powers.item().apply(minecraft.player).isEmpty()) onClose();
    }

    private int sliceAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - centerY();
        if (dx * dx + dy * dy < Math.pow(outerRadius() * WheelTextures.INNER * 0.55, 2)) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        return Math.floorMod((int) Math.round(angle / (360.0 / count)), count);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        setHovered(sliceAt(mouseX, mouseY));
    }

    private void setHovered(int index) {
        if (index != hovered && index >= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.POWER_SELECT.get(), 1.3F + 0.1F * index, 0.25F));
        }
        hovered = index;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (KeyBindings.CONSTRUCT_WHEEL.matches(event)) {
            confirm();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + Math.min(9, count)) {
            hovered = key - GLFW.GLFW_KEY_1;
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setHovered(sliceAt(event.x(), event.y()));
            confirm();
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int start = hovered >= 0 ? hovered : current;
            setHovered(Math.floorMod(start + (scrollY > 0 ? -1 : 1), count));
            return true;
        }
        return false;
    }

    private void confirm() {
        if (confirmed) return;
        confirmed = true;
        if (hovered >= 0 && hovered != current) {
            ModNetwork.sendToServer(new SelectPowerPacket(hovered, false));
        }
        onClose();
    }

    private float outerRadius() {
        return Math.min(height * 0.34F, 120.0F);
    }

    private float centerY() {
        return height * 0.46F;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, powers.background());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        WheelTextures.ensure(powers.theme(), count);
        float openT = Math.min(1.0F, (System.nanoTime() - openedAt) / 140_000_000.0F);
        float open = 1.0F - (1.0F - openT) * (1.0F - openT);

        float cx = width / 2.0F;
        float cy = centerY();
        float outer = outerRadius() * (0.75F + 0.25F * open);
        float inner = outer * WheelTextures.INNER;
        float slice = 360.0F / count;
        int alpha = (int) (255 * open);
        int tint = argb(alpha, 0xFFFFFF);

        drawLayer(graphics, powers.theme().base(), cx, cy, outer, 0.0F, 1.0F, tint);
        if (current != hovered) {
            drawLayer(graphics, powers.theme().currentLayer(), cx, cy, outer, current * slice, 1.0F, tint);
        }
        for (int i = 0; i < count; i++) {
            pop[i] += ((i == hovered ? 1.0F : 0.0F) - pop[i]) * 0.3F;
            if (pop[i] > 0.02F) {
                drawLayer(graphics, powers.theme().highlight(), cx, cy, outer, i * slice, 1.0F + 0.08F * pop[i], argb(alpha * Math.min(1.0F, pop[i] * 1.5F), 0xFFFFFF));
            }
        }
        for (int i = 0; i < count; i++) {
            float centerAngle = -90.0F + i * slice;
            float grow = 1.0F + 0.08F * pop[i];
            float mid = (inner + outer * grow) / 2.0F;
            float ix = cx + Mth.cos(centerAngle * Mth.DEG_TO_RAD) * mid;
            float iy = cy + Mth.sin(centerAngle * Mth.DEG_TO_RAD) * mid;
            float scale = 2.2F + 0.6F * pop[i];
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(scale, scale);
            int iconTint = i == hovered || i == current ? 0xFFFFFF : powers.dimIcon();
            graphics.blit(RenderPipelines.GUI_TEXTURED, powers.icons(), -8, -8, i * 16, 0, 16, 16, powers.iconsW(), ICONS_H, argb(alpha, iconTint));
            graphics.pose().popMatrix();
            float lx = cx + Mth.cos(centerAngle * Mth.DEG_TO_RAD) * (outer * grow - 11);
            float ly = cy + Mth.sin(centerAngle * Mth.DEG_TO_RAD) * (outer * grow - 11);
            graphics.drawCenteredString(font, String.valueOf(i + 1), (int) lx, (int) ly - 4, argb(alpha * 0.75F, powers.numberColor()));
        }

        int shown = hovered >= 0 ? hovered : current;
        ItemStack item = powers.item().apply(minecraft.player);
        RingEnergy energy = powers.energy().apply(item);
        int cost = powers.cost().apply(shown);
        List<FormattedCharSequence> nameLines = font.split(powers.name().apply(shown).copy().withStyle(s -> s.withBold(true)), (int) (inner * 1.9F));
        int lineY = (int) cy - (nameLines.size() * 10 + 12) / 2;
        for (FormattedCharSequence line : nameLines) {
            graphics.drawCenteredString(font, line, (int) cx, lineY, argb(alpha, powers.nameColor()));
            lineY += 10;
        }
        boolean affordable = energy.stored() >= cost;
        graphics.drawCenteredString(font, Component.translatable(powers.costKey(), cost),
                (int) cx, lineY + 3, argb(alpha, affordable ? powers.costColor() : 0xFF6060));

        float top = cy - outer * WheelTextures.EXTENT;
        float bottom = cy + outer * WheelTextures.EXTENT;
        graphics.drawCenteredString(font, Component.translatable(powers.energyKey(), energy.stored(), energy.capacity()),
                (int) cx, (int) top - 11, argb(alpha, energy.fraction() < 0.2F ? 0xFF6060 : powers.nameColor()));
        int descY = (int) bottom + 3;
        List<FormattedCharSequence> desc = font.split(powers.description().apply(shown), Math.min(width - 20, 300));
        for (FormattedCharSequence line : desc.subList(0, Math.min(3, desc.size()))) {
            graphics.drawCenteredString(font, line, (int) cx, descY, argb(alpha, powers.textColor()));
            descY += 10;
        }
        graphics.drawCenteredString(font, Component.translatable("wheel.greenlantern.power_hint", KeyBindings.CONSTRUCT_WHEEL.getTranslatedKeyMessage(),
                KeyBindings.HERO_POWER.getTranslatedKeyMessage()), (int) cx, descY + 2, argb(alpha * 0.7F, powers.hintColor()));
    }

    private static void drawLayer(GuiGraphics graphics, Identifier texture, float cx, float cy, float outer, float rotation, float scale, int color) {
        int size = Math.round(outer * WheelTextures.EXTENT * 2);
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate(rotation * Mth.DEG_TO_RAD);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -size / 2, -size / 2, 0, 0, size, size,
                WheelTextures.SIZE, WheelTextures.SIZE, WheelTextures.SIZE, WheelTextures.SIZE, color);
        graphics.pose().popMatrix();
    }

    private static int argb(float alpha, int rgb) {
        return Mth.clamp((int) alpha, 0, 255) << 24 | (rgb & 0xFFFFFF);
    }
}
