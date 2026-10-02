package com.danrod505.greenlantern.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * Ring perpendicular to a direction (given as the particle velocity) that expands and fades:
 * the vapor cone left behind when a Lantern breaks the sound barrier.
 */
public class SonicRingParticle extends SingleQuadParticle {
    private final Quaternionf front;
    private final Quaternionf back;
    private final float maxSize;

    protected SonicRingParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 1.0E-4 ? (float) (dx / len) : 0.0F;
        float ny = len > 1.0E-4 ? (float) (dy / len) : 1.0F;
        float nz = len > 1.0E-4 ? (float) (dz / len) : 0.0F;
        this.front = new Quaternionf().rotationTo(0.0F, 0.0F, 1.0F, nx, ny, nz);
        this.back = new Quaternionf(front).rotateX((float) Math.PI);
        // Weak rings (len < 1) are the flickering supersonic cone; strong ones are the boom itself.
        boolean boom = len >= 0.9;
        this.maxSize = boom ? 5.5F : 2.2F;
        this.lifetime = boom ? 14 : 7;
        this.xd = -nx * 0.05;
        this.yd = -ny * 0.05;
        this.zd = -nz * 0.05;
        this.gravity = 0;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.quadSize = 0.4F;
        this.alpha = boom ? 0.95F : 0.45F;
        setColor(0.75F, 1.0F, 0.8F);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        quadSize = 0.4F + maxSize * (1.0F - (1.0F - life) * (1.0F - life));
        alpha = Math.max(0.0F, (maxSize > 3 ? 0.95F : 0.45F) * (1.0F - life));
    }

    /** Particle quads are one-sided: draw the ring facing both ways so it is seen from any angle. */
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        extractRotatedQuad(state, camera, front, partialTick);
        extractRotatedQuad(state, camera, back, partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SonicRingParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
