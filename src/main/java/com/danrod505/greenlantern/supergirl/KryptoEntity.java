package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Krypto, the Super-Dog: a white dog in a little red cape who stays with Supergirl while she wears
 * the suit. He walks behind her and flies beside her (even past the sound barrier), sits or follows
 * with a click, bites whatever she fights or whatever hurts her, now and then blasts it with a short
 * burst of heat vision, and brings her the loot her enemies drop. He never dies: when his health
 * runs out he flies off whimpering and comes back a minute later (see {@link SupergirlServer}).
 * <p>
 * He is never saved with the world: the Argo Pendant remembers his name and health (see
 * {@link KryptoData}), and he comes down from the sky again whenever she suits up.
 */
public class KryptoEntity extends Wolf {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("greenlantern", "textures/entity/krypto.png");
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(KryptoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int RED = 0xD8202E;
    private static final int GOLD = 0xFFD447;
    /** How far from her he flies before he catches up in a streak of light. */
    private static final double CATCH_UP = 28.0;

    /** Ticks left of his dive from the sky when he arrives. */
    private int arriving;
    private int heatCooldown;
    private int fetchCooldown;
    private ItemStack carried = ItemStack.EMPTY;

    public KryptoEntity(EntityType<? extends KryptoEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.45F));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.25, 8.0F, 2.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    /** Whether he is flying (the renderer stretches his legs out like a flying hero). */
    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    private void setFlying(boolean flying) {
        if (flying != isFlying()) entityData.set(FLYING, flying);
        setNoGravity(flying);
    }

    @Override
    public Identifier getTexture() {
        return TEXTURE;
    }

    /** He comes down from the sky: a dive toward her for up to {@code ticks} ticks. */
    void startArrival(int ticks) {
        arriving = ticks;
        setFlying(true);
    }

    public boolean isArriving() {
        return arriving > 0;
    }

    // ---- who he fights -----------------------------------------------------------------------------------

