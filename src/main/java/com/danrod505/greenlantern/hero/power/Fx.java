package com.danrod505.greenlantern.hero.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Sounds and particles that powers share. */
public final class Fx {
    private Fx() {
    }

    /** A sound everyone nearby hears, at the entity. */
    public static void play(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A sound only this player hears (a power's inner sense, a warning). */
    public static void hear(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }

    /** The height of the ground under {@code at}, looking at most 6 blocks down ({@code at.y} when there is none). */
    public static double groundBelow(ServerLevel level, Vec3 at) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return pos.getY();
            pos.move(Direction.DOWN);
        }
        return at.y;
    }

    /** Bits of the ground thrown up in a disc of {@code radius} around {@code center} (a landing, a stomp). */
    public static void dust(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double r = level.random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockState state = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 3, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }

    /** A flat ring of particles flying outwards from {@code center} (a shockwave, a pulse). */
    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            level.sendParticles(particle, center.x + cos * 0.6, center.y, center.z + sin * 0.6, 0, cos, 0.02, sin, radius * 0.1);
        }
    }

    /** Particles along a line, {@code perBlock} per block (a beam, a trail). */
    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double perBlock) {
        Vec3 step = to.subtract(from);
        int n = Math.max(1, (int) Math.ceil(step.length() * perBlock));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(step.scale(i / (double) n));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
