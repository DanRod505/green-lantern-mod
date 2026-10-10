package com.danrod505.greenlantern.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.joml.Quaternionf;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Flat ring lying on the ground that quickly expands and fades (hammer / blast impacts). */
public class ShockwaveParticle extends SingleQuadParticle {
    private static final Quaternionf UP = new Quaternionf().rotationX(-Mth.HALF_PI);
    private static final Quaternionf DOWN = new Quaternionf().rotationX(Mth.HALF_PI);

    private final float growth;

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        this(level, x, y, z, sprites, 0.85F, 1.0F, 0.88F, 6.0F, 12);
    }

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float r, float g, float b, float growth, int lifetime) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.growth = growth;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = lifetime;
        this.quadSize = 0.5F;
        setColor(r, g, b);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        quadSize = 0.5F + growth * (1.0F - (1.0F - life) * (1.0F - life));
        alpha = 1.0F - life;
    }

    /** Particle quads are one-sided: draw the flat ring facing up and down. */
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        extractRotatedQuad(state, camera, UP, partialTick);
        extractRotatedQuad(state, camera, DOWN, partialTick);
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
            return new ShockwaveParticle(level, x, y, z, sprites);
        }
    }

    /** Superman's: a wide white ring of dust and air (the super punch, his landings). */
    public static class SupermanProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SupermanProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new ShockwaveParticle(level, x, y, z, sprites, 0.95F, 0.97F, 1.0F, 9.0F, 16);
        }
    }

    /** Wonder Woman's: a ring of golden force (the bracelets' shockwave, her landings). */
    public static class AmazonProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public AmazonProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new ShockwaveParticle(level, x, y, z, sprites, 1.0F, 0.82F, 0.35F, 11.0F, 18);
        }
    }
}
