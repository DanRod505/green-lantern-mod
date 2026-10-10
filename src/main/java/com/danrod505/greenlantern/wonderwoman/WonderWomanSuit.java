package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.Hero;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.FlightHandler;
import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.core.Holder;
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
 * Wonder Woman's armor comes out of the Tiara of Themyscira: the red bodice with the golden eagle,
 * the blue skirt with white stars, the red and white boots, the silver Bracelets of Submission (and
 * the black hair crowned by the tiara). While it is worn the Amazon princess is stronger, tougher,
 * quicker and steadier than any mortal, though not as mighty as the Man of Steel.
 */
public final class WonderWomanSuit {
    private static final Identifier STRENGTH = GreenLantern.id("wonder_woman_strength");
    private static final Identifier HEALTH = GreenLantern.id("wonder_woman_health");
    private static final Identifier SPEED = GreenLantern.id("wonder_woman_speed");
    private static final Identifier ATTACK_SPEED = GreenLantern.id("wonder_woman_attack_speed");
    private static final Identifier STEADY = GreenLantern.id("wonder_woman_steady");
    private static final Identifier STEP = GreenLantern.id("wonder_woman_step");
    private static final Identifier JUMP = GreenLantern.id("wonder_woman_jump");
    private static final Identifier REACH = GreenLantern.id("wonder_woman_reach");
    private static final Identifier BREATH = GreenLantern.id("wonder_woman_breath");

    private WonderWomanSuit() {}

    public static void toggle(ServerPlayer player) {
        if (WonderWomanHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (WonderWomanHelper.isSuited(player)) return true;
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_tiara"), true);
            return false;
        }
        // Only one hero suit at a time.
        Hero.dismissOthers(player, Hero.WONDER_WOMAN);
        Uniform.equipSuit(player,
                ModItems.WONDER_WOMAN_HAIR.get().getDefaultInstance(),
                ModItems.WONDER_WOMAN_SUIT.get().getDefaultInstance(),
                ModItems.WONDER_WOMAN_LEGGINGS.get().getDefaultInstance(),
                ModItems.WONDER_WOMAN_BOOTS.get().getDefaultInstance());
        updateModifiers(player, true);
        FlightHandler.refreshAbilities(player);

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WONDER_WOMAN_SUIT_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        burst(level, player, 50);
        WonderWomanServer.sendEvent(player, AmazonFlags.EVENT_SUIT_UP);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
        WonderWomanServer.onSuitRemoved(player);
        Uniform.removeSuit(player);
        updateModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WONDER_WOMAN_SUIT_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            burst(level, player, 20);
        }
    }

    /** A burst of golden sparks, like a flash of lightning from Olympus. */
    static void burst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), count, 0.45, 0.9, 0.45, 0.08);
        level.sendParticles(ModParticles.AMAZON_SHOCKWAVE.get(), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
    }

    /** Adds (suited) or removes the strength, toughness and agility bonuses of the armor. */
    public static void updateModifiers(ServerPlayer player, boolean suited) {
        apply(player, Attributes.ATTACK_DAMAGE, STRENGTH, 7.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.MAX_HEALTH, HEALTH, 16.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.MOVEMENT_SPEED, SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, suited);
        apply(player, Attributes.ATTACK_SPEED, ATTACK_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, suited);
        apply(player, Attributes.KNOCKBACK_RESISTANCE, STEADY, 0.8, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.STEP_HEIGHT, STEP, 0.5, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.JUMP_STRENGTH, JUMP, 0.2, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, 1.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.OXYGEN_BONUS, BREATH, 4.0, AttributeModifier.Operation.ADD_VALUE, suited);
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
