package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: the keys of the wheel of the new model (catalog, stage 14; every hero since the
 * adaptation of the first 9): the power key (or a direct key) uses an ability with how it was pressed
 * (a tap, holding it, letting it go after holding: the function that goes off depends on it), and the
 * skin key (characters of the engine only).
 *
 * @param action  {@link #USE} or {@link #SKIN}
 * @param ability the ability to use, -1 for the chosen one
 * @param press   how the key was pressed ({@link AbilityFunctions.Press} index)
 */
public record EnginePacket(int action, int ability, int press) {
    public static final int USE = 0;
    public static final int SKIN = 1;

    public static final StreamCodec<RegistryFriendlyByteBuf, EnginePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnginePacket::action,
            ByteBufCodecs.VAR_INT, p -> p.ability + 1,
            ByteBufCodecs.VAR_INT, EnginePacket::press,
            (action, ability, press) -> new EnginePacket(action, ability - 1, press));

    public static EnginePacket use(int ability, AbilityFunctions.Press press) {
        return new EnginePacket(USE, ability, press.ordinal());
    }

    public static EnginePacket skin() {
        return new EnginePacket(SKIN, -1, 0);
    }

    public static void handle(EnginePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !player.isAlive()) return;
        HeroDefinition hero = HeroRegistry.context(player).orElse(null);
        if (hero == null || !hero.abilityWheel()) return;
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        switch (packet.action) {
            case USE -> {
                int count = hero.powers().set().count();
                int index = packet.ability < 0 ? hero.powers().selectedIndex(item) : packet.ability;
                if (index < 0 || index >= count || !(hero.powers().power(index) instanceof EngineAbility ability)) return;
                EngineServer.useAbility(player, item, ability, AbilityFunctions.Press.byIndex(packet.press));
            }
            case SKIN -> {
                if (hero instanceof EngineHero engine) EngineServer.cycleSkin(player, engine, item);
            }
            default -> {
            }
        }
    }
}
