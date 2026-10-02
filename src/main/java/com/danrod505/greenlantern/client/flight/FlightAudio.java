package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GLClientConfig;
import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Flight audio: a wind rush that rises with speed for every flying Lantern, and the flight theme.
 * <ul>
 *     <li><b>LANTERNS</b> (default): the song plays from its start when you fly fast and jumps to
 *     its climax, with a crossfade, the moment you break the sound barrier.</li>
 *     <li><b>ORIGINAL</b>: the procedural theme with two aligned layers; the epic layer joins at
 *     the sound barrier.</li>
 * </ul>
 * Chosen with {@code flightTheme} in greenlantern-client.toml.
 */
public final class FlightAudio {
    /** Where the climax starts in lanterns_theme_full.ogg (see tools/import_flight_music.py). */
    public static final float CLIMAX_SECONDS = 97.45F;

    private static final Map<Integer, WindSound> WIND = new HashMap<>();
    private static int fastTicks;
    private static int idleTicks;
    private static int supersonicMemory;
    private static boolean wasSupersonic;
    private static GLClientConfig.FlightTheme activeTheme;

    // ORIGINAL theme: two aligned looping layers.
    private static ThemeSound base;
    private static ThemeSound peak;

    // LANTERNS theme: the current section of the song and the one fading out after a jump.
    private static ThemeSound song;
    private static ThemeSound fadingOut;
    private static float songOffset;
    private static int songTicks;

