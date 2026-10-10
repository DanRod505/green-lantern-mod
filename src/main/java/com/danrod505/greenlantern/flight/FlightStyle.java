package com.danrod505.greenlantern.flight;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.ring.LanternHero;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * How a hero's power flight looks, sounds and handles (each flying hero gives its own: see
 * {@code HeroDefinition#flightStyle}); {@link FlightProfile} holds the speeds. Shared by the server
 * (takeoff, sonic boom and hero landing seen by others) and the client (the flyer's own effects,
 * HUD, trail and theme music).
 *
 * @param look             the trail, aura and body particles drawn by the client
 * @param takeoffPitch     pitch of the takeoff sound
 * @param boom             sound of breaking the sound barrier
 * @param shockwave        ground shockwave of the takeoff and the hero landing
 * @param glow             motes of the takeoff and the sonic rings
 * @param spark            sparks of the hero landing
 * @param sonicRing        rings left when breaking the sound barrier
 * @param landingRing      ring of the hero landing
 * @param landingPuff      puffs around the hero landing
 * @param dustRadius       radius of the takeoff debris
 * @param dustCount        amount of takeoff debris
 * @param landingDamage    damage source of the hero landing (level, flyer)
 * @param turn             how sharply the flyer turns at top speed
 * @param lift             how much a climb lifts the flyer
 * @param crashSpeed       speed above which hitting a wall stuns
 * @param hud              colours of the flight HUD
 * @param themeBase        base layer of the flight theme
 * @param themePeak        peak layer of the flight theme
 * @param peakNearTopSpeed the peak layer joins near top speed instead of after breaking the sound barrier
 */
public record FlightStyle(Look look, float takeoffPitch, Supplier<SoundEvent> boom, Supplier<? extends ParticleOptions> shockwave,
        Supplier<? extends ParticleOptions> glow, Supplier<? extends ParticleOptions> spark, Supplier<? extends ParticleOptions> sonicRing,
        Supplier<? extends ParticleOptions> landingRing, Supplier<? extends ParticleOptions> landingPuff, double dustRadius, int dustCount,
        BiFunction<Level, Player, DamageSource> landingDamage, double turn, double lift, double crashSpeed, Hud hud, Supplier<SoundEvent> themeBase,
        Supplier<SoundEvent> themePeak, boolean peakNearTopSpeed) {

    /** The client's trail, aura and body particles. */
    public enum Look {
        /** Green hard-light ribbon, aura and glow motes. */
        LANTERN,
        /** White vapor trail with blue and red glow. */
        SUPERMAN,
        /** Golden, crimson-edged trail and sparks of divine light. */
        AMAZON
    }

    /**
     * Colours of the flight HUD.
     *
     * @param flash           screen flash when breaking the sound barrier (RGB)
     * @param streak          tint of some speed streaks (RGB)
     * @param edge            screen edge glow, even bands (RGB)
     * @param edge2           screen edge glow, odd bands (RGB)
     * @param mach            Mach number (ARGB)
     * @param machSupersonic  Mach number past Mach 1 (ARGB)
     * @param barMax          Mach shown by a full bar
     * @param bar             Mach bar (ARGB)
     * @param barSupersonic   Mach bar past Mach 1 (ARGB)
     * @param supersonicLabel the "supersonic" label (ARGB)
     */
    public record Hud(int flash, int streak, int edge, int edge2, int mach, int machSupersonic, float barMax, int bar, int barSupersonic,
            int supersonicLabel) {}

    /** The suited hero's flight style (the Lantern's when none). */
    public static FlightStyle of(Player player) {
        FlightStyle style = HeroRegistry.suited(player).map(HeroDefinition::flightStyle).orElse(null);
        return style != null ? style : LanternHero.FLIGHT_STYLE;
    }
}
