package com.danrod505.greenlantern.engine.preset;

import com.mojang.math.Transformation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Display entities of the visual presets: an item that flies (a piece of armor, a weapon) and a
 * translucent block of energy (a construct). They belong to the player like a summon, so one left
 * over after a restart is dropped, and they move smoothly ({@code teleport_duration}).
 */
public final class Displays {
    private Displays() {}

    /** An item floating at {@code at}, {@code scale} times its size, always facing the camera when {@code facing}. */
    public static Display.ItemDisplay item(ServerPlayer owner, ItemStack stack, Vec3 at, float scale, boolean facing) {
        ServerLevel level = owner.level();
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.store("item", ItemStack.CODEC, RegistryOps.create(NbtOps.INSTANCE, level.registryAccess()), stack);
        tag.putInt("teleport_duration", 2);
        if (facing) tag.store("billboard", Display.BillboardConstraints.CODEC, Display.BillboardConstraints.CENTER);
        tag.store("transformation", Transformation.EXTENDED_CODEC, scaled(scale, scale, scale));
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        display.setPos(at);
        display.setNoGravity(true);
        return Summons.spawn(owner, display);
    }

    /**
     * A block of energy: {@code state} (a stained glass, so it is see-through) stretched to
     * {@code size} with its corner at {@code corner}, glowing in {@code glow} when it is not -1.
     */
    public static Display.BlockDisplay block(ServerPlayer owner, BlockState state, Vec3 corner, Vector3f size, int glow) {
        ServerLevel level = owner.level();
        Display.BlockDisplay display = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        tag.putInt("teleport_duration", 2);
        tag.store("transformation", Transformation.EXTENDED_CODEC, scaled(size.x, size.y, size.z));
        tag.store("brightness", Brightness.CODEC, Brightness.FULL_BRIGHT);
        if (glow >= 0) tag.putInt("glow_color_override", glow);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        display.setPos(corner);
        display.setNoGravity(true);
        if (glow >= 0) display.setGlowingTag(true);
        return Summons.spawn(owner, display);
    }

    private static Transformation scaled(float x, float y, float z) {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(x, y, z), new Quaternionf());
    }
}
