package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;
import static com.danrod505.greenlantern.gametest.heroes.PresetTests.*;

import com.danrod505.greenlantern.engine.AbilityKind;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.ControlPresets;
import com.danrod505.greenlantern.engine.preset.EmpowerPresets;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.engine.preset.Summons;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The ability presets of stage 2, batch 3 (Controle, Invocação, Fortalecimento, Suprema), on the
 * test character Protótipo with abilities made on the spot. The monsters the summons go for are
 * zombies without a mind of their own, so they stay where the test puts them.
 */
public final class PresetLot3Tests {
    private PresetLot3Tests() {}

    static final List<String> BATCH_3 = List.of(
            "puxar", "prender", "lentidao", "medo_e_confusao", "dominar", "levitar",
            "ajudante", "construto", "veiculo", "enxame", "copia", "torre",
            "aura", "cura", "crescer", "furia", "sentidos",
            "suprema_de_combate", "grande_obra");

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("preset_batch3_registered", () -> PresetLot3Tests::registered);
        tests.register("preset_puxar", () -> PresetLot3Tests::pull);
        tests.register("preset_prender", () -> PresetLot3Tests::hold);
        tests.register("preset_lentidao", () -> PresetLot3Tests::slow);
        tests.register("preset_medo_e_confusao", () -> PresetLot3Tests::confusion);
        tests.register("preset_dominar", () -> PresetLot3Tests::dominate);
        tests.register("preset_levitar", () -> PresetLot3Tests::levitate);
        tests.register("preset_ajudante", () -> PresetLot3Tests::helper);
        tests.register("preset_construto", () -> PresetLot3Tests::construct);
        tests.register("preset_veiculo", () -> PresetLot3Tests::vehicle);
        tests.register("preset_enxame", () -> PresetLot3Tests::swarm);
        tests.register("preset_copia", () -> PresetLot3Tests::copies);
        tests.register("preset_torre", () -> PresetLot3Tests::turret);
        tests.register("preset_aura", () -> PresetLot3Tests::aura);
        tests.register("preset_cura", () -> PresetLot3Tests::heal);
        tests.register("preset_crescer", () -> PresetLot3Tests::grow);
        tests.register("preset_furia", () -> PresetLot3Tests::fury);
        tests.register("preset_sentidos", () -> PresetLot3Tests::senses);
        tests.register("preset_suprema_de_combate", () -> PresetLot3Tests::greatBlow);
        tests.register("preset_grande_obra", () -> PresetLot3Tests::greatWork);
    }

    private static double attribute(ServerPlayer player, Holder<Attribute> attribute) {
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

    /** A zombie that stays put and does not burn in the sun, so only the presets hurt it. */
    private static Zombie target(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = dummy(helper, x, y, z);
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
        return zombie;
    }

    private static boolean hurt(Zombie zombie) {
        return zombie.getHealth() < zombie.getMaxHealth() || zombie.isDeadOrDying();
    }

    /** The summons of the player around them. */
    private static <T extends Entity> List<T> summons(ServerPlayer player, Class<T> type) {
        return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(24.0), e -> player.getUUID().equals(Summons.owner(e)));
    }

    private static int count(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(13, 7, 13)))) {
            if (helper.getLevel().getBlockState(pos).is(block)) n++;
        }
        return n;
    }

    // ---- tests ------------------------------------------------------------------------------------

    /** The 19 presets of batch 3 are there for the character files to name. */
    public static void registered(GameTestHelper helper) {
        for (String id : BATCH_3) helper.assertTrue(Presets.exists(id), "missing preset " + id);
        helper.succeed();
    }

    public static void pull(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 9.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("puxar", AbilityKind.CONTROL, 0, 1, 12, 0, 0, settings())), "the lasso goes off");
        helper.assertTrue(pig.getDeltaMovement().z < -0.3, "and pulls the pig to the player, got " + pig.getDeltaMovement());
        // With nothing aimed at it gives the energy back.
        player.setYRot(180.0F);
        player.setYHeadRot(180.0F);
        player.setXRot(0.0F);
        helper.assertFalse(use(player, ability("puxar", AbilityKind.CONTROL, 0, 1, 12, 0, 0, settings())), "nothing to pull: it does not go off");
        remove(player);
        helper.succeed();
    }

    public static void hold(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie zombie = target(helper, 7.5, 1, 7.5);
        lookAt(player, zombie);
        Vec3 anchor = zombie.position();
        helper.assertTrue(use(player, ability("prender", AbilityKind.CONTROL, 0, 1, 12, 0, 4, settings("material", "gelo", "vida", 6))), "the prison goes off");
        helper.assertTrue(ControlPresets.held(zombie), "it holds the zombie");
        zombie.setPos(anchor.x + 2.0, anchor.y, anchor.z);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(zombie.position().distanceTo(anchor) < 0.1, "the zombie stays where it was caught, got " + zombie.position());
                    zombie.invulnerableTime = 0;
                    zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().magic(), 7.0F);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertFalse(ControlPresets.held(zombie), "enough damage breaks the prison");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void slow(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 9.5);
        lookAt(player, pig);
        helper.assertTrue(use(player, ability("lentidao", AbilityKind.CONTROL, 0, 1, 16, 0, 2, settings("forca", 3, "elemento", "gelo"))), "the slow goes off");
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "the pig is slowed");
                    helper.assertTrue(pig.getEffect(MobEffects.SLOWNESS).getAmplifier() >= 2, "more and more, got " + pig.getEffect(MobEffects.SLOWNESS));
                    helper.assertTrue(pig.getTicksFrozen() > 0, "the ice freezes it");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void confusion(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie one = helper.spawn(EntityType.ZOMBIE, new Vec3(5.5, 1, 6.5));
        Zombie two = helper.spawn(EntityType.ZOMBIE, new Vec3(9.5, 1, 6.5));
        helper.assertTrue(use(player, ability("medo_e_confusao", AbilityKind.CONTROL, 0, 1, 0, 8, 3, settings("tipo", "confusao"))), "the confusion goes off");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(one.getTarget() == two && two.getTarget() == one, "the zombies go for each other, got " + one.getTarget() + " / " + two.getTarget());
                    remove(player);
                })
                .thenSucceed();
    }

    public static void dominate(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(7.5, 1, 5.5));
        Zombie foe = target(helper, 7.5, 1, 11.5);
        lookAt(player, zombie);
        helper.assertTrue(use(player, ability("dominar", AbilityKind.CONTROL, 0, 1, 12, 0, 3, settings())), "the domination goes off");
        helper.assertTrue(player.getUUID().equals(Summons.owner(zombie)), "the zombie is the player's");
        helper.startSequence()
                .thenExecuteAfter(3, () -> helper.assertTrue(zombie.getTarget() == foe, "it goes for the other zombie, got " + zombie.getTarget()))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(Summons.owner(zombie) == null, "after the time it is let go");
                    helper.assertTrue(zombie.isAlive(), "and stays in the world");
                    zombie.discard();
                    // A creature too strong is not dominated.
                    IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new Vec3(7.5, 1, 6.5));
                    golem.setNoAi(true);
                    lookAt(player, golem);
                    helper.assertFalse(use(player, ability("dominar", AbilityKind.CONTROL, 0, 1, 12, 0, 3, settings())), "the golem is too strong");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void levitate(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Pig pig = pig(helper, 7.5, 7.5);
        lookAt(player, pig);
        double start = pig.getY();
        helper.assertTrue(use(player, ability("levitar", AbilityKind.CONTROL, 0, 1, 16, 0, 1.5, settings("altura", 3))), "the lift goes off");
        helper.startSequence()
                .thenExecuteAfter(20, () -> helper.assertTrue(pig.getY() > start + 2.5, "the pig floats up, got " + (pig.getY() - start)))
                .thenExecuteAfter(15, () -> {
                    helper.assertFalse(pig.isNoGravity(), "then it is let go");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void helper(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie foe = target(helper, 7.5, 1, 9.5);
        helper.assertTrue(use(player, ability("ajudante", AbilityKind.SUMMON, 6, 1, 0, 0, 4, settings("quantidade", 2))), "the call goes off");
        List<net.minecraft.world.entity.animal.wolf.Wolf> wolves = summons(player, net.minecraft.world.entity.animal.wolf.Wolf.class);
        helper.assertTrue(wolves.size() == 2, "two wolves come, got " + wolves.size());
        helper.assertTrue(wolves.getFirst().isTame() && wolves.getFirst().isOwnedBy(player), "tamed by the player");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(foe), "the wolves go for the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(wolves.stream().allMatch(Entity::isRemoved), "and go away at the end"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void construct(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie foe = target(helper, 7.5, 1, 7.5);
        helper.assertTrue(use(player, ability("construto", AbilityKind.SUMMON, 4, 1, 10, 0, 3, settings("forma", "martelo", "intervalo", 0.5))),
                "the construct goes off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(foe), "the hammer falls on the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(PresetRuntime.running(player) == 0, "and goes away at the end"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void vehicle(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 4.5);
        helper.assertTrue(use(player, ability("veiculo", AbilityKind.SUMMON, 0, 1, 0, 0, 1.5, settings())), "the mount comes");
        Entity ride = player.getVehicle();
        helper.assertTrue(ride instanceof net.minecraft.world.entity.animal.equine.Horse, "the player rides a horse, got " + ride);
        helper.assertTrue(Summons.summoned(ride), "a summoned one");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ride.isRemoved() && player.getVehicle() == null, "it goes away at the end"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void swarm(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie foe = target(helper, 7.5, 1, 9.5);
        helper.assertTrue(use(player, ability("enxame", AbilityKind.SUMMON, 1, 1, 12, 0, 4, settings("quantidade", 4))), "the swarm goes off");
        List<net.minecraft.world.entity.ambient.Bat> bats = summons(player, net.minecraft.world.entity.ambient.Bat.class);
        helper.assertTrue(bats.size() == 4, "four bats, got " + bats.size());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(foe), "the bats bite the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(bats.stream().allMatch(Entity::isRemoved), "and go away at the end"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void copies(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(7.5, 1, 10.5));
        zombie.setTarget(player);
        helper.assertTrue(use(player, ability("copia", AbilityKind.SUMMON, 1, 1, 0, 0, 2, settings("quantidade", 2))), "the copies come");
        List<Mannequin> copies = summons(player, Mannequin.class);
        helper.assertTrue(copies.size() == 2, "two copies, got " + copies.size());
        var profile = copies.getFirst().get(net.minecraft.core.component.DataComponents.PROFILE);
        helper.assertTrue(profile != null && profile.skinPatch().body().isPresent()
                && profile.skinPatch().body().get().id().getPath().equals("entity/skin/prototipo"), "with the character's skin, got " + profile);
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(zombie.getTarget() instanceof Mannequin, "the zombie goes for a copy, got " + zombie.getTarget()))
                .thenWaitUntil(() -> helper.assertTrue(copies.stream().allMatch(Entity::isRemoved), "and they go away at the end"))
                .thenExecute(() -> {
                    zombie.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    public static void turret(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 2.5);
        Zombie foe = target(helper, 7.5, 1, 11.5);
        helper.assertTrue(use(player, ability("torre", AbilityKind.SUMMON, 3, 1, 16, 0, 3, settings("cadencia", 0.5))), "the turret goes up");
        List<ArmorStand> towers = summons(player, ArmorStand.class);
        helper.assertTrue(towers.size() == 1, "one turret, got " + towers.size());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hurt(foe), "it shoots the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(towers.getFirst().isRemoved(), "and goes away at the end"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void aura(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        double damage = attribute(player, Attributes.ATTACK_DAMAGE);
        double speed = attribute(player, Attributes.MOVEMENT_SPEED);
        EngineAbility aura = ability("aura", AbilityKind.EMPOWER, 0, 1, 0, 0, 10, settings("atributo", "todos", "quanto", 0.5));
        helper.assertTrue(use(player, aura), "the aura goes on");
        helper.assertTrue(Math.abs(attribute(player, Attributes.ATTACK_DAMAGE) - damage * 1.5) < 1e-6, "+50% damage, got " + attribute(player, Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(attribute(player, Attributes.MOVEMENT_SPEED) - speed * 1.5) < 1e-6, "+50% speed");
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(use(player, aura), "the key again turns it off");
                    helper.assertTrue(Math.abs(attribute(player, Attributes.ATTACK_DAMAGE) - damage) < 1e-6, "back to the normal damage");
                    helper.assertTrue(Math.abs(attribute(player, Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "and speed");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void heal(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        EngineAbility heal = ability("cura", AbilityKind.EMPOWER, 0, 0.05, 0, 0, 0, settings("quanto", 6));
        player.setHealth(player.getMaxHealth());
        helper.assertFalse(use(player, heal), "with full health it does not go off");
        magicLoss(helper, player, 10.0F);
        float before = player.getHealth();
        helper.assertTrue(use(player, heal), "the healing goes off");
        helper.assertTrue(Math.abs(player.getHealth() - before - 6.0F) < 0.01F, "it gives back 6, got " + (player.getHealth() - before));
        remove(player);
        helper.succeed();
    }

    public static void grow(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        double health = player.getMaxHealth();
        double damage = attribute(player, Attributes.ATTACK_DAMAGE);
        helper.assertTrue(use(player, ability("crescer", AbilityKind.EMPOWER, 0, 1, 0, 0, 1, settings("tamanho", 1.8, "dano_extra", 4, "vida_extra", 10))),
                "the growth goes on");
        helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 1.8) < 1e-6, "bigger, got " + attribute(player, Attributes.SCALE));
        helper.assertTrue(Math.abs(player.getMaxHealth() - health - 10) < 1e-6, "with more health");
        helper.assertTrue(Math.abs(attribute(player, Attributes.ATTACK_DAMAGE) - damage - 4) < 1e-6, "and more damage");
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - 1.0) < 1e-6, "back to the normal size at the end");
                    helper.assertTrue(Math.abs(player.getMaxHealth() - health) < 1e-6 && player.getHealth() <= player.getMaxHealth(), "and health");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void fury(GameTestHelper helper) {
        ServerPlayer player = loaded(suited(helper, 7.5, 7.5));
        double damage = attribute(player, Attributes.ATTACK_DAMAGE);
        helper.assertTrue(use(player, ability("furia", AbilityKind.EMPOWER, 0, 1, 0, 0, 8, settings("troca", 0.5))), "the fury goes on");
        helper.assertTrue(Math.abs(attribute(player, Attributes.ATTACK_DAMAGE) - damage * 1.5) < 1e-6, "+50% damage");
        float loss = magicLoss(helper, player, 4.0F);
        helper.assertTrue(Math.abs(loss - 6.0F) < 0.01F, "and +50% damage taken, lost " + loss);
        remove(player);
        helper.succeed();
    }

    public static void senses(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.DIAMOND_ORE);
        helper.setBlock(new BlockPos(11, 2, 11), Blocks.IRON_ORE);
        Pig pig = pig(helper, 10.5, 4.5);
        helper.assertTrue(use(player, ability("sentidos", AbilityKind.EMPOWER, 0, 1, 16, 0, 4, settings("mostra", "tudo"))), "the senses go on");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(EmpowerPresets.shownOres(helper.getLevel(), player) == 2, "the two ores show, got " + EmpowerPresets.shownOres(helper.getLevel(), player));
                    helper.assertTrue(pig.hasEffect(MobEffects.GLOWING), "the pig glows");
                    helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "the player sees in the dark");
                    helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
                })
                .thenExecuteAfter(41, () -> helper.assertTrue(EmpowerPresets.shownOres(helper.getLevel(), player) == 1, "the mined ore is no longer shown"))
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(EmpowerPresets.shownOres(helper.getLevel(), player) == 0, "and none at the end");
                    helper.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "nor the night vision");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void greatBlow(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 7.5);
        Zombie near = target(helper, 7.5, 1, 10.5);
        Zombie far = target(helper, 7.5, 1, 13.5);
        EngineAbility supreme = supreme(ability("suprema_de_combate", AbilityKind.BLAST, 12, 30, 0, 4, 0, settings("carga", 0.5, "visual", "raio")));
        helper.assertTrue(use(player, supreme), "the supreme ability goes off, with no bar to fill");
        helper.assertFalse(use(player, supreme), "then waits for its long cooldown");
        helper.startSequence()
                .thenExecuteAfter(8, () -> helper.assertFalse(hurt(near), "while it charges nothing happens yet"))
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(hurt(near), "then it hits whoever is within the radius");
                    helper.assertFalse(hurt(far), "and not beyond it, got " + far.getLastDamageSource() + " " + far.getHealth() + " at " + far.position());
                    remove(player);
                })
                .thenSucceed();
    }

    public static void greatWork(GameTestHelper helper) {
        ServerPlayer player = suited(helper, 7.5, 1.5);
        EngineAbility work = supreme(ability("grande_obra", AbilityKind.BUILDING, 0, 1, 0, 0, 0, settings("tempo", 1, "distancia", 2)));
        helper.assertTrue(use(player, work), "the work starts");
        helper.assertTrue(PresetRuntime.undoable(player, "teste_grande_obra"), "and can be taken down");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    int partial = count(helper, Blocks.SNOW_BLOCK);
                    helper.assertTrue(partial > 0, "it goes up piece by piece");
                })
                .thenExecuteAfter(25, () -> {
                    int snow = count(helper, Blocks.SNOW_BLOCK);
                    helper.assertTrue(snow > 20, "the igloo is up, got " + snow + " snow blocks");
                    helper.assertTrue(use(player, work), "the key again takes it down, with no cooldown");
                    helper.assertTrue(count(helper, Blocks.SNOW_BLOCK) == 0, "all of it");
                    helper.assertFalse(PresetRuntime.undoable(player, "teste_grande_obra"), "once");
                    remove(player);
                })
                .thenSucceed();
    }

}
