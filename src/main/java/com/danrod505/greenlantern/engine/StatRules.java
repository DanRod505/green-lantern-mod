package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.SuitModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Turns the stats of a character of the engine into its suit (catalog, stage 12): the armor of the
 * pieces adds up to the defense, and the bonuses give the hearts, the damage, the speed, the
 * firmness and what the class passive changes in the body.
 */
public final class StatRules {
    /** How the defense is split among head, chest, legs and feet (like diamond armor: 3, 8, 6, 3). */
    private static final int[] WEIGHTS = {3, 8, 6, 3};

    private StatRules() {}

    public static CharacterData.Suit suit(CharacterData data) {
        int total = data.defense();
        int[] pieces = new int[4];
        int given = 0;
        for (int i = 0; i < 4; i++) {
            pieces[i] = (int) Math.floor(total * WEIGHTS[i] / 20.0);
            given += pieces[i];
        }
        pieces[1] += total - given;
        return new CharacterData.Suit(Optional.of(Characters.SKIN_SUIT.identifier()), new CharacterData.Defense(pieces[0], pieces[1], pieces[2], pieces[3]),
                data.level() >= 4 ? 4.0 : 2.0, 0.0, Identifier.withDefaultNamespace("item.armor.equip_generic"),
                Identifier.withDefaultNamespace("repairs_diamond_armor"), List.of());
    }

    public static List<SuitModifier> bonuses(CharacterData data) {
        String id = data.id();
        List<SuitModifier> list = new ArrayList<>();
        double health = (data.hearts() - 10.0) * 2.0;
        if (health != 0) list.add(SuitModifier.add(Attributes.MAX_HEALTH, id + "_health", health));
        if (data.damage() != 0) list.add(SuitModifier.add(Attributes.ATTACK_DAMAGE, id + "_strength", data.damage()));
        if (data.speed() != 0) list.add(SuitModifier.multiply(Attributes.MOVEMENT_SPEED, id + "_speed", data.speed() / 100.0));
        if (data.firmness() != 0) list.add(SuitModifier.add(Attributes.KNOCKBACK_RESISTANCE, id + "_steady", data.firmness()));
        switch (data.passive()) {
            case UNSHAKEABLE -> list.add(SuitModifier.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, id + "_blast_proof", 1.0));
            case QUICK_STEP -> list.add(SuitModifier.add(Attributes.STEP_HEIGHT, id + "_step", 0.5));
            default -> {
            }
        }
        return list;
    }
}
