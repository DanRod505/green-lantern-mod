package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Damage types are data driven: see {@code data/greenlantern/damage_type/}. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> HARD_LIGHT = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("hard_light"));

    public static DamageSource hardLight(Level level, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HARD_LIGHT), direct, attacker);
    }

    public static final ResourceKey<DamageType> SPEED_FORCE = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("speed_force"));

    public static DamageSource speedForce(Level level, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SPEED_FORCE), direct, attacker);
    }

    public static final ResourceKey<DamageType> SHARK_BITE = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("shark_bite"));

    public static DamageSource sharkBite(Level level, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SHARK_BITE), direct, attacker);
    }

    public static final ResourceKey<DamageType> KRAKEN = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("kraken"));

    public static DamageSource kraken(Level level, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(KRAKEN), direct, attacker);
    }

    public static final ResourceKey<DamageType> HEAT_VISION = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("heat_vision"));

    public static DamageSource heatVision(Level level, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HEAT_VISION), attacker, attacker);
    }

    public static final ResourceKey<DamageType> SUPER_PUNCH = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("super_punch"));

    public static DamageSource superPunch(Level level, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SUPER_PUNCH), attacker, attacker);
    }

    public static final ResourceKey<DamageType> SUPER_BREATH = ResourceKey.create(Registries.DAMAGE_TYPE, GreenLantern.id("super_breath"));

    public static DamageSource superBreath(Level level, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SUPER_BREATH), attacker, attacker);
    }

    private ModDamageTypes() {}
}
