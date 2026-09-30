package com.danrod505.greenlantern.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Glowing green particle. Two flavours share this class: sparks (small, fast, twinkling) and
 * glows (soft orbs that drift and fade out). Both are always fully bright.
 */
public class LanternParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final boolean spark;
    private final float baseSize;

    protected LanternParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean spark) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.spark = spark;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        if (spark) {
            this.friction = 0.86F;
            this.gravity = 0.02F;
            this.lifetime = 8 + random.nextInt(8);
            this.baseSize = 0.08F + random.nextFloat() * 0.06F;
        } else {
            this.friction = 0.92F;
            this.gravity = -0.002F;
            this.lifetime = 16 + random.nextInt(14);
            this.baseSize = 0.12F + random.nextFloat() * 0.12F;
        }
        this.quadSize = baseSize;
        float tint = random.nextFloat() * 0.25F;
        // Textures are already green; a light random tint adds variety.
        setColor(0.8F + tint * 0.8F, 1.0F, 0.85F + tint * 0.6F);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        if (spark) {
            quadSize = baseSize * (1.0F - life * life);
            alpha = 1.0F;
        } else {
            quadSize = baseSize * (0.7F + 0.6F * Mth.sin(life * Mth.PI));
            alpha = 1.0F - life * life;
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class SparkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new LanternParticle(level, x, y, z, xd, yd, zd, sprites, true);
        }
    }

    public static class GlowProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public GlowProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new LanternParticle(level, x, y, z, xd, yd, zd, sprites, false);
        }
    }
}
