package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.engine.preset.MovementPresets;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.PowerSet;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.FlightHandler;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraftforge.registries.RegistryObject;

/**
 * A character of the engine, built from its data file with no code of its own: the item (class
 * energy, cooldowns, Supreme charge, skin), the suit pieces with the stats of its class and level,
 * the 6 abilities on the wheel and the HUD. With the full suit on, the player is drawn as the
 * character, with its skin on its skeleton. The abilities run through their presets (stage 2); until
 * a preset exists the ability spends its energy, starts its cooldown and says it is not built yet.
 */
public final class EngineHero extends HeroDefinition {
    /** Shared icons of the 10 kinds of ability, then the generic emblem (16x16 each). */
    public static final int ICONS_WIDTH = 16 * (AbilityKind.values().length + 1);

    private final CharacterData data;
    private final RegistryObject<CharacterItem> item;
    private final HeroEnergy energy;
    private final HeroPowers<EngineAbility> powers;
    /** The settings of the character's "voo" ability, or null when it doesn't fly. */
    private final CharacterData.Settings flight;

    private EngineHero(CharacterData data, RegistryObject<CharacterItem> item, SuitSet suit) {
        super(data.id(), stack -> stack.is(item.get()), suit);
        this.data = data;
        this.item = item;
        this.energy = new HeroEnergy(EngineContent.ENERGY, data::energy);
        List<CharacterData.Ability> list = data.abilities();
        EngineAbility[] abilities = new EngineAbility[list.size()];
        for (int i = 0; i < abilities.length; i++) abilities[i] = new EngineAbility(list.get(i), i, list.get(i).kind().orElseThrow());
        this.powers = new HeroPowers<>(new PowerSet<>(abilities), energy, ChatFormatting.AQUA, 0.8F, 0.08F, EngineServer::useAbility, wheel(data));
        CharacterData.Ability fly = list.stream().filter(a -> a.preset().equals(MovementPresets.FLIGHT)).findFirst().orElse(null);
        this.flight = fly == null ? null : fly.settings();
    }

    /** Builds the character of a data file and registers its item and suit pieces. */
    public static EngineHero create(CharacterData data) {
        EngineHero[] self = new EngineHero[1];
        RegistryObject<CharacterItem> item = EngineContent.characterItem(data, () -> self[0]);
        ArmorMaterial material = Characters.armorMaterial(data.id(), Characters.SKIN_SUIT);
        RegistryObject<SuitArmorItem> head = EngineContent.suitPiece(data, material, ArmorType.HELMET, "cabeca");
        RegistryObject<SuitArmorItem> chest = EngineContent.suitPiece(data, material, ArmorType.CHESTPLATE, "peito");
        RegistryObject<SuitArmorItem> legs = EngineContent.suitPiece(data, material, ArmorType.LEGGINGS, "pernas");
        RegistryObject<SuitArmorItem> feet = EngineContent.suitPiece(data, material, ArmorType.BOOTS, "pes");
        SuitSet suit = new SuitSet(head, chest, legs, feet, "message.greenlantern.engine_no_item",
                () -> SoundEvents.BEACON_ACTIVATE, () -> SoundEvents.BEACON_DEACTIVATE, Characters.suitModifiers(data.id()));
        self[0] = new EngineHero(data, item, suit);
        return self[0];
    }

    /** Wheel colours worked out from the character's colours. */
    private static WheelStyle wheel(CharacterData data) {
        CharacterData.Colors colors = data.colors();
        int main = colors.main();
        int second = colors.second();
        int light = colors.light();
        int dark = ARGB.scaleRGB(main, 0.25F) & 0xFFFFFF;
        int darker = ARGB.scaleRGB(main, 0.12F) & 0xFFFFFF;
        WheelTheme theme = new WheelTheme("engine_" + data.id() + "_wheel", dark, darker, second, ARGB.scaleRGB(main, 0.7F) & 0xFFFFFF, darker, dark,
                ARGB.scaleRGB(main, 0.6F) & 0xFFFFFF, main, light, light, second);
        return new WheelStyle(theme, GreenLantern.id("textures/gui/engine/abilities.png"), ICONS_WIDTH, "wheel.greenlantern.engine_cost",
                "energy.greenlantern." + data.characterClass().id() + ".amount", 0x66000000 | darker, 0xFF000000 | light, 0xFF000000 | second,
                0xFFE8E8E8, 0xFFB0B0B0, 0xFF000000 | light, 0xFFB0B0B0);
    }

    public CharacterData data() {
        return data;
    }

    public CharacterItem item() {
        return item.get();
    }

    public HeroEnergy energy() {
        return energy;
    }

    @Override
    public HeroPowers<EngineAbility> powers() {
        return powers;
    }

    /** Whether all four suit pieces are on: only then the skin is the look. */
    public boolean fullSuit(Player player) {
        SuitSet suit = suit();
        return isSuited(player) && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(suit.head().get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(suit.legs().get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(suit.feet().get());
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(item.get().charged(item.get().getDefaultInstance()));
        output.accept(item.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return () -> new com.danrod505.greenlantern.client.engine.EngineHeroClient(this);
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        EngineServer.onSuitUp(player, this);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        EngineServer.onSuitDown(player, this);
    }

    @Override
    public void tick(ServerPlayer player) {
        EngineServer.tick(player, this);
    }

    /** A character with a "voo" ability power-flies like the heroes do (double tap jump, or the ability's key). */
    @Override
    public FlightProfile flightProfile() {
        return flight == null ? null : MovementPresets.flightProfile(data, flight);
    }

    /** Flight runs on the class energy (free in creative). */
    @Override
    public boolean canFly(Player player, ItemStack item) {
        return flight != null && !item.isEmpty() && (player.isCreative() || energy.get(item).stored() > 0);
    }

    @Override
    public void payFlight(ServerPlayer player, ItemStack item, float multiplier) {
        if (flight == null) return;
        int cost = (int) Math.round(flight.number("gasto_por_segundo", 2.0) * multiplier);
        if (cost <= 0) return;
        energy.drain(item, cost);
        if (energy.get(item).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.engine_flight_empty");
    }

    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return EngineServer.fallDamage(player, this);
    }

    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        return isSuited(player) ? EngineServer.damageTaken(player, this, source) : 1.0F;
    }

    @Override
    public int hudHeight() {
        return 70;
    }
}
