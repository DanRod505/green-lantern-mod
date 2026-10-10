package com.danrod505.greenlantern.client.render.wonderwoman;

import com.danrod505.greenlantern.client.render.HardLight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/** The glowing golden rope of the Lasso of Truth, drawn as two crossed ribbons (a cheap round cord). */
public final class Rope {
    public static final int GOLD = 0xFFFFC832;
    public static final int GLOW = 0x55FFD86A;

    private Rope() {}

    /** One straight piece of rope from p0 to p1 (relative to the pose), with a faint glow around it. */
    public static void segment(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, float width, int color, int glow) {
        Vec3 dir = p1.subtract(p0);
        if (dir.lengthSqr() < 1.0E-8) return;
        dir = dir.normalize();
        Vec3 a = dir.cross(new Vec3(0, 1, 0));
        if (a.lengthSqr() < 1.0E-4) a = new Vec3(1, 0, 0);
        a = a.normalize();
        Vec3 b = dir.cross(a).normalize();
        ribbon(vc, pose, p0, p1, a.scale(width), color);
        ribbon(vc, pose, p0, p1, b.scale(width), color);
        if (glow != 0) {
            ribbon(vc, pose, p0, p1, a.scale(width * 3.0), glow);
            ribbon(vc, pose, p0, p1, b.scale(width * 3.0), glow);
        }
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 w, int color) {
        HardLight.quad(vc, pose,
                (float) (p0.x - w.x), (float) (p0.y - w.y), (float) (p0.z - w.z),
                (float) (p0.x + w.x), (float) (p0.y + w.y), (float) (p0.z + w.z),
                (float) (p1.x + w.x), (float) (p1.y + w.y), (float) (p1.z + w.z),
                (float) (p1.x - w.x), (float) (p1.y - w.y), (float) (p1.z - w.z), color, 0, 1, 0);
        HardLight.quad(vc, pose,
                (float) (p1.x - w.x), (float) (p1.y - w.y), (float) (p1.z - w.z),
                (float) (p1.x + w.x), (float) (p1.y + w.y), (float) (p1.z + w.z),
                (float) (p0.x + w.x), (float) (p0.y + w.y), (float) (p0.z + w.z),
                (float) (p0.x - w.x), (float) (p0.y - w.y), (float) (p0.z - w.z), color, 0, 1, 0);
    }

    /** A horizontal loop of rope (in the XZ plane of the pose) around a center. */
    public static void loop(VertexConsumer vc, PoseStack.Pose pose, Vec3 center, double radius, int segments, float angle, float width, int color, int glow) {
        for (int i = 0; i < segments; i++) {
            double a0 = angle + i * Math.PI * 2 / segments;
            double a1 = angle + (i + 1) * Math.PI * 2 / segments;
            Vec3 p0 = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            segment(vc, pose, p0, p1, width, color, glow);
        }
    }
}
