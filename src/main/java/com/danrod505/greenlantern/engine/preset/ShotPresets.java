package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Bolts;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shot (disparo) presets, at a distance: projectile, continuous beam, fan, throw that comes back,
 * ballistic shot and strike from the sky. Numbers: {@code dano}, {@code alcance}, {@code raio},
 * {@code duracao}; the rest are settings, {@code elemento} among them.
 */
public final class ShotPresets {
    private ShotPresets() {}

    public static void register() {
        Presets.registerFight("projetil", ShotPresets::projectile);
        Presets.registerFight("feixe_continuo", ShotPresets::beam);
        Presets.registerFight("leque", ShotPresets::fan);
        Presets.registerFight("arremesso_que_volta", ShotPresets::boomerang);
        Presets.registerFight("balistico", ShotPresets::ballistic);
        Presets.registerFight("raio_do_ceu", ShotPresets::skyStrike);
    }

    /** Ticks the bolts until they are all gone, hitting with the ability's damage and element. */
    private static void fly(PresetContext c, Bolts bolts, ParticleOptions trail, java.util.function.Consumer<Bolts.Impact> extra) {
        ServerLevel level = c.level();
        PresetRuntime.start(c.player(), age -> {
            bolts.tick(c.player(), level, (from, to) -> Fx.line(level, from, to, trail, 3.0), impact -> {
                if (impact.entity() != null) c.hit(impact.entity(), c.damage());
                PresetFx.burst(c, impact.at(), 8, 0.2);
                level.playSound(null, impact.at().x, impact.at().y, impact.at().z, PresetFx.impactSound(c.element()),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.2F);
                if (extra != null) extra.accept(impact);
            });
            return !bolts.isEmpty();
        });
    }

    /**
     * Projétil: a shot that flies and stops at the first creature or block. Settings:
     * {@code velocidade} (blocks per tick, 1.6), {@code perseguicao} (0 flies straight, up to 1 turns
     * hard towards the target). {@code alcance} is how far it goes (32).
     */
    static Outcome projectile(PresetContext c) {
        CharacterData.Settings s = c.settings();
        double speed = Math.max(0.3, s.number("velocidade", 1.6));
        double reach = c.reach(32.0);
        double homing = Mth.clamp(s.number("perseguicao", 0.0), 0.0, 1.0);
        Bolts bolts = new Bolts(Math.max(2, (int) Math.ceil(reach / speed)), homing);
        LivingEntity target = homing > 0 ? c.aimedTarget(reach, 30.0) : null;
        bolts.fire(c.muzzle(), c.player().getLookAngle().scale(speed), target);
        Fx.play(c.level(), c.player(), PresetFx.shotSound(c.element()), 1.0F, 1.0F);
        fly(c, bolts, PresetFx.particle(c), null);
        return Outcome.DONE;
    }

    /**
     * Leque: several shots at once, spread side to side. Settings: {@code quantidade} (5),
     * {@code abertura} (the whole spread in degrees, 40), {@code velocidade} (1.6). Each shot does
     * {@code dano}; {@code alcance} is how far they go (24).
     */
    static Outcome fan(PresetContext c) {
        CharacterData.Settings s = c.settings();
        int count = Mth.clamp(s.whole("quantidade", 5), 1, 24);
        double spread = s.number("abertura", 40.0);
        double speed = Math.max(0.3, s.number("velocidade", 1.6));
        Bolts bolts = new Bolts(Math.max(2, (int) Math.ceil(c.reach(24.0) / speed)), 0.0);
        Vec3 look = c.player().getLookAngle();
        for (int i = 0; i < count; i++) {
            double angle = count == 1 ? 0 : -spread / 2 + spread * i / (count - 1);
            bolts.fire(c.muzzle(), look.yRot((float) Math.toRadians(angle)).scale(speed), null);
        }
        Fx.play(c.level(), c.player(), PresetFx.shotSound(c.element()), 1.0F, 0.8F);
        Fx.play(c.level(), c.player(), SoundEvents.CROSSBOW_SHOOT, 0.6F, 1.2F);
        fly(c, bolts, PresetFx.particle(c), null);
        return Outcome.DONE;
    }

