package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One of the Guardians of the Universe, the small blue-skinned founders of the Corps. They hover
 * motionless above their pillars on Oa, watch whoever comes near, share their wisdom when spoken to
 * and refill the ring of any Lantern who asks.
 */
public class OaGuardianEntity extends PathfinderMob {
    public static final int LINES = 10;
    private int talkCooldown;

    public OaGuardianEntity(EntityType<? extends OaGuardianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static void spawn(ServerLevel level, double x, double y, double z) {
        OaGuardianEntity guardian = ModEntities.OA_GUARDIAN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guardian == null) return;
        // Face the Central Power Battery.
        float yaw = (float) Math.toDegrees(Math.atan2(x - 0.5, -(z - 0.5)));
        guardian.snapTo(x, y, z, yaw, 0.0F);
        guardian.setYHeadRot(yaw);
        guardian.setYBodyRot(yaw);
        level.addFreshEntity(guardian);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 20.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        if (talkCooldown > 0) talkCooldown--;
        setDeltaMovement(getDeltaMovement().multiply(0.0, 0.0, 0.0));
        super.aiStep();
        if (level().isClientSide() && random.nextInt(6) == 0) {
            level().addParticle(ModParticles.GLOW.get(), getX() + (random.nextDouble() - 0.5) * 0.8, getY() + random.nextDouble() * 0.3, getZ() + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level() instanceof ServerLevel serverLevel && talkCooldown == 0) {
            talkCooldown = 30;
            Component line = Component.translatable("entity.greenlantern.oa_guardian.say." + random.nextInt(LINES));
            player.displayClientMessage(Component.translatable("message.greenlantern.npc_says", getDisplayName(), line).withStyle(ChatFormatting.AQUA), false);
            getLookControl().setLookAt(player);
            ItemStack ring = RingHelper.findRing(player);
            if (!ring.isEmpty() && !RingEnergy.get(ring).isFull()) {
                RingEnergy.set(ring, RingEnergy.configuredCapacity());
                player.displayClientMessage(Component.translatable("message.greenlantern.guardian_recharge").withStyle(ChatFormatting.GREEN), true);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHARGE_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                serverLevel.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.7, 0.4, 0.02);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