    private FlightAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopMusic(mc);
            WIND.clear();
            return;
        }
        tickWind(mc);
        tickMusic(mc);
    }

    /** Seconds into the Lanterns song currently playing, or -1 when it isn't playing. */
    public static float songPosition() {
        return song == null ? -1.0F : songOffset + songTicks / 20.0F;
    }

    /** Short description of the music state (for logs and debugging). */
    public static String describeMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (song != null) {
            return String.format("song pos=%.1fs section=%s active=%s vol=%.2f", songPosition(),
                    songOffset > 0 ? "climax" : "full", mc.getSoundManager().isActive(song), song.getVolume());
        }
        if (base != null) {
            return String.format("layers base=%.2f peak=%.2f", base.getVolume(), peak == null ? 0F : peak.getVolume());
        }
        return "silent";
    }

    private static void tickWind(Minecraft mc) {
        for (FlightVisuals.Visual visual : FlightVisuals.all()) {
            if (visual.speed > 0.5F && !WIND.containsKey(visual.entityId)) {
                Entity entity = mc.level.getEntity(visual.entityId);
                if (entity == null) continue;
                WindSound sound = new WindSound(entity, entity == mc.player);
                WIND.put(visual.entityId, sound);
                mc.getSoundManager().play(sound);
            }
        }
        Iterator<Map.Entry<Integer, WindSound>> it = WIND.entrySet().iterator();
        while (it.hasNext()) {
            WindSound sound = it.next().getValue();
            if (sound.isStopped() || !mc.getSoundManager().isActive(sound) && sound.age > 5) it.remove();
        }
    }

    private static void tickMusic(Minecraft mc) {
        boolean fast = FlightController.isPowerFlying() && FlightController.speed() > GLConfig.CRUISE_SPEED.get() * 1.25;
        if (fast) {
            fastTicks++;
            idleTicks = 0;
        } else {
            idleTicks++;
            fastTicks = 0;
        }
        boolean supersonic = FlightController.isSupersonic();
        boolean boom = supersonic && !wasSupersonic;
        wasSupersonic = supersonic;
        supersonicMemory = supersonic ? 80 : Math.max(0, supersonicMemory - 1);

        GLClientConfig.FlightTheme theme = GLClientConfig.FLIGHT_THEME.get();
        if (theme != activeTheme) {
            stopMusic(mc);
            activeTheme = theme;
        }
        boolean wantMusic = GLClientConfig.FLIGHT_MUSIC.get() && (fastTicks > 15 || (isPlaying() && idleTicks < 100));
        if (theme == GLClientConfig.FlightTheme.ORIGINAL) {
            tickLayered(mc, wantMusic, wantMusic && supersonicMemory > 0);
        } else {
            tickSong(mc, wantMusic, boom);
        }
        if (isPlaying()) {
            // Our theme replaces the background music while it plays.
            mc.getMusicManager().stopPlaying();
        }
    }

    private static boolean isPlaying() {
        return (base != null && !base.isStopped()) || (song != null && !song.isStopped());
    }

    private static void tickLayered(Minecraft mc, boolean wantBase, boolean wantPeak) {
        boolean playing = base != null && !base.isStopped();
        if (wantBase && !playing) {
            base = new ThemeSound(ModSounds.FLIGHT_THEME_BASE.get(), true, 0.025F, 0.012F);
            peak = new ThemeSound(ModSounds.FLIGHT_THEME_PEAK.get(), true, 0.09F, 0.012F);
            peak.stopWhenSilent = false;
            mc.getSoundManager().play(base);
            mc.getSoundManager().play(peak);
            playing = true;
        }
        if (playing) {
            base.target = wantBase ? 1.0F : 0.0F;
            peak.target = wantPeak ? 1.0F : 0.0F;
        } else if (peak != null) {
            // Both layers end together.
            mc.getSoundManager().stop(peak);
            peak = null;
        }
    }

    private static void tickSong(Minecraft mc, boolean wantMusic, boolean boom) {
        // A section that finished playing (the song ended) restarts from the beginning.
        if (song != null && (song.isStopped() || (songTicks > 40 && !mc.getSoundManager().isActive(song)))) {
            song = null;
        }
        if (song == null) {
            if (!wantMusic) return;
            startSection(mc, ModSounds.LANTERNS_THEME_FULL.get(), 0.0F, 0.02F);
        }
        songTicks++;
        float position = songOffset + songTicks / 20.0F;
        // Breaking the sound barrier before the climax: jump straight to it, in sync with the boom.
        if (boom && wantMusic && position < CLIMAX_SECONDS - 1.0F) {
            fadingOut = song;
            fadingOut.target = 0.0F;
            fadingOut.fadeOut = 0.08F;
            startSection(mc, ModSounds.LANTERNS_THEME_CLIMAX.get(), CLIMAX_SECONDS, 1.0F);
        }
        song.target = wantMusic ? 1.0F : 0.0F;
        if (fadingOut != null && fadingOut.isStopped()) fadingOut = null;
    }

    private static void startSection(Minecraft mc, SoundEvent event, float offset, float fadeIn) {
        song = new ThemeSound(event, false, fadeIn, 0.012F);
        song.target = 1.0F;
        songOffset = offset;
        songTicks = 0;
        mc.getSoundManager().play(song);
    }

    private static void stopMusic(Minecraft mc) {
        for (ThemeSound sound : new ThemeSound[] {base, peak, song, fadingOut}) {
            if (sound != null) mc.getSoundManager().stop(sound);
        }
        base = null;
        peak = null;
        song = null;
        fadingOut = null;
    }

    /** Wind rush following a flying player (non-positional for the local player). */
    static final class WindSound extends AbstractTickableSoundInstance {
        private final Entity entity;
        private final boolean local;
        int age;

        WindSound(Entity entity, boolean local) {
            super(ModSounds.FLIGHT_WIND.get(), SoundSource.PLAYERS, RandomSource.create());
            this.entity = entity;
            this.local = local;
            this.looping = true;
            this.volume = 0.0F;
            if (local) {
                this.relative = true;
                this.attenuation = SoundInstance.Attenuation.NONE;
            } else {
                this.x = entity.getX();
                this.y = entity.getY();
                this.z = entity.getZ();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            age++;
            FlightVisuals.Visual visual = FlightVisuals.get(entity.getId());
            if (entity.isRemoved() || visual == null) {
                stop();
                return;
            }
            float barrier = GLConfig.SOUND_BARRIER_SPEED.get().floatValue();
            float f = Mth.clamp(visual.speed / barrier, 0.0F, 1.4F);
            float target = visual.speed > 0.4F ? Mth.clamp(0.15F + 0.7F * f, 0.0F, 1.0F) : 0.0F;
            volume += (target - volume) * 0.15F;
            pitch = 0.6F + 0.75F * f;
            if (!local) {
                x = entity.getX();
                y = entity.getY();
                z = entity.getZ();
            }
            if (target == 0.0F && volume < 0.01F && age > 10) stop();
        }
    }

    /** A piece of the flight theme, faded in and out smoothly; stops itself once faded out. */
    static final class ThemeSound extends AbstractTickableSoundInstance {
        float target;
        float fadeIn;
        float fadeOut;
        /** Silent layers that must stay in sync (the ORIGINAL peak layer) keep playing at volume 0. */
        boolean stopWhenSilent = true;
        private int age;

        ThemeSound(SoundEvent event, boolean looping, float fadeIn, float fadeOut) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.fadeIn = fadeIn;
            this.fadeOut = fadeOut;
            this.looping = looping;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.0F;
            this.target = 0.0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            age++;
            float goal = target * GLClientConfig.FLIGHT_MUSIC_VOLUME.get().floatValue();
            if (volume < goal) volume = Math.min(goal, volume + fadeIn);
            else volume = Math.max(goal, volume - fadeOut);
            if (stopWhenSilent && target == 0.0F && volume <= 0.001F && age > 5) stop();
        }
    }
}
