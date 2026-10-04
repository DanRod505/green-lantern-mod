package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.trench.TrenchLife;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A cocoon of the Trench: a pod of flesh and membrane hanging in the caves of a nest, with a
 * captured villager inside, kept alive (it breathes through the membrane). Break the cocoon (it
 * takes a few hits) to free the villager: it swims up to the surface, breathing water for a while,
 * and leaves a few emeralds behind in thanks. The creatures of the nest don't like that at all.
 */
public class TrenchCocoonEntity extends Mob {
    public static final float WIDTH = 1.0F;
    public static final float HEIGHT = 2.3F;

    private int nest = -1;
    private int emptyTicks;

    public TrenchCocoonEntity(EntityType<? extends TrenchCocoonEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Shuts the villager in a new cocoon at the spot (where the cocoon's bottom hangs). */
    public static @Nullable TrenchCocoonEntity encase(ServerLevel level, Vec3 spot, Villager villager, int nest) {
        TrenchCocoonEntity cocoon = ModEntities.TRENCH_COCOON.get().create(level, EntitySpawnReason.STRUCTURE);
        if (cocoon == null) return null;
        cocoon.nest = nest;
        cocoon.snapTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(cocoon);
        if (villager.isPassenger()) villager.stopRiding();
        villager.snapTo(spot.x, spot.y + 0.15, spot.z, cocoon.getYRot(), 0.0F);
        villager.startRiding(cocoon, true, true);
        level.playSound(null, spot.x, spot.y + 1, spot.z, net.minecraft.sounds.SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()), spot.x, spot.y + 1.2, spot.z,
                30, 0.4, 0.8, 0.4, 0.1);
        return cocoon;
    }

    /** Whether a cocoon already hangs at the spot. */
    public static boolean occupied(ServerLevel level, Vec3 spot) {
        return !level.getEntitiesOfClass(TrenchCocoonEntity.class, new AABB(spot, spot).inflate(1.2, 2.0, 1.2), Entity::isAlive).isEmpty();
    }

    public @Nullable Villager captive() {
        return getFirstPassenger() instanceof Villager villager ? villager : null;
    }

    public int nestIndex() {
        return nest;
    }

    @Override
    protected void registerGoals() {}

    @Override
    public void aiStep() {
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel serverLevel) {
            Villager captive = captive();
            if (captive != null) {
                captive.setAirSupply(captive.getMaxAirSupply());
                emptyTicks = 0;
            } else if (++emptyTicks > 100) {
                // A cocoon with nobody inside withers away.
                burst(serverLevel);
                discard();
                return;
            }
        }
        super.aiStep();
    }

    @Override
    public void travel(Vec3 input) {
        // Hangs still.
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.cocoon_hint").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Only hands and weapons tear the membrane (no drowning, no suffocation, no own creatures).
        if (source.getEntity() instanceof TrenchCreatureEntity || source.getEntity() == null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()), getX(), getY() + 1.2, getZ(),
                    12, 0.35, 0.6, 0.35, 0.1);
            if (source.getEntity() instanceof LivingEntity attacker) TrenchLife.cocoonAttacked(level, this, attacker);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            Villager villager = captive();
            Player rescuer = source.getEntity() instanceof Player player ? player : null;
            burst(level);
            if (villager != null) free(level, villager, rescuer);
            discard();
        }
    }

    private void burst(ServerLevel level) {
        level.playSound(null, getX(), getY() + 1, getZ(), ModSounds.COCOON_BURST.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()), getX(), getY() + 1.2, getZ(),
                60, 0.45, 0.9, 0.45, 0.15);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.8, 0.4, 0.2);
    }

    /** The villager is out: it swims up, breathing water for a while, and thanks its rescuer. */
    private void free(ServerLevel level, Villager villager, @Nullable Player rescuer) {
        villager.stopRiding();
        villager.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 180, 0));
        villager.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 60, 0));
        villager.setDeltaMovement(0, 0.3, 0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.6, villager.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.HEART, villager.getX(), villager.getY() + 2.0, villager.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        villager.spawnAtLocation(level, new ItemStack(Items.EMERALD, 2 + random.nextInt(4)));
        TrenchLife.villagerFreed(level, villager, rescuer);
    }

    // ---- holding the captive ------------------------------------------------------------------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Villager;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, 0.15, 0.0);
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean canBeRiddenUnderFluidType(net.minecraftforge.fluids.FluidType type, Entity rider) {
        return true;
    }

    // ---- nature -----------------------------------------------------------------------------------

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.SLIME_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COCOON_BURST.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 80 * 80;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Nest", nest);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nest = input.getIntOr("Nest", -1);
    }
}
