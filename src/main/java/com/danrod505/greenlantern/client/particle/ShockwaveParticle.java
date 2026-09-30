package com.danrod505.greenlantern.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Flat ring lying on the ground that quickly expands and fades (hammer / blast impacts). */
public class ShockwaveParticle extends SingleQuadParticle {
    private static final FacingCameraMode FLAT = (rotation, camera, partialTick) -> rotation.rotationX(-Mth.HALF_PI);

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 12;
        this.quadSize = 0.5F;
        setColor(0.5F, 1.0F, 0.6F);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        quadSize = 0.5F + 6.0F * (1.0F - (1.0F - life) * (1.0F - life));
        alpha = 1.0F - life;
    }

    @Override
    public FacingCameraMode getFacingCameraMode() {
        return FLAT;
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
}
