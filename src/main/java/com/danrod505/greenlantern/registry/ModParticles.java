package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, GreenLantern.MODID);

    /** Small, fast, bright spark of hard light. */
    public static final RegistryObject<SimpleParticleType> SPARK = PARTICLES.register("lantern_spark", () -> new SimpleParticleType(false));
    /** Soft glowing orb that slowly fades out. */
    public static final RegistryObject<SimpleParticleType> GLOW = PARTICLES.register("lantern_glow", () -> new SimpleParticleType(false));
    /** Expanding ring used for shockwaves and impacts (always rendered, even at "decreased" particles). */
    public static final RegistryObject<SimpleParticleType> SHOCKWAVE = PARTICLES.register("lantern_shockwave", () -> new SimpleParticleType(true));

    private ModParticles() {}
}
