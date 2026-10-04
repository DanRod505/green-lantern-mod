package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.entity.AquaTridentEntity;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The trident of Atlantis. Summoned by Aquaman's emblem, it only exists in his hands: it vanishes
 * if anyone else holds it, if it is dropped or when the armor is taken off.
 * <ul>
 *     <li>Left click: a heavy blow that also sends out a burst of water, knocking back and hurting
 *     the enemies close to the target.</li>
 *     <li>Hold right click and release: throw it. It hits hard and always flies back to you.</li>
 * </ul>
 */
public class AquaTridentItem extends Item {
    /** Attack damage shown on the tooltip (the configured damage is applied on top, see {@link #getAttackDamageBonus}). */
    private static final double BASE_DAMAGE = 14.0;
    private static final int THROW_CHARGE_TICKS = 8;

    public AquaTridentItem(Properties properties) {
        super(properties);
    }

    public static Properties properties() {
        return new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, BASE_DAMAGE - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(GreenLantern.id("trident_reach"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return (float) (GLConfig.TRIDENT_DAMAGE.get() - BASE_DAMAGE);
    }

    // ---- throwing ----------------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!AquamanHelper.isSuited(player)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (getUseDuration(stack, entity) - timeLeft < THROW_CHARGE_TICKS) return false;
        if (entity instanceof ServerPlayer player) throwTrident(player, stack);
        return true;
    }

    /** Throws the trident the player is holding (it comes back on its own). */
    public static @Nullable AquaTridentEntity throwTrident(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look.scale(0.5)).add(0, -0.15, 0);
        AquaTridentEntity thrown = AquaTridentEntity.create(level, player, stack.copy(), start, look.scale(AquaTridentEntity.THROW_SPEED));
        level.addFreshEntity(thrown);
        stack.shrink(1);
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRIDENT_THROW.get(), SoundSource.PLAYERS, 1.2F, 0.95F + level.random.nextFloat() * 0.1F);
        return thrown;
    }

    // ---- melee ---------------------------------------------------------------------------------------

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        // The blow carries the weight of the ocean: a burst of water around the target.
        Vec3 push = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        push = push.lengthSqr() < 1.0E-4 ? attacker.getLookAngle() : push.normalize();
        target.push(push.x * 0.9, 0.35, push.z * 0.9);
        target.hurtMarked = true;
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 30, 0.5, 0.4, 0.5, 0.4);
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 14, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.playSound(null, c.x, c.y, c.z, ModSounds.TRIDENT_SWING.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        float splash = (float) (GLConfig.TRIDENT_DAMAGE.get() * 0.3);
        DamageSource source = attacker instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(attacker);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
                e -> e != target && e != attacker && e instanceof Enemy && e.isAlive())) {
            if (other.hurtServer(level, source, splash)) {
                Vec3 away = other.position().subtract(target.position()).normalize();
                other.push(away.x * 0.6, 0.25, away.z * 0.6);
                other.hurtMarked = true;
            }
        }
    }

    // ---- only in Aquaman's hands ------------------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player && !AquamanHelper.isSuited(player) && !player.isCreative()) {
            stack.setCount(0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.aquaman_trident_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
