package com.danrod505.greenlantern.client.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.HeroHud;
import com.danrod505.greenlantern.cyborg.CyborgContent;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Cyborg: the standard hero HUD and the hero's section in the Heroes' Guide. */
public final class CyborgClient implements HeroClient {
    /** Same grid as {@code data/greenlantern/recipe/mother_box.json}. */
    private static final GuideScreen.Recipe RECIPE = new GuideScreen.Recipe("mother_box", () -> new ItemStack(CyborgContent.ITEM.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
            null, null, null));

    private final HeroHud hud = new HeroHud(CyborgHero.INSTANCE, 0xB8BEC8, 0xFF5A3C, 0xC0C81E1E, 0xFFFF5A3C, null);

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("cyborg_hud"), hud::render);
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(new GuideScreen.Section("cyborg", () -> CyborgContent.ITEM.get().charged(CyborgContent.ITEM.get().getDefaultInstance()), 0xFFC81E1E,
                List.of(GuideScreen.text("cyborg"), GuideScreen.text("cyborg_powers"), GuideScreen.text("controls_cyborg"), GuideScreen.recipes("cyborg", RECIPE))));
    }
}
