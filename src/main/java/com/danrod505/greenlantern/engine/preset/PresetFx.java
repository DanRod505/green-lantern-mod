package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.network.EngineFxPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * The look, sound and side effect of the elements the presets use ({@code "elemento"} in an
 * ability's settings): energia (the character's colour), raio, fogo, gelo, agua, sombra, magia, luz,
 * fumaca, terra, gas and som. Batch 5 of the presets brings the full families of particles and the
 * sounds of each class; these are the basic ones.
 */
public final class PresetFx {
    public static final String ENERGY = "energia";

    private PresetFx() {}

    /** The particle of an element; energy is a dust of {@code color}. */
    public static ParticleOptions particle(String element, int color) {
        return switch (element) {
            case "raio" -> ParticleTypes.ELECTRIC_SPARK;
            case "fogo" -> ParticleTypes.FLAME;
            case "gelo" -> ParticleTypes.SNOWFLAKE;
            case "agua" -> ParticleTypes.SPLASH;
            case "sombra" -> ParticleTypes.SQUID_INK;
            case "magia" -> ParticleTypes.WITCH;
            case "luz" -> ParticleTypes.END_ROD;
            case "fumaca" -> ParticleTypes.LARGE_SMOKE;
            case "terra" -> new DustParticleOptions(0x7A5A36, 1.4F);
            case "gas" -> new DustParticleOptions(0x8FCB3A, 1.6F);
            case "som" -> new DustParticleOptions(0xE8F4FF, 1.0F);
            default -> new DustParticleOptions(color, 1.2F);
        };
    }

    /** A puff of the element's particles at {@code at}. */
    public static void burst(ServerLevel level, Vec3 at, String element, int color, int count, double spread) {
        level.sendParticles(particle(element, color), at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    /** The sound of a shot of the element. */
    public static SoundEvent shotSound(String element) {
        return switch (element) {
            case "fogo" -> SoundEvents.BLAZE_SHOOT;
            case "gelo" -> SoundEvents.SNOW_GOLEM_SHOOT;
            case "raio" -> SoundEvents.TRIDENT_THUNDER.value();
            case "agua" -> SoundEvents.DROWNED_SHOOT;
            case "sombra", "magia" -> SoundEvents.EVOKER_CAST_SPELL;
            case "som" -> SoundEvents.NOTE_BLOCK_BELL.value();
            default -> SoundEvents.SHULKER_SHOOT;
        };
    }

    /** The sound of something of the element hitting. */
    public static SoundEvent impactSound(String element) {
        return switch (element) {
            case "fogo" -> SoundEvents.FIRECHARGE_USE;
            case "gelo" -> SoundEvents.GLASS_BREAK;
            case "raio" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case "agua" -> SoundEvents.GENERIC_SPLASH;
            case "sombra", "magia" -> SoundEvents.ILLUSIONER_CAST_SPELL;
            default -> SoundEvents.SHULKER_BULLET_HIT;
        };
    }

    /** What the element does to whoever it hits: fire burns, ice slows, gas poisons, sound stuns... */
    public static void elementHit(String element, LivingEntity target, ServerPlayer by) {
        switch (element) {
            case "fogo" -> target.igniteForSeconds(4.0F);
            case "gelo" -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 60));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), by);
            }
            case "gas" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), by);
            case "sombra" -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), by);
            case "agua" -> target.clearFire();
            case "som", "raio" -> {
                if (target instanceof Mob mob) Hits.stun(mob, by, 20);
            }
            default -> {
            }
        }
    }

    /** Shakes the camera of this player ({@code strength} 0 to 1). */
    public static void shake(ServerPlayer player, float strength, int ticks) {
        if (strength <= 0 || player.connection == null) return;
        ModNetwork.CHANNEL.send(new EngineFxPacket(strength, ticks), PacketDistributor.PLAYER.with(player));
    }
}
