package com.danrod505.greenlantern.batman;

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
 * The batsuit comes out of the Utility Belt: the cowl with its ears, the grey suit with the bat on
 * the chest and the cape, the leggings and the boots. While it is worn Batman sees in the dark
 * (the cowl's lenses), hits harder (years of training), takes knocks better and glides with the cape.
 */
public final class BatmanSuit {
    private static final Identifier STRENGTH_MODIFIER = GreenLantern.id("batman_strength");
    private static final Identifier TOUGHNESS_MODIFIER = GreenLantern.id("batman_knockback");
    private static final Identifier SPEED_MODIFIER = GreenLantern.id("batman_speed");

    private BatmanSuit() {}

    public static void toggle(ServerPlayer player) {
        if (BatmanHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (BatmanHelper.isSuited(player)) return true;
        ItemStack belt = BatmanHelper.findBelt(player);
        if (belt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_belt"), true);
            return false;
        }
        // Only one hero suit at a time.
        Hero.dismissOthers(player, Hero.BATMAN);
        Uniform.equipSuit(player,
                ModItems.BATMAN_COWL.get().getDefaultInstance(),
                ModItems.BATMAN_SUIT.get().getDefaultInstance(),
                ModItems.BATMAN_LEGGINGS.get().getDefaultInstance(),
                ModItems.BATMAN_BOOTS.get().getDefaultInstance());
        updateModifiers(player, true);

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BATMAN_SUIT_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        shadows(level, player, 40);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
        BatmanServer.onSuitRemoved(player);
        Uniform.removeSuit(player);
        updateModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BATMAN_SUIT_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            shadows(level, player, 20);
        }
    }

    /** A swirl of smoke and dark wisps all over the body (Batman comes out of the shadows). */
    static void shadows(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.4, 0.8, 0.4, 0.05);
    }

    /** Adds (suited) or removes the strength, toughness and agility bonuses of the suit. */
    public static void updateModifiers(ServerPlayer player, boolean suited) {
        apply(player, Attributes.ATTACK_DAMAGE, STRENGTH_MODIFIER, 3.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.KNOCKBACK_RESISTANCE, TOUGHNESS_MODIFIER, 0.3, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, suited);
    }

    private static void apply(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (on) {
            if (!instance.hasModifier(id)) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
            }
        } else {
            instance.removeModifier(id);
        }
    }
}
