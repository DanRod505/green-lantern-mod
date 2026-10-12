package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Empowerment (fortalecimento) presets: the aura, healing, growing, fury and the senses. They change
 * the player's body for a while (see {@link PresetBody}) and put it all back when they end; all but
 * the healing are lasting presets, which the key turns off. Numbers: {@code duracao}, {@code alcance};
 * the rest are settings.
 */
public final class EmpowerPresets {
    /** The most ores the senses show at once. */
    static final int MAX_ORES = 32;

    private EmpowerPresets() {}

    public static void register() {
        Presets.registerFight("aura", EmpowerPresets::aura);
        Presets.registerFight("cura", EmpowerPresets::heal);
        Presets.registerFight("crescer", EmpowerPresets::grow);
        Presets.registerFight("furia", EmpowerPresets::fury);
        Presets.registerFight("sentidos", EmpowerPresets::senses);
    }

    /** Points around the body, rising with {@code age}: the look of a power that is on. */
    static void glow(ServerLevel level, ServerPlayer player, ParticleOptions particle, int age, int count) {
        double height = player.getBbHeight();
        double width = player.getBbWidth() * 0.9;
        for (int i = 0; i < count; i++) {
            double a = Mth.TWO_PI * i / count + age * 0.25;
            double y = (age * 0.07 + (double) i / count) % 1.0 * height;
            level.sendParticles(particle, player.getX() + Math.cos(a) * width, player.getY() + y, player.getZ() + Math.sin(a) * width, 1, 0, 0.02, 0, 0);
        }
    }

    // ---- aura --------------------------------------------------------------------------------------

