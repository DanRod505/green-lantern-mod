package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Amazon sword, forged on Themyscira. Called by the tiara, it only exists in Wonder Woman's
 * hands: it vanishes if anyone else holds it, if it is dropped or when the armor comes off.
 * Every blow sweeps through the enemies around the target and feeds her divine power.
 */
public class AmazonSwordItem extends Item {
    /** Attack damage shown on the tooltip (the configured damage is applied on top, see {@link #getAttackDamageBonus}). */
    private static final double BASE_DAMAGE = 11.0;

    public AmazonSwordItem(Properties properties) {
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
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(GreenLantern.id("amazon_sword_reach"), 0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return (float) (GLConfig.AMAZON_SWORD_DAMAGE.get() - BASE_DAMAGE);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.15);
        level.playSound(null, c.x, c.y, c.z, ModSounds.AMAZON_SWORD_SWING.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        // A sweeping cut: the enemies close to the target are hit too.
        DamageSource source = attacker instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(attacker);
        float sweep = (float) (GLConfig.AMAZON_SWORD_DAMAGE.get() * 0.4);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.5, 0.3, 1.5),
                e -> e != target && e != attacker && e.isAlive() && !(e instanceof Player p && p.isCreative()) && !attacker.isPassengerOfSameVehicle(e))) {
            if (other.hurtServer(level, source, sweep)) {
                Vec3 away = other.position().subtract(attacker.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                other.push(away.x * 0.4, 0.1, away.z * 0.4);
                other.hurtMarked = true;
            }
        }
        // Battle feeds the gifts of the gods.
        if (attacker instanceof Player player && WonderWomanHelper.isSuited(player)) {
            ItemStack tiara = WonderWomanHelper.findTiara(player);
            if (!tiara.isEmpty()) WonderWomanHero.DIVINE_POWER.add(tiara, GLConfig.SWORD_HIT_CHARGE.get());
        }
    }

    // ---- only in Wonder Woman's hands --------------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player && !WonderWomanHelper.isSuited(player) && !player.isCreative()) {
            stack.setCount(0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.amazon_sword_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