    /**
     * Feixe contínuo: a ray that stays on for {@code duracao} seconds (2) and follows where the
     * player looks, hurting {@code dano} per second. {@code alcance} is its length (24). Settings:
     * {@code cor} (a colour like {@code "#FF3020"} instead of the character's), {@code forma} (reto,
     * espiral or raio) and {@code espessura} (1).
     */
    static Outcome beam(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(24.0);
        int ticks = c.durationTicks(2.0);
        int color = colour(c.settings().text("cor", ""), c.color());
        ParticleOptions glow = c.element().equals(PresetFx.ENERGY) ? new DustParticleOptions(c.lightColor(), 0.7F) : PresetFx.particle(c.element(), color);
        float perHit = c.damage() / 4.0F;
        Fx.play(level, player, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
        PresetRuntime.start(player, age -> {
            if (!c.hero().isSuited(player)) return false;
            Vec3 from = c.muzzle();
            HitResult hit = Targets.aim(level, player, reach);
            Vec3 end = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(reach)) : PresetContext.point(hit);
            Visuals.beam(c, from, end, color, glow, age);
            level.sendParticles(glow, end.x, end.y, end.z, 4, 0.15, 0.15, 0.15, 0.02);
            if (age % 5 == 0) {
                for (LivingEntity target : alongBeam(c, from, end)) c.hit(target, perHit);
            }
            if (age % 10 == 1) Fx.play(level, player, SoundEvents.GUARDIAN_ATTACK, 0.4F, 1.8F);
            return age < ticks;
        });
        return Outcome.DONE;
    }

