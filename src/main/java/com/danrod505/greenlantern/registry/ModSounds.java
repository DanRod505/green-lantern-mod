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
    public static final RegistryObject<SoundEvent> DRILL_SUMMON = register("drill_summon");
    public static final RegistryObject<SoundEvent> DRILL_LOOP = register("drill_loop");
    public static final RegistryObject<SoundEvent> DRILL_GRIND = register("drill_grind");
    public static final RegistryObject<SoundEvent> MECHA_SUMMON = register("mecha_summon");
    public static final RegistryObject<SoundEvent> MECHA_STEP = register("mecha_step");
    public static final RegistryObject<SoundEvent> MECHA_THRUSTER = register("mecha_thruster");
    public static final RegistryObject<SoundEvent> MECHA_LASER = register("mecha_laser");
    public static final RegistryObject<SoundEvent> MECHA_MISSILE = register("mecha_missile");
    public static final RegistryObject<SoundEvent> MECHA_LAND = register("mecha_land");
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
    public static final RegistryObject<SoundEvent> PORTAL_OPEN = register("portal_open");
    public static final RegistryObject<SoundEvent> PORTAL_HUM = register("portal_hum");
    public static final RegistryObject<SoundEvent> PORTAL_TRAVEL = register("portal_travel");
    /** Original ambient music of Oa (see tools/generate_oa_music.py), played by the dimension type. */
    public static final RegistryObject<SoundEvent> MUSIC_OA = register("music.oa");
    /** Original flight theme, two aligned stems (see tools/generate_flight_audio.py). */
    public static final RegistryObject<SoundEvent> FLIGHT_THEME_BASE = register("flight_theme_base");
    public static final RegistryObject<SoundEvent> FLIGHT_THEME_PEAK = register("flight_theme_peak");

    // ---- The Flash (all original, see tools/generate_flash_audio.py) ----------------------------
    public static final RegistryObject<SoundEvent> FLASH_SUIT_UP = register("flash_suit_up");
    public static final RegistryObject<SoundEvent> FLASH_SUIT_DOWN = register("flash_suit_down");
    public static final RegistryObject<SoundEvent> SPEED_START = register("speed_start");
    public static final RegistryObject<SoundEvent> SPEED_CRACKLE = register("speed_crackle");
    public static final RegistryObject<SoundEvent> SPEED_BOOM = register("speed_boom");
    public static final RegistryObject<SoundEvent> SPEED_SKID = register("speed_skid");
    public static final RegistryObject<SoundEvent> SUPER_JUMP = register("super_jump");
    public static final RegistryObject<SoundEvent> SPEED_LANDING = register("speed_landing");
    public static final RegistryObject<SoundEvent> TORNADO_LOOP = register("tornado_loop");
    public static final RegistryObject<SoundEvent> TORNADO_START = register("tornado_start");
    public static final RegistryObject<SoundEvent> PHASE_START = register("phase_start");
    public static final RegistryObject<SoundEvent> PHASE_LOOP = register("phase_loop");
    public static final RegistryObject<SoundEvent> PHASE_END = register("phase_end");
    public static final RegistryObject<SoundEvent> LIGHTNING_THROW = register("lightning_throw");
    public static final RegistryObject<SoundEvent> LIGHTNING_HIT = register("lightning_hit");
    public static final RegistryObject<SoundEvent> POWER_SELECT = register("power_select");
    /** Original running theme, two aligned stems like the flight theme. */
    public static final RegistryObject<SoundEvent> SPEED_THEME_BASE = register("speed_theme_base");
    public static final RegistryObject<SoundEvent> SPEED_THEME_PEAK = register("speed_theme_peak");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }

    private ModSounds() {}
}
