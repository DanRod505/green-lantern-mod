package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroRegistry;
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
 * Superman's suit comes out of the Kryptonian Crystal: the blue suit with the S on the chest and the
 * red cape, the red trunks with the yellow belt, the red boots (and the curl of black hair). While it
 * is worn the Man of Steel is far stronger, tougher, faster and can't be pushed around.
 */
public final class SupermanSuit {
    private static final Identifier STRENGTH = GreenLantern.id("superman_strength");
    private static final Identifier PUNCH_KNOCKBACK = GreenLantern.id("superman_punch_knockback");
    private static final Identifier HEALTH = GreenLantern.id("superman_health");
    private static final Identifier SPEED = GreenLantern.id("superman_speed");
    private static final Identifier STEADY = GreenLantern.id("superman_steady");
    private static final Identifier BLAST_PROOF = GreenLantern.id("superman_blast_proof");
    private static final Identifier STEP = GreenLantern.id("superman_step");
    private static final Identifier JUMP = GreenLantern.id("superman_jump");
    private static final Identifier MINING = GreenLantern.id("superman_mining");
    private static final Identifier REACH = GreenLantern.id("superman_reach");
    private static final Identifier BLOCK_REACH = GreenLantern.id("superman_block_reach");
    private static final Identifier BREATH = GreenLantern.id("superman_breath");

    private SupermanSuit() {}

    public static void toggle(ServerPlayer player) {
        if (SupermanHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (SupermanHelper.isSuited(player)) return true;
        ItemStack crystal = SupermanHelper.findCrystal(player);
        if (crystal.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_crystal"), true);
            return false;
        }
        // Only one hero suit at a time.
        HeroRegistry.dismissOthers(player, SupermanHero.INSTANCE);
        Uniform.equipSuit(player,
                ModItems.SUPERMAN_HAIR.get().getDefaultInstance(),
                ModItems.SUPERMAN_SUIT.get().getDefaultInstance(),
                ModItems.SUPERMAN_LEGGINGS.get().getDefaultInstance(),
                ModItems.SUPERMAN_BOOTS.get().getDefaultInstance());
        updateModifiers(player, true);
        FlightHandler.refreshAbilities(player);

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SUPERMAN_SUIT_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        sunburst(level, player, 50);
        SupermanServer.sendEvent(player, SuperFlags.EVENT_SUIT_UP);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
        SupermanServer.onSuitRemoved(player);
        Uniform.removeSuit(player);
        updateModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SUPERMAN_SUIT_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            sunburst(level, player, 20);
        }
    }

    /** A burst of golden sunlight and blue sparks all over the body. */
    static void sunburst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), count, 0.45, 0.9, 0.45, 0.05);
        level.sendParticles(ModParticles.SUPER_RING.get(), player.getX(), player.getY() + 1.0, player.getZ(), 0, 0.0, 1.0, 0.0, 1.0);
    }

    /** Adds (suited) or removes the strength, toughness and agility bonuses of the suit. */
    public static void updateModifiers(ServerPlayer player, boolean suited) {
        apply(player, Attributes.ATTACK_DAMAGE, STRENGTH, 10.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.ATTACK_KNOCKBACK, PUNCH_KNOCKBACK, 2.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.MAX_HEALTH, HEALTH, 20.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.MOVEMENT_SPEED, SPEED, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, suited);
        apply(player, Attributes.KNOCKBACK_RESISTANCE, STEADY, 1.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, BLAST_PROOF, 1.0, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.STEP_HEIGHT, STEP, 0.65, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.JUMP_STRENGTH, JUMP, 0.25, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.BLOCK_BREAK_SPEED, MINING, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, suited);
        apply(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, 1.5, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH, 1.5, AttributeModifier.Operation.ADD_VALUE, suited);
        apply(player, Attributes.OXYGEN_BONUS, BREATH, 8.0, AttributeModifier.Operation.ADD_VALUE, suited);
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
