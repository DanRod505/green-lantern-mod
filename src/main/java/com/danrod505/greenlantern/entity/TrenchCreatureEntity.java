package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.atlantis.Atlantis;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.trench.Trench;
import com.danrod505.greenlantern.trench.TrenchNest;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A creature of the Trench: a pale-eyed, needle-toothed predator of the deep, all sinew and claws.
 * They are not scattered around like zombies: each one belongs to a nest and lives in its pack.
 * <ul>
 *     <li>One pack circles inside the pit, guarding the brood; the other patrols the territory
 *     around it. Anything that swims into the territory is hunted down, by the whole pack.</li>
 *     <li>They hunt like sharks: they circle their prey, then dart in to claw and bite and pull
 *     back. Out of their territory they only go after what swims close to them.</li>
 *     <li>They eat the fish of their sea, and carry villagers they catch in the water back to the
 *     nest, where they shut them in cocoons.</li>
 *     <li>They fear light: a lantern, a torch, a sea lantern or a Lantern's ring in hand often makes
 *     them flinch away instead of striking.</li>
 *     <li>The brute (bigger, slower, much tougher) leads them; its screech darkens the sea.</li>
 *     <li>Now and then a war party leaves the nests to raid Atlantis.</li>
 * </ul>
 */
public class TrenchCreatureEntity extends Monster {
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 1.8F;
    public static final float BRUTE_SCALE = 1.45F;

