package com.danrod505.greenlantern.client.aqua;

import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** The rush of water around every Aquaman swimming fast (a loop that rises with speed). */
public final class SwimAudio {
    private static final Map<Integer, RushSound> RUSH = new HashMap<>();

    private SwimAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            RUSH.clear();
            return;
        }
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
