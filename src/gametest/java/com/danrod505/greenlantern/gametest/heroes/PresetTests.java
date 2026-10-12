package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.AbilitySlot;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The ability presets of stage 2, batch 1 (Golpe, Disparo, Explosão). Each test gives the test
 * character Protótipo an ability that names the preset, with its numbers and settings, uses it the
 * way the power key does and checks what it does to dummy pigs. No hero of the mod uses these yet.
 */
public final class PresetTests {
    private PresetTests() {}

    static final List<String> BATCH_1 = List.of(
            "soco_forte", "investida", "golpe_de_arma", "pisao", "agarrar_e_arremessar", "sequencia",
            "projetil", "feixe_continuo", "leque", "arremesso_que_volta", "balistico", "raio_do_ceu",
            "onda_em_volta", "explosao_no_ponto", "cone", "pulso", "chuva");

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("preset_batch1_registered", () -> PresetTests::registered);
        tests.register("preset_soco_forte", () -> PresetTests::strongPunch);
        tests.register("preset_investida", () -> PresetTests::charge);
        tests.register("preset_golpe_de_arma", () -> PresetTests::weaponSwing);
        tests.register("preset_pisao", () -> PresetTests::stomp);
        tests.register("preset_agarrar_e_arremessar", () -> PresetTests::grabAndThrow);
        tests.register("preset_sequencia", () -> PresetTests::combo);
        tests.register("preset_projetil", () -> PresetTests::projectile);
        tests.register("preset_feixe_continuo", () -> PresetTests::beam);
        tests.register("preset_leque", () -> PresetTests::fan);
        tests.register("preset_arremesso_que_volta", () -> PresetTests::boomerang);
        tests.register("preset_balistico", () -> PresetTests::ballistic);
        tests.register("preset_raio_do_ceu", () -> PresetTests::skyStrike);
        tests.register("preset_onda_em_volta", () -> PresetTests::wave);
        tests.register("preset_explosao_no_ponto", () -> PresetTests::pointBlast);
        tests.register("preset_cone", () -> PresetTests::cone);
        tests.register("preset_pulso", () -> PresetTests::pulse);
        tests.register("preset_chuva", () -> PresetTests::rain);
    }

    // ---- helpers ----------------------------------------------------------------------------------

    static EngineHero prototype() {
        return (EngineHero) HeroRegistry.byId("prototipo").orElseThrow();
    }

    /** The Protótipo suited, with a full bar, at (x, z) of the arena looking along +z (yaw 0). */
    static ServerPlayer suited(GameTestHelper helper, double x, double z) {
        EngineHero hero = prototype();
        ServerPlayer player = player(helper, x, 1, z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
        helper.assertTrue(hero.summonSuit(player), "the suit should go on");
        return player;
    }

    /** Settings from name, value pairs: numbers, words, yes/no and lists of numbers ({@code double[]}). */
    public static CharacterData.Settings settings(Object... pairs) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            JsonElement json;
            if (value instanceof Number n) json = new JsonPrimitive(n);
            else if (value instanceof Boolean b) json = new JsonPrimitive(b);
            else if (value instanceof double[] list) {
                JsonArray array = new JsonArray();
                for (double d : list) array.add(d);
                json = array;
            } else json = new JsonPrimitive(String.valueOf(value));
            map.put((String) pairs[i], json);
        }
        return new CharacterData.Settings(map);
    }

    /** An ability (first slot of the wheel) that runs {@code preset}: damage, cost 5, cooldown, reach, radius, duration. */
    public static EngineAbility ability(String preset, AbilityKind kind, double damage, double cooldown, double reach, double radius, double duration,
                                        CharacterData.Settings settings) {
        CharacterData.Numbers numbers = new CharacterData.Numbers(damage, 5, cooldown, reach, radius, duration);
        CharacterData.Ability data = new CharacterData.Ability("teste_" + preset, Optional.of(preset), Optional.of(kind), Optional.of(AbilitySlot.MAIN),
                preset, Optional.empty(), numbers, settings);
        return new EngineAbility(data, 0, kind);
    }

    static boolean use(ServerPlayer player, EngineAbility ability) {
        return EngineServer.useAbility(player, player.getMainHandItem(), ability);
    }

    static boolean hurt(Pig pig) {
        return pig.getHealth() < pig.getMaxHealth() || pig.isDeadOrDying();
    }

    static Pig pig(GameTestHelper helper, double x, double z) {
        return dummyPig(helper, x, 1, z);
    }

    // ---- tests ------------------------------------------------------------------------------------

    /** The 17 presets of batch 1 are there for the character files to name. */
    public static void registered(GameTestHelper helper) {
        for (String id : BATCH_1) helper.assertTrue(Presets.hasFight(id), "missing preset " + id);
        helper.succeed();
    }

    public static void strongPunch(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 3.5);
        Pig pig = pig(helper, 7.5, 5.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("soco_forte", AbilityKind.STRIKE, 4, 1, 0, 0, 0, settings("empurrao", 2.0))), "the punch goes off");
        helper.assertTrue(hurt(pig), "the punch hurts the pig");
        helper.assertTrue(pig.getDeltaMovement().z > 0.5, "and throws it back, got " + pig.getDeltaMovement());
        helper.assertTrue(EngineServer.state(player.getMainHandItem()).remaining(0, helper.getLevel().getGameTime()) > 0, "then it cools down");
        remove(player);
        helper.succeed();
    }

    public static void charge(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 6.5);
        double start = player.getZ();
        helper.assertTrue(use(player, ability("investida", AbilityKind.STRIKE, 3, 1, 7, 0, 0, settings("velocidade", 1.0))), "the charge goes off");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(player.getZ() - start > 4.0, "the charge carries the player forward, moved " + (player.getZ() - start));
                    helper.assertTrue(hurt(pig), "and rams the pig on the way");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void weaponSwing(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 6.5);
        Pig left = pig(helper, 9.0, 8.5);
        Pig right = pig(helper, 6.0, 8.5);
        Pig behind = pig(helper, 7.5, 3.5);
        helper.assertTrue(use(player, ability("golpe_de_arma", AbilityKind.STRIKE, 3, 1, 3.5, 0, 0, settings("arma", "machado"))), "the swing goes off");
        helper.assertTrue(hurt(left) && hurt(right), "the arc cuts both pigs in front");
        helper.assertFalse(hurt(behind), "but not the one behind");
        helper.startSequence().thenExecuteAfter(6, () -> remove(player)).thenSucceed();
    }

    public static void stomp(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        player.setOnGround(true);
        Pig a = pig(helper, 10.0, 7.5);
        Pig b = pig(helper, 7.5, 5.0);
        Pig far = pig(helper, 1.5, 1.5);
        helper.assertTrue(use(player, ability("pisao", AbilityKind.STRIKE, 3, 1, 0, 4, 0, settings())), "the stomp goes off");
        helper.assertTrue(hurt(a) && hurt(b), "the stomp hurts the pigs around");
        helper.assertTrue(a.getDeltaMovement().y > 0.3 && b.getDeltaMovement().y > 0.3, "and throws them up");
        helper.assertFalse(hurt(far), "but not one out of reach");
        remove(player);
        helper.succeed();
    }

    public static void grabAndThrow(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 3.5);
        // A pig with its AI on: only those fly when thrown (a dummy without AI hangs where it is let go).
        Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new Vec3(7.5, 1, 5.5));
        lookAt(player, pig);
        ItemStack item = player.getMainHandItem();
        int before = prototype().energy().get(item).stored();
        helper.assertFalse(use(player, ability("agarrar_e_arremessar", AbilityKind.STRIKE, 3, 1, 0, 0, 0, settings("peso_maximo", 5))),
                "a pig is too heavy for a weak grab");
        helper.assertTrue(prototype().energy().get(item).stored() == before, "a grab that fails gives the energy back");
        Vec3 start = pig.position();
        helper.assertTrue(use(player, ability("agarrar_e_arremessar", AbilityKind.STRIKE, 3, 1, 0, 0, 0, settings("distancia", 14))), "the grab goes off");
        helper.startSequence()
                .thenExecuteAfter(4, () -> helper.assertTrue(pig.getY() > player.getY() + 1.5, "the pig is lifted over the head"))
                .thenWaitUntil(() -> helper.assertTrue(hurt(pig), "the pig lands and gets hurt"))
                .thenExecute(() -> {
                    helper.assertTrue(pig.position().distanceTo(start) > 2.0, "it was thrown away, moved " + pig.position().distanceTo(start));
                    remove(player);
                })
                .thenSucceed();
    }

    public static void combo(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 5.5);
        Pig pig = pig(helper, 7.5, 7.5);
        lookAt(player, pig);
        EngineAbility combo = ability("sequencia", AbilityKind.STRIKE, 1, 5, 0, 0, 0, settings("danos", new double[]{1, 1, 3}, "intervalo", 1.0));
        ItemStack item = player.getMainHandItem();
        helper.assertTrue(use(player, combo), "first blow");
        long left = EngineServer.state(item).remaining(0, helper.getLevel().getGameTime());
        helper.assertTrue(left > 0 && left <= 6, "between blows the wait is short, got " + left);
        helper.startSequence()
                .thenExecuteAfter(7, () -> helper.assertTrue(use(player, combo), "second blow in rhythm"))
                .thenExecuteAfter(7, () -> {
                    helper.assertTrue(use(player, combo), "third blow");
                    helper.assertTrue(Math.abs(pig.getHealth() - (pig.getMaxHealth() - 5)) < 1e-3, "1 + 1 + 3 damage, health " + pig.getHealth());
                    long wait = EngineServer.state(item).remaining(0, helper.getLevel().getGameTime());
                    helper.assertTrue(wait > 60, "after the last blow the whole cooldown, got " + wait);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void projectile(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 9.0, 11.5);
        player.setXRot(4.0F);
        helper.assertTrue(use(player, ability("projetil", AbilityKind.SHOT, 4, 1, 20, 0, 0,
                settings("perseguicao", 0.4, "velocidade", 1.0, "elemento", "fogo"))), "the shot goes off");
        helper.assertTrue(PresetRuntime.running(player) == 1, "the shot is in flight");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(pig), "the homing shot reaches the pig off to the side"))
                .thenExecute(() -> {
                    helper.assertTrue(pig.isOnFire(), "a fire shot sets it burning");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void beam(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 3.5);
        Pig pig = pig(helper, 7.5, 9.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("feixe_continuo", AbilityKind.SHOT, 4, 1, 16, 0, 1.0, settings())), "the beam goes on");
        helper.startSequence()
                .thenExecuteAfter(12, () -> helper.assertTrue(PresetRuntime.running(player) == 1, "it stays on"))
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(hurt(pig), "the beam burns the pig");
                    helper.assertTrue(PresetRuntime.running(player) == 0, "and goes off after its duration");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void fan(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig left = pig(helper, 10.5, 8.5);
        Pig middle = pig(helper, 7.5, 8.5);
        Pig right = pig(helper, 4.5, 8.5);
        player.setXRot(6.0F);
        helper.assertTrue(use(player, ability("leque", AbilityKind.SHOT, 2, 1, 16, 0, 0, settings("quantidade", 5, "abertura", 60))), "the fan goes off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(left) && hurt(middle) && hurt(right), "the spread hits all three pigs"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void boomerang(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig first = pig(helper, 7.5, 7.5);
        Pig second = pig(helper, 11.5, 9.5);
        lookAt(player, first);
        helper.assertTrue(use(player, ability("arremesso_que_volta", AbilityKind.SHOT, 3, 1, 10, 0, 0,
                settings("quicadas", 1, "item", "minecraft:shield"))), "the throw goes off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(first) && hurt(second), "it hits the first pig and bounces to the second"))
                .thenWaitUntil(() -> helper.assertTrue(PresetRuntime.running(player) == 0, "and comes back to the hand"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void ballistic(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 10.5);
        Pig near = pig(helper, 9.0, 11.5);
        lookAt(player, pig);
        EngineAbility shot = ability("balistico", AbilityKind.SHOT, 3, 1, 0, 2.5, 0,
                settings("municao", "minecraft:arrow", "ponta", "explosiva", "gravidade", 0.01));
        helper.assertFalse(use(player, shot), "no arrows, no shot");
        player.getInventory().add(new ItemStack(Items.ARROW, 2));
        helper.assertTrue(use(player, shot), "with arrows it shoots");
        helper.assertTrue(player.getInventory().countItem(Items.ARROW) == 1, "and uses one arrow");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(pig) && hurt(near), "the explosive tip hurts the pig and the one by it"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void skyStrike(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 3.5);
        Pig pig = pig(helper, 7.5, 8.5);
        helper.assertFalse(use(player, ability("raio_do_ceu", AbilityKind.SHOT, 4, 1, 3, 2, 0, settings())),
                "with nothing in reach there is nowhere to strike");
        EngineAbility strike = ability("raio_do_ceu", AbilityKind.SHOT, 4, 1, 24, 2, 0, settings("atraso", 0.5));
        lookAt(player, pig);
        helper.assertTrue(use(player, strike), "aimed at the pig it goes off");
        helper.assertFalse(hurt(pig), "the warning comes first");
        helper.startSequence()
                .thenExecuteAfter(14, () -> {
                    helper.assertTrue(hurt(pig), "then the strike falls on the pig");
                    helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 1, 8))).isAir(), "and the lightning sets no fire");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void wave(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        Pig a = pig(helper, 10.5, 7.5);
        Pig b = pig(helper, 7.5, 4.5);
        helper.assertTrue(use(player, ability("onda_em_volta", AbilityKind.BLAST, 3, 1, 0, 5, 0, settings())), "the wave goes off");
        helper.assertTrue(hurt(a) && hurt(b), "the wave hurts the pigs around");
        helper.assertTrue(a.getDeltaMovement().x > 0.3 && b.getDeltaMovement().z < -0.3, "and pushes them away");
        remove(player);
        helper.succeed();
    }

    public static void pointBlast(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 10.5);
        Pig side = pig(helper, 9.5, 10.5);
        lookAt(player, pig);
        float health = player.getHealth();
        helper.assertTrue(use(player, ability("explosao_no_ponto", AbilityKind.BLAST, 5, 1, 24, 3, 0, settings())), "the blast goes off");
        helper.assertTrue(hurt(pig) && hurt(side), "the blast hurts both pigs");
        helper.assertTrue(player.getHealth() == health, "but never the one who made it");
        helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 0, 10))).isAir(), "without quebra_blocos the floor stays");
        remove(player);
        helper.succeed();
    }

    public static void cone(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 4.5);
        Pig pig = pig(helper, 7.5, 7.5);
        Pig outside = pig(helper, 12.5, 5.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("cone", AbilityKind.BLAST, 4, 1, 6, 0, 1.0, settings("elemento", "gelo", "angulo", 40))), "the breath goes off");
        helper.startSequence()
                .thenExecuteAfter(22, () -> {
                    helper.assertTrue(hurt(pig), "the ice breath hurts the pig in front");
                    helper.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "and slows it");
                    helper.assertFalse(hurt(outside), "not the pig off to the side");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void pulse(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        Pig near = pig(helper, 10.5, 7.5);
        Pig far = pig(helper, 1.5, 13.5);
        helper.assertTrue(use(player, ability("pulso", AbilityKind.BLAST, 0, 1, 0, 5, 3, settings())), "the pulse goes off");
        var slow = near.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(slow != null && slow.getAmplifier() >= 6 && slow.getDuration() > 40, "the pig nearby is stunned");
        helper.assertFalse(far.hasEffect(MobEffects.SLOWNESS), "the far one is not");
        remove(player);
        helper.succeed();
    }

    public static void rain(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 9.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("chuva", AbilityKind.BLAST, 1, 1, 24, 1.2, 2.0, settings("golpes_por_segundo", 10))), "the rain starts");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(pig), "the blows falling on the area hit the pig"))
                .thenWaitUntil(() -> helper.assertTrue(PresetRuntime.running(player) == 0, "and the rain stops after its duration"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }
}
