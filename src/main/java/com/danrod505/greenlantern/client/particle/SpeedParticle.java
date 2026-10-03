package com.danrod505.greenlantern.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Speed Force particles: crackling yellow sparks that flicker in and out, and short orange
 * streaks of displaced air. Always fully bright.
 */
public class SpeedParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final boolean streak;
    private final float baseSize;

    protected SpeedParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean streak) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.streak = streak;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        if (streak) {
            this.friction = 0.75F;
            this.lifetime = 5 + random.nextInt(5);
            this.baseSize = 0.2F + random.nextFloat() * 0.2F;
        } else {
            this.friction = 0.8F;
            this.lifetime = 4 + random.nextInt(7);
            this.baseSize = 0.06F + random.nextFloat() * 0.08F;
        }
        this.quadSize = baseSize;
        float tint = random.nextFloat() * 0.2F;
        setColor(1.0F, 0.9F + tint * 0.5F, 0.8F + tint);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        if (streak) {
            setSpriteFromAge(sprites);
            quadSize = baseSize * (1.0F - life * 0.5F);
            alpha = 1.0F - life;
        } else {
            // Electric flicker: jump between sprites and sizes.
            setSprite(sprites.get(random));
            quadSize = baseSize * (0.6F + random.nextFloat() * 0.8F) * (1.0F - life * life);
            alpha = random.nextFloat() < 0.2F ? 0.4F : 1.0F;
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
            return new SpeedParticle(level, x, y, z, xd, yd, zd, sprites, false);
        }
    }

    public static class StreakProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public StreakProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SpeedParticle(level, x, y, z, xd, yd, zd, sprites, true);
        }
    }
}
