package com.danrod505.greenlantern.client.aqua;

import com.danrod505.greenlantern.GLClientConfig;
import com.danrod505.greenlantern.client.flight.FlightAudio;
import com.danrod505.greenlantern.entity.AtlanteanMountEntity;
import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * The rush of water around every Aquaman swimming fast (a loop that rises with speed) and the
 * swimming theme of the local player: the base stem fades in once the swim gets going (or riding a
 * sea mount at speed) and the peak stem joins gradually as the sprint builds up.
 */
public final class SwimAudio {
    private static final Map<Integer, RushSound> RUSH = new HashMap<>();
    private static FlightAudio.ThemeSound base;
    private static FlightAudio.ThemeSound peak;
    private static int swimTicks;
    private static int idleTicks;

    private SwimAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            RUSH.clear();
            stopMusic(mc);
            return;
        }
        tickMusic(mc);
        for (SwimVisuals.Visual visual : SwimVisuals.all()) {
            if (visual.swimming() && !RUSH.containsKey(visual.entityId)) {
                Entity entity = mc.level.getEntity(visual.entityId);
                if (entity == null) continue;
                RushSound sound = new RushSound(entity, entity == mc.player);
                RUSH.put(visual.entityId, sound);
                mc.getSoundManager().play(sound);
            }
        }
        Iterator<Map.Entry<Integer, RushSound>> it = RUSH.entrySet().iterator();
        while (it.hasNext()) {
            RushSound sound = it.next().getValue();
            if (sound.isStopped() || !mc.getSoundManager().isActive(sound) && sound.age > 5) it.remove();
        }
    }

    /** Short description of the music state (for logs and debugging). */
    public static String describeMusic() {
        if (base == null) return "silent";
        return String.format("base=%.2f peak=%.2f", base.level(), peak == null ? 0F : peak.level());
    }

    /** 0-1 intensity of the swim right now: 0 when not swimming (or riding) through the sea. */
    private static float intensity(LocalPlayer player) {
        if (SwimController.isSwimming()) {
            return SwimController.speed() > 0.5 ? 0.25F + 0.75F * SwimController.boost() : 0.0F;
        }
        if ((player.getVehicle() instanceof GreatWhiteSharkEntity || player.getVehicle() instanceof AtlanteanMountEntity)
                && player.getVehicle().isInWater()) {
            var v = player.getVehicle();
            double speed = Math.sqrt(v.distanceToSqr(v.xo, v.yo, v.zo));
            return speed > 0.35 ? (float) Mth.clamp(speed / 1.8, 0.25, 1.0) : 0.0F;
        }
        return 0.0F;
    }

    private static void tickMusic(Minecraft mc) {
        float intensity = intensity(mc.player);
        if (intensity > 0.0F) {
            swimTicks++;
            idleTicks = 0;
        } else {
            idleTicks++;
            swimTicks = 0;
        }
        boolean playing = base != null && !base.isStopped();
        // Starts after a moment of swimming; a short pause (a breath at the surface) doesn't stop it.
        boolean wantBase = GLClientConfig.FLIGHT_MUSIC.get() && (swimTicks > 12 || (playing && idleTicks < 120));
        if (wantBase && !playing) {
            base = new FlightAudio.ThemeSound(ModSounds.SWIM_THEME_BASE.get(), 0.02F, 0.008F);
            peak = new FlightAudio.ThemeSound(ModSounds.SWIM_THEME_PEAK.get(), 0.015F, 0.01F);
            peak.stopWhenSilent = false;
            mc.getSoundManager().play(base);
            mc.getSoundManager().play(peak);
            playing = true;
        }
        if (playing) {
            base.target = wantBase ? 1.0F : 0.0F;
            // The peak stem follows the build-up of the sprint: it starts joining halfway.
            peak.target = wantBase ? Mth.clamp((intensity - 0.45F) / 0.45F, 0.0F, 1.0F) : 0.0F;
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

    static final class RushSound extends AbstractTickableSoundInstance {
        private final Entity entity;
        private final boolean local;
        int age;

        RushSound(Entity entity, boolean local) {
            super(ModSounds.SWIM_LOOP.get(), SoundSource.PLAYERS, RandomSource.create());
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
            SwimVisuals.Visual visual = SwimVisuals.get(entity.getId());
            if (entity.isRemoved() || visual == null) {
                stop();
                return;
            }
            float f = Mth.clamp(visual.speed / 2.4F, 0.0F, 1.5F);
            float target = visual.speed > 0.4F ? Mth.clamp(0.15F + 0.7F * f, 0.0F, 0.9F) : 0.0F;
            pitch = 0.75F + 0.45F * f;
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
