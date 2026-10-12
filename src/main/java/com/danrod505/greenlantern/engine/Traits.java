package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The traits of a character ({@code "passivas"} in its file): what is natural to it, off the wheel,
 * turned on by Minecraft's own action or by the moment. A character has up to 4, besides the passive
 * of its class. Each reads its settings ({@code "ajustes"}):
 * <ul>
 *     <li>voo: power flight on a double jump (the heroes' flight; see {@code MovementPresets.flightProfile}).</li>
 *     <li>levitar: the same, slow and steady ({@code velocidade_maxima} 0.6).</li>
 *     <li>supervelocidade: sprinting speeds up by itself up to {@code velocidade} (+150%) in {@code aceleracao}
 *     seconds (2), steps up blocks, runs on water and up walls ({@code agua}, {@code paredes}).</li>
 *     <li>aquatico: in water, swims fast ({@code velocidade}, 0 to 2), breathes and sees well.</li>
 *     <li>planar: jumping while falling opens the cape or wings: a slow fall ({@code queda_por_segundo}, 2) going
 *     where the player looks ({@code controle}, 0.6), with no fall damage.</li>
 *     <li>superforca: breaks blocks faster ({@code forca}, +100%).</li>
 *     <li>invulneravel: no damage from {@code danos} (queda, fogo, frio, afogamento; all four by default).</li>
 *     <li>visao_no_escuro: sees in the dark.</li>
 *     <li>sentidos_agucados: crouching and still, the creatures within {@code raio} (16) glow through walls.</li>
 *     <li>pulo_alto: jumps much higher ({@code altura}, +0.3) and lands without damage.</li>
 *     <li>escalar: walking into a wall climbs it ({@code velocidade}, 0.2 blocks per tick); crouching holds on.</li>
 *     <li>regeneracao: {@code espera} seconds (5) without damage, health comes back ({@code cura}, 1 per second).</li>
 *     <li>elastico: reaches blocks and creatures {@code alcance} (3) blocks further, no fall damage.</li>
 *     <li>imune_a_veneno: poison, spoiled food and gases have no effect.</li>
 * </ul>
 * Super speed, swimming, gliding and the high jump are the passive versions of the movement presets.
 */
public final class Traits {
    public static final List<String> TYPES = List.of("voo", "levitar", "supervelocidade", "aquatico", "planar", "superforca", "invulneravel",
            "visao_no_escuro", "sentidos_agucados", "pulo_alto", "escalar", "regeneracao", "elastico", "imune_a_veneno");
    /** At most this many traits per character (the class passive is apart). */
    public static final int MAX = 4;
    public static final List<String> DAMAGES = List.of("queda", "fogo", "frio", "afogamento");

    private static final Map<UUID, Long> LAST_HURT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SPRINT = new ConcurrentHashMap<>();
    private static final Set<UUID> GLIDING = ConcurrentHashMap.newKeySet();

    private Traits() {}

    private static Identifier id(String what) {
        return GreenLantern.id("trait_" + what);
    }

    // ---- flight ------------------------------------------------------------------------------------

    /** The settings of the character's flight (voo, or the slow levitar), or null when it doesn't fly. */
    public static CharacterData.Settings flight(CharacterData data) {
        var fly = data.trait("voo");
        if (fly.isPresent()) return fly.get().settings();
        return data.trait("levitar").map(t -> t.settings().or(Map.of("velocidade_maxima", num(0.6), "velocidade_inicial", num(0.3),
                "aceleracao", num(2.0), "gasto_por_segundo", num(1.0), "pouso", num(0.0)))).orElse(null);
    }

    private static JsonElement num(double value) {
        return new JsonPrimitive(value);
    }

    // ---- which are on ------------------------------------------------------------------------------

    /** Whether the trait is doing something now (the HUD lights its icon). Runs on both sides. */
    public static boolean on(CharacterData.Trait trait, Player player) {
        return switch (trait.type()) {
            case "voo", "levitar" -> player.getAbilities().flying;
            case "supervelocidade" -> player.isSprinting();
            case "aquatico" -> player.isInWater();
            case "planar" -> !player.onGround() && !player.getAbilities().flying && !player.isInWater() && player.getDeltaMovement().y < -0.05;
            case "visao_no_escuro" -> player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
            case "sentidos_agucados" -> player.isCrouching();
            case "escalar" -> player.horizontalCollision && !player.onGround();
            case "regeneracao" -> player.getHealth() < player.getMaxHealth();
            default -> true;
        };
    }

    // ---- suit and tick -----------------------------------------------------------------------------

    /** The suit came off: every change of the traits goes away. */
    static void suitDown(ServerPlayer player) {
        for (String what : List.of("superforca", "pulo_alto", "pulo_alto_queda", "elastico_blocos", "elastico_mobs", "supervelocidade",
                "supervelocidade_degrau", "planar")) {
            for (Holder<Attribute> attribute : List.of(Attributes.BLOCK_BREAK_SPEED, Attributes.JUMP_STRENGTH, Attributes.SAFE_FALL_DISTANCE,
                    Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE, Attributes.MOVEMENT_SPEED, Attributes.STEP_HEIGHT,
                    Attributes.GRAVITY)) {
                remove(player, attribute, id(what));
            }
        }
        SPRINT.remove(player.getUUID());
        GLIDING.remove(player.getUUID());
    }

    /** Every tick with the suit on. */
    static void tick(ServerPlayer player, CharacterHero hero) {
        tick(player, hero, Set.of());
    }

    /** Every tick with the suit on, leaving out the traits in {@code own} (the 9 heroes' own code already gives those). */
    static void tick(ServerPlayer player, CharacterHero hero, Set<String> own) {
        CharacterData data = hero.data();
        if (data.traits().isEmpty()) return;
        boolean second = player.tickCount % 20 == 0;
        for (CharacterData.Trait trait : data.traits()) {
            if (own.contains(trait.type())) continue;
            CharacterData.Settings s = trait.settings();
            switch (trait.type()) {
                case "supervelocidade" -> superSpeed(player, hero, s);
                case "planar" -> glide(player, s);
                case "escalar" -> climb(player, s);
                case "superforca" -> {
                    if (second) put(player, Attributes.BLOCK_BREAK_SPEED, "superforca", s.number("forca", 1.0), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                }
                case "pulo_alto" -> {
                    if (second) put(player, Attributes.JUMP_STRENGTH, "pulo_alto", s.number("altura", 0.3), AttributeModifier.Operation.ADD_VALUE);
                }
                case "elastico" -> {
                    if (second) {
                        double reach = s.number("alcance", 3.0);
                        put(player, Attributes.BLOCK_INTERACTION_RANGE, "elastico_blocos", reach, AttributeModifier.Operation.ADD_VALUE);
                        put(player, Attributes.ENTITY_INTERACTION_RANGE, "elastico_mobs", reach, AttributeModifier.Operation.ADD_VALUE);
                    }
                }
                case "aquatico" -> {
                    if (second && player.isInWater()) {
                        int speed = Mth.clamp(s.whole("velocidade", 0), 0, 2);
                        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, speed, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, false, true));
                    }
                }
                case "visao_no_escuro" -> {
                    if (second && player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                    }
                }
                case "sentidos_agucados" -> {
                    if (second && player.isCrouching() && player.getDeltaMovement().horizontalDistanceSqr() < 0.001) senses(player, s);
                }
                case "regeneracao" -> {
                    if (second) regenerate(player, s);
                }
                case "imune_a_veneno" -> {
                    if (second) {
                        for (Holder<MobEffect> effect : POISONS) player.removeEffect(effect);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static final List<Holder<MobEffect>> POISONS = List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.NAUSEA);

    /** Whether an effect may not take the player (poison and the like, with imune_a_veneno). */
    static boolean blocks(EngineHero hero, Holder<MobEffect> effect) {
        return hero.data().trait("imune_a_veneno").isPresent() && POISONS.contains(effect);
    }

    /** Running speeds up by itself while the player sprints. */
    private static void superSpeed(ServerPlayer player, CharacterHero hero, CharacterData.Settings s) {
        UUID uuid = player.getUUID();
        if (!player.isSprinting() || player.getAbilities().flying) {
            if (SPRINT.remove(uuid) != null) {
                remove(player, Attributes.MOVEMENT_SPEED, id("supervelocidade"));
                remove(player, Attributes.STEP_HEIGHT, id("supervelocidade_degrau"));
            }
            return;
        }
        int ticks = SPRINT.merge(uuid, 1, Integer::sum);
        double ramp = Math.max(1.0, s.number("aceleracao", 2.0) * 20.0);
        if (ticks % 5 == 1) {
            double speed = Math.max(0.0, s.number("velocidade", 1.5)) * Math.min(1.0, ticks / ramp);
            put(player, Attributes.MOVEMENT_SPEED, "supervelocidade", speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            put(player, Attributes.STEP_HEIGHT, "supervelocidade_degrau", 0.5, AttributeModifier.Operation.ADD_VALUE);
        }
        double drain = s.number("gasto_por_segundo", 0.0);
        if (drain > 0 && ticks % 20 == 0 && !player.isCreative()) {
            var item = hero.findItem(player);
            if (item.isEmpty() || !hero.energy().tryConsume(item, (int) Math.ceil(drain))) {
                player.setSprinting(false);
                return;
            }
        }
        ServerLevel level = player.level();
        Vec3 now = player.position();
        if (s.flag("agua", true) && !player.isUnderWater() && level.getFluidState(BlockPos.containing(now.x, now.y - 0.1, now.z)).is(FluidTags.WATER)) {
            Vec3 ahead = Targets.horizontal(player).scale(0.5);
            push(player, new Vec3(ahead.x, 0.12, ahead.z));
            if (ticks % 2 == 0) level.sendParticles(ParticleTypes.SPLASH, now.x, now.y, now.z, 4, 0.3, 0.05, 0.3, 0.1);
        }
        if (s.flag("paredes", true) && player.horizontalCollision) {
            Vec3 look = Targets.horizontal(player);
            push(player, new Vec3(look.x * 0.1, 0.45, look.z * 0.1));
        }
        if (ticks % 3 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, now.x, now.y + 0.5, now.z, 1, 0.2, 0.4, 0.2, 0.05);
        player.resetFallDistance();
    }

    /** Jumping while falling opens the cape: a slow fall going where the player looks. */
    private static void glide(ServerPlayer player, CharacterData.Settings s) {
        UUID uuid = player.getUUID();
        boolean gliding = GLIDING.contains(uuid);
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || player.isPassenger()) {
            if (gliding) {
                GLIDING.remove(uuid);
                remove(player, Attributes.GRAVITY, id("planar"));
            }
            return;
        }
        double fall = Math.max(0.2, s.number("queda_por_segundo", 2.0)) / 20.0;
        if (!gliding) {
            if (player.fallDistance < 1.5 || !player.getLastClientInput().jump()) return;
            GLIDING.add(uuid);
            // With less gravity the air drag holds the fall at about this speed.
            double factor = fall * 0.02 / 0.98 / 0.08;
            put(player, Attributes.GRAVITY, "planar", Mth.clamp(factor, 0.01, 1.0) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            Fx.play(player.level(), player, SoundEvents.ELYTRA_FLYING, 0.4F, 1.4F);
        }
        player.resetFallDistance();
        double control = Math.max(0.0, s.number("controle", 0.6));
        if (player.tickCount % 4 == 0 && control > 0) {
            Vec3 look = Targets.horizontal(player);
            push(player, new Vec3(look.x * control, -fall, look.z * control));
        }
        Vec3 back = player.position().add(Targets.horizontal(player).scale(-0.5)).add(0, 1.0, 0);
        if (player.tickCount % 2 == 0) player.level().sendParticles(ParticleTypes.CLOUD, back.x, back.y, back.z, 1, 0.3, 0.1, 0.3, 0.0);
    }

    /** Walking into a wall climbs it; crouching holds on. */
    private static void climb(ServerPlayer player, CharacterData.Settings s) {
        if (!player.horizontalCollision || player.getAbilities().flying) return;
        var input = player.getLastClientInput();
        if (!input.forward() && !input.shift()) return;
        Vec3 motion = player.getDeltaMovement();
        double up = input.shift() ? 0.0 : Math.max(0.05, s.number("velocidade", 0.2));
        push(player, new Vec3(motion.x * 0.5, up, motion.z * 0.5));
        player.resetFallDistance();
    }

    private static void senses(ServerPlayer player, CharacterData.Settings s) {
        double radius = Math.min(32.0, s.number("raio", 16.0));
        for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive())) {
            other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
        }
    }

    private static void regenerate(ServerPlayer player, CharacterData.Settings s) {
        long quiet = player.level().getGameTime() - LAST_HURT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (quiet < Math.round(s.number("espera", 5.0) * 20) || player.getHealth() >= player.getMaxHealth()) return;
        player.heal((float) Math.max(0.0, s.number("cura", 1.0)));
    }

    // ---- damage ------------------------------------------------------------------------------------

    /** The player was hurt (regeneration waits again). */
    static void hurt(ServerPlayer player) {
        LAST_HURT.put(player.getUUID(), player.level().getGameTime());
    }

    /** Whether the traits take away the fall damage (invulnerable to falls, the high jump, elastic). */
    static boolean noFall(CharacterData data) {
        return data.trait("pulo_alto").isPresent() || data.trait("elastico").isPresent()
                || data.trait("invulneravel").map(t -> damages(t).contains("queda")).orElse(false);
    }

    /** Whether the traits take away this damage (invulneravel). */
    static boolean immune(CharacterData data, DamageSource source) {
        var trait = data.trait("invulneravel");
        if (trait.isEmpty()) return false;
        List<String> damages = damages(trait.get());
        return damages.contains("queda") && source.is(DamageTypeTags.IS_FALL)
                || damages.contains("fogo") && source.is(DamageTypeTags.IS_FIRE)
                || damages.contains("frio") && source.is(DamageTypeTags.IS_FREEZING)
                || damages.contains("afogamento") && (source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.DRY_OUT));
    }

    private static List<String> damages(CharacterData.Trait trait) {
        List<String> list = trait.settings().words("danos");
        return list.isEmpty() ? DAMAGES : list;
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private static void put(ServerPlayer player, Holder<Attribute> attribute, String what, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(id(what), amount, operation));
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static void push(ServerPlayer player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
    }
}
