package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.hero.power.Dash;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Movement (movimento) presets: flight, super speed, gliding, the giant leap, the grapple,
 * teleporting, fast swimming and passing through walls. Numbers: {@code alcance}, {@code raio},
 * {@code duracao}, {@code dano} (of the leap's landing); the rest are settings.
 */
public final class MovementPresets {
    /** The flight preset: a character with an ability that names it power-flies. */
    public static final String FLIGHT = "voo";
    /** Minecraft's gravity and air drag, for the leap and the glide. */
    private static final double GRAVITY = 0.08;

    private MovementPresets() {}

    public static void register() {
        Presets.registerFight(FLIGHT, MovementPresets::flight);
        Presets.registerFight("supervelocidade", MovementPresets::superSpeed);
        Presets.registerFight("planar", MovementPresets::glide);
        Presets.registerFight("salto_gigante", MovementPresets::giantLeap);
        Presets.registerWorld("gancho", MovementPresets::grapple);
        Presets.registerWorld("teleporte", MovementPresets::teleport);
        Presets.registerFight("nado_rapido", MovementPresets::fastSwim);
        Presets.registerFight("atravessar", MovementPresets::phase);
        Presets.registerWorld("levar_junto", MovementPresets::takeAlong);
    }

    // ---- voo ---------------------------------------------------------------------------------------

    /**
     * The power flight of a character with a "voo" ability, the heroes' flight (hold forward to
     * speed up, the sound barrier, rolls, the air brake, the landing). Settings:
     * {@code velocidade_maxima} (blocks per tick: the stat {@code voo}, or 2.5),
     * {@code velocidade_inicial} (0.7), {@code aceleracao} (seconds to the sound barrier, 5),
     * {@code gasto_por_segundo} (class energy per second of flight, 2), {@code pouso} (strength of the
     * hero landing, 1).
     */
    public static FlightProfile flightProfile(CharacterData data, CharacterData.Settings s) {
        double max = Mth.clamp(s.number("velocidade_maxima", data.flight() > 0 ? data.flight() : 2.5), 0.5, 9.0);
        double cruise = Mth.clamp(s.number("velocidade_inicial", 0.7), 0.2, max);
        return new FlightProfile(cruise, GLConfig.SOUND_BARRIER_SPEED.get(), max, Math.max(0.2, s.number("aceleracao", 5.0)), 1.6,
                s.number("pouso", 1.0));
    }

    /** Voo: the key takes off (a jump up into flight) or, while flying, lands. Double tap jump also flies. */
    static Outcome flight(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        if (!player.isCreative() && c.energy().get(c.item()).stored() <= 0) return Outcome.fail(Component.translatable("message.greenlantern.engine_flight_empty"));
        Abilities abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
            player.onUpdateAbilities();
            Fx.play(level, player, SoundEvents.ELYTRA_FLYING, 0.3F, 0.6F);
            PresetFx.burst(c, player.position().add(0, 0.5, 0), 10, 0.4);
            return Outcome.cooldown(10);
        }
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();
        Vec3 look = Targets.horizontal(player);
        PresetBody.push(player, new Vec3(look.x * 0.3, c.settings().number("impulso", 0.9), look.z * 0.3));
        Fx.ring(level, player.position().add(0, 0.1, 0), 1.2, ParticleTypes.CLOUD, 16);
        PresetFx.burst(c, player.position().add(0, 0.3, 0), 16, 0.4);
        Fx.play(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.7F);
        return Outcome.cooldown(10);
    }

    // ---- supervelocidade ---------------------------------------------------------------------------

    /**
     * Supervelocidade: for {@code duracao} seconds (10) the character runs much faster, steps up
     * whole blocks, runs on water and up walls (sprinting into them), and leaves a trail. Pressing
     * the key again stops it. Settings: {@code velocidade} (extra speed, 1.5 is +150%), {@code rastro}
     * (the trail's element, the ability's by default), {@code gasto_por_segundo} (0), {@code agua}
     * (true), {@code paredes} (true).
     */
    static Outcome superSpeed(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(10.0);
        double drain = s.number("gasto_por_segundo", 0.0);
        boolean water = s.flag("agua", true);
        boolean walls = s.flag("paredes", true);
        ParticleOptions trail = PresetFx.particle(s.text("rastro", c.element()), c.lightColor());
        Identifier speed = PresetBody.times(c, Attributes.MOVEMENT_SPEED, "speed", Math.max(0.0, s.number("velocidade", 1.5)));
        Identifier step = PresetBody.add(c, Attributes.STEP_HEIGHT, "step", 0.5);
        Fx.play(level, player, SoundEvents.TRIDENT_RIPTIDE_2.value(), 0.8F, 1.5F);
        PresetFx.burst(level, player.position().add(0, 0.9, 0), c.element(), c.lightColor(), 20, 0.4);
        Vec3[] last = {player.position()};
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (drain > 0 && age % 20 == 0 && !player.isCreative() && !pay(c, (int) Math.ceil(drain))) return false;
            Vec3 now = player.position();
            Vec3 moved = now.subtract(last[0]);
            last[0] = now;
            double flat = moved.horizontalDistance();
            if (flat > 0.15) {
                Fx.line(level, now.subtract(moved).add(0, 0.9, 0), now.add(0, 0.9, 0), trail, 3.0);
                if (age % 2 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, now.x, now.y + 0.5, now.z, 2, 0.2, 0.4, 0.2, 0.05);
            }
            if (water && flat > 0.15 && !player.isUnderWater() && level.getFluidState(BlockPos.containing(now.x, now.y - 0.1, now.z)).is(FluidTags.WATER)) {
                PresetBody.push(player, new Vec3(moved.x, 0.12, moved.z));
                level.sendParticles(ParticleTypes.SPLASH, now.x, now.y, now.z, 6, 0.3, 0.05, 0.3, 0.1);
            }
            if (walls && player.horizontalCollision && player.isSprinting()) {
                Vec3 look = Targets.horizontal(player);
                PresetBody.push(player, new Vec3(look.x * 0.1, 0.45, look.z * 0.1));
            }
            player.resetFallDistance();
            return age < ticks;
        }, () -> {
            PresetBody.remove(player, Attributes.MOVEMENT_SPEED, speed);
            PresetBody.remove(player, Attributes.STEP_HEIGHT, step);
            Fx.play(level, player, SoundEvents.BREEZE_LAND, 0.6F, 1.2F);
        }));
    }

    /** Takes class energy for a lasting preset; false when it ran out. */
    static boolean pay(PresetContext c, int amount) {
        ItemStack item = c.hero().findItem(c.player());
        return !item.isEmpty() && c.energy().tryConsume(item, amount);
    }

    // ---- planar ------------------------------------------------------------------------------------

    /**
     * Planar: a cape or wings that glide. While it lasts the character falls slowly, without fall
     * damage, and goes where they look; it ends on landing after a glide, after {@code duracao}
     * seconds (30) or on the key. Settings: {@code queda_por_segundo} (blocks, 2),
     * {@code controle} (forward speed in blocks per tick, 0.6).
     */
    static Outcome glide(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(30.0);
        double fall = Math.max(0.2, s.number("queda_por_segundo", 2.0)) / 20.0;
        double control = Math.max(0.0, s.number("controle", 0.6));
        // With less gravity the air drag holds the fall at about this speed.
        double factor = fall * 0.02 / 0.98 / GRAVITY;
        Identifier gravity = PresetBody.times(c, Attributes.GRAVITY, "gravity", Mth.clamp(factor, 0.01, 1.0) - 1.0);
        Fx.play(level, player, SoundEvents.ELYTRA_FLYING, 0.4F, 1.4F);
        boolean[] airborne = {!player.onGround()};
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (player.onGround()) return !airborne[0] && age < ticks;
            airborne[0] = true;
            player.resetFallDistance();
            if (!player.getAbilities().flying && !player.isInWater()) {
                if (age % 4 == 0 && control > 0) {
                    Vec3 look = Targets.horizontal(player);
                    PresetBody.push(player, new Vec3(look.x * control, -fall, look.z * control));
                }
                Vec3 back = player.position().add(Targets.horizontal(player).scale(-0.5)).add(0, 1.0, 0);
                level.sendParticles(ParticleTypes.CLOUD, back.x, back.y, back.z, 1, 0.3, 0.1, 0.3, 0.0);
                if (age % 3 == 0) level.sendParticles(particle, back.x, back.y, back.z, 2, 0.4, 0.1, 0.4, 0.0);
            }
            return age < ticks;
        }, () -> PresetBody.remove(player, Attributes.GRAVITY, gravity)));
    }

    // ---- salto_gigante -----------------------------------------------------------------------------

    /**
     * Salto gigante: a leap very high and far where the player looks; the landing hurts and pushes
     * everyone within {@code raio} (3) with {@code dano}. No fall damage. Settings: {@code altura}
     * (blocks, 8), {@code distancia} (blocks, 10).
     */
    static Outcome giantLeap(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double height = Math.max(1.0, s.number("altura", 8.0));
        double distance = Math.max(0.0, s.number("distancia", 10.0));
        double radius = c.radius(3.0);
        // Up at the speed that reaches the height (with a little for the drag); forward as far as
        // the air drag lets the time in the air carry it.
        double up = Math.sqrt(2.0 * GRAVITY * height) * 1.1;
        double forward = distance / 10.5;
        Vec3 look = Targets.horizontal(player);
        PresetBody.push(player, new Vec3(look.x * forward, up, look.z * forward));
        Fx.dust(level, player.position(), 1.2, 16);
        Fx.ring(level, player.position().add(0, 0.1, 0), 1.0, ParticleTypes.CLOUD, 20);
        Fx.play(level, player, SoundEvents.BREEZE_JUMP, 1.0F, 0.6F);
        Fx.play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 0.3F, 1.8F);
        PresetRuntime.start(player, age -> {
            player.resetFallDistance();
            if (age % 2 == 0) PresetFx.burst(c, player.position().add(0, 0.5, 0), 2, 0.2);
            if (age > 4 && player.onGround()) {
                land(c, radius);
                return false;
            }
            return age < 200;
        });
        return Outcome.DONE;
    }

    private static void land(PresetContext c, double radius) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        Vec3 at = player.position();
        for (LivingEntity target : Targets.inRadius(level, at.add(0, 0.5, 0), radius + 0.5, c::hittable)) {
            c.hit(target, c.damage());
            Hits.knockAway(target, at, 0.8, 0.5);
        }
        Fx.dust(level, at, radius, 40);
        Fx.ring(level, at.add(0, 0.1, 0), radius, ParticleTypes.CLOUD, 30);
        PresetFx.burst(c, at.add(0, 0.3, 0), 20, radius * 0.3);
        Fx.play(level, player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.0F, 0.9F);
        PresetFx.shake(player, 0.4F, 8);
    }

    // ---- gancho ------------------------------------------------------------------------------------

    /**
     * Gancho: a line to the block the player aims at, within {@code alcance} (24), that pulls them
     * there and holds them on arrival. It fails, giving the energy back, when no block is in reach.
     * Settings: {@code velocidade} (blocks per tick, 1.2). With {@code dano}, it rams whoever is in the
     * way.
     */
    static Outcome grapple(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(24.0);
        double speed = Mth.clamp(c.settings().number("velocidade", 1.2), 0.2, 4.0);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_aim"));
        Vec3 anchor = hit.getLocation();
        // The feet go to just short of the face, with the eyes about at the anchor.
        Vec3 to = anchor.subtract(look.scale(0.7)).subtract(0, 1.4, 0);
        Vec3 path = to.subtract(player.position());
        double distance = path.length();
        if (distance > 1.0) {
            Dash dash = new Dash();
            dash.start(player, path, distance, Math.max(2, Math.min(80, (int) Math.ceil(distance / speed))));
            ParticleOptions line = PresetFx.particle(c);
            PresetRuntime.start(player, age -> {
                boolean going = dash.tick(player, level, (target, direction) -> {
                    if (c.damage() > 0) c.hit(target, c.damage());
                    Hits.fling(target, direction, 0.6, 0.3);
                });
                Fx.line(level, c.muzzle(), anchor, line, 2.0);
                if (!going) PresetBody.push(player, new Vec3(0, 0.1, 0));
                return going;
            });
        }
        level.sendParticles(ParticleTypes.CRIT, anchor.x, anchor.y, anchor.z, 10, 0.1, 0.1, 0.1, 0.2);
        Fx.play(level, player, SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.8F);
        level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.CHAIN_PLACE, player.getSoundSource(), 1.0F, 1.0F);
        return Outcome.DONE;
    }

    // ---- teleporte ---------------------------------------------------------------------------------

    /**
     * Teleporte: the character goes and appears where they aim, within {@code alcance} (16), on the
     * ground near that point. It fails, giving the energy back, when there is no room there.
     * Settings: {@code tempo} (seconds between going and arriving, 0.25), {@code visual} (energia,
     * tubo for a boom tube, sombra, magia).
     */
    static Outcome teleport(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(16.0);
        String look = c.settings().text("visual", "energia");
        int delay = Math.max(0, (int) Math.round(c.settings().number("tempo", 0.25) * 20.0));
        HitResult aim = c.aim(reach);
        Vec3 point;
        if (aim instanceof BlockHitResult block) point = block.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getUnitVec3i()).scale(0.5));
        else if (aim instanceof EntityHitResult entity) point = entity.getEntity().position().subtract(player.getLookAngle().scale(1.2));
        else point = player.getEyePosition().add(player.getLookAngle().scale(reach));
        Vec3 spot = standingSpot(level, player, point);
        if (spot == null) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_place"));
        Vec3 from = player.position();
        portal(level, from, look, c.color(), true);
        if (delay == 0) {
            arrive(c, spot, look);
            return Outcome.DONE;
        }
        PresetRuntime.start(player, age -> {
            if (age % 2 == 0) portal(level, spot, look, c.color(), false);
            if (age < delay) return true;
            arrive(c, spot, look);
            return false;
        });
        return Outcome.DONE;
    }

    private static void arrive(PresetContext c, Vec3 spot, String look) {
        ServerPlayer player = c.player();
        player.teleportTo(spot.x, spot.y, spot.z);
        player.resetFallDistance();
        PresetBody.push(player, Vec3.ZERO);
        portal(c.level(), spot, look, c.color(), true);
    }

    /** The look of a teleport at one end: a boom tube, a shadow, magic or the character's energy. */
    private static void portal(ServerLevel level, Vec3 at, String look, int color, boolean loud) {
        Vec3 mid = at.add(0, 1.0, 0);
        switch (look) {
            case "tubo" -> {
                for (int i = 0; i < 3; i++) Fx.ring(level, at.add(0, 0.4 + 0.6 * i, 0), 1.1, ParticleTypes.END_ROD, 20);
                level.sendParticles(ParticleTypes.FIREWORK, mid.x, mid.y, mid.z, 20, 0.3, 0.6, 0.3, 0.05);
                if (loud) level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            case "sombra" -> {
                level.sendParticles(ParticleTypes.SQUID_INK, mid.x, mid.y, mid.z, 30, 0.4, 0.8, 0.4, 0.02);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, mid.x, mid.y, mid.z, 12, 0.4, 0.6, 0.4, 0.01);
                if (loud) level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.6F);
            }
            case "magia" -> {
                level.sendParticles(ParticleTypes.WITCH, mid.x, mid.y, mid.z, 30, 0.4, 0.7, 0.4, 0.05);
                level.sendParticles(ParticleTypes.ENCHANT, mid.x, mid.y, mid.z, 40, 0.5, 0.8, 0.5, 0.5);
                if (loud) level.playSound(null, at.x, at.y, at.z, SoundEvents.ILLUSIONER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            default -> {
                level.sendParticles(PresetFx.particle(PresetFx.ENERGY, color), mid.x, mid.y, mid.z, 30, 0.4, 0.8, 0.4, 0.0);
                level.sendParticles(ParticleTypes.PORTAL, mid.x, mid.y, mid.z, 30, 0.4, 0.8, 0.4, 0.3);
                if (loud) level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * A place where the player fits standing on something, at {@code point} or a few blocks below or
     * above it; null when there is none.
     */
    static Vec3 standingSpot(ServerLevel level, ServerPlayer player, Vec3 point) {
        BlockPos base = BlockPos.containing(point);
        for (int dy : new int[]{0, -1, -2, -3, -4, -5, 1, 2}) {
            BlockPos pos = base.above(dy);
            Vec3 spot = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (!fits(level, player, spot)) continue;
            BlockState below = level.getBlockState(pos.below());
            if (!below.getCollisionShape(level, pos.below()).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) return spot;
        }
        return null;
    }

    private static boolean fits(ServerLevel level, ServerPlayer player, Vec3 spot) {
        return level.noCollision(player, player.getBoundingBox().move(spot.subtract(player.position())));
    }

    // ---- nado_rapido -------------------------------------------------------------------------------

    /**
     * Nado rápido: for {@code duracao} seconds (20) the character swims much faster and, with
     * {@code folego} (true), breathes under water; in the water the key also shoots them forward.
     * Settings: {@code velocidade} (1).
     */
    static Outcome fastSwim(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(20.0);
        double speed = Mth.clamp(s.number("velocidade", 1.0), 0.0, 3.0);
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks, 0, false, true, true));
        if (s.flag("folego", true)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks, 0, false, true, true));
            player.setAirSupply(player.getMaxAirSupply());
        }
        Identifier swim = PresetBody.add(c, Attributes.WATER_MOVEMENT_EFFICIENCY, "swim", Math.min(1.0, speed * 0.5));
        if (player.isInWater()) PresetBody.push(player, player.getLookAngle().scale(0.8 * Math.max(0.5, speed)));
        Fx.play(level, player, SoundEvents.DOLPHIN_SPLASH, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (age % 2 == 0 && player.isInWater()) {
                Vec3 back = player.position().add(0, 0.5, 0);
                level.sendParticles(ParticleTypes.BUBBLE, back.x, back.y, back.z, 3, 0.2, 0.2, 0.2, 0.05);
                level.sendParticles(ParticleTypes.DOLPHIN, back.x, back.y, back.z, 2, 0.3, 0.3, 0.3, 0.0);
            }
            return age < ticks;
        }, () -> PresetBody.remove(player, Attributes.WATER_MOVEMENT_EFFICIENCY, swim)));
        return Outcome.DONE;
    }

    // ---- levar_junto -------------------------------------------------------------------------------

    /**
     * Levar junto: the {@code quantos} (3) nearest friendly creatures within {@code raio} (4)
     * (animals, villagers, tamed ones and the character's own summons, never monsters or players) go
     * along with the character for {@code duracao} seconds (30), floating close behind them wherever
     * they fly, run or teleport; they land softly. Pressing again lets them down.
     */
    static Outcome takeAlong(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int many = Mth.clamp(c.settings().whole("quantos", 3), 1, 8);
        List<LivingEntity> along = new java.util.ArrayList<>();
        for (LivingEntity e : Targets.inRadius(level, player.position(), c.radius(4.0), e -> e != player && friendly(player, e))) {
            if (along.size() >= many) break;
            along.add(e);
        }
        if (along.isEmpty()) return WorldPresets.nothing();
        int ticks = c.durationTicks(30.0);
        Fx.play(level, player, SoundEvents.ENDER_EYE_LAUNCH, 1.0F, 1.2F);
        along.forEach(e -> {
            e.setNoGravity(true);
            PresetFx.burst(c, e.position().add(0, e.getBbHeight() / 2, 0), 8, 0.4);
        });
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            Vec3 back = player.getLookAngle().multiply(1, 0, 1);
            back = back.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : back.normalize();
            for (int i = 0; i < along.size(); i++) {
                LivingEntity e = along.get(i);
                if (!e.isAlive() || e.level() != level) continue;
                double angle = (i - (along.size() - 1) / 2.0) * 0.7;
                Vec3 spot = player.position().subtract(back.yRot((float) angle).scale(1.6 + 0.3 * i)).add(0, 0.2, 0);
                e.snapTo(spot.x, spot.y, spot.z, player.getYRot(), 0);
                e.setDeltaMovement(Vec3.ZERO);
                e.resetFallDistance();
                if (age % 10 == 0) PresetFx.burst(c, e.position(), 2, 0.2);
            }
            return age < ticks && player.level() == level;
        }, () -> along.forEach(e -> {
            e.setNoGravity(false);
            e.resetFallDistance();
        })));
    }

    /** Whether a creature is one the character can take along: never a monster, never a player. */
    static boolean friendly(ServerPlayer player, LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.player.Player || e instanceof net.minecraft.world.entity.monster.Enemy) return false;
        UUID owner = Summons.owner(e);
        if (owner != null) return owner.equals(player.getUUID());
        if (e instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerReference() != null) {
            return pet.getOwnerReference().getUUID().equals(player.getUUID());
        }
        return e instanceof net.minecraft.world.entity.animal.Animal || e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager;
    }

    // ---- atravessar --------------------------------------------------------------------------------

    /**
     * Atravessar: the character vibrates (or turns intangible) and passes through the wall in front
     * to the first free place behind it, if the wall is at most {@code espessura} blocks thick (3);
     * blocks that can't be broken (bedrock) can't be crossed. Crossing takes {@code duracao} seconds
     * (0.3), untouchable. Settings: {@code espessura}, {@code visual} (vibrar or intangivel).
     */
    static Outcome phase(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int thickness = Mth.clamp((int) Math.round(c.settings().number("espessura", 3.0)), 1, 16);
        String look = c.settings().text("visual", "vibrar");
        Direction facing = player.getDirection();
        BlockPos feet = player.blockPosition();
        int wallStart = -1;
        for (int i = 1; i <= 2 && wallStart < 0; i++) {
            if (solid(level, feet.relative(facing, i))) wallStart = i;
        }
        if (wallStart < 0) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_wall"));
        Vec3 spot = null;
        for (int i = wallStart; i <= wallStart + thickness; i++) {
            BlockPos pos = feet.relative(facing, i);
            if (unbreakable(level, pos) || unbreakable(level, pos.above())) break;
            Vec3 at = new Vec3(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
            if (i > wallStart && fits(level, player, at)) {
                spot = at;
                break;
            }
        }
        if (spot == null) return Outcome.fail(Component.translatable("message.greenlantern.engine_too_thick"));
        Vec3 from = player.position();
        Vec3 to = spot;
        int ticks = Math.max(2, c.durationTicks(0.3));
        Guards.Guard untouchable = new Guards.Guard(1.0, 180.0, false, 1.0);
        Guards.add(player, untouchable);
        ParticleOptions particle = look.equals("intangivel") ? ParticleTypes.SOUL : PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, look.equals("intangivel") ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.BEACON_AMBIENT, 1.5F, 2.0F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Vec3 at = from.lerp(to, Math.min(1.0, age / (double) ticks));
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            level.sendParticles(particle, at.x, at.y + 1.0, at.z, 8, 0.3, 0.6, 0.3, 0.02);
            if (!look.equals("intangivel")) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1.0, at.z, 4, 0.3, 0.6, 0.3, 0.1);
            return age < ticks;
        }, () -> Guards.remove(player, untouchable)));
        return Outcome.DONE;
    }

    private static boolean solid(ServerLevel level, BlockPos feet) {
        return !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static boolean unbreakable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getDestroySpeed(level, pos) < 0 || !level.isInWorldBounds(pos);
    }
}
