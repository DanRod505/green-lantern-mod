package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityWheel;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPower;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * Turns a hero's icon sheet ({@link AbilityIcons}) into the texture its wheel and HUD draw. The
 * functions that use one of the hero's own powers (the first 9 heroes) keep that power's icon from
 * the hero's sheet of before, and so does the emblem.
 */
public final class EngineIcons {
    private static final Set<String> DONE = new HashSet<>();

    private EngineIcons() {}

    public static void ensure(HeroDefinition hero) {
        if (!hero.abilityWheel() || !DONE.add(hero.id())) return;
        CharacterData data = hero.data();
        List<com.danrod505.greenlantern.engine.AbilityKind> kinds = new ArrayList<>();
        for (int i = 0; i < hero.powers().set().count(); i++) kinds.add(((EngineAbility) hero.powers().power(i)).kind());
        int[] pixels = AbilityIcons.sheet(data, kinds);
        int width = pixels.length / AbilityIcons.SIZE;
        NativeImage image = new NativeImage(width, AbilityIcons.SIZE, true);
        for (int y = 0; y < AbilityIcons.SIZE; y++) {
            for (int x = 0; x < width; x++) image.setPixel(x, y, pixels[y * width + x]);
        }
        if (hero.ownIcons() != null) ownIcons(hero, data, image);
        Identifier id = hero.powers().wheel().icons();
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }

    /** The icons of the hero's own powers on the cells of the functions that use them, and the emblem last. */
    private static void ownIcons(HeroDefinition hero, CharacterData data, NativeImage image) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(hero.ownIcons());
        if (resource.isEmpty()) return;
        try (InputStream in = resource.get().open(); NativeImage own = NativeImage.read(in)) {
            int ownCells = own.getWidth() / AbilityIcons.SIZE;
            List<CharacterData.Ability> abilities = data.abilities();
            for (int a = 0; a < abilities.size(); a++) {
                CharacterData.Ability ability = abilities.get(a);
                List<CharacterData.Function> functions = ability.allFunctions();
                for (int f = 0; f < functions.size(); f++) {
                    HeroPower power = AbilityWheel.ownPowerOf(hero, ability, functions.get(f));
                    if (power == null || power.iconIndex() >= ownCells) continue;
                    int cell = AbilityIcons.functionCell(data, a, f);
                    copy(own, power.iconIndex(), image, cell);
                    if (f == 0 && ability.supreme()) border(image, cell);
                }
            }
            int emblem = hero.ownEmblemIcon();
            if (emblem >= 0 && emblem < ownCells) copy(own, emblem, image, AbilityIcons.cells(data) - 1);
        } catch (IOException e) {
            GreenLantern.LOGGER.warn("Could not read the icons of {}: {}", hero.id(), e.toString());
        }
    }

    private static void copy(NativeImage from, int fromCell, NativeImage to, int toCell) {
        for (int y = 0; y < AbilityIcons.SIZE; y++) {
            for (int x = 0; x < AbilityIcons.SIZE; x++) {
                to.setPixel(toCell * AbilityIcons.SIZE + x, y, from.getPixel(fromCell * AbilityIcons.SIZE + x, y));
            }
        }
    }

    /** The gold frame of the supreme ability. */
    private static void border(NativeImage image, int cell) {
        int gold = 0xFF000000 | AbilityIcons.GOLD;
        int last = AbilityIcons.SIZE - 1;
        for (int k = 0; k < AbilityIcons.SIZE; k++) {
            image.setPixel(cell * AbilityIcons.SIZE + k, 0, gold);
            image.setPixel(cell * AbilityIcons.SIZE + k, last, gold);
            image.setPixel(cell * AbilityIcons.SIZE, k, gold);
            image.setPixel(cell * AbilityIcons.SIZE + last, k, gold);
        }
    }
}
