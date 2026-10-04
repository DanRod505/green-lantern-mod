package com.danrod505.greenlantern.trench;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.atlantis.Atlantis;
import com.danrod505.greenlantern.entity.TrenchCocoonEntity;
import com.danrod505.greenlantern.entity.TrenchCreatureEntity;
import com.danrod505.greenlantern.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The life of the nests of the Trench, while players are around:
 * <ul>
 *     <li>the first time someone comes near, a nest fills up: two packs of creatures (one led by a
 *     brute) and the villagers they keep in cocoons;</li>
 *     <li>the dead are replaced slowly, from the deep (out of the chambers), when nobody watches;</li>
 *     <li>every now and then a hunting party comes home with a new captive;</li>
 *     <li>the territory: a sting when a player crosses into it, clicks and creaks out of the caves,
 *     and the fish fading away (the Trench eats them);</li>
 *     <li>the raids: a war party swims out of a nest to attack Atlantis while someone is there.</li>
 * </ul>
 */
public final class TrenchLife {
    private static final int RESPAWN_TICKS = 20 * 60;
    private static final int RAID_TICKS = 20 * 60 * 4;
    private static final int CAPTIVES = 6;

    private static final Map<UUID, Integer> IN_TERRITORY = new HashMap<>();
    private static final Int2LongOpenHashMap LAST_RESPAWN = new Int2LongOpenHashMap();
    private static @Nullable Raid raid;

    /** A raid on Atlantis in progress. */
    private static final class Raid {
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("event.greenlantern.trench_raid").withStyle(ChatFormatting.DARK_RED),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        final List<UUID> raiders = new ArrayList<>();
        final long start;

        Raid(long start) {
            this.start = start;
        }
    }

    private TrenchLife() {}

    static void onServerStopped() {
        IN_TERRITORY.clear();
        LAST_RESPAWN.clear();
        if (raid != null) raid.bar.removeAllPlayers();
        raid = null;
    }

    public static int population() {
        return Math.max(1, GLConfig.TRENCH_NEST_POPULATION.get());
    }

    static void tick(ServerLevel level, List<Trench.Nest> nests) {
        long time = level.getGameTime();
        if (time % 20 == 0) territory(level, nests, time);
        if (time % 40 == 0) {
            for (Trench.Nest nest : nests) {
                if (level.getNearestPlayer(nest.x() + 0.5, nest.rim(), nest.z() + 0.5, 128.0, false) == null) continue;
                if (!level.areEntitiesLoaded(ChunkPos.asLong(nest.x() >> 4, nest.z() >> 4))) continue;
                Trench.State state = Trench.state(level.getServer());
                if (!state.populated.contains(nest.index())) {
                    populate(level, nest);
                    state.populated.add(nest.index());
                    Trench.save(level.getServer());
                } else {
                    replenish(level, nest, time);
                    if (time % 6000 == 0) bringCaptive(level, nest);
                }
            }
        }
        tickRaid(level, nests, time);
    }

    // ---- the nest ------------------------------------------------------------------------------------

    private static List<TrenchCreatureEntity> creatures(ServerLevel level, Trench.Nest nest) {
        AABB box = new AABB(nest.x() - 96, nest.floor() - 16, nest.z() - 96, nest.x() + 96, nest.waterTop() + 8, nest.z() + 96);
        return level.getEntitiesOfClass(TrenchCreatureEntity.class, box, c -> c.isAlive() && c.nestIndex() == nest.index() && !c.isRaider());
    }

    public static List<TrenchCocoonEntity> cocoons(ServerLevel level, Trench.Nest nest) {
        AABB box = new AABB(nest.x() - 64, nest.floor() - 16, nest.z() - 64, nest.x() + 64, nest.waterTop() + 8, nest.z() + 64);
        return level.getEntitiesOfClass(TrenchCocoonEntity.class, box, c -> c.isAlive() && c.nestIndex() == nest.index());
    }

