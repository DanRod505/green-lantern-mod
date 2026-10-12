package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the item of a character of the engine remembers about its abilities: when each one is
 * ready again (game time, in ticks) and when an ability was last used. Kept on the item so the
 * client draws the cooldowns without a packet of its own. (Items of 2.5.0 also kept the charge of
 * the old Supreme bar, {@code "suprema"}: it is just ignored now.)
 */
public record AbilityState(List<Long> readyAt, long lastUse) {
    public static final AbilityState EMPTY = new AbilityState(List.of(), 0L);

    public static final Codec<AbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.listOf().optionalFieldOf("pronta_em", List.of()).forGetter(AbilityState::readyAt),
            Codec.LONG.optionalFieldOf("ultimo_uso", 0L).forGetter(AbilityState::lastUse)
    ).apply(i, AbilityState::new));

    public static final StreamCodec<ByteBuf, AbilityState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), AbilityState::readyAt,
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
        return new AbilityState(List.copyOf(list), now);
    }
}
