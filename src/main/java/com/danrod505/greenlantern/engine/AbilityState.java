package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * What the item of a character of the engine remembers about its abilities: when each one is
 * ready again (game time, in ticks), the charge of the Supreme (0 to {@link #SUPREME_FULL}, filled
 * by dealing and taking damage) and when an ability was last used. Kept on the item so the client
 * draws the cooldowns and the Supreme bar without a packet of its own.
 */
public record AbilityState(List<Long> readyAt, int supreme, long lastUse) {
    public static final int SUPREME_FULL = 1000;
    public static final AbilityState EMPTY = new AbilityState(List.of(), 0, 0L);

    public static final Codec<AbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.listOf().optionalFieldOf("pronta_em", List.of()).forGetter(AbilityState::readyAt),
            Codec.INT.optionalFieldOf("suprema", 0).forGetter(AbilityState::supreme),
            Codec.LONG.optionalFieldOf("ultimo_uso", 0L).forGetter(AbilityState::lastUse)
    ).apply(i, AbilityState::new));

    public static final StreamCodec<ByteBuf, AbilityState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), AbilityState::readyAt,
            ByteBufCodecs.VAR_INT, AbilityState::supreme,
            ByteBufCodecs.VAR_LONG, AbilityState::lastUse,
            AbilityState::new);

    public long readyAt(int index) {
        return index < readyAt.size() ? readyAt.get(index) : 0L;
    }

    /** Ticks until ability {@code index} is ready (0 when it is). */
    public long remaining(int index, long now) {
        return Math.max(0L, readyAt(index) - now);
    }

    public AbilityState withCooldown(int index, long readyTick, long now) {
        List<Long> list = new ArrayList<>(readyAt);
        while (list.size() <= index) list.add(0L);
        list.set(index, readyTick);
        return new AbilityState(List.copyOf(list), supreme, now);
    }

    public AbilityState withSupreme(int value) {
        return new AbilityState(readyAt, Mth.clamp(value, 0, SUPREME_FULL), lastUse);
    }

    public boolean supremeFull() {
        return supreme >= SUPREME_FULL;
    }

    public float supremeFraction() {
        return Mth.clamp((float) supreme / SUPREME_FULL, 0.0F, 1.0F);
    }
}
