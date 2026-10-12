package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.engine.preset.Visuals;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * The server side of the characters of the engine: the class energy (how it fills), the abilities
 * (energy, cooldown, Supreme charge, the use in a fight and in the world), the skins, the class
 * passives and the weaknesses. Everything is decided here; the client only draws.
 * <p>
 * None of this touches the first 9 heroes: they are not {@link EngineHero}s.
 */
public final class EngineServer {
    /** Supreme charge for each point of damage dealt or taken. */
    private static final int SUPREME_PER_DAMAGE = 10;
    /** Hearts of damage the ring bearer's light shield takes in each fight. */
    private static final float LIGHT_SHIELD = 4.0F;
    /** Seconds without damage before a new fight starts (for the light shield). */
    private static final int FIGHT_GAP_TICKS = 200;

    private static final Map<UUID, Double> RECHARGE_REST = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> SHIELD = new ConcurrentHashMap<>();

    private EngineServer() {}

    public static void register() {
        LivingHurtEvent.BUS.addListener(Priority.LOW, EngineServer::onHurt);
        ShieldBlockEvent.BUS.addListener(EngineServer::onShieldBlock);
        LivingEntityUseItemEvent.Finish.BUS.addListener(EngineServer::onAte);
        PresetRuntime.register();
    }

    /** The character of the engine whose suit the player wears, or null. */
    public static EngineHero suited(Player player) {
        return HeroRegistry.suited(player).orElse(null) instanceof EngineHero hero ? hero : null;
    }

    public static AbilityState state(ItemStack item) {
        AbilityState state = item.get(EngineContent.ABILITIES.get());
        return state != null ? state : AbilityState.EMPTY;
    }

    public static SkinState skin(EngineHero hero, ItemStack item) {
        SkinState state = item.get(EngineContent.SKIN.get());
        return state != null ? state : new SkinState(hero.data().defaultSkin().id(), List.of());
    }

    // ---- abilities ---------------------------------------------------------------------------------

    /**
     * Uses an ability in a fight (the power key, or right click with the item): it needs the suit,
     * a finished cooldown, enough energy and, for the Supreme, a full Supreme bar. Returns whether
     * it went off.
     */
    public static boolean useAbility(ServerPlayer player, ItemStack item, EngineAbility ability) {
        EngineHero hero = owner(item);
        if (hero == null) return false;
        if (!hero.isSuited(player)) {
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_need_suit"), true);
            return false;
        }
        if (PresetRuntime.stop(player, ability.data().id())) return true;
        if (PresetRuntime.undo(player, ability.data().id())) return true;
        if (!ready(player, hero, item, ability)) return false;
        return finish(player, hero, item, ability, Presets.useInFight(player, hero, item, ability));
    }

    /** Uses the chosen ability in the world (crouch with the power key, or crouch and right click). */
    public static boolean useInWorld(ServerPlayer player, EngineHero hero, ItemStack item) {
        if (item.isEmpty() || !hero.isSuited(player)) return false;
        return useInWorld(player, hero, item, hero.powers().set().selected(item));
    }

    /** Uses this ability in the world. */
    public static boolean useInWorld(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        if (item.isEmpty() || !hero.isSuited(player)) return false;
        String key = PresetRuntime.worldKey(ability.data().id());
        if (PresetRuntime.stop(player, key) || PresetRuntime.undo(player, key)) return true;
        if (!ready(player, hero, item, ability)) return false;
        return finish(player, hero, item, ability, Presets.useInWorld(player, hero, item, ability));
    }

    /** The Supreme key: with the Supreme bar full, uses the Supreme (always the last ability). */
    public static boolean useSupreme(ServerPlayer player, EngineHero hero) {
        ItemStack item = hero.findItem(player);
        if (item.isEmpty() || !hero.isSuited(player)) return false;
        EngineAbility supreme = hero.powers().power(hero.powers().set().count() - 1) instanceof EngineAbility a ? a : null;
        if (supreme == null || !supreme.supreme()) return false;
        return useAbility(player, item, supreme);
    }

    private static boolean ready(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        AbilityState state = state(item);
        long now = player.level().getGameTime();
        long left = state.remaining(ability.ordinal(), now);
        if (left > 0) {
            refuse(player, Component.translatable("message.greenlantern.engine_cooldown", ability.displayName(), String.format("%.1f", left / 20.0)));
            return false;
        }
        if (ability.supreme() && !state.supremeFull() && !player.isCreative()) {
            refuse(player, Component.translatable("message.greenlantern.engine_supreme_not_ready", Math.round(state.supremeFraction() * 100)));
            return false;
        }
        if (!player.isCreative() && !hero.energy().tryConsume(item, ability.cost())) {
            refuse(player, Component.translatable("message.greenlantern.engine_no_energy", hero.data().characterClass().energyName()));
            return false;
        }
        return true;
    }

