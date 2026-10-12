package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.PowerSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;

/**
 * The wheel of one of the first 9 heroes in the new model (stage 3, lot 1): the abilities of its
 * file, run by the engine like those of any character, paid with the hero's own energy and drawn
 * with the colours of its wheel of today (the icons are drawn by {@link AbilityIcons}). The powers of
 * today stay as they were ({@link #legacy()}): the abilities call them by name through the preset
 * {@code poder_de_hoje}, and the hero's own controls (the grapple, the tornado) still use them.
 */
public final class LegacyWheel {
    /** The traits the code of the 9 heroes already did before: the engine does not do them again. */
    public static final Set<String> OWN_TRAITS = Set.of("voo", "planar", "aquatico", "supervelocidade");
    /** Height of the HUD of one of the 9 heroes: its energy bar, the wheel of the new model and a line of hints. */
    public static final int HUD_HEIGHT = 74;

    private final CharacterData data;
    private final HeroPowers<EngineAbility> powers;
    private final HeroPowers<?> legacy;

    /**
     * @param legacy the powers of today (null for the Lantern, whose constructs are its powers of today)
     * @param style  the hero's wheel of today: its colours and texts stay, the icons are the engine's
     */
    public LegacyWheel(String id, HeroEnergy energy, WheelStyle style, ChatFormatting color, float pitchBase, float pitchStep, HeroPowers<?> legacy) {
        this.data = Characters.get(id);
        this.legacy = legacy;
        List<CharacterData.Ability> list = data.abilities();
        EngineAbility[] abilities = new EngineAbility[list.size()];
        for (int i = 0; i < abilities.length; i++) {
            abilities[i] = new EngineAbility(list.get(i), i, list.get(i).kind().orElseThrow(() -> new IllegalStateException(id + ": an ability without classe")));
        }
        WheelStyle wheel = new WheelStyle(style.theme(), GreenLantern.id("engine_icons/" + id), AbilityIcons.SIZE * AbilityIcons.cells(data),
                style.costKey(), style.energyKey(), style.background(), style.nameColor(), style.costColor(), style.textColor(), style.dimIcon(),
                style.numberColor(), style.hintColor());
        this.powers = new HeroPowers<>(new PowerSet<>(abilities), energy, color, pitchBase, pitchStep, EngineServer::useAbility, wheel);
    }

    public CharacterData data() {
        return data;
    }

    public HeroPowers<EngineAbility> powers() {
        return powers;
    }

    /** The powers of today, or null. */
    public HeroPowers<?> legacy() {
        return legacy;
    }
}
