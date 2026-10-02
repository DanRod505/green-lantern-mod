package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);

    public static final RegistryObject<SoundEvent> RING_ACTIVATE = register("ring_activate");
    public static final RegistryObject<SoundEvent> RING_DEACTIVATE = register("ring_deactivate");
    public static final RegistryObject<SoundEvent> BLAST_FIRE = register("blast_fire");
    public static final RegistryObject<SoundEvent> BLAST_IMPACT = register("blast_impact");
    public static final RegistryObject<SoundEvent> GUN_FIRE = register("gun_fire");
    public static final RegistryObject<SoundEvent> BUBBLE_UP = register("bubble_up");
    public static final RegistryObject<SoundEvent> BUBBLE_HIT = register("bubble_hit");
    public static final RegistryObject<SoundEvent> SAW_SUMMON = register("saw_summon");
    public static final RegistryObject<SoundEvent> SAW_LOOP = register("saw_loop");
    public static final RegistryObject<SoundEvent> SAW_CUT = register("saw_cut");
    public static final RegistryObject<SoundEvent> HAMMER_SUMMON = register("hammer_summon");
    public static final RegistryObject<SoundEvent> HAMMER_IMPACT = register("hammer_impact");
    public static final RegistryObject<SoundEvent> CHARGE_LOOP = register("charge_loop");
    public static final RegistryObject<SoundEvent> CHARGE_COMPLETE = register("charge_complete");
    public static final RegistryObject<SoundEvent> CONSTRUCT_SELECT = register("construct_select");
    public static final RegistryObject<SoundEvent> LOW_ENERGY = register("low_energy");
    public static final RegistryObject<SoundEvent> FLIGHT_WHOOSH = register("flight_whoosh");
    public static final RegistryObject<SoundEvent> FLIGHT_WIND = register("flight_wind");
    public static final RegistryObject<SoundEvent> SONIC_BOOM = register("sonic_boom");
    public static final RegistryObject<SoundEvent> FLIGHT_TAKEOFF = register("flight_takeoff");
    public static final RegistryObject<SoundEvent> FLIGHT_ROLL = register("flight_roll");
    public static final RegistryObject<SoundEvent> HERO_LANDING = register("hero_landing");
    /** Original flight theme, two aligned stems (see tools/generate_flight_audio.py). */
    public static final RegistryObject<SoundEvent> FLIGHT_THEME_BASE = register("flight_theme_base");
    public static final RegistryObject<SoundEvent> FLIGHT_THEME_PEAK = register("flight_theme_peak");
    /** "Lanterns" flight theme: the whole song, and the song from its climax onwards. */
    public static final RegistryObject<SoundEvent> LANTERNS_THEME_FULL = register("lanterns_theme_full");
    public static final RegistryObject<SoundEvent> LANTERNS_THEME_CLIMAX = register("lanterns_theme_climax");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }

    private ModSounds() {}
}
