package com.danrod505.greenlantern.gametest.scripts;

import com.danrod505.greenlantern.gametest.ClientScript;
import java.util.List;

/**
 * Every screenshot script, by name. The Screenshots workflow picks one with its {@code script}
 * input, or from the branch name with {@code .github/screenshot-scripts.txt}.
 */
public final class ClientScripts {
    public static final List<ClientScript.Script> ALL = List.of(
            DefaultScript.SCRIPT,
            WheelScript.SCRIPT,
            FlightScript.SCRIPT,
            MechaScript.SCRIPT,
            OaScript.SCRIPT,
            FlashScript.SCRIPT,
            AquamanScript.SCRIPT,
            AtlantisScript.SCRIPT,
            KrakenScript.SCRIPT,
            BatmanScript.SCRIPT,
            MountsScript.SCRIPT,
            SupermanScript.SCRIPT,
            TrenchScript.SCRIPT,
            WonderWomanScript.SCRIPT,
            CyborgScript.SCRIPT
            // tools/new_hero.py adds new heroes above this line
    );

    private ClientScripts() {}

    /** The script with that name; any other value (like {@code 1}) plays the default one. */
    public static ClientScript.Script byName(String name) {
        for (ClientScript.Script script : ALL) {
            if (script.name().equals(name)) return script;
        }
        return DefaultScript.SCRIPT;
    }
}
