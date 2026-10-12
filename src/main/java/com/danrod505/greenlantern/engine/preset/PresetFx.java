package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.network.EngineFxPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * The look, sound and side effect of the elements the presets use ({@code "elemento"} in an
 * ability's settings): energia (the character's colour), raio, fogo, gelo, agua, sombra, magia, luz,
 * fumaca, terra, gas, som, plantas and vento. Each element is a family of two particles, the main one and an accent
 * (fire: flames and small flames; water: splashes and drops...). The colour ({@code "cor"}), the size
 * ({@code "particula_tamanho"}, of the dust particles) and the amount ({@code "particula_quantidade"}) come from the
 * ability's settings when the preset passes its {@link PresetContext}.
 */
public final class PresetFx {
    public static final String ENERGY = "energia";

    private PresetFx() {}

    /** The elements there are. */
    public static final java.util.Set<String> ELEMENTS = java.util.Set.of(ENERGY, "raio", "fogo", "gelo", "agua", "sombra", "magia", "luz", "fumaca",
            "terra", "gas", "som", "plantas", "vento");

    public static boolean known(String element) {
        return ELEMENTS.contains(element);
    }

    /** The particle of an element; energy is a dust of {@code color}. */
    public static ParticleOptions particle(String element, int color) {
        return particle(element, color, 1.0F);
    }

    /** The main particle of the ability's element, colour and size. */
    public static ParticleOptions particle(PresetContext c) {
        return particle(c.element(), c.color(), c.particleSize());
    }

    /** The main particle of an element; {@code size} scales the dust ones. */
    public static ParticleOptions particle(String element, int color, float size) {
        return switch (element) {
            case "raio" -> ParticleTypes.ELECTRIC_SPARK;
            case "fogo" -> ParticleTypes.FLAME;
            case "gelo" -> ParticleTypes.SNOWFLAKE;
            case "agua" -> ParticleTypes.SPLASH;
            case "sombra" -> ParticleTypes.SQUID_INK;
            case "magia" -> ParticleTypes.WITCH;
            case "luz" -> ParticleTypes.END_ROD;
            case "fumaca" -> ParticleTypes.LARGE_SMOKE;
            case "terra" -> dust(0x7A5A36, 1.4F * size);
            case "gas" -> dust(0x8FCB3A, 1.6F * size);
            case "som" -> dust(0xE8F4FF, 1.0F * size);
            case "plantas" -> dust(0x3C8C2E, 1.2F * size);
            case "vento" -> ParticleTypes.CLOUD;
            default -> dust(color, 1.2F * size);
        };
    }

    /** The second particle of an element's family, mixed in with the main one. */
    public static ParticleOptions accent(String element, int color, float size) {
        return switch (element) {
            case "raio" -> dust(color, 0.8F * size);
            case "fogo" -> ParticleTypes.SMALL_FLAME;
            case "gelo" -> ParticleTypes.ITEM_SNOWBALL;
            case "agua" -> ParticleTypes.FALLING_WATER;
            case "sombra" -> dust(0x150A20, 1.8F * size);
            case "magia" -> ParticleTypes.ENCHANT;
            case "luz" -> dust(mix(color, 0xFFFFFF, 0.6F), 1.0F * size);
            case "fumaca" -> ParticleTypes.SMOKE;
            case "terra" -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case "gas" -> ParticleTypes.SMOKE;
            case "som" -> ParticleTypes.NOTE;
            case "plantas" -> ParticleTypes.HAPPY_VILLAGER;
            case "vento" -> ParticleTypes.WHITE_ASH;
            default -> dust(mix(color, 0xFFFFFF, 0.55F), 0.7F * size);
        };
    }