    private static final EntityDataAccessor<Boolean> DATA_BRUTE = SynchedEntityData.defineId(TrenchCreatureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(TrenchCreatureEntity.class, EntityDataSerializers.INT);

    /** Light that keeps the Trench at bay (held in either hand). */
    private static final Set<Item> LIGHTS = Set.of(Items.TORCH, Items.SOUL_TORCH, Items.LANTERN, Items.SOUL_LANTERN, Items.SEA_LANTERN, Items.GLOWSTONE,
            Items.SHROOMLIGHT, Items.JACK_O_LANTERN, Items.END_ROD, Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT,
            Items.BEACON, Items.CONDUIT);

    /** The nest it belongs to (-1: none, it lives around {@link #home}). */
    private int nest = -1;
    private int pack;
    private boolean raider;
    private @Nullable BlockPos home;

    // Server: hunting.
    private int attackCooldown;
    private int screechCooldown;
    private int flinchTicks;
    private int dryTicks;
    private int targetDryTicks;
    private Vec3 swim = Vec3.ZERO;
    // Server: carrying a villager home (route through the pit and into a chamber).
    private int routeStage;
    private int routeTicks;
    private int routeSpot = -1;

    // Client: animation.
    public float swimPhase;
    public float swimPhaseO;
    public float swimAmount;
    public float swimAmountO;
    private int lastAttack;
    private int attackAnim;

    public TrenchCreatureEntity(EntityType<? extends TrenchCreatureEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    /** A creature of a nest (or, with nest -1, one living around the spot it is put). */
    public static @Nullable TrenchCreatureEntity spawn(ServerLevel level, Vec3 pos, boolean brute, int nest, int pack) {
        TrenchCreatureEntity creature = ModEntities.TRENCH_CREATURE.get().create(level, EntitySpawnReason.STRUCTURE);
        if (creature == null) return null;
        creature.nest = nest;
        creature.pack = pack;
        creature.home = BlockPos.containing(pos);
        creature.setBrute(brute);
        creature.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        creature.setPersistenceRequired();
        level.addFreshEntity(creature);
        return creature;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BRUTE, false);
        builder.define(DATA_ATTACK, 0);
    }

    public boolean isBrute() {
        return entityData.get(DATA_BRUTE);
    }

    /** Makes it a brute (or a common creature): size, health, strength and armor. */
    public void setBrute(boolean brute) {
        entityData.set(DATA_BRUTE, brute);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(brute ? 50.0 : 22.0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(brute ? 8.0 : 4.0);
        getAttribute(Attributes.ARMOR).setBaseValue(brute ? 6.0 : 2.0);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(brute ? 0.6 : 0.2);
        getAttribute(Attributes.SCALE).setBaseValue(brute ? BRUTE_SCALE : 1.0);
        setHealth(getMaxHealth());
        xpReward = brute ? 15 : 6;
    }

    public int nestIndex() {
        return nest;
    }

    public int pack() {
        return pack;
    }

    public boolean isRaider() {
        return raider;
    }

    public void setRaider(boolean raider) {
        this.raider = raider;
    }

    public @Nullable Villager carried() {
        return getFirstPassenger() instanceof Villager villager ? villager : null;
    }

    private Trench.@Nullable Nest nest(ServerLevel level) {
        return nest < 0 ? null : Trench.nest(level.getServer(), nest);
    }

    private double swimSpeed() {
        return isBrute() ? 0.24 : 0.3;
    }

    private double lungeSpeed() {
        return isBrute() ? 0.42 : 0.55;
    }

    // ---- the brain -------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        boolean water = isInWater();
        setNoGravity(water);
        if (water) resetFallDistance();
        if (level() instanceof ServerLevel serverLevel && !isNoAi() && isAlive()) serverAi(serverLevel, water);
        super.aiStep();
        if (level().isClientSide()) clientTick();
    }

    private void serverAi(ServerLevel level, boolean water) {
        if (attackCooldown > 0) attackCooldown--;
        if (screechCooldown > 0) screechCooldown--;
        if (flinchTicks > 0) flinchTicks--;
        dryTicks = water ? 0 : dryTicks + 1;
        if (dryTicks > 200 && tickCount % 20 == 0) hurtServer(level, damageSources().dryOut(), 1.0F);

        Villager carried = carried();
        if (carried != null) carried.setAirSupply(carried.getMaxAirSupply());
        if (carried == null && (tickCount % 10 == 0 || (getTarget() != null && !getTarget().isAlive()))) chooseTarget(level);

        LivingEntity target = getTarget();
        Vec3 want;
        if (carried != null) {
            want = carryHome(level, carried);
        } else if (target != null) {
            want = hunt(level, target);
        } else {
            want = patrol(level);
        }
        if (water) {
            swim = swim.lerp(want, 0.3);
            // A fresh hit knocks it back for a moment.
            if (hurtTime < 6) setDeltaMovement(swim);
            if (horizontalCollision) setDeltaMovement(getDeltaMovement().add(0, 0.12, 0));
        } else {
            swim = Vec3.ZERO;
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x * 0.9 + want.x * 0.12, m.y, m.z * 0.9 + want.z * 0.12);
        }
        Vec3 face = carried == null && target != null ? target.position().subtract(position()) : getDeltaMovement();
        if (face.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 14.0F));
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        Vec3 motion = getDeltaMovement();
        float pitch = water ? (float) (-Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG) : 0.0F;
        setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -60.0F, 60.0F), 6.0F));
    }

    private boolean valid(ServerLevel level, LivingEntity target) {
        if (!target.isAlive() || target.level() != level || target.distanceToSqr(this) > 48 * 48) return false;
        if (target instanceof TrenchCreatureEntity) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (target instanceof Villager villager && villager.isPassenger()) return false;
        Trench.Nest home = nest(level);
        if (!raider && home != null && home.distance2D(position()) > Trench.TERRITORY + 40) return false;
        // A target that left the water is given up after a while (they lunge at the shore, no further).
        targetDryTicks = target.isInWater() ? 0 : targetDryTicks + 10;
        return targetDryTicks < 80 || target.distanceToSqr(this) < 5 * 5;
    }

    private void chooseTarget(ServerLevel level) {
        LivingEntity current = getTarget();
        if (current != null && !valid(level, current)) {
            setTarget(null);
            current = null;
        }
        boolean prey = current instanceof AbstractFish || current instanceof Squid;
        if (current != null && !prey) return;
        Trench.Nest home = nest(level);
        boolean inTerritory = home != null && home.inTerritory(position());
        // Players: anyone in the territory; elsewhere, whoever swims close.
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) continue;
            double d = player.distanceToSqr(this);
            double range = raider ? 40 : home != null && home.inTerritory(player.position()) ? 38 : inTerritory ? 24 : 16;
            if (d > range * range || d >= bestD) continue;
            if (!raider && !player.isInWater() && d > 6 * 6) continue;
            best = player;
            bestD = d;
        }
        if (best != null) {
            hunt(level, best, true);
            return;
        }
        if (raider) {
            List<AtlanteanEntity> atlanteans = level.getEntitiesOfClass(AtlanteanEntity.class, getBoundingBox().inflate(24.0));
            if (!atlanteans.isEmpty()) {
                setTarget(atlanteans.get(random.nextInt(atlanteans.size())));
                return;
            }
        }
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, getBoundingBox().inflate(20.0),
                v -> v.isAlive() && !v.isPassenger() && (v.isInWater() || v.distanceToSqr(this) < 6 * 6));
        if (!villagers.isEmpty()) {
            setTarget(villagers.getFirst());
            return;
        }
        if (current == null && random.nextInt(3) == 0) {
            List<LivingEntity> fish = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0),
                    e -> (e instanceof AbstractFish || e instanceof Squid) && e.isAlive());
            if (!fish.isEmpty()) setTarget(fish.getFirst());
        }
    }

    /** Hunts the target, calling the pack (and screeching) the first time. */
    public void hunt(ServerLevel level, LivingEntity target, boolean alertPack) {
        boolean fresh = getTarget() != target;
        setTarget(target);
        targetDryTicks = 0;
        if (fresh && target instanceof Player && screechCooldown == 0) {
            screechCooldown = 160;
            level.playSound(null, getX(), getY(), getZ(), ModSounds.TRENCH_SCREECH.get(), SoundSource.HOSTILE, isBrute() ? 2.0F : 1.4F, getVoicePitch());
            if (isBrute()) {
                // The brute's cry darkens the sea around it.
                for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(12.0))) {
                    if (!player.isCreative() && !player.isSpectator()) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), this);
                }
            }
        }
        if (alertPack && fresh) {
            for (TrenchCreatureEntity other : level.getEntitiesOfClass(TrenchCreatureEntity.class, getBoundingBox().inflate(32.0))) {
                if (other == this || other.carried() != null) continue;
                if (nest >= 0 && other.nest != nest && !(raider && other.raider)) continue;
                LivingEntity theirs = other.getTarget();
                if (theirs == null || theirs instanceof AbstractFish || theirs instanceof Squid) other.hunt(level, target, false);
            }
        }
    }

    private static Vec3 capped(Vec3 v, double speed) {
        double len = v.length();
        return len < 1.0E-4 ? Vec3.ZERO : v.scale(Math.min(speed, len) / len);
    }

    private boolean scaredBy(LivingEntity target) {
        if (!(target instanceof Player player)) return false;
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (LIGHTS.contains(stack.getItem())) return true;
            if (stack.getItem() instanceof PowerRingItem) return true;
        }
        return false;
    }

    private Vec3 hunt(ServerLevel level, LivingEntity target) {
        Vec3 me = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 there = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 to = there.subtract(me);
        double dist = to.length();
        if (flinchTicks > 0) return capped(to, 1.0).scale(-0.35);
        double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.1 * getScale();
        if (dist <= reach && attackCooldown == 0) {
            if (target instanceof Villager villager) {
                grab(level, villager);
                return Vec3.ZERO;
            }
            if (scaredBy(target) && random.nextFloat() < (isBrute() ? 0.3F : 0.6F)) {
                // The light: it recoils with a hiss.
                flinchTicks = 20;
                attackCooldown = 30;
                level.playSound(null, getX(), getY(), getZ(), ModSounds.TRENCH_HURT.get(), SoundSource.HOSTILE, 1.0F, 1.4F);
                return capped(to, 1.0).scale(-0.45);
            }
            strike(level, target);
            return capped(to, 1.0).scale(-0.25);
        }
        boolean fish = target instanceof AbstractFish || target instanceof Squid;
        if (attackCooldown > 8 && !fish) {
            // Between strikes the pack circles its prey, each in its own place.
            double slot = getId() * 2.399 + tickCount * 0.05 * (getId() % 2 == 0 ? 1 : -1);
            double ring = 4.0 + getScale() * 1.5;
            Vec3 around = there.add(Math.cos(slot) * ring, Math.sin(slot * 0.7) * 1.5, Math.sin(slot) * ring);
            return capped(around.subtract(me), swimSpeed());
        }
        return capped(to, dist < 7.0 ? lungeSpeed() : swimSpeed());
    }

    private void strike(ServerLevel level, LivingEntity target) {
        attackCooldown = isBrute() ? 32 : 22;
        swing(InteractionHand.MAIN_HAND);
        entityData.set(DATA_ATTACK, entityData.get(DATA_ATTACK) + 1);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.TRENCH_BITE.get(), SoundSource.HOSTILE, 1.0F, getVoicePitch());
        doHurtTarget(level, target);
        level.sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
    }

    /** Seizes a villager to carry it back to the nest (it breathes through the Trench's grip). */
    private void grab(ServerLevel level, Villager villager) {
        attackCooldown = 20;
        if (!villager.startRiding(this, true, true)) return;
        setTarget(null);
        routeStage = 0;
        routeTicks = 0;
        routeSpot = -1;
        villager.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 2400, 0));
        entityData.set(DATA_ATTACK, entityData.get(DATA_ATTACK) + 1);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.TRENCH_SCREECH.get(), SoundSource.HOSTILE, 1.4F, 1.2F);
        if (nest < 0) {
            Trench.Nest nearest = Trench.nearest(level.getServer(), position());
            if (nearest != null) nest = nearest.index();
        }
    }

    /**
     * Swims home with the villager: over the pit, down between the ribs, to the mouth of a cave and
     * into its chamber, where the villager is shut in a cocoon.
     */
    private Vec3 carryHome(ServerLevel level, Villager villager) {
        Trench.Nest home = nest(level);
        if (home == null) {
            // Nowhere to take it: let it go.
            villager.stopRiding();
            return Vec3.ZERO;
        }
        List<Vec3> spots = TrenchNest.cocoonSpots(home);
        if (routeSpot < 0 || routeSpot >= spots.size() || TrenchCocoonEntity.occupied(level, spots.get(routeSpot))) {
            routeSpot = -1;
            for (int i = 0; i < spots.size(); i++) {
                int k = (i + getId()) % spots.size();
                if (!TrenchCocoonEntity.occupied(level, spots.get(k))) {
                    routeSpot = k;
                    break;
                }
            }
        }
        Vec3 dest = routeSpot >= 0 ? spots.get(routeSpot) : home.centerVec().add(0, 3, 0);
        int chamber = routeSpot >= 0 ? routeSpot / 2 : -1;
        Vec3 mouth = chamber >= 0 ? TrenchNest.mouth(home, chamber) : dest;
        if (++routeTicks > 600) {
            // Stuck: on to the next leg of the way.
            routeTicks = 0;
            routeStage = Math.min(routeStage + 1, 3);
        }
        if (routeStage < 3 && position().distanceTo(dest) < 7.0) routeStage = 3;
        Vec3 goal = switch (routeStage) {
            case 0 -> TrenchNest.entry(home, home.rim() + 18);
            case 1 -> TrenchNest.entry(home, home.floor() + 7);
            case 2 -> mouth;
            default -> dest;
        };
        double dist = position().distanceTo(goal);
        if (routeStage == 0 && Math.sqrt(Math.pow(getX() - goal.x, 2) + Math.pow(getZ() - goal.z, 2)) < 2.0) {
            routeStage = 1;
            routeTicks = 0;
        } else if (routeStage == 1 && dist < 2.0) {
            routeStage = 2;
            routeTicks = 0;
        } else if (routeStage == 2 && dist < 1.8) {
            routeStage = 3;
            routeTicks = 0;
        } else if (routeStage == 3 && dist < 1.5) {
            villager.stopRiding();
            TrenchCocoonEntity.encase(level, dest, villager, home.index());
            routeStage = 0;
            routeSpot = -1;
            return Vec3.ZERO;
        }
        return capped(goal.subtract(position()), swimSpeed());
    }

    /** No prey: the pack patrols (one inside the pit, one around the territory); raiders swim at Atlantis. */
    private Vec3 patrol(ServerLevel level) {
        long time = level.getGameTime();
        if (raider) {
            Atlantis.Site site = Atlantis.site(level.getServer());
            if (site != null) {
                double a = getId() * 1.3 + time * 0.01;
                Vec3 goal = new Vec3(site.x() + 0.5 + Math.cos(a) * 14, site.floor() + 10, site.z() + 0.5 + Math.sin(a) * 14);
                return capped(goal.subtract(position()), swimSpeed());
            }
        }
        Trench.Nest home = nest(level);
        if (home == null) {
            BlockPos center = this.home != null ? this.home : blockPosition();
            if (this.home == null) this.home = center;
            double a = getId() * 1.9 + time * 0.012;
            Vec3 goal = new Vec3(center.getX() + 0.5 + Math.cos(a) * 6, center.getY() + 1 + Math.sin(time * 0.02) * 1.5, center.getZ() + 0.5 + Math.sin(a) * 6);
            return capped(goal.subtract(position()), 0.12);
        }
        double dir = pack == 0 ? 1 : -1;
        double a = pack * Math.PI + time * 0.004 * dir;
        Vec3 goal;
        if (pack == 0) {
            // Inside the pit, around the brood mound.
            double r = 11.0 + 2.0 * Math.sin(time * 0.003);
            goal = new Vec3(home.x() + 0.5 + Math.cos(a) * r, home.floor() + 9 + 3 * Math.sin(time * 0.01), home.z() + 0.5 + Math.sin(a) * r);
        } else {
            // Out in the territory, beyond the spires.
            double r = Trench.TERRITORY - 6.0;
            goal = new Vec3(home.x() + 0.5 + Math.cos(a) * r, home.rim() + 4 + 2 * Math.sin(time * 0.01 + 1), home.z() + 0.5 + Math.sin(a) * r);
        }
        double s = getId() * 1.7;
        goal = goal.add(Math.cos(s) * 3.0, Math.sin(s * 1.3) * 1.5, Math.sin(s) * 3.0);
        Vec3 to = goal.subtract(position());
        return capped(to, to.length() > 12 ? swimSpeed() * 0.85 : 0.13);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof TrenchCreatureEntity) return false;
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && valid(level, attacker)) {
            Villager carried = carried();
            if (carried == null) hunt(level, attacker, true);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        // Whatever it was carrying is free.
        Villager carried = carried();
        if (carried != null) carried.stopRiding();
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int bones = random.nextInt(isBrute() ? 4 : 3);
        if (bones > 0) spawnAtLocation(level, new ItemStack(Items.BONE, bones));
        int ink = random.nextInt(3);
        if (ink > 0) spawnAtLocation(level, new ItemStack(random.nextInt(4) == 0 ? Items.GLOW_INK_SAC : Items.INK_SAC, ink));
        if (random.nextInt(isBrute() ? 2 : 6) == 0) spawnAtLocation(level, new ItemStack(Items.PRISMARINE_SHARD, 1 + random.nextInt(2)));
        if (isBrute() && random.nextInt(3) == 0) spawnAtLocation(level, new ItemStack(Items.NAUTILUS_SHELL));
    }

    // ---- riding (a captured villager) ---------------------------------------------------------------

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
        return new Vec3(0.0, getBbHeight() * 0.1, getBbWidth() * 0.5 + 0.35).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TRENCH_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TRENCH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TRENCH_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return (isBrute() ? 0.72F : 1.0F) + (random.nextFloat() - 0.5F) * 0.2F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    // ---- client -----------------------------------------------------------------------------------

    private void clientTick() {
        swimPhaseO = swimPhase;
        swimAmountO = swimAmount;
        double moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
        boolean water = isInWater();
        float target = water ? (float) Mth.clamp(moved * 4.0, 0.0, 1.0) : 0.0F;
        swimAmount = Mth.approach(swimAmount, target, 0.06F);
        swimPhase += (float) (0.12 + moved * 1.6);
        int attack = entityData.get(DATA_ATTACK);
        if (attack != lastAttack) {
            lastAttack = attack;
            attackAnim = 10;
        } else if (attackAnim > 0) {
            attackAnim--;
        }
        if (water && moved > 0.25 && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.BUBBLE, getX() + (random.nextDouble() - 0.5) * 0.5, getY() + getBbHeight() * 0.5,
                    getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0.05, 0);
        }
    }

    public float swimPhase(float partialTick) {
        return Mth.lerp(partialTick, swimPhaseO, swimPhase);
    }

    public float swimAmount(float partialTick) {
        return Mth.lerp(partialTick, swimAmountO, swimAmount);
    }

    /** 0-1 over a claw strike (1 at the start). */
    public float attackProgress(float partialTick) {
        return attackAnim <= 0 ? 0.0F : Mth.clamp((attackAnim - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    // ---- saving -----------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Brute", isBrute());
        output.putInt("Nest", nest);
        output.putInt("Pack", pack);
        output.putBoolean("Raider", raider);
        if (home != null) {
            output.putInt("HomeX", home.getX());
            output.putInt("HomeY", home.getY());
            output.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_BRUTE, input.getBooleanOr("Brute", false));
        xpReward = isBrute() ? 15 : 6;
        nest = input.getIntOr("Nest", -1);
        pack = input.getIntOr("Pack", 0);
        raider = input.getBooleanOr("Raider", false);
        if (input.getInt("HomeX").isPresent()) {
            home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
        }
    }
}