    /**
     * After the preset: the cooldown (the ability's, or the one the preset asked for) and, for the
     * Supreme, an empty bar. A use that could not go off gives the energy back and says why.
     */
    private static boolean finish(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability, Presets.Outcome outcome) {
        if (!outcome.went()) {
            if (!player.isCreative()) hero.energy().add(item, ability.cost());
            refuse(player, outcome.reason());
            return false;
        }
        long now = player.level().getGameTime();
        int cooldown = outcome.cooldownTicks() > 0 ? outcome.cooldownTicks() : ability.cooldownTicks();
        AbilityState state = state(item).withCooldown(ability.ordinal(), now + cooldown, now);
        if (ability.supreme()) state = state.withSupreme(0);
        item.set(EngineContent.ABILITIES.get(), state);
        return true;
    }

    /** Starts the ability's cooldown now: a lasting preset (a bubble, super speed) just ended. */
    public static void startCooldown(ServerPlayer player, EngineHero hero, EngineAbility ability) {
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        long now = player.level().getGameTime();
        item.set(EngineContent.ABILITIES.get(), state(item).withCooldown(ability.ordinal(), now + ability.cooldownTicks(), now));
    }

    /** Not enough energy, still cooling down, Supreme not full: a short sound and the reason. */
    private static void refuse(ServerPlayer player, Component reason) {
        player.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
        play(player, EngineContent.NO_ENERGY.get(), 0.6F, 0.8F);
    }

    private static EngineHero owner(ItemStack item) {
        return item.getItem() instanceof CharacterItem characterItem ? characterItem.hero() : null;
    }

    // ---- skins -------------------------------------------------------------------------------------

