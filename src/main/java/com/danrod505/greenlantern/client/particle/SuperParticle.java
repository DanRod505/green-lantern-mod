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
 * Superman's particles, on white textures tinted per kind: red-hot sparks of heat vision, golden
 * motes of sunlight and the icy puffs of super breath. All of them are fully bright.
 */
public class SuperParticle extends SingleQuadParticle {
    private enum Kind { HEAT, SOLAR, FROST, AMAZON }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;

    protected SuperParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.kind = kind;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        float tint = random.nextFloat();
        switch (kind) {
            case HEAT -> {
                this.friction = 0.84F;
                this.gravity = 0.04F;
                this.lifetime = 6 + random.nextInt(8);
                this.baseSize = 0.07F + random.nextFloat() * 0.07F;
                setColor(1.0F, 0.45F + 0.4F * tint, 0.12F + 0.2F * tint);
            }
            case SOLAR -> {
                this.friction = 0.92F;
                this.gravity = -0.004F;
                this.lifetime = 18 + random.nextInt(14);
                this.baseSize = 0.08F + random.nextFloat() * 0.1F;
                setColor(1.0F, 0.86F + 0.12F * tint, 0.45F + 0.4F * tint);
            }
            case FROST -> {
                this.friction = 0.9F;
                this.gravity = 0.0F;
                this.lifetime = 14 + random.nextInt(10);
                this.baseSize = 0.25F + random.nextFloat() * 0.2F;
                this.hasPhysics = true;
                setColor(0.82F + 0.15F * tint, 0.92F + 0.08F * tint, 1.0F);
            }
            case AMAZON -> {
                // Wonder Woman: sparks of golden divine light, a few of them crimson.
                this.friction = 0.88F;
                this.gravity = 0.01F;
                this.lifetime = 10 + random.nextInt(12);
                this.baseSize = 0.06F + random.nextFloat() * 0.08F;
                if (random.nextFloat() < 0.2F) {
                    setColor(1.0F, 0.25F + 0.2F * tint, 0.25F);
                } else {
                    setColor(1.0F, 0.78F + 0.18F * tint, 0.3F + 0.35F * tint);
                }
            }
            default -> throw new IllegalStateException();
        }
        this.quadSize = baseSize;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        switch (kind) {
            case HEAT -> {
                quadSize = baseSize * (1.0F - life * life);
                alpha = 1.0F;
            }
            case SOLAR -> {
                quadSize = baseSize * (0.7F + 0.6F * Mth.sin(life * Mth.PI));
                alpha = 1.0F - life * life;
            }
            case AMAZON -> {
                quadSize = baseSize * (1.0F - 0.6F * life);
                alpha = 1.0F - life * life;
            }
            case FROST -> {
                // The cloud swells as it slows down and fades.
                quadSize = baseSize * (1.0F + 3.0F * life);
                alpha = 0.75F * (1.0F - life);
            }
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

    public static class HeatProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public HeatProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SuperParticle(level, x, y, z, xd, yd, zd, sprites, Kind.HEAT);
        }
    }

    public static class SolarProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SolarProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SuperParticle(level, x, y, z, xd, yd, zd, sprites, Kind.SOLAR);
        }
    }

    public static class FrostProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FrostProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SuperParticle(level, x, y, z, xd, yd, zd, sprites, Kind.FROST);
        }
    }

    public static class AmazonProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public AmazonProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new SuperParticle(level, x, y, z, xd, yd, zd, sprites, Kind.AMAZON);
        }
    }
}
