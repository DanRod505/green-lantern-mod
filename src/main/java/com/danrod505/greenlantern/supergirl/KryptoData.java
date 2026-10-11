package com.danrod505.greenlantern.supergirl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Krypto as the Argo Pendant remembers him while he is away (before the first suit up, while the
 * suit is off, or while he recovers): whether he already met her, the name she gave him, his health
 * and when he comes back after his health ran out.
 *
 * @param met    he already came down from the sky once
 * @param name   the name on his name tag ("" for none)
 * @param health his health when he left (0 or less: full health)
 * @param backAt game time he comes back after his health ran out (0: he can come right away)
 */
public record KryptoData(boolean met, String name, float health, long backAt) {
    public static final KryptoData NEW = new KryptoData(false, "", 0.0F, 0L);

    public static final Codec<KryptoData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("met", false).forGetter(KryptoData::met),
            Codec.STRING.optionalFieldOf("name", "").forGetter(KryptoData::name),
            Codec.FLOAT.optionalFieldOf("health", 0.0F).forGetter(KryptoData::health),
            Codec.LONG.optionalFieldOf("back_at", 0L).forGetter(KryptoData::backAt)).apply(instance, KryptoData::new));

    public static final StreamCodec<ByteBuf, KryptoData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KryptoData::met,
            ByteBufCodecs.STRING_UTF8, KryptoData::name,
            ByteBufCodecs.FLOAT, KryptoData::health,
            ByteBufCodecs.VAR_LONG, KryptoData::backAt,
            KryptoData::new);
}
