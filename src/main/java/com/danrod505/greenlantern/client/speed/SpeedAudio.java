package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.GLClientConfig;
import com.danrod505.greenlantern.client.flight.FlightAudio;
import com.danrod505.greenlantern.flash.PhaseState;
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
import net.minecraft.world.entity.player.Player;

/**
 * Speedster audio: the rush of air around every running speedster, the hum of vibrating molecules
 * while phasing and the running theme (a base layer once the run gets going plus a peak layer
 * that joins past the sound barrier).
 */
public final class SpeedAudio {
    private static final Map<Integer, FollowSound> WIND = new HashMap<>();
    private static FollowSound phase;
    private static FlightAudio.ThemeSound base;
    private static FlightAudio.ThemeSound peak;
    private static int fastTicks;
    private static int idleTicks;
    private static int supersonicMemory;

    private SpeedAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopMusic(mc);
            WIND.clear();
            phase = null;
            return;
        }
        tickWind(mc);
        tickPhase(mc);
        tickMusic(mc);
    }

    /** Short description of the music state (for logs and debugging). */
    public static String describeMusic() {
        if (base == null) return "silent";
        return String.format("base=%.2f peak=%.2f", base.level(), peak == null ? 0F : peak.level());
    }

    private static void tickWind(Minecraft mc) {
        for (SpeedVisuals.Visual visual : SpeedVisuals.all()) {
            if (visual.running() && visual.speed > 0.4F && !WIND.containsKey(visual.entityId)) {
                Entity entity = mc.level.getEntity(visual.entityId);
                if (entity == null) continue;
                FollowSound sound = new FollowSound(ModSounds.FLIGHT_WIND.get(), entity, entity == mc.player, FollowSound.Kind.WIND);
                WIND.put(visual.entityId, sound);
                mc.getSoundManager().play(sound);
            }
        }
        Iterator<Map.Entry<Integer, FollowSound>> it = WIND.entrySet().iterator();
        while (it.hasNext()) {
            FollowSound sound = it.next().getValue();
            if (sound.isStopped() || !mc.getSoundManager().isActive(sound) && sound.age > 5) it.remove();
        }
    }

    private static void tickPhase(Minecraft mc) {
        boolean phasing = PhaseState.isPhasing(mc.player);
        if (phasing && (phase == null || phase.isStopped())) {
            phase = new FollowSound(ModSounds.PHASE_LOOP.get(), mc.player, true, FollowSound.Kind.PHASE);
            mc.getSoundManager().play(phase);
        }
        if (phase != null && phase.isStopped()) phase = null;
    }

    private static void tickMusic(Minecraft mc) {
        boolean fast = SpeedController.isRunning() && SpeedController.mach() > 0.45F;
        if (fast) {
            fastTicks++;
            idleTicks = 0;
        } else {
            idleTicks++;
            fastTicks = 0;
        }
        supersonicMemory = SpeedController.isSupersonic() ? 80 : Math.max(0, supersonicMemory - 1);
        boolean playing = base != null && !base.isStopped();
        boolean wantBase = GLClientConfig.FLIGHT_MUSIC.get() && (fastTicks > 10 || (playing && idleTicks < 100));
        boolean wantPeak = wantBase && supersonicMemory > 0;

        if (wantBase && !playing) {
            base = new FlightAudio.ThemeSound(ModSounds.SPEED_THEME_BASE.get(), 0.04F, 0.012F);
            peak = new FlightAudio.ThemeSound(ModSounds.SPEED_THEME_PEAK.get(), 0.1F, 0.012F);
            peak.stopWhenSilent = false;
            mc.getSoundManager().play(base);
            mc.getSoundManager().play(peak);
            playing = true;
        }
        if (playing) {
            base.target = wantBase ? 1.0F : 0.0F;
            peak.target = wantPeak ? 1.0F : 0.0F;
            mc.getMusicManager().stopPlaying();
        } else if (peak != null) {
            mc.getSoundManager().stop(peak);
            peak = null;
        }
    }

    private static void stopMusic(Minecraft mc) {
        if (base != null) mc.getSoundManager().stop(base);
        if (peak != null) mc.getSoundManager().stop(peak);
        base = null;
        peak = null;
    }

    /** A loop that follows a speedster (non-positional for the local player). */
    static final class FollowSound extends AbstractTickableSoundInstance {
        enum Kind { WIND, PHASE }

        private final Entity entity;
        private final boolean local;
        private final Kind kind;
        int age;

        FollowSound(SoundEvent event, Entity entity, boolean local, Kind kind) {
            super(event, SoundSource.PLAYERS, RandomSource.create());
            this.entity = entity;
            this.local = local;
            this.kind = kind;
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
            if (entity.isRemoved()) {
                stop();
                return;
            }
            float target;
            if (kind == Kind.WIND) {
                SpeedVisuals.Visual visual = SpeedVisuals.get(entity.getId());
                if (visual == null) {
                    stop();
                    return;
                }
                float f = Mth.clamp(visual.speed / 2.4F, 0.0F, 1.6F);
                target = visual.running() && visual.speed > 0.35F ? Mth.clamp(0.2F + 0.65F * f, 0.0F, 1.0F) : 0.0F;
                pitch = 0.7F + 0.6F * f;
            } else {
                target = entity instanceof Player player && PhaseState.isPhasing(player) ? 0.8F : 0.0F;
                pitch = 1.0F + 0.05F * Mth.sin(age * 0.7F);
            }
            volume += (target - volume) * 0.2F;
            if (!local) {
                x = entity.getX();
                y = entity.getY();
                z = entity.getZ();
            }
            if (target == 0.0F && volume < 0.01F && age > 10) stop();
        }
    }
}
