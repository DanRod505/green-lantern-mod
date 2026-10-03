package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SelectConstructPacket;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Radial construct selector in the style of a weapon wheel: hold the wheel key, point the mouse at
 * a construct and release the key (or click) to select it. Number keys and the mouse wheel work too.
 */
public class ConstructWheelScreen extends Screen {
    private static final Identifier ICONS = GreenLantern.id("textures/gui/constructs.png");

    private final List<Construct> constructs = ConstructRegistry.all();
    private final Construct current;
    private int hovered = -1;
    private final long openedAt = System.nanoTime();
    private final float[] pop = new float[constructs.size()];
    private boolean confirmed;

    public ConstructWheelScreen() {
        super(Component.translatable("wheel.greenlantern.title"));
        ItemStack ring = RingHelper.findRing(net.minecraft.client.Minecraft.getInstance().player);
        this.current = ring.isEmpty() ? constructs.getFirst() : ConstructRegistry.selected(ring);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Closed by other means (e.g. the ring was dropped).
        if (minecraft.player == null || RingHelper.findRing(minecraft.player).isEmpty()) onClose();
    }

    // ---- input -----------------------------------------------------------------------------------

    private int sliceAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - centerY();
        if (dx * dx + dy * dy < Math.pow(outerRadius() * WheelTextures.INNER * 0.55, 2)) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        int n = constructs.size();
        return Math.floorMod((int) Math.round(angle / (360.0 / n)), n);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        setHovered(sliceAt(mouseX, mouseY));
    }

    private void setHovered(int index) {
        if (index != hovered && index >= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CONSTRUCT_SELECT.get(), 1.4F + 0.08F * index, 0.25F));
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
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + Math.min(9, constructs.size())) {
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
            int start = hovered >= 0 ? hovered : constructs.indexOf(current);
            setHovered(Math.floorMod(start + (scrollY > 0 ? -1 : 1), constructs.size()));
            return true;
        }
        return false;
    }

    private void confirm() {
        if (confirmed) return;
        confirmed = true;
        if (hovered >= 0 && constructs.get(hovered) != current) {
            ModNetwork.sendToServer(new SelectConstructPacket(constructs.get(hovered).id()));
        }
        onClose();
    }

    // ---- rendering -------------------------------------------------------------------------------

    private float outerRadius() {
        return Math.min(height * 0.33F, 115.0F);
    }


    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0x66000A04);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int n = constructs.size();
        WheelTextures.ensure(n);
        float openT = Math.min(1.0F, (System.nanoTime() - openedAt) / 160_000_000.0F);
        float open = 1.0F - (1.0F - openT) * (1.0F - openT);

        float cx = width / 2.0F;
        float cy = centerY();
        float outer = outerRadius() * (0.75F + 0.25F * open);
        float inner = outer * WheelTextures.INNER;
        float slice = 360.0F / n;
        int alpha = (int) (255 * open);
        int tint = argb(alpha, 0xFFFFFF);

        // Wheel background, the construct currently on the ring and the hovered slice.
        drawLayer(graphics, WheelTextures.BASE, cx, cy, outer, 0.0F, 1.0F, tint);
        int currentIndex = constructs.indexOf(current);
        if (currentIndex >= 0 && currentIndex != hovered) {
            drawLayer(graphics, WheelTextures.CURRENT, cx, cy, outer, currentIndex * slice, 1.0F, tint);
        }
        for (int i = 0; i < n; i++) {
            pop[i] += ((i == hovered ? 1.0F : 0.0F) - pop[i]) * 0.3F;
            if (pop[i] > 0.02F) {
                drawLayer(graphics, WheelTextures.HIGHLIGHT, cx, cy, outer, i * slice, 1.0F + 0.08F * pop[i], argb(alpha * Math.min(1.0F, pop[i] * 1.5F), 0xFFFFFF));
            }
        }

        for (int i = 0; i < n; i++) {
            Construct construct = constructs.get(i);
            boolean isHovered = i == hovered;
            float centerAngle = -90.0F + i * slice;
            float grow = 1.0F + 0.08F * pop[i];
            float mid = (inner + outer * grow) / 2.0F;
            float ix = cx + Mth.cos(centerAngle * Mth.DEG_TO_RAD) * mid;
            float iy = cy + Mth.sin(centerAngle * Mth.DEG_TO_RAD) * mid;
            float scale = 2.0F + 0.6F * pop[i];
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(scale, scale);
            int iconTint = isHovered || construct == current ? 0xFFFFFF : 0xB8D8C0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, -8, -8, construct.iconIndex() * 16, 0, 16, 16, 128, 16, argb(alpha, iconTint));
            graphics.pose().popMatrix();

            // Number shortcut near the outer edge.
            float lx = cx + Mth.cos(centerAngle * Mth.DEG_TO_RAD) * (outer * grow - 11);
            float ly = cy + Mth.sin(centerAngle * Mth.DEG_TO_RAD) * (outer * grow - 11);
            graphics.drawCenteredString(font, String.valueOf(i + 1), (int) lx, (int) ly - 4, argb(alpha * 0.75F, 0x9BE8AA));
        }

        // Center: name and energy cost of the hovered (or current) construct.
        Construct shown = hovered >= 0 ? constructs.get(hovered) : current;
        ItemStack ring = RingHelper.findRing(minecraft.player);
        RingEnergy energy = RingEnergy.get(ring);
        List<FormattedCharSequence> nameLines = font.split(shown.name().copy().withStyle(s -> s.withBold(true)), (int) (inner * 1.75F));
        int lineY = (int) cy - (nameLines.size() * 10 + 12) / 2;
        for (FormattedCharSequence line : nameLines) {
            graphics.drawCenteredString(font, line, (int) cx, lineY, argb(alpha, 0x7CFF96));
            lineY += 10;
        }
        boolean affordable = energy.stored() >= shown.activationCost();
        graphics.drawCenteredString(font, Component.translatable("wheel.greenlantern.cost", shown.activationCost()),
                (int) cx, lineY + 3, argb(alpha, affordable ? 0x9BE8AA : 0xFF6060));

        // Above the wheel: ring energy. Below: description and controls.
        float top = cy - outer * WheelTextures.EXTENT;
        float bottom = cy + outer * WheelTextures.EXTENT;
        graphics.drawCenteredString(font, Component.translatable("tooltip.greenlantern.energy", energy.stored(), energy.capacity()),
                (int) cx, (int) top - 11, argb(alpha, energy.fraction() < 0.2F ? 0xFF6060 : 0x7CFF96));
        int descY = (int) bottom + 3;
        List<FormattedCharSequence> desc = font.split(shown.description(), Math.min(width - 20, 300));
        for (FormattedCharSequence line : desc.subList(0, Math.min(2, desc.size()))) {
            graphics.drawCenteredString(font, line, (int) cx, descY, argb(alpha, 0xD8E8DC));
            descY += 10;
        }
        graphics.drawCenteredString(font, Component.translatable("wheel.greenlantern.hint", KeyBindings.CONSTRUCT_WHEEL.getTranslatedKeyMessage()),
                (int) cx, descY + 2, argb(alpha * 0.7F, 0xB0C8B6));
    }

    private float centerY() {
        return height * 0.46F;
    }

    /** Draws one wheel texture centered on the screen, rotated (degrees, clockwise) and scaled. */
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
