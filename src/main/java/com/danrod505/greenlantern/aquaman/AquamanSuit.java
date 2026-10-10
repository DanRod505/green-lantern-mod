package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.Hero;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.FlightHandler;
import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * The armor of Atlantis springs out of the Atlantean Emblem: the golden scale shirt, the green
 * scaled leggings and boots (Aquaman wears no mask, so the player's own helmet stays on).
 * While it is worn Aquaman breathes underwater, sees clearly in the deep, mines at full speed
 * underwater and hits harder.
 */
public final class AquamanSuit {
    private static final Identifier WATER_MODIFIER = GreenLantern.id("aquaman_water");
    private static final Identifier MINING_MODIFIER = GreenLantern.id("aquaman_mining");
    private static final Identifier STRENGTH_MODIFIER = GreenLantern.id("aquaman_strength");
    private static final Identifier TOUGHNESS_MODIFIER = GreenLantern.id("aquaman_knockback");

    private AquamanSuit() {}

    public static void toggle(ServerPlayer player) {
        if (AquamanHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (AquamanHelper.isSuited(player)) return true;
        ItemStack emblem = AquamanHelper.findEmblem(player);
        if (emblem.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_emblem"), true);
            return false;
        }
        // Only one hero suit at a time.
        Hero.dismissOthers(player, Hero.AQUAMAN);
        Uniform.equipSuit(player,
                ItemStack.EMPTY,
                ModItems.AQUAMAN_SUIT.get().getDefaultInstance(),
                ModItems.AQUAMAN_LEGGINGS.get().getDefaultInstance(),
                ModItems.AQUAMAN_BOOTS.get().getDefaultInstance());
        updateModifiers(player, true);
        player.setAirSupply(player.getMaxAirSupply());

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AQUAMAN_SUIT_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        splash(level, player, 50);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
        AquamanServer.onSuitRemoved(player);
        Uniform.removeSuit(player);
        updateModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AQUAMAN_SUIT_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            splash(level, player, 25);
        }
    }

    /** A spray of water and bubbles all over the body. */
    static void splash(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.4, 0.9, 0.4, 0.3);
        level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.4, 0.9, 0.4, 0.05);
        level.sendParticles(ParticleTypes.DOLPHIN, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.5, 0.9, 0.5, 0.05);
    }

    /** Adds (suited) or removes the swimming, underwater mining and strength bonuses of the suit. */
    public static void updateModifiers(ServerPlayer player, boolean suited) {
        apply(player, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_MODIFIER, 1.0, suited);
        apply(player, Attributes.SUBMERGED_MINING_SPEED, MINING_MODIFIER, 4.0, suited);
        apply(player, Attributes.ATTACK_DAMAGE, STRENGTH_MODIFIER, 2.0, suited);
        apply(player, Attributes.KNOCKBACK_RESISTANCE, TOUGHNESS_MODIFIER, 0.4, suited);
    }

    private static void apply(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (on) {
            if (!instance.hasModifier(id)) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            instance.removeModifier(id);
        }
    }
}
