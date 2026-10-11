package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Skeleton;
import com.danrod505.greenlantern.engine.SkinState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * How a player wearing the full suit of a character of the engine looks: the skin chosen (kept on
 * the chest piece, which every client sees) on the character's skeleton. Null for everyone else,
 * the first 9 heroes included, and when the skin's texture is missing.
 */
public record SkinLook(EngineHero hero, CharacterData.Skin skin, Skeleton skeleton) {
    private static final Map<Identifier, Boolean> EXISTS = new HashMap<>();

    public static SkinLook of(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity entity = mc.level.getEntity(entityId);
        return entity instanceof Player player ? of(player) : null;
    }

    public static SkinLook of(Player player) {
        EngineHero hero = EngineServer.suited(player);
        if (hero == null || !hero.fullSuit(player)) return null;
        CharacterData data = hero.data();
        SkinState state = player.getItemBySlot(EquipmentSlot.CHEST).get(EngineContent.SKIN.get());
        CharacterData.Skin skin = data.defaultSkin();
        if (state != null) {
            for (CharacterData.Skin s : data.skins()) {
                if (s.id().equals(state.selected())) skin = s;
            }
        }
        if (!exists(skin.texture())) return null;
        return new SkinLook(hero, skin, data.skeleton());
    }

    /** Whether a texture is in the resource packs (asked once per texture). */
    public static boolean exists(Identifier texture) {
        return EXISTS.computeIfAbsent(texture, t -> Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
    }

    public static void clearCache() {
        EXISTS.clear();
    }
}
