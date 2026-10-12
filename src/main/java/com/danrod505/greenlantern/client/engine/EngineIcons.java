package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Turns a character's icon sheet ({@link AbilityIcons}) into the texture its wheel and HUD draw. */
public final class EngineIcons {
    private static final Set<String> DONE = new HashSet<>();

    private EngineIcons() {}

    public static void ensure(EngineHero hero) {
        if (!DONE.add(hero.id())) return;
        List<com.danrod505.greenlantern.engine.AbilityKind> kinds = new ArrayList<>();
        for (int i = 0; i < hero.powers().set().count(); i++) kinds.add(((EngineAbility) hero.powers().power(i)).kind());
        int[] pixels = AbilityIcons.sheet(hero.data(), kinds);
        int width = pixels.length / AbilityIcons.SIZE;
        NativeImage image = new NativeImage(width, AbilityIcons.SIZE, true);
        for (int y = 0; y < AbilityIcons.SIZE; y++) {
            for (int x = 0; x < width; x++) image.setPixel(x, y, pixels[y * width + x]);
        }
        Identifier id = hero.powers().wheel().icons();
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }
}
