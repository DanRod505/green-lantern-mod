package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;
import static com.danrod505.greenlantern.gametest.heroes.PresetTests.*;

import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.PresetFx;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.engine.preset.Summons;
import com.danrod505.greenlantern.engine.preset.Transformations;
import com.danrod505.greenlantern.engine.preset.Visuals;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The presets of stage 2, batch 5: visuals and sounds (element particles, beams, constructs,
 * camera, the sounds of each class, the wheel icons) and the 15 transformations, on the test
 * character Protótipo.
 */
public final class PresetLot5Tests {
    private PresetLot5Tests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("preset_batch5_sounds", () -> PresetLot5Tests::sounds);
        tests.register("preset_transformations", () -> PresetLot5Tests::transformations);
        tests.register("preset_transformation_check", () -> PresetLot5Tests::transformationCheck);
        tests.register("preset_visual_settings", () -> PresetLot5Tests::visualSettings);
        tests.register("preset_camera", () -> PresetLot5Tests::camera);
        tests.register("preset_beams_and_constructs", () -> PresetLot5Tests::beamsAndConstructs);
        tests.register("preset_wheel_icons", () -> PresetLot5Tests::wheelIcons);
    }

    /** The entities a transformation or construct of the player put in the world. */
    private static <T extends Entity> List<T> owned(ServerPlayer player, Class<T> type) {
        return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(24.0), e -> player.getUUID().equals(Summons.owner(e)));
    }

    /** A sound set for each of the 10 classes and a sound for each of the 15 transformations. */
    public static void sounds(GameTestHelper helper) {
        helper.assertTrue(EngineContent.CLASS_SOUNDS.size() == AbilityKind.values().length, "a sound set per class");
        for (AbilityKind kind : AbilityKind.values()) helper.assertTrue(EngineContent.CLASS_SOUNDS.get(kind).get() != null, "sound of " + kind.id());
        helper.assertTrue(Transformations.IDS.size() == 15, "15 transformations");
        for (String id : Transformations.IDS) helper.assertTrue(EngineContent.TRANSFORM_SOUNDS.get(id).get() != null, "sound of " + id);
        helper.succeed();
    }

    /**
     * All 15 play on a suited player at once: the bats of the shadows and the flying armor pieces
     * show up, and when they end nothing is left (no entity, no growth, no invisibility).
     */
    public static void transformations(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        EngineHero hero = prototype();
        double size = player.getAttributeValue(Attributes.SCALE);
        helper.assertFalse(Transformations.play(player, hero, Transformations.NONE), "padrao plays nothing");
        helper.assertFalse(Transformations.play(player, hero, "inexistente"), "an unknown one plays nothing");
        for (String id : Transformations.IDS) helper.assertTrue(Transformations.play(player, hero, id), id + " plays");
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(!owned(player, Bat.class).isEmpty(), "the bats of the shadows come");
                    helper.assertTrue(!owned(player, Display.ItemDisplay.class).isEmpty(), "the suit pieces and weapons fly");
                })
                .thenExecuteAfter(20, () -> helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > size, "the beast grows"))
                .thenWaitUntil(() -> helper.assertTrue(PresetRuntime.running(player) == 0, "they all end"))
                .thenExecute(() -> {
                    helper.assertTrue(owned(player, Entity.class).isEmpty(), "nothing is left in the world");
                    helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - size) < 1.0E-6, "back to the normal size");
                    helper.assertFalse(player.hasEffect(MobEffects.INVISIBILITY), "visible again after the Boom Tube");
                    remove(player);
                })
                .thenSucceed();
    }

    /** A character file must name one of the 15 (or padrao). */
    public static void transformationCheck(GameTestHelper helper) {
        CharacterData d = prototype().data();
        for (String id : List.of("raio", "anel", "padrao", "nada")) {
            CharacterData changed = new CharacterData(d.id(), d.name(), d.characterClass(), d.level(), d.skeleton(), id, d.theme(), d.legacy(), d.source(),
                    d.stats(), d.suit(), d.abilities(), d.skins(), d.weaknesses(), d.colors(), d.visual(), d.traits());
            boolean bad = Characters.check(changed, d.id()).stream().anyMatch(p -> p.contains("transformacao"));
            helper.assertTrue(bad == id.equals("nada"), "transformacao " + id + (bad ? " refused" : " accepted"));
        }
        helper.succeed();
    }

    /** The colour, size and amount of an ability's particles, and every element has its family. */
    public static void visualSettings(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        EngineHero hero = prototype();
        EngineAbility plain = ability("soco_forte", AbilityKind.STRIKE, 1, 1, 3, 0, 0, settings());
        EngineAbility tuned = ability("soco_forte", AbilityKind.STRIKE, 1, 1, 3, 0, 0, settings("cor", "#FF2040", "particula_tamanho", 2.0, "particula_quantidade", 0));
        PresetContext a = new PresetContext(player, hero, player.getMainHandItem(), plain);
        PresetContext b = new PresetContext(player, hero, player.getMainHandItem(), tuned);
        helper.assertTrue(a.color() == hero.data().colors().main(), "the character's colour by default");
        helper.assertTrue(b.color() == 0xFF2040, "the ability's own colour");
        helper.assertTrue(b.particleSize() == 2.0F && b.particleAmount() == 0.0, "size and amount");
        helper.assertTrue(PresetFx.particle(b) instanceof DustParticleOptions dust && dust.getScale() > 2.0F, "bigger energy dust");
        for (String element : PresetFx.ELEMENTS) {
            helper.assertTrue(PresetFx.particle(element, 0xFFFFFF, 1.0F) != null && PresetFx.accent(element, 0xFFFFFF, 1.0F) != null, element + " has a family");
        }
        remove(player);
        helper.succeed();
    }

    /**
     * The camera settings run after an ability goes off: a short slow motion here (a server with no
     * single player) slows the creatures near the player down instead of the game.
     */
    public static void camera(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie foe = dummy(helper, 7.5, 1, 6.5);
        float rate = helper.getLevel().getServer().tickRateManager().tickrate();
        EngineAbility slow = ability("pisao", AbilityKind.STRIKE, 0, 1, 0, 2, 0, settings("zoom", 0.7, "camera_lenta", 1.5, "som_timbre", "metal"));
        helper.assertTrue(use(player, slow), "the stomp goes off");
        helper.assertTrue(foe.hasEffect(MobEffects.SLOWNESS), "the zombie near is slowed");
        helper.assertTrue(helper.getLevel().getServer().tickRateManager().tickrate() == rate, "the game keeps its speed");
        remove(player);
        helper.succeed();
    }

    /** Every beam form draws, and a solid construct is see-through glass that goes away with it. */
    public static void beamsAndConstructs(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Vec3 from = player.getEyePosition();
        for (String form : List.of("reto", "espiral", "raio")) {
            Visuals.beam(helper.getLevel(), from, from.add(0, 0, 8), 0x40FF60, PresetFx.dust(0xFFFFFF, 0.6F), form, 2.0, 3);
        }
        dummy(helper, 7.5, 1, 8.5);
        EngineAbility construct = ability("construto", AbilityKind.SUMMON, 1, 1, 10, 0, 1, settings("forma", "broca", "escala", 1.5));
        helper.assertTrue(use(player, construct), "the construct goes off");
        List<Display.BlockDisplay> pieces = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    pieces.addAll(owned(player, Display.BlockDisplay.class));
                    helper.assertTrue(pieces.size() == 3, "the drill is three glowing boxes, has " + pieces.size());
                })
                .thenWaitUntil(() -> helper.assertTrue(PresetRuntime.running(player) == 0, "the construct ends"))
                .thenExecute(() -> {
                    helper.assertTrue(pieces.stream().allMatch(Entity::isRemoved), "and its boxes go");
                    remove(player);
                })
                .thenSucceed();
    }

    /** Every preset has a shape on the wheel; an ability picks another with icone, a colour with icone_cor. */
    public static void wheelIcons(GameTestHelper helper) {
        List<String> presets = new ArrayList<>(Presets.fightIds());
        presets.addAll(PresetLot4Tests.BATCH_4);
        // A power of before takes the shape its ability picks with icone, or its class's.
        presets.remove(com.danrod505.greenlantern.engine.OwnPowers.PRESET);
        for (String preset : presets) {
            String shape = AbilityIcons.presetShape(preset);
            helper.assertTrue(shape != null && AbilityIcons.hasShape(shape), "preset " + preset + " has an icon");
        }
        CharacterData data = prototype().data();
        List<AbilityKind> kinds = data.abilities().stream().map(a -> a.kind().orElseThrow()).toList();
        int[] sheet = AbilityIcons.sheet(data, kinds);
        int width = AbilityIcons.SIZE * AbilityIcons.cells(data);
        helper.assertTrue(sheet.length == width * AbilityIcons.SIZE, "one cell per ability, function and trait, and the emblem");
        for (int cell = 0; cell < AbilityIcons.cells(data); cell++) {
            boolean drawn = false;
            for (int y = 0; y < AbilityIcons.SIZE && !drawn; y++) {
                for (int x = 0; x < AbilityIcons.SIZE && !drawn; x++) drawn = sheet[y * width + cell * AbilityIcons.SIZE + x] != 0;
            }
            helper.assertTrue(drawn, "cell " + cell + " is drawn");
        }
        CharacterData.Ability picked = ability("soco_forte", AbilityKind.STRIKE, 1, 1, 3, 0, 0, settings("icone", "coracao", "icone_cor", "#123456")).data();
        helper.assertTrue(AbilityIcons.shape(picked, AbilityKind.STRIKE).equals("coracao"), "icone picks the shape");
        helper.assertTrue(AbilityIcons.color(picked, data) == 0x123456, "icone_cor picks the colour");
        int[] heart = AbilityIcons.icon("coracao", 0x123456, data.colors());
        boolean painted = false;
        for (int pixel : heart) painted |= (pixel & 0xFFFFFF) == 0x123456;
        helper.assertTrue(painted, "painted in its colour");
        helper.succeed();
    }
}
