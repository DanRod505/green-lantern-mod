package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The skin chosen on a character's item and the skins it has unlocked besides the default one
 * (alternative skins come from Suit Molds, stage 15; in creative every skin is unlocked).
 */
public record SkinState(String selected, List<String> unlocked) {
    public static final Codec<SkinState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("escolhida").forGetter(SkinState::selected),
            Codec.STRING.listOf().optionalFieldOf("liberadas", List.of()).forGetter(SkinState::unlocked)
    ).apply(i, SkinState::new));

    public static final StreamCodec<ByteBuf, SkinState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkinState::selected,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SkinState::unlocked,
            SkinState::new);

    public SkinState unlock(String skin) {
        if (unlocked.contains(skin)) return this;
        List<String> list = new ArrayList<>(unlocked);
        list.add(skin);
        return new SkinState(selected, List.copyOf(list));
    }

    public SkinState select(String skin) {
        return new SkinState(skin, unlocked);
    }
}