    /** The creatures a straight beam touches, from {@code from} to {@code to}. */
    private static List<LivingEntity> alongBeam(PresetContext c, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        Vec3 unit = length < 1.0E-4 ? Vec3.ZERO : dir.scale(1.0 / length);
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(0.8), e -> {
            if (!c.hittable(e)) return false;
            Vec3 to2 = Targets.middle(e).subtract(from);
            double along = Mth.clamp(to2.dot(unit), 0.0, length);
            return to2.subtract(unit.scale(along)).length() <= 0.6 + e.getBbWidth() / 2.0;
        });
    }

    private enum Leg { OUT, BOUNCE, BACK }

    /**
     * Arremesso que volta: a batarang, a shield or a boomerang flies out, hits whoever is in its way,
     * may bounce to the next creature, and comes back to the hand. Settings: {@code quicadas}
     * (bounces, 0), {@code velocidade} (1.2), {@code item} (the item it shows, an item id).
     * {@code alcance} is how far it goes (16).
     */
    static Outcome boomerang(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double reach = c.reach(16.0);
        double speed = Math.max(0.3, s.number("velocidade", 1.2));
        int[] bounces = {Math.max(0, s.whole("quicadas", 0))};
        Item shown = StrikePresets.item(s.text("item", ""), null);
        ParticleOptions look = shown != null ? new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(shown)) : PresetFx.particle(c);
        ParticleOptions rim = PresetFx.particle(c);
        Vec3[] pos = {c.muzzle()};
        Vec3 goal = pos[0].add(player.getLookAngle().scale(reach));
        Leg[] leg = {Leg.OUT};
        LivingEntity[] next = {null};
        Set<Integer> hit = new HashSet<>();
        Fx.play(level, player, SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.4F);
        PresetRuntime.start(player, age -> {
            Vec3 aim = switch (leg[0]) {
                case OUT -> goal;
                case BOUNCE -> next[0] != null && next[0].isAlive() ? Targets.middle(next[0]) : player.getEyePosition();
                case BACK -> player.getEyePosition().add(0, -0.4, 0);
            };
            Vec3 to = aim.subtract(pos[0]);
            double step = Math.min(speed, to.length());
            Vec3 end = step < 1.0E-4 ? pos[0] : pos[0].add(to.normalize().scale(step));
            if (leg[0] == Leg.OUT) {
                BlockHitResult wall = level.clip(new ClipContext(pos[0], end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (wall.getType() != HitResult.Type.MISS) {
                    end = wall.getLocation();
                    leg[0] = Leg.BACK;
                    Fx.play(level, player, SoundEvents.SHIELD_BLOCK.value(), 0.6F, 1.4F);
                }
            }
            for (LivingEntity target : alongBeam(c, pos[0], end)) {
                if (!hit.add(target.getId())) continue;
                c.hit(target, c.damage());
                Hits.knockAway(target, pos[0], 0.4, 0.15);
                Fx.play(level, target, PresetFx.impactSound(c.element()), 0.8F, 1.3F);
                if (leg[0] != Leg.BACK && bounces[0] > 0) {
                    List<LivingEntity> others = Targets.inRadius(level, Targets.middle(target), 8.0, e -> c.hittable(e) && !hit.contains(e.getId()));
                    if (!others.isEmpty()) {
                        bounces[0]--;
                        next[0] = others.getFirst();
                        leg[0] = Leg.BOUNCE;
                        continue;
                    }
                }
                if (leg[0] == Leg.BOUNCE) leg[0] = Leg.BACK;
            }
            pos[0] = end;
            if (leg[0] == Leg.OUT && pos[0].distanceTo(goal) < 0.2) leg[0] = Leg.BACK;
            if (leg[0] == Leg.BOUNCE && (next[0] == null || !next[0].isAlive())) leg[0] = Leg.BACK;
            double spin = age * 1.1;
            level.sendParticles(look, pos[0].x, pos[0].y, pos[0].z, 2, 0.05, 0.05, 0.05, 0.0);
            for (int i = 0; i < 3; i++) {
                double a = spin + i * Mth.TWO_PI / 3;
                level.sendParticles(rim, pos[0].x + Math.cos(a) * 0.35, pos[0].y, pos[0].z + Math.sin(a) * 0.35, 1, 0, 0, 0, 0);
            }
            if (age % 5 == 0) level.playSound(null, pos[0].x, pos[0].y, pos[0].z, SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 0.3F, 1.8F);
            if (leg[0] == Leg.BACK && pos[0].distanceTo(player.getEyePosition().add(0, -0.4, 0)) < 1.0) {
                Fx.play(level, player, SoundEvents.ITEM_PICKUP, 0.8F, 0.9F);
                return false;
            }
            return age < 200;
        });
        return Outcome.DONE;
    }

    /**
     * Balístico: an arrow or a bullet that falls over the distance, with a special tip. Settings:
     * {@code ponta} (normal, explosiva, rede, gelo, fogo, eletrica), {@code velocidade} (2.5),
     * {@code gravidade} (0.05), {@code municao} (an item id the shot uses up, none by default; in
     * creative it is free). {@code alcance} (64) and {@code raio} (the blast of an explosive tip, 2.5).
     */
    static Outcome ballistic(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String ammo = s.text("municao", "");
        if (!ammo.isEmpty() && !player.isCreative()) {
            Item item = StrikePresets.item(ammo, null);
            if (item == null || !useAmmo(player, item)) {
                Component name = item != null ? item.getName() : Component.literal(ammo);
                return Outcome.fail(Component.translatable("message.greenlantern.engine_no_ammo", name));
            }
        }
        String tip = s.text("ponta", "normal");
        double speed = Math.max(0.3, s.number("velocidade", 2.5));
        Bolts bolts = new Bolts(Math.max(2, (int) Math.ceil(c.reach(64.0) / speed * 1.5)), 0.0, Math.max(0.0, s.number("gravidade", 0.05)));
        bolts.fire(c.muzzle(), player.getLookAngle().scale(speed), null);
        Fx.play(level, player, tip.equals("normal") ? SoundEvents.ARROW_SHOOT : SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F);
        ParticleOptions trail = switch (tip) {
            case "fogo" -> ParticleTypes.FLAME;
            case "gelo" -> ParticleTypes.SNOWFLAKE;
            case "eletrica" -> ParticleTypes.ELECTRIC_SPARK;
            case "explosiva" -> ParticleTypes.SMOKE;
            default -> ParticleTypes.CRIT;
        };
        double blast = c.radius(2.5);
        fly(c, bolts, trail, impact -> {
            LivingEntity target = impact.entity();
            Vec3 at = impact.at();
            switch (tip) {
                case "explosiva" -> {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.1F);
                    for (LivingEntity near : Targets.inRadius(level, at, blast, c::hittable)) {
                        if (near != target) c.hit(near, c.damage() * 0.6F);
                        Hits.knockAway(near, at, 0.6, 0.3);
                    }
                }
                case "rede" -> {
                    for (LivingEntity near : Targets.inRadius(level, at, 1.5, c::hittable)) {
                        near.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 6), player);
                        if (near instanceof Mob mob) mob.getNavigation().stop();
                    }
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(net.minecraft.world.item.Items.COBWEB)),
                            at.x, at.y, at.z, 24, 0.5, 0.5, 0.5, 0.05);
                }
                case "gelo" -> {
                    if (target != null) PresetFx.elementHit("gelo", target, player);
                }
                case "fogo" -> {
                    if (target != null) PresetFx.elementHit("fogo", target, player);
                }
                case "eletrica" -> {
                    if (target != null) PresetFx.elementHit("raio", target, player);
                }
                default -> {
                }
            }
        });
        return Outcome.DONE;
    }

    /** Uses up one of {@code item} from the player's inventory; false when they have none. */
    private static boolean useAmmo(ServerPlayer player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * Raio do céu: a lightning bolt or a ray falls on the aimed point after a short warning and hurts
     * everyone within {@code raio} (2.5). Settings: {@code atraso} (seconds of warning, 0.6),
     * {@code visual} (relampago or feixe). {@code alcance} is how far it can be aimed (32).
     */
    static Outcome skyStrike(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        HitResult aim = c.aim(c.reach(32.0));
        if (aim == null) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_aim"));
        Vec3 point = aim instanceof net.minecraft.world.phys.EntityHitResult e ? e.getEntity().position() : aim.getLocation();
        double radius = c.radius(2.5);
        int delay = Math.max(1, (int) Math.round(s.number("atraso", 0.6) * 20.0));
        boolean ray = s.text("visual", "relampago").equals("feixe");
        String element = s.values().containsKey("elemento") ? c.element() : ray ? "luz" : "raio";
        ParticleOptions warn = PresetFx.particle(element, c.color());
        Fx.play(level, player, ray ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0F, 1.2F);
        PresetRuntime.start(player, age -> {
            if (age < delay) {
                Fx.ring(level, point.add(0, 0.1, 0), radius * 0.2, warn, 12);
                return true;
            }
            if (ray) {
                Fx.line(level, point.add(0, 24, 0), point, new DustParticleOptions(c.lightColor(), 2.0F), 2.0);
                Fx.line(level, point.add(0, 24, 0), point, warn, 1.0);
                level.playSound(null, point.x, point.y, point.z, SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.8F);
            } else {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.setVisualOnly(true);
                    bolt.setPos(point);
                    level.addFreshEntity(bolt);
                }
            }
            Fx.ring(level, point.add(0, 0.2, 0), radius, warn, 28);
            for (LivingEntity target : Targets.inRadius(level, point.add(0, 0.8, 0), radius + 0.6, c::hittable)) {
                c.hit(target, c.damage());
                PresetFx.elementHit(element, target, player);
            }
            return false;
        });
        return Outcome.DONE;
    }

    private static int colour(String hex, int fallback) {
        if (hex.isEmpty()) return fallback;
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
