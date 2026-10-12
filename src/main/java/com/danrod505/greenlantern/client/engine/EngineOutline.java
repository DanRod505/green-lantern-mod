package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.client.cyborg.CyborgScanVisuals;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.preset.GatheringPresets;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The outline of the block an engine character aims at (the new model of abilities, 2.6.0): when
 * the function V would use now is a world preset, the block it would work on shows in the
 * character's light colour through walls, as big as the preset's {@code tamanho} square.
 */
final class EngineOutline implements WorldLayer {
    private static final Identifier BOX = GreenLantern.id("textures/misc/xray_box.png");

    private final EngineHero hero;
    /** The box to draw this frame, relative to the origin, or null. */
    private float[] box;

    EngineOutline(EngineHero hero) {
        this.hero = hero;
    }

    @Override
    public void extract(Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, float partialTick) {
        box = null;
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui || !hero.isSuited(player)) return;
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        EngineAbility ability = (EngineAbility) hero.powers().power(hero.powers().selectedIndex(item));
        CharacterData.Ability data = ability.data();
        AbilityFunctions.Chosen chosen = AbilityFunctions.choose(data, EngineKeys.press(ability), player);
        if (!Presets.hasWorld(chosen.function().preset())) return;
        CharacterData.Numbers numbers = data.numbers(chosen.function());
        double reach = numbers.reach() > 0 ? Math.min(numbers.reach(), 32.0) : AbilityFunctions.AIM_REACH;
        HitResult hit = player.pick(reach, partialTick, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return;
        int size = Mth.clamp(data.settings(chosen.function()).whole("tamanho", 1), 1, 9);
        BlockPos min = block.getBlockPos();
        BlockPos max = min;
        if (size > 1) {
            for (BlockPos pos : GatheringPresets.face(block, size, 1)) {
                min = BlockPos.min(min, pos);
                max = BlockPos.max(max, pos);
            }
        }
        box = new float[] {
                (float) (min.getX() - origin.x) - 0.01F, (float) (min.getY() - origin.y) - 0.01F, (float) (min.getZ() - origin.z) - 0.01F,
                (float) (max.getX() + 1 - origin.x) + 0.01F, (float) (max.getY() + 1 - origin.y) + 0.01F, (float) (max.getZ() + 1 - origin.z) + 0.01F,
                Float.intBitsToFloat(0xC0000000 | hero.data().colors().light())};
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        float[] b = box;
        if (b == null) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(BOX),
                (pose, vc) -> CyborgScanVisuals.box(vc, pose, b[0], b[1], b[2], b[3], b[4], b[5], Float.floatToRawIntBits(b[6])));
    }
}
