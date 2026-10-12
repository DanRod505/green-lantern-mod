package com.danrod505.greenlantern.client.shazam;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.HeroHud;
import com.danrod505.greenlantern.shazam.ShazamConfig;
import com.danrod505.greenlantern.shazam.ShazamContent;
import com.danrod505.greenlantern.shazam.ShazamHero;
import com.danrod505.greenlantern.shazam.ShazamPower;
import com.danrod505.greenlantern.shazam.ShazamServer;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Shazam: the hero HUD, the health of the aimed creature under Wisdom of Solomon and the hero's section in the Heroes' Guide. */
public final class ShazamClient implements HeroClient {
    /** Same grid as {@code data/greenlantern/recipe/wizard_bolt.json}. */
    private static final GuideScreen.Recipe RECIPE = new GuideScreen.Recipe("wizard_bolt", () -> new ItemStack(ShazamContent.ITEM.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
            null, null, null));

    private final HeroHud hud = new HeroHud(ShazamHero.INSTANCE, 0xC81E1E, 0xFFF6B0, 0xC0F2B705, 0xFFFFF6B0, (player, power) -> {
        ItemStack item = ShazamHero.INSTANCE.findItem(player);
        return power.id().equals(ShazamPower.ATLAS_STAMINA.id()) && ShazamServer.isAtlas(item) || power.id().equals(ShazamPower.SOLOMON_WISDOM.id()) && ShazamServer.isSolomon(item);
    });

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new ShazamLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("shazam_hud"), hud::render);
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("shazam_solomon"), ShazamClient::renderSolomon);
    }

    /** Wisdom of Solomon: the name and health of the creature he aims at, under the crosshair. */
    private static void renderSolomon(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || !ShazamHero.INSTANCE.isSuited(player)) return;
        if (!ShazamServer.isSolomon(ShazamHero.INSTANCE.findItem(player))) return;
        LivingEntity target = aimed(player, ShazamConfig.SOLOMON_RANGE.get());
        if (target == null) return;
        Component line = Component.translatable("hud.greenlantern.shazam_solomon_target", target.getDisplayName(),
                Mth.ceil(target.getHealth()), Mth.ceil(target.getMaxHealth()));
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        int w = mc.font.width(line);
        graphics.fill(x - w / 2 - 3, y - 2, x + w / 2 + 3, y + 10, 0x90200404);
        int barW = w + 6;
        float f = Mth.clamp(target.getHealth() / Math.max(1.0F, target.getMaxHealth()), 0.0F, 1.0F);
        graphics.fill(x - barW / 2, y + 10, x - barW / 2 + Math.round(barW * f), y + 12, 0xFFF2B705);
        graphics.drawCenteredString(mc.font, line, x, y, 0xFFFFF6B0);
    }

    /** The first living thing along the player's line of sight, up to {@code range} blocks, stopping at blocks. */
    private static LivingEntity aimed(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), 0.3F);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(new GuideScreen.Section("shazam", () -> ShazamContent.ITEM.get().charged(ShazamContent.ITEM.get().getDefaultInstance()), 0xFFF2B705,
                List.of(GuideScreen.text("shazam"), GuideScreen.text("shazam_powers"), GuideScreen.text("controls_shazam"), GuideScreen.recipes("shazam", RECIPE))));
    }
}
