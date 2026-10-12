package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.CharacterHero;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.LegacyHero;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Turns a character's icon sheet ({@link AbilityIcons}) into the texture its wheel and HUD draw.
 * For the 9 heroes, an ability or function that runs a power of their own code keeps that power's
 * icon from their old sheet (the supreme with its gold border).
 */
public final class EngineIcons {
    private static final Set<String> DONE = new HashSet<>();

    private EngineIcons() {}

    public static void ensure(CharacterHero hero) {
        if (!DONE.add(hero.id())) return;
        List<com.danrod505.greenlantern.engine.AbilityKind> kinds = new ArrayList<>();
        for (int i = 0; i < hero.powers().set().count(); i++) kinds.add(((EngineAbility) hero.powers().power(i)).kind());
        CharacterData data = hero.data();
        int[] pixels = AbilityIcons.sheet(data, kinds);
        int width = pixels.length / AbilityIcons.SIZE;
        if (hero instanceof LegacyHero legacy) ownIcons(legacy, data, pixels, width);
        NativeImage image = new NativeImage(width, AbilityIcons.SIZE, true);
        for (int y = 0; y < AbilityIcons.SIZE; y++) {
            for (int x = 0; x < width; x++) image.setPixel(x, y, pixels[y * width + x]);
        }
        Identifier id = hero.powers().wheel().icons();
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }

    /** Copies the icons of the powers of the hero's own code from its old sheet onto the cells that run them. */
    private static void ownIcons(LegacyHero hero, CharacterData data, int[] pixels, int width) {
        NativeImage old = read(hero.ownWheel().icons());
        if (old == null) return;
        try (old) {
            List<CharacterData.Ability> abilities = data.abilities();
            for (int a = 0; a < abilities.size(); a++) {
                CharacterData.Ability ability = abilities.get(a);
                List<CharacterData.Function> functions = ability.allFunctions();
                for (int f = 0; f < functions.size(); f++) {
                    String power = LegacyHero.ownPowerOf(ability, functions.get(f)).orElse(null);
                    int index = power == null ? -1 : hero.ownIconIndex(power);
                    if (index < 0 || (index + 1) * AbilityIcons.SIZE > old.getWidth()) continue;
                    int cell = AbilityIcons.functionCell(data, a, f);
                    for (int y = 0; y < AbilityIcons.SIZE; y++) {
                        for (int x = 0; x < AbilityIcons.SIZE; x++) {
                            int argb = old.getPixel(index * AbilityIcons.SIZE + x, y);
                            boolean edge = x == 0 || y == 0 || x == AbilityIcons.SIZE - 1 || y == AbilityIcons.SIZE - 1;
                            if (f == 0 && ability.supreme() && edge) argb = 0xFF000000 | AbilityIcons.GOLD;
                            pixels[y * width + cell * AbilityIcons.SIZE + x] = argb;
                        }
                    }
                }
            }
        }
    }

    private static NativeImage read(Identifier texture) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture)) {
            return NativeImage.read(in);
        } catch (IOException e) {
            GreenLantern.LOGGER.warn("Could not read the icon sheet {}", texture, e);
            return null;
        }
    }
}