    /** Never players, villagers, golems or anyone's tamed animals. */
    static boolean friendly(LivingEntity target) {
        return target instanceof Player || target instanceof AbstractVillager || target instanceof IronGolem
                || target instanceof TamableAnimal tamed && tamed.isTame();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !friendly(target) && super.canAttack(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !friendly(target) && super.wantsToAttack(target, owner);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target != null && friendly(target) ? null : target);
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public @Nullable Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BONE) || super.isFood(stack);
    }

    /** A bone or meat in her hand heals him; an empty hand makes him sit or follow (as any dog). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BONE) && isOwnedBy(player) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                usePlayerItem(player, hand, stack);
                heal(8.0F);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ---- sounds ------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null || random.nextInt(4) == 0 ? SupergirlContent.KRYPTO_BARK.get() : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SupergirlContent.KRYPTO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SupergirlContent.KRYPTO_WHINE.get();
    }

    /** A bark, heard by everyone around. */
    public void bark() {
        level().playSound(null, getX(), getY(), getZ(), SupergirlContent.KRYPTO_BARK.get(), SoundSource.NEUTRAL, 1.2F, 0.95F + random.nextFloat() * 0.1F);
    }

    // ---- he never dies -----------------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel) {
            setHealth(getMaxHealth());
            SupergirlServer.kryptoDowned(this);
            return;
        }
        super.die(source);
    }

    /** He flies off into the sky (the suit came off, she left, or his health ran out). */
    void flyAway(boolean hurt) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(RED, 1.4F), getX(), getY() + 0.5, getZ(), 20, 0.3, 0.6, 0.3, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 16, 0.2, 2.0, 0.2, 0.08);
            level.playSound(null, getX(), getY(), getZ(), hurt ? SupergirlContent.KRYPTO_WHINE.get() : SupergirlContent.KRYPTO_FLY.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (!carried.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.3, getZ(), carried));
                carried = ItemStack.EMPTY;
            }
        }
        discard();
    }

    // ---- every tick --------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        resetFallDistance();
        if (!(level() instanceof ServerLevel level)) {
            if (isFlying() && tickCount % 2 == 0) {
                level().addParticle(new DustParticleOptions(tickCount % 4 == 0 ? RED : GOLD, 0.9F), getX(), getY() + 0.5, getZ(), 0, 0, 0);
            }
            return;
        }
        ServerPlayer owner = getOwner() instanceof ServerPlayer player ? player : null;
        if (!SupergirlServer.isHerKrypto(owner, this)) {
            flyAway(false);
            return;
        }
        if (heatCooldown > 0) heatCooldown--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && heatCooldown <= 0 && distanceToSqr(target) < 16 * 16 && hasLineOfSight(target)) heatVision(level, target);
        if (fetchCooldown > 0) fetchCooldown--;
        else if (!isOrderedToSit() && target == null) fetch(level, owner);
    }

    @Override
    public void aiStep() {
        if (level() instanceof ServerLevel && getOwner() instanceof ServerPlayer owner && owner.level() == level()) fly(owner);
        super.aiStep();
    }

    /** Flight: the dive when he arrives, then flying beside her whenever she flies. */
    private void fly(ServerPlayer owner) {
        Vec3 ownerVelocity = owner.position().subtract(owner.xo, owner.yo, owner.zo);
        double ownerSpeed = ownerVelocity.length();
        if (arriving > 0) {
            arriving--;
            Vec3 to = besideHer(owner, 1.6).subtract(position());
            double dist = to.length();
            setDeltaMovement(dist < 1.0E-3 ? Vec3.ZERO : to.normalize().scale(Math.min(dist, 1.1 + ownerSpeed)));
            face(getDeltaMovement());
            if (dist < 1.5 || arriving == 0) {
                arriving = 0;
                setFlying(!onGround() && owner.getAbilities().flying);
                bark();
                ((ServerLevel) level()).sendParticles(ModParticles.SOLAR_GLOW.get(), getX(), getY() + 0.4, getZ(), 20, 0.4, 0.3, 0.4, 0.05);
            }
            return;
        }
        if (isOrderedToSit()) {
            if (isFlying()) setFlying(false);
            return;
        }
        boolean ownerFlying = owner.getAbilities().flying;
        if (!ownerFlying && (!isFlying() || onGround())) {
            if (isFlying()) setFlying(false);
            return;
        }
        if (distanceToSqr(owner) > CATCH_UP * CATCH_UP) {
            catchUp(owner);
            return;
        }
        if (!isFlying()) {
            setFlying(true);
            level().playSound(null, getX(), getY(), getZ(), SupergirlContent.KRYPTO_FLY.get(), SoundSource.NEUTRAL, 0.8F, 1.1F);
        }
        getNavigation().stop();
        LivingEntity target = getTarget();
        Vec3 goal;
        if (target != null && target.isAlive() && distanceToSqr(target) < 24 * 24) {
            goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
        } else if (ownerFlying) {
            goal = besideHer(owner, 1.6).add(ownerVelocity.scale(2.0));
        } else {
            // She landed: down to her side, then on his feet again.
            goal = besideHer(owner, 1.4);
            if (distanceToSqr(goal) < 1.5) setFlying(false);
        }
        Vec3 to = goal.subtract(position());
        if (ownerFlying && ownerSpeed > 1.0 && (target == null || !target.isAlive())) {
            // At her speed he simply keeps formation: half way to his spot beside her every tick, carried by her speed.
            Vec3 next = position().add(ownerVelocity).add(to.subtract(ownerVelocity).scale(0.5));
            if (level().noCollision(this, getBoundingBox().move(next.subtract(position())))) setPos(next.x, next.y, next.z);
            setDeltaMovement(ownerVelocity);
            face(ownerVelocity);
            return;
        }
        Vec3 want = ownerVelocity.add(to.scale(0.3));
        double max = Math.max(1.2, ownerSpeed * 1.25 + 0.4);
        if (want.length() > max) want = want.normalize().scale(max);
        setDeltaMovement(getDeltaMovement().lerp(want, 0.6));
        face(getDeltaMovement());
        if (target != null && distanceToSqr(target) < 2.5 * 2.5 && tickCount % 10 == 0) doHurtTarget((ServerLevel) level(), target);
        if (ownerSpeed > 2.0 && tickCount % 2 == 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** A spot to her right, a little behind her. */
    private static Vec3 besideHer(ServerPlayer owner, double side) {
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        return owner.position().add(right.scale(side)).subtract(flat.scale(0.8)).add(0, 0.3, 0);
    }

    private void face(Vec3 motion) {
        if (motion.horizontalDistanceSqr() < 1.0E-3) return;
        float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** Too far behind: a streak of light and he is next to her again. */
    void catchUp(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(new DustParticleOptions(RED, 1.2F), getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.0);
        Vec3 at = besideHer(owner, 1.6);
        snapTo(at.x, at.y, at.z, owner.getYRot(), 0.0F);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.4, at.z, 16, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, at.x, at.y, at.z, SupergirlContent.KRYPTO_FLY.get(), SoundSource.NEUTRAL, 0.8F, 1.2F);
    }

    // ---- heat vision and fetching ------------------------------------------------------------------------

    private void heatVision(ServerLevel level, LivingEntity target) {
        heatCooldown = Mth.ceil(SupergirlConfig.KRYPTO_HEAT_SECONDS.get() * 20.0);
        Vec3 eyes = getEyePosition();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        HitResult block = level.clip(new ClipContext(eyes, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) return;
        getLookControl().setLookAt(target);
        Vec3 step = at.subtract(eyes);
        int points = Math.max(2, (int) (step.length() * 4));
        for (int i = 0; i <= points; i++) {
            Vec3 p = eyes.add(step.scale(i / (double) points));
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? RED : 0xFF6A30, 0.8F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(ModParticles.HEAT_SPARK.get(), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.02);
        target.invulnerableTime = 0;
        target.hurtServer(level, ModDamageTypes.heatVision(level, this), 5.0F);
        target.igniteForSeconds(3.0F);
        level.playSound(null, getX(), getY(), getZ(), SupergirlContent.HEAT_BOLT.get(), SoundSource.NEUTRAL, 0.8F, 1.4F);
    }

    /** Loot lying around (not what a player threw away) goes to her; what he carries he drops at her feet. */
    private void fetch(ServerLevel level, ServerPlayer owner) {
        if (!carried.isEmpty()) {
            if (distanceToSqr(owner) < 3.0 * 3.0) {
                ItemEntity drop = new ItemEntity(level, owner.getX(), owner.getY() + 0.3, owner.getZ(), carried);
                drop.setNoPickUpDelay();
                level.addFreshEntity(drop);
                carried = ItemStack.EMPTY;
                bark();
                fetchCooldown = 20;
            } else if (!isFlying() && tickCount % 10 == 0) {
                getNavigation().moveTo(owner, 1.3);
            }
            return;
        }
        if (tickCount % 10 != 0) return;
        ItemEntity loot = null;
        double best = 10.0 * 10.0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10.0), e -> e.isAlive() && e.getOwner() == null)) {
            if (item.distanceToSqr(owner) < 2.5 * 2.5 || item.distanceToSqr(owner) > 12.0 * 12.0) continue;
            double d = item.distanceToSqr(this);
            if (d < best) {
                best = d;
                loot = item;
            }
        }
        if (loot == null) return;
        if (distanceToSqr(loot) < 1.6 * 1.6) {
            carried = loot.getItem().copy();
            loot.discard();
            level.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.4F);
        } else if (!isFlying()) {
            getNavigation().moveTo(loot, 1.3);
        }
    }

    /** What he has in his mouth (shown to tests). */
    public ItemStack carried() {
        return carried;
    }

    /** Super Hearing found an enemy: he turns to it and barks. */
    void alert(Entity enemy) {
        getLookControl().setLookAt(enemy);
        bark();
        if (level() instanceof ServerLevel level) {
            Vec3 to = enemy.position().subtract(position()).normalize();
            for (int i = 1; i <= 4; i++) {
                Vec3 p = getEyePosition().add(to.scale(i * 0.6));
                level.sendParticles(new DustParticleOptions(GOLD, 0.7F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
