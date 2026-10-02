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
 * The theme has two aligned layers: the base layer fades in once you fly fast, and the epic peak
 * layer joins it the moment you break the sound barrier.
 */
public final class FlightAudio {
    private static final Map<Integer, WindSound> WIND = new HashMap<>();
    private static ThemeSound base;
    private static ThemeSound peak;
    private static int fastTicks;
    private static int idleTicks;
    private static int supersonicMemory;

    private FlightAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopAll();
            return;
        }
        tickWind(mc);
        tickMusic(mc);
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
        boolean enabled = GLClientConfig.FLIGHT_MUSIC.get();
        boolean fast = FlightController.isPowerFlying() && FlightController.speed() > GLConfig.CRUISE_SPEED.get() * 1.25;
        if (fast) {
            fastTicks++;
            idleTicks = 0;
        } else {
            idleTicks++;
            fastTicks = 0;
        }
        supersonicMemory = FlightController.isSupersonic() ? 80 : Math.max(0, supersonicMemory - 1);
        boolean playing = base != null && !base.isStopped();
        boolean wantBase = enabled && (fastTicks > 15 || (playing && idleTicks < 100));
        boolean wantPeak = wantBase && supersonicMemory > 0;

        if (wantBase && !playing) {
            base = new ThemeSound(ModSounds.FLIGHT_THEME_BASE.get(), 0.025F);
            peak = new ThemeSound(ModSounds.FLIGHT_THEME_PEAK.get(), 0.09F);
            mc.getSoundManager().play(base);
            mc.getSoundManager().play(peak);
            playing = true;
        }
        if (playing) {
            base.target = wantBase ? 1.0F : 0.0F;
            peak.target = wantPeak ? 1.0F : 0.0F;
            // Our theme replaces the background music while it plays.
            mc.getMusicManager().stopPlaying();
        }
    }

    private static void stopAll() {
        WIND.clear();
        base = null;
        peak = null;
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

    /** One layer of the flight theme, faded in and out smoothly. */
    static final class ThemeSound extends AbstractTickableSoundInstance {
        float target = 1.0F;
        private final float fadeIn;

        ThemeSound(SoundEvent event, float fadeIn) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.fadeIn = fadeIn;
            this.looping = true;
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
            float max = GLClientConfig.FLIGHT_MUSIC_VOLUME.get().floatValue();
            float goal = target * max;
            if (volume < goal) volume = Math.min(goal, volume + fadeIn);
            else volume = Math.max(goal, volume - 0.012F);
            // The base layer decides when the theme ends; both layers stop together.
            if (this == base && target == 0.0F && volume <= 0.001F) {
                stop();
                if (peak != null) peak.stop();
            }
        }
    }
}
