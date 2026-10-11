package com.danrod505.greenlantern.client.supergirl;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.supergirl.KryptoEntity;
import com.danrod505.greenlantern.supergirl.SupergirlConfig;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlServer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Super Hearing on the client: while it is on (the pendant says so), rings of sound ripple out from
 * every creature she hears, seen through walls and the ground (golden for enemies, pale for the
 * others), and arrows around the crosshair point to the closest footsteps. It never shows ores.
 */
public final class SupergirlHearingVisuals implements WorldLayer {
    private static final Identifier RING = GreenLantern.id("textures/misc/sound_ring.png");
    private static final int MAX_HEARD = 48;
    private static final int SEGMENTS = 28;

    /** A creature she hears: where it is (relative to the frame origin), its size and colour, and its own phase. */
    private record Heard(float x, float y, float z, float width, float height, int color, float phase) {}

    private float fade;
    private List<LivingEntity> heard = List.of();
    private final List<Heard> frame = new ArrayList<>();
    private float time;
    /** The camera's left and up directions this frame, so the ripples face her eyes through any wall. */
    private float lx, ly, lz, ux, uy, uz;

    /** Whether the local player has Super Hearing on. */
    static boolean localHearing(Player player) {
        if (player == null || !SupergirlHero.INSTANCE.isSuited(player)) return false;
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        return !item.isEmpty() && SupergirlServer.isHearing(item);
    }

    static boolean audible(Player player, LivingEntity e) {
        return e != player && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !(e instanceof KryptoEntity);
    }

    /** Every client tick: who she hears now. */
    void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;
        boolean on = level != null && localHearing(player);
        fade = Mth.approach(fade, on ? 1.0F : 0.0F, 0.1F);
        if (level == null || player == null || fade <= 0.0F) {
            heard = List.of();
            return;
        }
        double radius = SupergirlConfig.HEARING_RADIUS.get();
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> audible(player, e) && e.distanceToSqr(player) <= radius * radius));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        heard = found.size() > MAX_HEARD ? found.subList(0, MAX_HEARD) : found;
    }

    @Override
    public void extract(Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, float partialTick) {
        frame.clear();
        time = level.getGameTime() + partialTick;
        if (fade <= 0.0F) return;
        var camera = mc.gameRenderer.getMainCamera();
        lx = camera.leftVector().x();
        ly = camera.leftVector().y();
        lz = camera.leftVector().z();
        ux = camera.upVector().x();
        uy = camera.upVector().y();
        uz = camera.upVector().z();
        for (LivingEntity e : heard) {
            if (e.isRemoved()) continue;
            Vec3 at = e.getPosition(partialTick);
            int color = e instanceof Enemy ? 0xFFD447 : 0xE8F0FF;
            frame.add(new Heard((float) (at.x - origin.x), (float) (at.y - origin.y), (float) (at.z - origin.z), e.getBbWidth(), e.getBbHeight(),
                    color, (e.getId() * 7) % 30));
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        if (frame.isEmpty()) return;
        List<Heard> list = List.copyOf(frame);
        float t = time;
        float alpha = fade;
        float[] left = {lx, ly, lz};
        float[] up = {ux, uy, uz};
        collector.submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(RING), (pose, vc) -> {
            for (Heard h : list) {
                float cy = h.y() + h.height() * 0.55F;
                float size = Math.max(h.width(), h.height() * 0.5F);
                // A steady glow where the heartbeat is, and two ripples a second and a half apart, growing and fading:
                // facing her (seen through walls and the ground) and flat at the feet.
                facing(vc, pose, h.x(), cy, h.z(), size * 0.35F, size * 0.35F, (int) (150 * alpha) << 24 | h.color(), left, up);
                for (int k = 0; k < 2; k++) {
                    float p = ((t + h.phase() + k * 15) % 30) / 30.0F;
                    float r = size * 0.5F + p * (1.0F + size);
                    int a = Mth.clamp((int) (230 * alpha * (1.0F - p)), 0, 255);
                    facing(vc, pose, h.x(), cy, h.z(), r, 0.12F, a << 24 | h.color(), left, up);
                    ring(vc, pose, h.x(), h.y() + 0.05F, h.z(), r, 0.1F, (a / 2) << 24 | h.color());
                }
            }
        });
    }

    /** A flat ring of quads, seen from both sides. */
    private static void ring(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float radius, float thickness, int color) {
        float inner = Math.max(0.0F, radius - thickness);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(vc, pose, x + c0 * inner, y, z + s0 * inner, x + c0 * radius, y, z + s0 * radius,
                    x + c1 * radius, y, z + s1 * radius, x + c1 * inner, y, z + s1 * inner, color);
        }
    }

    /** A ring standing up toward the camera (left and up are the camera's axes). */
    private static void facing(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float radius, float thickness, int color,
                               float[] left, float[] up) {
        float inner = Math.max(0.0F, radius - thickness);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ox0 = left[0] * c0 + up[0] * s0, oy0 = left[1] * c0 + up[1] * s0, oz0 = left[2] * c0 + up[2] * s0;
            float ox1 = left[0] * c1 + up[0] * s1, oy1 = left[1] * c1 + up[1] * s1, oz1 = left[2] * c1 + up[2] * s1;
            quad(vc, pose, x + ox0 * inner, y + oy0 * inner, z + oz0 * inner, x + ox0 * radius, y + oy0 * radius, z + oz0 * radius,
                    x + ox1 * radius, y + oy1 * radius, z + oz1 * radius, x + ox1 * inner, y + oy1 * inner, z + oz1 * inner, color);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        vertex(vc, pose, x0, y0, z0, 0, 0, color);
        vertex(vc, pose, x1, y1, z1, 0, 1, color);
        vertex(vc, pose, x2, y2, z2, 1, 1, color);
        vertex(vc, pose, x3, y3, z3, 1, 0, color);
        vertex(vc, pose, x3, y3, z3, 1, 0, color);
        vertex(vc, pose, x2, y2, z2, 1, 1, color);
        vertex(vc, pose, x1, y1, z1, 0, 1, color);
        vertex(vc, pose, x0, y0, z0, 0, 0, color);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }

    /** HUD: arrows around the crosshair pointing to the six closest creatures she hears. */
    void renderArrows(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || fade <= 0.0F || mc.options.hideGui || heard.isEmpty()) return;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        double radius = SupergirlConfig.HEARING_RADIUS.get();
        int shown = 0;
        for (LivingEntity e : heard) {
            if (shown >= 6) break;
            if (e.isRemoved()) continue;
            shown++;
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F - player.getYRot();
            float angle = bearing * Mth.DEG_TO_RAD;
            float near = (float) (1.0 - Math.sqrt(dx * dx + dz * dz) / radius);
            int a = Mth.clamp((int) (255 * fade * (0.35F + 0.65F * near)), 0, 255);
            int color = a << 24 | (e instanceof Enemy ? 0xFFD447 : 0xE8F0FF);
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, cy);
            graphics.pose().rotate(angle);
            graphics.pose().translate(0, -26 - 6 * (1.0F - near));
            // A chevron pointing outward.
            for (int row = 0; row < 4; row++) {
                graphics.fill(-row - 1, row, -row + 1, row + 2, color);
                graphics.fill(row, row, row + 2, row + 2, color);
            }
            graphics.pose().popMatrix();
        }
    }
}