    /**
     * Aura: more of one thing for {@code duracao} seconds (10). Settings: {@code atributo} (dano,
     * velocidade, defesa or todos; dano), {@code quanto} (0.5: +50% damage or speed, half the damage
     * taken away), {@code cor} (the aura's colour, the character's when absent).
     */
    static Outcome aura(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        String what = s.text("atributo", "dano");
        double amount = Math.max(0.0, s.number("quanto", 0.5));
        int ticks = c.durationTicks(10.0);
        boolean all = what.equals("todos");
        Identifier damage = all || what.equals("dano") ? PresetBody.times(c, Attributes.ATTACK_DAMAGE, "aura_damage", amount) : null;
        Identifier speed = all || what.equals("velocidade") ? PresetBody.times(c, Attributes.MOVEMENT_SPEED, "aura_speed", amount) : null;
        Guards.Guard guard = all || what.equals("defesa") ? new Guards.Guard(Math.min(0.8, amount), 180.0, false, 1.0) : null;
        if (guard != null) Guards.add(player, guard);
        int color = (int) s.number("cor", c.lightColor());
        ParticleOptions particle = c.element().equals(PresetFx.ENERGY) ? new DustParticleOptions(color, 1.0F) : PresetFx.particle(c.element(), color);
        Fx.play(level, player, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.4F);
        level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 2 == 0) glow(level, player, particle, age, 6);
            return age < ticks;
        }, () -> {
            if (damage != null) PresetBody.remove(player, Attributes.ATTACK_DAMAGE, damage);
            if (speed != null) PresetBody.remove(player, Attributes.MOVEMENT_SPEED, speed);
            if (guard != null) Guards.remove(player, guard);
            Fx.play(level, player, SoundEvents.BEACON_DEACTIVATE, 0.6F, 1.4F);
        }));
    }

    // ---- cura --------------------------------------------------------------------------------------

    /**
     * Cura: gives back {@code quanto} health (8, four hearts) at once, or little by little over
     * {@code tempo} seconds (0: at once). It does not go off with full health.
     */
    static Outcome heal(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        float amount = (float) Math.max(0.0, s.number("quanto", 8.0));
        if (player.getHealth() >= player.getMaxHealth()) {
            return Outcome.fail(Component.translatable("message.greenlantern.engine_full_health"));
        }
        int ticks = (int) Math.round(Math.max(0.0, s.number("tempo", 0.0)) * 20.0);
        ParticleOptions particle = c.element().equals(PresetFx.ENERGY) ? ParticleTypes.HEART : PresetFx.particle(c.element(), c.lightColor());
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.5F);
        if (ticks <= 0) {
            player.heal(amount);
            level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
            return Outcome.DONE;
        }
        float perTick = amount / ticks;
        PresetRuntime.start(player, age -> {
            player.heal(perTick);
            if (age % 10 == 0) level.sendParticles(particle, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.4, 0.4, 0.4, 0.02);
            return age < ticks;
        });
        return Outcome.DONE;
    }

    // ---- crescer -----------------------------------------------------------------------------------

    /**
     * Crescer: the character grows to {@code tamanho} times their size (1.8) for {@code duracao}
     * seconds (20), with {@code dano_extra} damage (4), {@code vida_extra} health (10, given at once)
     * and {@code firmeza} (0.6, how little they are pushed). It needs room overhead.
     */
    static Outcome grow(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double size = Mth.clamp(s.number("tamanho", 1.8), 1.0, 4.0);
        double width = player.getBbWidth() * size / 2.0;
        AABB room = new AABB(player.getX() - width, player.getY(), player.getZ() - width,
                player.getX() + width, player.getY() + player.getBbHeight() * size, player.getZ() + width);
        if (!level.noCollision(player, room)) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_space"));
        int ticks = c.durationTicks(20.0);
        double health = Math.max(0.0, s.number("vida_extra", 10.0));
        Identifier scale = PresetBody.add(c, Attributes.SCALE, "grow_size", size - 1.0);
        Identifier damage = PresetBody.add(c, Attributes.ATTACK_DAMAGE, "grow_damage", Math.max(0.0, s.number("dano_extra", 4.0)));
        Identifier life = PresetBody.add(c, Attributes.MAX_HEALTH, "grow_health", health);
        Identifier firm = PresetBody.add(c, Attributes.KNOCKBACK_RESISTANCE, "grow_firm", Mth.clamp(s.number("firmeza", 0.6), 0.0, 1.0));
        Identifier step = PresetBody.add(c, Attributes.STEP_HEIGHT, "grow_step", size - 1.0);
        player.heal((float) health);
        Fx.play(level, player, SoundEvents.RAVAGER_ROAR, 0.8F, 0.8F);
        Fx.dust(level, player.position(), 1.5, 16);
        PresetFx.shake(player, 0.25F, 8);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 20 == 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                Fx.play(level, player, SoundEvents.RAVAGER_STEP, 0.5F, 0.7F);
            }
            return age < ticks;
        }, () -> {
            Holder<Attribute>[] attributes = attributes(Attributes.SCALE, Attributes.ATTACK_DAMAGE, Attributes.MAX_HEALTH, Attributes.KNOCKBACK_RESISTANCE, Attributes.STEP_HEIGHT);
            Identifier[] ids = {scale, damage, life, firm, step};
            for (int i = 0; i < ids.length; i++) PresetBody.remove(player, attributes[i], ids[i]);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.6, 0.8, 0.6, 0.02);
            Fx.play(level, player, SoundEvents.PUFFER_FISH_BLOW_OUT, 1.0F, 0.6F);
        }));
    }

    @SafeVarargs
    private static Holder<Attribute>[] attributes(Holder<Attribute>... attributes) {
        return attributes;
    }

    // ---- furia -------------------------------------------------------------------------------------

    /**
     * Fúria: more damage and less defense for {@code duracao} seconds (8). Settings: {@code troca}
     * (0.5: +50% damage dealt and +50% damage taken).
     */
    static Outcome fury(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double trade = Mth.clamp(c.settings().number("troca", 0.5), 0.0, 3.0);
        int ticks = c.durationTicks(8.0);
        Identifier damage = PresetBody.times(c, Attributes.ATTACK_DAMAGE, "fury_damage", trade);
        Guards.Guard weak = new Guards.Guard(-trade, 180.0, false, 1.0);
        Guards.add(player, weak);
        ParticleOptions particle = c.element().equals(PresetFx.ENERGY) ? new DustParticleOptions(0xD01010, 1.1F) : PresetFx.particle(c);
        Fx.play(level, player, SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2.1, player.getZ(), 3, 0.3, 0.1, 0.3, 0);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 3 == 0) glow(level, player, particle, age, 4);
            if (age % 30 == 0) level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2.1, player.getZ(), 1, 0.2, 0.1, 0.2, 0);
            return age < ticks;
        }, () -> {
            PresetBody.remove(player, Attributes.ATTACK_DAMAGE, damage);
            Guards.remove(player, weak);
        }));
    }

    // ---- sentidos ----------------------------------------------------------------------------------

    /**
     * Sentidos: for {@code duracao} seconds (10) the character sees in the dark and through walls:
     * the creatures within {@code alcance} (24) glow and the nearest ores (up to 32) show their
     * outline in the character's colour. Settings: {@code mostra} (mobs, minerios or tudo; tudo).
     */
    static Outcome senses(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        String shows = c.settings().text("mostra", "tudo");
        boolean mobs = !shows.equals("minerios");
        boolean ores = !shows.equals("mobs");
        double reach = Math.min(32.0, c.reach(24.0));
        int ticks = c.durationTicks(10.0);
        Map<BlockPos, Display.BlockDisplay> shown = new HashMap<>();
        Fx.play(level, player, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.4F);
        Fx.play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 20 == 1) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
                if (mobs) {
                    for (LivingEntity mob : Targets.inRadius(level, player.position(), reach, e -> e != player && !Summons.summoned(e))) {
                        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
                    }
                }
            }
            if (ores && age % 40 == 1) showOres(c, reach, shown);
            return age < ticks;
        }, () -> {
            shown.values().forEach(Summons::dismiss);
            shown.clear();
            MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
            if (sight != null && sight.getDuration() <= 260) player.removeEffect(MobEffects.NIGHT_VISION);
        }));
    }

    /** Brings the shown ores up to date: the nearest ones within {@code reach}, the mined ones gone. */
    private static void showOres(PresetContext c, double reach, Map<BlockPos, Display.BlockDisplay> shown) {
        showBlocks(c, reach, shown, (level, pos) -> level.getBlockState(pos).is(Tags.Blocks.ORES));
    }

    /**
     * Brings the outlined blocks up to date: the nearest {@link #MAX_ORES} within {@code reach} that
     * pass {@code wanted}; the ones that no longer do lose their outline.
     */
    static void showBlocks(PresetContext c, double reach, Map<BlockPos, Display.BlockDisplay> shown, BiPredicate<ServerLevel, BlockPos> wanted) {
        ServerLevel level = c.level();
        BlockPos center = c.player().blockPosition();
        int r = (int) Math.ceil(reach);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) <= reach * reach && wanted.test(level, pos)) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        Set<BlockPos> keep = new HashSet<>(found.subList(0, Math.min(MAX_ORES, found.size())));
        shown.entrySet().removeIf(e -> {
            if (keep.contains(e.getKey()) && wanted.test(level, e.getKey())) return false;
            Summons.dismiss(e.getValue());
            return true;
        });
        for (BlockPos pos : keep) {
            if (shown.containsKey(pos)) continue;
            Display.BlockDisplay display = outline(c, pos, level.getBlockState(pos));
            if (display != null) shown.put(pos, display);
        }
    }

    /** A glowing copy of the block that shows its outline through walls. */
    private static Display.BlockDisplay outline(PresetContext c, BlockPos pos, BlockState state) {
        Display.BlockDisplay display = EntityType.BLOCK_DISPLAY.create(c.level(), EntitySpawnReason.TRIGGERED);
        if (display == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        tag.putInt("glow_color_override", c.lightColor());
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, c.level().registryAccess(), tag));
        display.setPos(pos.getX(), pos.getY(), pos.getZ());
        display.setGlowingTag(true);
        display.setNoGravity(true);
        return Summons.spawn(c, display);
    }

    /** How many blocks the player's senses (or search) outline right now (for the tests). */
    public static int shownOres(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(Display.BlockDisplay.class, player.getBoundingBox().inflate(32.0),
                d -> player.getUUID().equals(Summons.owner(d))).size();
    }
}
