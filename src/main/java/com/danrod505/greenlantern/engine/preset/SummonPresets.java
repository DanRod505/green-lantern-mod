package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Bolts;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import com.danrod505.greenlantern.engine.EngineServer;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Summon (invocação) presets: a helper creature, an energy construct, a vehicle, a swarm, copies
 * and a turret. What they call is the character's (see {@link Summons}) and goes away when
 * {@code duracao} ends. Numbers: {@code dano}, {@code alcance}, {@code duracao}.
 */
public final class SummonPresets {
    private SummonPresets() {}

    public static void register() {
        Presets.registerFight("ajudante", SummonPresets::helper);
        Presets.registerFight("construto", SummonPresets::construct);
        Presets.registerFight("veiculo", SummonPresets::vehicle);
        Presets.registerFight("enxame", SummonPresets::swarm);
        Presets.registerFight("copia", SummonPresets::copies);
        Presets.registerFight("torre", SummonPresets::turret);
    }

    /** The entity type a setting names, or the fallback. */
    private static EntityType<?> type(String id, EntityType<?> fallback) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return fallback;
        return BuiltInRegistries.ENTITY_TYPE.getValue(key);
    }

    private static void base(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && value > 0) instance.setBaseValue(value);
    }

    /** A place by the player, {@code side} blocks to the right and {@code ahead} in front. */
    private static Vec3 beside(ServerPlayer player, double side, double ahead) {
        Vec3 forward = Targets.horizontal(player);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return player.position().add(right.scale(side)).add(forward.scale(ahead));
    }

    private static void arrive(ServerLevel level, Entity entity, PresetContext c) {
        Vec3 mid = entity.position().add(0, entity.getBbHeight() / 2, 0);
        level.sendParticles(ParticleTypes.POOF, mid.x, mid.y, mid.z, 16, 0.4, 0.4, 0.4, 0.02);
        PresetFx.burst(level, mid, c.element(), c.lightColor(), 16, 0.5);
    }

    // ---- ajudante ----------------------------------------------------------------------------------

    /**
     * Ajudante: calls {@code quantidade} (1) creatures of {@code criatura} (minecraft:wolf) that fight
     * for the character for {@code duracao} seconds (30). Settings: {@code vida} (their health; 0
     * keeps the creature's). {@code dano} is their attack (0 keeps theirs).
     */
    static Outcome helper(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        EntityType<?> type = type(s.text("criatura", "minecraft:wolf"), EntityType.WOLF);
        int count = Mth.clamp(s.whole("quantidade", 1), 1, 8);
        int ticks = c.durationTicks(30.0);
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (!(entity instanceof Mob mob)) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_creature"));
            Vec3 at = beside(player, (i % 2 == 0 ? 1.5 : -1.5) * (1 + i / 2), 0.5);
            mob.snapTo(at.x, player.getY(), at.z, player.getYRot(), 0.0F);
            base(mob, Attributes.MAX_HEALTH, s.number("vida", 0.0));
            base(mob, Attributes.ATTACK_DAMAGE, c.damage());
            mob.setHealth(mob.getMaxHealth());
            if (mob instanceof TamableAnimal pet) pet.tame(player);
            Summons.spawn(c, mob);
            arrive(level, mob, c);
            Summons.adopt(c, mob, ticks, true, null);
        }
        Fx.play(level, player, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.2F);
        return Outcome.DONE;
    }

    // ---- construto ---------------------------------------------------------------------------------

    /**
     * Construto: an energy object in the character's colour that acts on its own for {@code duracao}
     * seconds (8) against the monsters within {@code alcance} (10): {@code forma} martelo (falls on
     * one every so often and shakes the ground around it), serra (spins around the character cutting
     * whoever comes close) or broca (a spike that goes for one and drills it). {@code dano} each blow.
     * Settings: {@code intervalo} (seconds between blows, 1.5 for the hammer), {@code visual} (solido:
     * a translucent object of energy, the default; particulas: only particles) and {@code escala} (1).
     */
    static Outcome construct(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        String shape = c.settings().text("forma", "martelo");
        double reach = c.reach(10.0);
        int ticks = c.durationTicks(8.0);
        int every = Math.max(5, (int) Math.round(c.settings().number("intervalo", 1.5) * 20));
        ParticleOptions main = PresetFx.particle(c);
        ParticleOptions light = PresetFx.particle(c.element(), c.lightColor());
        Vec3[] at = {player.position().add(0, 2.5, 0)};
        LivingEntity[] target = {null};
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
        Visuals.Construct solid = c.settings().text("visual", "solido").equals("solido") ? Visuals.construct(c, shape, at[0]) : null;
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (target[0] == null || !target[0].isAlive() || target[0].distanceTo(player) > reach + 4) {
                target[0] = Summons.foe(player, player, reach);
            }
            switch (shape) {
                case "serra" -> {
                    double a = age * 0.45;
                    at[0] = player.position().add(Math.cos(a) * 2.2, 1.0, Math.sin(a) * 2.2);
                    disc(level, at[0], 0.6, age, main, light);
                    if (age % 5 == 0) {
                        for (LivingEntity e : Targets.inRadius(level, at[0], 1.3, c::hittable)) {
                            c.hit(e, c.damage());
                            Hits.knockAway(e, player.position(), 0.4, 0.1);
                        }
                        Fx.play(level, player, SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.4F, 1.6F);
                    }
                }
                case "broca" -> {
                    Vec3 goal = target[0] != null ? Targets.middle(target[0]) : player.position().add(0, 2.5, 0);
                    at[0] = at[0].lerp(goal, 0.15);
                    Vec3 dir = goal.subtract(at[0]).lengthSqr() > 1.0E-3 ? goal.subtract(at[0]).normalize() : Targets.horizontal(player);
                    spike(level, at[0], dir, age, main, light);
                    if (target[0] != null && age % 6 == 0 && at[0].distanceTo(goal) < 1.5) {
                        c.hit(target[0], c.damage());
                        level.sendParticles(ParticleTypes.CRIT, goal.x, goal.y, goal.z, 6, 0.2, 0.2, 0.2, 0.2);
                        Fx.play(level, target[0], SoundEvents.GRINDSTONE_USE, 0.5F, 1.6F);
                    }
                }
                default -> {
                    int phase = age % every;
                    Vec3 over = target[0] != null ? target[0].position() : player.position().add(Targets.horizontal(player).scale(2.5));
                    double height = phase < every - 4 ? 4.0 : 4.0 * (every - phase) / 4.0;
                    at[0] = new Vec3(over.x, over.y + height, over.z);
                    hammer(level, at[0], main, light);
                    if (phase == every - 1 && target[0] != null) {
                        Vec3 ground = target[0].position();
                        for (LivingEntity e : Targets.inRadius(level, ground.add(0, 0.5, 0), 2.2, c::hittable)) {
                            c.hit(e, c.damage());
                            Hits.knockAway(e, ground, 0.5, 0.45);
                        }
                        Fx.ring(level, ground.add(0, 0.1, 0), 2.0, ParticleTypes.CLOUD, 20);
                        Fx.play(level, target[0], SoundEvents.ANVIL_LAND, 0.8F, 0.7F);
                    }
                }
            }
            if (solid != null) solid.moveTo(at[0]);
            return age < ticks;
        }, () -> {
            if (solid != null) solid.remove();
        }));
        return Outcome.DONE;
    }

    private static void disc(ServerLevel level, Vec3 at, double r, int age, ParticleOptions main, ParticleOptions light) {
        for (int i = 0; i < 10; i++) {
            double a = Mth.TWO_PI * i / 10 + age * 0.8;
            level.sendParticles(i % 2 == 0 ? main : light, at.x + Math.cos(a) * r, at.y, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        level.sendParticles(light, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private static void spike(ServerLevel level, Vec3 at, Vec3 dir, int age, ParticleOptions main, ParticleOptions light) {
        for (int i = 0; i < 8; i++) {
            double t = i / 8.0;
            double r = 0.45 * (1 - t);
            double a = age * 0.9 + i;
            Vec3 side = dir.cross(new Vec3(0, 1, 0)).lengthSqr() < 1.0E-3 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = side.cross(dir);
            Vec3 p = at.add(dir.scale(t * 1.4)).add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            level.sendParticles(i % 2 == 0 ? main : light, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void hammer(ServerLevel level, Vec3 at, ParticleOptions main, ParticleOptions light) {
        for (double x = -0.6; x <= 0.6; x += 0.3) {
            for (double y = -0.3; y <= 0.3; y += 0.3) level.sendParticles(main, at.x + x, at.y + y, at.z, 1, 0, 0, 0, 0);
        }
        for (double y = 0.3; y <= 1.6; y += 0.3) level.sendParticles(light, at.x, at.y + y, at.z, 1, 0, 0, 0, 0);
    }

    // ---- veiculo -----------------------------------------------------------------------------------

    /**
     * Veículo: a mount the character rides at once for {@code duracao} seconds (60), or until they
     * get off. Settings: {@code entidade} (minecraft:horse; a camel, a happy ghast to fly...),
     * {@code velocidade} (extra speed, 0.5), {@code vida} (0 keeps its own), {@code armas} (false:
     * with true it shoots at the monsters near it every second, {@code dano} each).
     */
    static Outcome vehicle(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        Entity entity = type(s.text("entidade", "minecraft:horse"), EntityType.HORSE).create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof LivingEntity ride)) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_creature"));
        int ticks = c.durationTicks(60.0);
        boolean weapons = s.flag("armas", false);
        ride.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        base(ride, Attributes.MAX_HEALTH, s.number("vida", 0.0));
        AttributeInstance speed = ride.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() * (1.0 + Math.max(0.0, s.number("velocidade", 0.5))));
        AttributeInstance flying = ride.getAttribute(Attributes.FLYING_SPEED);
        if (flying != null) flying.setBaseValue(flying.getBaseValue() * (1.0 + Math.max(0.0, s.number("velocidade", 0.5))));
        ride.setHealth(ride.getMaxHealth());
        if (ride instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setOwner(player);
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        } else if (ride instanceof HappyGhast ghast) {
            ghast.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WHITE_HARNESS));
        } else {
            ride.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
        Summons.spawn(c, ride);
        arrive(level, ride, c);
        player.startRiding(ride, true, true);
        Fx.play(level, player, SoundEvents.HORSE_SADDLE.value(), 1.0F, 1.0F);
        Bolts bolts = new Bolts(20, 0.2);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (!ride.isAlive() || (age > 10 && !ride.hasPassenger(player))) return false;
            if (weapons) {
                if (age % 20 == 0) {
                    LivingEntity foe = Summons.foe(player, ride, 16.0);
                    if (foe != null) {
                        Vec3 from = ride.position().add(0, ride.getBbHeight() * 0.8, 0);
                        bolts.fire(from, Targets.middle(foe).subtract(from).normalize().scale(1.4), foe);
                        Fx.play(level, ride, SoundEvents.CROSSBOW_SHOOT, 0.8F, 1.2F);
                    }
                }
                bolts.tick(player, level, (a, b) -> Fx.line(level, a, b, PresetFx.particle(c), 3.0), impact -> {
                    if (impact.entity() != null) c.hit(impact.entity(), c.damage());
                    PresetFx.burst(c, impact.at(), 6, 0.2);
                });
            }
            return age < ticks;
        }, () -> {
            ride.ejectPassengers();
            Summons.dismiss(ride);
        }));
        return Outcome.DONE;
    }

    // ---- enxame ------------------------------------------------------------------------------------

    /**
     * Enxame: {@code quantidade} (8) small creatures of {@code criatura} (minecraft:bat) that swarm the
     * monsters within {@code alcance} (12) of the character for {@code duracao} seconds (6), each
     * biting for {@code dano}; with none they circle the character.
     */
    static Outcome swarm(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        EntityType<?> type = type(s.text("criatura", "minecraft:bat"), EntityType.BAT);
        int count = Mth.clamp(s.whole("quantidade", 8), 1, 24);
        int ticks = c.durationTicks(6.0);
        double reach = c.reach(12.0);
        List<Entity> swarm = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (entity == null) break;
            if (entity instanceof Mob mob) mob.setNoAi(true);
            entity.setNoGravity(true);
            entity.snapTo(player.getX(), player.getY() + 1.5, player.getZ(), 0.0F, 0.0F);
            swarm.add(Summons.spawn(c, entity));
        }
        if (swarm.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_creature"));
        Fx.play(level, player, SoundEvents.BAT_TAKEOFF, 1.0F, 0.8F);
        LivingEntity[] target = {null};
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (age % 10 == 1 && (target[0] == null || !target[0].isAlive())) target[0] = Summons.foe(player, player, reach);
            Vec3 center = target[0] != null ? Targets.middle(target[0]) : player.position().add(0, 2.2, 0);
            double r = target[0] != null ? 0.9 : 1.8;
            for (int i = 0; i < swarm.size(); i++) {
                Entity bat = swarm.get(i);
                if (bat.isRemoved()) continue;
                double a = Mth.TWO_PI * i / swarm.size() + age * 0.25;
                Vec3 spot = center.add(Math.cos(a) * r, Math.sin(age * 0.3 + i) * 0.5, Math.sin(a) * r);
                Vec3 next = bat.position().lerp(spot, 0.3);
                bat.setYRot((float) Math.toDegrees(Math.atan2(-(next.x - bat.getX()), next.z - bat.getZ())));
                bat.setPos(next.x, next.y, next.z);
                if (target[0] != null && (age + i) % 10 == 0 && next.distanceTo(center) < 1.6) c.hit(target[0], c.damage());
            }
            if (age % 15 == 0) Fx.play(level, player, SoundEvents.BAT_AMBIENT, 0.5F, 1.2F);
            return age < ticks;
        }, () -> swarm.forEach(Summons::dismiss)));
        return Outcome.DONE;
    }

    // ---- copia -------------------------------------------------------------------------------------

    /**
     * Cópia: {@code quantidade} (2) copies of the character that draw the monsters near them, walk at
     * them and hit weakly ({@code dano}) for {@code duracao} seconds (8). Settings: {@code vida} (6).
     */
    static Outcome copies(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int count = Mth.clamp(c.settings().whole("quantidade", 2), 1, 6);
        int ticks = c.durationTicks(8.0);
        double health = Math.max(1.0, c.settings().number("vida", 6.0));
        List<Mannequin> copies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Mannequin copy = EntityType.MANNEQUIN.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (copy == null) break;
            Vec3 at = beside(player, (i % 2 == 0 ? 1.6 : -1.6) * (1 + i / 2), 0.0);
            copy.snapTo(at.x, player.getY(), at.z, player.getYRot(), 0.0F);
            copy.setComponent(DataComponents.PROFILE, look(c));
            copy.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            base(copy, Attributes.MAX_HEALTH, health);
            copy.setHealth((float) health);
            copies.add(Summons.spawn(c, copy));
            arrive(level, copy, c);
        }
        if (copies.isEmpty()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_creature"));
        Fx.play(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
        Summons.decoys(player, copies);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            boolean any = false;
            for (Mannequin copy : copies) {
                if (!copy.isAlive()) continue;
                any = true;
                LivingEntity foe = Summons.foe(player, copy, 16.0);
                if (age % 2 == 1) {
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, copy.getBoundingBox().inflate(16.0), m -> m.getTarget() == player)) {
                        mob.setTarget(copies.get(level.random.nextInt(copies.size())));
                    }
                }
                if (foe == null) continue;
                Vec3 to = foe.position().subtract(copy.position());
                double flat = Math.sqrt(to.x * to.x + to.z * to.z);
                float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
                copy.setYRot(yaw);
                copy.setYHeadRot(yaw);
                copy.setYBodyRot(yaw);
                if (flat > 1.4) {
                    Vec3 step = new Vec3(to.x, 0, to.z).normalize().scale(0.22);
                    copy.setPos(copy.getX() + step.x, copy.getY(), copy.getZ() + step.z);
                } else if ((age + copies.indexOf(copy) * 7) % 20 == 0) {
                    copy.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                    c.hit(foe, c.damage());
                }
            }
            return any && age < ticks;
        }, () -> {
            Summons.decoys(player, List.of());
            copies.forEach(Summons::dismiss);
        }));
        return Outcome.DONE;
    }

    /** The look of a copy: the character's skin (the one chosen now) on the player's model. */
    static ResolvableProfile look(PresetContext c) {
        // The first heroes still wear their suit over the player's own skin (until their skins of stage 3, lot 2).
        if (!(c.hero() instanceof com.danrod505.greenlantern.engine.EngineHero hero)) return ResolvableProfile.createResolved(c.player().getGameProfile());
        JsonObject json = new JsonObject();
        String skin = EngineServer.skin(hero, c.item()).selected();
        json.addProperty("texture", "greenlantern:entity/skin/" + skin);
        json.addProperty("model", c.hero().data().skeleton().slim() ? "slim" : "wide");
        return ResolvableProfile.CODEC.parse(JsonOps.INSTANCE, json).result()
                .orElseGet(() -> ResolvableProfile.createResolved(c.player().getGameProfile()));
    }

    // ---- torre -------------------------------------------------------------------------------------

    /**
     * Torre: a turret set in front of the character that shoots by itself at the monsters within
     * {@code alcance} (16) for {@code duracao} seconds (15), {@code dano} each shot. Settings:
     * {@code cadencia} (seconds between shots, 1), {@code velocidade} (1.4).
     */
    static Outcome turret(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(16.0);
        int ticks = c.durationTicks(15.0);
        int every = Math.max(4, (int) Math.round(c.settings().number("cadencia", 1.0) * 20));
        double speed = Math.max(0.4, c.settings().number("velocidade", 1.4));
        ArmorStand stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (stand == null) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_creature"));
        Vec3 at = beside(player, 0.0, 2.0);
        stand.snapTo(at.x, player.getY(), at.z, player.getYRot(), 0.0F);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DISPENSER));
        Summons.spawn(c, stand);
        arrive(level, stand, c);
        Fx.play(level, stand, SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.2F);
        Bolts bolts = new Bolts(Math.max(2, (int) Math.ceil(reach / speed)) + 4, 0.15);
        ParticleOptions trail = PresetFx.particle(c);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Vec3 head = stand.position().add(0, 1.75, 0);
            if (age % 8 == 0) Fx.ring(level, stand.position().add(0, 0.05, 0), 0.7, trail, 10);
            if (age % every == 0) {
                LivingEntity foe = Summons.foe(player, stand, reach);
                if (foe != null) {
                    Vec3 dir = Targets.middle(foe).subtract(head).normalize();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
                    stand.setYRot(yaw);
                    stand.setYHeadRot(yaw);
                    bolts.fire(head.add(dir.scale(0.5)), dir.scale(speed), foe);
                    Fx.play(level, stand, SoundEvents.DISPENSER_LAUNCH, 0.8F, 1.4F);
                }
            }
            bolts.tick(player, level, (a, b) -> Fx.line(level, a, b, trail, 3.0), impact -> {
                if (impact.entity() != null) c.hit(impact.entity(), c.damage());
                PresetFx.burst(c, impact.at(), 6, 0.2);
            });
            return age < ticks;
        }, () -> Summons.dismiss(stand)));
        return Outcome.DONE;
    }
}
