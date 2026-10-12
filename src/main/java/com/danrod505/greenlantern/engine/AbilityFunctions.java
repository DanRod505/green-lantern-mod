package com.danrod505.greenlantern.engine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Which function of an ability goes off (the new model: no more crouch + V, no fight or world mode).
 * The extra functions of the ability are tried in order and the first whose condition holds goes
 * off; when none does, the default one does. A condition looks at how the key is pressed (a tap,
 * holding it, or letting go after holding), what the crosshair is on (a creature, a kind of block,
 * any block, or nothing) and where the player is (on the ground, in the air, in water, sprinting).
 * The same code runs on the client, so the HUD shows the function before it goes off.
 */
public final class AbilityFunctions {
    /** How far the crosshair looks for the condition, when the function has no reach of its own. */
    public static final double AIM_REACH = 8.0;
    /** Ticks the key is held before it counts as holding (a shorter press is a tap). */
    public static final int HOLD_TICKS = 8;

    /** How the key is pressed. */
    public enum Press {
        TAP("toque"),
        HOLD("segurar"),
        RELEASE("soltar");

        private final String id;

        Press(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Press byIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : TAP;
        }
    }

    public static final List<String> PRESSES = List.of("toque", "segurar", "soltar");
    public static final List<String> PLACES = List.of("chao", "ar", "agua", "correndo");
    /** The kinds of aim a condition may name (or a block id, or a {@code #tag} of blocks). */
    public static final List<String> AIMS = List.of("mob", "bloco", "vazio", "minerio", "madeira", "folhas", "planta", "terra", "pedra", "agua",
            "lava", "fogo", "gelo", "fornalha", "bau", "maquina");

    /** The function that goes off: its index (0 is the default one) and the function. */
    public record Chosen(int index, CharacterData.Function function) {}

    private AbilityFunctions() {}

    /** The function of {@code ability} that goes off for this press, where the player aims and stands now. */
    public static Chosen choose(CharacterData.Ability ability, Press press, Player player) {
        List<CharacterData.Function> extra = ability.functions();
        for (int i = 0; i < extra.size(); i++) {
            CharacterData.Function function = extra.get(i);
            if (holds(ability, function, press, player)) return new Chosen(i + 1, function);
        }
        return new Chosen(0, ability.defaultFunction());
    }

    /** Whether the ability has a function for holding the key or letting it go (then a press waits to see which). */
    public static boolean usesHold(CharacterData.Ability ability) {
        return ability.functions().stream().anyMatch(f -> f.when().press().filter(p -> !p.equals("toque")).isPresent());
    }

    /** Whether the ability has a function for {@code press}. */
    public static boolean has(CharacterData.Ability ability, Press press) {
        return ability.functions().stream().anyMatch(f -> f.when().press().filter(p -> p.equals(press.id())).isPresent());
    }

    /** Whether a function's condition holds. */
    public static boolean holds(CharacterData.Ability ability, CharacterData.Function function, Press press, Player player) {
        CharacterData.When when = function.when();
        if (when.press().isPresent() && !when.press().get().equals(press.id())) return false;
        if (when.place().isPresent() && !place(when.place().get(), player)) return false;
        if (when.aim().isPresent()) {
            double reach = ability.numbers(function).reach();
            return aims(when.aim().get(), player, reach > 0 ? reach : AIM_REACH);
        }
        return true;
    }

    private static boolean place(String place, Player player) {
        return switch (place) {
            case "chao" -> player.onGround();
            case "ar" -> !player.onGround() && !player.isInWater();
            case "agua" -> player.isInWater();
            case "correndo" -> player.isSprinting();
            default -> false;
        };
    }

    /** Whether the crosshair, within {@code reach}, is on what {@code aim} names. */
    public static boolean aims(String aim, Player player, double reach) {
        Level level = player.level();
        boolean fluid = aim.equals("agua") || aim.equals("lava");
        HitResult hit = aim(player, reach, fluid);
        return switch (aim) {
            case "mob" -> hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity;
            case "vazio" -> hit.getType() == HitResult.Type.MISS;
            case "bloco" -> hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK;
            default -> hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK && block(aim, level, b.getBlockPos());
        };
    }

    /** Whether the block at {@code pos} is of the kind {@code aim} names. */
    public static boolean block(String aim, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return switch (aim) {
            case "minerio" -> state.is(Tags.Blocks.ORES);
            case "madeira" -> state.is(BlockTags.LOGS);
            case "folhas" -> state.is(BlockTags.LEAVES);
            case "planta" -> state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)
                    || state.getBlock() instanceof BonemealableBlock && !state.is(BlockTags.DIRT);
            case "terra" -> state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(net.minecraft.world.level.block.Blocks.GRAVEL) || state.is(BlockTags.SNOW)
                    || state.is(net.minecraft.world.level.block.Blocks.CLAY);
            case "pedra" -> state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.STONES)
                    || state.is(Tags.Blocks.COBBLESTONES);
            case "agua" -> state.getFluidState().is(FluidTags.WATER);
            case "lava" -> state.getFluidState().is(FluidTags.LAVA);
            case "fogo" -> state.is(BlockTags.FIRE) || state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
            case "gelo" -> state.is(BlockTags.ICE) || state.is(BlockTags.SNOW);
            case "fornalha" -> state.getBlock() instanceof AbstractFurnaceBlock;
            case "bau" -> state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Container;
            case "maquina" -> state.isSignalSource() || state.hasProperty(BlockStateProperties.POWERED);
            default -> named(aim, state);
        };
    }

    /** A block id ({@code minecraft:stone}) or a tag of blocks ({@code #minecraft:logs}). */
    private static boolean named(String aim, BlockState state) {
        if (aim.startsWith("#")) {
            Identifier id = Identifier.tryParse(aim.substring(1));
            return id != null && state.is(TagKey.create(Registries.BLOCK, id));
        }
        Identifier id = Identifier.tryParse(aim);
        return id != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id);
    }

    /** Whether a condition's aim is one the game knows (a kind, a block id or a tag). */
    public static boolean knownAim(String aim) {
        if (AIMS.contains(aim)) return true;
        if (aim.startsWith("#")) return Identifier.tryParse(aim.substring(1)) != null;
        Identifier id = Identifier.tryParse(aim);
        return id != null && BuiltInRegistries.BLOCK.containsKey(id);
    }

    /** What the crosshair is on within {@code reach}: a creature, a block (a fluid too with {@code fluids}) or nothing. */
    public static HitResult aim(Player player, double reach, boolean fluids) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != player && !e.isSpectator() && e.isPickable() && e instanceof LivingEntity, 0.3F);
        return entity != null ? entity : block;
    }
}