    /** First visit: the packs come out of the chambers and the captives hang in their cocoons. */
    static void populate(ServerLevel level, Trench.Nest nest) {
        RandomSource random = RandomSource.create(TrenchNest.seed(nest));
        List<Vec3> spots = TrenchNest.cocoonSpots(nest);
        int captives = 0;
        for (int i = 0; i < spots.size() && captives < CAPTIVES; i++) {
            if (i % 2 == 1 && random.nextInt(3) != 0) continue;
            Villager villager = villager(level, spots.get(i));
            if (villager == null) continue;
            if (TrenchCocoonEntity.encase(level, spots.get(i), villager, nest.index()) != null) captives++;
        }
        int total = population();
        for (int i = 0; i < total; i++) {
            Vec3 den = TrenchNest.den(nest, i);
            TrenchCreatureEntity.spawn(level, den.add(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5), i == 0, nest.index(), i < total / 2 ? 0 : 1);
        }
        GreenLantern.LOGGER.info("A nest of the Trench woke up ({} creatures, {} captives)", total, captives);
    }

    private static @Nullable Villager villager(ServerLevel level, Vec3 pos) {
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
        if (villager == null) return null;
        villager.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.STRUCTURE, null);
        villager.setPersistenceRequired();
        villager.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 120, 0));
        level.addFreshEntity(villager);
        return villager;
    }

    /** One creature at a time comes out of the deep to fill the gaps (never under a player's nose). */
    private static void replenish(ServerLevel level, Trench.Nest nest, long time) {
        if (time - LAST_RESPAWN.getOrDefault(nest.index(), -RESPAWN_TICKS) < RESPAWN_TICKS) return;
        List<TrenchCreatureEntity> present = creatures(level, nest);
        if (present.size() >= population()) return;
        int den = level.getRandom().nextInt(TrenchNest.CHAMBERS);
        Vec3 pos = TrenchNest.den(nest, den);
        if (level.getNearestPlayer(pos.x, pos.y, pos.z, 16.0, false) != null) return;
        boolean brute = present.stream().noneMatch(TrenchCreatureEntity::isBrute);
        long pack0 = present.stream().filter(c -> c.pack() == 0).count();
        TrenchCreatureEntity.spawn(level, pos, brute, nest.index(), pack0 <= present.size() - pack0 ? 0 : 1);
        LAST_RESPAWN.put(nest.index(), time);
    }

    /** A hunting party comes home from the edge of the territory, a villager in its claws. */
    static void bringCaptive(ServerLevel level, Trench.Nest nest) {
        if (cocoons(level, nest).size() >= 3) return;
        double a = level.getRandom().nextDouble() * Mth.TWO_PI;
        Vec3 pos = new Vec3(nest.x() + 0.5 + Math.cos(a) * (Trench.TERRITORY - 4), nest.rim() + 6, nest.z() + 0.5 + Math.sin(a) * (Trench.TERRITORY - 4));
        if (!level.isLoaded(BlockPos.containing(pos))) return;
        TrenchCreatureEntity hunter = TrenchCreatureEntity.spawn(level, pos, false, nest.index(), 1);
        Villager villager = villager(level, pos);
        if (hunter == null || villager == null) return;
        villager.startRiding(hunter, true, true);
        TrenchCreatureEntity.spawn(level, pos.add(1.5, 0.5, 1.5), false, nest.index(), 1);
    }

    /** Someone is tearing at a cocoon: the whole nest comes for them. */
    public static void cocoonAttacked(ServerLevel level, TrenchCocoonEntity cocoon, LivingEntity attacker) {
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        for (TrenchCreatureEntity creature : level.getEntitiesOfClass(TrenchCreatureEntity.class, cocoon.getBoundingBox().inflate(40.0))) {
            if (creature.carried() == null && (creature.nestIndex() == cocoon.nestIndex() || cocoon.nestIndex() < 0)) creature.hunt(level, attacker, false);
        }
    }

    /** A villager was freed from a cocoon. */
    public static void villagerFreed(ServerLevel level, Villager villager, @Nullable Player rescuer) {
        Trench.countRescue(level.getServer());
        if (rescuer != null) {
            rescuer.displayClientMessage(Component.translatable("message.greenlantern.villager_rescued", Trench.rescued(level.getServer()))
                    .withStyle(ChatFormatting.GREEN), false);
        }
    }

    // ---- the territory -------------------------------------------------------------------------------

    private static void playTo(ServerPlayer player, SoundEvent sound, double x, double y, double z, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT, x, y, z, volume, pitch, player.getRandom().nextLong()));
    }

    private static void territory(ServerLevel level, List<Trench.Nest> nests, long time) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            Trench.Nest inside = null;
            for (Trench.Nest nest : nests) {
                if (nest.inTerritory(player.position())) inside = nest;
            }
            Integer was = IN_TERRITORY.get(player.getUUID());
            if (inside == null) {
                if (was != null) IN_TERRITORY.remove(player.getUUID());
                continue;
            }
            if (was == null || was != inside.index()) {
                IN_TERRITORY.put(player.getUUID(), inside.index());
                player.displayClientMessage(Component.translatable("message.greenlantern.trench_territory").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
                playTo(player, ModSounds.TRENCH_TERRITORY.get(), player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
            }
            // Out of the caves: clicks, creaks, a far-off wail.
            if (random.nextInt(5) == 0) {
                Vec3 from;
                if (inside.distance2D(player.position()) < Trench.PIT_RADIUS + 18 && random.nextBoolean()) {
                    from = TrenchNest.den(inside, random.nextInt(TrenchNest.CHAMBERS));
                } else {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double d = 14 + random.nextDouble() * 12;
                    from = player.position().add(Math.cos(a) * d, -6 - random.nextDouble() * 8, Math.sin(a) * d);
                }
                playTo(player, ModSounds.TRENCH_AMBIENCE.get(), from.x, from.y, from.z, 2.5F, 0.85F + random.nextFloat() * 0.3F);
            }
            // The fish fade away: those out of sight are gone, those in sight flee.
            if (time % 100 == 0) {
                for (LivingEntity fish : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40.0),
                        e -> (e instanceof AbstractFish || e instanceof Squid) && e.isAlive())) {
                    if (!inside.inTerritory(fish.position())) continue;
                    if (fish.distanceToSqr(player) > 14 * 14) {
                        level.sendParticles(ParticleTypes.BUBBLE, fish.getX(), fish.getY(), fish.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                        fish.discard();
                    } else {
                        Vec3 away = fish.position().subtract(inside.centerVec()).multiply(1, 0, 1);
                        if (away.lengthSqr() > 1.0E-4) fish.setDeltaMovement(away.normalize().scale(0.4).add(0, 0.05, 0));
                    }
                }
            }
        }
    }

    // ---- raids on Atlantis ---------------------------------------------------------------------------

    private static boolean nearAtlantis(Atlantis.Site site, Entity entity) {
        double dx = entity.getX() - (site.x() + 0.5);
        double dz = entity.getZ() - (site.z() + 0.5);
        return dx * dx + dz * dz <= (Atlantis.OUTER + 40) * (Atlantis.OUTER + 40) && entity.getY() < site.waterTop() + 40;
    }

    private static void tickRaid(ServerLevel level, List<Trench.Nest> nests, long time) {
        Atlantis.Site site = Atlantis.site(level.getServer());
        if (site == null) return;
        Trench.State state = Trench.state(level.getServer());
        if (raid != null) {
            if (time % 10 != 0) return;
            int alive = 0;
            List<TrenchCreatureEntity> living = new ArrayList<>();
            for (UUID id : raid.raiders) {
                if (level.getEntity(id) instanceof TrenchCreatureEntity c && c.isAlive()) {
                    alive++;
                    living.add(c);
                }
            }
            raid.bar.setProgress(Mth.clamp(alive / (float) Math.max(1, raid.raiders.size()), 0.0F, 1.0F));
            for (ServerPlayer player : level.players()) {
                if (nearAtlantis(site, player)) {
                    if (!raid.bar.getPlayers().contains(player)) raid.bar.addPlayer(player);
                } else if (raid.bar.getPlayers().contains(player)) {
                    raid.bar.removePlayer(player);
                }
            }
            if (alive == 0) {
                endRaid(level, Component.translatable("message.greenlantern.trench_raid_won").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            } else if (time - raid.start > RAID_TICKS) {
                for (TrenchCreatureEntity c : living) c.setRaider(false);
                endRaid(level, Component.translatable("message.greenlantern.trench_raid_retreat").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (!GLConfig.TRENCH_RAIDS.get() || time % 200 != 0) return;
        long interval = 20L * 60 * GLConfig.TRENCH_RAID_INTERVAL_MINUTES.get();
        if (state.nextRaid < 0) {
            state.nextRaid = time + interval;
            Trench.save(level.getServer());
            return;
        }
        if (time < state.nextRaid) return;
        state.nextRaid = time + interval / 2 + level.getRandom().nextInt((int) Math.max(1, interval));
        Trench.save(level.getServer());
        boolean someone = false;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && site.contains(player.position())) someone = true;
        }
        if (someone && level.getRandom().nextDouble() < GLConfig.TRENCH_RAID_CHANCE.get()) startRaid(level, site, nests);
    }

    private static void endRaid(ServerLevel level, Component message) {
        if (raid == null) return;
        for (ServerPlayer player : raid.bar.getPlayers()) player.displayClientMessage(message, false);
        raid.bar.removeAllPlayers();
        raid = null;
    }

    public static boolean raidActive() {
        return raid != null;
    }

    /** Sends a war party from a nest against Atlantis. Returns the raiders (empty if it could not start). */
    public static List<TrenchCreatureEntity> startRaid(ServerLevel level, Atlantis.Site site, List<Trench.Nest> nests) {
        if (raid != null || nests.isEmpty()) return List.of();
        RandomSource random = level.getRandom();
        Trench.Nest from = nests.get(random.nextInt(nests.size()));
        double a = Math.atan2(from.z() - site.z(), from.x() - site.x());
        Vec3 gate = new Vec3(site.x() + 0.5 + Math.cos(a) * (Atlantis.RADIUS + 6), site.floor() + 12, site.z() + 0.5 + Math.sin(a) * (Atlantis.RADIUS + 6));
        int players = 0;
        for (ServerPlayer player : level.players()) if (nearAtlantis(site, player)) players++;
        int count = Math.min(9, 5 + players);
        Raid r = new Raid(level.getGameTime());
        List<TrenchCreatureEntity> raiders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Vec3 pos = gate.add((random.nextDouble() - 0.5) * 8, (random.nextDouble() - 0.5) * 4, (random.nextDouble() - 0.5) * 8);
            TrenchCreatureEntity c = TrenchCreatureEntity.spawn(level, pos, i == 0, from.index(), 1);
            if (c == null) continue;
            c.setRaider(true);
            r.raiders.add(c.getUUID());
            raiders.add(c);
        }
        if (raiders.isEmpty()) return raiders;
        raid = r;
        Component warning = Component.translatable("message.greenlantern.trench_raid").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            if (!nearAtlantis(site, player)) continue;
            r.bar.addPlayer(player);
            player.displayClientMessage(warning, false);
            playTo(player, ModSounds.TRENCH_RAID.get(), gate.x, gate.y, gate.z, 4.0F, 1.0F);
        }
        GreenLantern.LOGGER.info("The Trench raids Atlantis ({} raiders from the nest at {} {})", raiders.size(), from.x(), from.z());
        return raiders;
    }
}