    /** {@code a} moved {@code t} of the way to {@code b} (RGB). */
    public static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    public static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(color & 0xFFFFFF, Math.max(0.05F, Math.min(4.0F, size)));
    }

    /** A puff of the element's family at {@code at}: {@code count} main particles and a third as many accents. */
    public static void burst(ServerLevel level, Vec3 at, String element, int color, int count, double spread) {
        burst(level, at, element, color, 1.0F, count, spread);
    }

    /** A puff of the ability's element, colour, size and amount. */
    public static void burst(PresetContext c, Vec3 at, int count, double spread) {
        burst(c.level(), at, c.element(), c.color(), c.particleSize(), (int) Math.round(count * c.particleAmount()), spread);
    }

    public static void burst(ServerLevel level, Vec3 at, String element, int color, float size, int count, double spread) {
        if (count <= 0) return;
        level.sendParticles(particle(element, color, size), at.x, at.y, at.z, count, spread, spread, spread, 0.02);
        int accents = count / 3;
        if (accents > 0) level.sendParticles(accent(element, color, size), at.x, at.y, at.z, accents, spread, spread, spread, 0.02);
    }

    /** The sound of a shot of the element. */
    public static SoundEvent shotSound(String element) {
        return switch (element) {
            case "fogo" -> SoundEvents.BLAZE_SHOOT;
            case "gelo" -> SoundEvents.SNOW_GOLEM_SHOOT;
            case "raio" -> SoundEvents.TRIDENT_THUNDER.value();
            case "agua" -> SoundEvents.DROWNED_SHOOT;
            case "sombra", "magia" -> SoundEvents.EVOKER_CAST_SPELL;
            case "som" -> SoundEvents.NOTE_BLOCK_BELL.value();
            default -> SoundEvents.SHULKER_SHOOT;
        };
    }

    /** The sound of something of the element hitting. */
    public static SoundEvent impactSound(String element) {
        return switch (element) {
            case "fogo" -> SoundEvents.FIRECHARGE_USE;
            case "gelo" -> SoundEvents.GLASS_BREAK;
            case "raio" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case "agua" -> SoundEvents.GENERIC_SPLASH;
            case "sombra", "magia" -> SoundEvents.ILLUSIONER_CAST_SPELL;
            default -> SoundEvents.SHULKER_BULLET_HIT;
        };
    }

    /** What the element does to whoever it hits: fire burns, ice slows, gas poisons, sound stuns... */
    public static void elementHit(String element, LivingEntity target, ServerPlayer by) {
        switch (element) {
            case "fogo" -> target.igniteForSeconds(4.0F);
            case "gelo" -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 60));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), by);
            }
            case "gas" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), by);
            case "sombra" -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), by);
            case "agua" -> target.clearFire();
            case "som", "raio" -> {
                if (target instanceof Mob mob) Hits.stun(mob, by, 20);
            }
            default -> {
            }
        }
    }

    /** Shakes the camera of this player ({@code strength} 0 to 1). */
    public static void shake(ServerPlayer player, float strength, int ticks) {
        if (strength <= 0 || player.connection == null) return;
        ModNetwork.CHANNEL.send(new EngineFxPacket(EngineFxPacket.SHAKE, strength, ticks), PacketDistributor.PLAYER.with(player));
    }

    /** Zooms the camera of this player to {@code fov} (0.6 close, 1.3 wide) for a moment. */
    public static void zoom(ServerPlayer player, float fov, int ticks) {
        if (fov <= 0 || fov == 1.0F || player.connection == null) return;
        ModNetwork.CHANNEL.send(new EngineFxPacket(EngineFxPacket.ZOOM, fov, ticks), PacketDistributor.PLAYER.with(player));
    }

    /** The normal tick rate of the game. */
    private static final float NORMAL_RATE = 20.0F;
    /** The tick rate a running slow motion set, so its end only puts back its own. */
    private static float slowRate = 0.0F;

    /**
     * A short slow motion: alone in a single player world the game itself runs slower for
     * {@code ticks} (at {@code speed} of the normal rate); with other players around it would slow
     * them too, so then only the creatures near the player slow down.
     */
    public static void slowMotion(ServerPlayer player, float speed, int ticks) {
        if (ticks <= 0 || speed >= 1.0F) return;
        MinecraftServer server = player.level().getServer();
        ServerTickRateManager rates = server.tickRateManager();
        if (server.isSingleplayer() && !server.isPublished() && server.getPlayerCount() == 1 && rates.tickrate() == NORMAL_RATE) {
            float rate = NORMAL_RATE * Math.max(0.2F, speed);
            slowRate = rate;
            rates.setTickRate(rate);
            PresetRuntime.start(player, PresetRuntime.Task.ending(age -> age < ticks, () -> {
                if (rates.tickrate() == slowRate) rates.setTickRate(NORMAL_RATE);
                slowRate = 0.0F;
            }));
            return;
        }
        int level = speed < 0.35F ? 3 : speed < 0.6F ? 2 : 1;
        for (LivingEntity near : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12),
                e -> e != player && !(e instanceof net.minecraft.world.entity.player.Player))) {
            near.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, level, false, false), player);
        }
    }

    /**
     * The camera settings every ability may have: {@code "zoom"} (the field of view, with
     * {@code "zoom_duracao"} in seconds) and {@code "camera_lenta"} (seconds of slow motion, at
     * {@code "camera_lenta_velocidade"} of the normal speed). Run after the ability went off.
     */
    public static void camera(PresetContext c) {
        CharacterData.Settings s = c.settings();
        if (s.has("zoom")) zoom(c.player(), (float) s.number("zoom", 1.0), (int) Math.round(s.number("zoom_duracao", 0.8) * 20));
        if (s.has("camera_lenta")) slowMotion(c.player(), (float) s.number("camera_lenta_velocidade", 0.4), (int) Math.round(s.number("camera_lenta", 1.0) * 20));
    }

    /**
     * The sound of the ability's class (3 variations each), with its pitch ({@code "som_altura"}),
     * volume ({@code "som_volume"}) and timbre ({@code "som_timbre"}: grave, agudo, metal, magia,
     * energia, natural); {@code "som": false} silences it.
     */
    public static void classSound(PresetContext c) {
        CharacterData.Settings s = c.settings();
        if (!s.flag("som", true)) return;
        float pitch = (float) Math.max(0.5, Math.min(2.0, s.number("som_altura", 1.0)));
        float volume = (float) Math.max(0.0, Math.min(2.0, s.number("som_volume", 0.7)));
        String timbre = s.text("som_timbre", "padrao");
        if (timbre.equals("grave")) pitch *= 0.75F;
        if (timbre.equals("agudo")) pitch *= 1.3F;
        ServerPlayer player = c.player();
        Fx.play(player.level(), player, EngineContent.CLASS_SOUNDS.get(c.ability().kind()).get(), volume, pitch);
        SoundEvent accent = switch (timbre) {
            case "metal" -> SoundEvents.ANVIL_LAND;
            case "magia" -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case "energia" -> SoundEvents.BEACON_POWER_SELECT;
            case "natural" -> SoundEvents.AZALEA_LEAVES_BREAK;
            default -> null;
        };
        if (accent != null) Fx.play(player.level(), player, accent, volume * 0.35F, pitch);
    }
}