    /** Changes to the next skin the item has unlocked (every skin in creative). */
    public static void cycleSkin(ServerPlayer player, EngineHero hero, ItemStack item) {
        if (item.isEmpty()) return;
        List<CharacterData.Skin> skins = hero.data().skins();
        SkinState state = skin(hero, item);
        List<CharacterData.Skin> available = skins.stream()
                .filter(s -> s == skins.getFirst() || player.isCreative() || state.unlocked().contains(s.id())).toList();
        int index = 0;
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).id().equals(state.selected())) index = i;
        }
        CharacterData.Skin next = available.get((index + 1) % available.size());
        item.set(EngineContent.SKIN.get(), state.select(next.id()));
        showSkin(player, item);
        Component name = Component.translatableWithFallback("skin.greenlantern." + next.id(), next.name());
        if (available.size() == 1) {
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_one_skin", name), true);
        } else {
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_skin", name), true);
        }
        play(player, EngineContent.SKIN_CHANGE.get(), 0.8F, 1.0F);
    }

    // ---- suit and tick -----------------------------------------------------------------------------

    static void onSuitUp(ServerPlayer player, EngineHero hero) {
        ItemStack item = hero.findItem(player);
        if (!item.isEmpty() && item.get(EngineContent.SKIN.get()) == null) {
            item.set(EngineContent.SKIN.get(), new SkinState(hero.data().defaultSkin().id(), List.of()));
        }
        showSkin(player, item);
    }

    /** Copies the skin choice onto the chest piece: other players see the armor, not the inventory. */
    static void showSkin(Player player, ItemStack item) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        SkinState state = item.get(EngineContent.SKIN.get());
        if (chest.isEmpty() || state == null || chest == item) return;
        chest.set(EngineContent.SKIN.get(), state);
    }

    static void onSuitDown(ServerPlayer player, EngineHero hero) {
        SHIELD.remove(player.getUUID());
        PresetRuntime.stopLasting(player);
        Visuals.forget(player);
    }

    static void tick(ServerPlayer player, EngineHero hero) {
        if (hero.isSuited(player)) Visuals.tick(player, hero);
        if (player.tickCount % 20 != 0) return;
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        recharge(player, hero, item);
        if (!hero.isSuited(player)) return;
        passive(player, hero);
        weaknesses(player, hero, item);
    }

    private static void recharge(ServerPlayer player, EngineHero hero, ItemStack item) {
        RingEnergy energy = hero.energy().get(item);
        if (energy.capacity() != hero.energy().capacity()) hero.energy().set(item, energy.stored());
        if (energy.isFull()) return;
        double rate = rechargePerSecond(player, hero, item) + RECHARGE_REST.getOrDefault(player.getUUID(), 0.0);
        int whole = (int) Math.floor(rate);
        RECHARGE_REST.put(player.getUUID(), rate - whole);
        if (whole > 0) hero.energy().add(item, whole);
    }

    /** How much class energy comes back this second (see {@link RechargeCondition}). */
    public static double rechargePerSecond(ServerPlayer player, EngineHero hero, ItemStack item) {
        ClassRules.EnergyRule rule = hero.data().classRule().energy();
        double base = rule.recharge();
        double boosted = base * rule.multiplier();
        ServerLevel level = player.level();
        String source = hero.data().source().orElse("");
        return switch (rule.condition()) {
            case NEVER -> 0.0;
            case ALWAYS -> base;
            case SOURCE -> nearSource(player, source) ? boosted : base;
            case RUNNING -> player.isSprinting() ? boosted : base;
            case DARK -> player.isCrouching() || level.getMaxLocalRawBrightness(player.blockPosition()) < 8 ? boosted : base;
            case PAUSE -> level.getGameTime() - state(item).lastUse() > 40 ? boosted : 0.0;
            case NIGHT -> level.isDarkOutside() ? boosted * (level.getMoonBrightness(player.blockPosition()) >= 1.0F ? 1.5 : 1.0) : base;
            case REDSTONE -> nearRedstone(level, player.blockPosition()) ? boosted : base;
            case ELEMENT -> nearSource(player, source) ? boosted : base;
            case FULL -> player.getFoodData().getFoodLevel() < 6 ? 0.0 : player.getFoodData().getFoodLevel() >= 18 ? boosted : base;
        };
    }

    /** Whether the player is by the source of their energy or their element. */
    static boolean nearSource(ServerPlayer player, String source) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        return switch (source) {
            case "sol" -> level.isBrightOutside() && level.canSeeSky(pos.above());
            case "tempestade", "raio" -> level.isThundering();
            case "calor" -> level.dimension() == Level.NETHER || player.isInLava() || anyBlock(level, pos, 3, s -> s.getFluidState().is(FluidTags.LAVA));
            case "fogo" -> player.isOnFire() || player.isInLava() || anyBlock(level, pos, 3, s -> s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA));
            case "agua" -> player.isInWater() || level.isRainingAt(pos.above());
            case "gelo" -> level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel()) || anyBlock(level, pos, 3, s -> s.is(BlockTags.ICE) || s.is(BlockTags.SNOW));
            case "plantas" -> anyBlock(level, pos, 3, s -> s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS));
            case "vento" -> pos.getY() > 120 || player.getAbilities().flying;
            default -> false;
        };
    }

    private static boolean nearRedstone(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            if (level.hasNeighborSignal(pos)) return true;
        }
        return false;
    }

    private static boolean anyBlock(ServerLevel level, BlockPos center, int radius, java.util.function.Predicate<BlockState> test) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (test.test(level.getBlockState(pos))) return true;
        }
        return false;
    }

    // ---- passives ----------------------------------------------------------------------------------

    private static void passive(ServerPlayer player, EngineHero hero) {
        ServerLevel level = player.level();
        switch (hero.data().passive()) {
            case NIGHT_EYES -> {
                if (level.getMaxLocalRawBrightness(player.blockPosition()) < 8) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                }
            }
            case SEE_HIDDEN -> {
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0), e -> e != player && e.isInvisible())) {
                    other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
                }
            }
            case HEARTY -> {
                if (player.getFoodData().getFoodLevel() >= 18 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
            }
            case ELEMENT_IMMUNE -> {
                if (hero.data().source().orElse("").equals("veneno")) player.removeEffect(MobEffects.POISON);
            }
            default -> {
            }
        }
    }

    static float fallDamage(Player player, EngineHero hero) {
        return switch (hero.data().passive()) {
            case UNSHAKEABLE -> 0.0F;
            case QUICK_STEP -> player.isSprinting() ? 0.0F : -1.0F;
            default -> -1.0F;
        };
    }

    /** Damage multiplier while suited: the elementalist's immunity and the damage weaknesses. */
    static float damageTaken(ServerPlayer player, EngineHero hero, DamageSource source) {
        float multiplier = 1.0F;
        if (hero.data().passive() == Passive.ELEMENT_IMMUNE && immune(hero.data().source().orElse(""), source)) return 0.0F;
        for (CharacterData.Weakness weakness : hero.data().weaknesses()) {
            if (!weakness.type().equals("dano")) continue;
            for (Identifier type : weakness.damageTypes()) {
                if (source.typeHolder().is(type)) multiplier *= (float) weakness.multiplier();
            }
        }
        return multiplier;
    }

    private static boolean immune(String element, DamageSource source) {
        return switch (element) {
            case "fogo" -> source.is(DamageTypeTags.IS_FIRE);
            case "gelo" -> source.is(DamageTypeTags.IS_FREEZING);
            case "raio" -> source.is(DamageTypeTags.IS_LIGHTNING);
            case "agua" -> source.is(DamageTypeTags.IS_DROWNING);
            default -> false;
        };
    }

    // ---- weaknesses --------------------------------------------------------------------------------

    private static void weaknesses(ServerPlayer player, EngineHero hero, ItemStack item) {
        for (CharacterData.Weakness weakness : hero.data().weaknesses()) {
            if (!weakness.type().equals("bloco_perto") || weakness.block().isEmpty()) continue;
            Block block = BuiltInRegistries.BLOCK.getValue(weakness.block().get());
            int radius = Mth.clamp((int) Math.ceil(weakness.reach()), 1, 8);
            if (!anyBlock(player.level(), player.blockPosition(), radius, s -> s.is(block))) continue;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, true));
            hero.energy().drain(item, Math.max(1, hero.energy().capacity() / 50));
            player.hurtServer(player.level(), player.damageSources().magic(), 1.0F);
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_weakness",
                    weakness.description().map(Component::literal).orElse(Component.translatable(block.getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN), true);
        }
    }

    // ---- events ------------------------------------------------------------------------------------

    /** Dealing and taking damage fills the Supreme; blows that land refill some classes; the ring bearer's light shield. */
    private static void onHurt(LivingHurtEvent event) {
        float amount = event.getAmount();
        if (amount <= 0) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            EngineHero hero = suited(attacker);
            if (hero != null) {
                ItemStack item = hero.findItem(attacker);
                if (!item.isEmpty()) {
                    chargeSupreme(attacker, item, amount);
                    int perHit = hero.data().classRule().energy().perHit();
                    if (perHit > 0) hero.energy().add(item, perHit);
                }
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            EngineHero hero = suited(victim);
            if (hero == null) return;
            ItemStack item = hero.findItem(victim);
            if (item.isEmpty()) return;
            if (hero.data().passive() == Passive.LIGHT_SHIELD && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                amount = lightShield(victim, hero, item, amount);
                event.setAmount(amount);
            }
            chargeSupreme(victim, item, amount);
        }
    }

    /** The first 2 hearts of damage of each fight come out of the ring's charge (10 charge per point). */
    private static float lightShield(ServerPlayer player, EngineHero hero, ItemStack item, float amount) {
        long now = player.level().getGameTime();
        float[] shield = SHIELD.computeIfAbsent(player.getUUID(), id -> new float[]{0.0F, 0.0F});
        if (now - (long) shield[1] > FIGHT_GAP_TICKS) shield[0] = 0.0F;
        shield[1] = now;
        float left = LIGHT_SHIELD - shield[0];
        if (left <= 0) return amount;
        float absorbed = Math.min(left, amount);
        int cost = Math.round(absorbed * 10);
        if (!hero.energy().has(item, cost)) return amount;
        hero.energy().drain(item, cost);
        shield[0] += absorbed;
        return amount - absorbed;
    }

    private static void chargeSupreme(ServerPlayer player, ItemStack item, float damage) {
        AbilityState state = state(item);
        if (state.supremeFull()) return;
        AbilityState next = state.withSupreme(state.supreme() + Math.round(damage * SUPREME_PER_DAMAGE));
        item.set(EngineContent.ABILITIES.get(), next);
        if (next.supremeFull()) {
            play(player, EngineContent.SUPREME_READY.get(), 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.greenlantern.engine_supreme_ready").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** The warrior's Fury comes back when a shield blocks. */
    private static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EngineHero hero = suited(player);
        if (hero == null || hero.data().passive() != Passive.FURY_BLOCK) return;
        ItemStack item = hero.findItem(player);
        if (!item.isEmpty()) hero.energy().add(item, 10);
    }

    /** The shapeshifter: food fills more and turns into Instinct. */
    private static void onAte(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EngineHero hero = suited(player);
        if (hero == null || hero.data().passive() != Passive.HEARTY) return;
        var food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food == null) return;
        player.getFoodData().eat(Math.max(1, food.nutrition() / 2), 0.3F);
        ItemStack item = hero.findItem(player);
        if (!item.isEmpty()) hero.energy().add(item, food.nutrition() * 10);
    }

    // ---- helpers -----------------------------------------------------------------------------------

    static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A puff of the character's colours around the player (the provisional look of an ability without a preset). */
    static void puff(ServerPlayer player, EngineHero hero) {
        CharacterData.Colors colors = hero.data().colors();
        ServerLevel level = player.level();
        for (int color : new int[]{colors.main(), colors.second()}) {
            level.sendParticles(new DustParticleOptions(color, 1.2F), player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.5, 0.6, 0.5, 0.0);
        }
    }
}
