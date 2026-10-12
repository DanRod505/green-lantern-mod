package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.hero.power.Fx;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The visual presets of batch 5: the trail a character leaves when it flies or runs and the aura
 * around its body (both set in the character's file, {@code "visual": {"rastro": {...}, "aura":
 * {...}}}), the drawing of beams (straight, spiral or lightning, with a thickness) and the
 * translucent energy objects of constructs.
 */
public final class Visuals {
    /** Where each suited player was a tick ago, for the speed of the trail. */
    private static final Map<UUID, Vec3> LAST = new ConcurrentHashMap<>();

    private Visuals() {}

    // ---- trail and aura --------------------------------------------------------------------------------

    /**
     * Every tick of a suited character: the trail ({@code rastro}: {@code cor}, {@code largura}
     * 0.3, {@code brilho} 0 to 1, 0.5) while it moves fast (flying, running, falling), and the aura
     * ({@code aura}: {@code cor}, {@code largura} 0.6, {@code brilho} 0.5, {@code quando} sempre,
     * parado or voando).
     */
    public static void tick(ServerPlayer player, EngineHero hero) {
        CharacterData data = hero.data();
        CharacterData.Visual visual = data.visual();
        Vec3 now = player.position();
        Vec3 before = LAST.put(player.getUUID(), now);
        double speed = before == null ? 0.0 : now.distanceTo(before);
        if (speed > 8.0) speed = 0.0; // a teleport, not a run
        ServerLevel level = player.level();
        if (!visual.trail().values().isEmpty() && speed > 0.28) trail(level, player, visual.trail(), data.colors(), before, now);
        if (!visual.aura().values().isEmpty()) aura(level, player, visual.aura(), data.colors(), speed);
    }

    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }

    private static void trail(ServerLevel level, ServerPlayer player, CharacterData.Settings s, CharacterData.Colors colors, Vec3 from, Vec3 to) {
        int color = s.color("cor", colors.main());
        double width = Math.max(0.05, Math.min(1.5, s.number("largura", 0.3)));
        double shine = Math.max(0.0, Math.min(1.0, s.number("brilho", 0.5)));
        float size = (float) (0.6 + width * 2.0);
        double height = player.getBbHeight() * 0.5;
        int steps = (int) Math.ceil(from.distanceTo(to) / 0.25);
        for (int i = 0; i < steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps).add(0, height, 0);
            level.sendParticles(PresetFx.dust(color, size), at.x, at.y, at.z, 1, width * 0.4, width * 0.4, width * 0.4, 0.0);
            if (level.random.nextDouble() < shine * 0.5) {
                level.sendParticles(PresetFx.dust(PresetFx.mix(color, colors.light(), 0.6F), size * 0.6F), at.x, at.y, at.z, 1, width * 0.2, width * 0.2, width * 0.2, 0.0);
            }
        }
        if (shine > 0.7 && player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, from.x, from.y + height, from.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void aura(ServerLevel level, ServerPlayer player, CharacterData.Settings s, CharacterData.Colors colors, double speed) {
        String when = s.text("quando", "sempre");
        boolean flying = !player.onGround() && !player.isInWater();
        if (when.equals("parado") && speed > 0.05) return;
        if (when.equals("voando") && !flying) return;
        if (player.tickCount % 2 != 0) return;
        int color = s.color("cor", colors.main());
        double width = Math.max(0.3, Math.min(2.0, s.number("largura", 0.6)));
        double shine = Math.max(0.0, Math.min(1.0, s.number("brilho", 0.5)));
        double height = player.getBbHeight();
        for (int i = 0; i < 4; i++) {
            double a = player.tickCount * 0.15 + i * Math.PI / 2;
            double y = level.random.nextDouble() * height;
            double r = width * (0.8 + 0.2 * Math.sin(y * 3));
            level.sendParticles(PresetFx.dust(color, 0.9F), player.getX() + Math.cos(a) * r, player.getY() + y, player.getZ() + Math.sin(a) * r,
                    1, 0.02, 0.05, 0.02, 0.0);
        }
        if (level.random.nextDouble() < shine * 0.4) {
            level.sendParticles(PresetFx.dust(colors.light(), 0.6F), player.getX(), player.getY() + level.random.nextDouble() * height, player.getZ(),
                    1, width * 0.6, 0.1, width * 0.6, 0.0);
        }
        if (shine > 0.75 && player.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + height * 0.6, player.getZ(), 1, width * 0.5, 0.3, width * 0.5, 0.0);
        }
    }

    // ---- beams -----------------------------------------------------------------------------------------

    /**
     * Draws a beam from {@code from} to {@code to}: {@code forma} reto (a core and a glow), espiral
     * (two strands turning around the core) or raio (a jagged lightning line, new every tick), as
     * thick as {@code espessura} (1).
     */
    public static void beam(PresetContext c, Vec3 from, Vec3 to, int color, ParticleOptions glow, int age) {
        CharacterData.Settings s = c.settings();
        beam(c.level(), from, to, color, glow, s.text("forma", "reto"), Math.max(0.2, Math.min(4.0, s.number("espessura", 1.0))), age);
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color, ParticleOptions glow, String form, double thickness, int age) {
        ParticleOptions core = PresetFx.dust(color, (float) (1.4 * thickness));
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 1.0E-3) return;
        Vec3 unit = dir.scale(1.0 / length);
        Vec3 side = unit.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(unit).normalize();
        switch (form) {
            case "espiral" -> {
                Fx.line(level, from, to, core, 2.0);
                for (double d = 0; d < length; d += 0.2) {
                    double a = d * 2.5 - age * 0.6;
                    double r = 0.25 * thickness;
                    for (int strand = 0; strand < 2; strand++) {
                        double b = a + strand * Math.PI;
                        Vec3 at = from.add(unit.scale(d)).add(side.scale(Math.cos(b) * r)).add(up.scale(Math.sin(b) * r));
                        level.sendParticles(glow, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    }
                }
            }
            case "raio" -> {
                Vec3 last = from;
                int segments = Math.max(3, (int) (length / 1.2));
                for (int i = 1; i <= segments; i++) {
                    Vec3 next = from.add(unit.scale(length * i / segments));
                    if (i < segments) {
                        double jag = 0.35 * thickness;
                        next = next.add(side.scale((level.random.nextDouble() - 0.5) * 2 * jag)).add(up.scale((level.random.nextDouble() - 0.5) * 2 * jag));
                    }
                    Fx.line(level, last, next, core, 4.0);
                    Fx.line(level, last, next, glow, 1.5);
                    last = next;
                }
            }
            default -> {
                Fx.line(level, from, to, core, 3.0);
                Fx.line(level, from, to, glow, 1.0);
                if (thickness > 1.5) {
                    double r = 0.12 * thickness;
                    for (int k = 0; k < 4; k++) {
                        double a = k * Math.PI / 2 + age * 0.3;
                        Vec3 off = side.scale(Math.cos(a) * r).add(up.scale(Math.sin(a) * r));
                        Fx.line(level, from.add(off), to.add(off), core, 1.0);
                    }
                }
            }
        }
    }

    // ---- constructs ------------------------------------------------------------------------------------

    /**
     * A translucent object of energy made of glowing glass boxes in the colour nearest
     * {@code color}: {@code martelo}, {@code serra}, {@code broca}, {@code cubo} or {@code escudo},
     * {@code scale} times its size. Move it with {@link #moveTo} every tick; {@link #remove} it at the end.
     */
    public static final class Construct {
        private record Piece(Display.BlockDisplay display, Vec3 offset, Vector3f size) {}

        private final List<Piece> pieces = new ArrayList<>();

        Construct(ServerPlayer owner, String shape, int color, double scale, Vec3 at) {
            BlockState glass = DefensePresets.glass(color);
            switch (shape) {
                case "martelo" -> {
                    add(owner, glass, color, at, new Vec3(0, 0, 0), new Vector3f(1.2F, 0.8F, 0.8F), scale);
                    add(owner, glass, color, at, new Vec3(0, 0.8, 0), new Vector3f(0.22F, 1.3F, 0.22F), scale);
                }
                case "serra" -> {
                    add(owner, glass, color, at, Vec3.ZERO, new Vector3f(1.2F, 0.12F, 1.2F), scale);
                    add(owner, glass, color, at, Vec3.ZERO, new Vector3f(0.5F, 0.16F, 0.5F), scale);
                }
                case "broca" -> {
                    add(owner, glass, color, at, new Vec3(0, 0, 0), new Vector3f(0.7F, 0.5F, 0.7F), scale);
                    add(owner, glass, color, at, new Vec3(0, -0.45, 0), new Vector3f(0.45F, 0.45F, 0.45F), scale);
                    add(owner, glass, color, at, new Vec3(0, -0.8, 0), new Vector3f(0.2F, 0.35F, 0.2F), scale);
                }
                case "escudo" -> add(owner, glass, color, at, Vec3.ZERO, new Vector3f(1.6F, 1.8F, 0.12F), scale);
                default -> add(owner, glass, color, at, Vec3.ZERO, new Vector3f(1.0F, 1.0F, 1.0F), scale);
            }
        }

        private void add(ServerPlayer owner, BlockState glass, int color, Vec3 at, Vec3 offset, Vector3f size, double scale) {
            Vector3f scaled = new Vector3f(size).mul((float) scale);
            Vec3 shift = offset.scale(scale);
            Display.BlockDisplay display = Displays.block(owner, glass, corner(at, shift, scaled), scaled, color);
            if (display != null) pieces.add(new Piece(display, shift, scaled));
        }

        private static Vec3 corner(Vec3 center, Vec3 offset, Vector3f size) {
            return center.add(offset).subtract(size.x / 2.0, size.y / 2.0, size.z / 2.0);
        }

        /** Moves the object so its middle is at {@code center}. */
        public void moveTo(Vec3 center) {
            for (Piece piece : pieces) {
                if (!piece.display.isRemoved()) piece.display.setPos(corner(center, piece.offset, piece.size));
            }
        }

        public int size() {
            return (int) pieces.stream().filter(p -> !p.display.isRemoved()).count();
        }

        public void remove() {
            for (Piece piece : pieces) Summons.dismiss(piece.display);
        }
    }

    /** A translucent construct of {@code shape} for the ability ({@code escala} sets its size, 1). */
    public static Construct construct(PresetContext c, String shape, Vec3 at) {
        return new Construct(c.player(), shape, c.color(), Math.max(0.3, Math.min(4.0, c.settings().number("escala", 1.0))), at);
    }
}
