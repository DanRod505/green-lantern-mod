package com.danrod505.greenlantern.block;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModBlockEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Handles a charging session: while the player that started it stays close to the lantern with
 * the ring in hand, energy flows from the lantern into the ring.
 */
public class PowerBatteryBlockEntity extends BlockEntity {
    public static final int OATH_LINES = 4;
    private static final double MAX_DISTANCE_SQR = 5.0 * 5.0;

    private @Nullable UUID chargingPlayer;
    private int chargeTicks;

    public PowerBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_BATTERY.get(), pos, state);
    }

    public void toggleCharging(ServerPlayer player) {
        if (player.getUUID().equals(chargingPlayer)) {
            stopCharging(player, false);
            return;
        }
        ItemStack ring = RingHelper.heldRing(player);
        if (RingEnergy.get(ring).stored() >= RingEnergy.configuredCapacity()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.already_full").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        chargingPlayer = player.getUUID();
        chargeTicks = 0;
        setChargingState(true);
        level.playSound(null, worldPosition, ModSounds.CHARGE_LOOP.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        if (GLConfig.SHOW_OATH.get()) {
            sendOathLine(player, 0);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerBatteryBlockEntity battery) {
        battery.tickCharging((ServerLevel) level);
    }

    private void tickCharging(ServerLevel level) {
        if (chargingPlayer == null) {
            if (PowerBatteryBlock.isCharging(getBlockState())) setChargingState(false);
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(chargingPlayer);
        Vec3 center = Vec3.atCenterOf(worldPosition);
        ItemStack ring = player == null ? ItemStack.EMPTY : RingHelper.heldRing(player);
        if (player == null || !player.isAlive() || player.level() != level || ring.isEmpty()
                || player.distanceToSqr(center) > MAX_DISTANCE_SQR) {
            stopCharging(player, false);
            return;
        }

        chargeTicks++;
        RingEnergy.add(ring, GLConfig.CHARGE_PER_TICK.get());

        // Energy stream from the lantern to the ring.
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.4)).subtract(0, 0.5, 0);
        Vec3 from = center.add(0, 0.25, 0);
        Vec3 delta = hand.subtract(from);
        for (int i = 0; i < 2; i++) {
            double t = level.random.nextDouble();
            Vec3 p = from.add(delta.scale(t));
            level.sendParticles(ModParticles.GLOW.get(), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(ModParticles.SPARK.get(), hand.x, hand.y, hand.z, 1, 0.1, 0.1, 0.1, 0.02);

        if (chargeTicks % 40 == 0) {
            level.playSound(null, worldPosition, ModSounds.CHARGE_LOOP.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }

        float fraction = RingEnergy.get(ring).fraction();
        if (GLConfig.SHOW_OATH.get()) {
            int line = Math.min(OATH_LINES - 1, (int) (fraction * OATH_LINES));
            if (chargeTicks % 20 == 0) sendOathLine(player, line);
        }
        if (RingEnergy.get(ring).isFull()) {
            stopCharging(player, true);
        }
    }

    private void sendOathLine(ServerPlayer player, int line) {
        player.displayClientMessage(Component.translatable("oath.greenlantern.line" + (line + 1))
                .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC), true);
    }

    private void stopCharging(@Nullable ServerPlayer player, boolean complete) {
        chargingPlayer = null;
        chargeTicks = 0;
        setChargingState(false);
        if (player != null && complete) {
            level.playSound(null, worldPosition, ModSounds.CHARGE_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.greenlantern.charged").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ModParticles.SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
            }
        } else if (player != null) {
            player.displayClientMessage(Component.translatable("message.greenlantern.charge_stopped"), true);
        }
    }

    private void setChargingState(boolean charging) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(PowerBatteryBlock.CHARGING) && state.getValue(PowerBatteryBlock.CHARGING) != charging) {
            level.setBlock(worldPosition, state.setValue(PowerBatteryBlock.CHARGING, charging), 3);
        }
    }
}
