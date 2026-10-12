package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;
import static com.danrod505.greenlantern.gametest.heroes.PresetTests.*;

import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.Guards;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The ability presets of stage 2, batch 2 (Movimento, Defesa, Evasão), on the test character
 * Protótipo with abilities made on the spot (the flight on the Protótipo Alado, whose file names it).
 * A test player is moved by the server only when the preset moves it itself (a dash, a teleport);
 * for the pushes the tests check the motion the client would get.
 */
public final class PresetLot2Tests {
    private PresetLot2Tests() {}

    static final List<String> BATCH_2 = List.of(
            "voo", "supervelocidade", "planar", "salto_gigante", "gancho", "teleporte", "nado_rapido", "atravessar",
            "bolha", "bloqueio", "pele_dura", "parede", "refletir",
            "esquiva", "invisivel", "fumaca", "encolher", "fuga_de_forma");

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("preset_batch2_registered", () -> PresetLot2Tests::registered);
        tests.register("preset_voo", () -> PresetLot2Tests::flight);
        tests.register("preset_supervelocidade", () -> PresetLot2Tests::superSpeed);
        tests.register("preset_planar", () -> PresetLot2Tests::glide);
        tests.register("preset_salto_gigante", () -> PresetLot2Tests::giantLeap);
        tests.register("preset_gancho", () -> PresetLot2Tests::grapple);
        tests.register("preset_teleporte", () -> PresetLot2Tests::teleport);
        tests.register("preset_nado_rapido", () -> PresetLot2Tests::fastSwim);
        tests.register("preset_atravessar", () -> PresetLot2Tests::phase);
        tests.register("preset_bolha", () -> PresetLot2Tests::bubble);
        tests.register("preset_bloqueio", () -> PresetLot2Tests::block);
        tests.register("preset_pele_dura", () -> PresetLot2Tests::hardSkin);
        tests.register("preset_parede", () -> PresetLot2Tests::wall);
        tests.register("preset_refletir", () -> PresetLot2Tests::reflect);
        tests.register("preset_esquiva", () -> PresetLot2Tests::dodge);
        tests.register("preset_invisivel", () -> PresetLot2Tests::invisible);
        tests.register("preset_fumaca", () -> PresetLot2Tests::smoke);
        tests.register("preset_encolher", () -> PresetLot2Tests::shrink);
        tests.register("preset_fuga_de_forma", () -> PresetLot2Tests::shapeEscape);
    }

    private static boolean cooling(GameTestHelper helper, ServerPlayer player) {
        return EngineServer.state(player.getMainHandItem()).remaining(0, helper.getLevel().getGameTime()) > 1;
    }

    private static double attribute(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        return player.getAttributeValue(attribute);
    }

    /** A test player whose client "loaded": only then can it be hurt. */
    private static ServerPlayer loaded(ServerPlayer player) {
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }

    /** The health a hit of magic damage (no armor) takes away. */
    private static float magicLoss(GameTestHelper helper, ServerPlayer player, float amount) {
        player.invulnerableTime = 0;
        float before = player.getHealth();
        player.hurtServer(helper.getLevel(), player.damageSources().magic(), amount);
        return before - player.getHealth();
    }

    // ---- tests ------------------------------------------------------------------------------------

    /** The 18 presets of batch 2 are there for the character files to name. */
    public static void registered(GameTestHelper helper) {
        for (String id : BATCH_2) helper.assertTrue(Presets.exists(id), "missing preset " + id);
        helper.succeed();
    }

    /** A character whose file has a "voo" ability power-flies; its key takes off and lands. */
    public static void flight(GameTestHelper helper) {
        EngineHero winged = (EngineHero) HeroRegistry.byId("prototipo_alado").orElseThrow();
        helper.assertTrue(winged.flightProfile() != null && Math.abs(winged.flightProfile().max() - 3.0) < 1e-6,
                "the winged one flies at the top speed of its file, got " + winged.flightProfile());
        helper.assertTrue(prototype().flightProfile() == null, "a character without a flight ability doesn't fly");
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(winged));
        helper.assertTrue(winged.summonSuit(player), "the suit goes on");
        EngineAbility fly = (EngineAbility) winged.powers().power(4);
        helper.assertTrue(fly.data().preset().equals("voo"), "the 5th ability is the take-off");
        helper.assertTrue(EngineServer.useAbility(player, player.getMainHandItem(), fly), "the key takes off");
        helper.assertTrue(player.getAbilities().flying, "the player flies");
        helper.assertTrue(player.getDeltaMovement().y > 0.5, "with a jump up, got " + player.getDeltaMovement());
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(EngineServer.useAbility(player, player.getMainHandItem(), fly), "the key again lands");
                    helper.assertFalse(player.getAbilities().flying, "the player no longer flies");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void superSpeed(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        double speed = attribute(player, Attributes.MOVEMENT_SPEED);
        double step = attribute(player, Attributes.STEP_HEIGHT);
        EngineAbility run = ability("supervelocidade", AbilityKind.MOBILITY, 0, 4, 0, 0, 10, settings("velocidade", 2.0));
        helper.assertTrue(use(player, run), "the run starts");
        helper.assertTrue(attribute(player, Attributes.MOVEMENT_SPEED) > speed * 2.9, "three times as fast, got " + attribute(player, Attributes.MOVEMENT_SPEED));
        helper.assertTrue(attribute(player, Attributes.STEP_HEIGHT) >= step + 0.5, "and it steps up whole blocks");
        helper.assertFalse(cooling(helper, player), "while it runs there is no cooldown yet");
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(use(player, run), "the key again stops it");
                    helper.assertTrue(Math.abs(attribute(player, Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "back to the normal speed");
                    helper.assertTrue(cooling(helper, player), "and the cooldown starts when it ends");
                    // The same run ends with the suit.
                    player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(prototype()));
                    helper.assertTrue(use(player, run), "it starts again");
                    helper.assertTrue(PresetRuntime.running(player, "teste_supervelocidade"), "it runs");
                    prototype().dismissSuit(player, false);
                    helper.assertFalse(PresetRuntime.running(player, "teste_supervelocidade"), "taking the suit off ends it");
                    helper.assertTrue(attribute(player, Attributes.MOVEMENT_SPEED) < speed * 1.5, "and its speed");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void glide(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        player.setOnGround(false);
        helper.assertTrue(use(player, ability("planar", AbilityKind.MOBILITY, 0, 4, 0, 0, 10, settings("queda_por_segundo", 2.0))), "the glide opens");
        helper.assertTrue(attribute(player, Attributes.GRAVITY) < 0.01, "the fall is slow, gravity " + attribute(player, Attributes.GRAVITY));
        helper.startSequence()
                .thenExecuteAfter(5, () -> player.setOnGround(true))
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(Math.abs(attribute(player, Attributes.GRAVITY) - 0.08) < 1e-6, "landing ends it");
                    helper.assertTrue(cooling(helper, player), "and starts the cooldown");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void giantLeap(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        player.setOnGround(true);
        Pig near = pig(helper, 9.5, 7.5);
        Pig far = pig(helper, 1.5, 1.5);
        helper.assertTrue(use(player, ability("salto_gigante", AbilityKind.MOBILITY, 3, 1, 0, 3, 0, settings("altura", 8, "distancia", 10))),
                "the leap goes off");
        Vec3 jump = player.getDeltaMovement();
        helper.assertTrue(jump.y > 1.0 && jump.z > 0.5, "high and forward, got " + jump);
        helper.assertFalse(hurt(near), "nothing is hurt on the way up");
        player.setOnGround(false);
        helper.startSequence()
                .thenExecuteAfter(8, () -> player.setOnGround(true))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(hurt(near), "the landing hurts the pig nearby");
                    helper.assertFalse(hurt(far), "but not the far one");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void grapple(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        helper.assertFalse(use(player, ability("gancho", AbilityKind.MOBILITY, 0, 1, 3, 0, 0, settings())), "with no block in reach it fails");
        helper.setBlock(new BlockPos(7, 2, 11), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 3, 11), Blocks.STONE);
        player.setXRot(-2.0F);
        double start = player.getZ();
        helper.assertTrue(use(player, ability("gancho", AbilityKind.MOBILITY, 0, 1, 24, 0, 0, settings("velocidade", 1.5))), "the line goes to the block");
        helper.startSequence()
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(player.getZ() - start > 5.0, "it pulls the player to the block, moved " + (player.getZ() - start));
                    helper.assertTrue(player.getZ() < helper.absoluteVec(new Vec3(0, 0, 11)).z, "and stops at its face");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void teleport(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 3.5, 3.5);
        // Down at the floor 6 blocks ahead.
        player.setXRot((float) Math.toDegrees(Math.atan2(1.62, 6.0)));
        helper.assertTrue(use(player, ability("teleporte", AbilityKind.MOBILITY, 0, 1, 16, 0, 0, settings("visual", "tubo", "tempo", 0.25))),
                "the teleport opens");
        Vec3 target = helper.absoluteVec(new Vec3(3.5, 1, 9.5));
        helper.startSequence()
                .thenExecuteAfter(8, () -> {
                    helper.assertTrue(player.position().distanceTo(target) < 1.2, "the player appears where they aimed, at " + player.position()
                            + " for " + target);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void fastSwim(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.assertTrue(use(player, ability("nado_rapido", AbilityKind.MOBILITY, 0, 1, 0, 0, 2, settings())), "the swim goes on");
        helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE) && player.hasEffect(MobEffects.WATER_BREATHING), "fast swimming and breath");
        helper.assertTrue(attribute(player, Attributes.WATER_MOVEMENT_EFFICIENCY) > 0.4, "it moves well in water");
        helper.startSequence()
                .thenExecuteAfter(45, () -> {
                    helper.assertTrue(attribute(player, Attributes.WATER_MOVEMENT_EFFICIENCY) < 1e-6, "and goes back after its duration");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void phase(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 4.5);
        for (int z = 6; z <= 7; z++) {
            for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
        }
        helper.assertFalse(use(player, ability("atravessar", AbilityKind.MOBILITY, 0, 1, 0, 0, 0, settings("espessura", 1))),
                "a wall 2 blocks thick is too much for 1");
        helper.assertTrue(use(player, ability("atravessar", AbilityKind.MOBILITY, 0, 1, 0, 0, 0.3, settings("espessura", 3))), "it passes");
        helper.assertTrue(Guards.of(player).stream().anyMatch(g -> g.reduction() >= 1.0), "untouchable while crossing");
        Vec3 behind = helper.absoluteVec(new Vec3(7.5, 1, 8.5));
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(player.position().distanceTo(behind) < 0.3, "the player is behind the wall, at " + player.position());
                    helper.assertTrue(Guards.of(player).isEmpty(), "and touchable again");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void bubble(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        EngineAbility bubble = ability("bolha", AbilityKind.DEFENSE, 0, 4, 0, 2, 10, settings("reducao", 0.6));
        helper.assertTrue(use(player, bubble), "the bubble goes up");
        float loss = magicLoss(helper, player, 10.0F);
        helper.assertTrue(Math.abs(loss - 4.0F) < 0.01F, "it takes 60% of a hit away, lost " + loss);
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(use(player, bubble), "the key again takes it down");
                    helper.assertTrue(Guards.of(player).isEmpty(), "no guard left");
                    helper.assertTrue(cooling(helper, player), "and the cooldown starts");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void block(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        Pig front = pig(helper, 7.5, 9.5);
        Pig back = pig(helper, 7.5, 5.5);
        helper.assertTrue(use(player, ability("bloqueio", AbilityKind.DEFENSE, 0, 1, 0, 0, 2, settings("reducao", 0.8, "angulo", 120))), "the guard goes up");
        player.invulnerableTime = 0;
        float before = player.getHealth();
        player.hurtServer(helper.getLevel(), player.damageSources().mobAttack(front), 6.0F);
        float fromFront = before - player.getHealth();
        player.invulnerableTime = 0;
        before = player.getHealth();
        player.hurtServer(helper.getLevel(), player.damageSources().mobAttack(back), 6.0F);
        float fromBack = before - player.getHealth();
        helper.assertTrue(fromBack > 0 && fromFront < fromBack * 0.4F, "the front hit is blocked (" + fromFront + "), the back one isn't (" + fromBack + ")");
        remove(player);
        helper.succeed();
    }

    public static void hardSkin(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        helper.assertTrue(use(player, ability("pele_dura", AbilityKind.DEFENSE, 0, 1, 0, 0, 1, settings("reducao", 0.4))), "the skin hardens");
        float loss = magicLoss(helper, player, 10.0F);
        helper.assertTrue(Math.abs(loss - 6.0F) < 0.01F, "40% less damage, lost " + loss);
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(Guards.of(player).isEmpty(), "it wears off after its duration");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void wall(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 3.5);
        helper.assertTrue(use(player, ability("parede", AbilityKind.DEFENSE, 0, 1, 0, 0, 1, settings("material", "pedra", "largura", 3, "altura", 2))),
                "the wall goes up");
        helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(7, 1, 6));
        helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(6, 2, 6));
        helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(8, 1, 6));
        helper.assertBlockNotPresent(Blocks.COBBLESTONE, new BlockPos(7, 3, 6));
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(7, 1, 6));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 6));
                    remove(player);
                })
                .thenSucceed();
    }

    public static void reflect(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 3.5));
        Pig shooter = pig(helper, 7.5, 12.5);
        helper.assertTrue(use(player, ability("refletir", AbilityKind.DEFENSE, 0, 1, 0, 0, 5, settings())), "the reflection goes up");
        Vec3 from = helper.absoluteVec(new Vec3(7.5, 2.4, 10.5));
        Arrow arrow = new Arrow(helper.getLevel(), from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setOwner(shooter);
        arrow.shoot(0, 0, -1, 1.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float health = player.getHealth();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(arrow.getOwner() == player, "the arrow comes back as the player's"))
                .thenExecute(() -> {
                    helper.assertTrue(player.getHealth() == health, "and doesn't hurt them");
                    helper.assertTrue(arrow.getDeltaMovement().z > 0 || arrow.isRemoved() || hurt(shooter), "it flies back to the shooter");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void dodge(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        double x = player.getX();
        helper.assertTrue(use(player, ability("esquiva", AbilityKind.EVASION, 0, 1, 4, 0, 0, settings("lado", "direita", "janela", 0.5))), "the dodge goes off");
        helper.assertTrue(magicLoss(helper, player, 5.0F) == 0.0F, "untouchable for an instant");
        helper.startSequence()
                .thenExecuteAfter(14, () -> {
                    helper.assertTrue(x - player.getX() > 3.0, "a step to the right (west, looking south), moved " + (x - player.getX()));
                    helper.assertTrue(magicLoss(helper, player, 2.0F) > 0.0F, "then touchable again");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void invisible(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(7.5, 1, 11.5));
        zombie.setTarget(player);
        EngineAbility vanish = ability("invisivel", AbilityKind.EVASION, 0, 4, 0, 0, 10, settings());
        helper.assertTrue(use(player, vanish), "the player vanishes");
        helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY), "unseen");
        helper.assertTrue(zombie.getTarget() == null, "the zombie loses them");
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == null, "and can't pick them again");
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    zombie.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1.0F);
                    helper.assertFalse(player.hasEffect(MobEffects.INVISIBILITY), "attacking breaks it");
                    helper.assertFalse(Guards.hidden(player), "creatures find them again");
                    helper.assertTrue(cooling(helper, player), "and the cooldown starts");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void smoke(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(10.5, 1, 7.5));
        zombie.setTarget(player);
        helper.assertTrue(use(player, ability("fumaca", AbilityKind.EVASION, 0, 1, 0, 4, 2, settings())), "the smoke bomb goes off");
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(zombie.hasEffect(MobEffects.BLINDNESS), "the zombie in the smoke is blinded");
                    helper.assertTrue(zombie.getTarget() == null, "and loses the player");
                    helper.assertTrue(Guards.hidden(player), "who is hidden while in it");
                })
                .thenExecuteAfter(45, () -> {
                    helper.assertFalse(Guards.hidden(player), "the smoke clears after its duration");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void shrink(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        EngineAbility shrink = ability("encolher", AbilityKind.EVASION, 0, 4, 0, 0, 30, settings("tamanho", 0.25));
        helper.assertTrue(use(player, shrink), "the player shrinks");
        helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 0.25) < 1e-6, "to a quarter, got " + attribute(player, Attributes.SCALE));
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(use(player, shrink), "the key brings them back");
                    helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 1.0) < 1e-6, "full size");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void shapeEscape(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        double speed = attribute(player, Attributes.MOVEMENT_SPEED);
        helper.assertTrue(use(player, ability("fuga_de_forma", AbilityKind.EVASION, 0, 1, 0, 0, 1, settings("forma", "animal"))), "the shape changes");
        helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 0.5) < 1e-6, "a small animal");
        helper.assertTrue(attribute(player, Attributes.MOVEMENT_SPEED) > speed * 1.5, "and quick");
        helper.assertTrue(Guards.hidden(player), "lost by the creatures");
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 1.0) < 1e-6, "back to the character after its duration");
                    helper.assertTrue(Math.abs(attribute(player, Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "at the normal speed");
                    helper.assertFalse(Guards.hidden(player), "and found again");
                    remove(player);
                })
                .thenSucceed();
    }
}
