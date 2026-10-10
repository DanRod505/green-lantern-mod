package com.danrod505.greenlantern.client.batman;

import com.danrod505.greenlantern.entity.BatmobileEntity;
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
 * Batman's loops: the wind in the cape of every gliding Batman, and the growl of every Batmobile with
 * a driver (the pitch follows the speed, the boost roars over it).
 */
public final class BatmanAudio {
    private static final Map<Integer, Loop> WIND = new HashMap<>();
    private static final Map<Integer, Loop> ENGINES = new HashMap<>();

    private BatmanAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            WIND.clear();
            ENGINES.clear();
            return;
        }
        for (var player : mc.level.players()) {
            if (BatmanVisuals.spread(player.getId(), 1.0F) > 0.5F && !WIND.containsKey(player.getId())) {
                Loop loop = new Loop(ModSounds.CAPE_WIND.get(), player, player == mc.player, false);
                WIND.put(player.getId(), loop);
                mc.getSoundManager().play(loop);
            }
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof BatmobileEntity car && car.getControllingPassenger() != null && !ENGINES.containsKey(car.getId())) {
                Loop loop = new Loop(ModSounds.BATMOBILE_ENGINE.get(), car, mc.player.getVehicle() == car, true);
                ENGINES.put(car.getId(), loop);
                mc.getSoundManager().play(loop);
            }
        }
        prune(mc, WIND);
        prune(mc, ENGINES);
    }

    private static void prune(Minecraft mc, Map<Integer, Loop> loops) {
        Iterator<Map.Entry<Integer, Loop>> it = loops.entrySet().iterator();
        while (it.hasNext()) {
            Loop loop = it.next().getValue();
            if (loop.isStopped() || !mc.getSoundManager().isActive(loop) && loop.age > 5) it.remove();
        }
    }

    static final class Loop extends AbstractTickableSoundInstance {
        private final Entity entity;
        private final boolean local;
        private final boolean engine;
        int age;

        Loop(SoundEvent sound, Entity entity, boolean local, boolean engine) {
            super(sound, engine ? SoundSource.NEUTRAL : SoundSource.PLAYERS, RandomSource.create());
            this.entity = entity;
            this.local = local;
            this.engine = engine;
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
            if (engine) {
                BatmobileEntity car = (BatmobileEntity) entity;
                if (car.getControllingPassenger() == null) {
                    target = 0.0F;
                } else {
                    double v = Math.max(Math.abs(car.drivingSpeed()), Math.hypot(car.getX() - car.xo, car.getZ() - car.zo));
                    float f = (float) Mth.clamp(v / 1.9, 0.0, 1.2);
                    target = 0.45F + 0.4F * f + (car.isBoosting() ? 0.15F : 0.0F);
                    pitch = 0.7F + 0.75F * f + (car.isBoosting() ? 0.15F : 0.0F);
                }
                target *= local ? 0.6F : 1.0F;
            } else {
                float s = BatmanVisuals.spread(entity.getId(), 1.0F);
                double speed = entity.getDeltaMovement().length();
                if (local) speed = Math.max(speed, GlideController.speed());
                target = s > 0.3F ? Mth.clamp(0.2F + (float) speed * 0.45F, 0.0F, 0.85F) * s : 0.0F;
                pitch = 0.8F + (float) Math.min(0.5, speed * 0.25);
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
